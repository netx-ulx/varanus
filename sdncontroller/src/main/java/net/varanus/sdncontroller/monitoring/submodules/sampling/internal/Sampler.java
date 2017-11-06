package net.varanus.sdncontroller.monitoring.submodules.sampling.internal;


import static net.varanus.sdncontroller.monitoring.submodules.sampling.internal.Utils.getSamplingTags;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.projectfloodlight.openflow.protocol.OFFlowRemoved;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.types.VlanVid;
import org.slf4j.Logger;

import com.google.common.base.Preconditions;

import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.varanus.sdncontroller.linkstats.ILinkStatsService;
import net.varanus.sdncontroller.linkstats.sample.SwitchCounterSample;
import net.varanus.sdncontroller.linkstats.sample.TrajectorySample;
import net.varanus.sdncontroller.logging.Logging;
import net.varanus.sdncontroller.monitoring.internal.IMonitoringModuleContext;
import net.varanus.sdncontroller.monitoring.submodules.probing.IProbingService;
import net.varanus.sdncontroller.monitoring.submodules.sampling.internal.CollectionManager.CollectionRequester;
import net.varanus.sdncontroller.monitoring.submodules.sampling.internal.SwitchComm.SwitchCommException;
import net.varanus.sdncontroller.monitoring.util.ISubmoduleManager;
import net.varanus.sdncontroller.types.DatapathLink;
import net.varanus.sdncontroller.types.EndpointKind;
import net.varanus.sdncontroller.types.FlowedLink;
import net.varanus.sdncontroller.util.DurationRange;
import net.varanus.sdncontroller.util.module.ModuleUtils;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.collect.CollectionUtils;
import net.varanus.util.collect.Cycler;
import net.varanus.util.concurrent.AtomicCyclicInteger;
import net.varanus.util.concurrent.ConcurrencyUtils;
import net.varanus.util.concurrent.ConcurrentService;
import net.varanus.util.openflow.OFMessageUtils;
import net.varanus.util.openflow.types.Flow;
import net.varanus.util.openflow.types.FlowDirectedNodePort;
import net.varanus.util.openflow.types.NodeId;
import net.varanus.util.openflow.types.PortId;
import net.varanus.util.security.SecureRandoms;
import net.varanus.util.security.UnavailableSecureRandomException;
import net.varanus.util.time.TimeLong;
import net.varanus.util.time.Timed;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
final class Sampler extends ConcurrentService implements ISubmoduleManager
{
    private static final TimeLong IDLE_SLEEP_TIME = TimeLong.of(1, TimeUnit.SECONDS);

    private static final Logger LOG = Logging.monitoring.sampling.LOG;

    private final SwitchCommHelper switchComm;
    private final SamplerState     state;
    private final Object           stateLock;

    private final AtomicCyclicInteger currentTagIndex;

    private @Nullable DurationRange sampDurationRange;
    private @Nullable TimeLong      preSampDuration;
    private @Nullable TimeLong      postSampDuration;

    private @Nullable IProbingService probingService;

