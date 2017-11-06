package net.varanus.util.unitvalue;


import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToDoubleFunction;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.functional.BasePossible;
import net.varanus.util.functional.Possible;
import net.varanus.util.functional.PossibleDouble;
import net.varanus.util.functional.functions.DoubleBiFunction;
import net.varanus.util.functional.functions.ObjDoubleFunction;
import net.varanus.util.lang.MoreObjects;


/**
 * @param <U>
 *            The unit type
 * @param <UDV>
 *            The unitvalue type
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public interface UnitDoubleValue<U, UDV extends UnitDoubleValue<U, UDV>> extends BasePossible<UDV>
{
    @ReturnValuesAreNonnullByDefault
    public static interface UDoublePair<U>
    {
        public U unit();

        public double value();
    }

    public ObjDoubleFunction<U, UDoublePair<U>> pairFactory();

    public UDV ifPresent( U unit, DoubleConsumer consumer );

    public UDV ifPresentPair( U unit, Consumer<? super UDoublePair<U>> consumer );

    public double in( U unit ) throws NoSuchElementException;

    public default double inOrElse( U unit, double other )
    {
        return this.isPresent() ? this.in(unit) : other;
    }

    public default double inOrElseGet( U unit, DoubleSupplier other )
    {
        return this.isPresent() ? this.in(unit) : other.getAsDouble();
    }

    public default <X extends Throwable> double inOrElseThrow( U unit, Supplier<? extends X> exSupplier ) throws X
    {
        if (this.isPresent())
            return this.in(unit);
        else
            throw exSupplier.get();
    }

    public default UDoublePair<U> toPair( U unit ) throws NoSuchElementException
    {
        return pairFactory().apply(unit, this.in(unit));
    }

    public default UDoublePair<U> toPairOrElse( U unit, double other )
    {
        if (this.isPresent())
            return toPair(unit);
        else
            return pairFactory().apply(unit, other);
    }

    public default UDoublePair<U> toPairOrElseGet( U unit, DoubleSupplier other )
    {
        if (this.isPresent())
            return toPair(unit);
        else
            return pairFactory().apply(unit, other.getAsDouble());
    }

    public default <X extends Throwable> UDoublePair<U> toPairOrElseThrow( U unit, Supplier<? extends X> exSupplier )
        throws X
    {
        if (this.isPresent())
            return toPair(unit);
        else
            throw exSupplier.get();
    }

    public default boolean contains( U unit, double value )
    {
        return this.isPresent() && this.in(unit) == value;
    }

    public default boolean test( U unit, DoublePredicate predicate )
    {
        Objects.requireNonNull(predicate);
        return this.isPresent() && predicate.test(this.in(unit));
    }

    public default boolean testPair( U unit, Predicate<? super UDoublePair<? extends U>> predicate )
    {
        Objects.requireNonNull(predicate);
        return this.isPresent() && predicate.test(this.toPair(unit));
    }

    public UDV filter( U unit, DoublePredicate predicate );

    public UDV filterPair( U unit, Predicate<? super UDoublePair<? extends U>> predicate );

    public UDV transform( U unit, DoubleUnaryOperator transformer );

    public UDV transformPair( U unit, ToDoubleFunction<? super UDoublePair<? extends U>> transformer );

    public UDV flatTransform( U unit, DoubleFunction<? extends UDV> transformer );

    public UDV flatTransformPair( U unit, Function<? super UDoublePair<? extends U>, ? extends UDV> transformer );

    public UDV combine( UDV other, U unit, DoubleBinaryOperator combiner );

    public UDV combinePair( UDV other,
                            U unit,
                            ToDoubleBiFunction<? super UDoublePair<? extends U>,
                                               ? super UDoublePair<? extends U>> combiner );

    public default <T> Possible<T> map( U unit, DoubleFunction<? extends T> mapper )
    {
        Objects.requireNonNull(mapper);
        if (!this.isPresent())
            return Possible.absent();
        else
            return Possible.ofNullable(mapper.apply(this.in(unit)));
    }

    public default <T> Possible<T> mapPair( U unit, Function<? super UDoublePair<? extends U>, T> mapper )
    {
        Objects.requireNonNull(mapper);
        if (!this.isPresent())
            return Possible.absent();
        else
            return Possible.ofNullable(mapper.apply(this.toPair(unit)));
    }

    public default <T> Possible<T> coMap( UDV other, U unit, DoubleBiFunction<? extends T> coMapper )
    {
        Objects.requireNonNull(coMapper);
        if (this.isPresent() && other.isPresent())
            return Possible.ofNullable(coMapper.apply(this.in(unit), other.in(unit)));
        else
            return Possible.absent();
    }

    public default <T> Possible<T> coMapPair( UDV other,
                                              U unit,
                                              BiFunction<? super UDoublePair<? extends U>,
                                                         ? super UDoublePair<? extends U>,
                                                         ? extends T> coMapper )
    {
        Objects.requireNonNull(coMapper);
        if (this.isPresent() && other.isPresent())
            return Possible.ofNullable(coMapper.apply(this.toPair(unit), other.toPair(unit)));
        else
            return Possible.absent();
    }

    public default <T> Possible<T> flatMap( U unit, DoubleFunction<? extends Possible<T>> mapper )
    {
        Objects.requireNonNull(mapper);
        if (!this.isPresent())
            return Possible.absent();
        else
            return Objects.requireNonNull(mapper.apply(this.in(unit)));
    }

    public default <T> Possible<T> flatMapPair( U unit,
                                                Function<? super UDoublePair<? extends U>,
                                                         ? extends Possible<T>> mapper )
    {
        Objects.requireNonNull(mapper);
        if (!this.isPresent())
            return Possible.absent();
        else
            return Objects.requireNonNull(mapper.apply(this.toPair(unit)));
    }

    public default <T> Possible<T> flatCoMap( UDV other, U unit, DoubleBiFunction<? extends Possible<T>> coMapper )
    {
        Objects.requireNonNull(coMapper);
        if (this.isPresent() && other.isPresent())
            return Objects.requireNonNull(coMapper.apply(this.in(unit), other.in(unit)));
        else
            return Possible.absent();
    }

    public default <T> Possible<T> flatCoMapPair( UDV other,
                                                  U unit,
                                                  BiFunction<? super UDoublePair<? extends U>,
                                                             ? super UDoublePair<? extends U>,
                                                             ? extends Possible<T>> coMapper )
    {
        Objects.requireNonNull(coMapper);
        if (this.isPresent() && other.isPresent())
            return Objects.requireNonNull(coMapper.apply(this.toPair(unit), other.toPair(unit)));
        else
            return Possible.absent();
    }

    public default PossibleDouble asPossible( U unit )
    {
        return this.isPresent() ? PossibleDouble.of(this.in(unit)) : PossibleDouble.absent();
    }

    public default Possible<UDoublePair<U>> asPossiblePair( U unit )
    {
        return this.isPresent() ? Possible.of(this.toPair(unit)) : Possible.absent();
    }

    public default DoubleStream asStream( U unit )
    {
        return this.isPresent() ? DoubleStream.of(this.in(unit)) : DoubleStream.empty();
    }

    public default Stream<UDoublePair<U>> asPairStream( U unit )
    {
        return this.isPresent() ? Stream.of(this.toPair(unit)) : Stream.empty();
    }

    public default Set<UDoublePair<U>> asPairSet( U unit )
    {
        return this.isPresent() ? Collections.singleton(this.toPair(unit)) : Collections.emptySet();
    }

    public default String toString( U unit )
    {
        return toString(unit, UDoublePair::toString, "absent");
    }

    public default String toString( U unit, Function<? super UDoublePair<? extends U>, String> converter )
    {
        return toString(unit, converter, "absent");
    }

    public default String toString( U unit, String absentString )
    {
        return toString(unit, UDoublePair::toString, absentString);
    }

    public default String toString( U unit,
                                    Function<? super UDoublePair<? extends U>, String> converter,
                                    String absentString )
    {
        MoreObjects.requireNonNull(converter, "converter", absentString, "absentString");
        if (this.isPresent())
            return Objects.requireNonNull(converter.apply(this.toPair(unit)));
        else
            return absentString;
    }
}
