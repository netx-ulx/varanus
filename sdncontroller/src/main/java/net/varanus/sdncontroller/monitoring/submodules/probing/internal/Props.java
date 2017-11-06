package net.varanus.sdncontroller.monitoring.submodules.probing.internal;


import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.ParametersAreNonnullByDefault;

import org.projectfloodlight.openflow.types.EthType;

import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.varanus.sdncontroller.util.DurationRange;
import net.varanus.sdncontroller.util.module.ModuleUtils;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.lang.Comparables;
import net.varanus.util.text.CustomProperty;
import net.varanus.util.text.IntProperty;
import net.varanus.util.time.TimeLong;


/**
 * 
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
final class Props
{
    static DurationRange getProbingDurationRange( Map<String, String> params ) throws FloodlightModuleException
    {
        TimeLong minDuration = getMinProbingDuration(params);
        TimeLong maxDuration = getMaxProbingDuration(params);
        if (Comparables.aGTb(minDuration, maxDuration))
            throw new FloodlightModuleException(
                "invalid probing round duration range: minimum value is greater than maximum value");

        return new DurationRange(minDuration, maxDuration);
    }

    private static TimeLong getMinProbingDuration( Map<String, String> params )
        throws FloodlightModuleException
    {
        return ModuleUtils.readCustomProperty(params,
            CustomProperty.of(
                "probing_minProbingRoundDurationMillis",
                TimeLong.of(1, TimeUnit.SECONDS),
                s -> TimeLong.parse(s, TimeUnit.MILLISECONDS)));
    }

    private static TimeLong getMaxProbingDuration( Map<String, String> params )
        throws FloodlightModuleException
    {
        return ModuleUtils.readCustomProperty(params,
            CustomProperty.of(
                "probing_maxProbingRoundDurationMillis",
                TimeLong.of(2, TimeUnit.SECONDS),
                s -> TimeLong.parse(s, TimeUnit.MILLISECONDS)));
    }

    static TimeLong getPreTransmissionDuration( Map<String, String> params ) throws FloodlightModuleException
    {
        return ModuleUtils.readCustomProperty(params,
            CustomProperty.of(
                "probing_preTransmissionDurationMillis",
                TimeLong.of(500, TimeUnit.MILLISECONDS),
                s -> TimeLong.parse(s, TimeUnit.MILLISECONDS)));
    }

    static int getMaxSimultaneousProbings( Map<String, String> params ) throws FloodlightModuleException
    {
        return ModuleUtils.readIntProperty(params,
            IntProperty.ofPositive("probing_maxSimultaneousProbings", 1));
    }

    static EthType getProbePacketEthertype( Map<String, String> params ) throws FloodlightModuleException
    {
        int ethType = ModuleUtils.readIntProperty(params,
            IntProperty.ofPositive("probing_probePacketEthertype"));
        return EthType.of(ethType);
    }

    private Props()
    {
        // not used
    }
}
