package net.varanus.sdncontroller.qosrouting.internal;


import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.slf4j.Logger;

import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.varanus.sdncontroller.alias.IAliasService;
import net.varanus.sdncontroller.logging.Logging;
import net.varanus.sdncontroller.qosrouting.FlowedRoute;
import net.varanus.sdncontroller.qosrouting.IFlowedConnectionMap;
import net.varanus.sdncontroller.qosrouting.IQoSRoutingListener;
import net.varanus.sdncontroller.qosrouting.IQoSRoutingService;
import net.varanus.sdncontroller.types.FlowedConnection;
import net.varanus.sdncontroller.util.module.IModuleManager;
import net.varanus.sdncontroller.util.module.ModuleUtils;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.text.StringUtils;


/**
 * 
 */
@FieldsAreNonnullByDefault
final class Debugger implements IModuleManager, IQoSRoutingListener
{
    private static final Logger LOG = Logging.qosrouting.LOG;

    private @Nullable Set<FlowedConnection> debuggedFlowedConns;

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies()
    {
        return ModuleUtils.services(IQoSRoutingService.class, IAliasService.class);
    }

    @Override
    public void init( FloodlightModuleContext context, Class<? extends IFloodlightModule> moduleClass )
        throws FloodlightModuleException
    {
        IAliasService aliasService = ModuleUtils.getServiceImpl(context, IAliasService.class);

        Map<String, String> params = context.getConfigParams(moduleClass);
        this.debuggedFlowedConns = Props.getDebuggedFlowedConnection(params, aliasService);

        ModuleUtils.getServiceImpl(context, IQoSRoutingService.class).addListener(this);
    }

    @Override
    public void startUp( FloodlightModuleContext context, Class<? extends IFloodlightModule> moduleClass )
    {
        if (debuggedFlowedConns.isEmpty()) {
            LOG.debug("No debugged flowed-connections configured, nothing to debug");
        }
        else {
            LOG.debug("The following flowed-connections will be debugged:{}{}",
                System.lineSeparator(),
                StringUtils.joinAllInLines(debuggedFlowedConns));
        }
    }

    @Override
    public void connectionRegistered( FlowedConnection conn )
    {
        if (debuggedFlowedConns.contains(conn)) {
            LOG.debug("Flowed-connection ADDED: {}", conn);
        }
    }

    @Override
    public void connectionUnregistered( FlowedConnection conn )
    {
        if (debuggedFlowedConns.contains(conn)) {
            LOG.debug("Flowed-connection REMOVED: {}", conn);
        }
    }

    @Override
    public void connectionMapUpdated( IFlowedConnectionMap map )
    {
        FlowedConnection conn = map.getConnection();
        if (debuggedFlowedConns.contains(conn)) {
            LOG.debug("Flowed-connection UPDATED: {}", conn);
            LOG.debug("Flowed-connection routes:{}{}",
                System.lineSeparator(),
                StringUtils.joinAllInLines(map.getAllRoutes().stream()
                    .map(FlowedRoute::toPrettyString)));
        }
    }
}
