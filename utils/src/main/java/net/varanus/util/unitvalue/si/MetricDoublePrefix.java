package net.varanus.util.unitvalue.si;


import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;


/**
 * 
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public enum MetricDoublePrefix implements SIMultipleDoubleUnit<MetricDoublePrefix>
{
    UNIT
    {//@formatter:off
        @Override
        public String toString() { return ""; }
        
        @Override
        public double toUnitValue( double v ) { return v; }

        @Override
        public double toKiloValue( double v ) { return v/(K/u); }

        @Override
        public double toMegaValue( double v ) { return v/(M/u); }

        @Override
        public double toGigaValue( double v ) { return v/(G/u); }

        @Override
        public double toTeraValue( double v ) { return v/(T/u); }

        @Override
        public double convert(double v, MetricDoublePrefix u) { return u.toUnitValue(v); }
    },//@formatter:on

    KILO
    {//@formatter:off
        @Override
        public String toString() { return "K"; }
        
        @Override
        public double toUnitValue( double v ) { return v * (K/u); }

        @Override
        public double toKiloValue( double v ) { return v; }

        @Override
        public double toMegaValue( double v ) { return v/(M/K); }

        @Override
        public double toGigaValue( double v ) { return v/(G/K); }

        @Override
        public double toTeraValue( double v ) { return v/(T/K); }

        @Override
        public double convert(double v, MetricDoublePrefix u) { return u.toKiloValue(v); }
    },//@formatter:on

    MEGA
    {//@formatter:off
        @Override
        public String toString() { return "M"; }
        
        @Override
        public double toUnitValue( double v ) { return v * (M/u); }

        @Override
        public double toKiloValue( double v ) { return v * (M/K); }

        @Override
        public double toMegaValue( double v ) { return v; }

        @Override
        public double toGigaValue( double v ) { return v/(G/M); }

        @Override
        public double toTeraValue( double v ) { return v/(T/M); }

        @Override
        public double convert(double v, MetricDoublePrefix u) { return u.toMegaValue(v); }
    },//@formatter:on

    GIGA
    {//@formatter:off
        @Override
        public String toString() { return "G"; }
        
        @Override
        public double toUnitValue( double v ) { return v * (G/u); }

        @Override
        public double toKiloValue( double v ) { return v * (G/K); }

        @Override
        public double toMegaValue( double v ) { return v * (G/M); }

        @Override
        public double toGigaValue( double v ) { return v; }

        @Override
        public double toTeraValue( double v ) { return v/(T/G); }

        @Override
        public double convert(double v, MetricDoublePrefix u) { return u.toGigaValue(v); }
    },//@formatter:on

    TERA
    {//@formatter:off
        @Override
        public String toString() { return "T"; }
        
        @Override
        public double toUnitValue( double v ) { return v * (T/u); }

        @Override
        public double toKiloValue( double v ) { return v * (T/K); }

        @Override
        public double toMegaValue( double v ) { return v * (T/M); }

        @Override
        public double toGigaValue( double v ) { return v * (T/G); }

        @Override
        public double toTeraValue( double v ) { return v; }

        @Override
        public double convert(double v, MetricDoublePrefix u) { return u.toTeraValue(v); }
    };//@formatter:on

    private static final double u = 1d;
    private static final double K = 1000d * u;
    private static final double M = 1000d * K;
    private static final double G = 1000d * M;
    private static final double T = 1000d * G;

    @Override
    public MetricDoublePrefix asUnit()
    {
        return UNIT;
    }

    @Override
    public MetricDoublePrefix asKilo()
    {
        return KILO;
    }

    @Override
    public MetricDoublePrefix asMega()
    {
        return MEGA;
    }

    @Override
    public MetricDoublePrefix asGiga()
    {
        return GIGA;
    }

    @Override
    public MetricDoublePrefix asTera()
    {
        return TERA;
    }
}
