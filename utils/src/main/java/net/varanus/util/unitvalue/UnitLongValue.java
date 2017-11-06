package net.varanus.util.unitvalue;


import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.function.LongSupplier;
import java.util.function.LongUnaryOperator;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToLongBiFunction;
import java.util.function.ToLongFunction;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.functional.BasePossible;
import net.varanus.util.functional.Possible;
import net.varanus.util.functional.PossibleLong;
import net.varanus.util.functional.functions.LongBiFunction;
import net.varanus.util.functional.functions.ObjLongFunction;
import net.varanus.util.lang.MoreObjects;


/**
 * @param <U>
 *            The unit type
 * @param <ULV>
 *            The unitvalue type
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public interface UnitLongValue<U, ULV extends UnitLongValue<U, ULV>> extends BasePossible<ULV>
{
    @ReturnValuesAreNonnullByDefault
    public static interface ULongPair<U>
    {
        public U unit();

        public long value();
    }

    public ObjLongFunction<U, ULongPair<U>> pairFactory();

    public ULV ifPresent( U unit, LongConsumer consumer );

    public ULV ifPresentPair( U unit, Consumer<? super ULongPair<U>> consumer );

    public long in( U unit ) throws NoSuchElementException;

    public default long inOrElse( U unit, long other )
    {
        return this.isPresent() ? this.in(unit) : other;
    }

    public default long inOrElseGet( U unit, LongSupplier other )
    {
        return this.isPresent() ? this.in(unit) : other.getAsLong();
    }

    public default <X extends Throwable> long inOrElseThrow( U unit, Supplier<? extends X> exSupplier ) throws X
    {
        if (this.isPresent())
            return this.in(unit);
        else
            throw exSupplier.get();
    }

    public default ULongPair<U> toPair( U unit ) throws NoSuchElementException
    {
        return pairFactory().apply(unit, this.in(unit));
    }

    public default ULongPair<U> toPairOrElse( U unit, long other )
    {
        if (this.isPresent())
            return toPair(unit);
        else
            return pairFactory().apply(unit, other);
    }

    public default ULongPair<U> toPairOrElseGet( U unit, LongSupplier other )
    {
        if (this.isPresent())
            return toPair(unit);
        else
            return pairFactory().apply(unit, other.getAsLong());
    }

    public default <X extends Throwable> ULongPair<U> toPairOrElseThrow( U unit, Supplier<? extends X> exSupplier )
        throws X
    {
        if (this.isPresent())
            return toPair(unit);
        else
            throw exSupplier.get();
    }

    public default boolean contains( U unit, long value )
    {
        return this.isPresent() && this.in(unit) == value;
    }

    public default boolean test( U unit, LongPredicate predicate )
    {
        Objects.requireNonNull(predicate);
        return this.isPresent() && predicate.test(this.in(unit));
    }

    public default boolean testPair( U unit, Predicate<? super ULongPair<? extends U>> predicate )
    {
        Objects.requireNonNull(predicate);
        return this.isPresent() && predicate.test(this.toPair(unit));
    }

    public ULV filter( U unit, LongPredicate predicate );

    public ULV filterPair( U unit, Predicate<? super ULongPair<? extends U>> predicate );

    public ULV transform( U unit, LongUnaryOperator transformer );

    public ULV transformPair( U unit, ToLongFunction<? super ULongPair<? extends U>> transformer );

    public ULV flatTransform( U unit, LongFunction<? extends ULV> transformer );

    public ULV flatTransformPair( U unit, Function<? super ULongPair<? extends U>, ? extends ULV> transformer );

    public ULV combine( ULV other, U unit, LongBinaryOperator combiner );

    public ULV combinePair( ULV other,
                            U unit,
                            ToLongBiFunction<? super ULongPair<? extends U>,
                                             ? super ULongPair<? extends U>> combiner );

    public default <T> Possible<T> map( U unit, LongFunction<? extends T> mapper )
    {
        Objects.requireNonNull(mapper);
        if (!this.isPresent())
            return Possible.absent();
        else
            return Possible.ofNullable(mapper.apply(this.in(unit)));
    }

    public default <T> Possible<T> mapPair( U unit, Function<? super ULongPair<? extends U>, T> mapper )
    {
        Objects.requireNonNull(mapper);
        if (!this.isPresent())
            return Possible.absent();
        else
            return Possible.ofNullable(mapper.apply(this.toPair(unit)));
    }

    public default <T> Possible<T> coMap( ULV other, U unit, LongBiFunction<? extends T> coMapper )
    {
        Objects.requireNonNull(coMapper);
        if (this.isPresent() && other.isPresent())
            return Possible.ofNullable(coMapper.apply(this.in(unit), other.in(unit)));
        else
            return Possible.absent();
    }

    public default <T> Possible<T> coMapPair( ULV other,
                                              U unit,
                                              BiFunction<? super ULongPair<? extends U>,
                                                         ? super ULongPair<? extends U>,
                                                         ? extends T> coMapper )
    {
        Objects.requireNonNull(coMapper);
        if (this.isPresent() && other.isPresent())
            return Possible.ofNullable(coMapper.apply(this.toPair(unit), other.toPair(unit)));
        else
            return Possible.absent();
    }

    public default <T> Possible<T> flatMap( U unit, LongFunction<? extends Possible<T>> mapper )
    {
        Objects.requireNonNull(mapper);
        if (!this.isPresent())
            return Possible.absent();
        else
            return Objects.requireNonNull(mapper.apply(this.in(unit)));
    }

    public default <T> Possible<T> flatMapPair( U unit,
                                                Function<? super ULongPair<? extends U>, ? extends Possible<T>> mapper )
    {
        Objects.requireNonNull(mapper);
        if (!this.isPresent())
            return Possible.absent();
        else
            return Objects.requireNonNull(mapper.apply(this.toPair(unit)));
    }

    public default <T> Possible<T> flatCoMap( ULV other, U unit, LongBiFunction<? extends Possible<T>> coMapper )
    {
        Objects.requireNonNull(coMapper);
        if (this.isPresent() && other.isPresent())
            return Objects.requireNonNull(coMapper.apply(this.in(unit), other.in(unit)));
        else
            return Possible.absent();
    }

    public default <T> Possible<T> flatCoMapPair( ULV other,
                                                  U unit,
                                                  BiFunction<? super ULongPair<? extends U>,
                                                             ? super ULongPair<? extends U>,
                                                             ? extends Possible<T>> coMapper )
    {
        Objects.requireNonNull(coMapper);
        if (this.isPresent() && other.isPresent())
            return Objects.requireNonNull(coMapper.apply(this.toPair(unit), other.toPair(unit)));
        else
            return Possible.absent();
    }

    public default PossibleLong asPossible( U unit )
    {
        return this.isPresent() ? PossibleLong.of(this.in(unit)) : PossibleLong.absent();
    }

    public default Possible<ULongPair<U>> asPossiblePair( U unit )
    {
        return this.isPresent() ? Possible.of(this.toPair(unit)) : Possible.absent();
    }

    public default LongStream asStream( U unit )
    {
        return this.isPresent() ? LongStream.of(this.in(unit)) : LongStream.empty();
    }

    public default Stream<ULongPair<U>> asPairStream( U unit )
    {
        return this.isPresent() ? Stream.of(this.toPair(unit)) : Stream.empty();
    }

    public default Set<ULongPair<U>> asPairSet( U unit )
    {
        return this.isPresent() ? Collections.singleton(this.toPair(unit)) : Collections.emptySet();
    }

    public default String toString( U unit )
    {
        return toString(unit, ULongPair::toString, "absent");
    }

    public default String toString( U unit, Function<? super ULongPair<? extends U>, String> converter )
    {
        return toString(unit, converter, "absent");
    }

    public default String toString( U unit, String absentString )
    {
        return toString(unit, ULongPair::toString, absentString);
    }

    public default String toString( U unit,
                                    Function<? super ULongPair<? extends U>, String> converter,
                                    String absentString )
    {
        MoreObjects.requireNonNull(converter, "converter", absentString, "absentString");
        if (this.isPresent())
            return Objects.requireNonNull(converter.apply(this.toPair(unit)));
        else
            return absentString;
    }
}
