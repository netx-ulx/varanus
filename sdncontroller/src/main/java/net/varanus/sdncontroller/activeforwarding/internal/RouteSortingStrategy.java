package net.varanus.sdncontroller.activeforwarding.internal;


import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.sdncontroller.linkstats.FlowedLinkStats;
import net.varanus.sdncontroller.util.Ratio;
import net.varanus.sdncontroller.util.RatioSummary;
import net.varanus.sdncontroller.util.TimeSummary;
import net.varanus.sdncontroller.util.stats.Stat;
import net.varanus.sdncontroller.util.stats.StatType;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.time.TimeDouble;
import net.varanus.util.time.TimeDoubleUnit;


/**
 * 
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
enum RouteSortingStrategy
{
    MIN_LATENCY
    {
        @Override
        double weightStats( FlowedLinkStats stats )
        {
            return Weighters.weightLatency(stats);
        }
    },

    MIN_BYTE_LOSS
    {
        @Override
        double weightStats( FlowedLinkStats stats )
        {
            return Weighters.weightByteLoss(stats);
        }
    },

    GOODNESS
    {
        @Override
        double weightStats( FlowedLinkStats stats )
        {
            return Weighters.weightGoodness(stats);
        }
    };

    static RouteSortingStrategy parse( String s )
    {
        return valueOf(s.toUpperCase());
    }

    abstract double weightStats( FlowedLinkStats stats );

    @Override
    public String toString()
    {
        return name().toLowerCase();
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    private static final class Weighters
    {
        private static final double         LATENCY_CENTER      = 50;
        private static final TimeDoubleUnit LATENCY_WEIGHT_UNIT = TimeDoubleUnit.MILLISECONDS;
        private static final double         BYTE_LOSS_CENTER    = mapProperFraction(0.25);

        static double weightLatency( FlowedLinkStats stats )
        {
            Stat<TimeSummary> latStat = stats.getLatency();
            if (latStat.value().isPresent()) {
                TimeDouble latency = latStat.value().getMean();
                return latencyBaseWeight(latency) + penalty(latStat.type());
            }
            else {
                return 0;
            }
        }

        static double weightByteLoss( FlowedLinkStats stats )
        {
            Stat<RatioSummary> lossStat = stats.getByteLoss();
            if (lossStat.value().isPresent()) {
                Ratio byteLoss = lossStat.value().getMean();
                return byteLossBaseWeight(byteLoss) + penalty(lossStat.type());
            }
            else {
                return 0;
            }
        }

        static double weightGoodness( FlowedLinkStats stats )
        {
            return weightLatency(stats) + weightByteLoss(stats);
        }

        private static double latencyBaseWeight( TimeDouble lat )
        {
            if (lat.isPresent()) {
                double x = lat.in(LATENCY_WEIGHT_UNIT);
                if (x != Double.POSITIVE_INFINITY)
                    return x / (LATENCY_CENTER + x);
                else
                    return 1;
            }
            else {
                return 0;
            }
        }

        private static double byteLossBaseWeight( Ratio loss )
        {
            if (loss.isProperFraction()) {
                double x = mapProperFraction(loss.doubleValue());
                if (x != Double.POSITIVE_INFINITY)
                    return x / (BYTE_LOSS_CENTER + x);
                else
                    return 1;
            }
            else {
                return 0;
            }
        }

        // requires proper fraction argument
        // maps [0, 1) to [0, +inf)
        private static double mapProperFraction( double d )
        {
            return d / (1.0 - d);
        }

        private static double penalty( StatType type )
        {
            switch (type) {
                case SAFE:
                    return 0;

                case UNSAFE:
                    return 1;

                default:
                    throw new IllegalStateException("unexpected statistic type");
            }
        }

        private Weighters()
        {
            // not used
        }
    }
}
