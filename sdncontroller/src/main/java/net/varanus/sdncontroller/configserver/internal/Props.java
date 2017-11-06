package net.varanus.sdncontroller.configserver.internal;


import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.varanus.sdncontroller.util.module.ModuleUtils;
import net.varanus.util.text.IntProperty;


/**
 * 
 */
@ParametersAreNonnullByDefault
final class Props
{
    static int getLocalPort( Map<String, String> params ) throws FloodlightModuleException
    {
        return ModuleUtils.readIntProperty(params, IntProperty.ofNonNegative("localPort"));
    }

    private Props()
    {
        // not used
    }
}
