package net.varanus.util.collect;


import java.util.Objects;

import javax.annotation.OverridingMethodsMustInvokeSuper;


/**
 * @param <A>
 * @param <B>
 */
public abstract class AbstractPair<A, B> implements Pair<A, B>
{
    @Override
    @OverridingMethodsMustInvokeSuper
    public boolean equals( Object other )
    {
        return other instanceof AbstractPair<?, ?>
               && this.equals((AbstractPair<?, ?>)other);
    }

    public final boolean equals( AbstractPair<?, ?> other )
    {
        return other != null
               && Objects.equals(this.getFirst(), other.getFirst())
               && Objects.equals(this.getSecond(), other.getSecond());
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public int hashCode()
    {
        return Objects.hash(getFirst(), getSecond());
    }

    @Override
    public String toString()
    {
        return toTupleString();
    }
}
