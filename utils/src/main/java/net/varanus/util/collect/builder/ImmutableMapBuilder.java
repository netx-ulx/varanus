package net.varanus.util.collect.builder;


import com.google.common.collect.ImmutableMap;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;


/**
 * @param <K>
 * @param <V>
 */
@FieldsAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class ImmutableMapBuilder<K, V> extends MapBuilder<K, V, ImmutableMap<K, V>>
{
    public static <K, V> ImmutableMapBuilder<K, V> create()
    {
        return new ImmutableMapBuilder<>();
    }

    private ImmutableMap.Builder<K, V> delegate;

    private ImmutableMapBuilder()
    {
        this.delegate = ImmutableMap.builder();
    }

    @Override
    protected void _put( K key, V value )
    {
        delegate.put(key, value);
    }

    @Override
    protected void _clear()
    {
        this.delegate = ImmutableMap.builder();
    }

    @Override
    public ImmutableMap<K, V> build()
    {
        return delegate.build();
    }
}
