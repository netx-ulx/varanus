package net.varanus.util.time;


import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.functional.PossibleLong;
import net.varanus.util.functional.functions.ObjLongFunction;
import net.varanus.util.text.StringUtils;
import net.varanus.util.unitvalue.AbstractUnitLongValue;


@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class TimeLong extends AbstractUnitLongValue<TimeUnit, TimeLong>
{
    private static final TimeLong ABSENT = new TimeLong(0, false);
    public static final TimeLong  ZERO   = new TimeLong(0, true);

    public static TimeLong absent()
    {
        return ABSENT;
    }

    public static TimeLong ofNanos( long durationNanos )
    {
        return new TimeLong(durationNanos, true);
    }

    public static TimeLong of( long duration, TimeUnit unit )
    {
        return new TimeLong(unit.toNanos(duration), true);
    }

    public static TimeLong ofPossibleNanos( PossibleLong durationNanos )
    {
        return durationNanos.mapToObj(TimeLong::ofNanos).orElse(absent());
    }

    public static TimeLong ofPossible( PossibleLong duration, TimeUnit unit )
    {
        return duration.mapToObj(d -> TimeLong.of(d, unit)).orElse(absent());
    }

    public static TimeLong fromDuration( Duration duration )
    {
        return new TimeLong(duration.toNanos(), true);
    }

    public static TimeLong parseNanos( String s )
    {
        return ofNanos(StringUtils.parseLong(s));
    }

    public static TimeLong parse( String s, TimeUnit unit )
    {
        return of(StringUtils.parseLong(s), unit);
    }

    private TimeLong( long valNanos, boolean isPresent )
    {
        super(valNanos, isPresent);
    }

    public long inNanos()
    {
        return toBaseValue();
    }

    public PossibleLong asPossibleInNanos()
    {
        return asPossibleBase();
    }

    public TimeDouble asDouble()
    {
        return isPresent() ? TimeDouble.ofNanos(inNanos())
                           : TimeDouble.absent();
    }

    public Duration asDuration()
    {
        return Duration.ofNanos(inNanos());
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
        return (other instanceof TimeLong)
               && super.equals(other);
    }

    public boolean equals( TimeLong other )
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
    public ObjLongFunction<TimeUnit, ULongPair<TimeUnit>> pairFactory()
    {
        return ( unit, value ) -> new SimpleULongPair<TimeUnit>(unit, value) {

            @Override
            public String toString()
            {
                return String.format("%s %s", value(), TimeUtils.abbreviatedUnit(unit()));
            }
        };
    }

    @Override
    protected long convertTo( TimeUnit unit, long baseValue )
    {
        return unit.convert(baseValue, NANOSECONDS);
    }

    @Override
    protected TimeLong buildAbsent()
    {
        return absent();
    }

    @Override
    protected TimeLong buildPresent( long value, TimeUnit unit )
    {
        return of(value, unit);
    }

    @Override
    protected TimeLong buildPresent( long baseValue )
    {
        return ofNanos(baseValue);
    }
}
