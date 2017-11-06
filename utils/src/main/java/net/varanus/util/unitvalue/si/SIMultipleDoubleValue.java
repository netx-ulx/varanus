package net.varanus.util.unitvalue.si;


import java.util.Objects;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.functional.functions.ObjDoubleFunction;
import net.varanus.util.unitvalue.AbstractUnitDoubleValue;


/**
 * @param <U>
 * @param <V>
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public abstract class SIMultipleDoubleValue<U extends SIMultipleDoubleUnit<U>, V extends SIMultipleDoubleValue<U, V>>
    extends AbstractUnitDoubleValue<U, V>
{
    private static final int DEFAULT_TO_STRING_PRECISION = 3;

    protected SIMultipleDoubleValue( double baseValue, boolean isPresent )
    {
        super(baseValue, isPresent);
    }

    protected abstract U baseUnit();

    @Override
    protected final double convertTo( U unit, double baseValue )
    {
        return unit.convert(baseValue, baseUnit());
    }

    @Override
    public ObjDoubleFunction<U, UDoublePair<U>> pairFactory()
    {
        return ( unit, value ) -> new SimpleUDoublePair<U>(unit, value) {

            @Override
            public String toString()
            {
                return String.format("%.3f %s", value(), unit());
            }
        };
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public boolean equals( Object other )
    {
        return (other instanceof SIMultipleDoubleValue<?, ?>)
               && super.equals(other);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public int hashCode()
    {
        return super.hashCode();
    }

    @Override
    public String toString( U unit )
    {
        return toString(unit, DEFAULT_TO_STRING_PRECISION, "absent");
    }

    @Override
    public String toString( U unit, String absentString )
    {
        return toString(unit, DEFAULT_TO_STRING_PRECISION, absentString);
    }

    public String toString( U unit, int precision )
    {
        return toString(unit, precision, "absent");
    }

    public String toString( U unit, final int precision, String absentString )
    {
        validatePrecision(precision);
        return toString(
            unit,
            pair -> String.format("%." + precision + "f %s", pair.value(), pair.unit()),
            absentString);
    }

    public String toStringAnd( U unit, Object unitSuffix )
    {
        return toStringAnd(unit, DEFAULT_TO_STRING_PRECISION, unitSuffix, "absent");
    }

    public String toStringAnd( U unit, Object unitSuffix, String absentString )
    {
        return toStringAnd(unit, DEFAULT_TO_STRING_PRECISION, unitSuffix, absentString);
    }

    public String toStringAnd( U unit, int precision, Object unitSuffix )
    {
        return toStringAnd(unit, precision, unitSuffix, "absent");
    }

    public String toStringAnd( U unit, final int precision, final Object unitSuffix, String absentString )
    {
        validatePrecision(precision);
        Objects.requireNonNull(unitSuffix);
        return toString(
            unit,
            pair -> String.format("%." + precision + "f %s%s", pair.value(), pair.unit(), unitSuffix),
            absentString);
    }

    protected final String toSmartString( U unitType )
    {
        return toSmartString(unitType, DEFAULT_TO_STRING_PRECISION);
    }

    protected final String toSmartString( U unitType, int precision )
    {
        if (!isPresent()) {
            return toString(unitType.asUnit(), precision);
        }
        else if (toBaseValue() < unitType.asKilo().toUnitValue(1)) {
            return toString(unitType.asUnit(), precision);
        }
        else if (toBaseValue() < unitType.asMega().toUnitValue(1)) {
            return toString(unitType.asKilo(), precision);
        }
        else if (toBaseValue() < unitType.asGiga().toUnitValue(1)) {
            return toString(unitType.asMega(), precision);
        }
        else if (toBaseValue() < unitType.asTera().toUnitValue(1)) {
            return toString(unitType.asGiga(), precision);
        }
        else {
            return toString(unitType.asTera(), precision);
        }
    }

    protected final String toSmartStringAnd( U unitType, Object unitSuffix )
    {
        return toSmartStringAnd(unitType, DEFAULT_TO_STRING_PRECISION, unitSuffix);
    }

    protected final String toSmartStringAnd( U unitType, int precision, Object unitSuffix )
    {
        if (!isPresent()) {
            return toStringAnd(unitType.asUnit(), precision, unitSuffix);
        }
        else if (toBaseValue() < unitType.asKilo().toUnitValue(1)) {
            return toStringAnd(unitType.asUnit(), precision, unitSuffix);
        }
        else if (toBaseValue() < unitType.asMega().toUnitValue(1)) {
            return toStringAnd(unitType.asKilo(), precision, unitSuffix);
        }
        else if (toBaseValue() < unitType.asGiga().toUnitValue(1)) {
            return toStringAnd(unitType.asMega(), precision, unitSuffix);
        }
        else if (toBaseValue() < unitType.asTera().toUnitValue(1)) {
            return toStringAnd(unitType.asGiga(), precision, unitSuffix);
        }
        else {
            return toStringAnd(unitType.asTera(), precision, unitSuffix);
        }
    }

    protected static final void validatePrecision( int precision )
    {
        if (precision < 1) {
            throw new IllegalArgumentException("precision must be positive");
        }
    }
}
