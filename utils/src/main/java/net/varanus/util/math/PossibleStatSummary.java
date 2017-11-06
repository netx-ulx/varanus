package net.varanus.util.math;


import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.functional.BasePossible;
import net.varanus.util.functional.Possible;


/**
 * @param <T>
 *            The type of summarized statistic
 * @param <P>
 *            The type of the possible statistic summary
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public abstract class PossibleStatSummary<T, P extends PossibleStatSummary<T, P>>
    implements StatSummary<T>, BasePossible<P>
{
    private final Possible<Values<T>> values;

    protected PossibleStatSummary()
    {
        this.values = Possible.absent();
    }

    protected PossibleStatSummary( T latest, T mean, T stdDev )
    {
        this.values = Possible.of(new Values<>(
            Objects.requireNonNull(latest),
            Objects.requireNonNull(mean),
            Objects.requireNonNull(stdDev)));
    }

    @Override
    public final T getLatest() throws NoSuchElementException
    {
        return values.get().latest;
    }

    @Override
    public final T getMean() throws NoSuchElementException
    {
        return values.get().mean;
    }

    @Override
    public final T getStdDev() throws NoSuchElementException
    {
        return values.get().stdDev;
    }

    @Override
    public final boolean isPresent()
    {
        return values.isPresent();
    }

    @Override
    public final P ifAbsent( Runnable action )
    {
        values.ifAbsent(action);
        return castThis();
    }

    @Override
    public final P ifPresent( Runnable action )
    {
        values.ifPresent(action);
        return castThis();
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public boolean equals( Object other )
    {
        return (other instanceof PossibleStatSummary<?, ?>)
               && this.equals((PossibleStatSummary<?, ?>)other);
    }

    protected final boolean equals( PossibleStatSummary<?, ?> other )
    {
        return (other != null)
               && this.values.equals(other.values);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public int hashCode()
    {
        return values.hashCode();
    }

    @Override
    public String toString()
    {
        return toString(T::toString);
    }

    public String toString( Function<? super T, String> converter )
    {
        Objects.requireNonNull(converter);
        return values.toString(v -> String.format("%s (+/- %s)",
            converter.apply(v.mean), converter.apply(v.stdDev)));
    }

    @SuppressWarnings( "unchecked" )
    protected final P castThis()
    {
        return (P)this;
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    private static final class Values<T>
    {
        final T latest;
        final T mean;
        final T stdDev;

        Values( T latest, T mean, T stdDev )
        {
            this.latest = latest;
            this.mean = mean;
            this.stdDev = stdDev;
        }

        @Override
        public boolean equals( Object other )
        {
            return (other instanceof Values<?>)
                   && this.equals((Values<?>)other);
        }

        private boolean equals( Values<?> other )
        {
            return (other != null)
                   && this.latest.equals(other.latest)
                   && this.mean.equals(other.mean)
                   && this.stdDev.equals(other.stdDev);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(latest, mean, stdDev);
        }
    }
}
