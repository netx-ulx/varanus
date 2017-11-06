package net.varanus.util.unitvalue.si;


import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.functional.functions.ObjLongFunction;
import net.varanus.util.unitvalue.AbstractUnitLongValue;


/**
 * @param <U>
 * @param <V>
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public abstract class SIMultipleLongValue<U extends SIMultipleLongUnit<U>, V extends SIMultipleLongValue<U, V>>
    extends AbstractUnitLongValue<U, V>
{
    protected SIMultipleLongValue( long baseValue, boolean isPresent )
    {
        super(baseValue, isPresent);
    }

    protected abstract U baseUnit();

    @Override
    protected final long convertTo( U unit, long baseValue )
    {
        return unit.convert(baseValue, baseUnit());
    }

    @Override
    public ObjLongFunction<U, ULongPair<U>> pairFactory()
    {
        return ( unit, value ) -> new SimpleULongPair<U>(unit, value) {

            @Override
            public String toString()
            {
                return String.format("%d %s", value(), unit());
            }
        };
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public boolean equals( Object other )
    {
        return (other instanceof SIMultipleLongValue<?, ?>)
               && super.equals(other);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public int hashCode()
    {
        return super.hashCode();
    }

    public String toStringAnd( U unit, final Object unitSuffix )
    {
        return toString(unit, pair -> String.format("%s%s", pair, unitSuffix));
    }

    protected final String toSmartString( U unitType )
    {
        if (!isPresent()) {
            return toString(unitType.asUnit());
        }
        else if (toBaseValue() < unitType.asKilo().toUnitValue(1)) {
            return toString(unitType.asUnit());
        }
        else if (toBaseValue() < unitType.asMega().toUnitValue(1)) {
            return toString(unitType.asKilo());
        }
        else if (toBaseValue() < unitType.asGiga().toUnitValue(1)) {
            return toString(unitType.asMega());
        }
        else if (toBaseValue() < unitType.asTera().toUnitValue(1)) {
            return toString(unitType.asGiga());
        }
        else {
            return toString(unitType.asTera());
        }
    }

    protected final String toSmartStringAnd( U unitType, Object unitSuffix )
    {
        if (!isPresent()) {
            return toStringAnd(unitType.asUnit(), unitSuffix);
        }
        else if (toBaseValue() < unitType.asKilo().toUnitValue(1)) {
            return toStringAnd(unitType.asUnit(), unitSuffix);
        }
        else if (toBaseValue() < unitType.asMega().toUnitValue(1)) {
            return toStringAnd(unitType.asKilo(), unitSuffix);
        }
        else if (toBaseValue() < unitType.asGiga().toUnitValue(1)) {
            return toStringAnd(unitType.asMega(), unitSuffix);
        }
        else if (toBaseValue() < unitType.asTera().toUnitValue(1)) {
            return toStringAnd(unitType.asGiga(), unitSuffix);
        }
        else {
            return toStringAnd(unitType.asTera(), unitSuffix);
        }
    }
}
