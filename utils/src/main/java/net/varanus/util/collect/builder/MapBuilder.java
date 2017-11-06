package net.varanus.util.collect.builder;


import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;


/**
 * @param <K>
 * @param <V>
 * @param <M>
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public abstract class MapBuilder<K, V, M extends Map<K, V>> implements BaseBuilder<M>
{
    private boolean nullKeysAllowed;
    private boolean nullValuesAllowed;

    public MapBuilder()
    {
        this.nullKeysAllowed = false;
        this.nullValuesAllowed = false;
    }

    public final MapBuilder<K, V, M> areNullKeysAllowed( boolean allowed )
    {
        this.nullKeysAllowed = allowed;
        return this;
    }

    public final MapBuilder<K, V, M> areNullValuesAllowed( boolean allowed )
    {
        this.nullValuesAllowed = allowed;
        return this;
    }

    private K checkNullKey( K key )
    {
        return nullKeysAllowed ? key : Objects.requireNonNull(key);
    }

    private V checkNullValue( V value )
    {
        return nullValuesAllowed ? value : Objects.requireNonNull(value);
    }

    public final MapBuilder<K, V, M> put( K key, V value )
    {
        _put(checkNullKey(key), checkNullValue(value));
        return this;
    }

    public final MapBuilder<K, V, M> put( Entry<? extends K, ? extends V> entry )
    {
        put(entry.getKey(), entry.getValue());
        return this;
    }

    @SafeVarargs
    @SuppressWarnings( "varargs" )
    public final MapBuilder<K, V, M> put( Entry<? extends K, ? extends V>... entries )
    {
        for (Entry<? extends K, ? extends V> e : entries) {
            put(e.getKey(), e.getValue());
        }
        return this;
    }

    public final MapBuilder<K, V, M> putIf( boolean condition, K key, V value )
    {
        if (condition)
            put(key, value);
        return this;
    }

    public final MapBuilder<K, V, M> putIf( boolean condition, Entry<? extends K, ? extends V> entry )
    {
        if (condition)
            put(entry);
        return this;
    }

    @SafeVarargs
    @SuppressWarnings( "varargs" )
    public final MapBuilder<K, V, M> putIf( boolean condition, Entry<? extends K, ? extends V>... entries )
    {
        if (condition)
            put(entries);
        return this;
    }

    public final MapBuilder<K, V, M> putIfPresent( Optional<? extends K> key, Optional<? extends V> value )
    {
        if (key.isPresent() && value.isPresent())
            put(key.get(), value.get());
        return this;
    }

    public final MapBuilder<K, V, M> putIfPresent( Optional<? extends Entry<? extends K, ? extends V>> entry )
    {
        entry.ifPresent(this::put);
        return this;
    }

    public final MapBuilder<K, V, M> putAll( Iterable<? extends Entry<? extends K, ? extends V>> entries )
    {
        for (Entry<? extends K, ? extends V> e : entries) {
            put(e);
        }
        return this;
    }

    public final MapBuilder<K, V, M> putAllIf( boolean condition,
                                               Iterable<? extends Entry<? extends K, ? extends V>> entries )
    {
        if (condition)
            putAll(entries);
        return this;
    }

    public final MapBuilder<K, V, M> putEach( Stream<? extends Entry<? extends K, ? extends V>> entries )
    {
        entries.forEach(this::put);
        return this;
    }

    public final MapBuilder<K, V, M> putEachIf( Predicate<? super K> keyConstraint,
                                                Predicate<? super V> valueConstraint,
                                                Iterable<? extends Entry<? extends K, ? extends V>> entries )
    {
        for (Entry<? extends K, ? extends V> e : entries) {
            putIf(keyConstraint.test(e.getKey()) && valueConstraint.test(e.getValue()), e);
        }
        return this;
    }

    public final MapBuilder<K, V, M> putEachIf( Predicate<? super Entry<? extends K, ? extends V>> constraint,
                                                Iterable<? extends Entry<? extends K, ? extends V>> entries )
    {
        for (Entry<? extends K, ? extends V> e : entries) {
            putIf(constraint.test(e), e);
        }
        return this;
    }

    public final MapBuilder<K, V, M> clear()
    {
        _clear();
        return this;
    }

    @Override
    public abstract M build();

    protected abstract void _put( K key, V value );

    protected abstract void _clear();
}
