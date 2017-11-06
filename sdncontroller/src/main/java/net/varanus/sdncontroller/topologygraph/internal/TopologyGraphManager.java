package net.varanus.sdncontroller.topologygraph.internal;


import static net.varanus.util.openflow.OFMessageUtils.eternalFlow;
import static net.varanus.util.openflow.OFMessageUtils.withNoOverlap;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.jgrapht.graph.DirectedPseudograph;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFFlowDelete;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TableId;
import org.slf4j.Logger;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IOFSwitchListener;
import net.floodlightcontroller.core.PortChangeType;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.types.NodePortTuple;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryListener;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryService;
import net.floodlightcontroller.linkdiscovery.internal.LinkInfo;
import net.varanus.sdncontroller.alias.IAliasService;
import net.varanus.sdncontroller.logging.Logging;
import net.varanus.sdncontroller.topologygraph.ITopologyGraphListener;
import net.varanus.sdncontroller.topologygraph.ITopologyGraphService;
import net.varanus.sdncontroller.topologygraph.event.ITopologyLinkEvent;
import net.varanus.sdncontroller.topologygraph.event.ITopologyNodeEvent;
import net.varanus.sdncontroller.topologygraph.event.ITopologyPortEvent;
import net.varanus.sdncontroller.types.DatapathLink;
import net.varanus.sdncontroller.util.Fields;
import net.varanus.sdncontroller.util.LinkUtils;
import net.varanus.sdncontroller.util.module.IModuleManager;
import net.varanus.sdncontroller.util.module.ModuleUtils;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.lang.Unsigned;
import net.varanus.util.openflow.types.BidiNodePorts;
import net.varanus.util.openflow.types.Flow;
import net.varanus.util.openflow.types.MatchEntry;
import net.varanus.util.openflow.types.NodeId;
import net.varanus.util.openflow.types.NodePort;
import net.varanus.util.openflow.types.PortId;


/**
 * 
 */
@FieldsAreNonnullByDefault
public final class TopologyGraphManager implements IModuleManager, ITopologyGraphService
{
    private static final Logger LOG = Logging.topologygraph.LOG;

    private static final Flow LLDP_FLOW = Flow.of(MatchEntry.ofExact(MatchField.ETH_TYPE, EthType.LLDP));
    private static final Flow BSN_FLOW  = Flow.of(MatchEntry.ofExact(MatchField.ETH_TYPE, Fields.BSN_ETHER_TYPE));

    private final NetworkListener netListener;

    private final DatapathTopology   topology;
    private final Set<BidiNodePorts> disabledBidiLinks;
    private final Set<PortId>        supprLinkDiscPorts;
    private final Object             topologyLock;

    private final Set<ITopologyGraphListener> graphListeners;

    private @Nullable ILinkDiscoveryService linkDiscService;

