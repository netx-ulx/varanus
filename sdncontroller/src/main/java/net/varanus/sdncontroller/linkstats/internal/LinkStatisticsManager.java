package net.varanus.sdncontroller.linkstats.internal;


import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.slf4j.Logger;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.varanus.sdncontroller.linkstats.FlowedLinkStats;
import net.varanus.sdncontroller.linkstats.GeneralLinkStats;
import net.varanus.sdncontroller.linkstats.ILinkStatsListener;
import net.varanus.sdncontroller.linkstats.ILinkStatsService;
import net.varanus.sdncontroller.linkstats.sample.LLDPProbingSample;
import net.varanus.sdncontroller.linkstats.sample.SecureProbingSample;
import net.varanus.sdncontroller.linkstats.sample.SwitchCounterSample;
import net.varanus.sdncontroller.linkstats.sample.TrajectorySample;
import net.varanus.sdncontroller.logging.Logging;
import net.varanus.sdncontroller.topologygraph.ITopologyGraphListener;
import net.varanus.sdncontroller.topologygraph.ITopologyGraphService;
import net.varanus.sdncontroller.topologygraph.event.ITopologyLinkEvent;
import net.varanus.sdncontroller.topologygraph.event.ITopologyNodeEvent;
import net.varanus.sdncontroller.topologygraph.event.ITopologyPortEvent;
import net.varanus.sdncontroller.types.DatapathLink;
import net.varanus.sdncontroller.types.FlowedLink;
import net.varanus.sdncontroller.util.module.IModuleManager;
import net.varanus.sdncontroller.util.module.ModuleUtils;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.collect.CollectionUtils;
import net.varanus.util.functional.StreamUtils;
import net.varanus.util.openflow.OFMessageUtils;
import net.varanus.util.openflow.types.Flow;
import net.varanus.util.openflow.types.NodeId;
import net.varanus.util.time.Timed;
import net.varanus.util.unitvalue.si.InfoDouble;
import net.varanus.util.unitvalue.si.InfoLong;
import net.varanus.util.unitvalue.si.InfoLongUnit;
import net.varanus.util.unitvalue.si.MetricDouble;


