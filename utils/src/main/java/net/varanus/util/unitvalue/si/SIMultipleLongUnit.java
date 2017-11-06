package net.varanus.util.unitvalue.si;


import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;


/**
 * @param <U>
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public interface SIMultipleLongUnit<U extends SIMultipleLongUnit<U>>
{
    public long toUnitValue( long value );

    public long toKiloValue( long value );

    public long toMegaValue( long value );

    public long toGigaValue( long value );

    public long toTeraValue( long value );

    public long convert( long sourceValue, U siUnit );

    public U asUnit();

    public U asKilo();

    public U asMega();

    public U asGiga();

    public U asTera();
}
