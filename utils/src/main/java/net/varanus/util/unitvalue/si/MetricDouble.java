package net.varanus.util.unitvalue.si;


import static net.varanus.util.unitvalue.si.MetricDoublePrefix.UNIT;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.functional.PossibleDouble;


/**
 * 
 */
@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class MetricDouble extends SIMultipleDoubleValue<MetricDoublePrefix, MetricDouble>
{
    private static final MetricDouble ABSENT = new MetricDouble(0, false);
    public static final MetricDouble  ZERO   = new MetricDouble(0, true);

    public static MetricDouble absent()
    {
        return ABSENT;
    }

    public static MetricDouble ofUnits( double valUnits )
    {
        return of(valUnits, UNIT);
    }

    public static MetricDouble of( double val, MetricDoublePrefix unit )
    {
        return new MetricDouble(unit.toUnitValue(val), true);
    }

    public static MetricDouble ofPossibleUnits( PossibleDouble valBytes )
    {
        return valBytes.mapToObj(MetricDouble::ofUnits).orElse(absent());
    }

    public static MetricDouble ofPossible( PossibleDouble val, MetricDoublePrefix unit )
    {
        return val.mapToObj(v -> MetricDouble.of(v, unit)).orElse(absent());
    }

    public static MetricDouble parseUnits( String s )
    {
        return ofUnits(Double.parseDouble(s));
    }

    public static MetricDouble parse( String s, MetricDoublePrefix unit )
    {
        return of(Double.parseDouble(s), unit);
    }

    private MetricDouble( double baseValue, boolean isPresent )
    {
        super(baseValue, isPresent);
    }

    public double inUnits()
    {
        return toBaseValue();
    }

    public PossibleDouble asPossibleInUnits()
    {
        return asPossibleBase();
    }

    public MetricLong asLong()
    {
        return isPresent() ? MetricLong.ofUnits(Math.round(inUnits()))
                           : MetricLong.absent();
    }

    @Override
    public boolean equals( Object other )
    {
        return (other instanceof MetricDouble)
               && super.equals(other);
    }

    public boolean equals( MetricDouble other )
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

    public String toString( int precision )
    {
        return toSmartString(UNIT, precision);
    }

    public String toStringAnd( Object unitSuffix )
    {
        return toSmartStringAnd(UNIT, unitSuffix);
    }

    public String toStringAnd( int precision, Object unitSuffix )
    {
        return toSmartStringAnd(UNIT, precision, unitSuffix);
    }

    @Override
    protected MetricDoublePrefix baseUnit()
    {
        return UNIT;
    }

    @Override
    protected MetricDouble buildAbsent()
    {
        return absent();
    }

    @Override
    protected MetricDouble buildPresent( double value, MetricDoublePrefix unit )
    {
        return of(value, unit);
    }

    @Override
    protected MetricDouble buildPresent( double baseValue )
    {
        return ofUnits(baseValue);
    }
}
