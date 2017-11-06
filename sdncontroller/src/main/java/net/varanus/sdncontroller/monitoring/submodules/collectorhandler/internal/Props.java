package net.varanus.sdncontroller.monitoring.submodules.collectorhandler.internal;


import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.varanus.sdncontroller.util.module.ModuleUtils;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.openflow.types.PortId;
import net.varanus.util.text.CustomProperty;
import net.varanus.util.text.IntProperty;


/**
 * 
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
final class Props
{
    static int getLocalPort( Map<String, String> params ) throws FloodlightModuleException
    {
        return ModuleUtils.readIntProperty(params,
            IntProperty.ofNonNegative("collectorhandler_localPort"));
    }

    static PortId getSamplingPort( Map<String, String> params ) throws FloodlightModuleException
    {
        return ModuleUtils.readCustomProperty(params,
            CustomProperty.of("collectorhandler_samplingOFPort", PortId::parse));
    }

    private Props()
    {
        // not used
    }
}
