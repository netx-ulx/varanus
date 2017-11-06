package net.varanus.sdncontroller.alias.internal;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.ParametersAreNonnullByDefault;

import org.projectfloodlight.openflow.types.DatapathId;

import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.varanus.sdncontroller.util.module.ModuleUtils;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.openflow.NodePortUtils;
import net.varanus.util.text.CustomProperty;


/**
 * 
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
final class Props
{
    static ConcurrentMap<DatapathId, String> getDatapathAliases( Map<String, String> params )
        throws FloodlightModuleException
    {
        return ModuleUtils.readCustomProperty(params,
            CustomProperty.of("switchAliases", Props::parseAliasMap));
    }

    private static ConcurrentMap<DatapathId, String> parseAliasMap( String s ) throws IllegalArgumentException
    {
        return NodePortUtils.parseAliasMap(s, ConcurrentHashMap::new);
    }

    private Props()
    {
        // not used
    }
}
