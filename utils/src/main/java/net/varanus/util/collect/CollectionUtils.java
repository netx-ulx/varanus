package net.varanus.util.collect;


import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.functional.FunctionUtils;
import net.varanus.util.functional.OptionalUtils;
import net.varanus.util.functional.StreamUtils;
import net.varanus.util.lang.Comparators;


/**
 * 
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class CollectionUtils
{
    public static <T> Optional<T> findAny( Collection<T> col, Predicate<? super T> filter )
    {
        return col.stream().filter(filter).findAny();
    }

    public static <T> Optional<T> findFirst( Collection<T> col, Predicate<? super T> filter )
    {
        return col.stream().filter(filter).findFirst();
    }

    public static <K> Optional<K> findAnyKey( Map<K, ?> map, Predicate<? super K> filter )
    {
        return findAny(map.keySet(), filter);
    }

    public static <K> Optional<K> findFirstKey( Map<K, ?> map, Predicate<? super K> filter )
    {
        return findFirst(map.keySet(), filter);
    }

    public static <V> Optional<V> findAnyValue( Map<?, V> map, Predicate<? super V> filter )
    {
        return findAny(map.values(), filter);
    }

    public static <V> Optional<V> findFirstValue( Map<?, V> map, Predicate<? super V> filter )
    {
        return findFirst(map.values(), filter);
    }

    public static <K, V> Optional<Entry<K, V>> findAnyEntry( Map<K, V> map, Predicate<? super Entry<K, V>> filter )
    {
        return findAny(map.entrySet(), filter);
    }

    public static <K, V> Optional<Entry<K, V>> findFirstEntry( Map<K, V> map, Predicate<? super Entry<K, V>> filter )
    {
        return findFirst(map.entrySet(), filter);
    }

    public static <K, V> Optional<Entry<K, V>> findAnyEntry( Map<K, V> map,
                                                             Predicate<? super K> keyFilter,
                                                             Predicate<? super V> valueFilter )
    {
        return findAny(map.entrySet(), ( e ) -> keyFilter.test(e.getKey()) && valueFilter.test(e.getValue()));
    }

    public static <K, V> Optional<Entry<K, V>> findFirstEntry( Map<K, V> map,
                                                               Predicate<? super K> keyFilter,
                                                               Predicate<? super V> valueFilter )
    {
        return findFirst(map.entrySet(), ( e ) -> keyFilter.test(e.getKey()) && valueFilter.test(e.getValue()));
    }

    public static <K, V> Optional<V> findAnyValueByKey( Map<K, V> map, Predicate<? super K> keyFilter )
    {
        return findAnyEntry(map, ( e ) -> keyFilter.test(e.getKey())).map(Entry::getValue);
    }

    public static <K, V> Optional<V> findFirstValueByKey( Map<K, V> map, Predicate<? super K> keyFilter )
    {
        return findFirstEntry(map, ( e ) -> keyFilter.test(e.getKey())).map(Entry::getValue);
    }

    public static @Nullable <K, V> V get( Map<K, V> map, @Nullable K key, Supplier<? extends V> defaultSupplier )
    {
        /*
         * As of Java 8, Map.getOrDefault(K, V) only accepts a default value,
         * not a supplier or function
         */

        V v;
        if (((v = map.get(key)) != null) || map.containsKey(key))
            return v;
        else
            return defaultSupplier.get();
    }

    public static @Nullable <K, V> V get( Map<K, V> map,
                                          @Nullable K key,
                                          Function<? super K, ? extends V> defaultFunction )
    {
        /*
         * As of Java 8, Map.getOrDefault(K, V) only accepts a default value,
         * not a supplier or function
         */

        V v;
        if (((v = map.get(key)) != null) || map.containsKey(key))
            return v;
        else
            return defaultFunction.apply(key);
    }

    public static @Nullable <K, V> V computeIfAbsent( Map<K, V> map,
                                                      @Nullable K key,
                                                      Supplier<? extends V> valueSupplier )
    {
        /*
         * As of Java 8, Map.computeIfAbsent only accepts a function.
         */

        return map.computeIfAbsent(key, FunctionUtils.asFunction(valueSupplier));
    }

    public static boolean equals( Iterable<?> iter1, Iterable<?> iter2 )
    {
        if ((iter1 instanceof Collection<?>) && (iter2 instanceof Collection<?>))
            return ((Collection<?>)iter1).equals(iter2);
        else
            return equalsBy(iter1, iter2, Objects::equals);
    }

    public static <T, U> boolean equalsBy( Iterable<T> iter1,
                                           Iterable<U> iter2,
                                           BiPredicate<? super T, ? super U> comparator )
    {
        Iterator<T> iterator1 = iter1.iterator();
        Iterator<U> iterator2 = iter2.iterator();
        Objects.requireNonNull(comparator);
        while (iterator1.hasNext()) {
            if (!iterator2.hasNext()) {
                return false;
            }
            T t = iterator1.next();
            U u = iterator2.next();
            if (!comparator.test(t, u)) {
                return false;
            }
        }
        return !iterator2.hasNext();
    }

    public static <T> boolean equalsBy( Iterable<T> iter1, Iterable<T> iter2, Comparator<? super T> comparator )
    {
        Comparator<Iterable<T>> iterComp = Comparators.comparingIterables(comparator);
        return iterComp.compare(iter1, iter2) == 0;
    }

    public static <T extends Comparable<? super T>> Iterable<T> sortedIterable( Iterable<T> iter )
    {
        return () -> StreamUtils.toSequentialStream(iter).sorted().iterator();
    }

    public static <T> Iterable<T> sortedIterable( Iterable<T> iter, Comparator<T> elementComp )
    {
        return () -> StreamUtils.toSequentialStream(iter).sorted(elementComp).iterator();
    }

    @SafeVarargs
    @SuppressWarnings( "varargs" )
    public static <T> Iterable<T> iterableCopyOf( T... elements )
    {
        return listCopyOf(elements);
    }

    @SafeVarargs
    @SuppressWarnings( "varargs" )
    public static <T> List<T> listCopyOf( T... elements )
    {
        return copyOf(() -> newArrayList(elements.length), elements);
    }

    @SafeVarargs
    @SuppressWarnings( "varargs" )
    public static <T> Set<T> setCopyOf( T... elements )
    {
        return copyOf(() -> newLinkedHashSet(elements.length), elements);
    }

    @SafeVarargs
    @SuppressWarnings( "varargs" )
    public static <T, C extends Collection<T>> C copyOf( Supplier<C> colFactory, T... elements )
    {
        return toCollection(Arrays.asList(elements), Function.identity(), colFactory);
    }

    public static <K, V> Map<K, V> copyOf( Map<K, V> map )
    {
        return toMap(map, Function.identity(), Function.identity(), () -> newLinkedHashMap(map.size()));
    }

    public static <E0, E> List<E> toList( Collection<E0> col, Function<? super E0, ? extends E> mapper )
    {
        return toCollection(col, mapper, () -> newArrayList(col.size()));
    }

    public static <E0, E> List<E> toFlatList( Collection<E0> col,
                                              Function<? super E0, ? extends Stream<? extends E>> mapper )
    {
        // here we do not guess the initial list size due to not knowing how
        // many elements each mapped stream has
        return toFlatCollection(col, mapper, ArrayList::new);
    }

    public static <E0, E> List<E> toOptFlatList( Collection<E0> col,
                                                 Function<? super E0, Optional<? extends E>> mapper )
    {
        // here we do not guess the initial list size due to not knowing how
        // many present optional elements are mapped
        return toOptFlatCollection(col, mapper, ArrayList::new);
    }

    public static <E0, E> Set<E> toSet( Collection<E0> col, Function<? super E0, ? extends E> mapper )
    {
        return toCollection(col, mapper, () -> newLinkedHashSet(col.size()));
    }

    public static <E0, E> Set<E> toFlatSet( Collection<E0> col,
                                            Function<? super E0, ? extends Stream<? extends E>> mapper )
    {
        // here we do not guess the initial set size due to not knowing how
        // many elements each mapped stream has
        return toFlatCollection(col, mapper, LinkedHashSet::new);
    }

    public static <E0, E> Set<E> toOptFlatSet( Collection<E0> col,
                                               Function<? super E0, Optional<? extends E>> mapper )
    {
        // here we do not guess the initial set size due to not knowing how
        // many present optional elements are mapped
        return toOptFlatCollection(col, mapper, LinkedHashSet::new);
    }

    public static <E0, E, C extends Collection<E>> C toCollection( Collection<E0> col,
                                                                   Function<? super E0, ? extends E> mapper,
                                                                   Supplier<C> colFactory )
    {
        return col.stream()
            .map(mapper)
            .collect(Collectors.toCollection(colFactory));
    }

    public static <E0, E, C extends Collection<E>> C toFlatCollection( Collection<E0> col,
                                                                       Function<? super E0,
                                                                                ? extends Stream<? extends E>> mapper,
                                                                       Supplier<C> colFactory )
    {
        return col.stream()
            .flatMap(mapper)
            .collect(Collectors.toCollection(colFactory));
    }

    public static <E0, E, C extends Collection<E>> C toOptFlatCollection( Collection<E0> col,
                                                                          Function<? super E0,
                                                                                   Optional<? extends E>> mapper,
                                                                          Supplier<C> colFactory )
    {
        return col.stream()
            .flatMap(e -> OptionalUtils.asStream(mapper.apply(e)))
            .collect(Collectors.toCollection(colFactory));
    }

    public static <K0, V0, K, V> Map<K, V> toMap( Map<K0, V0> map,
                                                  Function<? super K0, ? extends K> keyMapper,
                                                  Function<? super V0, ? extends V> valMapper )
    {
        return toMap(map, keyMapper, valMapper, () -> newLinkedHashMap(map.size()));
    }

    public static <K0, V0, K, V> Map<K, V> toMap( Map<K0, V0> map,
                                                  Function<? super Entry<? extends K0, ? extends V0>,
                                                           ? extends Entry<? extends K, ? extends V>> entryMapper )
    {
        return toMap(map, entryMapper, () -> newLinkedHashMap(map.size()));
    }

    public static <K0, V0, K, V> Map<K, V> toFlatMap( Map<K0, V0> map,
                                                      Function<? super Entry<? extends K0, ? extends V0>,
                                                               ? extends Stream<? extends Entry<? extends K,
                                                                                                ? extends V>>> entryMapper )
    {
        // here we do not guess the initial set size due to not knowing how
        // many elements each mapped stream has
        return toFlatMap(map, entryMapper, LinkedHashMap::new);
    }

    public static <K0, V0, K, V> Map<K, V> toOptFlatMap( Map<K0, V0> map,
                                                         Function<? super Entry<? extends K0, ? extends V0>,
                                                                  Optional<? extends Entry<? extends K,
                                                                                           ? extends V>>> entryMapper )
    {
        // here we do not guess the initial set size due to not knowing how
        // many present optional elements are mapped
        return toOptFlatMap(map, entryMapper, LinkedHashMap::new);
    }

    public static <E, K, V> Map<K, V> toMap( Collection<E> col,
                                             Function<? super E, ? extends K> keyMapper,
                                             Function<? super E, ? extends V> valMapper )
    {
        return toMap(col, keyMapper, valMapper, () -> newLinkedHashMap(col.size()));
    }

    public static <E, K, V> Map<K, V> toMap( Collection<E> col,
                                             Function<? super E,
                                                      ? extends Entry<? extends K, ? extends V>> entryMapper )
    {
        return toMap(col, entryMapper, () -> newLinkedHashMap(col.size()));
    }

    public static <E, K, V> Map<K, V> toFlatMap( Collection<E> col,
                                                 Function<? super E,
                                                          ? extends Stream<? extends Entry<? extends K,
                                                                                           ? extends V>>> entryMapper )
    {
        // here we do not guess the initial map size due to not knowing how
        // many elements each mapped stream has
        return toFlatMap(col, entryMapper, LinkedHashMap::new);
    }

    public static <E, K, V> Map<K, V> toOptFlatMap( Collection<E> col,
                                                    Function<? super E,
                                                             Optional<? extends Entry<? extends K,
                                                                                      ? extends V>>> entryMapper )
    {
        // here we do not guess the initial map size due to not knowing how
        // many present optional elements are mapped
        return toOptFlatMap(col, entryMapper, LinkedHashMap::new);
    }

    public static <K0, V0, K, V, M extends Map<K, V>> M toMap( Map<K0, V0> map,
                                                               Function<? super K0, ? extends K> keyMapper,
                                                               Function<? super V0, ? extends V> valMapper,
                                                               Supplier<M> mapFactory )
    {
        return toMap(map.entrySet(), e -> keyMapper.apply(e.getKey()), e -> valMapper.apply(e.getValue()), mapFactory);
    }

    public static <K0, V0, K, V, M extends Map<K, V>> M toMap( Map<K0, V0> map,
                                                               Function<? super Entry<? extends K0, ? extends V0>,
                                                                        ? extends Entry<? extends K,
                                                                                        ? extends V>> entryMapper,
                                                               Supplier<M> mapFactory )
    {
        return toMap(map.entrySet(), entryMapper, mapFactory);
    }

    public static <K0, V0, K, V, M extends Map<K, V>> M toFlatMap( Map<K0, V0> map,
                                                                   Function<? super Entry<? extends K0, ? extends V0>,
                                                                            ? extends Stream<? extends Entry<? extends K,
                                                                                                             ? extends V>>> entryMapper,
                                                                   Supplier<M> mapFactory )
    {
        return toFlatMap(map.entrySet(), entryMapper, mapFactory);
    }

    public static <K0, V0, K, V, M extends Map<K, V>> M toOptFlatMap( Map<K0, V0> map,
                                                                      Function<? super Entry<? extends K0,
                                                                                             ? extends V0>,
                                                                               Optional<? extends Entry<? extends K,
                                                                                                        ? extends V>>> entryMapper,
                                                                      Supplier<M> mapFactory )
    {
        return toOptFlatMap(map.entrySet(), entryMapper, mapFactory);
    }

    public static <E, K, V, M extends Map<K, V>> M toMap( Collection<E> col,
                                                          Function<? super E, ? extends K> keyMapper,
                                                          Function<? super E, ? extends V> valMapper,
                                                          Supplier<M> mapFactory )
    {
        return col.stream()
            .collect(StreamUtils.toMapCollector(
                keyMapper,
                valMapper,
                mapFactory));
    }

    public static <E, K, V, M extends Map<K, V>> M toMap( Collection<E> col,
                                                          Function<? super E,
                                                                   ? extends Entry<? extends K,
                                                                                   ? extends V>> entryMapper,
                                                          Supplier<M> mapFactory )
    {
        return col.stream()
            .collect(StreamUtils.toMapCollector(
                e -> entryMapper.apply(e).getKey(),
                e -> entryMapper.apply(e).getValue(),
                mapFactory));
    }

    public static <E, K, V, M extends Map<K, V>> M toFlatMap( Collection<E> col,
                                                              Function<? super E,
                                                                       ? extends Stream<? extends Entry<? extends K,
                                                                                                        ? extends V>>> entryMapper,
                                                              Supplier<M> mapFactory )
    {
        return col.stream()
            .flatMap(entryMapper)
            .collect(StreamUtils.toMapCollector(
                Entry::getKey,
                Entry::getValue,
                mapFactory));
    }

    public static <E, K, V, M extends Map<K, V>> M toOptFlatMap( Collection<E> col,
                                                                 Function<? super E,
                                                                          Optional<? extends Entry<? extends K,
                                                                                                   ? extends V>>> entryMapper,
                                                                 Supplier<M> mapFactory )
    {
        return col.stream()
            .flatMap(e -> OptionalUtils.asStream(entryMapper.apply(e)))
            .collect(StreamUtils.toMapCollector(
                e -> e.getKey(),
                e -> e.getValue(),
                mapFactory));
    }

    public static <E0, E> E[] toArray( Collection<E0> col, Class<E> arrayCompType )
    {
        return toArray(col, FunctionUtils.castFunction(), arrayCompType);
    }

    public static <E0, E> E[] toArray( Collection<E0> col, IntFunction<E[]> arrayFactory )
    {
        return toArray(col, FunctionUtils.castFunction(), arrayFactory);
    }

    @SuppressWarnings( "unchecked" )
    public static <E0, E> E[] toArray( Collection<E0> col, Function<E0, E> mapper, Class<E> arrayCompType )
    {
        return toArray(col, mapper, ( len ) -> (E[])Array.newInstance(arrayCompType, len));
    }

    public static <E0, E> E[] toArray( Collection<E0> col, Function<E0, E> mapper, IntFunction<E[]> arrayFactory )
    {
        return col.stream()
            .map(mapper)
            .toArray(arrayFactory);
    }

    /*
     * Copied from com.google.common.collect.Maps
     */
    public static int initialMapCapacity( int expectedSize )
    {
        if (expectedSize < 3) {
            if (expectedSize < 0)
                throw new IllegalArgumentException("expected size is negative");
            return expectedSize + 1;
        }
        if (expectedSize < (1 << (Integer.SIZE - 2))) {
            return expectedSize + expectedSize / 3;
        }
        return Integer.MAX_VALUE; // any large value
    }

    private static <T> ArrayList<T> newArrayList( int initialSize )
    {
        return new ArrayList<>(initialSize);
    }

    private static <T> LinkedHashSet<T> newLinkedHashSet( int initialSize )
    {
        return new LinkedHashSet<>(initialMapCapacity(initialSize));
    }

    private static <K, V> LinkedHashMap<K, V> newLinkedHashMap( int initialSize )
    {
        return new LinkedHashMap<>(initialMapCapacity(initialSize));
    }

    private CollectionUtils()
    {
        // not used
    }
}
