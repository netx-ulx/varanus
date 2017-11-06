package net.varanus.util.time;


import java.util.concurrent.TimeUnit;

import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;


/**
 * 
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public enum TimeDoubleUnit
{
    // Copied mostly from java.util.concurrent.TimeUnit class

    NANOSECONDS
    {//@formatter:off
        @Override
        public double toNanos(double d)   { return d; }
        @Override
        public double toMicros(double d)  { return d / (C1/C0); }
        @Override
        public double toMillis(double d)  { return d / (C2/C0); }
        @Override
        public double toSeconds(double d) { return d / (C3/C0); }
        @Override
        public double toMinutes(double d) { return d / (C4/C0); }
        @Override
        public double toHours(double d)   { return d / (C5/C0); }
        @Override
        public double toDays(double d)    { return d / (C6/C0); }
        @Override
        public double convert(double d, TimeDoubleUnit u) { return u.toNanos(d); }
        @Override
        public TimeUnit toTimeUnit() { return TimeUnit.NANOSECONDS; }
    },//@formatter:on
    MICROSECONDS
    {//@formatter:off
        @Override
        public double toNanos(double d)   { return d * (C1/C0); }
        @Override
        public double toMicros(double d)  { return d; }
        @Override
        public double toMillis(double d)  { return d / (C2/C1); }
        @Override
        public double toSeconds(double d) { return d / (C3/C1); }
        @Override
        public double toMinutes(double d) { return d / (C4/C1); }
        @Override
        public double toHours(double d)   { return d / (C5/C1); }
        @Override
        public double toDays(double d)    { return d / (C6/C1); }
        @Override
        public double convert(double d, TimeDoubleUnit u) { return u.toMicros(d); }
        @Override
        public TimeUnit toTimeUnit() { return TimeUnit.MICROSECONDS; }
    },//@formatter:on
    MILLISECONDS
    {//@formatter:off
        @Override
        public double toNanos(double d)   { return d * (C2/C0); }
        @Override
        public double toMicros(double d)  { return d * (C2/C1); }
        @Override
        public double toMillis(double d)  { return d; }
        @Override
        public double toSeconds(double d) { return d / (C3/C2); }
        @Override
        public double toMinutes(double d) { return d / (C4/C2); }
        @Override
        public double toHours(double d)   { return d / (C5/C2); }
        @Override
        public double toDays(double d)    { return d / (C6/C2); }
        @Override
        public double convert(double d, TimeDoubleUnit u) { return u.toMillis(d); }
        @Override
        public TimeUnit toTimeUnit() { return TimeUnit.MILLISECONDS; }
    },//@formatter:on
    SECONDS
    {//@formatter:off
        @Override
        public double toNanos(double d)   { return d * (C3/C0); }
        @Override
        public double toMicros(double d)  { return d * (C3/C1); }
        @Override
        public double toMillis(double d)  { return d * (C3/C2); }
        @Override
        public double toSeconds(double d) { return d; }
        @Override
        public double toMinutes(double d) { return d / (C4/C3); }
        @Override
        public double toHours(double d)   { return d / (C5/C3); }
        @Override
        public double toDays(double d)    { return d / (C6/C3); }
        @Override
        public double convert(double d, TimeDoubleUnit u) { return u.toSeconds(d); }
        @Override
        public TimeUnit toTimeUnit() { return TimeUnit.SECONDS; }
    },//@formatter:on
    MINUTES
    {//@formatter:off
        @Override
        public double toNanos(double d)   { return d * (C4/C0); }
        @Override
        public double toMicros(double d)  { return d * (C4/C1); }
        @Override
        public double toMillis(double d)  { return d * (C4/C2); }
        @Override
        public double toSeconds(double d) { return d * (C4/C3); }
        @Override
        public double toMinutes(double d) { return d; }
        @Override
        public double toHours(double d)   { return d / (C5/C4); }
        @Override
        public double toDays(double d)    { return d / (C6/C4); }
        @Override
        public double convert(double d, TimeDoubleUnit u) { return u.toMinutes(d); }
        @Override
        public TimeUnit toTimeUnit() { return TimeUnit.MINUTES; }
    },//@formatter:on
    HOURS
    {//@formatter:off
        @Override
        public double toNanos(double d)   { return d * (C5/C0); }
        @Override
        public double toMicros(double d)  { return d * (C5/C1); }
        @Override
        public double toMillis(double d)  { return d * (C5/C2); }
        @Override
        public double toSeconds(double d) { return d * (C5/C3); }
        @Override
        public double toMinutes(double d) { return d * (C5/C4); }
        @Override
        public double toHours(double d)   { return d; }
        @Override
        public double toDays(double d)    { return d / (C6/C5); }
        @Override
        public double convert(double d, TimeDoubleUnit u) { return u.toHours(d); }
        @Override
        public TimeUnit toTimeUnit() { return TimeUnit.HOURS; }
    },//@formatter:on
    DAYS
    {//@formatter:off
        @Override
        public double toNanos(double d)   { return d * (C6/C0); }
        @Override
        public double toMicros(double d)  { return d * (C6/C1); }
        @Override
        public double toMillis(double d)  { return d * (C6/C2); }
        @Override
        public double toSeconds(double d) { return d * (C6/C3); }
        @Override
        public double toMinutes(double d) { return d * (C6/C4); }
        @Override
        public double toHours(double d)   { return d * (C6/C5); }
        @Override
        public double toDays(double d)    { return d; }
        @Override
        public double convert(double d, TimeDoubleUnit u) { return u.toDays(d); }
        @Override
        public TimeUnit toTimeUnit() { return TimeUnit.DAYS; }
    };//@formatter:on

    // Handy constants for conversion methods
    private static final double C0 = 1d;
    private static final double C1 = C0 * 1000d;
    private static final double C2 = C1 * 1000d;
    private static final double C3 = C2 * 1000d;
    private static final double C4 = C3 * 60d;
    private static final double C5 = C4 * 60d;
    private static final double C6 = C5 * 24d;

    public static TimeDoubleUnit fromTimeUnit( TimeUnit unit )
    {
        switch (unit) {
            case NANOSECONDS:
                return NANOSECONDS;

            case MICROSECONDS:
                return MICROSECONDS;

            case MILLISECONDS:
                return MILLISECONDS;

            case SECONDS:
                return SECONDS;

            case MINUTES:
                return MINUTES;

            case HOURS:
                return HOURS;

            case DAYS:
                return DAYS;

            default:
                throw new IllegalArgumentException("unexpected TimeUnit value");
        }
    }

    public abstract double convert( double sourceDuration, TimeDoubleUnit sourceUnit );

    public abstract double toNanos( double duration );

    public abstract double toMicros( double duration );

    public abstract double toMillis( double duration );

    public abstract double toSeconds( double duration );

    public abstract double toMinutes( double duration );

    public abstract double toHours( double duration );

    public abstract double toDays( double duration );

    public abstract TimeUnit toTimeUnit();

    public long convertAsLong( double sourceDuration, TimeDoubleUnit sourceUnit )
    {
        return asLong(convert(sourceDuration, sourceUnit));
    }

    public long toNanosAsLong( double duration )
    {
        return asLong(toNanos(duration));
    }

    public long toMicrosAsLong( double duration )
    {
        return asLong(toMicros(duration));
    }

    public long toMillisAsLong( double duration )
    {
        return asLong(toMillis(duration));
    }

    public long toSecondsAsLong( double duration )
    {
        return asLong(toSeconds(duration));
    }

    public long toMinutesAsLong( double duration )
    {
        return asLong(toMinutes(duration));
    }

    public long toHoursAsLong( double duration )
    {
        return asLong(toHours(duration));
    }

    public long toDaysAsLong( double duration )
    {
        return asLong(toDays(duration));
    }

    public void timedWait( Object obj, double timeout ) throws InterruptedException
    {
        TimeUnit.NANOSECONDS.timedWait(obj, toNanosAsLong(timeout));
    }

    public void timedJoin( Thread thread, double timeout ) throws InterruptedException
    {
        TimeUnit.NANOSECONDS.timedJoin(thread, toNanosAsLong(timeout));
    }

    public void sleep( double timeout ) throws InterruptedException
    {
        TimeUnit.NANOSECONDS.sleep(toNanosAsLong(timeout));
    }

    private static long asLong( double duration )
    {
        return Math.round(duration);
    }
}
