package net.varanus.sdncontroller.linkstats.internal;


import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.sdncontroller.linkstats.FlowedLinkStats;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.time.TimeDoubleUnit;


/**
 * 
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
enum LogFileFStats
{
    TRAJECTORY_LATENCY
    {
        @Override
        String getStat( FlowedLinkStats stats )
        {
            return stats.trajectory().getLatency().value()
                .latestToString(lat -> lat.toString(TimeDoubleUnit.MILLISECONDS));
        }
    },
    TRAJECTORY_BYTE_LOSS
    {
        @Override
        String getStat( FlowedLinkStats stats )
        {
            return stats.trajectory().getByteLoss().value()
                .latestToString(rat -> rat.toDecimalString(3));
        }
    },
    TRAJECTORY_PACKET_LOSS
    {
        @Override
        String getStat( FlowedLinkStats stats )
        {
            return stats.trajectory().getPacketLoss().value()
                .latestToString(rat -> rat.toDecimalString(3));
        }
    };

    static LogFileFStats parse( String value ) throws IllegalArgumentException
    {
        return LogFileFStats.valueOf(value.toUpperCase());
    }

    abstract String getStat( FlowedLinkStats stats );

    @Override
    public String toString()
    {
        return name().toLowerCase();
    }
}
