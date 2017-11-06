package net.varanus.sdncontroller.staticforwarding.internal;


import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.slf4j.Logger;

import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.staticflowentry.IStaticFlowEntryPusherService;
import net.floodlightcontroller.staticflowentry.StaticFlowEntryPusher;
import net.floodlightcontroller.storage.IStorageSourceService;
import net.varanus.sdncontroller.alias.IAliasService;
import net.varanus.sdncontroller.logging.Logging;
import net.varanus.sdncontroller.topologygraph.ITopologyGraphListener;
import net.varanus.sdncontroller.topologygraph.ITopologyGraphService;
import net.varanus.sdncontroller.topologygraph.event.ITopologyLinkEvent;
import net.varanus.sdncontroller.topologygraph.event.ITopologyNodeEvent;
import net.varanus.sdncontroller.topologygraph.event.ITopologyPortEvent;
import net.varanus.sdncontroller.topologygraph.event.ITopologyNodeEvent.NodeEventType;
import net.varanus.sdncontroller.util.module.IModuleManager;
import net.varanus.sdncontroller.util.module.ModuleUtils;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.openflow.types.NodeId;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class StaticForwardingManager implements IModuleManager, ITopologyGraphListener
{
    private static final Logger LOG = Logging.staticforwarding.LOG;

    private @Nullable Map<NodeId, List<Map<String, Object>>> staticFlowEntries;
    private @Nullable IStorageSourceService                  storageSrcService;

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies()
    {
        return ModuleUtils.services(
            IStaticFlowEntryPusherService.class,
            IStorageSourceService.class,
            ITopologyGraphService.class,
            IAliasService.class);
    }

    @Override
    public void init( FloodlightModuleContext context, Class<? extends IFloodlightModule> moduleClass )
        throws FloodlightModuleException
    {
        IAliasService aliasService = ModuleUtils.getServiceImpl(context, IAliasService.class);
        Map<String, String> params = context.getConfigParams(moduleClass);
        this.staticFlowEntries = Props.getStaticFlowEntries(params, aliasService);

        this.storageSrcService = ModuleUtils.getServiceImpl(context, IStorageSourceService.class);
        ModuleUtils.getServiceImpl(context, ITopologyGraphService.class).addListener(this);
    }

    @Override
    public void startUp( FloodlightModuleContext context, Class<? extends IFloodlightModule> moduleClass )
    {
        if (this.staticFlowEntries.isEmpty()) {
            LOG.info("No static flow entries configured, nothing to install");
        }
        else {
            LOG.info("There are configured static flow entries, will install them after switches connect");
        }
    }

    @Override
    public void onNodeEvent( ITopologyNodeEvent event )
    {
        if (event.getType().equals(NodeEventType.NODE_ADDED)) {
            NodeId nodeId = event.getNodeId();
            List<Map<String, Object>> switchEntries = staticFlowEntries.get(nodeId);
            if (switchEntries != null) {
                LOG.debug("Installing flow entries for switch {}", nodeId);
                for (Map<String, Object> flowEntry : switchEntries) {
                    storageSrcService.insertRow(StaticFlowEntryPusher.TABLE_NAME, flowEntry);
                }
            }
        }
    }

    @Override
    public void onPortEvent( ITopologyPortEvent event )
    {/* not used */}

    @Override
    public void onLinkEvent( ITopologyLinkEvent event )
    {/* not used */}
}
