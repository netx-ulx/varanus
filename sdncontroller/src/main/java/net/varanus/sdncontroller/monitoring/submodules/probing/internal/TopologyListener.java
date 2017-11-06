package net.varanus.sdncontroller.monitoring.submodules.probing.internal;


import java.util.Collection;
import java.util.Optional;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.slf4j.Logger;

import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.linkdiscovery.internal.LinkInfo;
import net.varanus.sdncontroller.linkstats.ILinkStatsService;
import net.varanus.sdncontroller.linkstats.sample.LLDPProbingSample;
import net.varanus.sdncontroller.logging.Logging;
import net.varanus.sdncontroller.monitoring.internal.IMonitoringModuleContext;
import net.varanus.sdncontroller.monitoring.submodules.probing.internal.SwitchComm.SwitchCommException;
import net.varanus.sdncontroller.monitoring.util.ISubmoduleManager;
import net.varanus.sdncontroller.topologygraph.ITopologyGraphListener;
import net.varanus.sdncontroller.topologygraph.ITopologyGraphService;
import net.varanus.sdncontroller.topologygraph.event.ITopologyLinkEvent;
import net.varanus.sdncontroller.topologygraph.event.ITopologyNodeEvent;
import net.varanus.sdncontroller.topologygraph.event.ITopologyPortEvent;
import net.varanus.sdncontroller.types.DatapathLink;
import net.varanus.sdncontroller.util.LinkUtils;
import net.varanus.sdncontroller.util.module.ModuleUtils;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.openflow.types.NodeId;
import net.varanus.util.time.TimeDouble;
import net.varanus.util.time.Timed;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
final class TopologyListener implements ISubmoduleManager, ITopologyGraphListener
{
    private static final Logger LOG = Logging.monitoring.probing.LOG;

    private final Prober prober;

    private @Nullable ILinkStatsService linkStatsService;

    TopologyListener( Prober prober )
    {
        this.prober = prober;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies()
    {
        return ModuleUtils.services(ITopologyGraphService.class, ILinkStatsService.class);
    }

    @Override
    public void init( IMonitoringModuleContext context ) throws FloodlightModuleException
    {
        this.linkStatsService = context.getServiceImpl(ILinkStatsService.class);
        context.getServiceImpl(ITopologyGraphService.class).addListener(this);
    }

    @Override
    public void startUp( IMonitoringModuleContext context ) throws FloodlightModuleException
    { /* do nothing */}

    @Override
    public void onNodeEvent( ITopologyNodeEvent event )
    {
        try {
            NodeId nodeId = event.getNodeId();
            switch (event.getType()) {
                case NODE_ADDED:
                    LOG.debug("NODE ADDED: {}", nodeId);
                    prober.onAddedNode(event.getIOFSwitch(), nodeId);
                break;

                case NODE_ACTIVATED:
                case NODE_UPDATED:
                case NODE_DEACTIVATED:
                case NODE_REMOVED:
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
    public void onLinkEvent( ITopologyLinkEvent event )
    {
        DatapathLink link = event.getLink();
        switch (event.getType()) {
            case LINK_ADDED:
                LOG.debug("LINK ADDED: {}", link);
                updateOnLLDPProbing(link, event.getLinkInfo());
                prober.onAddedLink(link);
            break;

            case LINK_UPDATED:
                LOG.debug("LINK UPDATED: {}", link);
                updateOnLLDPProbing(link, event.getLinkInfo());
            break;

            case LINK_REMOVED:
                LOG.debug("LINK REMOVED: {}", link);
                prober.onRemovedLink(link);
            break;

            default:
                throw new AssertionError("unexpected enum value");
        }
    }

    private void updateOnLLDPProbing( DatapathLink link, Optional<LinkInfo> info )
    {
        if (info.isPresent()) {
            Timed<TimeDouble> latency = Timed.now(LinkUtils.getLLDPLatency(info.get()));
            LLDPProbingSample sample = LLDPProbingSample.of(link, latency);
            LOG.debug("Updating LLDP latency of {} for link {}", latency, link);
            linkStatsService.updateGeneralStats(sample);
        }
    }

    @Override
    public void onPortEvent( ITopologyPortEvent event )
    { /* do nothing */ }
}
