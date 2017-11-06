package net.varanus.util.collect.builder;


import com.google.common.collect.ImmutableSet;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;


/**
 * @param <E>
 */
@FieldsAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class ImmutableSetBuilder<E> extends CollectionBuilder<E, ImmutableSet<E>>
{
    public static <E> ImmutableSetBuilder<E> create()
    {
        return new ImmutableSetBuilder<>();
    }

    private ImmutableSet.Builder<E> delegate;

    private ImmutableSetBuilder()
    {
        this.delegate = ImmutableSet.builder();
    }

    @Override
    protected void _add( E element )
    {
        delegate.add(element);
    }

    @Override
    protected void _clear()
    {
        this.delegate = ImmutableSet.builder();
    }

    @Override
    public ImmutableSet<E> build()
    {
        return delegate.build();
    }
}
