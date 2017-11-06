package net.varanus.util.math;


import java.util.Objects;
import java.util.function.DoubleFunction;
import java.util.function.ToDoubleFunction;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.collect.builder.BaseBuilder;


/**
 * @param <T>
 *            The type of summarized statistic
 */
@ReturnValuesAreNonnullByDefault
public interface StatSummary<T>
{
    public T getLatest();

    public T getMean();

    public T getStdDev();

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static abstract class AbstractBuilder<T, S extends StatSummary<T>> implements BaseBuilder<S>
    {
        private final DescriptiveStatistics descStats;
        private final ToDoubleFunction<T>   toDouble;
        private final DoubleFunction<T>     fromDouble;
        private @Nullable T                 latest;

        protected AbstractBuilder( int windowSize, ToDoubleFunction<T> toDouble, DoubleFunction<T> fromDouble )
        {
            this.descStats = new DescriptiveStatistics(windowSize);
            this.toDouble = Objects.requireNonNull(toDouble);
            this.fromDouble = Objects.requireNonNull(fromDouble);
            this.latest = null;
        }

        public final long getNumValues()
        {
            return descStats.getN();
        }

        protected final void _addValue( T value )
        {
            double d = toDouble.applyAsDouble(Objects.requireNonNull(value));
            descStats.addValue(d);
            this.latest = value;
        }

        protected final void _reset()
        {
            descStats.clear();
            this.latest = null;
        }

        protected final boolean _hasValues()
        {
            return descStats.getN() > 0;
        }

        protected final T _getLatest() throws IllegalStateException
        {
            if (_hasValues())
                return Objects.requireNonNull(latest);
            else
                throw new IllegalStateException("no values were added yet");
        }

        protected final T _getMean() throws IllegalStateException
        {
            if (_hasValues())
                return Objects.requireNonNull(fromDouble.apply(descStats.getMean()));
            else
                throw new IllegalStateException("no values were added yet");
        }

        protected final T _getStdDev() throws IllegalStateException
        {
            if (_hasValues())
                return Objects.requireNonNull(fromDouble.apply(descStats.getStandardDeviation()));
            else
                throw new IllegalStateException("no values were added yet");
        }
    }
}
