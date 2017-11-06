package net.varanus.util.time;


import static net.varanus.util.time.TimeDoubleUnit.NANOSECONDS;

import java.time.Duration;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.functional.PossibleDouble;
import net.varanus.util.functional.functions.ObjDoubleFunction;
import net.varanus.util.unitvalue.AbstractUnitDoubleValue;


/**
 * 
 */
@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class TimeDouble extends AbstractUnitDoubleValue<TimeDoubleUnit, TimeDouble>
{
    private static final int DEFAULT_TO_STRING_PRECISION = 3;

    private static final TimeDouble ABSENT = new TimeDouble(0, false);
    public static final TimeDouble  ZERO   = new TimeDouble(0, true);

    public static TimeDouble absent()
    {
        return ABSENT;
    }

    public static TimeDouble ofNanos( double durationNanos )
    {
        return new TimeDouble(durationNanos, true);
    }

    public static TimeDouble of( double duration, TimeDoubleUnit unit )
    {
        return new TimeDouble(unit.toNanos(duration), true);
    }

    public static TimeDouble ofPossibleNanos( PossibleDouble durationNanos )
    {
        return durationNanos.mapToObj(TimeDouble::ofNanos).orElse(absent());
    }

    public static TimeDouble ofPossible( PossibleDouble duration, TimeDoubleUnit unit )
    {
        return duration.mapToObj(d -> TimeDouble.of(d, unit)).orElse(absent());
    }

    public static TimeDouble fromDuration( Duration duration )
    {
        return new TimeDouble(duration.toNanos(), true);
    }

    public static TimeDouble parseNanos( String s )
    {
        return ofNanos(Double.parseDouble(s));
    }

    public static TimeDouble parse( String s, TimeDoubleUnit unit )
    {
        return of(Double.parseDouble(s), unit);
    }

    private TimeDouble( double valNanos, boolean isPresent )
    {
        super(valNanos, isPresent);
    }

    public double inNanos()
    {
        return toBaseValue();
    }

    public PossibleDouble asPossibleInNanos()
    {
        return asPossibleBase();
    }

    public TimeLong asLong()
    {
        return isPresent() ? TimeLong.ofNanos(inNanosAsLong())
                           : TimeLong.absent();
    }

    public Duration asDuration()
    {
        return Duration.ofNanos(inNanosAsLong());
    }

    public void sleep() throws InterruptedException
    {
        NANOSECONDS.sleep(inNanos());
    }

    public void timedWait( Object obj ) throws InterruptedException
    {
        NANOSECONDS.timedWait(obj, inNanos());
    }

    public void timedJoin( Thread thread ) throws InterruptedException
    {
        NANOSECONDS.timedJoin(thread, inNanos());
    }

    @Override
    public boolean equals( Object other )
    {
        return (other instanceof TimeDouble)
               && super.equals(other);
    }

    public boolean equals( TimeDouble other )
    {
        return super.equals(other);
    }

    @Override
    public int hashCode()
    {
        return super.hashCode();
    }

    @Override
    public String toString()
    {
        if (this.isPresent())
            return TimeUtils.toSmartDurationString(toBaseValue());
        else
            return toString(NANOSECONDS);
    }

    @Override
    public String toString( TimeDoubleUnit unit )
    {
        return toString(unit, DEFAULT_TO_STRING_PRECISION, "absent");
    }

    @Override
    public String toString( TimeDoubleUnit unit, String absentString )
    {
        return toString(unit, DEFAULT_TO_STRING_PRECISION, absentString);
    }

    public String toString( TimeDoubleUnit unit, int precision )
    {
        return toString(unit, precision, "absent");
    }

    public String toString( TimeDoubleUnit unit, final int precision, String absentString )
    {
        validatePrecision(precision);
        return toString(
            unit,
            pair -> String.format("%." + precision + "f %s", pair.value(), TimeUtils.abbreviatedUnit(pair.unit())),
            absentString);
    }

    @Override
    public ObjDoubleFunction<TimeDoubleUnit, UDoublePair<TimeDoubleUnit>> pairFactory()
    {
        return ( unit, value ) -> new SimpleUDoublePair<TimeDoubleUnit>(unit, value) {

            @Override
            public String toString()
            {
                return String.format("%s %s", value(), TimeUtils.abbreviatedUnit(unit()));
            }
        };
    }

    @Override
    protected double convertTo( TimeDoubleUnit unit, double baseValue )
    {
        return unit.convert(baseValue, NANOSECONDS);
    }

    @Override
    protected TimeDouble buildAbsent()
    {
        return absent();
    }

    @Override
    protected TimeDouble buildPresent( double value, TimeDoubleUnit unit )
    {
        return of(value, unit);
    }

    @Override
    protected TimeDouble buildPresent( double baseValue )
    {
        return ofNanos(baseValue);
    }

    private long inNanosAsLong()
    {
        return NANOSECONDS.toNanosAsLong(inNanos());
    }

    private static final void validatePrecision( int precision )
    {
        if (precision < 1) {
            throw new IllegalArgumentException("precision must be positive");
        }
    }
}
