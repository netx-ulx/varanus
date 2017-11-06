package net.varanus.util.collect;


import java.util.Comparator;
import java.util.Objects;


/**
 * A comparable pair of elements.
 * 
 * @param <A>
 *            The type of the first comparable element.
 * @param <B>
 *            The type of the second comparable element
 */
public interface ComparablePair<A extends Comparable<? super A>, B extends Comparable<? super B>>
    extends Pair<A, B>, Comparable<Pair<A, B>>
{
    /**
     * Returns an immutable pair containing the provided comparable elements.
     * 
     * @param first
     *            The first comparable element of this pair
     * @param second
     *            The second comparable element of this pair
     * @return an immutable pair containing the provided comparable elements
     */
    public static <A extends Comparable<? super A>, B extends Comparable<? super B>> ComparablePair<A,
                                                                                                    B> of( final A first,
                                                                                                           final B second )
    {
        return new ComparablePair<A, B>() {

            @Override
            public A getFirst()
            {
                return first;
            }

            @Override
            public B getSecond()
            {
                return second;
            }

            @Override
            public boolean equals( Object other )
            {
                return (other instanceof ComparablePair) && this.equals((ComparablePair<?, ?>)other);
            }

            public boolean equals( ComparablePair<?, ?> other )
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
        };
    }

    @Override
    public default int compareTo( Pair<A, B> other )
    {
        return Comparator
            .comparing(Pair<A, B>::getFirst)
            .thenComparing(Pair<A, B>::getSecond)
            .compare(this, other);
    }
}
