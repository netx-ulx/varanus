package net.varanus.util.collect;


import java.util.Comparator;


/**
 * A comparable pair of elements with a common type.
 * 
 * @param <T>
 *            The type of both comparable elements.
 */
public interface ComparableCommonPair<T extends Comparable<? super T>>
    extends CommonPair<T>, Comparable<Pair<T, T>>
{
    /**
     * Returns an immutable common pair containing the provided comparable
     * elements.
     * 
     * @param first
     *            The first element of this pair
     * @param second
     *            The second element of this pair
     * @return an immutable common pair containing the provided elements
     */
    public static <T extends Comparable<? super T>> ComparableCommonPair<T> of( final T first, final T second )
    {
        return new AbstractComparableCommonPair<T>() {

            @Override
            public T getFirst()
            {
                return first;
            }

            @Override
            public T getSecond()
            {
                return second;
            }
        };
    }

    @Override
    public default int compareTo( Pair<T, T> other )
    {
        return Comparator
            .comparing(Pair<T, T>::getFirst)
            .thenComparing(Pair<T, T>::getSecond)
            .compare(this, other);
    }
}
