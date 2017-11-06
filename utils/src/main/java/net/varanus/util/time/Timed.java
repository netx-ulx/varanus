package net.varanus.util.time;


import java.time.Instant;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.lang.MoreObjects;


/**
 * A value with an associated timestamp.
 * 
 * @param <T>
 *            The type of the value
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public interface Timed<T>
{
    public T value();

    public Instant timestamp();

    public default Timed<T> withTime( Instant timestamp )
    {
        return Timed.of(value(), Objects.requireNonNull(timestamp));
    }

    public default <U> Timed<U> toSameTime( U value )
    {
        return Timed.of(Objects.requireNonNull(value), timestamp());
    }

    public default Timed<T> transformSameTime( UnaryOperator<T> transformer )
    {
        return Timed.of(transformer.apply(value()), timestamp());
    }

    public default <U> Timed<U> mapSameTime( Function<? super T, ? extends U> mapper )
    {
        return Timed.of(mapper.apply(value()), timestamp());
    }

    public static <T> Timed<T> of( final T value, final Instant timestamp )
    {
        MoreObjects.requireNonNull(value, "value", timestamp, "timestamp");
        return new Timed<T>() {

            @Override
            public T value()
            {
                return value;
            }

            @Override
            public Instant timestamp()
            {
                return timestamp;
            }

            @Override
            public boolean equals( Object other )
            {
                return (other instanceof Timed<?>) && this.equals((Timed<?>)other);
            }

            public boolean equals( Timed<?> other )
            {
                return (other != null)
                       && this.value().equals(other.value())
                       && this.timestamp().equals(other.timestamp());
            }

            @Override
            public int hashCode()
            {
                return Objects.hash(value(), timestamp());
            }

            @Override
            public String toString()
            {
                return String.format("%s [%s]", value(), timestamp());
            }
        };
    }

    public static <T> Timed<T> now( T value )
    {
        return of(value, Instant.now());
    }
}
