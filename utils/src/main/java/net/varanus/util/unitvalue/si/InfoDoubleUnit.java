package net.varanus.util.unitvalue.si;


import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;


/**
 * 
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public enum InfoDoubleUnit implements SIMultipleDoubleUnit<InfoDoubleUnit>
{
    BITS
    {//@formatter:off
        @Override
        public String toString() { return "b"; }
        
        @Override
        public boolean isBitBased() { return true; }
        
        @Override
        public boolean isByteBased() { return false; }

        @Override
        public double toBits(double v) { return v; }

        @Override
        public double toKiloBits(double v) { return v/(Kb/b); }

        @Override
        public double toMegaBits(double v) { return v/(Mb/b); }

        @Override
        public double toGigaBits(double v) { return v/(Gb/b); }

        @Override
        public double toTeraBits(double v) { return v/(Tb/b); }

        @Override
        public double toBytes(double v) { return v/(B/b); }

        @Override
        public double toKiloBytes(double v) { return v/(KB/b); }

        @Override
        public double toMegaBytes(double v) { return v/(MB/b); }

        @Override
        public double toGigaBytes(double v) { return v/(GB/b); }

        @Override
        public double toTeraBytes(double v) { return v/(TB/b); }

        @Override
        public double convert(double v, InfoDoubleUnit u) { return u.toBits(v); }
        
        @Override
        public InfoLongUnit asLongUnit() { return InfoLongUnit.BITS; }
    },//@formatter:on

    KILOBITS
    {//@formatter:off
        @Override
        public String toString() { return "Kb"; }
        
        @Override
        public boolean isBitBased() { return true; }
        
        @Override
        public boolean isByteBased() { return false; }

        @Override
        public double toBits(double v) { return v * (Kb/b); }
        
        @Override
        public double toKiloBits(double v) { return v; }
        
        @Override
        public double toMegaBits(double v) { return v/(Mb/Kb); }
        
        @Override
        public double toGigaBits(double v) { return v/(Gb/Kb); }
        
        @Override
        public double toTeraBits(double v) { return v/(Tb/Kb); }

        @Override
        public double toBytes(double v) { return v * (Kb/B); }
        
        @Override
        public double toKiloBytes(double v) { return v/(KB/Kb); }
        
        @Override
        public double toMegaBytes(double v) { return v/(MB/Kb); }
        
        @Override
        public double toGigaBytes(double v) { return v/(GB/Kb); }
        
        @Override
        public double toTeraBytes(double v) { return v/(TB/Kb); }

        @Override
        public double convert(double v, InfoDoubleUnit u) { return u.toKiloBits(v); }
        
        @Override
        public InfoLongUnit asLongUnit() { return InfoLongUnit.KILOBITS; }
    },//@formatter:on

    MEGABITS
    {//@formatter:off
        @Override
        public String toString() { return "Mb"; }
        
        @Override
        public boolean isBitBased() { return true; }
        
        @Override
        public boolean isByteBased() { return false; }

        @Override
        public double toBits(double v) { return v * (Mb/b); }
        
        @Override
        public double toKiloBits(double v) { return v * (Mb/Kb); }
        
        @Override
        public double toMegaBits(double v) { return v; }
        
        @Override
        public double toGigaBits(double v) { return v/(Gb/Mb); }
        
        @Override
        public double toTeraBits(double v) { return v/(Tb/Mb); }

        @Override
        public double toBytes(double v) { return v * (Mb/B); }
        
        @Override
        public double toKiloBytes(double v) { return v * (Mb/KB); }
        
        @Override
        public double toMegaBytes(double v) { return v/(MB/Mb); }
        
        @Override
        public double toGigaBytes(double v) { return v/(GB/Mb); }
        
        @Override
        public double toTeraBytes(double v) { return v/(TB/Mb); }

        @Override
        public double convert(double v, InfoDoubleUnit u) { return u.toMegaBits(v); }
        
        @Override
        public InfoLongUnit asLongUnit() { return InfoLongUnit.MEGABITS; }
    },//@formatter:on

    GIGABITS
    {//@formatter:off
        @Override
        public String toString() { return "Gb"; }
        
        @Override
        public boolean isBitBased() { return true; }
        
        @Override
        public boolean isByteBased() { return false; }

        @Override
        public double toBits(double v) { return v * (Gb/b); }
        
        @Override
        public double toKiloBits(double v) { return v * (Gb/Kb); }
        
        @Override
        public double toMegaBits(double v) { return v * (Gb/Mb); }
        
        @Override
        public double toGigaBits(double v) { return v; }
        
        @Override
        public double toTeraBits(double v) { return v/(Tb/Gb); }

        @Override
        public double toBytes(double v) { return v * (Gb/B); }
        
        @Override
        public double toKiloBytes(double v) { return v * (Gb/KB); }
        
        @Override
        public double toMegaBytes(double v) { return v * (Gb/MB); }
        
        @Override
        public double toGigaBytes(double v) { return v/(GB/Gb); }
        
        @Override
        public double toTeraBytes(double v) { return v/(TB/Gb); }

        @Override
        public double convert(double v, InfoDoubleUnit u) { return u.toGigaBits(v); }
        
        @Override
        public InfoLongUnit asLongUnit() { return InfoLongUnit.GIGABITS; }
    },//@formatter:on

    TERABITS
    {//@formatter:off
        @Override
        public String toString() { return "Tb"; }
        
        @Override
        public boolean isBitBased() { return true; }
        
        @Override
        public boolean isByteBased() { return false; }

        @Override
        public double toBits(double v) { return v * (Tb/b); }
        
        @Override
        public double toKiloBits(double v) { return v * (Tb/Kb); }
        
        @Override
        public double toMegaBits(double v) { return v * (Tb/Mb); }
        
        @Override
        public double toGigaBits(double v) { return v * (Tb/Gb); }
        
        @Override
        public double toTeraBits(double v) { return v; }

        @Override
        public double toBytes(double v) { return v * (Tb/B); }
        
        @Override
        public double toKiloBytes(double v) { return v * (Tb/KB); }
        
        @Override
        public double toMegaBytes(double v) { return v * (Tb/MB); }
        
        @Override
        public double toGigaBytes(double v) { return v * (Tb/GB); }
        
        @Override
        public double toTeraBytes(double v) { return v/(TB/Tb); }

        @Override
        public double convert(double v, InfoDoubleUnit u) { return u.toTeraBits(v); }
        
        @Override
        public InfoLongUnit asLongUnit() { return InfoLongUnit.TERABITS; }
    },//@formatter:on

    BYTES
    {//@formatter:off
        @Override
        public String toString() { return "B"; }
        
        @Override
        public boolean isBitBased() { return false; }
        
        @Override
        public boolean isByteBased() { return true; }

        @Override
        public double toBits(double v) { return v * (B/b); }
        
        @Override
        public double toKiloBits(double v) { return v/(Kb/B); }
        
        @Override
        public double toMegaBits(double v) { return v/(Mb/B); }
        
        @Override
        public double toGigaBits(double v) { return v/(Gb/B); }
        
        @Override
        public double toTeraBits(double v) { return v/(Tb/B); }

        @Override
        public double toBytes(double v) { return v; }
        
        @Override
        public double toKiloBytes(double v) { return v/(KB/B); }
        
        @Override
        public double toMegaBytes(double v) { return v/(MB/B); }
        
        @Override
        public double toGigaBytes(double v) { return v/(GB/B); }
        
        @Override
        public double toTeraBytes(double v) { return v/(TB/B); }

        @Override
        public double convert(double v, InfoDoubleUnit u) { return u.toBytes(v); }
        
        @Override
        public InfoLongUnit asLongUnit() { return InfoLongUnit.BYTES; }
    },//@formatter:on

    KILOBYTES
    {//@formatter:off
        @Override
        public String toString() { return "KB"; }
        
        @Override
        public boolean isBitBased() { return false; }
        
        @Override
        public boolean isByteBased() { return true; }

        @Override
        public double toBits(double v) { return v * (KB/b); }
        
        @Override
        public double toKiloBits(double v) { return v * (KB/Kb); }
        
        @Override
        public double toMegaBits(double v) { return v/(Mb/KB); }
        
        @Override
        public double toGigaBits(double v) { return v/(Gb/KB); }
        
        @Override
        public double toTeraBits(double v) { return v/(Tb/KB); }

        @Override
        public double toBytes(double v) { return v * (KB/B); }
        
        @Override
        public double toKiloBytes(double v) { return v; }
        
        @Override
        public double toMegaBytes(double v) { return v/(MB/KB); }
        
        @Override
        public double toGigaBytes(double v) { return v/(GB/KB); }
        
        @Override
        public double toTeraBytes(double v) { return v/(TB/KB); }

        @Override
        public double convert(double v, InfoDoubleUnit u) { return u.toKiloBytes(v); }
        
        @Override
        public InfoLongUnit asLongUnit() { return InfoLongUnit.KILOBYTES; }
    },//@formatter:on

    MEGABYTES
    {//@formatter:off
        @Override
        public String toString() { return "MB"; }
        
        @Override
        public boolean isBitBased() { return false; }
        
        @Override
        public boolean isByteBased() { return true; }

        @Override
        public double toBits(double v) { return v * (MB/b); }
        
        @Override
        public double toKiloBits(double v) { return v * (MB/Kb); }
        
        @Override
        public double toMegaBits(double v) { return v * (MB/Mb); }
        
        @Override
        public double toGigaBits(double v) { return v/(Gb/MB); }
        
        @Override
        public double toTeraBits(double v) { return v/(Tb/MB); }

        @Override
        public double toBytes(double v) { return v * (MB/B); }
        
        @Override
        public double toKiloBytes(double v) { return v * (MB/KB); }
        
        @Override
        public double toMegaBytes(double v) { return v; }
        
        @Override
        public double toGigaBytes(double v) { return v/(GB/MB); }
        
        @Override
        public double toTeraBytes(double v) { return v/(TB/MB); }

        @Override
        public double convert(double v, InfoDoubleUnit u) { return u.toMegaBytes(v); }
        
        @Override
        public InfoLongUnit asLongUnit() { return InfoLongUnit.MEGABYTES; }
    },//@formatter:on

    GIGABYTES
    {//@formatter:off
        @Override
        public String toString() { return "GB"; }
        
        @Override
        public boolean isBitBased() { return false; }
        
        @Override
        public boolean isByteBased() { return true; }

        @Override
        public double toBits(double v) { return v * (GB/b); }
        
        @Override
        public double toKiloBits(double v) { return v * (GB/Kb); }
        
        @Override
        public double toMegaBits(double v) { return v * (GB/Mb); }
        
        @Override
        public double toGigaBits(double v) { return v * (GB/Gb); }
        
        @Override
        public double toTeraBits(double v) { return v/(Tb/GB); }

        @Override
        public double toBytes(double v) { return v * (GB/B); }
        
        @Override
        public double toKiloBytes(double v) { return v * (GB/KB); }
        
        @Override
        public double toMegaBytes(double v) { return v * (GB/MB); }
        
        @Override
        public double toGigaBytes(double v) { return v; }
        
        @Override
        public double toTeraBytes(double v) { return v/(TB/GB); }

        @Override
        public double convert(double v, InfoDoubleUnit u) { return u.toGigaBytes(v); }
        
        @Override
        public InfoLongUnit asLongUnit() { return InfoLongUnit.GIGABYTES; }
    },//@formatter:on

    TERABYTES
    {//@formatter:off
        @Override
        public String toString() { return "TB"; }
        
        @Override
        public boolean isBitBased() { return false; }
        
        @Override
        public boolean isByteBased() { return true; }

        @Override
        public double toBits(double v) { return v * (TB/b); }
        
        @Override
        public double toKiloBits(double v) { return v * (TB/Kb); }
        
        @Override
        public double toMegaBits(double v) { return v * (TB/Mb); }
        
        @Override
        public double toGigaBits(double v) { return v * (TB/Gb); }
        
        @Override
        public double toTeraBits(double v) { return v * (TB/Tb); }

        @Override
        public double toBytes(double v) { return v * (TB/B); }
        
        @Override
        public double toKiloBytes(double v) { return v * (TB/KB); }
        
        @Override
        public double toMegaBytes(double v) { return v * (TB/MB); }
        
        @Override
        public double toGigaBytes(double v) { return v * (TB/GB); }
        
        @Override
        public double toTeraBytes(double v) { return v; }

        @Override
        public double convert(double v, InfoDoubleUnit u) { return u.toTeraBytes(v); }
        
        @Override
        public InfoLongUnit asLongUnit() { return InfoLongUnit.TERABYTES; }
    };//@formatter:on

    private static final double b  = 1d;
    private static final double Kb = 1000d * b;
    private static final double Mb = 1000d * Kb;
    private static final double Gb = 1000d * Mb;
    private static final double Tb = 1000d * Gb;

    private static final double B  = 8d * b;
    private static final double KB = 1000d * B;
    private static final double MB = 1000d * KB;
    private static final double GB = 1000d * MB;
    private static final double TB = 1000d * GB;

    public static InfoDoubleUnit fromLongUnit( InfoLongUnit unit )
    {
        switch (unit) {
            case BITS:
                return BITS;

            case KILOBITS:
                return KILOBITS;

            case MEGABITS:
                return MEGABITS;

            case GIGABITS:
                return GIGABITS;

            case TERABITS:
                return TERABITS;

            case BYTES:
                return BYTES;

            case KILOBYTES:
                return KILOBYTES;

            case MEGABYTES:
                return MEGABYTES;

            case GIGABYTES:
                return GIGABYTES;

            case TERABYTES:
                return TERABYTES;

            default:
                throw new IllegalArgumentException("unknown long unit");
        }
    }

    public abstract boolean isBitBased();

    public abstract boolean isByteBased();

    public abstract double toBits( double value );

    public abstract double toKiloBits( double value );

    public abstract double toMegaBits( double value );

    public abstract double toGigaBits( double value );

    public abstract double toTeraBits( double value );

    public abstract double toBytes( double value );

    public abstract double toKiloBytes( double value );

    public abstract double toMegaBytes( double value );

    public abstract double toGigaBytes( double value );

    public abstract double toTeraBytes( double value );

    public abstract InfoLongUnit asLongUnit();

    @Override
    public final double toUnitValue( double value )
    {
        if (isBitBased())
            return toBits(value);
        else if (isByteBased())
            return toBytes(value);
        else
            throw new AssertionError("unexpected");
    }

    @Override
    public final double toKiloValue( double value )
    {
        if (isBitBased())
            return toKiloBits(value);
        else if (isByteBased())
            return toKiloBytes(value);
        else
            throw new AssertionError("unexpected");
    }

    @Override
    public final double toMegaValue( double value )
    {
        if (isBitBased())
            return toMegaBits(value);
        else if (isByteBased())
            return toMegaBytes(value);
        else
            throw new AssertionError("unexpected");
    }

    @Override
    public final double toGigaValue( double value )
    {
        if (isBitBased())
            return toGigaBits(value);
        else if (isByteBased())
            return toGigaBytes(value);
        else
            throw new AssertionError("unexpected");
    }

    @Override
    public final double toTeraValue( double value )
    {
        if (isBitBased())
            return toTeraBits(value);
        else if (isByteBased())
            return toTeraBytes(value);
        else
            throw new AssertionError("unexpected");
    }

    @Override
    public InfoDoubleUnit asUnit()
    {
        if (isBitBased())
            return BITS;
        else if (isByteBased())
            return BYTES;
        else
            throw new AssertionError("unexpected");
    }

    @Override
    public InfoDoubleUnit asKilo()
    {
        if (isBitBased())
            return KILOBITS;
        else if (isByteBased())
            return KILOBYTES;
        else
            throw new AssertionError("unexpected");
    }

    @Override
    public InfoDoubleUnit asMega()
    {
        if (isBitBased())
            return MEGABITS;
        else if (isByteBased())
            return MEGABYTES;
        else
            throw new AssertionError("unexpected");
    }

    @Override
    public InfoDoubleUnit asGiga()
    {
        if (isBitBased())
            return GIGABITS;
        else if (isByteBased())
            return GIGABYTES;
        else
            throw new AssertionError("unexpected");
    }

    @Override
    public InfoDoubleUnit asTera()
    {
        if (isBitBased())
            return TERABITS;
        else if (isByteBased())
            return TERABYTES;
        else
            throw new AssertionError("unexpected");
    }
}
