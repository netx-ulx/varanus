package net.varanus.util.unitvalue;


import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToDoubleFunction;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.functional.PossibleDouble;
import net.varanus.util.functional.functions.ObjDoubleFunction;
import net.varanus.util.lang.Comparables;
import net.varanus.util.unitvalue.UnitLongValue.ULongPair;


/**
 * @param <U>
 *            The unit type
 * @param <UDV>
 *            The unitvalue type
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public abstract class AbstractUnitDoubleValue<U, UDV extends AbstractUnitDoubleValue<U, UDV>>
    implements UnitDoubleValue<U, UDV>, Comparable<UDV>
{
    private final PossibleDouble possible;

    protected AbstractUnitDoubleValue( double baseValue, boolean isPresent )
    {
        this.possible = isPresent ? PossibleDouble.of(validValue(baseValue)) : PossibleDouble.absent();
    }

    protected abstract double convertTo( U unit, double baseValue );

    protected abstract UDV buildAbsent();

    protected abstract UDV buildPresent( double value, U unit );

    protected abstract UDV buildPresent( double baseValue );

    @SuppressWarnings( "unchecked" )
    protected final UDV castThis()
    {
        return (UDV)this;
    }

    @Override
    public ObjDoubleFunction<U, UDoublePair<U>> pairFactory()
    {
        return SimpleUDoublePair<U>::new;
    }

    @Override
    public final boolean isPresent()
    {
        return possible.isPresent();
    }

    @Override
    public final UDV ifAbsent( Runnable action )
    {
        possible.ifAbsent(action);
        return castThis();
    }

    @Override
    public final UDV ifPresent( Runnable action )
    {
        possible.ifPresent(action);
        return castThis();
    }

    protected final UDV ifPresentBase( DoubleConsumer consumer )
    {
        possible.ifPresent(consumer);
        return castThis();
    }

    @Override
    public final UDV ifPresent( U unit, DoubleConsumer consumer )
    {
        if (this.isPresent())
            consumer.accept(this.in(unit));
        return castThis();
    }

    @Override
    public final UDV ifPresentPair( U unit, Consumer<? super UDoublePair<U>> consumer )
    {
        if (this.isPresent())
            consumer.accept(this.toPair(unit));
        return castThis();
    }

    protected final double toBaseValue() throws NoSuchElementException
    {
        return possible.getAsDouble();
    }

    @Override
    public final double in( U unit ) throws NoSuchElementException
    {
        return convertTo(Objects.requireNonNull(unit), this.toBaseValue());
    }

    protected final boolean containsBase( double value )
    {
        return possible.contains(value);
    }

    protected final UDV filterBase( DoublePredicate predicate )
    {
        Objects.requireNonNull(predicate);
        if (!this.isPresent())
            return castThis();
        else
            return predicate.test(this.toBaseValue()) ? castThis() : buildAbsent();
    }

    @Override
    public final UDV filter( U unit, DoublePredicate predicate )
    {
        Objects.requireNonNull(predicate);
        if (!this.isPresent())
            return castThis();
        else
            return predicate.test(this.in(unit)) ? castThis() : buildAbsent();
    }

    @Override
    public final UDV filterPair( U unit, Predicate<? super UDoublePair<? extends U>> predicate )
    {
        Objects.requireNonNull(predicate);
        if (!this.isPresent())
            return castThis();
        else
            return predicate.test(this.toPair(unit)) ? castThis() : buildAbsent();
    }

    protected final UDV transformBase( DoubleUnaryOperator transformer )
    {
        Objects.requireNonNull(transformer);
        if (!this.isPresent()) {
            return castThis();
        }
        else {
            double value = transformer.applyAsDouble(this.toBaseValue());
            return buildPresent(value);
        }
    }

    @Override
    public final UDV transform( U unit, DoubleUnaryOperator transformer )
    {
        Objects.requireNonNull(transformer);
        if (!this.isPresent()) {
            return castThis();
        }
        else {
            double value = transformer.applyAsDouble(this.in(unit));
            return buildPresent(value, unit);
        }
    }

    @Override
    public final UDV transformPair( U unit, ToDoubleFunction<? super UDoublePair<? extends U>> transformer )
    {
        Objects.requireNonNull(transformer);
        if (!this.isPresent()) {
            return castThis();
        }
        else {
            double value = transformer.applyAsDouble(this.toPair(unit));
            return buildPresent(value, unit);
        }
    }

    protected final UDV flatTransformBase( DoubleFunction<? extends UDV> transformer )
    {
        Objects.requireNonNull(transformer);
        if (!this.isPresent())
            return castThis();
        else
            return transformer.apply(this.toBaseValue());
    }

    @Override
    public final UDV flatTransform( U unit, DoubleFunction<? extends UDV> transformer )
    {
        Objects.requireNonNull(transformer);
        if (!this.isPresent())
            return castThis();
        else
            return transformer.apply(this.in(unit));
    }

    @Override
    public final UDV flatTransformPair( U unit, Function<? super UDoublePair<? extends U>, ? extends UDV> transformer )
    {
        Objects.requireNonNull(transformer);
        if (!this.isPresent())
            return castThis();
        else
            return transformer.apply(this.toPair(unit));
    }

    protected final UDV combineBase( UDV other, DoubleBinaryOperator combiner )
    {
        Objects.requireNonNull(combiner);
        if (this.isPresent() && other.isPresent()) {
            double combined = combiner.applyAsDouble(this.toBaseValue(), other.toBaseValue());
            return buildPresent(combined);
        }
        else {
            return buildAbsent();
        }
    }

    @Override
    public final UDV combine( UDV other, U unit, DoubleBinaryOperator combiner )
    {
        Objects.requireNonNull(combiner);
        if (this.isPresent() && other.isPresent()) {
            double combined = combiner.applyAsDouble(this.in(unit), other.in(unit));
            return buildPresent(combined, unit);
        }
        else {
            return buildAbsent();
        }
    }

    @Override
    public final UDV combinePair( UDV other,
                                  U unit,
                                  ToDoubleBiFunction<? super UDoublePair<? extends U>,
                                                     ? super UDoublePair<? extends U>> combiner )
    {
        Objects.requireNonNull(combiner);
        if (this.isPresent() && other.isPresent()) {
            double combined = combiner.applyAsDouble(this.toPair(unit), other.toPair(unit));
            return buildPresent(combined, unit);
        }
        else {
            return buildAbsent();
        }
    }

    protected final PossibleDouble asPossibleBase()
    {
        return possible;
    }

    @Override
    public final PossibleDouble asPossible( U unit )
    {
        return possible.map(val -> convertTo(unit, val));
    }

    public final UDV plus( UDV other )
    {
        return combineBase(other, Double::sum);
    }

    public final UDV minus( UDV other )
    {
        return combineBase(other, ( thisVal, otherVal ) -> {
            if (thisVal < otherVal)
                throw new ArithmeticException("negative difference");
            else
                return thisVal - otherVal;
        });
    }

    public final UDV posDiff( UDV other )
    {
        return combineBase(other, ( a, b ) -> a > b ? (a - b) : 0);
    }

    public final UDV times( double multiplier )
    {
        if (multiplier < 0)
            throw new ArithmeticException("negative multiplier");
        else
            return transformBase(val -> val * multiplier);
    }

    public final UDV times( UDV other )
    {
        return combineBase(other, ( a, b ) -> a * b);
    }

    public final UDV divideBy( double divisor )
    {
        if (divisor < 0)
            throw new ArithmeticException("negative divisor");
        else
            return transformBase(val -> val / divisor);
    }

    public final UDV divideBy( UDV other )
    {
        return combineBase(other, ( a, b ) -> a / b);
    }

    public UDV min( UDV other )
    {
        return Comparables.min(castThis(), other);
    }

    public UDV minPresent( UDV other )
    {
        if (this.isPresent() && other.isPresent())
            return Comparables.min(castThis(), other);
        else
            return buildAbsent();
    }

    public UDV max( UDV other )
    {
        return Comparables.max(castThis(), other);
    }

    public UDV maxPresent( UDV other )
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
        return (other instanceof AbstractUnitDoubleValue<?, ?>)
               && this.equals((AbstractUnitDoubleValue<?, ?>)other);
    }

    protected final boolean equals( AbstractUnitDoubleValue<?, ?> other )
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
    public final int compareTo( UDV other )
    {
        if (this.isPresent() && other.isPresent())
            return Double.compare(this.toBaseValue(), other.toBaseValue());
        else if (this.isPresent())
            return 1; // other is absent
        else if (other.isPresent())
            return -1; // this is absent
        else
            return 0; // both are absent
    }

    private static double validValue( double val )
    {
        if (val < -0D) {
            throw new IllegalArgumentException("value must not be negative");
        }
        // else if (Double.isInfinite(val)) {
        // throw new IllegalArgumentException("value must not be an infinity");
        // }
        else if (Double.isNaN(val)) {
            throw new IllegalArgumentException("value must not be \"Not-a-Number\" (NaN)");
        }
        else {
            return val;
        }
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    public static class SimpleUDoublePair<U> implements UDoublePair<U>
    {
        private final U      unit;
        private final double value;

        public SimpleUDoublePair( U unit, double value )
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
        public double value()
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
