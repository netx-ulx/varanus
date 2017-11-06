package net.varanus.util.collect;


import java.util.Iterator;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;


/**
 * A pair of elements with a common type.
 * 
 * @param <T>
 *            The common type of both elements
 */
@ParametersAreNonnullByDefault
public interface CommonPair<T> extends Pair<T, T>, Iterable<T>
{
    /**
     * Returns an immutable common pair containing the provided elements.
     * 
     * @param first
     *            The first element of this pair
     * @param second
     *            The second element of this pair
     * @return an immutable common pair containing the provided elements
     */
    public static <T> CommonPair<T> of( final @Nullable T first, final @Nullable T second )
    {
        return new AbstractCommonPair<T>() {

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

    /**
     * Returns an iterator over the two elements of this pair.
     */
    @Override
    public Iterator<T> iterator();
}
