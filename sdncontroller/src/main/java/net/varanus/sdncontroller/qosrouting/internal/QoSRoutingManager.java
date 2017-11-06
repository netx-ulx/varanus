package net.varanus.sdncontroller.qosrouting.internal;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.jgrapht.graph.DirectedPseudograph;
import org.slf4j.Logger;

import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.varanus.sdncontroller.linkstats.FlowedLinkStats;
import net.varanus.sdncontroller.linkstats.GeneralLinkStats;
import net.varanus.sdncontroller.linkstats.ILinkStatsListener;
import net.varanus.sdncontroller.linkstats.ILinkStatsService;
import net.varanus.sdncontroller.logging.Logging;
import net.varanus.sdncontroller.monitoring.IMonitoringService;
import net.varanus.sdncontroller.qosrouting.IFlowedConnectionMap;
import net.varanus.sdncontroller.qosrouting.IQoSRoutingListener;
import net.varanus.sdncontroller.qosrouting.IQoSRoutingService;
import net.varanus.sdncontroller.topologygraph.ITopologyGraphListener;
import net.varanus.sdncontroller.topologygraph.ITopologyGraphService;
import net.varanus.sdncontroller.topologygraph.ITopologyGraphService.TopologySnapshot;
import net.varanus.sdncontroller.topologygraph.event.ITopologyLinkEvent;
import net.varanus.sdncontroller.topologygraph.event.ITopologyNodeEvent;
import net.varanus.sdncontroller.topologygraph.event.ITopologyPortEvent;
import net.varanus.sdncontroller.types.DatapathConnection;
import net.varanus.sdncontroller.types.DatapathLink;
import net.varanus.sdncontroller.types.FlowedConnection;
import net.varanus.sdncontroller.types.FlowedLink;
import net.varanus.sdncontroller.util.module.IModuleManager;
import net.varanus.sdncontroller.util.module.ModuleUtils;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.collect.CollectionUtils;
import net.varanus.util.openflow.types.Flow;
import net.varanus.util.openflow.types.NodeId;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
public final class QoSRoutingManager implements IModuleManager, IQoSRoutingService
{
    private static final Logger LOG = Logging.qosrouting.LOG;

    private final TopologyListener  topoListener;
    private final FlowedLinkMonitor flowedLinkMonitor;

    private final Map<Flow, TopologyHandler> topoHandlers;
    private final Object                     writeLock;

    private final Set<IQoSRoutingListener> listeners;

    private final Debugger debugger;

    private @Nullable ITopologyGraphService topoService;
    private @Nullable IMonitoringService    monitService;

