package net.varanus.util.unitvalue.si;


import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;


/**
 * @param <U>
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public interface SIMultipleDoubleUnit<U extends SIMultipleDoubleUnit<U>>
{
    public double toUnitValue( double value );

    public double toKiloValue( double value );

    public double toMegaValue( double value );

    public double toGigaValue( double value );

    public double toTeraValue( double value );

    public double convert( double sourceValue, U siUnit );

    public U asUnit();

    public U asKilo();

    public U asMega();

    public U asGiga();

    public U asTera();
}
