package net.varanus.util.collect.builder;


import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.collect.CollectionUtils;


/**
 * @param <E>
 */
@FieldsAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class LinkedHashSetBuilder<E> extends CollectionBuilder<E, LinkedHashSet<E>>
{
    public static <E> LinkedHashSetBuilder<E> create()
    {
        return new LinkedHashSetBuilder<>(new LinkedHashSet<>());
    }

    public static <E> LinkedHashSetBuilder<E> create( int initialCapacity )
    {
        return new LinkedHashSetBuilder<>(new LinkedHashSet<>(initialCapacity));
    }

    public static <E> LinkedHashSetBuilder<E> create( int initialCapacity, float loadFactor )
    {
        return new LinkedHashSetBuilder<>(new LinkedHashSet<>(initialCapacity, loadFactor));
    }

    public static <E> LinkedHashSet<E> build( E element )
    {
        int initialCapacity = CollectionUtils.initialMapCapacity(1);
        LinkedHashSet<E> set = new LinkedHashSet<>(initialCapacity);
        set.add(Objects.requireNonNull(element));
        return set;
    }

    @SafeVarargs
    public static <E> LinkedHashSet<E> build( E... elements )
    {
        int initialCapacity = CollectionUtils.initialMapCapacity(elements.length);
        LinkedHashSet<E> set = new LinkedHashSet<>(initialCapacity);
        for (E e : elements) {
            set.add(Objects.requireNonNull(e));
        }
        return set;
    }

    public static <E> LinkedHashSet<E> build( Iterable<? extends E> elements )
    {
        if (elements instanceof Collection<?>) {
            return new LinkedHashSet<>((Collection<? extends E>)elements);
        }
        else {
            LinkedHashSet<E> set = new LinkedHashSet<>();
            for (E e : elements) {
                set.add(Objects.requireNonNull(e));
            }
            return set;
        }
    }

    private final LinkedHashSet<E> set;

    private LinkedHashSetBuilder( LinkedHashSet<E> set )
    {
        this.set = set;
    }

    @Override
    protected void _add( E element )
    {
        set.add(element);
    }

    @Override
    protected void _clear()
    {
        set.clear();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public LinkedHashSet<E> build()
    {
        return (LinkedHashSet<E>)set.clone();
    }
}
