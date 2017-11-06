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
import net.varanus.util.time.TimeDouble;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public class TimeSummary extends PossibleStatSummary<TimeDouble, TimeSummary>
{
    private static final TimeSummary ABSENT = new TimeSummary();

    public static TimeSummary of( TimeDouble latest, TimeDouble mean, TimeDouble stdDev )
    {
        return new TimeSummary(
            Objects.requireNonNull(latest),
            Objects.requireNonNull(mean),
            Objects.requireNonNull(stdDev));
    }

    public static TimeSummary of( TimeDouble value )
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

    public static TimeSummary absent()
    {
        return ABSENT;
    }

    private TimeSummary()
    {
        super(); // absent stats
    }

    private TimeSummary( TimeDouble latest, TimeDouble mean, TimeDouble stdDev )
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
        return (other instanceof TimeSummary)
               && super.equals(other);
    }

    public boolean equals( TimeSummary other )
    {
        return super.equals(other);
    }

    @Override
    public int hashCode()
    {
        return super.hashCode();
    }

    public String latestToString()
    {
        return latestToString(TimeDouble::toString);
    }

    public String latestToString( Function<? super TimeDouble, String> converter )
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
    public static final class Builder extends AbstractBuilder<TimeDouble, TimeSummary>
    {
        public static final int DEFAULT_WINDOW_SIZE = 3;

        private static final ToDoubleFunction<TimeDouble> TO_DOUBLE   = TimeDouble::inNanos;
        private static final DoubleFunction<TimeDouble>   FROM_DOUBLE = TimeDouble::ofNanos;

        private Builder()
        {
            super(DEFAULT_WINDOW_SIZE, TO_DOUBLE, FROM_DOUBLE);
        }

        private Builder( int windowSize )
        {
            super(windowSize, TO_DOUBLE, FROM_DOUBLE);
        }

        public Builder addValue( TimeDouble value ) throws IllegalArgumentException
        {
            Preconditions.checkArgument(value.isPresent(), "expected present time value");
            _addValue(value);
            return this;
        }

        public Builder addValues( Iterable<TimeDouble> values ) throws IllegalArgumentException
        {
            ImmutableList<TimeDouble> validValues = ImmutableListBuilder.<TimeDouble>create()
                .addEach(StreamUtils.toSequentialStream(values)
                    .peek(v -> Preconditions.checkArgument(v.isPresent(), "expected present time value")))
                .build();

            for (TimeDouble v : validValues) {
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
        public TimeSummary build()
        {
            if (_hasValues())
                return new TimeSummary(_getLatest(), _getMean(), _getStdDev());
            else
                return TimeSummary.absent();
        }
    }
}
