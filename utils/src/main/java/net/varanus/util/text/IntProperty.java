package net.varanus.util.text;


import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;

import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;


/**
 *
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class IntProperty extends NumberProperty<Integer>
{
    public static IntProperty of( String propKey )
    {
        return ofMapped(propKey, IntUnaryOperator.identity());
    }

    public static IntProperty of( String propKey, int defaultValue )
    {
        return ofMapped(propKey, defaultValue, IntUnaryOperator.identity());
    }

    public static IntProperty ofNegative( String propKey )
    {
        return ofFiltered(propKey, ( val ) -> val < 0, "value must be negative");
    }

    public static IntProperty ofNegative( String propKey, int defaultValue )
    {
        return ofFiltered(propKey, defaultValue, ( val ) -> val < 0, "value must be negative");
    }

    public static IntProperty ofNonNegative( String propKey )
    {
        return ofFiltered(propKey, ( val ) -> val >= 0, "value must be non-negative");
    }

    public static IntProperty ofNonNegative( String propKey, int defaultValue )
    {
        return ofFiltered(propKey, defaultValue, ( val ) -> val >= 0, "value must be non-negative");
    }

    public static IntProperty ofPositive( String propKey )
    {
        return ofFiltered(propKey, ( val ) -> val > 0, "value must be positive");
    }

    public static IntProperty ofPositive( String propKey, int defaultValue )
    {
        return ofFiltered(propKey, defaultValue, ( val ) -> val > 0, "value must be positive");
    }

    public static IntProperty ofNonPositive( String propKey )
    {
        return ofFiltered(propKey, ( val ) -> val <= 0, "value must be non-positive");
    }

    public static IntProperty ofNonPositive( String propKey, int defaultValue )
    {
        return ofFiltered(propKey, defaultValue, ( val ) -> val <= 0, "value must be non-positive");
    }

    public static IntProperty ofFiltered( String propKey, IntPredicate filter )
    {
        return ofFiltered(propKey, filter, "invalid filtered value");
    }

    public static IntProperty ofFiltered( String propKey, IntPredicate filter, String filterErrorMsg )
    {
        Objects.requireNonNull(filter);
        return ofMapped(
            propKey,
            ( val ) -> {
                if (filter.test(val))
                    return val;
                else
                    throw new IllegalArgumentException(filterErrorMsg);
            });
    }

    public static IntProperty ofFiltered( String propKey, int defaultValue, IntPredicate filter )
    {
        return ofFiltered(propKey, defaultValue, filter, "invalid filtered value");
    }

    public static IntProperty ofFiltered( String propKey, int defaultValue, IntPredicate filter, String filterErrorMsg )
    {
        Objects.requireNonNull(filter);
        return ofMapped(
            propKey,
            defaultValue,
            ( val ) -> {
                if (filter.test(val))
                    return val;
                else
                    throw new IllegalArgumentException(filterErrorMsg);
            });
    }

    public static IntProperty ofMapped( String propKey, IntUnaryOperator postMapper )
    {
        return new IntProperty(propKey, Objects.requireNonNull(postMapper));
    }

    public static IntProperty ofMapped( String propKey, int defaultValue, IntUnaryOperator postMapper )
    {
        return new IntProperty(propKey, defaultValue, Objects.requireNonNull(postMapper));
    }

    private final IntUnaryOperator postMapper;

    private IntProperty( String propKey, IntUnaryOperator postMapper )
    {
        super(propKey);
        this.postMapper = postMapper;
    }

    private IntProperty( String propKey, int defaultValue, IntUnaryOperator postMapper )
    {
        super(propKey, defaultValue);
        this.postMapper = postMapper;
    }

    public int readInt( Map<String, String> props ) throws NumberFormatException, IllegalArgumentException
    {
        return readInt(asPropMapper(props));
    }

    public int readInt( Map<String, String> props, int radix ) throws NumberFormatException, IllegalArgumentException
    {
        return readInt(asPropMapper(props), radix);
    }

    public int readInt( Properties props ) throws NumberFormatException, IllegalArgumentException
    {
        return readInt(asPropMapper(props));
    }

    public int readInt( Properties props, int radix ) throws NumberFormatException, IllegalArgumentException
    {
        return readInt(asPropMapper(props), radix);
    }

    public int readInt( Function<String, String> propMapper ) throws NumberFormatException, IllegalArgumentException
    {
        return postMapper.applyAsInt(readNumber(propMapper));
    }

    public int readInt( Function<String, String> propMapper, int radix )
        throws NumberFormatException,
        IllegalArgumentException
    {
        return postMapper.applyAsInt(readNumber(propMapper, radix));
    }

    @Override
    protected Class<Integer> getNumberClass()
    {
        return Integer.TYPE;
    }
}
