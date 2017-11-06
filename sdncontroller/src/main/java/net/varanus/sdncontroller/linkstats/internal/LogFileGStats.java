package net.varanus.sdncontroller.linkstats.internal;


import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.sdncontroller.linkstats.GeneralLinkStats;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.time.TimeDoubleUnit;


/**
 * 
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
enum LogFileGStats
{
    LLDP_LATENCY
    {
        @Override
        String getStat( GeneralLinkStats stats )
        {
            return stats.lldpProbing().getLatency().value()
                .latestToString(lat -> lat.toString(TimeDoubleUnit.MILLISECONDS));
        }
    },

    SECURE_LATENCY
    {
        @Override
        String getStat( GeneralLinkStats stats )
        {
            return stats.secureProbing().getLatency().value()
                .latestToString(lat -> lat.toString(TimeDoubleUnit.MILLISECONDS));
        }
    };

    static LogFileGStats parse( String value ) throws IllegalArgumentException
    {
        return LogFileGStats.valueOf(value.toUpperCase());
    }

    abstract String getStat( GeneralLinkStats stats );

    @Override
    public String toString()
    {
        return name().toLowerCase();
    }
}