/**
 *
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class LinkStatisticsManager implements IModuleManager, ILinkStatsService
{
    private static final Logger LOG = Logging.linkstats.LOG;

    private final Map<DatapathLink, GStats>            genStatsMap;
    private final Map<DatapathLink, Timed<InfoDouble>> cachedVirCapacities;
    private final Object                               stateLock;

    private final Set<ILinkStatsListener> listeners;

    private final TopologyListener topoListener;
    private final Debugger         debugger;

    private int    lldpLatWinSize;
    private int    secProbeLatWinSize;
    private int    secProbeLossWinSize;
    private int    trajLatWinSize;
    private int    trajLossWinSize;
    private int    pktDropRateWinSize;
    private double hystThresFactor;

    public LinkStatisticsManager()
    {
        this.genStatsMap = new HashMap<>();
        this.cachedVirCapacities = new HashMap<>();
        this.stateLock = new Object();

        this.listeners = ModuleUtils.newListenerSet();

        this.topoListener = new TopologyListener();
        this.debugger = new Debugger();
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies()
    {
        return ModuleUtils.services(debugger.getModuleDependencies(), ITopologyGraphService.class);
    }

    @Override
    public void init( FloodlightModuleContext context, Class<? extends IFloodlightModule> moduleClass )
        throws FloodlightModuleException
    {
        Map<String, String> params = context.getConfigParams(moduleClass);
        this.lldpLatWinSize = Props.getLLDPProbingLatencyWindowSize(params);
        this.secProbeLatWinSize = Props.getSecureProbingLatencyWindowSize(params);
        this.secProbeLossWinSize = Props.getSecureProbingLossWindowSize(params);
        this.trajLatWinSize = Props.getTrajectoryLatencyWindowSize(params);
        this.trajLossWinSize = Props.getTrajectoryLossWindowSize(params);
        this.pktDropRateWinSize = Props.getPacketDropRateWindowSize(params);
        this.hystThresFactor = Props.getHysteresisThresholdFactor(params);

        context.getServiceImpl(ITopologyGraphService.class).addListener(topoListener);
        debugger.init(context, moduleClass);
    }

    @Override
    public void startUp( FloodlightModuleContext context, Class<? extends IFloodlightModule> moduleClass )
        throws FloodlightModuleException
    {
        LOG.info("Using an LLDP-probing latency window size of {}", lldpLatWinSize);
        LOG.info("Using a secure-probing latency window size of {}", secProbeLatWinSize);
        LOG.info("Using a secure-probing loss window size of {}", secProbeLossWinSize);
        LOG.info("Using a trajectory latency window size of {}", trajLatWinSize);
        LOG.info("Using a trajectory loss window size of {}", trajLossWinSize);
        LOG.info("Using a packet drop rate window size of {}", pktDropRateWinSize);
        LOG.info("Using a hysteresis threshold factor of {}", hystThresFactor);

        debugger.startUp(context, moduleClass);
    }

    @Override
    public Optional<GeneralLinkStats> getGeneralStats( DatapathLink link )
    {
        synchronized (stateLock) {
            return getGStats(link).map(GStats::freeze);
        }
    }

    @Override
    public List<GeneralLinkStats> getAllGeneralStats()
    {
        synchronized (stateLock) {
            return CollectionUtils.toList(getAllGStats(), GStats::freeze);
        }
    }

    @Override
    public boolean updateVirtualCapacity( DatapathLink link, Timed<InfoDouble> capacity )
        throws IllegalArgumentException
    {
        synchronized (stateLock) {
            Optional<GStats> opGStats = getGStats(link);
            boolean updated = opGStats.filter(gStats -> gStats.updateVirtualCapacity(capacity)).isPresent();
            if (updated) {
                cachedVirCapacities.put(link, capacity);
                onGeneralStatsUpdate(opGStats.get(), "virtual-capacity");
            }

            return updated;
        }
    }

    @Override
    public boolean updatePacketDropRate( NodeId sw, Timed<MetricDouble> pktDropRate )
    {
        synchronized (stateLock) {
            boolean anyUpdated = false;
            for (GStats gStats : getAllGStats()) {
                DatapathLink link = gStats.getLink();

                boolean srcUpdated = false;
                if (link.hasSrcNode(sw) && gStats.updateSourcePacketDropRate(pktDropRate)) {
                    srcUpdated = true;
                }

                boolean destUpdated = false;
                if (link.hasDestNode(sw) && gStats.updateDestinationPacketDropRate(pktDropRate)) {
                    destUpdated = true;
                }

                if (srcUpdated || destUpdated) {
                    anyUpdated = true;
                    if (srcUpdated && destUpdated) {
                        onGeneralStatsUpdate(gStats, "src/dest-packet-drop-rate");
                    }
                    else if (srcUpdated) {
                        onGeneralStatsUpdate(gStats, "src-packet-drop-rate");
                    }
                    else { // if (destUpdated) {
                        onGeneralStatsUpdate(gStats, "dest-packet-drop-rate");
                    }
                }
            }

            return anyUpdated;
        }
    }

    @Override
    public boolean updateGeneralStats( LLDPProbingSample sample )
    {
        synchronized (stateLock) {
            Optional<GStats> opGStats = getGStats(sample.getLink());
            boolean updated = opGStats.filter(gStats -> gStats.update(sample)).isPresent();
            if (updated) {
                onGeneralStatsUpdate(opGStats.get(), "LLDP-probing");
            }

            return updated;
        }
    }

    @Override
    public boolean updateGeneralStats( SecureProbingSample sample )
    {
        synchronized (stateLock) {
            Optional<GStats> opGStats = getGStats(sample.getLink());
            boolean updated = opGStats.filter(gStats -> gStats.update(sample)).isPresent();
            if (updated) {
                onGeneralStatsUpdate(opGStats.get(), "secure-probing");
            }

            return updated;
        }
    }

    @Override
    public Optional<FlowedLinkStats> getFlowedStats( FlowedLink link )
    {
        synchronized (stateLock) {
            return getGStats(link.unflowed()).flatMap(g -> g.getFlowed(link.getFlow()))
                .map(FStats::freeze);
        }
    }

    @Override
    public List<FlowedLinkStats> getAllFlowedStats()
    {
        synchronized (stateLock) {
            return CollectionUtils.toFlatList(
                getAllGStats(),
                gStats -> gStats.getAllFrozenFlowed().stream());
        }
    }

    @Override
    public List<FlowedLinkStats> getAllFlowedStats( DatapathLink link )
    {
        synchronized (stateLock) {
            return getGStats(link)
                .map(GStats::getAllFrozenFlowed)
                .orElseGet(ImmutableList::of);
        }
    }

    @Override
    public List<FlowedLinkStats> getAllFlowedStats( Flow flow )
    {
        synchronized (stateLock) {
            return CollectionUtils.toOptFlatList(
                getAllGStats(),
                gStats -> gStats.getFlowed(flow).map(FStats::freeze));
        }
    }

    @Override
    public boolean updateFlowedStats( SwitchCounterSample sample )
    {
        synchronized (stateLock) {
            FlowedLink link = sample.getLink();
            Optional<GStats> opGStats = getGStats(link.unflowed());
            Optional<FStats> opFStats = opGStats.map(g -> g.computeFlowed(link.getFlow()));
            boolean updated = opFStats.filter(fStats -> fStats.update(sample)).isPresent();
            if (updated) {
                onFlowedStatsUpdate(opFStats.get(), "switch-counter");
            }

            return updated;
        }
    }

    @Override
    public boolean updateFlowedStats( TrajectorySample sample )
    {
        synchronized (stateLock) {
            FlowedLink link = sample.getLink();
            Optional<GStats> opGStats = getGStats(link.unflowed());
            Optional<FStats> opFStats = opGStats.map(g -> g.computeFlowed(link.getFlow()));
            boolean updated = opFStats.filter(fStats -> fStats.update(sample)).isPresent();
            if (updated) {
                onFlowedStatsUpdate(opFStats.get(), "trajectory");
            }

            return updated;
        }
    }

    @Override
    public boolean clearFlowedStats( FlowedLink link )
    {
        synchronized (stateLock) {
            Optional<FStats> opFStats = getGStats(link.unflowed()).flatMap(g -> g.removeFlowed(link.getFlow()));
            boolean cleared = opFStats.isPresent();
            if (cleared) {
                LOG.trace("Flowed stats cleared for flowed-link {}", link);
                FlowedLinkStats fLinkStats = opFStats.get().freeze();
                listeners.forEach(lis -> lis.flowedCleared(fLinkStats));
            }

            return cleared;
        }
    }

    @Override
    public boolean clearAllFlowedStats()
    {
        synchronized (stateLock) {
            boolean anyCleared = false;
            for (GStats gStats : getAllGStats()) {
                List<FlowedLinkStats> fLinkStatsList = gStats.clear();
                if (!fLinkStatsList.isEmpty()) {
                    anyCleared = true;
                    LOG.trace("Flowed stats cleared for flowed-links {}",
                        StreamUtils.toString(fLinkStatsList.stream().map(FlowedLinkStats::getLink)));
                    listeners.forEach(lis -> lis.flowedCleared(fLinkStatsList));
                }
            }

            return anyCleared;
        }
    }

    @Override
    public boolean clearAllFlowedStats( DatapathLink link )
    {
        synchronized (stateLock) {
            boolean anyCleared = false;
            Optional<GStats> opGStats = getGStats(link);
            if (opGStats.isPresent()) {
                List<FlowedLinkStats> fLinkStatsList = opGStats.get().clear();
                if (!fLinkStatsList.isEmpty()) {
                    anyCleared = true;
                    LOG.trace("Flowed stats cleared for flowed-links {}",
                        StreamUtils.toString(fLinkStatsList.stream().map(FlowedLinkStats::getLink)));
                    listeners.forEach(lis -> lis.flowedCleared(fLinkStatsList));
                }
            }

            return anyCleared;
        }
    }

    @Override
    public boolean clearAllFlowedStats( Flow flow )
    {
        synchronized (stateLock) {
            boolean anyCleared = false;
            for (GStats gStats : getAllGStats()) {
                Optional<FStats> opFStats = gStats.removeFlowed(flow);
                if (opFStats.isPresent()) {
                    anyCleared = true;
                    FlowedLinkStats fLinkStats = opFStats.get().freeze();
                    LOG.trace("Flowed stats cleared for flowed-link {}", fLinkStats.getLink());
                    listeners.forEach(lis -> lis.flowedCleared(fLinkStats));
                }
            }

            return anyCleared;
        }
    }

    @Override
    public void addListener( ILinkStatsListener listener )
    {
        if (listeners.add(nonNull(listener))) {
            LOG.trace("Added link statistics listener");
        }
        else {
            LOG.warn("Trying to add an already registered listener");
        }
    }

    @Override
    public void removeListener( ILinkStatsListener listener )
    {
        if (listeners.remove(nonNull(listener))) {
            LOG.trace("Removed link statistics listener");
        }
        else {
            LOG.warn("Trying to remove an unregistered listener");
        }
    }

    private void onGeneralStatsUpdate( GStats stats, String updateType )
    {
        LOG.trace("General stats ({}) updated for link {}", updateType, stats.getLink());
        GeneralLinkStats gLinkStats = stats.freeze();
        ImmutableList<FlowedLinkStats> fLinkStatsList = stats.getAllFrozenFlowed();
        if (fLinkStatsList.isEmpty()) {
            listeners.forEach(lis -> lis.generalUpdated(gLinkStats));
        }
        else {
            if (LOG.isTraceEnabled())
                LOG.trace("Flowed stats updated ({}) for flowed-links {}",
                    updateType, StreamUtils.toString(fLinkStatsList.stream().map(FlowedLinkStats::getLink)));
            listeners.forEach(lis -> {
                lis.generalUpdated(gLinkStats);
                lis.flowedUpdated(fLinkStatsList);
            });
        }
    }

    private void onFlowedStatsUpdate( FStats stats, String updateType )
    {
        LOG.trace("Flowed stats ({}) updated for flowed-link {}", updateType, stats.getLink());
        FlowedLinkStats fLinkStats = stats.freeze();
        listeners.forEach(lis -> lis.flowedUpdated(fLinkStats));
    }

    private final class TopologyListener implements ITopologyGraphListener
    {
        @Override
        public void onLinkEvent( ITopologyLinkEvent event )
        {
            synchronized (stateLock) {
                DatapathLink link = event.getLink();
                switch (event.getType()) {
                    case LINK_ADDED: {
                        GStats gStats = createNewGStats(link);
                        LOG.trace("Created general stats for link {}", link);
                        updatePhysicalCapacity(gStats, event);
                        Timed<InfoDouble> virCapacity = cachedVirCapacities.get(link);
                        if (virCapacity != null)
                            updateVirtualCapacity(link, virCapacity);
                    }
                    break;

                    case LINK_UPDATED: {
                        GStats gStats = getExistingGStats(link);
                        updatePhysicalCapacity(gStats, event);
                    }
                    break;

                    case LINK_REMOVED: {
                        GStats gStats = removeExistingGStats(link);
                        LOG.trace("Removed general stats for link {}", link);
                        onRemovedGeneralStats(gStats);
                    }
                    break;

                    default:
                        throw new AssertionError("unexpected enum value");
                }
            }
        }

        // NOTE: Call only when holding stateLock
        private void updatePhysicalCapacity( GStats gStats, ITopologyLinkEvent event )
        {
            if (gStats.updatePhysicalCapacity(getPhyCap(event))) {
                onGeneralStatsUpdate(gStats, "physical-capacity");
            }
        }

        private Timed<InfoDouble> getPhyCap( ITopologyLinkEvent event )
        {
            DatapathLink link = event.getLink();

            InfoDouble capacity = InfoDouble.absent();
            Optional<IOFSwitch> srcSw = event.getSrcIOFSwitch();
            if (srcSw.isPresent()) {
                OFPortDesc portDesc = srcSw.get().getPort(link.getSrcPort().getOFPort());
                long capKilobits = OFMessageUtils.getCurrSpeedFromPortDesc(portDesc);
                capacity = InfoLong.of(capKilobits, InfoLongUnit.KILOBITS).asDouble();
            }

            return Timed.now(capacity);
        }

        // NOTE: Call only when holding stateLock
        private void onRemovedGeneralStats( GStats gStats )
        {
            GeneralLinkStats gLinkStats = gStats.freeze();
            ImmutableList<FlowedLinkStats> fLinkStatsList = gStats.clear();
            if (fLinkStatsList.isEmpty()) {
                listeners.forEach(lis -> lis.generalCleared(gLinkStats));
            }
            else {
                if (LOG.isTraceEnabled())
                    LOG.trace("Flowed stats cleared for flowed-links {}",
                        StreamUtils.toString(fLinkStatsList.stream().map(FlowedLinkStats::getLink)));
                listeners.forEach(lis -> {
                    lis.generalCleared(gLinkStats);
                    lis.flowedCleared(fLinkStatsList);
                });
            }
        }

        @Override
        public void onNodeEvent( ITopologyNodeEvent event )
        {/* not used */}

        @Override
        public void onPortEvent( ITopologyPortEvent event )
        {/* not used */}
    }

    // =========== Begin auxiliary methods for EXTERNAL methods ============ //

    private static <T> T nonNull( T obj )
    {
        return Objects.requireNonNull(obj);
    }

    private Optional<GStats> getGStats( DatapathLink dLink )
    {
        return Optional.ofNullable(genStatsMap.get(nonNull(dLink)));
    }

    private Collection<GStats> getAllGStats()
    {
        return genStatsMap.values();
    }

    // ============ End auxiliary methods for EXTERNAL methods ============= //

    // =========== Begin auxiliary methods for INTERNAL methods ============ //

    // assuming here that dLink is never null
    private GStats getExistingGStats( DatapathLink dLink )
    {
        GStats existing = genStatsMap.get(dLink);
        Preconditions.checkState(existing != null, "inconsistent GStats map state");
        return existing;
    }

    // assuming here that dLink is never null
    private GStats createNewGStats( DatapathLink dLink )
    {
        GStats newStats = GStats.newStats(dLink,
            lldpLatWinSize,
            secProbeLatWinSize, secProbeLossWinSize,
            trajLatWinSize, trajLossWinSize,
            pktDropRateWinSize,
            hystThresFactor);

        GStats expectNull = genStatsMap.putIfAbsent(dLink, newStats);
        Preconditions.checkState(expectNull == null, "inconsistent GStats map state");
        return newStats;
    }

    // assuming here that dLink is never null
    private GStats removeExistingGStats( DatapathLink dLink )
    {
        GStats existing = genStatsMap.remove(dLink);
        Preconditions.checkArgument(existing != null, "inconsistent GStats map state");
        return existing;
    }

    // ============= End auxiliary methods for INTERNAL methods ============= //
}