    Sampler( SwitchCommHelper switchComm )
    {
        super(ConcurrencyUtils.defaultDaemonThreadFactory(), ( msg, ex ) -> LOG.error(msg, ex));
        this.switchComm = switchComm;
        this.state = new SamplerState();
        this.stateLock = new Object();

        this.currentTagIndex = initSamplingVlanTagsIndexCycler(getSamplingTags().size());
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies()
    {
        return ModuleUtils.services(state.getModuleDependencies(), IProbingService.class);
    }

    @Override
    public void init( IMonitoringModuleContext context ) throws FloodlightModuleException
    {
        Map<String, String> params = context.getConfigParams();
        this.sampDurationRange = Props.getSamplingDurationRange(params);
        this.preSampDuration = Props.getPreSamplingExcessDuration(params);
        this.postSampDuration = Props.getPostSamplingExcessDuration(params);

        this.probingService = context.getServiceImpl(IProbingService.class);

        state.init(context);
    }

    @Override
    public void startUp( IMonitoringModuleContext context ) throws FloodlightModuleException
    {
        LOG.info("Using a sampling round duration range of {}", sampDurationRange);
        LOG.info("Using a pre-sampling excess duration of {}", preSampDuration);
        LOG.info("Using a post-sampling excess duration of {}", postSampDuration);

        state.startUp(context);
        start();
    }

    @Override
    protected void startUp()
    {
        LOG.debug("Starting up sampler");
    }

    @Override
    protected void shutDown()
    {
        LOG.debug("Shutting down sampler");
    }

    boolean isValidSamplableFlow( Flow flow )
    {
        return Utils.isValidSamplableMatch(flow.getMatch())
               && !probingFlowContainsSamplingFlow(flow);
    }

    void validateSamplableFlow( Flow flow ) throws IllegalArgumentException
    {
        Utils.validateSamplableMatch(flow.getMatch());
        Preconditions.checkArgument(
            !probingFlowContainsSamplingFlow(flow),
            "invalid samplable flow: is contained by secure probe base flow of %s",
            probingService.getProbeBaseFlow());
    }

    boolean startSamplingFlow( Flow flow ) throws IllegalArgumentException
    {
        validateSamplableFlow(flow);
        LOG.debug("Starting sampling flow {}", flow);
        if (samplingFlowContainsProbingFlow(flow))
            LOG.warn("Secure probe packets will not be sampled (secure probe base flow is {})",
                probingService.getProbeBaseFlow());

        synchronized (stateLock) {
            return state.addFlow(flow);
        }
    }

    private boolean samplingFlowContainsProbingFlow( Flow flow )
    {
        return flow.matchesAllOf(probingService.getProbeBaseFlow());
    }

    private boolean probingFlowContainsSamplingFlow( Flow flow )
    {
        return probingService.getProbeBaseFlow().matchesAllOf(flow);
    }

    boolean stopSamplingFlow( Flow flow ) throws IllegalArgumentException
    {
        validateSamplableFlow(flow);
        LOG.debug("Stopping sampling flow {}", flow);

        synchronized (stateLock) {
            return state.removeFlow(flow);
        }
    }

    void onAddedLink( DatapathLink link )
    {
        synchronized (stateLock) {
            state.addLink(link);
        }
    }

    void onRemovedLink( DatapathLink link )
    {
        synchronized (stateLock) {
            state.removeLink(link);
        }
    }

    void receiveSamplingResult( FlowDirectedNodePort endpoint, Timed<OFFlowRemoved> result )
    {
        synchronized (stateLock) {
            Optional<Sampling> optSamp = getMaybeActiveSampling();
            if (!optSamp.isPresent()) {
                LOG.warn("! Received a sampling result when there is no active sampling");
            }
            else {
                Sampling sampling = optSamp.get();
                Optional<FlowedLink> optLink = sampling.getLinkFromEndpoint(endpoint);
                if (!optLink.isPresent()) {
                    LOG.warn(
                        "! Received sampling result with unknown flowed-link end-point {}: {}",
                        endpoint,
                        OFMessageUtils.flowRemovedToString(result.value()));
                }
                else {
                    FlowedLink link = optLink.get();
                    Optional<SamplingUnit> optSampUnit = sampling.getUnit(link);
                    if (!optSampUnit.isPresent()) {
                        LOG.warn(
                            "! Received sampling result when no sampling exists for flowed-link {}: {}",
                            link,
                            OFMessageUtils.flowRemovedToString(result.value()));
                    }
                    else {
                        SamplingUnit unit = optSampUnit.get();
                        switch (EndpointKind.ofDirection(endpoint.getDirection())) {
                            case SOURCE: {
                                LOG.debug(
                                    "Received SRC flow statistics for flowed-link end-point {}: {}",
                                    endpoint,
                                    OFMessageUtils.flowRemovedToString(result.value()));

                                if (!unit.setSrcResult(result)) {
                                    LOG.warn(
                                        "Received duplicate SRC flow statistics for flowed-link end-point {}: {}",
                                        endpoint,
                                        OFMessageUtils.flowRemovedToString(result.value()));
                                }
                            }
                            break;

                            case DESTINATION: {
                                LOG.debug(
                                    "Received DEST flow statistics for flowed-link end-point {}: {}",
                                    endpoint,
                                    OFMessageUtils.flowRemovedToString(result.value()));

                                if (!unit.setDestResult(result)) {
                                    LOG.warn(
                                        "Received duplicate DEST flow statistics for flowed-link end-point {}: {}",
                                        endpoint,
                                        OFMessageUtils.flowRemovedToString(result.value()));
                                }
                            }
                            break;

                            default:
                                throw new AssertionError("unexpected enum value");
                        }
                    }
                }
            }
        }
    }

    @Override
    public void runInterruptibly() throws InterruptedException
    {
        try {
            final BarrierHandler barrierHandler = new BarrierHandler();

            while (true) {
                // =============================================================
                // Prepare the next active sampling, or sleep if no sampling is
                // scheduled
                // =============================================================
                if (!nextSampling()) {
                    LOG.trace("No sampling is scheduled; sleeping for {} ...", IDLE_SLEEP_TIME);
                    IDLE_SLEEP_TIME.sleep();
                    continue;
                }

                LOG.debug("");
                LOG.debug("===== Running a new sampling round =====");
                LOG.debug("Sampling the following links: {}", getActiveSamplingLinks());

                // =============================================================
                // Set up the parameters for the current round
                // =============================================================
                final TimeLong sampDuration = sampDurationRange.getRandomDuration();
                final VlanVid tag1 = getSamplingTags().get(currentTagIndex.getAndIncrement());
                final VlanVid tag2 = getSamplingTags().get(currentTagIndex.getAndIncrement());
                LOG.debug("Sampling duration will be {}; tags will be {} and {}",
                    new Object[] {sampDuration, tag1.getVlan(), tag2.getVlan()});

                // =============================================================
                // Install the sampling flows
                // =============================================================
                LOG.debug("Installing sampling flows with VLAN tag {}", tag1.getVlan());
                installSamplingFlows(tag1, barrierHandler);

                // =============================================================
                // Wait for the barrier replies
                // =============================================================
                LOG.debug("Waiting for barrier replies");
                barrierHandler.waitForReplies();

                // =============================================================
                // Send packet collection requests to collectors
                // =============================================================
                LOG.debug("Sending packet collection requests");
                Duration collDuration = preSampDuration.plus(sampDuration).plus(postSampDuration).asDuration();
                CompletableFuture<Void> collFuture = requestCollections(collDuration);

                // =============================================================
                // Wait for some excess time so that the switches have enough
                // time to set up the sampling flows
                // =============================================================
                LOG.debug("Waiting for a pre-sampling excess duration of {} ...", preSampDuration);
                preSampDuration.sleep();

                // =============================================================
                // 1st-pass update of the current tag in the source end-points
                // of all known links
                // =================================================================
                LOG.debug("Updating current tag in switches to tag {} (1st pass)", tag1.getVlan());
                Map<DatapathLink, Long> tag1stNanoTimes = updateTag(tag1, barrierHandler);

                // =============================================================
                // Wait for the barrier replies
                // =============================================================
                LOG.debug("Waiting for barrier replies");
                barrierHandler.waitForReplies();

                // =============================================================
                // Wait for some time so that the installed sampling rules
                // capture a certain portion of the traffic
                // =============================================================
                LOG.debug("Waiting for a sampling duration of {} ...", sampDuration);
                sampDuration.sleep();

                // =============================================================
                // 2nd-pass update of the current tag in the source end-points
                // for all known links
                // =============================================================
                LOG.debug("Updating current tag in switches to tag {} (2nd pass)", tag2.getVlan());
                Map<DatapathLink, Long> tag2ndNanoTimes = updateTag(tag2, barrierHandler);

                // =============================================================
                // Wait for the barrier replies
                // =============================================================
                LOG.debug("Waiting for barrier replies");
                barrierHandler.waitForReplies();

                // =============================================================
                // Wait for some excess time so that the remaining packets with
                // tag1 have time to fully traverse the links
                // =============================================================
                LOG.debug("Waiting for a post-sampling excess duration of {} ...", postSampDuration);
                postSampDuration.sleep();

                // =============================================================
                // Update to more accurate sampling durations (optimization) and
                // set the final sampling parameters
                // =============================================================
                LOG.debug("Optimizing sampling durations");
                Map<DatapathLink, TimeLong> sampDurations = new HashMap<>();
                for (Entry<DatapathLink, Long> t1PEntry : tag1stNanoTimes.entrySet()) {
                    DatapathLink link = t1PEntry.getKey();
                    if (tag2ndNanoTimes.containsKey(link)) {
                        long tag1stNanoTime = t1PEntry.getValue();
                        long tag2ndNanoTime = tag2ndNanoTimes.get(link);
                        final long elapsedNanos = tag2ndNanoTime - tag1stNanoTime;
                        sampDurations.put(
                            link,
                            (elapsedNanos > 0) ? TimeLong.ofNanos(elapsedNanos)
                                               : TimeLong.ZERO);
                    }
                }
                setSamplingParameters(sampDurations, tag1);

                // =============================================================
                // Remove the sampling flows
                // =============================================================
                LOG.debug("Removing sampling flows with VLAN tag {}", tag1.getVlan());
                purgeSamplingFlows(tag1, barrierHandler);

                // =============================================================
                // Wait for the barrier replies
                // =============================================================
                LOG.debug("Waiting for barrier replies");
                barrierHandler.waitForReplies();

                // =============================================================
                // Wait until the sampling is complete
                // =============================================================
                LOG.debug("Waiting until sampling completes...");
                try {
                    CompletableFuture.allOf(getSureActiveSampling(), collFuture).get();
                }
                catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof SamplingAbortedException) {
                        LOG.warn("Sampling was aborted: {}", cause.getMessage());
                    }
                    else {
                        LOG.error(cause.getMessage(), cause);
                        break;
                    }
                }
            }
        }
        catch (InterruptedException e) {
            LOG.debug("Sampler was interrupted, now exiting");
            throw e;
        }
    }

    private boolean nextSampling()
    {
        synchronized (stateLock) {
            return state.setNextActiveSampling();
        }
    }

    private Set<FlowedLink> getActiveSamplingLinks()
    {
        synchronized (stateLock) {
            return getSureActiveSampling().links();
        }
    }

    private void installSamplingFlows( VlanVid tag, BarrierHandler barrierHandler )
    {
        synchronized (stateLock) {
            Sampling sampling = getSureActiveSampling();
            for (SamplingUnit unit : sampling.units()) {
                try {
                    FlowedLink link = unit.getFlowedLink();
                    switchComm.installSamplingFlows(
                        link,
                        tag,
                        barrierHandler,
                        unit::getSrcSamplingAction,
                        unit::getDestSamplingAction);
                    LOG.trace("Sampling flows installed for flowed-link {}", link);
                }
                catch (SwitchCommException e) {
                    LOG.error(e.getMessage());
                }
            }
        }
    }

    private CompletableFuture<Void> requestCollections( Duration collDuration )
    {
        synchronized (stateLock) {
            Sampling sampling = getSureActiveSampling();
            List<CompletableFuture<?>> collFutures = CollectionUtils.toList(
                sampling.units(),
                unit -> unit.requestCollection(collDuration));
            return CompletableFuture.allOf(CollectionUtils.toArray(collFutures, CompletableFuture.class));
        }
    }

    // return the nanoTimes immediately after sending tag-change flow message
    private Map<DatapathLink, Long> updateTag( VlanVid tag, BarrierHandler barrierHandler )
    {
        // traverse all known links and change the current tag for each source
        // end-point

        synchronized (stateLock) {
            Map<DatapathLink, Long> nanoTimes = new LinkedHashMap<>();
            for (DatapathLink link : state.allLinks()) {
                try {
                    NodeId srcNodeId = link.getSrcNode();
                    PortId outPortId = link.getSrcPort();
                    switchComm.updateTag(srcNodeId, outPortId, tag);
                    nanoTimes.put(link, System.nanoTime());
                    switchComm.sendBarrier(srcNodeId, barrierHandler);
                    LOG.trace("Updated tag to {} for datapath-link {}", tag.getVlan(), link);
                }
                catch (SwitchCommException e) {
                    LOG.error(e.getMessage());
                }
            }

            return nanoTimes;
        }
    }

    private void setSamplingParameters( Map<DatapathLink, TimeLong> sampDurations, VlanVid tag )
    {
        synchronized (stateLock) {
            Sampling sampling = getSureActiveSampling();
            for (SamplingUnit unit : sampling.units()) {
                DatapathLink link = unit.getFlowedLink().unflowed();
                // get a dummy duration in case a link was removed meanwhile
                TimeLong sampDur = sampDurations.getOrDefault(link, TimeLong.ZERO);
                unit.setParameters(newParameters(sampDur, tag));
            }
        }
    }

    private SamplingParameters newParameters( TimeLong sampDur, VlanVid tag )
    {
        return new SamplingParameters(
            preSampDuration.asDuration(),
            sampDur.asDuration(),
            postSampDuration.asDuration(),
            tag);
    }

    private void purgeSamplingFlows( VlanVid tag, BarrierHandler barrierHandler )
    {
        synchronized (stateLock) {
            Set<NodeId> dejaVuNodes = new HashSet<>();
            Sampling sampling = getSureActiveSampling();
            for (SamplingUnit unit : sampling.units()) {
                try {
                    NodeId srcNodeId = unit.getFlowedLink().getSrcNode();
                    if (dejaVuNodes.add(srcNodeId)) {
                        switchComm.purgeSamplingTables(srcNodeId, tag, barrierHandler);
                    }
                }
                catch (SwitchCommException e) {
                    LOG.error(e.getMessage());
                }

                try {
                    NodeId destNodeId = unit.getFlowedLink().getDestNode();
                    if (dejaVuNodes.add(destNodeId)) {
                        switchComm.purgeSamplingTables(destNodeId, tag, barrierHandler);
                    }
                }
                catch (SwitchCommException e) {
                    LOG.error(e.getMessage());
                }
            }
        }
    }

    // NOTE: call only when stateLock is held and there is an active sampling
    private Sampling getSureActiveSampling()
    {
        return getMaybeActiveSampling()
            .orElseThrow(() -> new AssertionError("expected active sampling"));
    }

    // NOTE: call only when stateLock is held
    private Optional<Sampling> getMaybeActiveSampling()
    {
        return state.getActiveSampling();
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    private static final class SamplerState implements ISubmoduleManager
    {
        private final Set<DatapathLink> links;
        private final Set<Flow>         flows;

        private final List<FlowedLinkAggregate>            aggregates;
        private final Cycler<FlowedLinkAggregate>          aggregateCycler;
        private final Map<FlowedLink, FlowedLinkAggregate> aggregatesByLink;

        private Optional<Sampling> activeSampling;

        private int                         maxSimultSamplings;
        private @Nullable ILinkStatsService linkStatsService;
        private final CollectionManager     collManager;

        SamplerState()
        {
            this.links = new LinkedHashSet<>();
            this.flows = new LinkedHashSet<>();

            this.aggregates = new ArrayList<>();
            this.aggregateCycler = new Cycler<>();
            this.aggregatesByLink = new HashMap<>();

            this.activeSampling = Optional.empty();

            this.collManager = new CollectionManager();
        }

        @Override
        public Collection<Class<? extends IFloodlightService>> getModuleDependencies()
        {
            return ModuleUtils.services(collManager.getModuleDependencies(), ILinkStatsService.class);
        }

        @Override
        public void init( IMonitoringModuleContext context ) throws FloodlightModuleException
        {
            Map<String, String> params = context.getConfigParams();
            this.maxSimultSamplings = Props.getMaxSimultaneousSamplings(params);

            this.linkStatsService = context.getServiceImpl(ILinkStatsService.class);
            collManager.init(context);
        }

        @Override
        public void startUp( IMonitoringModuleContext context ) throws FloodlightModuleException
        {
            LOG.info("Using a maximum of {} simultaneous samplings", maxSimultSamplings);

            collManager.startUp(context);
        }

        Optional<Sampling> getActiveSampling()
        {
            return activeSampling;
        }

        Set<DatapathLink> allLinks()
        {
            return Collections.unmodifiableSet(links);
        }

        boolean setNextActiveSampling()
        {
            if (aggregateCycler.hasNext()) {
                this.activeSampling = Optional.of(
                    aggregateCycler.next().newSampling(
                        collManager,
                        linkStatsService::updateFlowedStats,
                        linkStatsService::updateFlowedStats));
                return true;
            }
            else {
                this.activeSampling = Optional.empty();
                return false;
            }
        }

        void addLink( DatapathLink link )
        {
            boolean added = links.add(link);
            Preconditions.checkArgument(added, "duplicate link addition");
            flowedLinksFor(link).forEach(this::addFlowedLink);
        }

        void removeLink( DatapathLink link )
        {
            boolean removed = links.remove(link);
            Preconditions.checkArgument(removed, "unknown link removal");
            flowedLinksFor(link).forEach(this::removeFlowedLink);
        }

        boolean addFlow( Flow flow )
        {
            if (flows.add(flow)) {
                flowedLinksFor(flow).forEach(this::addFlowedLink);
                return true;
            }
            else {
                return false;
            }
        }

        boolean removeFlow( Flow flow )
        {
            if (flows.remove(flow)) {
                linkStatsService.clearAllFlowedStats(flow);
                flowedLinksFor(flow).forEach(this::removeFlowedLink);
                return true;
            }
            else {
                return false;
            }
        }

        private Stream<FlowedLink> flowedLinksFor( DatapathLink link )
        {
            return flows.stream().map(flow -> link.flowed(flow));
        }

        private Stream<FlowedLink> flowedLinksFor( Flow flow )
        {
            return links.stream().map(link -> link.flowed(flow));
        }

        private void addFlowedLink( FlowedLink link )
        {
            LOG.debug("Adding flowed-link to be sampled: {}", link);

            // first try to insert the link into an existing aggregate
            boolean added = false;
            for (FlowedLinkAggregate aggr : aggregates) {
                if (aggr.tryAddingLink(link)) {
                    aggregatesByLink.put(link, aggr);
                    added = true;
                    break;
                }
            }

            if (!added) {
                // if the previous failed, create a new aggregate with the new
                // link
                FlowedLinkAggregate newAggr = new FlowedLinkAggregate(link, maxSimultSamplings);
                aggregatesByLink.put(link, newAggr);
                aggregates.add(newAggr);
                resetAggregateCycler();
            }

            LOG.trace("Current aggregates: {}", aggregates);
        }

        // requires existent link
        private void removeFlowedLink( FlowedLink link )
        {
            LOG.debug("Removing flowed-link from being sampled: {}", link);
            FlowedLinkAggregate aggr = aggregatesByLink.remove(link);
            aggr.removeLink(link);
            if (aggr.isEmpty()) {
                aggregates.remove(aggr);
                resetAggregateCycler();
            }
            LOG.trace("Current aggregates: {}", aggregates);

            getActiveSampling().ifPresent(( sampling ) -> {
                sampling.getUnit(link).ifPresent(( unit ) -> {
                    LOG.debug("Aborting active sampling for flowed-link {}", link);
                    unit.abort(String.format("link was removed: %s", link));
                });
            });
        }

        private void resetAggregateCycler()
        {
            aggregateCycler.reset(aggregates);
        }
    }

    /**
     * Aggregates multiple flowed-links that can be safely sampled in
     * simultaneous.
     */
    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    private static final class FlowedLinkAggregate
    {
        private final Set<FlowedLink> links;
        private final int             maxSimultSamplings;

        FlowedLinkAggregate( FlowedLink firstLink, int maxSimultSamplings )
        {
            Preconditions.checkArgument(maxSimultSamplings > 0);
            this.links = new LinkedHashSet<>(Collections.singleton(firstLink));
            this.maxSimultSamplings = maxSimultSamplings;
        }

        Sampling newSampling( CollectionManager collMngr,
                              Consumer<SwitchCounterSample> cbSampleDeliverer,
                              Consumer<TrajectorySample> tbSampleDeliverer )
        {
            return new Sampling(links, collMngr, cbSampleDeliverer, tbSampleDeliverer);
        }

        boolean isEmpty()
        {
            return links.isEmpty();
        }

        // requires inexistent link within this aggregate
        boolean tryAddingLink( FlowedLink newLink )
        {
            Preconditions.checkArgument(!links.contains(newLink), "expected inexistent link");

            if (links.size() == maxSimultSamplings)
                return false;

            if (links.stream().anyMatch(
                link -> link.unflowed().coincidesWith(newLink.unflowed())
                        && Flow.areInclusive(link.getFlow(), newLink.getFlow())))
                return false;

            links.add(newLink);
            return true;
        }

        // requires existent link within this aggregate
        void removeLink( FlowedLink link )
        {
            boolean removed = links.remove(link);
            Preconditions.checkArgument(removed, "expected existent link");
        }

        @Override
        public String toString()
        {
            return String.format("Aggregate(%d/%d)%s", links.size(), maxSimultSamplings, links);
        }
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    private static final class SamplingParameters
    {
        final Duration preDuration;
        final Duration duration;
        final Duration postDuration;
        final VlanVid  tag;

        SamplingParameters( Duration preDuration, Duration duration, Duration postDuration, VlanVid tag )
        {
            this.preDuration = preDuration;
            this.duration = duration;
            this.postDuration = postDuration;
            this.tag = tag;
        }
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    private static final class Sampling extends CompletableFuture<Void>
    {
        // NOTE: we assume that a flowed-link end-point is only part of one
        // unique flowed-link

        private final Map<FlowedLink, SamplingUnit>         units;
        private final Map<FlowDirectedNodePort, FlowedLink> links;

        Sampling( Set<FlowedLink> links,
                  CollectionManager collMngr,
                  Consumer<SwitchCounterSample> cbSampleDeliverer,
                  Consumer<TrajectorySample> tbSampleDeliverer )
        {
            this.units = buildUnitsMap(links, collMngr, cbSampleDeliverer, tbSampleDeliverer);
            this.links = buildLinksMap(links);

            SamplingUnit[] unitsArr = CollectionUtils.toArray(units.values(), SamplingUnit.class);
            CompletableFuture.allOf(unitsArr).whenComplete(( _void, ex ) -> {
                if (ex == null)
                    Sampling.this.complete(null);
                else
                    Sampling.this.completeExceptionally(ex);
            });
        }

        private static Map<FlowedLink, SamplingUnit> buildUnitsMap( Set<FlowedLink> links,
                                                                    CollectionManager collMngr,
                                                                    Consumer<SwitchCounterSample> cbSampleDeliverer,
                                                                    Consumer<TrajectorySample> tbSampleDeliverer )
        {
            return Collections.unmodifiableMap(
                CollectionUtils.toMap(
                    links,
                    Function.identity(),
                    link -> new SamplingUnit(
                        link,
                        collMngr.newCollectionRequester(link),
                        cbSampleDeliverer,
                        tbSampleDeliverer)));
        }

        private static Map<FlowDirectedNodePort, FlowedLink> buildLinksMap( Set<FlowedLink> links )
        {
            int initialCapacity = CollectionUtils.initialMapCapacity(2 * links.size());
            Map<FlowDirectedNodePort, FlowedLink> map = new LinkedHashMap<>(initialCapacity);
            for (FlowedLink link : links) {
                map.put(link.getSrcEndpoint(), link);
                map.put(link.getDestEndpoint(), link);
            }
            return Collections.unmodifiableMap(map);
        }

        Set<FlowedLink> links()
        {
            return units.keySet();
        }

        Collection<SamplingUnit> units()
        {
            return units.values();
        }

        Optional<SamplingUnit> getUnit( FlowedLink link )
        {
            return Optional.ofNullable(units.get(link));
        }

        Optional<FlowedLink> getLinkFromEndpoint( FlowDirectedNodePort endpoint )
        {
            return Optional.ofNullable(links.get(endpoint));
        }
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    private static final class SamplingUnit extends CompletableFuture<SwitchCounterSample>
    {
        private final FlowedLink                 flowedLink;
        private final CollectionRequester        collRequester;
        private final Consumer<TrajectorySample> tbSampleDeliverer;

        private @Nullable SamplingParameters   params;
        private @Nullable Timed<OFFlowRemoved> srcResult;
        private @Nullable Timed<OFFlowRemoved> destResult;

        SamplingUnit( FlowedLink flowedLink,
                      CollectionRequester collRequester,
                      Consumer<SwitchCounterSample> cbSampleDeliverer,
                      Consumer<TrajectorySample> tbSampleDeliverer )
        {
            this.flowedLink = flowedLink;
            this.collRequester = collRequester;
            this.tbSampleDeliverer = tbSampleDeliverer;

            this.exceptionally(ex -> {
                LOG.warn("! Switch counter sampling failed: {}", ex.getMessage());
                return SwitchCounterSample.noResults(flowedLink);
            }).thenAccept(cbSampleDeliverer);
        }

        FlowedLink getFlowedLink()
        {
            return flowedLink;
        }

        Optional<OFAction> getSrcSamplingAction( OFVersion version )
        {
            return collRequester.getSrcSamplingAction(version);
        }

        Optional<OFAction> getDestSamplingAction( OFVersion version )
        {
            return collRequester.getDestSamplingAction(version);
        }

        CompletableFuture<?> requestCollection( Duration collDuration )
        {
            return collRequester.requestCollection(collDuration)
                .exceptionally(( ex ) -> {
                    LOG.warn("! Trajectory sampling failed: {}", ex.getMessage());
                    return TrajectorySample.noResults(flowedLink);
                }).thenAccept(tbSampleDeliverer);
        }

        void setParameters( SamplingParameters params )
        {
            this.params = params;
        }

        boolean setSrcResult( Timed<OFFlowRemoved> srcResult )
        {
            if (this.srcResult == null && this.params != null) {
                this.srcResult = srcResult;
                tryDeliverResults();
                return true;
            }
            else {
                return false;
            }
        }

        boolean setDestResult( Timed<OFFlowRemoved> destResult )
        {
            if (this.destResult == null && this.params != null) {
                this.destResult = destResult;
                tryDeliverResults();
                return true;
            }
            else {
                return false;
            }
        }

        void abort( String reason )
        {
            completeExceptionally(new SamplingAbortedException(reason));
        }

        private void tryDeliverResults()
        {
            if (canDeliverResults()) {
                Preconditions.checkNotNull(params, "expected set parameters");
                complete(SwitchCounterSample.of(flowedLink, srcResult, destResult,
                    params.preDuration, params.duration, params.postDuration, params.tag));
            }
        }

        private boolean canDeliverResults()
        {
            return this.srcResult != null
                   && this.destResult != null;
        }
    }

    private static AtomicCyclicInteger initSamplingVlanTagsIndexCycler( int numTags )
    {
        SecureRandom rand = SecureRandoms.threadSafeSecureRandom();
        AtomicCyclicInteger cycler = new AtomicCyclicInteger(numTags - 1);
        cycler.set(rand.nextInt(numTags));

        return cycler;
    }

    private static final class SamplingAbortedException extends RuntimeException
    {
        private static final long serialVersionUID = 1L;

        SamplingAbortedException( String message )
        {
            // the stack trace will not be written upon throwing
            super(message, null, true, false);
        }
    }

    static {
        try {
            SecureRandoms.assertAvailableStrongSeedSecureRandom();
        }
        catch (UnavailableSecureRandomException e) {
            throw new AssertionError(e);
        }
    }
}
