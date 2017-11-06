package net.varanus.sdncontroller.qosrouting.internal;


import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.varanus.sdncontroller.alias.IAliasService;
import net.varanus.sdncontroller.types.FlowedConnection;
import net.varanus.sdncontroller.util.module.ModuleUtils;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.json.JSONUtils;
import net.varanus.util.text.CustomProperty;


/**
 * 
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
final class Props
{
    static Set<FlowedConnection> getDebuggedFlowedConnection( Map<String, String> params, IAliasService aliasService )
        throws FloodlightModuleException
    {
        return ModuleUtils.readCustomProperty(params,
            CustomProperty.of("debuggedFlowedConnections",
                Collections.emptySet(),
                s -> JSONUtils.parseSet(s, ss -> FlowedConnection.parse(ss, aliasService::getSwitchAlias))));
    }

    private Props()
    {
        // not used
    }
}
