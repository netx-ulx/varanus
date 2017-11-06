package net.varanus.sdncontroller.monitoring.submodules.sampling.internal;


import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.ParametersAreNonnullByDefault;

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
    static DurationRange getSamplingDurationRange( Map<String, String> params ) throws FloodlightModuleException
    {
        TimeLong minDuration = getMinSamplingDuration(params);
        TimeLong maxDuration = getMaxSamplingDuration(params);
        if (Comparables.aGTb(minDuration, maxDuration))
            throw new FloodlightModuleException(
                "invalid sampling round duration range: minimum value is greater than maximum value");

        return new DurationRange(minDuration, maxDuration);
    }

    private static TimeLong getMinSamplingDuration( Map<String, String> params ) throws FloodlightModuleException
    {
        return ModuleUtils.readCustomProperty(params,
            CustomProperty.of(
                "sampling_minSamplingRoundDurationMillis",
                TimeLong.of(1, TimeUnit.SECONDS),
                s -> TimeLong.parse(s, TimeUnit.MILLISECONDS)));
    }

    private static TimeLong getMaxSamplingDuration( Map<String, String> params ) throws FloodlightModuleException
    {
        return ModuleUtils.readCustomProperty(params,
            CustomProperty.of(
                "sampling_maxSamplingRoundDurationMillis",
                TimeLong.of(2, TimeUnit.SECONDS),
                s -> TimeLong.parse(s, TimeUnit.MILLISECONDS)));
    }

    static TimeLong getPreSamplingExcessDuration( Map<String, String> params ) throws FloodlightModuleException
    {
        return ModuleUtils.readCustomProperty(params,
            CustomProperty.of(
                "sampling_preSamplingExcessDurationMillis",
                TimeLong.of(1, TimeUnit.SECONDS),
                s -> TimeLong.parse(s, TimeUnit.MILLISECONDS)));
    }

    static TimeLong getPostSamplingExcessDuration( Map<String, String> params ) throws FloodlightModuleException
    {
        return ModuleUtils.readCustomProperty(params,
            CustomProperty.of(
                "sampling_postSamplingExcessDurationMillis",
                TimeLong.of(500, TimeUnit.MILLISECONDS),
                s -> TimeLong.parse(s, TimeUnit.MILLISECONDS)));
    }

    static int getMaxSimultaneousSamplings( Map<String, String> params ) throws FloodlightModuleException
    {
        return ModuleUtils.readIntProperty(params,
            IntProperty.ofPositive("sampling_maxSimultaneousSamplings", 1));
    }

    private Props()
    {
        // not used
    }
}
