package net.varanus.util.text;


import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.LongPredicate;
import java.util.function.LongUnaryOperator;

import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;


/**
 *
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class LongProperty extends NumberProperty<Long>
{
    public static LongProperty of( String propKey )
    {
        return ofMapped(propKey, LongUnaryOperator.identity());
    }

    public static LongProperty of( String propKey, long defaultValue )
    {
        return ofMapped(propKey, defaultValue, LongUnaryOperator.identity());
    }

    public static LongProperty ofNegative( String propKey )
    {
        return ofFiltered(propKey, ( val ) -> val < 0, "value must be negative");
    }

    public static LongProperty ofNegative( String propKey, long defaultValue )
    {
        return ofFiltered(propKey, defaultValue, ( val ) -> val < 0, "value must be negative");
    }

    public static LongProperty ofNonNegative( String propKey )
    {
        return ofFiltered(propKey, ( val ) -> val >= 0, "value must be non-negative");
    }

    public static LongProperty ofNonNegative( String propKey, long defaultValue )
    {
        return ofFiltered(propKey, defaultValue, ( val ) -> val >= 0, "value must be non-negative");
    }

    public static LongProperty ofPositive( String propKey )
    {
        return ofFiltered(propKey, ( val ) -> val > 0, "value must be positive");
    }

    public static LongProperty ofPositive( String propKey, long defaultValue )
    {
        return ofFiltered(propKey, defaultValue, ( val ) -> val > 0, "value must be positive");
    }

    public static LongProperty ofNonPositive( String propKey )
    {
        return ofFiltered(propKey, ( val ) -> val <= 0, "value must be non-positive");
    }

    public static LongProperty ofNonPositive( String propKey, long defaultValue )
    {
        return ofFiltered(propKey, defaultValue, ( val ) -> val <= 0, "value must be non-positive");
    }

    public static LongProperty ofFiltered( String propKey, LongPredicate filter )
    {
        return ofFiltered(propKey, filter, "invalid filtered value");
    }

    public static LongProperty ofFiltered( String propKey, LongPredicate filter, String filterErrorMsg )
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

    public static LongProperty ofFiltered( String propKey, long defaultValue, LongPredicate filter )
    {
        return ofFiltered(propKey, defaultValue, filter, "invalid filtered value");
    }

    public static LongProperty ofFiltered( String propKey,
                                           long defaultValue,
                                           LongPredicate filter,
                                           String filterErrorMsg )
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

    public static LongProperty ofMapped( String propKey, LongUnaryOperator postMapper )
    {
        return new LongProperty(propKey, Objects.requireNonNull(postMapper));
    }

    public static LongProperty ofMapped( String propKey, long defaultValue, LongUnaryOperator postMapper )
    {
        return new LongProperty(propKey, defaultValue, Objects.requireNonNull(postMapper));
    }

    private final LongUnaryOperator postMapper;

    private LongProperty( String propKey, LongUnaryOperator postMapper )
    {
        super(propKey);
        this.postMapper = postMapper;
    }

    private LongProperty( String propKey, long defaultValue, LongUnaryOperator postMapper )
    {
        super(propKey, defaultValue);
        this.postMapper = postMapper;
    }

    public long readLong( Map<String, String> props ) throws NumberFormatException, IllegalArgumentException
    {
        return readLong(asPropMapper(props));
    }

    public long readLong( Map<String, String> props, int radix ) throws NumberFormatException, IllegalArgumentException
    {
        return readLong(asPropMapper(props), radix);
    }

    public long readLong( Properties props ) throws NumberFormatException, IllegalArgumentException
    {
        return readLong(asPropMapper(props));
    }

    public long readLong( Properties props, int radix ) throws NumberFormatException, IllegalArgumentException
    {
        return readLong(asPropMapper(props), radix);
    }

    public long readLong( Function<String, String> propMapper ) throws NumberFormatException, IllegalArgumentException
    {
        return postMapper.applyAsLong(readNumber(propMapper));
    }

    public long readLong( Function<String, String> propMapper, int radix )
        throws NumberFormatException,
        IllegalArgumentException
    {
        return postMapper.applyAsLong(readNumber(propMapper, radix));
    }

    @Override
    protected Class<Long> getNumberClass()
    {
        return Long.TYPE;
    }
}
