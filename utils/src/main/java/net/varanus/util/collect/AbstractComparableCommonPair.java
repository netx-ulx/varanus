package net.varanus.util.collect;


import java.util.Objects;


/**
 * @param <T>
 */
public abstract class AbstractComparableCommonPair<T extends Comparable<? super T>>
    extends AbstractCommonPair<T>
    implements ComparableCommonPair<T>
{
    @Override
    public boolean equals( Object other )
    {
        return (other instanceof ComparableCommonPair) && this.equals((ComparableCommonPair<?>)other);
    }

    public boolean equals( ComparableCommonPair<?> other )
    {
        return (other != null)
               && Objects.equals(this.getFirst(), other.getFirst())
               && Objects.equals(this.getSecond(), other.getSecond());
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(getFirst(), getSecond());
    }
}
