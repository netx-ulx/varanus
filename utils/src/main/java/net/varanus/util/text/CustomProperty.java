package net.varanus.util.text;


import java.util.Map;
import java.util.Objects;
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
public class CustomProperty<T> extends BaseProperty<T>
{
    public static <T> CustomProperty<T> of( String propKey,
                                            ExceptionalFunction<String,
                                                                ? extends T,
                                                                ? extends IllegalArgumentException> valueConverter )
    {
        return new CustomProperty<>(propKey, valueConverter);
    }

    public static <T> CustomProperty<T> of( String propKey,
                                            T defaultValue,
                                            ExceptionalFunction<String,
                                                                ? extends T,
                                                                ? extends IllegalArgumentException> valueConverter )
    {
        return new CustomProperty<>(propKey, defaultValue, valueConverter);
    }

    private final ExceptionalFunction<String, ? extends T, ? extends IllegalArgumentException> valueConverter;

    private CustomProperty( String propKey,
                            ExceptionalFunction<String, ? extends T,
                                                ? extends IllegalArgumentException> valueConverter )
    {
        super(propKey);
        this.valueConverter = Objects.requireNonNull(valueConverter);
    }

    private CustomProperty( String propKey,
                            T defaultValue,
                            ExceptionalFunction<String, ? extends T,
                                                ? extends IllegalArgumentException> valueConverter )
    {
        super(propKey, defaultValue);
        this.valueConverter = Objects.requireNonNull(valueConverter);
    }

    public T readProperty( Map<String, String> props ) throws IllegalArgumentException
    {
        return readProperty(asPropMapper(props));
    }

    public T readProperty( Properties props ) throws IllegalArgumentException
    {
        return readProperty(asPropMapper(props));
    }

    public T readProperty( Function<String, String> propMapper ) throws IllegalArgumentException
    {
        return readProperty(propMapper, valueConverter);
    }
}
