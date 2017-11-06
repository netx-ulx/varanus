package net.varanus.sdncontroller.monitoring.submodules.probing.internal;


import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.packet.IPacket;
import net.varanus.sdncontroller.linkstats.ILinkStatsService;
import net.varanus.sdncontroller.linkstats.sample.SecureProbingSample;
import net.varanus.sdncontroller.logging.Logging;
import net.varanus.sdncontroller.monitoring.internal.IMonitoringModuleContext;
import net.varanus.sdncontroller.monitoring.submodules.collectorhandler.ICollectorHandlerService;
import net.varanus.sdncontroller.monitoring.submodules.probing.internal.SwitchComm.SwitchCommException;
import net.varanus.sdncontroller.monitoring.util.ISubmoduleManager;
import net.varanus.sdncontroller.types.DatapathLink;
import net.varanus.sdncontroller.util.DurationRange;
import net.varanus.sdncontroller.util.IPacketUtils;
import net.varanus.sdncontroller.util.module.ModuleUtils;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.collect.CollectionUtils;
import net.varanus.util.collect.Cycler;
import net.varanus.util.concurrent.ConcurrencyUtils;
import net.varanus.util.concurrent.ConcurrentService;
import net.varanus.util.io.ByteBuffers;
import net.varanus.util.io.ByteBuffers.BufferType;
import net.varanus.util.openflow.PacketBits;
import net.varanus.util.openflow.PacketBits.BitField;
import net.varanus.util.openflow.PacketBits.BitHeader;
import net.varanus.util.openflow.types.BitMatch;
import net.varanus.util.openflow.types.Flow;
import net.varanus.util.openflow.types.MatchEntry;
import net.varanus.util.openflow.types.NodeId;
import net.varanus.util.openflow.types.PortId;
import net.varanus.util.openflow.types.BitMatch.BitMasked;
import net.varanus.util.security.SecureRandoms;
import net.varanus.util.security.UnavailableSecureRandomException;
import net.varanus.util.time.TimeLong;


