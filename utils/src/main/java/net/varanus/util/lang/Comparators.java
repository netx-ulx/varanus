package net.varanus.util.lang;


import java.util.Comparator;
import java.util.Iterator;
import java.util.Objects;
import java.util.stream.Stream;


/**
 * 
 */
public class Comparators
{
    private static final Comparator<?> NATURAL_ORDER_ITERATOR_COMP = comparingIterators(Comparator.naturalOrder());
    private static final Comparator<?> NATURAL_ORDER_ITERABLE_COMP = comparingIterables(Comparator.naturalOrder());
    private static final Comparator<?> NATURAL_ORDER_STREAM_COMP   = comparingStreams(Comparator.naturalOrder());

    @SuppressWarnings( "unchecked" )
    public static <T extends Comparable<? super T>> Comparator<Iterator<T>> comparingIterators()
    {
        return (Comparator<Iterator<T>>)NATURAL_ORDER_ITERATOR_COMP;
    }

    public static <T> Comparator<Iterator<T>> comparingIterators( final Comparator<? super T> elementComp )
    {
        Objects.requireNonNull(elementComp);
        return ( iter1, iter2 ) -> compareIterators(iter1, iter2, elementComp);
    }

    @SuppressWarnings( "unchecked" )
    public static <T extends Comparable<? super T>> Comparator<Iterable<T>> comparingIterables()
    {
        return (Comparator<Iterable<T>>)NATURAL_ORDER_ITERABLE_COMP;
    }

    public static <T> Comparator<Iterable<T>> comparingIterables( final Comparator<? super T> elementComp )
    {
        Objects.requireNonNull(elementComp);
        return ( iter1, iter2 ) -> compareIterators(iter1.iterator(), iter2.iterator(), elementComp);
    }

    @SuppressWarnings( "unchecked" )
    public static <T extends Comparable<? super T>> Comparator<Stream<T>> comparingStreams()
    {
        return (Comparator<Stream<T>>)NATURAL_ORDER_STREAM_COMP;
    }

    public static <T> Comparator<Stream<T>> comparingStreams( final Comparator<? super T> elementComp )
    {
        return ( stream1, stream2 ) -> compareIterators(stream1.iterator(), stream2.iterator(), elementComp);
    }

    private static <T> int compareIterators( Iterator<T> iter1, Iterator<T> iter2, Comparator<? super T> elementComp )
    {
        while (iter1.hasNext() && iter2.hasNext()) {
            int comp = elementComp.compare(iter1.next(), iter2.next());
            if (comp != 0) {
                return comp;
            }
        }

        if (iter1.hasNext()) {
            return 1;
        }
        else if (iter2.hasNext()) {
            return -1;
        }
        else {
            return 0;
        }
    }

    private Comparators()
    {
        // not used
    }
}
