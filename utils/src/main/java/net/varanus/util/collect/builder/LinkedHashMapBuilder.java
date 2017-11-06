package net.varanus.util.collect.builder;


import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.collect.CollectionUtils;

import java.util.Objects;


/**
 * @param <K>
 * @param <V>
 */
@FieldsAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class LinkedHashMapBuilder<K, V> extends MapBuilder<K, V, LinkedHashMap<K, V>>
{
    public static <K, V> LinkedHashMapBuilder<K, V> create()
    {
        return new LinkedHashMapBuilder<>(new LinkedHashMap<>());
    }

    public static <K, V> LinkedHashMapBuilder<K, V> create( int initialCapacity )
    {
        return new LinkedHashMapBuilder<>(new LinkedHashMap<>(initialCapacity));
    }

    public static <K, V> LinkedHashMapBuilder<K, V> create( int initialCapacity, float loadFactor )
    {
        return new LinkedHashMapBuilder<>(new LinkedHashMap<>(initialCapacity, loadFactor));
    }

    public static <K, V> LinkedHashMap<K, V> build( K key, V value )
    {
        int initialCapacity = CollectionUtils.initialMapCapacity(1);
        LinkedHashMap<K, V> map = new LinkedHashMap<>(initialCapacity);
        map.put(Objects.requireNonNull(key), Objects.requireNonNull(value));
        return map;
    }

    public static <K, V> LinkedHashMap<K, V> build( Entry<? extends K, ? extends V> entry )
    {
        int initialCapacity = CollectionUtils.initialMapCapacity(1);
        LinkedHashMap<K, V> map = new LinkedHashMap<>(initialCapacity);
        map.put(Objects.requireNonNull(entry.getKey()), Objects.requireNonNull(entry.getValue()));
        return map;
    }

    @SafeVarargs
    @SuppressWarnings( "varargs" )
    public static <K, V> LinkedHashMap<K, V> build( Entry<? extends K, ? extends V>... entries )
    {
        int initialCapacity = CollectionUtils.initialMapCapacity(entries.length);
        LinkedHashMap<K, V> map = new LinkedHashMap<>(initialCapacity);
        for (Entry<? extends K, ? extends V> e : entries) {
            map.put(Objects.requireNonNull(e.getKey()), Objects.requireNonNull(e.getValue()));
        }
        return map;
    }

    public static <K, V> LinkedHashMap<K, V> build( Iterable<? extends Entry<? extends K, ? extends V>> entries )
    {
        final LinkedHashMap<K, V> map;
        if (entries instanceof Collection<?>) {
            int initialCapacity = CollectionUtils.initialMapCapacity(((Collection<?>)entries).size());
            map = new LinkedHashMap<>(initialCapacity);
        }
        else {
            map = new LinkedHashMap<>();
        }

        for (Entry<? extends K, ? extends V> e : entries) {
            map.put(Objects.requireNonNull(e.getKey()), Objects.requireNonNull(e.getValue()));
        }
        return map;
    }

    private final LinkedHashMap<K, V> map;

    private LinkedHashMapBuilder( LinkedHashMap<K, V> map )
    {
        this.map = map;
    }

    @Override
    protected void _put( K key, V value )
    {
        map.put(key, value);
    }

    @Override
    protected void _clear()
    {
        map.clear();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public LinkedHashMap<K, V> build()
    {
        return (LinkedHashMap<K, V>)map.clone();
    }
}
