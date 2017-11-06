package net.varanus.util.functional;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import net.varanus.util.collect.CommonPair;
import net.varanus.util.collect.PairSlide;


/**
 * 
 */
public final class StreamUtils
{
    public static <A, B> void zipRun( Stream<A> streamA, Stream<B> streamB, BiConsumer<? super A, ? super B> consumer )
    {
        zip(streamA, streamB, ( a, b ) -> {
            consumer.accept(a, b);
            return (Void)null;
        }).forEachOrdered(FunctionUtils.ignore());
    }

    public static <A, B, C> Stream<C> zip( Stream<A> streamA,
                                           Stream<B> streamB,
                                           BiFunction<? super A, ? super B, ? extends C> zipper )
    {
        Objects.requireNonNull(zipper);
        Iterator<A> iteratorA = streamA.iterator();
        Iterator<B> iteratorB = streamB.iterator();
        Iterator<C> iteratorC = new Iterator<C>() {
            @Override
            public boolean hasNext()
            {
                return iteratorA.hasNext() && iteratorB.hasNext();
            }

            @Override
            public C next()
            {
                return zipper.apply(iteratorA.next(), iteratorB.next());
            }
        };

        boolean parallel = streamA.isParallel() || streamB.isParallel();
        return toStream(parallel, iteratorC);
    }

    @SafeVarargs
    @SuppressWarnings( "varargs" )
    public static <T> Stream<List<T>> zipIterables( Iterable<T>... iterables )
    {
        return zipIterables(Arrays.asList(iterables));
    }

    public static <T> Stream<List<T>> zipIterables( Iterable<? extends Iterable<T>> iterables )
    {
        List<Iterator<T>> iterators = new ArrayList<>();
        for (Iterable<T> it : iterables) {
            iterators.add(it.iterator());
        }

        return toZipped(iterators, false);
    }

    @SafeVarargs
    @SuppressWarnings( "varargs" )
    public static <T> Stream<List<T>> zipStreams( Stream<T>... streams )
    {
        return zipStreams(Arrays.asList(streams));
    }

    public static <T> Stream<List<T>> zipStreams( Iterable<? extends Stream<T>> streams )
    {
        List<Iterator<T>> iterators = new ArrayList<>();
        boolean parallel = false;
        for (Stream<T> st : streams) {
            iterators.add(st.iterator());
            if (st.isParallel())
                parallel = true;
        }

        return toZipped(iterators, parallel);
    }

    private static <T> Stream<List<T>> toZipped( final List<Iterator<T>> iterators, boolean parallel )
    {
        Iterator<List<T>> zipIterator = new Iterator<List<T>>() {
            @Override
            public boolean hasNext()
            {
                for (Iterator<T> it : iterators) {
                    if (!it.hasNext())
                        return false;
                }
                return true;
            }

            @Override
            public List<T> next()
            {
                List<T> zipped = new ArrayList<>(iterators.size());
                for (Iterator<T> it : iterators) {
                    zipped.add(it.next());
                }
                return zipped;
            }
        };

        return toStream(parallel, zipIterator);
    }

    @SafeVarargs
    @SuppressWarnings( "varargs" )
    public static <T> Stream<T> toSequentialStream( T... elements )
    {
        return toStream(false, elements);
    }

    @SafeVarargs
    @SuppressWarnings( "varargs" )
    public static <T> Stream<T> toParallelStream( T... elements )
    {
        return toStream(true, elements);
    }

    @SafeVarargs
    @SuppressWarnings( "varargs" )
    public static <T> Stream<T> toStream( boolean parallel, T... elements )
    {
        Stream<T> s = (elements != null) ? Stream.of(elements) : Stream.empty();
        s = parallel ? s.parallel() : s.sequential();
        return s;
    }

    public static <T> Stream<T> toSequentialStream( Iterable<T> iterable )
    {
        return toStream(false, iterable);
    }

    public static <T> Stream<T> toParallelStream( Iterable<T> iterable )
    {
        return toStream(true, iterable);
    }

    public static <T> Stream<T> toStream( boolean parallel, Iterable<T> iterable )
    {
        return (iterable instanceof Collection) ? toStream(parallel, (Collection<T>)iterable)
                                                : StreamSupport.stream(iterable.spliterator(), parallel);
    }

    public static <T> Stream<T> toSequentialStream( Collection<T> collection )
    {
        return toStream(false, collection);
    }

    public static <T> Stream<T> toParallelStream( Collection<T> collection )
    {
        return toStream(true, collection);
    }

    public static <T> Stream<T> toStream( boolean parallel, Collection<T> collection )
    {
        return parallel ? collection.parallelStream() : collection.stream();
    }

    public static <T> Stream<T> toSequentialStream( Iterator<T> iterator )
    {
        return toStream(false, iterator);
    }

    public static <T> Stream<T> toParallelStream( Iterator<T> iterator )
    {
        return toStream(true, iterator);
    }

    public static <T> Stream<T> toStream( boolean parallel, Iterator<T> iterator )
    {
        Spliterator<T> spliterator = Spliterators.spliteratorUnknownSize(iterator, 0);
        return StreamSupport.stream(spliterator, parallel);
    }

    public static IntStream toSequentialIntStream( int... elements )
    {
        return toIntStream(false, elements);
    }

    public static IntStream toParallelIntStream( int... elements )
    {
        return toIntStream(true, elements);
    }

