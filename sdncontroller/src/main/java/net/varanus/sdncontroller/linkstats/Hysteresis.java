package net.varanus.sdncontroller.linkstats;


import java.time.Instant;
import java.util.Objects;

import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.sdncontroller.util.Ratio;
import net.varanus.sdncontroller.util.RatioSummary;
import net.varanus.sdncontroller.util.TimeSummary;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.lang.MoreObjects;
import net.varanus.util.math.Hysteresible;
import net.varanus.util.time.TimeDouble;
import net.varanus.util.time.TimeDoubleUnit;
import net.varanus.util.time.Timed;
import net.varanus.util.unitvalue.si.InfoDouble;
import net.varanus.util.unitvalue.si.InfoDoubleUnit;
import net.varanus.util.unitvalue.si.MetricDouble;
import net.varanus.util.unitvalue.si.MetricDoublePrefix;


/**
 * 
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
final class Hysteresis
{
    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    static final class HysteresibleLatency
    {
        static HysteresibleLatency of( int windowSize, double thresFactor )
        {
            return new HysteresibleLatency(
                TimeSummary.newBuilder(windowSize),
                new HysteresibleTimeDouble(thresFactor),
                Instant.now());
        }

        static HysteresibleLatency of( int windowSize, double thresFactor, Timed<TimeSummary> initialValue )
        {
            return new HysteresibleLatency(
                initialValue.value().createBuilder(windowSize),
                new HysteresibleTimeDouble(thresFactor, initialValue.value().getMean()),
                initialValue.timestamp());
        }

        private final TimeSummary.Builder      builder;
        private final Hysteresible<TimeDouble> hysteresible;
        private Instant                        timestamp;

        private HysteresibleLatency( TimeSummary.Builder builder,
                                     Hysteresible<TimeDouble> hysteresible,
                                     Instant timestamp )
        {
            this.builder = builder;
            this.hysteresible = hysteresible;
            this.timestamp = timestamp;
        }

        void add( TimeDouble value, Instant timestamp ) throws IllegalArgumentException
        {
            MoreObjects.requireNonNull(value, "value", timestamp, "timestamp");
            this.builder.addValue(value);
            this.timestamp = timestamp;
        }

        void add( Iterable<TimeDouble> values, Instant timestamp ) throws IllegalArgumentException
        {
            MoreObjects.requireNonNull(values, "values", timestamp, "timestamp");
            long prevNumValues = this.builder.getNumValues();
            this.builder.addValues(values);
            if (this.builder.getNumValues() > prevNumValues)
                this.timestamp = timestamp;
        }

        void reset( Instant timestamp )
        {
            Objects.requireNonNull(timestamp);
            this.builder.reset();
            this.hysteresible.reset();
            this.timestamp = timestamp;
        }

        Timed<TimeSummary> get()
        {
            TimeSummary summ = builder.build();
            if (summ.isPresent()) {
                hysteresible.update(summ.getMean());
                TimeSummary hysteresed = TimeSummary.of(summ.getLatest(), hysteresible.value(), summ.getStdDev());
                return Timed.of(hysteresed, timestamp);
            }
            else {
                hysteresible.update(TimeDouble.absent());
                return Timed.of(TimeSummary.absent(), timestamp);
            }
        }
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    static final class HysteresibleLoss
    {
        static HysteresibleLoss of( int windowSize, double thresFactor )
        {
            return new HysteresibleLoss(
                RatioSummary.newBuilder(windowSize),
                new HysteresibleRatio(thresFactor),
                Instant.now());
        }

        static HysteresibleLoss of( int windowSize, double thresFactor, Timed<RatioSummary> initialValue )
        {
            return new HysteresibleLoss(
                initialValue.value().createBuilder(windowSize),
                new HysteresibleRatio(thresFactor, initialValue.value().getMean()),
                initialValue.timestamp());
        }

        private final RatioSummary.Builder builder;
        private final Hysteresible<Ratio>  hysteresible;
        private Instant                    timestamp;

        private HysteresibleLoss( RatioSummary.Builder builder, Hysteresible<Ratio> hysteresible, Instant timestamp )
        {
            this.builder = builder;
            this.hysteresible = hysteresible;
            this.timestamp = timestamp;
        }

        void add( Ratio value, Instant timestamp ) throws IllegalArgumentException
        {
            MoreObjects.requireNonNull(value, "value", timestamp, "timestamp");
            this.builder.addValue(value);
            this.timestamp = timestamp;
        }

        void reset( Instant timestamp )
        {
            Objects.requireNonNull(timestamp);
            this.builder.reset();
            this.hysteresible.reset();
            this.timestamp = timestamp;
        }

        Timed<RatioSummary> get()
        {
            RatioSummary summ = builder.build();
            if (summ.isPresent()) {
                hysteresible.update(summ.getMean());
                RatioSummary hysteresed = RatioSummary.of(summ.getLatest(), hysteresible.value(), summ.getStdDev());
                return Timed.of(hysteresed, timestamp);
            }
            else {
                hysteresible.update(Ratio.NaN);
                return Timed.of(RatioSummary.absent(), timestamp);
            }
        }
    }

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    static final class HysteresibleDataRate extends HysteresibleRate<InfoDouble>
    {
        static HysteresibleDataRate of( double thresFactor )
        {
            return new HysteresibleDataRate(
                new HysteresibleInfoDouble(thresFactor),
                Instant.now());
        }

        static HysteresibleDataRate of( double thresFactor, Timed<InfoDouble> initialValue )
        {
            return new HysteresibleDataRate(
                new HysteresibleInfoDouble(thresFactor, initialValue.value()),
                initialValue.timestamp());
        }

        private HysteresibleDataRate( Hysteresible<InfoDouble> hysteresible, Instant timestamp )
        {
            super(hysteresible, timestamp);
        }
    }

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    static final class HysteresiblePacketRate extends HysteresibleRate<MetricDouble>
    {
        static HysteresiblePacketRate of( double thresFactor )
        {
            return new HysteresiblePacketRate(
                new HysteresibleMetricDouble(thresFactor),
                Instant.now());
        }

        static HysteresiblePacketRate of( double thresFactor, Timed<MetricDouble> initialValue )
        {
            return new HysteresiblePacketRate(
                new HysteresibleMetricDouble(thresFactor, initialValue.value()),
                initialValue.timestamp());
        }

        private HysteresiblePacketRate( Hysteresible<MetricDouble> hysteresible, Instant timestamp )
        {
            super(hysteresible, timestamp);
        }
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    private static abstract class HysteresibleRate<T>
    {
        private final Hysteresible<T> hysteresible;
        private Instant               timestamp;

        protected HysteresibleRate( Hysteresible<T> hysteresible, Instant timestamp )
        {
            this.hysteresible = hysteresible;
            this.timestamp = timestamp;
        }

        void add( T value, Instant timestamp )
        {
            MoreObjects.requireNonNull(value, "value", timestamp, "timestamp");
            this.hysteresible.update(value);
            this.timestamp = timestamp;
        }

        Timed<T> get()
        {
            return Timed.of(hysteresible.value(), timestamp);
        }
    }

    @ParametersAreNonnullByDefault
    private static final class HysteresibleTimeDouble extends Hysteresible<TimeDouble>
    {
        HysteresibleTimeDouble( double thresFactor )
        {
            super(thresFactor);
        }

        HysteresibleTimeDouble( double thresFactor, TimeDouble initialValue )
        {
            super(thresFactor, initialValue);
        }

        @Override
        protected double _convert( TimeDouble newValue )
        {
            return newValue.inOrElse(TimeDoubleUnit.NANOSECONDS, Double.NaN);
        }

        @Override
        protected TimeDouble _deconvert( double newDouble )
        {
            return Double.isNaN(newDouble) ? TimeDouble.absent() : TimeDouble.ofNanos(newDouble);
        }
    }

    @ParametersAreNonnullByDefault
    private static final class HysteresibleRatio extends Hysteresible<Ratio>
    {
        HysteresibleRatio( double thresFactor )
        {
            super(thresFactor);
        }

        HysteresibleRatio( double thresFactor, Ratio initialValue )
        {
            super(thresFactor, initialValue);
        }

        @Override
        protected double _convert( Ratio newValue )
        {
            return newValue.doubleValue();
        }

        @Override
        protected Ratio _deconvert( double newDouble )
        {
            return Ratio.of(newDouble);
        }
    }

    @ParametersAreNonnullByDefault
    private static final class HysteresibleInfoDouble extends Hysteresible<InfoDouble>
    {
        HysteresibleInfoDouble( double thresFactor )
        {
            super(thresFactor);
        }

        HysteresibleInfoDouble( double thresFactor, InfoDouble initialValue )
        {
            super(thresFactor, initialValue);
        }

        @Override
        protected double _convert( InfoDouble newValue )
        {
            return newValue.inOrElse(InfoDoubleUnit.BITS, Double.NaN);
        }

        @Override
        protected InfoDouble _deconvert( double newDouble )
        {
            return Double.isNaN(newDouble) ? InfoDouble.absent() : InfoDouble.ofBits(newDouble);
        }
    }

    @ParametersAreNonnullByDefault
    private static final class HysteresibleMetricDouble extends Hysteresible<MetricDouble>
    {
        HysteresibleMetricDouble( double thresFactor )
        {
            super(thresFactor);
        }

        HysteresibleMetricDouble( double thresFactor, MetricDouble initialValue )
        {
            super(thresFactor, initialValue);
        }

        @Override
        protected double _convert( MetricDouble newValue )
        {
            return newValue.inOrElse(MetricDoublePrefix.UNIT, Double.NaN);
        }

        @Override
        protected MetricDouble _deconvert( double newDouble )
        {
            return Double.isNaN(newDouble) ? MetricDouble.absent() : MetricDouble.ofUnits(newDouble);
        }
    }
}