/**
 *
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
final class Prober extends ConcurrentService implements ISubmoduleManager
{
    private static final TimeLong IDLE_SLEEP_TIME = TimeLong.of(1, TimeUnit.SECONDS);

    private static final Logger LOG = Logging.monitoring.probing.LOG;

    private final ProberState state;
    private final Object      stateLock;

    private @Nullable DurationRange probDurationRange;
    private @Nullable TimeLong      preXmitDuration;

    Prober()
    {
        super(ConcurrencyUtils.defaultDaemonThreadFactory(), ( msg, ex ) -> LOG.error(msg, ex));
        this.state = new ProberState();
        this.stateLock = new Object();
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies()
    {
        return state.getModuleDependencies();
    }

    @Override
    public void init( IMonitoringModuleContext context ) throws FloodlightModuleException
    {
        Map<String, String> params = context.getConfigParams();
        this.probDurationRange = Props.getProbingDurationRange(params);
        this.preXmitDuration = Props.getPreTransmissionDuration(params);

        state.init(context);
    }

    @Override
    public void startUp( IMonitoringModuleContext context ) throws FloodlightModuleException
    {
        LOG.info("Using a probing round duration range of {}", probDurationRange);
        LOG.info("Using a pre-transmission duration of {}", preXmitDuration);

        state.startUp(context);
        start();
    }

    @Override
    protected void startUp()
    {
        LOG.debug("Starting up prober");
    }

    @Override
    protected void shutDown()
    {
        LOG.debug("Shutting down prober");
    }

    Flow getProbeBaseFlow()
    {
        return state.getBaseFlow();
    }

    void onAddedNode( Optional<IOFSwitch> opSw, NodeId nodeId ) throws SwitchCommException
    {
        synchronized (stateLock) {
            state.setupProbingFlow(opSw, nodeId);
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

    @Override
    public void runInterruptibly() throws InterruptedException
    {
        try {
            while (true) {
                // =============================================================
                // Prepare the next probing, or sleep if no probing is scheduled
                // =============================================================
                Optional<Probing> opProbing = nextProbing();
                if (!opProbing.isPresent()) {
                    LOG.trace("No probing is scheduled; sleeping for {} ...", IDLE_SLEEP_TIME);
                    IDLE_SLEEP_TIME.sleep();
                    continue;
                }

                Probing probing = opProbing.get();
                LOG.debug("");
                LOG.debug("===== Running a new probing round =====");
                LOG.debug("Probing the following links: {}", probing.links());

                // =============================================================
                // Set up the parameters for the current round
                // =============================================================
                TimeLong probingDuration = probDurationRange.getRandomDuration();
                LOG.debug("Probing duration will be {}", probingDuration);

                // =============================================================
                // Start probing collections and send the probes after waiting
                // for a while
                // =============================================================
                LOG.debug("Launching probes...");
                probing.execute(probingDuration, preXmitDuration);

                // =============================================================
                // Wait until the probing is complete
                // =============================================================
                LOG.debug("Waiting until probing completes...");
                probing.get();
            }
        }
        catch (InterruptedException e) {
            LOG.debug("Prober was interrupted, now exiting");
            throw e;
        }
        catch (ExecutionException e) {
            Throwable cause = e.getCause();
            LOG.error(cause.getMessage(), cause);
        }
    }

    private Optional<Probing> nextProbing()
    {
        synchronized (stateLock) {
            return state.nextProbing();
        }
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    private static final class ProberState implements ISubmoduleManager
    {
        private final List<LinkAggregate>              aggregates;
        private final Cycler<LinkAggregate>            aggregateCycler;
        private final Map<DatapathLink, LinkAggregate> aggregatesByLink;

        private int            maxSimultProbings;
        private @Nullable Flow baseFlow;

        private @Nullable ICollectorHandlerService collHandService;
        private @Nullable IOFSwitchService         switchService;
        private @Nullable ILinkStatsService        linkStatsService;

        ProberState()
        {
            this.aggregates = new ArrayList<>();
            this.aggregateCycler = new Cycler<>();
            this.aggregatesByLink = new HashMap<>();
        }

        @Override
        public Collection<Class<? extends IFloodlightService>> getModuleDependencies()
        {
            return ModuleUtils.services(
                ICollectorHandlerService.class,
                IOFSwitchService.class,
                ILinkStatsService.class);
        }

        @Override
        public void init( IMonitoringModuleContext context ) throws FloodlightModuleException
        {
            Map<String, String> params = context.getConfigParams();
            this.maxSimultProbings = Props.getMaxSimultaneousProbings(params);
            EthType ethType = Props.getProbePacketEthertype(params);
            this.baseFlow = Flow.of(MatchEntry.ofExact(MatchField.ETH_TYPE, ethType));

            this.collHandService = context.getServiceImpl(ICollectorHandlerService.class);
            this.switchService = context.getServiceImpl(IOFSwitchService.class);
            this.linkStatsService = context.getServiceImpl(ILinkStatsService.class);
        }

        @Override
        public void startUp( IMonitoringModuleContext context ) throws FloodlightModuleException
        {
            LOG.info("Using a maximum of {} simultaneous probings", maxSimultProbings);
            LOG.info("Using a probe packet base flow of {}", baseFlow);
        }

        Flow getBaseFlow()
        {
            return Objects.requireNonNull(baseFlow, "probe base flow is not yet initialized");
        }

        void setupProbingFlow( Optional<IOFSwitch> opSw, NodeId nodeId ) throws SwitchCommException
        {
            OFPort sampPort = collHandService.getSamplingPort().getOFPort();
            SwitchComm.handleAddedSwitch(opSw, nodeId, baseFlow, sampPort);
        }

        Optional<Probing> nextProbing()
        {
            if (aggregateCycler.hasNext()) {
                return Optional.of(
                    aggregateCycler.next().newProbing(baseFlow, collHandService, switchService, linkStatsService));
            }
            else {
                return Optional.empty();
            }
        }

        void addLink( DatapathLink link )
        {
            LOG.debug("Adding link to be probed: {}", link);
            // first try to add to an existing set of links
            boolean added = false;
            for (LinkAggregate aggr : aggregates) {
                if (aggr.tryAddingLink(link)) {
                    aggregatesByLink.put(link, aggr);
                    added = true;
                    break;
                }
            }
            if (!added) {
                // if the previous failed, create a new aggregate with the new
                // link
                LinkAggregate newAggr = new LinkAggregate(link, maxSimultProbings);
                aggregatesByLink.put(link, newAggr);
                aggregates.add(newAggr);
                resetAggregateCycler();
            }
            LOG.trace("Current aggregates: {}", aggregates);
        }

        void removeLink( DatapathLink link )
        {
            LOG.debug("Removing flowed-link from being probed: {}", link);
            LinkAggregate aggr = aggregatesByLink.remove(link);
            aggr.removeLink(link);
            if (aggr.isEmpty()) {
                aggregates.remove(aggr);
                resetAggregateCycler();
            }
            LOG.trace("Current aggregates: {}", aggregates);
        }

        private void resetAggregateCycler()
        {
            aggregateCycler.reset(aggregates);
        }
    }

    /**
     * Aggregates multiple links that can be simultaneously probed.
     */
    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    private static final class LinkAggregate
    {
        private final Set<DatapathLink> links;
        private final int               maxSimultProbings;

        LinkAggregate( DatapathLink firstLink, int maxSimultProbings )
        {
            Preconditions.checkArgument(maxSimultProbings > 0);
            this.links = new LinkedHashSet<>(Collections.singleton(firstLink));
            this.maxSimultProbings = maxSimultProbings;
        }

        Probing newProbing( Flow baseFlow,
                            ICollectorHandlerService collHandService,
                            IOFSwitchService switchService,
                            ILinkStatsService linkStatsService )
        {
            return new Probing(ImmutableSet.copyOf(links),
                baseFlow, collHandService, switchService, linkStatsService);
        }

        boolean isEmpty()
        {
            return links.isEmpty();
        }

        // requires inexistent link within this aggregate
        boolean tryAddingLink( DatapathLink newLink )
        {
            Preconditions.checkArgument(!links.contains(newLink), "expected inexistent link");

            if (links.size() == maxSimultProbings)
                return false;

            links.add(newLink);
            return true;
        }

        // requires existent link within this aggregate
        void removeLink( DatapathLink link )
        {
            boolean removed = links.remove(link);
            Preconditions.checkArgument(removed, "expected existent link");
        }

        @Override
        public String toString()
        {
            return String.format("Aggregate(%d/%d)%s", links.size(), maxSimultProbings, links);
        }
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    private static final class Probing extends CompletableFuture<Void>
    {
        private final ImmutableSet<DatapathLink> links;
        private final ProbingUnit[]              units;

        Probing( ImmutableSet<DatapathLink> links,
                 Flow baseFlow,
                 ICollectorHandlerService collHandService,
                 IOFSwitchService switchService,
                 ILinkStatsService linkStatsService )
        {
            this.links = links;
            this.units = CollectionUtils.toArray(links,
                ( link ) -> {
                    ProbingUnit unit = new ProbingUnit(link, baseFlow, collHandService, switchService);
                    unit.thenAccept(sample -> linkStatsService.updateGeneralStats(sample));
                    return unit;
                },
                ProbingUnit.class);
        }

        ImmutableSet<DatapathLink> links()
        {
            return links;
        }

        void execute( TimeLong probingDuration, TimeLong preXmitDuration ) throws InterruptedException
        {
            Duration collDuration = probingDuration.plus(preXmitDuration).asDuration();
            for (ProbingUnit unit : units) {
                unit.startCollection(collDuration);
            }

            preXmitDuration.sleep();
            for (ProbingUnit unit : units) {
                unit.sendProbe();
            }

            CompletableFuture.allOf(units).whenComplete(( _void, ex ) -> {
                if (ex == null)
                    Probing.this.complete(null);
                else
                    Probing.this.completeExceptionally(ex);
            });
        }
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    private static final class ProbingUnit extends CompletableFuture<SecureProbingSample>
    {
        private final DatapathLink             link;
        private final BitMatch                 bitMatch;
        private final IPacket                  probePkt;
        private final ICollectorHandlerService collHandService;
        private final IOFSwitchService         switchService;

        ProbingUnit( DatapathLink link,
                     Flow baseFlow,
                     ICollectorHandlerService collHandService,
                     IOFSwitchService switchService )
        {
            this.link = link;
            long probeId = nextProbeId();
            this.bitMatch = buildProbingBitMatch(link, baseFlow, probeId);
            this.probePkt = buildProbePacket(link, baseFlow, probeId);
            this.collHandService = collHandService;
            this.switchService = switchService;
        }

        void startCollection( Duration collDuration )
        {
            if (collHandService.hasNecessaryCollectors(link)) {
                collHandService.sendProbingRequest(bitMatch, link, collDuration)
                    .exceptionally(ex -> {
                        LOG.error(ex.getMessage(), ex);
                        return SecureProbingSample.noResults(link);
                    }).thenAccept(this::complete);
            }
            else {
                LOG.debug("Cannot request probing collection due to lack of collectors for link {}", link);
                this.complete(SecureProbingSample.noResults(link));
            }
        }

        void sendProbe()
        {
            IOFSwitch srcSw = switchService.getActiveSwitch(link.getSrcNode().getDpid());
            if (srcSw != null) {
                OFPort sampPort = collHandService.getSamplingPort().getOFPort();
                SwitchComm.sendProbePacket(srcSw, link.getSrcEndpoint().undirected(), probePkt, sampPort);
            }
            else {
                LOG.warn("Failed to send probe due to lack of active source switch for link {}", link);
            }
        }

        private static BitMatch buildProbingBitMatch( DatapathLink link, Flow baseFlow, long probeId )
        {
            BitMatch.Builder builder = baseFlow.getBitMatch().createBuilder()
                .add(BitMasked.ofExact(SRC_NODE, getNodeIdBytes(link.getSrcNode())))
                .add(BitMasked.ofExact(SRC_PORT, getPortIdBytes(link.getSrcPort())))
                .add(BitMasked.ofExact(DEST_NODE, getNodeIdBytes(link.getDestNode())))
                .add(BitMasked.ofExact(DEST_PORT, getPortIdBytes(link.getDestPort())))
                .add(BitMasked.ofExact(PROBE_ID, getProbeIDBytes(probeId)));

            return builder.build();
        }

        private static IPacket buildProbePacket( DatapathLink link, Flow baseFlow, long probeId )
        {
            ByteBuffer payload = ByteBuffers.allocate(PAYLOAD.bytes(), BufferType.ARRAY_BACKED);
            putNodeIdBytes(payload, link.getSrcNode());
            putPortIdBytes(payload, link.getSrcPort());
            putNodeIdBytes(payload, link.getDestNode());
            putPortIdBytes(payload, link.getDestPort());
            putProbeIdBytes(payload, probeId);

            return IPacketUtils.fromMatch(baseFlow.getMatch(), payload.array());
        }

        private static byte[] getNodeIdBytes( NodeId nodeId )
        {
            ByteBuffer buf = ByteBuffers.allocate(SRC_NODE.bytes(), BufferType.ARRAY_BACKED);
            putNodeIdBytes(buf, nodeId);
            return buf.array();
        }

        private static void putNodeIdBytes( ByteBuffer buf, NodeId nodeId )
        {
            buf.putLong(nodeId.getLong());
        }

        private static byte[] getPortIdBytes( PortId portId )
        {
            ByteBuffer buf = ByteBuffers.allocate(SRC_PORT.bytes(), BufferType.ARRAY_BACKED);
            putPortIdBytes(buf, portId);
            return buf.array();
        }

        private static void putPortIdBytes( ByteBuffer buf, PortId portId )
        {
            buf.putInt(portId.getPortNumber());
        }

        private static byte[] getProbeIDBytes( long probeId )
        {
            ByteBuffer buf = ByteBuffers.allocate(PROBE_ID.bytes(), BufferType.ARRAY_BACKED);
            putProbeIdBytes(buf, probeId);
            return buf.array();
        }

        private static void putProbeIdBytes( ByteBuffer buf, long probeId )
        {
            buf.putLong(probeId);
        }

        private static long nextProbeId()
        {
            return SecureRandoms.threadSafeSecureRandom().nextLong();
        }

        static {
            try {
                SecureRandoms.assertAvailableStrongSeedSecureRandom();
            }
            catch (UnavailableSecureRandomException e) {
                throw new AssertionError(e);
            }
        }

        private static final int NODE_ID_BITLEN  = Long.SIZE;
        private static final int PORT_ID_BITLEN  = Integer.SIZE;
        private static final int PROBE_ID_BITLEN = Long.SIZE;

        private static final int SRC_NODE_BITOFF  = 0;
        private static final int SRC_PORT_BITOFF  = SRC_NODE_BITOFF + NODE_ID_BITLEN;
        private static final int DEST_NODE_BITOFF = SRC_PORT_BITOFF + PORT_ID_BITLEN;
        private static final int DEST_PORT_BITOFF = DEST_NODE_BITOFF + NODE_ID_BITLEN;
        private static final int PROBE_ID_BITOFF  = DEST_PORT_BITOFF + PORT_ID_BITLEN;

        private static final int PAYLOAD_DATA_BITLEN = (2 * (NODE_ID_BITLEN + PORT_ID_BITLEN)) + PROBE_ID_BITLEN;
        private static final int MIN_PAYLOAD_BITLEN  = (1500 - PacketBits.Eth.HEADER.bytes()) * Byte.SIZE;
        private static final int PAYLOAD_BITLEN      = Math.max(PAYLOAD_DATA_BITLEN, MIN_PAYLOAD_BITLEN);

        private static final BitHeader PAYLOAD = PacketBits.Eth.HEADER.nextHeader(PAYLOAD_BITLEN);

        private static final BitField SRC_NODE  = PAYLOAD.fieldAt(SRC_NODE_BITOFF, NODE_ID_BITLEN);
        private static final BitField SRC_PORT  = PAYLOAD.fieldAt(SRC_PORT_BITOFF, PORT_ID_BITLEN);
        private static final BitField DEST_NODE = PAYLOAD.fieldAt(DEST_NODE_BITOFF, NODE_ID_BITLEN);
        private static final BitField DEST_PORT = PAYLOAD.fieldAt(DEST_PORT_BITOFF, PORT_ID_BITLEN);
        private static final BitField PROBE_ID  = PAYLOAD.fieldAt(PROBE_ID_BITOFF, PROBE_ID_BITLEN);
    }
}
