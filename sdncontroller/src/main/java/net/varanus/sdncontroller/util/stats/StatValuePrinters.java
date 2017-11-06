package net.varanus.sdncontroller.util.stats;


import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.sdncontroller.util.Ratio;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.functional.Possible;
import net.varanus.util.functional.PossibleDouble;
import net.varanus.util.unitvalue.si.InfoDouble;
import net.varanus.util.unitvalue.si.MetricDouble;


/**
 * 
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class StatValuePrinters
{
    public static String roundedDouble( PossibleDouble val )
    {
        return val.toString(num -> String.format("%.3f", num));
    }

    public static String dataPerSecond( InfoDouble val )
    {
        return val.toBitStringAnd("/s");
    }

    public static String packetsPerSecond( MetricDouble val )
    {
        return val.toStringAnd("pps");
    }

    public static String ratio( Possible<Ratio> val )
    {
        return val.toString(Ratio::toPercentageString);
    }

    public static String prettyDataRecepRate( InfoDouble transRate, InfoDouble recepRate )
    {
        if (transRate.isPresent() && recepRate.isPresent()) {
            double diffBits = recepRate.inBits() - transRate.inBits();
            String sign = (diffBits < 0) ? "-" : "+";
            return String.format("%s (%s%s)",
                dataPerSecond(recepRate),
                sign,
                InfoDouble.ofBits(Math.abs(diffBits)).toBitString());
        }
        else {
            return dataPerSecond(recepRate);
        }
    }

    public static String prettyPacketRecepRate( MetricDouble transRate, MetricDouble recepRate )
    {
        if (transRate.isPresent() && recepRate.isPresent()) {
            double diff = recepRate.inUnits() - transRate.inUnits();
            String sign = (diff < 0) ? "-" : "+";
            return String.format("%s (%s%s)",
                packetsPerSecond(recepRate),
                sign,
                MetricDouble.ofUnits(Math.abs(diff)).toStringAnd("p"));
        }
        else {
            return packetsPerSecond(recepRate);
        }
    }

    private StatValuePrinters()
    {
        // not used
    }
}