    public QoSRoutingManager()
    {
        this.topoListener = new TopologyListener();
        this.flowedLinkMonitor = new FlowedLinkMonitor();

        this.topoHandlers = new HashMap<>();
        this.writeLock = new Object();

        this.listeners = ModuleUtils.newListenerSet();

        this.debugger = new Debugger();
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies()
    {
        return ModuleUtils.services(
            debugger.getModuleDependencies(),
            ITopologyGraphService.class,
            ILinkStatsService.class);
    }

    @Override
    public void init( FloodlightModuleContext context, Class<? extends IFloodlightModule> moduleClass )
        throws FloodlightModuleException
    {
        this.topoService = ModuleUtils.getServiceImpl(context, ITopologyGraphService.class);
        this.monitService = ModuleUtils.getServiceImpl(context, IMonitoringService.class);

        topoService.addListener(topoListener);
        ModuleUtils.getServiceImpl(context, ILinkStatsService.class).addListener(flowedLinkMonitor);

        debugger.init(context, moduleClass);
    }

    @Override
    public void startUp( FloodlightModuleContext context, Class<? extends IFloodlightModule> moduleClass )
        throws FloodlightModuleException
    {
        debugger.startUp(context, moduleClass);
    }

    @Override
    public Optional<IFlowedConnectionMap> getConnectionMap( FlowedConnection conn )
    {
        synchronized (writeLock) {
            return Optional.ofNullable(topoHandlers.get(conn.getFlow()))
                .flatMap(handler -> handler.getConnectionMap(conn.unflowed()));
        }
    }

    @Override
    public List<IFlowedConnectionMap> getAllConnectionMaps()
    {
        synchronized (writeLock) {
            return CollectionUtils.toFlatList(topoHandlers.values(),
                handler -> handler.getAllConnectionMaps().stream());
        }
    }

    @Override
    public List<IFlowedConnectionMap> getAllConnectionMaps( Flow flow )
    {
        synchronized (writeLock) {
            return Optional.ofNullable(topoHandlers.get(Objects.requireNonNull(flow)))
                .map(handler -> new ArrayList<>(handler.getAllConnectionMaps()))
                .orElseGet(ArrayList::new);
        }
    }

    @Override
    public boolean isValidRegistrableConnection( FlowedConnection conn )
    {
        return monitService.isValidMonitorableFlow(conn.getFlow());
    }

    @Override
    public void validateRegistrableConnection( FlowedConnection conn ) throws IllegalArgumentException
    {
        try {
            monitService.validateMonitorableFlow(conn.getFlow());
        }
        catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                String.format("invalid registrable connection: %s", e.getMessage()));
        }
    }

    @Override
    public boolean registerConnection( FlowedConnection conn, boolean startFlowMonitoring )
        throws IllegalArgumentException
    {
        validateRegistrableConnection(conn);

        synchronized (writeLock) {
            Flow flow = conn.getFlow();
            if (!topoHandlers.containsKey(flow) && startFlowMonitoring)
                monitService.startMonitoring(flow);

            TopologyHandler handler = topoHandlers.computeIfAbsent(
                flow,
                flo -> new TopologyHandler(flo, topoService.getSnapshot().getGraph()));

            return handler.registerConnection(conn.unflowed());
        }
    }

    @Override
    public boolean unregisterConnection( FlowedConnection conn, boolean stopFlowMonitoring )
        throws IllegalArgumentException
    {
        validateRegistrableConnection(conn);

        synchronized (writeLock) {
            Flow flow = conn.getFlow();
            TopologyHandler handler = topoHandlers.get(flow);
            if (handler != null) {
                boolean unregistered = handler.unregisterConnection(conn.unflowed());
                if (!handler.hasRegisteredConnections()) {
                    topoHandlers.remove(flow);
                    if (stopFlowMonitoring)
                        monitService.stopMonitoring(flow);
                }

                return unregistered;
            }
            else {
                return false;
            }
        }
    }

    @Override
    public void addListener( IQoSRoutingListener listener )
    {
        listeners.add(Objects.requireNonNull(listener));
    }

    @Override
    public void removeListener( IQoSRoutingListener listener )
    {
        listeners.remove(Objects.requireNonNull(listener));
    }

    private final class TopologyListener implements ITopologyGraphListener
    {
        @Override
        public void onNodeEvent( ITopologyNodeEvent event )
        {
            synchronized (writeLock) {
                switch (event.getType()) {
                    case NODE_ADDED:
                    case NODE_REMOVED:
                        TopologySnapshot snap = topoService.getSnapshot();
                        for (TopologyHandler handler : topoHandlers.values()) {
                            handler.updateTopology(snap.getGraph());
                        }
                    break;

                    case NODE_ACTIVATED:
                    case NODE_UPDATED:
                    case NODE_DEACTIVATED:
                    // ignore
                    break;

                    default:
                        throw new AssertionError("unexpected enum value");
                }
            }
        }

        @Override
        public void onPortEvent( ITopologyPortEvent event )
        {/* not used */}

        @Override
        public void onLinkEvent( ITopologyLinkEvent event )
        {
            synchronized (writeLock) {
                switch (event.getType()) {
                    case LINK_ADDED:
                    case LINK_REMOVED:
                        TopologySnapshot snap = topoService.getSnapshot();
                        for (TopologyHandler handler : topoHandlers.values()) {
                            handler.updateTopology(snap.getGraph());
                        }
                    break;

                    case LINK_UPDATED:
                    // ignore
                    break;

                    default:
                        throw new AssertionError("unexpected enum value");
                }
            }
        }
    }

    private final class FlowedLinkMonitor implements ILinkStatsListener
    {
        @Override
        public void flowedUpdated( FlowedLinkStats stats )
        {
            synchronized (writeLock) {
                FlowedLink link = stats.getLink();
                TopologyHandler handler = topoHandlers.get(link.getFlow());
                if (handler != null) {
                    handler.updateLinkStatistics(link.unflowed(), stats);
                }
            }
        }

        @Override
        public void flowedCleared( FlowedLinkStats last )
        { /* not used */ }

        @Override
        public void generalUpdated( GeneralLinkStats stats )
        { /* not used */ }

        @Override
        public void generalCleared( GeneralLinkStats last )
        { /* not used */ }
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    private final class TopologyHandler
    {
        private final FlowedTopology                                flowedTopo;
        private final Map<DatapathConnection, IFlowedConnectionMap> connMaps;

        TopologyHandler( Flow flow, DirectedPseudograph<NodeId, DatapathLink> topo )
        {
            this.flowedTopo = new FlowedTopology(flow, topo);
            this.connMaps = new HashMap<>();
        }

        Optional<IFlowedConnectionMap> getConnectionMap( DatapathConnection conn )
        {
            return Optional.ofNullable(connMaps.get(conn));
        }

        Collection<IFlowedConnectionMap> getAllConnectionMaps()
        {
            return connMaps.values();
        }

        void updateTopology( DirectedPseudograph<NodeId, DatapathLink> topo )
        {
            flowedTopo.updateTopology(topo);
            updateConnMaps();
        }

        void updateLinkStatistics( DatapathLink link, FlowedLinkStats stats )
        {
            LOG.trace("Statistics updated for flowed-link {}: {}", flowedLink(link), stats);
            flowedTopo.updateLinkStatistics(link, stats);
            updateConnMaps();
        }

        private void updateConnMaps()
        {
            connMaps.replaceAll(( conn, _oldMap ) -> {
                IFlowedConnectionMap newMap = flowedTopo.getConnectionMap(conn);
                LOG.trace("Connection map updated for flowed-connection {}", flowedConn(conn));

                notifyConnMapUpdated(newMap);
                return newMap;
            });
        }

        private void notifyConnMapUpdated( IFlowedConnectionMap map )
        {
            for (IQoSRoutingListener lis : listeners) {
                lis.connectionMapUpdated(map);
            }
        }

        boolean hasRegisteredConnections()
        {
            return !connMaps.isEmpty();
        }

        boolean registerConnection( DatapathConnection conn )
        {
            if (null == connMaps.putIfAbsent(conn, flowedTopo.getConnectionMap(conn))) {
                notifyConnRegistered(conn);
                return true;
            }
            else {
                return false;
            }
        }

        boolean unregisterConnection( DatapathConnection conn )
        {
            if (null != connMaps.remove(conn)) {
                notifyConnUnregistered(conn);
                return true;
            }
            else {
                return false;
            }
        }

        private void notifyConnRegistered( DatapathConnection conn )
        {
            FlowedConnection flowedConn = flowedConn(conn);
            for (IQoSRoutingListener lis : listeners) {
                lis.connectionRegistered(flowedConn);
            }
        }

        private void notifyConnUnregistered( DatapathConnection conn )
        {
            FlowedConnection flowedConn = flowedConn(conn);
            for (IQoSRoutingListener lis : listeners) {
                lis.connectionUnregistered(flowedConn);
            }
        }

        private FlowedConnection flowedConn( DatapathConnection conn )
        {
            return conn.flowed(flowedTopo.getFlow());
        }

        private FlowedLink flowedLink( DatapathLink link )
        {
            return link.flowed(flowedTopo.getFlow());
        }
    }
}
