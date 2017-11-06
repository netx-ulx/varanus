package net.varanus.util.collect;


import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.PeekingIterator;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;


/**
 * @param <T>
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class PairSlide<T> implements Iterable<CommonPair<T>>
{
    public static <T> PairSlide<T> over( Iterable<T> elements )
    {
        return new PairSlide<>(Objects.requireNonNull(elements));
    }

    @SafeVarargs
    @SuppressWarnings( "varargs" )
    public static <T> PairSlide<T> over( T... elements )
    {
        return over(Arrays.asList(elements));
    }

    private final Iterable<T> iterable;

    private PairSlide( Iterable<T> iterable )
    {
        this.iterable = iterable;
    }

    @Override
    public PeekingIterator<CommonPair<T>> iterator()
    {
        return new PairSlideIterator<>(iterable.iterator());
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    private static final class PairSlideIterator<T> extends AbstractIterator<CommonPair<T>>
        implements PeekingIterator<CommonPair<T>>
    {
        private final Iterator<T> iter;
        private boolean           computedFirstElement;
        private @Nullable T       previous;

        PairSlideIterator( Iterator<T> iter )
        {
            this.iter = iter;
            this.computedFirstElement = false;
            this.previous = null;
        }

        @Override
        protected CommonPair<T> computeNext()
        {
            if (hasAtLeastOneElement() && iter.hasNext()) {
                T current = iter.next();
                CommonPair<T> pair = CommonPair.of(previous, current);
                previous = current;
                return pair;
            }
            else {
                return endOfData();
            }
        }

        private boolean hasAtLeastOneElement()
        {
            if (computedFirstElement) {
                return true;
            }
            else if (iter.hasNext()) {
                previous = iter.next();
                computedFirstElement = true;
                return true;
            }
            else {
                return false;
            }
        }
    }
}
