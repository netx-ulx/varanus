package net.varanus.util.unitvalue.si;


import static net.varanus.util.unitvalue.si.MetricLongPrefix.UNIT;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.functional.PossibleLong;
import net.varanus.util.text.StringUtils;


/**
 * 
 */
@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class MetricLong extends SIMultipleLongValue<MetricLongPrefix, MetricLong>
{
    private static final MetricLong ABSENT = new MetricLong(0, false);
    public static final MetricLong  ZERO   = new MetricLong(0, true);

    public static MetricLong absent()
    {
        return ABSENT;
    }

    public static MetricLong ofUnits( long valUnits )
    {
        return of(valUnits, UNIT);
    }

    public static MetricLong of( long val, MetricLongPrefix unit )
    {
        return new MetricLong(unit.toUnitValue(val), true);
    }

    public static MetricLong ofPossibleUnits( PossibleLong valBytes )
    {
        return valBytes.mapToObj(MetricLong::ofUnits).orElse(absent());
    }

    public static MetricLong ofPossible( PossibleLong val, MetricLongPrefix unit )
    {
        return val.mapToObj(v -> MetricLong.of(v, unit)).orElse(absent());
    }

    public static MetricLong parseUnits( String s )
    {
        return ofUnits(StringUtils.parseLong(s));
    }

    public static MetricLong parse( String s, MetricLongPrefix unit )
    {
        return of(StringUtils.parseLong(s), unit);
    }

    private MetricLong( long baseValue, boolean isPresent )
    {
        super(baseValue, isPresent);
    }

    public long inUnits()
    {
        return toBaseValue();
    }

    public PossibleLong asPossibleInUnits()
    {
        return asPossibleBase();
    }

    public MetricDouble asDouble()
    {
        return isPresent() ? MetricDouble.ofUnits(inUnits())
                           : MetricDouble.absent();
    }

    @Override
    public boolean equals( Object other )
    {
        return (other instanceof MetricLong)
               && super.equals(other);
    }

    public boolean equals( MetricLong other )
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
        return toSmartString(UNIT);
    }

    public String toStringAnd( Object unitSuffix )
    {
        return toSmartStringAnd(UNIT, unitSuffix);
    }

    @Override
    protected MetricLongPrefix baseUnit()
    {
        return UNIT;
    }

    @Override
    protected MetricLong buildAbsent()
    {
        return absent();
    }

    @Override
    protected MetricLong buildPresent( long value, MetricLongPrefix unit )
    {
        return of(value, unit);
    }

    @Override
    protected MetricLong buildPresent( long baseValue )
    {
        return ofUnits(baseValue);
    }
}
