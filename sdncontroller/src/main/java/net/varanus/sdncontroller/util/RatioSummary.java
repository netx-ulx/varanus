package net.varanus.sdncontroller.util;


import java.util.Objects;
import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.collect.builder.ImmutableListBuilder;
import net.varanus.util.functional.StreamUtils;
import net.varanus.util.math.PossibleStatSummary;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class RatioSummary extends PossibleStatSummary<Ratio, RatioSummary>
{
    private static final RatioSummary ABSENT = new RatioSummary();

    public static RatioSummary of( Ratio latest, Ratio mean, Ratio stdDev )
    {
        return new RatioSummary(
            Objects.requireNonNull(latest),
            Objects.requireNonNull(mean),
            Objects.requireNonNull(stdDev));
    }

    public static RatioSummary of( Ratio value )
    {
        return new Builder().addValue(value).build();
    }

    public static Builder newBuilder()
    {
        return new Builder();
    }

    public static Builder newBuilder( int windowSize )
    {
        return new Builder(windowSize);
    }

    public static RatioSummary absent()
    {
        return ABSENT;
    }

    private RatioSummary()
    {
        super(); // absent stats
    }

    private RatioSummary( Ratio latest, Ratio mean, Ratio stdDev )
    {
        super(latest, mean, stdDev);
    }

    public Builder createBuilder()
    {
        Builder builder = new Builder();
        if (this.isPresent())
            builder.addValue(getLatest());
        return builder;
    }

    public Builder createBuilder( int windowSize )
    {
        Builder builder = new Builder(windowSize);
        if (this.isPresent())
            builder.addValue(getLatest());
        return builder;
    }

    @Override
    public boolean equals( Object other )
    {
        return (other instanceof RatioSummary)
               && super.equals(other);
    }

    public boolean equals( RatioSummary other )
    {
        return super.equals(other);
    }

    @Override
    public int hashCode()
    {
        return super.hashCode();
    }

    @Override
    public String toString()
    {
        return toString(Ratio::toPercentageString);
    }

    public String latestToString()
    {
        return latestToString(lat -> String.format("%s (%s)", lat.toPercentageString(), lat));
    }

    public String latestToString( Function<? super Ratio, String> converter )
    {
        Objects.requireNonNull(converter);
        if (this.isPresent())
            return converter.apply(getLatest());
        else
            return super.toString();
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class Builder extends AbstractBuilder<Ratio, RatioSummary>
    {
        public static final int DEFAULT_WINDOW_SIZE = 3;

        private static final ToDoubleFunction<Ratio> TO_DOUBLE   = Ratio::doubleValue;
        private static final DoubleFunction<Ratio>   FROM_DOUBLE = Ratio::of;

        private Builder()
        {
            super(DEFAULT_WINDOW_SIZE, TO_DOUBLE, FROM_DOUBLE);
        }

        private Builder( int windowSize )
        {
            super(windowSize, TO_DOUBLE, FROM_DOUBLE);
        }

        public Builder addValue( Ratio value ) throws IllegalArgumentException
        {
            Preconditions.checkArgument(!value.isNaN(), "expected not-NaN ratio value");
            _addValue(value);
            return this;
        }

        public Builder addValues( Iterable<Ratio> values ) throws IllegalArgumentException
        {
            ImmutableList<Ratio> validValues = ImmutableListBuilder.<Ratio>create()
                .addEach(StreamUtils.toSequentialStream(values)
                    .peek(v -> Preconditions.checkArgument(!v.isNaN(), "expected not-NaN ratio value")))
                .build();

            for (Ratio v : validValues) {
                _addValue(v);
            }
            return this;
        }

        public Builder reset()
        {
            _reset();
            return this;
        }

        @Override
        public RatioSummary build()
        {
            if (_hasValues())
                return new RatioSummary(_getLatest(), _getMean(), _getStdDev());
            else
                return RatioSummary.absent();
        }
    }
}
