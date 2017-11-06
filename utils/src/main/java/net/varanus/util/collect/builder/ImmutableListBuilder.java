package net.varanus.util.collect.builder;


import com.google.common.collect.ImmutableList;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;


/**
 * @param <E>
 */
@FieldsAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class ImmutableListBuilder<E> extends CollectionBuilder<E, ImmutableList<E>>
{
    public static <E> ImmutableListBuilder<E> create()
    {
        return new ImmutableListBuilder<>();
    }

    private ImmutableList.Builder<E> delegate;

    private ImmutableListBuilder()
    {
        this.delegate = ImmutableList.builder();
    }

    @Override
    protected void _add( E element )
    {
        delegate.add(element);
    }

    @Override
    protected void _clear()
    {
        this.delegate = ImmutableList.builder();
    }

    @Override
    public ImmutableList<E> build()
    {
        return delegate.build();
    }
}
