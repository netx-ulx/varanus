package net.varanus.sdncontroller.monitoring.submodules.switches.internal;


import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowStatsEntry;
import org.projectfloodlight.openflow.protocol.OFFlowStatsReply;
import org.projectfloodlight.openflow.protocol.OFFlowStatsRequest;
import org.projectfloodlight.openflow.types.TableId;
import org.projectfloodlight.openflow.types.U64;
import org.slf4j.Logger;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.varanus.sdncontroller.linkstats.ILinkStatsService;
import net.varanus.sdncontroller.logging.Logging;
import net.varanus.sdncontroller.monitoring.internal.IMonitoringModuleContext;
import net.varanus.sdncontroller.monitoring.util.ISubmoduleManager;
import net.varanus.sdncontroller.util.DurationRange;
import net.varanus.sdncontroller.util.Fields;
import net.varanus.sdncontroller.util.module.ModuleUtils;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.collect.CollectionUtils;
import net.varanus.util.collect.Cycler;
import net.varanus.util.concurrent.ConcurrencyUtils;
import net.varanus.util.concurrent.ConcurrentService;
import net.varanus.util.openflow.types.Flow;
import net.varanus.util.openflow.types.NodeId;
import net.varanus.util.time.TimeDouble;
import net.varanus.util.time.TimeDoubleUnit;
import net.varanus.util.time.TimeLong;
import net.varanus.util.time.Timed;
import net.varanus.util.unitvalue.si.MetricDouble;
import net.varanus.util.unitvalue.si.MetricLong;


