package net.varanus.sdncontroller.monitoring.submodules.sampling.internal;


import java.util.Collection;

import javax.annotation.ParametersAreNonnullByDefault;

import org.slf4j.Logger;

import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.varanus.sdncontroller.logging.Logging;
import net.varanus.sdncontroller.monitoring.internal.IMonitoringModuleContext;
import net.varanus.sdncontroller.monitoring.submodules.sampling.internal.SwitchComm.SwitchCommException;
import net.varanus.sdncontroller.monitoring.util.ISubmoduleManager;
import net.varanus.sdncontroller.topologygraph.ITopologyGraphListener;
import net.varanus.sdncontroller.topologygraph.ITopologyGraphService;
import net.varanus.sdncontroller.topologygraph.event.ITopologyLinkEvent;
import net.varanus.sdncontroller.topologygraph.event.ITopologyNodeEvent;
import net.varanus.sdncontroller.topologygraph.event.ITopologyPortEvent;
import net.varanus.sdncontroller.types.DatapathLink;
import net.varanus.sdncontroller.util.module.ModuleUtils;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.openflow.types.NodeId;
import net.varanus.util.openflow.types.NodePort;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
final class TopologyListener implements ITopologyGraphListener, ISubmoduleManager
{
    private static final Logger LOG = Logging.monitoring.sampling.LOG;

    private final Sampler sampler;

    TopologyListener( Sampler sampler )
    {
        this.sampler = sampler;
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
    public void startUp( IMonitoringModuleContext context )
    { /* do nothing */ }

    @Override
    public void onNodeEvent( ITopologyNodeEvent event )
    {
        try {
            NodeId nodeId = event.getNodeId();
            switch (event.getType()) {
                case NODE_ADDED: {
                    LOG.debug("SWITCH ADDED: {}", nodeId);
                    SwitchComm.handleAddedSwitch(event.getIOFSwitch(), nodeId);
                }
                break;

                case NODE_REMOVED: {
                    LOG.debug("SWITCH REMOVED: {}", nodeId);
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
        catch (SwitchCommException e) {
            LOG.error(e.getMessage());
        }
    }

    @Override
    public void onPortEvent( ITopologyPortEvent event )
    {
        try {
            NodePort nodePort = event.getNodePort();
            switch (event.getType()) {
                case PORT_ADDED: {
                    LOG.debug("SWITCH PORT ADDED: {}", nodePort);
                    SwitchComm.handleAddedSwitchPort(
                        event.getIOFSwitch(),
                        nodePort.getNodeId(),
                        nodePort.getPortId());
                }
                break;

                case PORT_ACTIVATED:
                case PORT_UPDATED:
                case PORT_DEACTIVATED:
                case PORT_REMOVED:
                // ignore
                // FIXME? handle port down
                break;

                default:
                    throw new AssertionError("unexpected enum value");
            }
        }
        catch (SwitchCommException e) {
            LOG.error(e.getMessage());
        }
    }

    @Override
    public void onLinkEvent( ITopologyLinkEvent event )
    {
        try {
            DatapathLink link = event.getLink();
            switch (event.getType()) {
                case LINK_ADDED: {
                    LOG.debug("LINK ADDED: {}", link);
                    SwitchComm.handleAddedLink(event.getSrcIOFSwitch(), event.getDestIOFSwitch(), link);
                    sampler.onAddedLink(link);
                }
                break;

                case LINK_UPDATED:
                // ignore
                break;

                case LINK_REMOVED: {
                    LOG.debug("LINK REMOVED: {}", link);
                    sampler.onRemovedLink(link);
                    SwitchComm.handleRemovedLink(event.getSrcIOFSwitch(), event.getDestIOFSwitch(), link);
                }
                break;

                default:
                    throw new AssertionError("unexpected enum value");
            }
        }
        catch (SwitchCommException e) {
            LOG.error(e.getMessage());
        }
    }
}
