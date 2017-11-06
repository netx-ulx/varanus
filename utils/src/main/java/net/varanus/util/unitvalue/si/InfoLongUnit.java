package net.varanus.util.unitvalue.si;


import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;


/**
 * 
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public enum InfoLongUnit implements SIMultipleLongUnit<InfoLongUnit>
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
        public long toBits(long v) { return v; }

        @Override
        public long toKiloBits(long v) { return v/(Kb/b); }

        @Override
        public long toMegaBits(long v) { return v/(Mb/b); }

        @Override
        public long toGigaBits(long v) { return v/(Gb/b); }

        @Override
        public long toTeraBits(long v) { return v/(Tb/b); }

        @Override
        public long toBytes(long v) { return v/(B/b); }

        @Override
        public long toKiloBytes(long v) { return v/(KB/b); }

        @Override
        public long toMegaBytes(long v) { return v/(MB/b); }

        @Override
        public long toGigaBytes(long v) { return v/(GB/b); }

        @Override
        public long toTeraBytes(long v) { return v/(TB/b); }

        @Override
        public long convert(long v, InfoLongUnit u) { return u.toBits(v); }
        
        @Override
        public InfoDoubleUnit asDoubleUnit() { return InfoDoubleUnit.BITS; }
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
        public long toBits(long v) { return x(v, Kb/b, MAX/(Kb/b)); }
        
        @Override
        public long toKiloBits(long v) { return v; }
        
        @Override
        public long toMegaBits(long v) { return v/(Mb/Kb); }
        
        @Override
        public long toGigaBits(long v) { return v/(Gb/Kb); }
        
        @Override
        public long toTeraBits(long v) { return v/(Tb/Kb); }

        @Override
        public long toBytes(long v) { return x(v, Kb/B, MAX/(Kb/B)); }
        
        @Override
        public long toKiloBytes(long v) { return v/(KB/Kb); }
        
        @Override
        public long toMegaBytes(long v) { return v/(MB/Kb); }
        
        @Override
        public long toGigaBytes(long v) { return v/(GB/Kb); }
        
        @Override
        public long toTeraBytes(long v) { return v/(TB/Kb); }

        @Override
        public long convert(long v, InfoLongUnit u) { return u.toKiloBits(v); }
        
        @Override
        public InfoDoubleUnit asDoubleUnit() { return InfoDoubleUnit.KILOBITS; }
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
        public long toBits(long v) { return x(v, Mb/b, MAX/(Mb/b)); }
        
        @Override
        public long toKiloBits(long v) { return x(v, Mb/Kb, MAX/(Mb/Kb)); }
        
        @Override
        public long toMegaBits(long v) { return v; }
        
        @Override
        public long toGigaBits(long v) { return v/(Gb/Mb); }
        
        @Override
        public long toTeraBits(long v) { return v/(Tb/Mb); }

        @Override
        public long toBytes(long v) { return x(v, Mb/B, MAX/(Mb/B)); }
        
        @Override
        public long toKiloBytes(long v) { return x(v, Mb/KB, MAX/(Mb/KB)); }
        
        @Override
        public long toMegaBytes(long v) { return v/(MB/Mb); }
        
        @Override
        public long toGigaBytes(long v) { return v/(GB/Mb); }
        
        @Override
        public long toTeraBytes(long v) { return v/(TB/Mb); }

        @Override
        public long convert(long v, InfoLongUnit u) { return u.toMegaBits(v); }
        
        @Override
        public InfoDoubleUnit asDoubleUnit() { return InfoDoubleUnit.MEGABITS; }
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
        public long toBits(long v) { return x(v, Gb/b, MAX/(Gb/b)); }
        
        @Override
        public long toKiloBits(long v) { return x(v, Gb/Kb, MAX/(Gb/Kb)); }
        
        @Override
        public long toMegaBits(long v) { return x(v, Gb/Mb, MAX/(Gb/Mb)); }
        
        @Override
        public long toGigaBits(long v) { return v; }
        
        @Override
        public long toTeraBits(long v) { return v/(Tb/Gb); }

        @Override
        public long toBytes(long v) { return x(v, Gb/B, MAX/(Gb/B)); }
        
        @Override
        public long toKiloBytes(long v) { return x(v, Gb/KB, MAX/(Gb/KB)); }
        
        @Override
        public long toMegaBytes(long v) { return x(v, Gb/MB, MAX/(Gb/MB)); }
        
        @Override
        public long toGigaBytes(long v) { return v/(GB/Gb); }
        
        @Override
        public long toTeraBytes(long v) { return v/(TB/Gb); }

        @Override
        public long convert(long v, InfoLongUnit u) { return u.toGigaBits(v); }
        
        @Override
        public InfoDoubleUnit asDoubleUnit() { return InfoDoubleUnit.GIGABITS; }
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
        public long toBits(long v) { return x(v, Tb/b, MAX/(Tb/b)); }
        
        @Override
        public long toKiloBits(long v) { return x(v, Tb/Kb, MAX/(Tb/Kb)); }
        
        @Override
        public long toMegaBits(long v) { return x(v, Tb/Mb, MAX/(Tb/Mb)); }
        
        @Override
        public long toGigaBits(long v) { return x(v, Tb/Gb, MAX/(Tb/Gb)); }
        
        @Override
        public long toTeraBits(long v) { return v; }

        @Override
        public long toBytes(long v) { return x(v, Tb/B, MAX/(Tb/B)); }
        
        @Override
        public long toKiloBytes(long v) { return x(v, Tb/KB, MAX/(Tb/KB)); }
        
        @Override
        public long toMegaBytes(long v) { return x(v, Tb/MB, MAX/(Tb/MB)); }
        
        @Override
        public long toGigaBytes(long v) { return x(v, Tb/GB, MAX/(Tb/GB)); }
        
        @Override
        public long toTeraBytes(long v) { return v/(TB/Tb); }

        @Override
        public long convert(long v, InfoLongUnit u) { return u.toTeraBits(v); }
        
        @Override
        public InfoDoubleUnit asDoubleUnit() { return InfoDoubleUnit.TERABITS; }
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
        public long toBits(long v) { return x(v, B/b, MAX/(B/b)); }
        
        @Override
        public long toKiloBits(long v) { return v/(Kb/B); }
        
        @Override
        public long toMegaBits(long v) { return v/(Mb/B); }
        
        @Override
        public long toGigaBits(long v) { return v/(Gb/B); }
        
        @Override
        public long toTeraBits(long v) { return v/(Tb/B); }

        @Override
        public long toBytes(long v) { return v; }
        
        @Override
        public long toKiloBytes(long v) { return v/(KB/B); }
        
        @Override
        public long toMegaBytes(long v) { return v/(MB/B); }
        
        @Override
        public long toGigaBytes(long v) { return v/(GB/B); }
        
        @Override
        public long toTeraBytes(long v) { return v/(TB/B); }

        @Override
        public long convert(long v, InfoLongUnit u) { return u.toBytes(v); }
        
        @Override
        public InfoDoubleUnit asDoubleUnit() { return InfoDoubleUnit.BYTES; }
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
        public long toBits(long v) { return x(v, KB/b, MAX/(KB/b)); }
        
        @Override
        public long toKiloBits(long v) { return x(v, KB/Kb, MAX/(KB/Kb)); }
        
        @Override
        public long toMegaBits(long v) { return v/(Mb/KB); }
        
        @Override
        public long toGigaBits(long v) { return v/(Gb/KB); }
        
        @Override
        public long toTeraBits(long v) { return v/(Tb/KB); }

        @Override
        public long toBytes(long v) { return x(v, KB/B, MAX/(KB/B)); }
        
        @Override
        public long toKiloBytes(long v) { return v; }
        
        @Override
        public long toMegaBytes(long v) { return v/(MB/KB); }
        
        @Override
        public long toGigaBytes(long v) { return v/(GB/KB); }
        
        @Override
        public long toTeraBytes(long v) { return v/(TB/KB); }

        @Override
        public long convert(long v, InfoLongUnit u) { return u.toKiloBytes(v); }
        
        @Override
        public InfoDoubleUnit asDoubleUnit() { return InfoDoubleUnit.KILOBYTES; }
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
        public long toBits(long v) { return x(v, MB/b, MAX/(MB/b)); }
        
        @Override
        public long toKiloBits(long v) { return x(v, MB/Kb, MAX/(MB/Kb)); }
        
        @Override
        public long toMegaBits(long v) { return x(v, MB/Mb, MAX/(MB/Mb)); }
        
        @Override
        public long toGigaBits(long v) { return v/(Gb/MB); }
        
        @Override
        public long toTeraBits(long v) { return v/(Tb/MB); }

        @Override
        public long toBytes(long v) { return x(v, MB/B, MAX/(MB/B)); }
        
        @Override
        public long toKiloBytes(long v) { return x(v, MB/KB, MAX/(MB/KB)); }
        
        @Override
        public long toMegaBytes(long v) { return v; }
        
        @Override
        public long toGigaBytes(long v) { return v/(GB/MB); }
        
        @Override
        public long toTeraBytes(long v) { return v/(TB/MB); }

        @Override
        public long convert(long v, InfoLongUnit u) { return u.toMegaBytes(v); }
        
        @Override
        public InfoDoubleUnit asDoubleUnit() { return InfoDoubleUnit.MEGABYTES; }
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
        public long toBits(long v) { return x(v, GB/b, MAX/(GB/b)); }
        
        @Override
        public long toKiloBits(long v) { return x(v, GB/Kb, MAX/(GB/Kb)); }
        
        @Override
        public long toMegaBits(long v) { return x(v, GB/Mb, MAX/(GB/Mb)); }
        
        @Override
        public long toGigaBits(long v) { return x(v, GB/Gb, MAX/(GB/Gb)); }
        
        @Override
        public long toTeraBits(long v) { return v/(Tb/GB); }

        @Override
        public long toBytes(long v) { return x(v, GB/B, MAX/(GB/B)); }
        
        @Override
        public long toKiloBytes(long v) { return x(v, GB/KB, MAX/(GB/KB)); }
        
        @Override
        public long toMegaBytes(long v) { return x(v, GB/MB, MAX/(GB/MB)); }
        
        @Override
        public long toGigaBytes(long v) { return v; }
        
        @Override
        public long toTeraBytes(long v) { return v/(TB/GB); }

        @Override
        public long convert(long v, InfoLongUnit u) { return u.toGigaBytes(v); }
        
        @Override
        public InfoDoubleUnit asDoubleUnit() { return InfoDoubleUnit.GIGABYTES; }
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
        public long toBits(long v) { return x(v, TB/b, MAX/(TB/b)); }
        
        @Override
        public long toKiloBits(long v) { return x(v, TB/Kb, MAX/(TB/Kb)); }
        
        @Override
        public long toMegaBits(long v) { return x(v, TB/Mb, MAX/(TB/Mb)); }
        
        @Override
        public long toGigaBits(long v) { return x(v, TB/Gb, MAX/(TB/Gb)); }
        
        @Override
        public long toTeraBits(long v) { return x(v, TB/Tb, MAX/(TB/Tb)); }

        @Override
        public long toBytes(long v) { return x(v, TB/B, MAX/(TB/B)); }
        
        @Override
        public long toKiloBytes(long v) { return x(v, TB/KB, MAX/(TB/KB)); }
        
        @Override
        public long toMegaBytes(long v) { return x(v, TB/MB, MAX/(TB/MB)); }
        
        @Override
        public long toGigaBytes(long v) { return x(v, TB/GB, MAX/(TB/GB)); }
        
        @Override
        public long toTeraBytes(long v) { return v; }

        @Override
        public long convert(long v, InfoLongUnit u) { return u.toTeraBytes(v); }
        
        @Override
        public InfoDoubleUnit asDoubleUnit() { return InfoDoubleUnit.TERABYTES; }
    };//@formatter:on

    private static final long b  = 1L;
    private static final long Kb = 1000L * b;
    private static final long Mb = 1000L * Kb;
    private static final long Gb = 1000L * Mb;
    private static final long Tb = 1000L * Gb;

    private static final long B  = 8L * b;
    private static final long KB = 1000L * B;
    private static final long MB = 1000L * KB;
    private static final long GB = 1000L * MB;
    private static final long TB = 1000L * GB;

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

    public static InfoLongUnit fromDoubleUnit( InfoLongUnit unit )
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
                throw new IllegalArgumentException("unknown double unit");
        }
    }

    public abstract boolean isBitBased();

    public abstract boolean isByteBased();

    public abstract long toBits( long value );

    public abstract long toKiloBits( long value );

    public abstract long toMegaBits( long value );

    public abstract long toGigaBits( long value );

    public abstract long toTeraBits( long value );

    public abstract long toBytes( long value );

    public abstract long toKiloBytes( long value );

    public abstract long toMegaBytes( long value );

    public abstract long toGigaBytes( long value );

    public abstract long toTeraBytes( long value );

    public abstract InfoDoubleUnit asDoubleUnit();

    @Override
    public final long toUnitValue( long value )
    {
        if (isBitBased())
            return toBits(value);
        else if (isByteBased())
            return toBytes(value);
        else
            throw new AssertionError("unexpected");
    }

    @Override
    public final long toKiloValue( long value )
    {
        if (isBitBased())
            return toKiloBits(value);
        else if (isByteBased())
            return toKiloBytes(value);
        else
            throw new AssertionError("unexpected");
    }

    @Override
    public final long toMegaValue( long value )
    {
        if (isBitBased())
            return toMegaBits(value);
        else if (isByteBased())
            return toMegaBytes(value);
        else
            throw new AssertionError("unexpected");
    }

    @Override
    public final long toGigaValue( long value )
    {
        if (isBitBased())
            return toGigaBits(value);
        else if (isByteBased())
            return toGigaBytes(value);
        else
            throw new AssertionError("unexpected");
    }

    @Override
    public final long toTeraValue( long value )
    {
        if (isBitBased())
            return toTeraBits(value);
        else if (isByteBased())
            return toTeraBytes(value);
        else
            throw new AssertionError("unexpected");
    }

    @Override
    public InfoLongUnit asUnit()
    {
        if (isBitBased())
            return BITS;
        else if (isByteBased())
            return BYTES;
        else
            throw new AssertionError("unexpected");
    }

    @Override
    public InfoLongUnit asKilo()
    {
        if (isBitBased())
            return KILOBITS;
        else if (isByteBased())
            return KILOBYTES;
        else
            throw new AssertionError("unexpected");
    }

    @Override
    public InfoLongUnit asMega()
    {
        if (isBitBased())
            return MEGABITS;
        else if (isByteBased())
            return MEGABYTES;
        else
            throw new AssertionError("unexpected");
    }

    @Override
    public InfoLongUnit asGiga()
    {
        if (isBitBased())
            return GIGABITS;
        else if (isByteBased())
            return GIGABYTES;
        else
            throw new AssertionError("unexpected");
    }

    @Override
    public InfoLongUnit asTera()
    {
        if (isBitBased())
            return TERABITS;
        else if (isByteBased())
            return TERABYTES;
        else
            throw new AssertionError("unexpected");
    }
}