/**
 *
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
final class SwitchMonitor extends ConcurrentService implements ISubmoduleManager
{
    private static final TimeLong IDLE_SLEEP_TIME = TimeLong.of(1, TimeUnit.SECONDS);

    private static final Logger LOG = Logging.monitoring.switches.LOG;

    private final MonitorState state;
    private final Object       stateLock;

    private @Nullable DurationRange monitWaitPeriodRange;

    SwitchMonitor()
    {
        super(ConcurrencyUtils.defaultDaemonThreadFactory(), ( msg, ex ) -> LOG.error(msg, ex));
        this.state = new MonitorState();
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
        this.monitWaitPeriodRange = Props.getMonitoringWaitPeriodRange(params);

        state.init(context);
    }

    @Override
    public void startUp( IMonitoringModuleContext context ) throws FloodlightModuleException
    {
        LOG.info("Using a switch monitoring round wait period range of {}", monitWaitPeriodRange);

        state.startUp(context);
        start();
    }

    @Override
    protected void startUp()
    {
        LOG.debug("Starting up switch monitor");
    }

    @Override
    protected void shutDown()
    {
        LOG.debug("Shutting down switch monitor");
    }

    void onAddedNode( NodeId swId )
    {
        synchronized (stateLock) {
            state.addSwitch(swId);
        }
    }

    void onRemovedNode( NodeId swId )
    {
        synchronized (stateLock) {
            state.removeSwitch(swId);
        }
    }

    @Override
    public void runInterruptibly() throws InterruptedException
    {
        try {
            while (true) {
                // ===========================================================
                // Prepare the next monitoring, or sleep if no monitoring is
                // scheduled
                // ===========================================================
                Optional<Monitoring> opMonitoring = nextMonitoring();
                if (!opMonitoring.isPresent()) {
                    LOG.trace("No switch monitoring is scheduled; sleeping for {} ...", IDLE_SLEEP_TIME);
                    IDLE_SLEEP_TIME.sleep();
                    continue;
                }

                Monitoring monitoring = opMonitoring.get();
                LOG.debug("");
                LOG.debug("===== Running a new switch monitoring round =====");
                LOG.debug("Monitoring the following switches: {}", monitoring.switches());

                // ===========================================================
                // Wait for a random duration before starting the monitoring
                // ===========================================================
                TimeLong waitPeriod = monitWaitPeriodRange.getRandomDuration();
                LOG.debug("Waiting {} before starting the monitoring...", waitPeriod);
                waitPeriod.sleep();

                // ===========================================================
                // Send monitoring requests
                // ===========================================================
                LOG.debug("Sending monitoring requests...");
                monitoring.execute();

                // ===========================================================
                // Wait until all monitoring requests are complete
                // ===========================================================
                LOG.debug("Waiting until all monitoring requests complete...");
                monitoring.get();
            }
        }
        catch (InterruptedException e) {
            LOG.debug("Switch monitor was interrupted, now exiting");
            throw e;
        }
        catch (ExecutionException e) {
            Throwable cause = e.getCause();
            LOG.error(cause.getMessage(), cause);
        }
    }

    private Optional<Monitoring> nextMonitoring()
    {
        synchronized (stateLock) {
            state.setNextActiveMonitoring();
            return state.getActiveMonitoring();
        }
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    private static final class MonitorState implements ISubmoduleManager
    {
        private final List<SwitchAggregate>        aggregates;
        private final Cycler<SwitchAggregate>      aggregateCycler;
        private final Map<NodeId, SwitchAggregate> aggregatesBySwitch;

        private Optional<Monitoring> activeMonitoring;

        private int                         maxSimultMonitorings;
        private @Nullable IOFSwitchService  switchService;
        private @Nullable ILinkStatsService linkStatsService;

        MonitorState()
        {
            this.aggregates = new ArrayList<>();
            this.aggregateCycler = new Cycler<>();
            this.aggregatesBySwitch = new HashMap<>();

            this.activeMonitoring = Optional.empty();
        }

        @Override
        public Collection<Class<? extends IFloodlightService>> getModuleDependencies()
        {
            return ModuleUtils.services(
                IOFSwitchService.class,
                ILinkStatsService.class);
        }

        @Override
        public void init( IMonitoringModuleContext context ) throws FloodlightModuleException
        {
            Map<String, String> params = context.getConfigParams();
            this.maxSimultMonitorings = Props.getMaxSimultaneousMonitorings(params);

            this.switchService = context.getServiceImpl(IOFSwitchService.class);
            this.linkStatsService = context.getServiceImpl(ILinkStatsService.class);
        }

        @Override
        public void startUp( IMonitoringModuleContext context ) throws FloodlightModuleException
        {
            LOG.info("Using a maximum of {} simultaneous switch monitorings", maxSimultMonitorings);
        }

        Optional<Monitoring> getActiveMonitoring()
        {
            return activeMonitoring;
        }

        void setNextActiveMonitoring()
        {
            if (aggregateCycler.hasNext()) {
                this.activeMonitoring =
                    Optional.of(aggregateCycler.next().newMonitoring(switchService, linkStatsService));
            }
            else {
                this.activeMonitoring = Optional.empty();
            }
        }

        void addSwitch( NodeId swId )
        {
            LOG.debug("Adding switch to be monitored: {}", swId);
            // first try to add to an existing set of links
            boolean added = false;
            for (SwitchAggregate aggr : aggregates) {
                if (aggr.tryAddingSwitch(swId)) {
                    aggregatesBySwitch.put(swId, aggr);
                    added = true;
                    break;
                }
            }
            if (!added) {
                // if the previous failed, create a new aggregate with the new
                // switch
                SwitchAggregate newAggr = new SwitchAggregate(swId, maxSimultMonitorings);
                aggregatesBySwitch.put(swId, newAggr);
                aggregates.add(newAggr);
                resetAggregateCycler();
            }
            LOG.trace("Current aggregates: {}", aggregates);
        }

        void removeSwitch( NodeId swId )
        {
            LOG.debug("Removing switch from being monitored: {}", swId);
            SwitchAggregate aggr = aggregatesBySwitch.remove(swId);
            aggr.removeSwitch(swId);
            if (aggr.isEmpty()) {
                aggregates.remove(aggr);
                resetAggregateCycler();
            }
            LOG.trace("Current aggregates: {}", aggregates);

            getActiveMonitoring().ifPresent(( monitoring ) -> {
                monitoring.getUnit(swId).ifPresent(( unit ) -> {
                    LOG.debug("Aborting active monitoring for switch {}", swId);
                    unit.abort();
                });
            });
        }

        private void resetAggregateCycler()
        {
            aggregateCycler.reset(aggregates);
        }
    }

    /**
     * Aggregates multiple switches that can be simultaneously monitored.
     */
    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    private static final class SwitchAggregate
    {
        private final Map<NodeId, SwitchState> switches;
        private final int                      maxSimultMonitorings;

        SwitchAggregate( NodeId firstSwId, int maxSimultMonitorings )
        {
            Preconditions.checkArgument(maxSimultMonitorings > 0);
            this.switches = new LinkedHashMap<>();
            switches.put(firstSwId, new SwitchState(firstSwId));
            this.maxSimultMonitorings = maxSimultMonitorings;
        }

        Monitoring newMonitoring( IOFSwitchService switchService, ILinkStatsService linkStatsService )
        {
            return new Monitoring(ImmutableSet.copyOf(switches.values()), switchService, linkStatsService);
        }

        boolean isEmpty()
        {
            return switches.isEmpty();
        }

        // requires inexistent switch within this aggregate
        boolean tryAddingSwitch( NodeId newSwId )
        {
            Preconditions.checkArgument(!switches.containsKey(newSwId), "expected inexistent switch");

            if (switches.size() == maxSimultMonitorings)
                return false;

            switches.put(newSwId, new SwitchState(newSwId));
            return true;
        }

        // requires existent switch within this aggregate
        void removeSwitch( NodeId swId )
        {
            boolean removed = null != switches.remove(swId);
            Preconditions.checkArgument(removed, "expected existent switch");
        }

        @Override
        public String toString()
        {
            return String.format("Aggregate(%d/%d)%s", switches.size(), maxSimultMonitorings, switches);
        }
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    private static final class Monitoring extends CompletableFuture<Void>
    {
        private final Map<NodeId, MonitoringUnit> units;

        Monitoring( ImmutableSet<SwitchState> switches,
                    IOFSwitchService switchService,
                    ILinkStatsService linkStatsService )
        {
            this.units = Collections.unmodifiableMap(CollectionUtils.toMap(switches,
                SwitchState::getSwitchId,
                ( state ) -> {
                    NodeId swId = state.getSwitchId();
                    MonitoringUnit unit = new MonitoringUnit(state, switchService);
                    unit.thenAccept(( pktDropRate ) -> {
                        linkStatsService.updatePacketDropRate(swId, pktDropRate);
                    });
                    return unit;
                }));
        }

        Set<NodeId> switches()
        {
            return units.keySet();
        }

        Optional<MonitoringUnit> getUnit( NodeId swId )
        {
            return Optional.ofNullable(units.get(swId));
        }

        void execute()
        {
            for (MonitoringUnit unit : units.values()) {
                unit.sendRequest();
            }

            MonitoringUnit[] unitsArray = CollectionUtils.toArray(units.values(), MonitoringUnit.class);
            CompletableFuture.allOf(unitsArray).whenComplete(( _void, ex ) -> {
                if (ex == null)
                    Monitoring.this.complete(null);
                else
                    Monitoring.this.completeExceptionally(ex);
            });
        }
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    private static final class MonitoringUnit extends CompletableFuture<Timed<MetricDouble>>
    {
        private final SwitchState      swState;
        private final IOFSwitchService switchService;

        MonitoringUnit( SwitchState swState, IOFSwitchService switchService )
        {
            this.swState = swState;
            this.switchService = switchService;
        }

        void sendRequest()
        {
            IOFSwitch sw = switchService.getActiveSwitch(swState.getSwitchId().getDpid());
            if (sw != null) {
                OFFactory fact = sw.getOFFactory();
                OFFlowStatsRequest req = fact.buildFlowStatsRequest()
                    .setTableId(TableId.of(1)) // FIXME this presumes sampling
                    .setMatch(fact.matchWildcardAll())
                    .build();

                ConcurrencyUtils.toCompletableFuture(sw.writeStatsRequest(req))
                    .whenComplete(( replyList, ex ) -> {
                        if (ex == null)
                            MonitoringUnit.this.complete(calcPps(replyList, swState));
                        else
                            MonitoringUnit.this.completeExceptionally(ex);
                    });
            }
            else {
                LOG.warn("Failed to send monitoring request due to lack of active switch {}", swState.getSwitchId());
                complete(Timed.now(MetricDouble.absent()));
            }
        }

        void abort()
        {
            complete(Timed.now(MetricDouble.absent()));
        }

        private static Timed<MetricDouble> calcPps( List<OFFlowStatsReply> replyList, SwitchState swState )
        {
            Instant timestamp = Instant.now();

            for (OFFlowStatsReply reply : replyList) {
                for (OFFlowStatsEntry entry : reply.getEntries()) {
                    if (Flow.of(entry.getMatch()).isFullyWildcarded() && entry.getInstructions().isEmpty()) {
                        // the drop rule entry
                        MetricDouble pps = swState.calculatePacketsPerSecond(entry);
                        return Timed.of(pps, timestamp);
                    }
                }
            }

            return Timed.of(MetricDouble.absent(), timestamp);
        }
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    private static final class SwitchState
    {
        private final NodeId swId;

        private @Nullable OFFlowStatsEntry prevEntry;

        SwitchState( NodeId swId )
        {
            this.swId = swId;
            this.prevEntry = null;
        }

        NodeId getSwitchId()
        {
            return swId;
        }

        MetricDouble calculatePacketsPerSecond( OFFlowStatsEntry newEntry )
        {
            OFFlowStatsEntry prevEntry = this.prevEntry;
            this.prevEntry = newEntry;

            if (prevEntry == null) {
                return MetricDouble.absent();
            }
            else {
                Duration prevDur = Duration.ofSeconds(prevEntry.getDurationSec(), prevEntry.getDurationNsec());
                Duration newDur = Duration.ofSeconds(newEntry.getDurationSec(), newEntry.getDurationNsec());

                Duration elapsed = newDur.minus(prevDur);
                if (elapsed.isNegative() || elapsed.isZero()) {
                    return MetricDouble.absent();
                }
                else {
                    MetricLong prevPktCount = getPacketCount(prevEntry, swId);
                    MetricLong newPktCount = getPacketCount(newEntry, swId);

                    MetricLong packets = newPktCount.posDiff(prevPktCount);
                    TimeDouble time = TimeDouble.fromDuration(elapsed);
                    return packets.asDouble().divideBy(time.in(TimeDoubleUnit.SECONDS));
                }
            }
        }

        private static MetricLong getPacketCount( OFFlowStatsEntry entry, NodeId nodeId )
        {
            final U64 count = entry.getPacketCount();
            if (count.equals(Fields.MAX_U64)) {
                LOG.trace("!!! flow packet count is not supported in switch {} !!!", nodeId);
                return MetricLong.absent();
            }
            else {
                return MetricLong.ofUnits(Fields.getSaturatedLong(count));
            }
        }
    }
}
