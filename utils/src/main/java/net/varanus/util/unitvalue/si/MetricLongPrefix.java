package net.varanus.util.unitvalue.si;


import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;


/**
 * 
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public enum MetricLongPrefix implements SIMultipleLongUnit<MetricLongPrefix>
{
    UNIT
    {//@formatter:off
        @Override
        public String toString() { return ""; }
        
        @Override
        public long toUnitValue( long v ) { return v; }

        @Override
        public long toKiloValue( long v ) { return v/(K/u); }

        @Override
        public long toMegaValue( long v ) { return v/(M/u); }

        @Override
        public long toGigaValue( long v ) { return v/(G/u); }

        @Override
        public long toTeraValue( long v ) { return v/(T/u); }

        @Override
        public long convert(long v, MetricLongPrefix u) { return u.toUnitValue(v); }
    },//@formatter:on

    KILO
    {//@formatter:off
        @Override
        public String toString() { return "K"; }
        
        @Override
        public long toUnitValue( long v ) { return x(v, (K/u), MAX/(K/u)); }

        @Override
        public long toKiloValue( long v ) { return v; }

        @Override
        public long toMegaValue( long v ) { return v/(M/K); }

        @Override
        public long toGigaValue( long v ) { return v/(G/K); }

        @Override
        public long toTeraValue( long v ) { return v/(T/K); }

        @Override
        public long convert(long v, MetricLongPrefix u) { return u.toKiloValue(v); }
    },//@formatter:on

    MEGA
    {//@formatter:off
        @Override
        public String toString() { return "M"; }
        
        @Override
        public long toUnitValue( long v ) { return x(v, (M/u), MAX/(M/u)); }

        @Override
        public long toKiloValue( long v ) { return x(v, (M/K), MAX/(M/K)); }

        @Override
        public long toMegaValue( long v ) { return v; }

        @Override
        public long toGigaValue( long v ) { return v/(G/M); }

        @Override
        public long toTeraValue( long v ) { return v/(T/M); }

        @Override
        public long convert(long v, MetricLongPrefix u) { return u.toMegaValue(v); }
    },//@formatter:on

    GIGA
    {//@formatter:off
        @Override
        public String toString() { return "G"; }
        
        @Override
        public long toUnitValue( long v ) { return x(v, (G/u), MAX/(G/u)); }

        @Override
        public long toKiloValue( long v ) { return x(v, (G/K), MAX/(G/K)); }

        @Override
        public long toMegaValue( long v ) { return x(v, (G/M), MAX/(G/M)); }

        @Override
        public long toGigaValue( long v ) { return v; }

        @Override
        public long toTeraValue( long v ) { return v/(T/G); }

        @Override
        public long convert(long v, MetricLongPrefix u) { return u.toGigaValue(v); }
    },//@formatter:on

    TERA
    {//@formatter:off
        @Override
        public String toString() { return "T"; }
        
        @Override
        public long toUnitValue( long v ) { return x(v, (T/u), MAX/(T/u)); }

        @Override
        public long toKiloValue( long v ) { return x(v, (T/K), MAX/(T/K)); }

        @Override
        public long toMegaValue( long v ) { return x(v, (T/M), MAX/(T/M)); }

        @Override
        public long toGigaValue( long v ) { return x(v, (T/G), MAX/(T/G)); }

        @Override
        public long toTeraValue( long v ) { return v; }

        @Override
        public long convert(long v, MetricLongPrefix u) { return u.toTeraValue(v); }
    };//@formatter:on

    private static final long u = 1L;
    private static final long K = 1000L * u;
    private static final long M = 1000L * K;
    private static final long G = 1000L * M;
    private static final long T = 1000L * G;

    private static final long MAX = Long.MAX_VALUE;

    /**
     * Scale d by m, checking for overflow.
     * This has a short name to make above code more readable.
     */
    private static long x( long d, long m, long over )
    {
        if (d > over) return Long.MAX_VALUE;
        if (d < -over) return Long.MIN_VALUE;
        return d * m;
    }

    @Override
    public MetricLongPrefix asUnit()
    {
        return UNIT;
    }

    @Override
    public MetricLongPrefix asKilo()
    {
        return KILO;
    }

    @Override
    public MetricLongPrefix asMega()
    {
        return MEGA;
    }

    @Override
    public MetricLongPrefix asGiga()
    {
        return GIGA;
    }

    @Override
    public MetricLongPrefix asTera()
    {
        return TERA;
    }
}
