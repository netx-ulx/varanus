package net.varanus.util.text;


import java.util.function.Function;

import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;


/**
 * @param <T>
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
abstract class NumberProperty<T extends Number> extends BaseProperty<T>
{
    NumberProperty( String propKey )
    {
        super(propKey);
    }

    NumberProperty( String propKey, T defaultValue )
    {
        super(propKey, defaultValue);
    }

    protected abstract Class<T> getNumberClass();

    protected final T readNumber( Function<String, String> propMapper )
        throws NumberFormatException, IllegalArgumentException
    {
        return readProperty(propMapper, ( value ) -> StringUtils.convertToNumber(value, getNumberClass()));
    }

    protected final T readNumber( Function<String, String> propMapper, int radix )
        throws NumberFormatException, IllegalArgumentException
    {
        return readProperty(propMapper, ( value ) -> StringUtils.convertToNumber(value, getNumberClass(), radix));
    }
}