    public static IntStream toIntStream( boolean parallel, int... elements )
    {
        IntStream s = (elements != null) ? IntStream.of(elements) : IntStream.empty();
        s = parallel ? s.parallel() : s.sequential();
        return s;
    }

    public static IntStream toSequentialIntStream( PrimitiveIterator.OfInt iterator )
    {
        return toIntStream(false, iterator);
    }

    public static IntStream toParallelIntStream( PrimitiveIterator.OfInt iterator )
    {
        return toIntStream(true, iterator);
    }

    public static IntStream toIntStream( boolean parallel, PrimitiveIterator.OfInt iterator )
    {
        Spliterator.OfInt spliterator = Spliterators.spliteratorUnknownSize(iterator, 0);
        return StreamSupport.intStream(spliterator, parallel);
    }

    public static LongStream toSequentialLongStream( long... elements )
    {
        return toLongStream(false, elements);
    }

    public static LongStream toParallelLongStream( long... elements )
    {
        return toLongStream(true, elements);
    }

    public static LongStream toLongStream( boolean parallel, long... elements )
    {
        LongStream s = (elements != null) ? LongStream.of(elements) : LongStream.empty();
        s = parallel ? s.parallel() : s.sequential();
        return s;
    }

    public static LongStream toSequentialLongStream( PrimitiveIterator.OfLong iterator )
    {
        return toLongStream(false, iterator);
    }

    public static LongStream toParallelLongStream( PrimitiveIterator.OfLong iterator )
    {
        return toLongStream(true, iterator);
    }

    public static LongStream toLongStream( boolean parallel, PrimitiveIterator.OfLong iterator )
    {
        Spliterator.OfLong spliterator = Spliterators.spliteratorUnknownSize(iterator, 0);
        return StreamSupport.longStream(spliterator, parallel);
    }

    public static DoubleStream toSequentialDoubleStream( double... elements )
    {
        return toDoubleStream(false, elements);
    }

    public static DoubleStream toParallelDoubleStream( double... elements )
    {
        return toDoubleStream(true, elements);
    }

    public static DoubleStream toDoubleStream( boolean parallel, double... elements )
    {
        DoubleStream s = (elements != null) ? DoubleStream.of(elements) : DoubleStream.empty();
        s = parallel ? s.parallel() : s.sequential();
        return s;
    }

    public static DoubleStream toSequentialDoubleStream( PrimitiveIterator.OfDouble iterator )
    {
        return toDoubleStream(false, iterator);
    }

    public static DoubleStream toParallelDoubleStream( PrimitiveIterator.OfDouble iterator )
    {
        return toDoubleStream(true, iterator);
    }

    public static DoubleStream toDoubleStream( boolean parallel, PrimitiveIterator.OfDouble iterator )
    {
        Spliterator.OfDouble spliterator = Spliterators.spliteratorUnknownSize(iterator, 0);
        return StreamSupport.doubleStream(spliterator, parallel);
    }

    public static String toString( Stream<?> stream )
    {
        return stream.map(String::valueOf).collect(Collectors.joining(", ", "[", "]"));
    }

    public static String toString( IntStream stream )
    {
        return stream.mapToObj(String::valueOf).collect(Collectors.joining(", ", "[", "]"));
    }

    public static String toString( LongStream stream )
    {
        return stream.mapToObj(String::valueOf).collect(Collectors.joining(", ", "[", "]"));
    }

    public static String toString( DoubleStream stream )
    {
        return stream.mapToObj(String::valueOf).collect(Collectors.joining(", ", "[", "]"));
    }

    public static <T> Stream<CommonPair<T>> toPairSlideStream( Stream<T> stream )
    {
        return toPairSlideStream(stream.iterator());
    }

    public static <T> Stream<CommonPair<T>> toPairSlideStream( Iterator<T> iter )
    {
        return toSequentialStream(PairSlide.over(() -> iter));
    }

    public static <T> ImmutableList<T> toImmutableList( Stream<T> stream )
    {
        return ImmutableList.copyOf(stream.iterator());
    }

    public static <T> ImmutableSet<T> toImmutableSet( Stream<T> stream )
    {
        return ImmutableSet.copyOf(stream.iterator());
    }

    public static <T, K, V, M extends Map<K, V>> Collector<T, ?, M> toMapCollector( Function<? super T,
                                                                                             ? extends K> keyMapper,
                                                                                    Function<? super T,
                                                                                             ? extends V> valueMapper,
                                                                                    Supplier<M> mapSupplier )
    {
        return Collectors.toMap(keyMapper, valueMapper, throwingMerger(), mapSupplier);
    }

    /**
     * NOTE: Copied from java.util.stream.Collectors
     * <p>
     * Returns a merge function, suitable for use in
     * {@link Map#merge(Object, Object, BiFunction) Map.merge()} or
     * {@link Collectors#toMap(Function, Function, BinaryOperator) toMap()},
     * which always
     * throws {@code IllegalStateException}. This can be used to enforce the
     * assumption that the elements being collected are distinct.
     *
     * @param <T>
     *            the type of input arguments to the merge function
     * @return a merge function which always throw {@code IllegalStateException}
     */
    private static <T> BinaryOperator<T> throwingMerger()
    {
        return ( u, v ) -> {
            throw new IllegalStateException(String.format("Duplicate key %s", u));
        };
    }

    private StreamUtils()
    {
        // not used
    }
}
