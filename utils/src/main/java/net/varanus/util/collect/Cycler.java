package net.varanus.util.collect;


import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.Iterators;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;


/**
 * @param <T>
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public class Cycler<T> implements Iterator<T>
{
    private Iterator<T> cycler;
    private int         size;

    public Cycler()
    {
        this(Collections.emptyList());
    }

    public Cycler( Collection<T> col )
    {
        this.cycler = Iterators.cycle(col);
        this.size = col.size();
    }

    public final void reset( Collection<T> col )
    {
        this.cycler = Iterators.cycle(col);
        this.size = col.size();
    }

    public final int size()
    {
        return size;
    }

    @Override
    public final boolean hasNext()
    {
        return cycler.hasNext();
    }

    @Override
    public final @Nullable T next()
    {
        return cycler.next();
    }

    public final void forOneLoop( Consumer<? super T> action )
    {
        Objects.requireNonNull(action);

        final Iterator<T> iter = this.cycler;
        final int size = this.size;
        for (int i = 0; i < size && iter.hasNext(); i++)
            action.accept(iter.next());
    }
}
