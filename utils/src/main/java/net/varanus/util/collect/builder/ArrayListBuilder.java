package net.varanus.util.collect.builder;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;


/**
 * @param <E>
 */
@FieldsAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class ArrayListBuilder<E> extends CollectionBuilder<E, ArrayList<E>>
{
    public static <E> ArrayListBuilder<E> create()
    {
        return new ArrayListBuilder<>(new ArrayList<>());
    }

    public static <E> ArrayListBuilder<E> create( int initialCapacity )
    {
        return new ArrayListBuilder<>(new ArrayList<>(initialCapacity));
    }

    public static <E> ArrayList<E> build( E element )
    {
        ArrayList<E> list = new ArrayList<>(1);
        list.add(Objects.requireNonNull(element));
        return list;
    }

    @SafeVarargs
    public static <E> ArrayList<E> build( E... elements )
    {
        ArrayList<E> list = new ArrayList<>(elements.length);
        for (E e : elements) {
            list.add(Objects.requireNonNull(e));
        }
        return list;
    }

    public static <E> ArrayList<E> build( Iterable<? extends E> elements )
    {
        if (elements instanceof Collection<?>) {
            return new ArrayList<>((Collection<? extends E>)elements);
        }
        else {
            ArrayList<E> list = new ArrayList<>();
            for (E e : elements) {
                list.add(Objects.requireNonNull(e));
            }
            return list;
        }
    }

    private final ArrayList<E> list;

    private ArrayListBuilder( ArrayList<E> list )
    {
        this.list = list;
    }

    @Override
    protected void _add( E element )
    {
        list.add(element);
    }

    @Override
    protected void _clear()
    {
        list.clear();
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public ArrayList<E> build()
    {
        return (ArrayList<E>)list.clone();
    }
}
