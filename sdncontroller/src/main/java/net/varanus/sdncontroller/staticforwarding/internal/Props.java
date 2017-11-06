package net.varanus.sdncontroller.staticforwarding.internal;


import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.staticflowentry.StaticFlowEntries;
import net.varanus.sdncontroller.alias.IAliasService;
import net.varanus.sdncontroller.util.module.ModuleUtils;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.json.JSONUtils;
import net.varanus.util.openflow.types.NodeId;
import net.varanus.util.text.CustomProperty;


/**
 * 
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
final class Props
{
    static Map<NodeId, List<Map<String, Object>>> getStaticFlowEntries( Map<String, String> params,
                                                                        IAliasService aliasService )
        throws FloodlightModuleException
    {
        return ModuleUtils.readCustomProperty(params,
            CustomProperty.of("staticFlowEntries",
                s -> Props.parseStaticFlowEntries(s, aliasService)));
    }

    private static Map<NodeId, List<Map<String, Object>>> parseStaticFlowEntries( String s, IAliasService aliasService )
        throws IllegalArgumentException
    {
        return JSONUtils.parseMapOfLists(s,
            ss -> NodeId.parse(ss, aliasService::getSwitchAlias),
            Props::parseStorageEntry);
    }

    private static Map<String, Object> parseStorageEntry( String s ) throws IllegalArgumentException
    {
        try {
            return StaticFlowEntries.jsonToStorageEntry(s);
        }
        catch (IOException e) {
            throw new IllegalArgumentException("error while parsing storage entry", e);
        }
    }

    private Props()
    {
        // not used
    }
}
