package net.varanus.sdncontroller.util;


import java.util.Objects;
import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.base.Preconditions;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.math.PossibleStatSummary;
import net.varanus.util.unitvalue.si.MetricDouble;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public class MetricSummary extends PossibleStatSummary<MetricDouble, MetricSummary>
{
    private static final MetricSummary ABSENT = new MetricSummary();

    public static MetricSummary of( MetricDouble latest, MetricDouble mean, MetricDouble stdDev )
    {
        return new MetricSummary(
            Objects.requireNonNull(latest),
            Objects.requireNonNull(mean),
            Objects.requireNonNull(stdDev));
    }

    public static MetricSummary of( MetricDouble value )
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

    public static MetricSummary absent()
    {
        return ABSENT;
    }

    private MetricSummary()
    {
        super(); // absent stats
    }

    private MetricSummary( MetricDouble latest, MetricDouble mean, MetricDouble stdDev )
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
        return (other instanceof MetricSummary)
               && super.equals(other);
    }

    public boolean equals( MetricSummary other )
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
        return latestToString(MetricDouble::toString);
    }

    public String latestToString( Function<? super MetricDouble, String> converter )
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
    public static final class Builder extends AbstractBuilder<MetricDouble, MetricSummary>
    {
        public static final int DEFAULT_WINDOW_SIZE = 3;

        private static final ToDoubleFunction<MetricDouble> TO_DOUBLE   = MetricDouble::inUnits;
        private static final DoubleFunction<MetricDouble>   FROM_DOUBLE = MetricDouble::ofUnits;

        private Builder()
        {
            super(DEFAULT_WINDOW_SIZE, TO_DOUBLE, FROM_DOUBLE);
        }

        private Builder( int windowSize )
        {
            super(windowSize, TO_DOUBLE, FROM_DOUBLE);
        }

        public Builder addValue( MetricDouble value ) throws IllegalArgumentException
        {
            Preconditions.checkArgument(value.isPresent(), "expected present metric value");
            _addValue(value);
            return this;
        }

        public Builder reset()
        {
            _reset();
            return this;
        }

        @Override
        public MetricSummary build()
        {
            if (_hasValues())
                return new MetricSummary(_getLatest(), _getMean(), _getStdDev());
            else
                return MetricSummary.absent();
        }
    }
}
