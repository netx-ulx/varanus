package net.varanus.util.unitvalue;


import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.functional.BasePossible;
import net.varanus.util.functional.Possible;
import net.varanus.util.lang.MoreObjects;


/**
 * @param <U>
 *            The unit type
 * @param <V>
 *            The value type
 * @param <UV>
 *            The unitvalue type
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public interface UnitValue<U, V, UV extends UnitValue<U, V, UV>> extends BasePossible<UV>
{
    @ReturnValuesAreNonnullByDefault
    public static interface UVPair<U, V>
    {
        public U unit();

        public V value();
    }

    public BiFunction<U, V, UVPair<U, V>> pairFactory();

    public UV ifPresent( U unit, Consumer<? super V> consumer );

    public UV ifPresentPair( U unit, Consumer<? super UVPair<U, V>> consumer );

    public V in( U unit ) throws NoSuchElementException;

    public default @Nullable V inOrElse( U unit, @Nullable V other )
    {
        return this.isPresent() ? this.in(unit) : other;
    }

    public default @Nullable V inOrElseGet( U unit, Supplier<? extends V> other )
    {
        return this.isPresent() ? this.in(unit) : other.get();
    }

    public default <X extends Throwable> V inOrElseThrow( U unit, Supplier<? extends X> exSupplier ) throws X
    {
        if (this.isPresent())
            return this.in(unit);
        else
            throw exSupplier.get();
    }

    public default UVPair<U, V> toPair( U unit ) throws NoSuchElementException
    {
        return pairFactory().apply(unit, this.in(unit));
    }

    public default UVPair<U, V> toPairOrElse( U unit, V other )
    {
        if (this.isPresent())
            return toPair(unit);
        else
            return pairFactory().apply(unit, other);
    }

    public default UVPair<U, V> toPairOrElseGet( U unit, Supplier<? extends V> other )
    {
        if (this.isPresent())
            return toPair(unit);
        else
            return pairFactory().apply(unit, other.get());
    }

    public default <X extends Throwable> UVPair<U, V> toPairOrElseThrow( U unit, Supplier<? extends X> exSupplier )
        throws X
    {
        if (this.isPresent())
            return toPair(unit);
        else
            throw exSupplier.get();
    }

    public default boolean contains( U unit, @Nullable Object value )
    {
        if (value == null)
            return !this.isPresent();
        else
            return this.isPresent() && this.in(unit).equals(value);
    }

    public default boolean test( U unit, Predicate<? super V> predicate )
    {
        Objects.requireNonNull(predicate);
        return this.isPresent() && predicate.test(this.in(unit));
    }

    public default boolean testPair( U unit, Predicate<? super UVPair<? extends U, ? extends V>> predicate )
    {
        Objects.requireNonNull(predicate);
        return this.isPresent() && predicate.test(this.toPair(unit));
    }

    public UV filter( U unit, Predicate<? super V> predicate );

    public UV filterPair( U unit, Predicate<? super UVPair<? extends U, ? extends V>> predicate );

    public UV transform( U unit, Function<? super V, ? extends V> transformer );

    public UV transformPair( U unit, Function<? super UVPair<? extends U, ? extends V>, ? extends V> transformer );

    public UV flatTransform( U unit, Function<? super V, ? extends UV> transformer );

    public UV flatTransformPair( U unit,
                                 Function<? super UVPair<? extends U, ? extends V>, ? extends UV> transformer );

    public UV combine( UV other, U unit, BiFunction<? super V, ? super V, ? extends V> combiner );

    public UV combinePair( UV other,
                           U unit,
                           BiFunction<? super UVPair<? extends U, ? extends V>,
                                      ? super UVPair<? extends U, ? extends V>,
                                      ? extends V> combiner );

    public default <T> Possible<T> map( U unit, Function<? super V, ? extends T> mapper )
    {
        Objects.requireNonNull(mapper);
        if (!this.isPresent())
            return Possible.absent();
        else
            return Possible.ofNullable(mapper.apply(this.in(unit)));
    }

    public default <T> Possible<T> mapPair( U unit, Function<? super UVPair<? extends U, ? extends V>, T> mapper )
    {
        Objects.requireNonNull(mapper);
        if (!this.isPresent())
            return Possible.absent();
        else
            return Possible.ofNullable(mapper.apply(this.toPair(unit)));
    }

    public default <T> Possible<T> coMap( UV other, U unit, BiFunction<? super V, ? super V, ? extends T> coMapper )
    {
        Objects.requireNonNull(coMapper);
        if (this.isPresent() && other.isPresent())
            return Possible.ofNullable(coMapper.apply(this.in(unit), other.in(unit)));
        else
            return Possible.absent();
    }

    public default <T> Possible<T> coMapPair( UV other,
                                              U unit,
                                              BiFunction<? super UVPair<? extends U, ? extends V>,
                                                         ? super UVPair<? extends U, ? extends V>,
                                                         ? extends T> coMapper )
    {
        Objects.requireNonNull(coMapper);
        if (this.isPresent() && other.isPresent())
            return Possible.ofNullable(coMapper.apply(this.toPair(unit), other.toPair(unit)));
        else
            return Possible.absent();
    }

    public default <T> Possible<T> flatMap( U unit, Function<? super V, ? extends Possible<T>> mapper )
    {
        Objects.requireNonNull(mapper);
        if (!this.isPresent())
            return Possible.absent();
        else
            return Objects.requireNonNull(mapper.apply(this.in(unit)));
    }

    public default <T> Possible<T> flatMapPair( U unit,
                                                Function<? super UVPair<? extends U, ? extends V>,
                                                         ? extends Possible<T>> mapper )
    {
        Objects.requireNonNull(mapper);
        if (!this.isPresent())
            return Possible.absent();
        else
            return Objects.requireNonNull(mapper.apply(this.toPair(unit)));
    }

    public default <T> Possible<T> flatCoMap( UV other,
                                              U unit,
                                              BiFunction<? super V, ? super V, ? extends Possible<T>> coMapper )
    {
        Objects.requireNonNull(coMapper);
        if (this.isPresent() && other.isPresent())
            return Objects.requireNonNull(coMapper.apply(this.in(unit), other.in(unit)));
        else
            return Possible.absent();
    }

    public default <T> Possible<T> flatCoMapPair( UV other,
                                                  U unit,
                                                  BiFunction<? super UVPair<? extends U, ? extends V>,
                                                             ? super UVPair<? extends U, ? extends V>,
                                                             ? extends Possible<T>> coMapper )
    {
        Objects.requireNonNull(coMapper);
        if (this.isPresent() && other.isPresent())
            return Objects.requireNonNull(coMapper.apply(this.toPair(unit), other.toPair(unit)));
        else
            return Possible.absent();
    }

    public default Possible<V> asPossible( U unit )
    {
        return this.isPresent() ? Possible.of(this.in(unit)) : Possible.absent();
    }

    public default Possible<UVPair<U, V>> asPossiblePair( U unit )
    {
        return this.isPresent() ? Possible.of(this.toPair(unit)) : Possible.absent();
    }

    public default Stream<V> asStream( U unit )
    {
        return this.isPresent() ? Stream.of(this.in(unit)) : Stream.empty();
    }

    public default Stream<UVPair<U, V>> asPairStream( U unit )
    {
        return this.isPresent() ? Stream.of(this.toPair(unit)) : Stream.empty();
    }

    public default Set<V> asSet( U unit )
    {
        return this.isPresent() ? Collections.singleton(this.in(unit)) : Collections.emptySet();
    }

    public default Set<UVPair<U, V>> asPairSet( U unit )
    {
        return this.isPresent() ? Collections.singleton(this.toPair(unit)) : Collections.emptySet();
    }

    public default String toString( U unit )
    {
        return toString(unit, UVPair::toString, "absent");
    }

    public default String toString( U unit,
                                    Function<? super UVPair<? extends U, ? extends V>, String> converter )
    {
        return toString(unit, converter, "absent");
    }

    public default String toString( U unit, String absentString )
    {
        return toString(unit, UVPair::toString, absentString);
    }

    public default String toString( U unit,
                                    Function<? super UVPair<? extends U, ? extends V>, String> converter,
                                    String absentString )
    {
        MoreObjects.requireNonNull(converter, "converter", absentString, "absentString");
        if (this.isPresent())
            return Objects.requireNonNull(converter.apply(this.toPair(unit)));
        else
            return absentString;
    }
}
