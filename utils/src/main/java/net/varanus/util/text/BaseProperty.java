package net.varanus.util.text;


import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;

import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.functional.functions.ExceptionalFunction;


/**
 * @param <T>
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
abstract class BaseProperty<T>
{
    protected static Function<String, String> asPropMapper( Map<String, String> map )
    {
        return map::get;
    }

    protected static Function<String, String> asPropMapper( Properties props )
    {
        return props::getProperty;
    }

    private final String      propKey;
    private final Optional<T> defaultValue;

    public BaseProperty( String propKey )
    {
        this.propKey = Objects.requireNonNull(propKey);
        this.defaultValue = Optional.empty();
    }

    public BaseProperty( String propKey, T defaultValue )
    {
        this.propKey = Objects.requireNonNull(propKey);
        this.defaultValue = Optional.of(defaultValue);
    }

    public final String getPropKey()
    {
        return propKey;
    }

    public final boolean hasDefaultValue()
    {
        return defaultValue.isPresent();
    }

    public final T getDefaultValue()
    {
        return defaultValue.orElseThrow(() -> new IllegalStateException("default value is not set"));
    }

    protected final T readProperty( Function<String, String> propMapper,
                                    ExceptionalFunction<String,
                                                        ? extends T,
                                                        ? extends IllegalArgumentException> valueConverter )
        throws IllegalArgumentException
    {
        String value = propMapper.apply(getPropKey());

        if (value == null && hasDefaultValue()) {
            return getDefaultValue();
        }
        else if (value == null) {
            throw new IllegalArgumentException(
                String.format("property '%s' not found", getPropKey()));
        }
        else {
            try {
                return Objects.requireNonNull(valueConverter.apply(value));
            }
            catch (IllegalArgumentException e) {
                String reason = e.getMessage();
                if (reason != null)
                    throw new IllegalArgumentException(
                        String.format("invalid property '%s': %s", getPropKey(), reason));
                else
                    throw new IllegalArgumentException(
                        String.format("invalid property '%s'", getPropKey()));
            }
        }
    }
}
