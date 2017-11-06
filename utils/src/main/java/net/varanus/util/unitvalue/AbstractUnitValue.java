package net.varanus.util.unitvalue;


import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import javax.annotation.Nullable;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.functional.Possible;


/**
 * @param <U>
 *            The unit type
 * @param <V>
 *            The value type
 * @param <UV>
 *            The unitvalue type
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public abstract class AbstractUnitValue<U, V, UV extends AbstractUnitValue<U, V, UV>> implements UnitValue<U, V, UV>
{
    private final Possible<V> possible;

    protected AbstractUnitValue( V baseValue, boolean isPresent )
    {
        this.possible = isPresent ? Possible.of(baseValue) : Possible.absent();
    }

    protected abstract V convertTo( U unit, V baseValue );

    protected abstract UV buildAbsent();

    protected abstract UV buildPresent( V value, U unit );

    protected abstract UV buildPresent( V baseValue );

    @SuppressWarnings( "unchecked" )
    protected final UV castThis()
    {
        return (UV)this;
    }

    @Override
    public BiFunction<U, V, UVPair<U, V>> pairFactory()
    {
        return SimpleUVPair<U, V>::new;
    }

    @Override
    public final boolean isPresent()
    {
        return possible.isPresent();
    }

    @Override
    public final UV ifAbsent( Runnable action )
    {
        possible.ifAbsent(action);
        return castThis();
    }

    @Override
    public final UV ifPresent( Runnable action )
    {
        possible.ifPresent(action);
        return castThis();
    }

    protected final UV ifPresentBase( Consumer<? super V> consumer )
    {
        possible.ifPresent(consumer);
        return castThis();
    }

    @Override
    public final UV ifPresent( U unit, Consumer<? super V> consumer )
    {
        if (this.isPresent())
            consumer.accept(this.in(unit));
        return castThis();
    }

    @Override
    public final UV ifPresentPair( U unit, Consumer<? super UVPair<U, V>> consumer )
    {
        if (this.isPresent())
            consumer.accept(this.toPair(unit));
        return castThis();
    }

    protected final V toBaseValue() throws NoSuchElementException
    {
        return possible.get();
    }

    @Override
    public final V in( U unit ) throws NoSuchElementException
    {
        return convertTo(Objects.requireNonNull(unit), this.toBaseValue());
    }

    protected final boolean containsBase( @Nullable Object value )
    {
        return possible.contains(value);
    }

    protected final UV filterBase( Predicate<? super V> predicate )
    {
        Objects.requireNonNull(predicate);
        if (!this.isPresent())
            return castThis();
        else
            return predicate.test(this.toBaseValue()) ? castThis() : buildAbsent();
    }

    @Override
    public final UV filter( U unit, Predicate<? super V> predicate )
    {
        Objects.requireNonNull(predicate);
        if (!this.isPresent())
            return castThis();
        else
            return predicate.test(this.in(unit)) ? castThis() : buildAbsent();
    }

    @Override
    public final UV filterPair( U unit, Predicate<? super UVPair<? extends U, ? extends V>> predicate )
    {
        Objects.requireNonNull(predicate);
        if (!this.isPresent())
            return castThis();
        else
            return predicate.test(this.toPair(unit)) ? castThis() : buildAbsent();
    }

    protected final UV transformBase( Function<? super V, ? extends V> transformer )
    {
        Objects.requireNonNull(transformer);
        if (!this.isPresent()) {
            return castThis();
        }
        else {
            V value = transformer.apply(this.toBaseValue());
            return (value == null) ? buildAbsent() : buildPresent(value);
        }
    }

    @Override
    public final UV transform( U unit, Function<? super V, ? extends V> transformer )
    {
        Objects.requireNonNull(transformer);
        if (!this.isPresent()) {
            return castThis();
        }
        else {
            V value = transformer.apply(this.in(unit));
            return (value == null) ? buildAbsent() : buildPresent(value, unit);
        }
    }

    @Override
    public final UV transformPair( U unit, Function<? super UVPair<? extends U, ? extends V>, ? extends V> transformer )
    {
        Objects.requireNonNull(transformer);
        if (!this.isPresent()) {
            return castThis();
        }
        else {
            V value = transformer.apply(this.toPair(unit));
            return (value == null) ? buildAbsent() : buildPresent(value, unit);
        }
    }

    protected final UV flatTransformBase( Function<? super V, ? extends UV> transformer )
    {
        Objects.requireNonNull(transformer);
        if (!this.isPresent())
            return castThis();
        else
            return transformer.apply(this.toBaseValue());
    }

    @Override
    public final UV flatTransform( U unit, Function<? super V, ? extends UV> transformer )
    {
        Objects.requireNonNull(transformer);
        if (!this.isPresent())
            return castThis();
        else
            return transformer.apply(this.in(unit));
    }

    @Override
    public final UV flatTransformPair( U unit,
                                       Function<? super UVPair<? extends U, ? extends V>, ? extends UV> transformer )
    {
        Objects.requireNonNull(transformer);
        if (!this.isPresent())
            return castThis();
        else
            return transformer.apply(this.toPair(unit));
    }

    protected final UV combineBase( UV other, BiFunction<? super V, ? super V, ? extends V> combiner )
    {
        Objects.requireNonNull(combiner);
        if (this.isPresent() && other.isPresent()) {
            V combined = combiner.apply(this.toBaseValue(), other.toBaseValue());
            return (combined == null) ? buildAbsent() : buildPresent(combined);
        }
        else {
            return buildAbsent();
        }
    }

    @Override
    public final UV combine( UV other, U unit, BiFunction<? super V, ? super V, ? extends V> combiner )
    {
        Objects.requireNonNull(combiner);
        if (this.isPresent() && other.isPresent()) {
            V combined = combiner.apply(this.in(unit), other.in(unit));
            return (combined == null) ? buildAbsent() : buildPresent(combined, unit);
        }
        else {
            return buildAbsent();
        }
    }

    @Override
    public final UV combinePair( UV other,
                                 U unit,
                                 BiFunction<? super UVPair<? extends U, ? extends V>,
                                            ? super UVPair<? extends U, ? extends V>,
                                            ? extends V> combiner )
    {
        Objects.requireNonNull(combiner);
        if (this.isPresent() && other.isPresent()) {
            V combined = combiner.apply(this.toPair(unit), other.toPair(unit));
            return (combined == null) ? buildAbsent() : buildPresent(combined, unit);
        }
        else {
            return buildAbsent();
        }
    }

    protected final Possible<V> asPossibleBase()
    {
        return possible;
    }

    @Override
    public final Possible<V> asPossible( U unit )
    {
        return possible.map(val -> convertTo(unit, val));
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public boolean equals( Object other )
    {
        return (other instanceof AbstractUnitValue<?, ?, ?>)
               && this.equals((AbstractUnitValue<?, ?, ?>)other);
    }

    private boolean equals( AbstractUnitValue<?, ?, ?> other )
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

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    protected static class SimpleUVPair<U, V> implements UVPair<U, V>
    {
        private final U unit;
        private final V value;

        protected SimpleUVPair( U unit, V value )
        {
            this.unit = Objects.requireNonNull(unit);
            this.value = Objects.requireNonNull(value);
        }

        @Override
        public U unit()
        {
            return unit;
        }

        @Override
        public V value()
        {
            return value;
        }

        @Override
        public boolean equals( Object other )
        {
            return (other instanceof UVPair<?, ?>)
                   && this.equals((UVPair<?, ?>)other);
        }

        private boolean equals( UVPair<?, ?> other )
        {
            return (other != null)
                   && Objects.equals(this.unit(), other.unit())
                   && Objects.equals(this.value(), other.value());
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
