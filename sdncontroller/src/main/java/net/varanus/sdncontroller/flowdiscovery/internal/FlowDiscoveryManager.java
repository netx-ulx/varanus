package net.varanus.sdncontroller.flowdiscovery.internal;


import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.slf4j.Logger;

import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.varanus.sdncontroller.alias.IAliasService;
import net.varanus.sdncontroller.logging.Logging;
import net.varanus.sdncontroller.qosrouting.IQoSRoutingService;
import net.varanus.sdncontroller.types.FlowedConnection;
import net.varanus.sdncontroller.util.module.IModuleManager;
import net.varanus.sdncontroller.util.module.ModuleUtils;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.text.StringUtils;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class FlowDiscoveryManager implements IModuleManager
{
    private static final Logger LOG = Logging.flowdiscovery.LOG;

    private @Nullable Set<FlowedConnection> flowedConns;
    private @Nullable IQoSRoutingService    qosRoutingService;

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies()
    {
        return ModuleUtils.services(IQoSRoutingService.class, IAliasService.class);
    }

    @Override
    public void init( FloodlightModuleContext context,
                      Class<? extends IFloodlightModule> moduleClass )
        throws FloodlightModuleException
    {
        IAliasService aliasService = ModuleUtils.getServiceImpl(context, IAliasService.class);
        Map<String, String> params = context.getConfigParams(moduleClass);
        this.flowedConns = Props.getStaticConnections(params, aliasService);

        this.qosRoutingService = ModuleUtils.getServiceImpl(context, IQoSRoutingService.class);
    }

    @Override
    public void startUp( FloodlightModuleContext context, Class<? extends IFloodlightModule> moduleClass )
        throws FloodlightModuleException
    {
        if (flowedConns.isEmpty()) {
            LOG.info("No static flowed-connections configured, nothing to route");
        }
        else {
            try {
                for (FlowedConnection conn : flowedConns) {
                    qosRoutingService.registerConnection(conn, true);
                }
            }
            catch (IllegalArgumentException e) {
                throw new FloodlightModuleException("error while configuring static flowed-connection", e);
            }

            LOG.info("Static flowed-connections configured:{}{}",
                System.lineSeparator(),
                StringUtils.joinAllInLines(flowedConns));
        }
    }
}
