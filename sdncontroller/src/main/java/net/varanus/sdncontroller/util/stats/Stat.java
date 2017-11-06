package net.varanus.sdncontroller.util.stats;


import static net.varanus.sdncontroller.util.stats.StatType.SAFE;
import static net.varanus.sdncontroller.util.stats.StatType.UNSAFE;

import java.time.Instant;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.collect.builder.BaseBuilder;
import net.varanus.util.lang.Comparables;
import net.varanus.util.time.Timed;


/**
 * @param <V>
 *            The class type of stat value
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class Stat<V> implements Timed<V>
{
    public static enum StringFormat
    {
        VALUE,
        VALUE_TYPE,
        VALUE_TIME,
        VALUE_TYPE_TIME
    }

    public static <V> Builder<V> newBuilder( Stat<V> stat )
    {
        return newBuilder(stat.value, stat.timestamp, stat.type, stat.valuePrinter);
    }

    public static <V> Builder<V> newBuilder( V value, Instant timestamp, StatType type )
    {
        return newBuilder(value, timestamp, type, String::valueOf);
    }

    public static <V> Builder<V> newBuilder( V value,
                                             Instant timestamp,
                                             StatType type,
                                             Function<? super V, String> valuePrinter )
    {
        return new Builder<>(
            Objects.requireNonNull(value),
            Objects.requireNonNull(timestamp),
            Objects.requireNonNull(type),
            Objects.requireNonNull(valuePrinter));
    }

    public static <V> Stat<V> of( V value, Instant timestamp, StatType type )
    {
        return of(value, timestamp, type, String::valueOf);
    }

    public static <V> Stat<V> of( V value, Instant timestamp, StatType type, Function<? super V, String> valuePrinter )
    {
        return new Stat<>(
            Objects.requireNonNull(value),
            Objects.requireNonNull(timestamp),
            Objects.requireNonNull(type),
            Objects.requireNonNull(valuePrinter));
    }

    private final V                           value;
    private final StatType                    type;
    private final Instant                     timestamp;
    private final Function<? super V, String> valuePrinter;

    private Stat( V value, Instant timestamp, StatType type, Function<? super V, String> valuePrinter )
    {
        this.value = value;
        this.type = type;
        this.timestamp = timestamp;
        this.valuePrinter = valuePrinter;
    }

    @Override
    public V value()
    {
        return value;
    }

    @Override
    public Instant timestamp()
    {
        return timestamp;
    }

    public StatType type()
    {
        return type;
    }

    public boolean isSafe()
    {
        return type.equals(StatType.SAFE);
    }

    @Override
    public Stat<V> withTime( Instant timestamp )
    {
        Objects.requireNonNull(timestamp);
        return new Stat<>(this.value(), timestamp, this.type(), this.valuePrinter);
    }

    @Override
    public <U> Stat<U> toSameTime( U value )
    {
        return of(value, this.timestamp, this.type);
    }

    public <U> Stat<U> toSameTime( U value, Function<? super U, String> valuePrinter )
    {
        return of(value, this.timestamp, this.type, valuePrinter);
    }

    @Override
    public Stat<V> transformSameTime( UnaryOperator<V> transformer )
    {
        return of(transformer.apply(this.value), this.timestamp, this.type, this.valuePrinter);
    }

    @Override
    public <U> Stat<U> mapSameTime( Function<? super V, ? extends U> mapper )
    {
        return of(mapper.apply(this.value), this.timestamp, this.type);
    }

    public <U> Stat<U> mapSameTime( Function<? super V, ? extends U> mapper, Function<? super U, String> valuePrinter )
    {
        return of(mapper.apply(this.value), this.timestamp, this.type, valuePrinter);
    }

    public Stat<V> with( StatType type )
    {
        return of(this.value, this.timestamp, type, this.valuePrinter);
    }

    public Stat<V> with( Function<? super V, String> valuePrinter )
    {
        return of(this.value, this.timestamp, this.type, valuePrinter);
    }

    public Stat<V> with( StatType type, Function<? super V, String> valuePrinter )
    {
        return of(this.value, this.timestamp, type, valuePrinter);
    }

    @Override
    public boolean equals( Object other )
    {
        return (other instanceof Stat<?>)
               && this.equals((Stat<?>)other);
    }

    public boolean equals( Stat<?> other )
    {
        // do not include the timestamp in the equality

        return (other != null)
               && this.value.equals(other.value)
               && this.type.equals(other.type);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(value, type);
    }

    @Override
    public String toString()
    {
        return toString(StringFormat.VALUE_TYPE_TIME);
    }

    public String toString( StringFormat format )
    {
        switch (format) {
            case VALUE:
                return printValue();

            case VALUE_TYPE:
                return String.format("%s (%s)", printValue(), type);

            case VALUE_TIME:
                return String.format("%s [%s]", printValue(), timestamp);

            case VALUE_TYPE_TIME:
                return String.format("%s (%s) [%s]", printValue(), type, timestamp);

            default:
                throw new AssertionError("unexpected enum value");
        }
    }

    private String printValue()
    {
        return valuePrinter.apply(value);
    }

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class Builder<V> implements BaseBuilder<Stat<V>>
    {
        private final V                           value;
        private final Instant                     timestamp;
        private final StatType                    type;
        private final Function<? super V, String> valuePrinter;

        private Builder( V value, Instant timestamp, StatType type, Function<? super V, String> valuePrinter )
        {
            this.value = value;
            this.timestamp = timestamp;
            this.type = type;
            this.valuePrinter = valuePrinter;
        }

        public Builder<V> apply( UnaryOperator<V> function )
        {
            V value = Objects.requireNonNull(function.apply(this.value));
            return new Builder<>(value, this.timestamp, this.type, this.valuePrinter);
        }

        public Builder<V> combineWith( Stat<V> stat, BinaryOperator<V> combiner )
        {
            return mixWith(stat, combiner, this.valuePrinter);
        }

        public <U, R> Builder<R> mixWith( Stat<U> stat, BiFunction<? super V, ? super U, R> mixer )
        {
            return mixWith(stat, mixer, String::valueOf);
        }

        public <U, R> Builder<R> mixWith( Stat<U> stat,
                                          BiFunction<? super V, ? super U, R> mixer,
                                          Function<? super R, String> valuePrinter )
        {
            R value = Objects.requireNonNull(mixer.apply(this.value, stat.value));
            Instant timestamp = Comparables.max(this.timestamp, stat.timestamp);
            StatType type = (this.type.equals(SAFE) && stat.isSafe()) ? SAFE : UNSAFE;
            return new Builder<>(value, timestamp, type, Objects.requireNonNull(valuePrinter));
        }

        @Override
        public Stat<V> build()
        {
            return Stat.of(value, timestamp, type, valuePrinter);
        }
    }
}
