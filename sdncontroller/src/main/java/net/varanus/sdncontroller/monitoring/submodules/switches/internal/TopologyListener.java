package net.varanus.sdncontroller.monitoring.submodules.switches.internal;


import java.util.Collection;

import javax.annotation.ParametersAreNonnullByDefault;

import org.slf4j.Logger;

import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.varanus.sdncontroller.logging.Logging;
import net.varanus.sdncontroller.monitoring.internal.IMonitoringModuleContext;
import net.varanus.sdncontroller.monitoring.util.ISubmoduleManager;
import net.varanus.sdncontroller.topologygraph.ITopologyGraphListener;
import net.varanus.sdncontroller.topologygraph.ITopologyGraphService;
import net.varanus.sdncontroller.topologygraph.event.ITopologyLinkEvent;
import net.varanus.sdncontroller.topologygraph.event.ITopologyNodeEvent;
import net.varanus.sdncontroller.topologygraph.event.ITopologyPortEvent;
import net.varanus.sdncontroller.util.module.ModuleUtils;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.openflow.types.NodeId;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
final class TopologyListener implements ISubmoduleManager, ITopologyGraphListener
{
    private static final Logger LOG = Logging.monitoring.switches.LOG;

    private final SwitchMonitor monitor;

    TopologyListener( SwitchMonitor monitor )
    {
        this.monitor = monitor;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies()
    {
        return ModuleUtils.services(ITopologyGraphService.class);
    }

    @Override
    public void init( IMonitoringModuleContext context ) throws FloodlightModuleException
    {
        context.getServiceImpl(ITopologyGraphService.class).addListener(this);
    }

    @Override
    public void startUp( IMonitoringModuleContext context ) throws FloodlightModuleException
    { /* do nothing */}

    @Override
    public void onNodeEvent( ITopologyNodeEvent event )
    {
        NodeId nodeId = event.getNodeId();
        switch (event.getType()) {
            case NODE_ADDED:
                LOG.debug("NODE ADDED: {}", nodeId);
                monitor.onAddedNode(nodeId);
            break;

            case NODE_ACTIVATED:
            case NODE_UPDATED:
            case NODE_DEACTIVATED:
            // ignore
            break;

            case NODE_REMOVED:
                LOG.debug("NODE REMOVED: {}", nodeId);
                monitor.onRemovedNode(nodeId);
            break;

            default:
                throw new AssertionError("unexpected enum value");
        }
    }

    @Override
    public void onLinkEvent( ITopologyLinkEvent event )
    {/* do nothing */ }

    @Override
    public void onPortEvent( ITopologyPortEvent event )
    { /* do nothing */ }
}
