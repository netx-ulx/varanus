package net.varanus.sdncontroller.monitoring.submodules.switches.internal;


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
    static DurationRange getMonitoringWaitPeriodRange( Map<String, String> params ) throws FloodlightModuleException
    {
        TimeLong minPeriod = getMinMonitoringWaitPeriod(params);
        TimeLong maxPeriod = getMaxMonitoringWaitPeriod(params);
        if (Comparables.aGTb(minPeriod, maxPeriod))
            throw new FloodlightModuleException(
                "invalid switch monitoring round wait period range: minimum value is greater than maximum value");

        return new DurationRange(minPeriod, maxPeriod);
    }

    private static TimeLong getMinMonitoringWaitPeriod( Map<String, String> params ) throws FloodlightModuleException
    {
        return ModuleUtils.readCustomProperty(params,
            CustomProperty.of(
                "switches_minMonitoringRoundWaitPeriodMillis",
                TimeLong.of(1, TimeUnit.SECONDS),
                s -> TimeLong.parse(s, TimeUnit.MILLISECONDS)));
    }

    private static TimeLong getMaxMonitoringWaitPeriod( Map<String, String> params ) throws FloodlightModuleException
    {
        return ModuleUtils.readCustomProperty(params,
            CustomProperty.of(
                "switches_maxMonitoringRoundWaitPeriodMillis",
                TimeLong.of(2, TimeUnit.SECONDS),
                s -> TimeLong.parse(s, TimeUnit.MILLISECONDS)));
    }

    static int getMaxSimultaneousMonitorings( Map<String, String> params ) throws FloodlightModuleException
    {
        return ModuleUtils.readIntProperty(params,
            IntProperty.ofPositive("switches_maxSimultaneousMonitorings", 1));
    }

    private Props()
    {
        // not used
    }
}
