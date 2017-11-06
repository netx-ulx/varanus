package net.varanus.util.unitvalue;


import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.function.LongUnaryOperator;
import java.util.function.Predicate;
import java.util.function.ToLongBiFunction;
import java.util.function.ToLongFunction;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.functional.PossibleLong;
import net.varanus.util.functional.functions.ObjLongFunction;
import net.varanus.util.lang.Comparables;


/**
 * @param <U>
 *            The unit type
 * @param <ULV>
 *            The unitvalue type
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public abstract class AbstractUnitLongValue<U, ULV extends AbstractUnitLongValue<U, ULV>>
    implements UnitLongValue<U, ULV>, Comparable<ULV>
{
    private final PossibleLong possible;

    protected AbstractUnitLongValue( long baseValue, boolean isPresent )
    {
        this.possible = isPresent ? PossibleLong.of(validValue(baseValue)) : PossibleLong.absent();
    }

    protected abstract long convertTo( U unit, long baseValue );

    protected abstract ULV buildAbsent();

    protected abstract ULV buildPresent( long value, U unit );

    protected abstract ULV buildPresent( long baseValue );

    @SuppressWarnings( "unchecked" )
    protected final ULV castThis()
    {
        return (ULV)this;
    }

    @Override
    public ObjLongFunction<U, ULongPair<U>> pairFactory()
    {
        return SimpleULongPair<U>::new;
    }

    @Override
    public final boolean isPresent()
    {
        return possible.isPresent();
    }

    @Override
    public final ULV ifAbsent( Runnable action )
    {
        possible.ifAbsent(action);
        return castThis();
    }

    @Override
    public final ULV ifPresent( Runnable action )
    {
        possible.ifPresent(action);
        return castThis();
    }

    protected final ULV ifPresentBase( LongConsumer consumer )
    {
        possible.ifPresent(consumer);
        return castThis();
    }

    @Override
    public final ULV ifPresent( U unit, LongConsumer consumer )
    {
        if (this.isPresent())
            consumer.accept(this.in(unit));
        return castThis();
    }

    @Override
    public final ULV ifPresentPair( U unit, Consumer<? super ULongPair<U>> consumer )
    {
        if (this.isPresent())
            consumer.accept(this.toPair(unit));
        return castThis();
    }

    protected final long toBaseValue() throws NoSuchElementException
    {
        return possible.getAsLong();
    }

    @Override
    public final long in( U unit ) throws NoSuchElementException
    {
        return convertTo(Objects.requireNonNull(unit), this.toBaseValue());
    }

    protected final boolean containsBase( long value )
    {
        return possible.contains(value);
    }

    protected final ULV filterBase( LongPredicate predicate )
    {
        Objects.requireNonNull(predicate);
        if (!this.isPresent())
            return castThis();
        else
            return predicate.test(this.toBaseValue()) ? castThis() : buildAbsent();
    }

    @Override
    public final ULV filter( U unit, LongPredicate predicate )
    {
        Objects.requireNonNull(predicate);
        if (!this.isPresent())
            return castThis();
        else
            return predicate.test(this.in(unit)) ? castThis() : buildAbsent();
    }

    @Override
    public final ULV filterPair( U unit, Predicate<? super ULongPair<? extends U>> predicate )
    {
        Objects.requireNonNull(predicate);
        if (!this.isPresent())
            return castThis();
        else
            return predicate.test(this.toPair(unit)) ? castThis() : buildAbsent();
    }

    protected final ULV transformBase( LongUnaryOperator transformer )
    {
        Objects.requireNonNull(transformer);
        if (!this.isPresent()) {
            return castThis();
        }
        else {
            long value = transformer.applyAsLong(this.toBaseValue());
            return buildPresent(value);
        }
    }

    @Override
    public final ULV transform( U unit, LongUnaryOperator transformer )
    {
        Objects.requireNonNull(transformer);
        if (!this.isPresent()) {
            return castThis();
        }
        else {
            long value = transformer.applyAsLong(this.in(unit));
            return buildPresent(value, unit);
        }
    }

    @Override
    public final ULV transformPair( U unit, ToLongFunction<? super ULongPair<? extends U>> transformer )
    {
        Objects.requireNonNull(transformer);
        if (!this.isPresent()) {
            return castThis();
        }
        else {
            long value = transformer.applyAsLong(this.toPair(unit));
            return buildPresent(value, unit);
        }
    }

    protected final ULV flatTransformBase( LongFunction<? extends ULV> transformer )
    {
        Objects.requireNonNull(transformer);
        if (!this.isPresent())
            return castThis();
        else
            return transformer.apply(this.toBaseValue());
    }

    @Override
    public final ULV flatTransform( U unit, LongFunction<? extends ULV> transformer )
    {
        Objects.requireNonNull(transformer);
        if (!this.isPresent())
            return castThis();
        else
            return transformer.apply(this.in(unit));
    }

    @Override
    public final ULV flatTransformPair( U unit, Function<? super ULongPair<? extends U>, ? extends ULV> transformer )
    {
        Objects.requireNonNull(transformer);
        if (!this.isPresent())
            return castThis();
        else
            return transformer.apply(this.toPair(unit));
    }

    protected final ULV combineBase( ULV other, LongBinaryOperator combiner )
    {
        Objects.requireNonNull(combiner);
        if (this.isPresent() && other.isPresent()) {
            long combined = combiner.applyAsLong(this.toBaseValue(), other.toBaseValue());
            return buildPresent(combined);
        }
        else {
            return buildAbsent();
        }
    }

    @Override
    public final ULV combine( ULV other, U unit, LongBinaryOperator combiner )
    {
        Objects.requireNonNull(combiner);
        if (this.isPresent() && other.isPresent()) {
            long combined = combiner.applyAsLong(this.in(unit), other.in(unit));
            return buildPresent(combined, unit);
        }
        else {
            return buildAbsent();
        }
    }

    @Override
    public final ULV combinePair( ULV other,
                                  U unit,
                                  ToLongBiFunction<? super ULongPair<? extends U>,
                                                   ? super ULongPair<? extends U>> combiner )
    {
        Objects.requireNonNull(combiner);
        if (this.isPresent() && other.isPresent()) {
            long combined = combiner.applyAsLong(this.toPair(unit), other.toPair(unit));
            return buildPresent(combined, unit);
        }
        else {
            return buildAbsent();
        }
    }

    protected final PossibleLong asPossibleBase()
    {
        return possible;
    }

    @Override
    public final PossibleLong asPossible( U unit )
    {
        return possible.map(val -> convertTo(unit, val));
    }

    public final ULV plus( ULV other )
    {
        return combineBase(other, Math::addExact);
    }

    public final ULV minus( ULV other )
    {
        return combineBase(other, ( thisVal, otherVal ) -> {
            if (thisVal < otherVal)
                throw new ArithmeticException("negative difference");
            else
                return thisVal - otherVal;
        });
    }

    public final ULV posDiff( ULV other )
    {
        return combineBase(other, ( a, b ) -> a > b ? (a - b) : 0);
    }

    public final ULV times( long multiplier )
    {
        if (multiplier < 0)
            throw new ArithmeticException("negative multiplier");
        else
            return transformBase(val -> Math.multiplyExact(val, multiplier));
    }

    public final ULV times( ULV other )
    {
        return combineBase(other, Math::multiplyExact);
    }

    public final ULV divideBy( long divisor )
    {
        if (divisor < 0)
            throw new ArithmeticException("negative divisor");
        else
            return transformBase(val -> val / divisor);
    }

    public final ULV divideBy( ULV other )
    {
        return combineBase(other, ( a, b ) -> a / b);
    }

    public ULV min( ULV other )
    {
        return Comparables.min(castThis(), other);
    }

    public ULV minPresent( ULV other )
    {
        if (this.isPresent() && other.isPresent())
            return Comparables.min(castThis(), other);
        else
            return buildAbsent();
    }

    public ULV max( ULV other )
    {
        return Comparables.max(castThis(), other);
    }

    public ULV maxPresent( ULV other )
    {
        if (this.isPresent() && other.isPresent())
            return Comparables.max(castThis(), other);
        else
            return buildAbsent();
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public boolean equals( Object other )
    {
        return (other instanceof AbstractUnitLongValue<?, ?>)
               && this.equals((AbstractUnitLongValue<?, ?>)other);
    }

    protected final boolean equals( AbstractUnitLongValue<?, ?> other )
    {
        return (other != null)
               && this.possible.equals(other.possible);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public int hashCode()
    {
        return possible.hashCode();
    }

    @Override
    public final int compareTo( ULV other )
    {
        if (this.isPresent() && other.isPresent())
            return Long.compare(this.toBaseValue(), other.toBaseValue());
        else if (this.isPresent())
            return 1; // other is absent
        else if (other.isPresent())
            return -1; // this is absent
        else
            return 0; // both are absent
    }

    private static long validValue( long val )
    {
        if (val < 0)
            throw new IllegalArgumentException("value must not be negative");
        else
            return val;
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    public static class SimpleULongPair<U> implements ULongPair<U>
    {
        private final U    unit;
        private final long value;

        public SimpleULongPair( U unit, long value )
        {
            this.unit = Objects.requireNonNull(unit);
            this.value = value;
        }

        @Override
        public U unit()
        {
            return unit;
        }

        @Override
        public long value()
        {
            return value;
        }

        @Override
        public boolean equals( Object other )
        {
            return (other instanceof ULongPair<?>)
                   && this.equals((ULongPair<?>)other);
        }

        private boolean equals( ULongPair<?> other )
        {
            return (other != null)
                   && Objects.equals(this.unit(), other.unit())
                   && this.value() == other.value();
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(unit(), value());
        }

        @Override
        public String toString()
        {
            return String.format("%s in %s", value(), unit());
        }
    }
}