    public TopologyGraphManager()
    {
        this.netListener = new NetworkListener();

        this.topology = new DatapathTopology();
        this.disabledBidiLinks = new LinkedHashSet<>();
        this.supprLinkDiscPorts = new LinkedHashSet<>();
        this.topologyLock = new Object();

        this.graphListeners = ModuleUtils.newListenerSet();
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies()
    {
        return ModuleUtils.services(
            IOFSwitchService.class,
            ILinkDiscoveryService.class,
            IAliasService.class);
    }

    @Override
    public void init( FloodlightModuleContext context, Class<? extends IFloodlightModule> moduleClass )
        throws FloodlightModuleException
    {
        this.linkDiscService = ModuleUtils.getServiceImpl(context, ILinkDiscoveryService.class);
        netListener.init(context);
    }

    @Override
    public void startUp( FloodlightModuleContext context, Class<? extends IFloodlightModule> moduleClass )
        throws FloodlightModuleException
    { /* do nothing */ }

    @Override
    public TopologySnapshot getSnapshot()
    {
        synchronized (topologyLock) {
            DirectedPseudograph<NodeId, DatapathLink> graph = topology.getSnapshot();
            ImmutableSet<BidiNodePorts> disabled = ImmutableSet.copyOf(disabledBidiLinks);
            return TopologySnapshot.of(graph, disabled);
        }
    }

    @Override
    public boolean enableBidiLink( BidiNodePorts bidiLink ) throws IllegalStateException
    {
        Objects.requireNonNull(bidiLink);
        synchronized (topologyLock) {
            Preconditions.checkState(!linkContainsGloballySuppressedPort(bidiLink),
                "cannot enable a link containing a port that has link discovery suppressed globally");

            if (disabledBidiLinks.remove(bidiLink)) {
                NodePortTuple firstNPT = getFirstNPT(bidiLink);
                NodePortTuple secondNPT = getSecondNPT(bidiLink);
                linkDiscService.RemoveFromSuppressLLDPs(firstNPT.getNodeId(), firstNPT.getPortId());
                linkDiscService.RemoveFromSuppressLLDPs(secondNPT.getNodeId(), secondNPT.getPortId());
                return true;
            }
            else {
                return false;
            }
        }
    }

    // NOTE: call only when holding topologyLock
    private boolean linkContainsGloballySuppressedPort( BidiNodePorts bidiLink )
    {
        return supprLinkDiscPorts.contains(bidiLink.getMin().getPortId())
               || supprLinkDiscPorts.contains(bidiLink.getMax().getPortId());
    }

    @Override
    public boolean disableBidiLink( BidiNodePorts bidiLink ) throws IllegalStateException
    {
        Objects.requireNonNull(bidiLink);
        synchronized (topologyLock) {
            if (disabledBidiLinks.add(bidiLink)) {
                NodePortTuple firstNPT = getFirstNPT(bidiLink);
                NodePortTuple secondNPT = getSecondNPT(bidiLink);
                linkDiscService.AddToSuppressLLDPs(firstNPT.getNodeId(), firstNPT.getPortId());
                linkDiscService.AddToSuppressLLDPs(secondNPT.getNodeId(), secondNPT.getPortId());
                return true;
            }
            else {
                return false;
            }
        }
    }

    private static NodePortTuple getFirstNPT( BidiNodePorts bidiLink )
    {
        return new NodePortTuple(
            bidiLink.getMin().getNodeId().getDpid(),
            bidiLink.getMin().getPortId().getOFPort());
    }

    private static NodePortTuple getSecondNPT( BidiNodePorts bidiLink )
    {
        return new NodePortTuple(
            bidiLink.getMax().getNodeId().getDpid(),
            bidiLink.getMax().getPortId().getOFPort());
    }

    @Override
    public void suppressLinkDiscovery( PortId portId )
    {
        synchronized (topologyLock) {
            if (supprLinkDiscPorts.add(Objects.requireNonNull(portId))) {
                LOG.info("Suppressing link discovery globally on port {}", portId);

                for (NodeId nodeId : topology.getSnapshot().vertexSet()) {
                    LOG.trace("Suppressing link discovery on switch {} for port {}", nodeId, portId);
                    linkDiscService.AddToSuppressLLDPs(nodeId.getDpid(), portId.getOFPort());
                }
            }
        }
    }

    @Override
    public void addListener( ITopologyGraphListener listener )
    {
        graphListeners.add(listener);
    }

    @Override
    public void removeListener( ITopologyGraphListener listener )
    {
        graphListeners.remove(listener);
    }

    @ParametersAreNonnullByDefault
    private final class NetworkListener implements IOFSwitchListener, ILinkDiscoveryListener
    {
        private @Nullable IAliasService    aliasService;
        private @Nullable IOFSwitchService switchService;

        void init( FloodlightModuleContext context )
        {
            this.aliasService = ModuleUtils.getServiceImpl(context, IAliasService.class);
            this.switchService = ModuleUtils.getServiceImpl(context, IOFSwitchService.class);

            switchService.addOFSwitchListener(this);
            linkDiscService.addListener(this);
        }

        // FIXME Apparently, it is possible for a link discovery update for a
        // new link to fire before the switch-added events for both source and
        // destination switches fire. This happens rarely, but it still happens,
        // which breaks some topology listeners that expect to have both source
        // and destination IOFSwitches available on link addition events.
        //
        // Fixing this is not trivial. For now keep it as it is and restart
        // floodlight if such a situation occurs :\

        @Override
        public void switchAdded( DatapathId dpid )
        {
            NodeId nodeId = NodeId.of(dpid, aliasService::getSwitchAlias);
            IOFSwitch sw = switchService.getSwitch(dpid);

            final boolean added;
            synchronized (topologyLock) {
                traceTopologyChangeBefore(topology, "adding node %s", nodeId);
                added = topology.addNode(nodeId);
                traceTopologyChangeAfter(topology, added);
            }

            if (added) {
                logAddition("node", nodeId);
                for (PortId portId : supprLinkDiscPorts) {
                    LOG.trace("Suppressing link discovery on switch {} for port {}", nodeId, portId);
                    linkDiscService.AddToSuppressLLDPs(dpid, portId.getOFPort());
                }
                onSwitchAddition(sw);
                LOG.trace("Notifying listeners");
                notifyNodeEvent(NodeEvent.added(nodeId, sw));
            }
        }

        @Override
        public void switchActivated( DatapathId dpid )
        {
            NodeId nodeId = NodeId.of(dpid, aliasService::getSwitchAlias);
            IOFSwitch sw = switchService.getActiveSwitch(dpid);

            final boolean isPresent;
            synchronized (topologyLock) {
                isPresent = topology.containsNode(nodeId);
            }

            if (isPresent) {
                logUpdate("node", "switch activation", nodeId);
                LOG.trace("Notifying listeners");
                notifyNodeEvent(NodeEvent.activated(nodeId, sw));
            }
        }

        @Override
        public void switchChanged( DatapathId dpid )
        {
            NodeId nodeId = NodeId.of(dpid, aliasService::getSwitchAlias);
            IOFSwitch sw = switchService.getSwitch(dpid);

            final boolean isPresent;
            synchronized (topologyLock) {
                isPresent = topology.containsNode(nodeId);
            }

            if (isPresent) {
                logUpdate("node", nodeId);
                LOG.trace("Notifying listeners");
                notifyNodeEvent(NodeEvent.updated(nodeId, sw));
            }
        }

        @Override
        public void switchDeactivated( DatapathId dpid )
        {
            NodeId nodeId = NodeId.of(dpid, aliasService::getSwitchAlias);
            IOFSwitch sw = switchService.getSwitch(dpid);

            final boolean isPresent;
            synchronized (topologyLock) {
                isPresent = topology.containsNode(nodeId);
            }

            if (isPresent) {
                logUpdate("node", "switch deactivation", nodeId);
                LOG.trace("Notifying listeners");
                notifyNodeEvent(NodeEvent.deactivated(nodeId, sw));
            }
        }

        @Override
        public void switchRemoved( DatapathId dpid )
        {
            NodeId nodeId = NodeId.of(dpid, aliasService::getSwitchAlias);

            final Optional<Set<DatapathLink>> removedNodeLinks;
            synchronized (topologyLock) {
                traceTopologyChangeBefore(topology, "removing node %s", nodeId);
                removedNodeLinks = topology.removeNode(nodeId);
                traceTopologyChangeAfter(topology, removedNodeLinks.isPresent());
            }

            if (removedNodeLinks.isPresent()) {
                logRemoval("node", nodeId);
                LOG.trace("Notifying listeners");
                notifyNodeEvent(NodeEvent.removed(nodeId));

                for (DatapathLink link : removedNodeLinks.get()) {
                    LinkInfo linkInfo = null;
                    IOFSwitch srcSw = switchService.getSwitch(link.getSrcNode().getDpid());
                    IOFSwitch destSw = switchService.getSwitch(link.getDestNode().getDpid());
                    logRemoval("link", link);
                    LOG.trace("Notifying listeners");
                    notifyLinkEvent(LinkEvent.removed(link, linkInfo, srcSw, destSw));
                }
            }
        }

        @Override
        public void switchPortChanged( DatapathId dpid, OFPortDesc portDesc, PortChangeType type )
        {
            NodeId nodeId = NodeId.of(dpid, aliasService::getSwitchAlias);
            PortId portId = PortId.of(portDesc.getPortNo());

            final boolean nodeIsPresent;
            synchronized (topologyLock) {
                nodeIsPresent = topology.containsNode(nodeId);
            }

            if (nodeIsPresent) {
                NodePort nodePort = NodePort.of(nodeId, portId);
                IOFSwitch sw = switchService.getSwitch(dpid);

                switch (type) {
                    case ADD: {
                        logAddition("port", nodePort);
                        LOG.trace("Notifying listeners");
                        notifyPortEvent(PortEvent.added(nodePort, portDesc, sw));
                    }
                    break;

                    case UP: {
                        logUpdate("port", "port activation", nodePort);
                        LOG.trace("Notifying listeners");
                        notifyPortEvent(PortEvent.activated(nodePort, portDesc, sw));
                    }
                    break;

                    case OTHER_UPDATE: {
                        logUpdate("port", nodePort);
                        LOG.trace("Notifying listeners");
                        notifyPortEvent(PortEvent.updated(nodePort, portDesc, sw));
                    }
                    break;

                    case DOWN: {
                        logUpdate("port", "port deactivation", nodePort);
                        LOG.trace("Notifying listeners");
                        notifyPortEvent(PortEvent.deactivated(nodePort, portDesc, sw));
                    }
                    break;

                    case DELETE: {
                        logRemoval("port", nodePort);
                        LOG.trace("Notifying listeners");
                        notifyPortEvent(PortEvent.removed(nodePort, portDesc, sw));
                    }
                    break;

                    default:
                        throw new AssertionError("unexpected enum value");
                }
            }
        }

        @Override
        public void linkDiscoveryUpdate( List<LDUpdate> updateList )
        {
            for (LDUpdate update : updateList) {
                linkDiscoveryUpdate(update);
            }
        }

        private void onSwitchAddition( @Nullable IOFSwitch sw )
        {
            if (sw != null) {
                LOG.trace("Setting up routing table...");
                OFFactory fact = sw.getOFFactory();
                TableId routTable = TableId.ZERO; // auto-adjusted by sampling

                // First clear all entries from the routing table
                OFFlowDelete.Builder clearBldr = fact.buildFlowDelete()
                    .setTableId(routTable)
                    .setMatch(fact.matchWildcardAll());
                sw.write(
                    clearBldr
                        .build());

                // Secondly, add explicit entries for sending LLDP/BSN packets
                // to the controller
                OFFlowAdd.Builder lldpBldr = buildControllerAction(fact, routTable, LLDP_FLOW);
                sw.write(
                    eternalFlow(
                        withNoOverlap(
                            lldpBldr))
                                .build());

                OFFlowAdd.Builder bsnBldr = buildControllerAction(fact, routTable, BSN_FLOW);
                sw.write(
                    eternalFlow(
                        withNoOverlap(
                            bsnBldr))
                                .build());

                // Finally add a default entry for dropping traffic
                OFFlowAdd.Builder dropBldr = fact.buildFlowAdd()
                    .setTableId(routTable)
                    .setPriority(0)
                    .setMatch(fact.matchWildcardAll())
                    .setInstructions(Collections.emptyList()); // drop
                sw.write(
                    eternalFlow(
                        withNoOverlap(
                            dropBldr))
                                .build());
            }
        }

        private OFFlowAdd.Builder buildControllerAction( OFFactory fact, TableId table, Flow flow )
        {
            return fact.buildFlowAdd()
                .setTableId(table)
                .setPriority(1)
                .setMatch(flow.getMatch(fact))
                .setInstructions(Collections.singletonList(
                    fact.instructions().applyActions(Collections.singletonList(
                        fact.actions().output(OFPort.CONTROLLER, Unsigned.MAX_SHORT)))));
        }

        private void linkDiscoveryUpdate( LDUpdate update )
        {
            switch (update.getOperation()) {
                case LINK_UPDATED: {
                    DatapathLink link = LinkUtils.getLinkFromUpdate(update, aliasService::getSwitchAlias);
                    LinkInfo linkInfo = linkDiscService.getLinkInfo(LinkUtils.getMutableLinkFromUpdate(update));
                    IOFSwitch srcSw = switchService.getSwitch(link.getSrcNode().getDpid());
                    IOFSwitch destSw = switchService.getSwitch(link.getDestNode().getDpid());

                    final boolean isNewLink;
                    synchronized (topologyLock) {
                        traceTopologyChangeBefore(topology, "adding link %s", link);
                        isNewLink = topology.addLink(link);
                        traceTopologyChangeAfter(topology, isNewLink);
                    }

                    if (isNewLink) {
                        logAddition("link", link);
                        LOG.trace("Notifying listeners");
                        notifyLinkEvent(LinkEvent.added(link, linkInfo, srcSw, destSw));
                    }
                    else {
                        logUpdate("link", link);
                        LOG.trace("Notifying listeners");
                        notifyLinkEvent(LinkEvent.updated(link, linkInfo, srcSw, destSw));
                    }
                }
                break;

                case LINK_REMOVED: {
                    DatapathLink link = LinkUtils.getLinkFromUpdate(update, aliasService::getSwitchAlias);
                    LinkInfo linkInfo = null;
                    IOFSwitch srcSw = switchService.getSwitch(link.getSrcNode().getDpid());
                    IOFSwitch destSw = switchService.getSwitch(link.getDestNode().getDpid());

                    final boolean removed;
                    synchronized (topologyLock) {
                        traceTopologyChangeBefore(topology, "removing link %s", link);
                        removed = topology.removeLink(link);
                        traceTopologyChangeAfter(topology, removed);
                    }

                    if (removed) {
                        logRemoval("link", link);
                        LOG.trace("Notifying listeners");
                        notifyLinkEvent(LinkEvent.removed(link, linkInfo, srcSw, destSw));
                    }
                }
                break;

                default:
                // ignore
                break;
            }
        }

        private void notifyNodeEvent( ITopologyNodeEvent ev )
        {
            try {
                for (ITopologyGraphListener listener : graphListeners) {
                    listener.onNodeEvent(ev);
                }
            }
            catch (Throwable t) {
                LOG.error(String.format("Exception upon node event %s", ev.getType()), t);
                throw t;
            }
        }

        private void notifyPortEvent( ITopologyPortEvent ev )
        {
            try {
                for (ITopologyGraphListener listener : graphListeners) {
                    listener.onPortEvent(ev);
                }
            }
            catch (Throwable t) {
                LOG.error(String.format("Exception upon port event %s", ev.getType()), t);
                throw t;
            }
        }

        private void notifyLinkEvent( ITopologyLinkEvent ev )
        {
            try {
                for (ITopologyGraphListener listener : graphListeners) {
                    listener.onLinkEvent(ev);
                }
            }
            catch (Throwable t) {
                LOG.error(String.format("Exception upon link event %s", ev.getType()), t);
                throw t;
            }
        }
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    private static final class NodeEvent implements ITopologyNodeEvent
    {
        static NodeEvent added( NodeId nodeId, @Nullable IOFSwitch sw )
        {
            return new NodeEvent(NodeEventType.NODE_ADDED, nodeId, sw);
        }

        static NodeEvent activated( NodeId nodeId, @Nullable IOFSwitch sw )
        {
            return new NodeEvent(NodeEventType.NODE_ACTIVATED, nodeId, sw);
        }

        static NodeEvent updated( NodeId nodeId, @Nullable IOFSwitch sw )
        {
            return new NodeEvent(NodeEventType.NODE_UPDATED, nodeId, sw);
        }

        static NodeEvent deactivated( NodeId nodeId, @Nullable IOFSwitch sw )
        {
            return new NodeEvent(NodeEventType.NODE_DEACTIVATED, nodeId, sw);
        }

        static NodeEvent removed( NodeId nodeId )
        {
            return new NodeEvent(NodeEventType.NODE_REMOVED, nodeId, null);
        }

        private final NodeEventType       type;
        private final NodeId              nodeId;
        private final Optional<IOFSwitch> sw;

        private NodeEvent( NodeEventType type, NodeId nodeId, @Nullable IOFSwitch sw )
        {
            this.type = type;
            this.nodeId = nodeId;
            this.sw = Optional.ofNullable(sw);
        }

        @Override
        public NodeEventType getType()
        {
            return type;
        }

        @Override
        public NodeId getNodeId()
        {
            return nodeId;
        }

        @Override
        public Optional<IOFSwitch> getIOFSwitch()
        {
            return sw;
        }
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    private static final class PortEvent implements ITopologyPortEvent
    {
        static PortEvent added( NodePort nodePort,
                                OFPortDesc portDesc,
                                @Nullable IOFSwitch sw )
        {
            return new PortEvent(PortEventType.PORT_ADDED, nodePort, portDesc, sw);
        }

        static PortEvent activated( NodePort nodePort,
                                    OFPortDesc portDesc,
                                    @Nullable IOFSwitch sw )
        {
            return new PortEvent(PortEventType.PORT_ACTIVATED, nodePort, portDesc, sw);
        }

        static PortEvent updated( NodePort nodePort,
                                  OFPortDesc portDesc,
                                  @Nullable IOFSwitch sw )
        {
            return new PortEvent(PortEventType.PORT_UPDATED, nodePort, portDesc, sw);
        }

        static PortEvent deactivated( NodePort nodePort,
                                      OFPortDesc portDesc,
                                      @Nullable IOFSwitch sw )
        {
            return new PortEvent(PortEventType.PORT_DEACTIVATED, nodePort, portDesc, sw);
        }

        static PortEvent removed( NodePort nodePort,
                                  OFPortDesc portDesc,
                                  @Nullable IOFSwitch sw )
        {
            return new PortEvent(PortEventType.PORT_REMOVED, nodePort, portDesc, sw);
        }

        private final PortEventType       type;
        private final NodePort            nodePort;
        private final OFPortDesc          portDesc;
        private final Optional<IOFSwitch> sw;

        private PortEvent( PortEventType type,
                           NodePort nodePort,
                           OFPortDesc portDesc,
                           @Nullable IOFSwitch sw )
        {
            this.type = type;
            this.nodePort = nodePort;
            this.portDesc = portDesc;
            this.sw = Optional.ofNullable(sw);
        }

        @Override
        public PortEventType getType()
        {
            return type;
        }

        @Override
        public NodePort getNodePort()
        {
            return nodePort;
        }

        @Override
        public OFPortDesc getOFPortDesc()
        {
            return portDesc;
        }

        @Override
        public Optional<IOFSwitch> getIOFSwitch()
        {
            return sw;
        }
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    private static final class LinkEvent implements ITopologyLinkEvent
    {
        static LinkEvent added( DatapathLink link,
                                @Nullable LinkInfo linkInfo,
                                @Nullable IOFSwitch srcSw,
                                @Nullable IOFSwitch destSw )
        {
            return new LinkEvent(LinkEventType.LINK_ADDED, link, linkInfo, srcSw, destSw);
        }

        static LinkEvent updated( DatapathLink link,
                                  @Nullable LinkInfo linkInfo,
                                  @Nullable IOFSwitch srcSw,
                                  @Nullable IOFSwitch destSw )
        {
            return new LinkEvent(LinkEventType.LINK_UPDATED, link, linkInfo, srcSw, destSw);
        }

        static LinkEvent removed( DatapathLink link,
                                  @Nullable LinkInfo linkInfo,
                                  @Nullable IOFSwitch srcSw,
                                  @Nullable IOFSwitch destSw )
        {
            return new LinkEvent(LinkEventType.LINK_REMOVED, link, linkInfo, srcSw, destSw);
        }

        private final LinkEventType       type;
        private final DatapathLink        link;
        private final Optional<LinkInfo>  linkInfo;
        private final Optional<IOFSwitch> srcSw;
        private final Optional<IOFSwitch> destSw;

        private LinkEvent( LinkEventType type,
                           DatapathLink link,
                           @Nullable LinkInfo linkInfo,
                           @Nullable IOFSwitch srcSw,
                           @Nullable IOFSwitch destSw )
        {
            this.type = type;
            this.link = link;
            this.linkInfo = Optional.ofNullable(linkInfo);
            this.srcSw = Optional.ofNullable(srcSw);
            this.destSw = Optional.ofNullable(destSw);
        }

        @Override
        public LinkEventType getType()
        {
            return type;
        }

        @Override
        public DatapathLink getLink()
        {
            return link;
        }

        @Override
        public Optional<LinkInfo> getLinkInfo()
        {
            return linkInfo;
        }

        @Override
        public Optional<IOFSwitch> getSrcIOFSwitch()
        {
            return srcSw;
        }

        @Override
        public Optional<IOFSwitch> getDestIOFSwitch()
        {
            return destSw;
        }
    }

    private static void logAddition( String objectName, Object object )
    {
        LOG.debug("{} was added to topology: {}", objectName, object);
    }

    private static void logUpdate( String objectName, Object object )
    {
        LOG.debug("{} was updated: {}", objectName, object);
    }

    private static void logUpdate( String objectName, String updateReason, Object object )
    {
        LOG.debug("{} was updated ({}): {}", new Object[] {objectName, updateReason, object});
    }

    private static void logRemoval( String objectName, Object object )
    {
        LOG.debug("{} was removed from topology: {}", objectName, object);
    }

    // requires changeFormat to contain a single "%s"
    private static void traceTopologyChangeBefore( DatapathTopology topo, String changeFmt, Object changeArg )
    {
        if (LOG.isTraceEnabled()) {
            String nl = System.lineSeparator();
            String change = String.format(changeFmt, changeArg);
            LOG.trace("Performing change to topology: {}{}Current topology:{}{}",
                new Object[] {change, nl, nl, topo});
        }
    }

    private static void traceTopologyChangeAfter( DatapathTopology topo, boolean modified )
    {
        if (LOG.isTraceEnabled()) {
            if (modified) {
                String nl = System.lineSeparator();
                LOG.trace("Topology was modified{}New topology:{}{}",
                    new Object[] {nl, nl, topo});
            }
            else {
                LOG.trace("Topology was not modified");
            }
        }
    }
}
