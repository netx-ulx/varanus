package net.varanus.sdncontroller.linkstats;


import java.time.Instant;

import javax.annotation.Nonnegative;
import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.base.Preconditions;

import net.varanus.sdncontroller.linkstats.Hysteresis.HysteresibleDataRate;
import net.varanus.sdncontroller.linkstats.Hysteresis.HysteresibleLatency;
import net.varanus.sdncontroller.linkstats.Hysteresis.HysteresibleLoss;
import net.varanus.sdncontroller.linkstats.Hysteresis.HysteresiblePacketRate;
import net.varanus.sdncontroller.util.MetricSummary;
import net.varanus.sdncontroller.util.Ratio;
import net.varanus.sdncontroller.util.RatioSummary;
import net.varanus.sdncontroller.util.TimeSummary;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.collect.builder.BaseBuilder;
import net.varanus.util.math.ExtraMath;
import net.varanus.util.time.TimeDouble;
import net.varanus.util.time.Timed;
import net.varanus.util.unitvalue.si.InfoDouble;
import net.varanus.util.unitvalue.si.MetricDouble;


/**
 * 
 */
public final class StatsBuilders
{
    @ReturnValuesAreNonnullByDefault
    public static interface BaseStatsBuilder<STATS, B extends BaseStatsBuilder<STATS, B>> extends BaseBuilder<STATS>
    {
        /**
         * Sets the current number of updates to the provided value.
         * 
         * @param numUpdates
         *            A non-negative value
         * @return this builder
         * @exception IllegalArgumentException
         *                If {@code numUpdates} is negative
         */
        public B setNumUpdates( @Nonnegative long numUpdates ) throws IllegalArgumentException;

        /**
         * Increments the current number of updates by one, saturating to
         * {@link Long#MAX_VALUE}.
         * 
         * @return this builder
         */
        public B incrementNumUpdates();

        /**
         * Resets the current number of updates to zero.
         * 
         * @return this builder
         */
        public B resetNumUpdates();

        /**
         * Resets all statistics of this builder to their default values and
         * sets their last-update-times to the {@linkplain Instant#now() current
         * time instant}. The number of updates is unchanged by this method.
         * 
         * @return this builder
         */
        public B clear();

        /**
         * Returns a snapshot of the current values of this builder's
         * statistics.
         */
        @Override
        public STATS build();
    }

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static interface LatencyStatsBuilder<STATS, B extends LatencyStatsBuilder<STATS, B>>
        extends BaseStatsBuilder<STATS, B>
    {
        /**
         * Adds the provided time duration to the current latency statistic
         * dataset and sets its last-update-time to the
         * {@linkplain Instant#now() current time instant}.
         * 
         * @param latency
         *            A time duration (must be
         *            {@linkplain TimeDouble#isPresent() present})
         * @return this builder
         * @exception IllegalArgumentException
         *                If {@code latency.isPresent() == false}
         */
        public default B collectLatency( TimeDouble latency ) throws IllegalArgumentException
        {
            return collectLatency(latency, Instant.now());
        }

        /**
         * Adds the provided time duration to the current latency statistic
         * dataset and sets its last-update-time to the provided timestamp.
         * 
         * @param latency
         *            A time duration (must be
         *            {@linkplain TimeDouble#isPresent() present})
         * @param timestamp
         *            A time instant
         * @return this builder
         * @exception IllegalArgumentException
         *                If {@code latency.isPresent() == false}
         */
        public B collectLatency( TimeDouble latency, Instant timestamp ) throws IllegalArgumentException;

        /**
         * Adds the provided time durations to the current latency statistic
         * dataset, and if at least one time duration is provided sets its
         * last-update-time to the {@linkplain Instant#now() current time
         * instant}.
         * 
         * @param latencies
         *            Multiple time durations (all must be
         *            {@linkplain TimeDouble#isPresent() present})
         * @return this builder
         * @exception IllegalArgumentException
         *                If for any element {@code lat} in {@code latencies}:
         *                {@code lat.isPresent() == false}
         */
        public default B collectLatencies( Iterable<TimeDouble> latencies ) throws IllegalArgumentException
        {
            return collectLatencies(latencies, Instant.now());
        }

        /**
         * Adds the provided time durations to the current latency statistic
         * dataset, and if at least one time duration is provided sets its
         * last-update-time to the provided timestamp.
         * 
         * @param latencies
         *            Multiple time durations (all must be
         *            {@linkplain TimeDouble#isPresent() present})
         * @param timestamp
         *            A time instant
         * @return this builder
         * @exception IllegalArgumentException
         *                If for any element {@code lat} in {@code latencies}:
         *                {@code lat.isPresent() == false}
         */
        public B collectLatencies( Iterable<TimeDouble> latencies, Instant timestamp ) throws IllegalArgumentException;

        /**
         * Clears the current latency statistic dataset and sets its
         * last-update-time to the {@linkplain Instant#now() current time
         * instant}.
         * 
         * @return this builder
         */
        public default B resetLatency()
        {
            return resetLatency(Instant.now());
        }

        /**
         * Clears the current latency statistic dataset and sets its
         * last-update-time to the provided timestamp.
         * 
         * @param timestamp
         *            A time instant
         * @return this builder
         */
        public B resetLatency( Instant timestamp );
    }

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static interface LossStatsBuilder<STATS, B extends LossStatsBuilder<STATS, B>>
        extends BaseStatsBuilder<STATS, B>
    {
        /**
         * Adds the provided ratio to the current byte loss statistic dataset
         * and sets its last-update-time to the {@linkplain Instant#now()
         * current time instant}.
         * 
         * @param byteLoss
         *            A byte loss ratio (must not be {@link Ratio#isNaN() NaN})
         * @return this builder
         * @exception IllegalArgumentException
         *                If {@code byteLoss.isNaN() == true}
         */
        public default B collectByteLoss( Ratio byteLoss ) throws IllegalArgumentException
        {
            return collectByteLoss(byteLoss, Instant.now());
        }

        /**
         * Adds the provided ratio to the current byte loss statistic dataset
         * and sets its last-update-time to the provided timestamp.
         * 
         * @param byteLoss
         *            A byte loss ratio (must not be {@link Ratio#isNaN() NaN})
         * @param timestamp
         *            A time instant
         * @return this builder
         * @exception IllegalArgumentException
         *                If {@code byteLoss.isNaN() == true}
         */
        public B collectByteLoss( Ratio byteLoss, Instant timestamp ) throws IllegalArgumentException;

        /**
         * Clears the current byte loss statistic dataset and sets its
         * last-update-time to the {@linkplain Instant#now() current time
         * instant}.
         * 
         * @return this builder
         */
        public default B resetByteLoss()
        {
            return resetByteLoss(Instant.now());
        }

        /**
         * Clears the current byte loss statistic dataset and sets its
         * last-update-time to the provided timestamp.
         * 
         * @param timestamp
         *            A time instant
         * @return this builder
         */
        public B resetByteLoss( Instant timestamp );

        /**
         * Adds the provided ratio to the current packet loss statistic dataset
         * and sets its last-update-time to the {@linkplain Instant#now()
         * current time instant}.
         * 
         * @param packetLoss
         *            A packet loss ratio (must not be {@link Ratio#isNaN()
         *            NaN})
         * @return this builder
         * @exception IllegalArgumentException
         *                If {@code packetLoss.isNaN() == true}
         */
        public default B collectPacketLoss( Ratio packetLoss ) throws IllegalArgumentException
        {
            return collectPacketLoss(packetLoss, Instant.now());
        }

        /**
         * Adds the provided ratio to the current packet loss statistic dataset
         * and sets its last-update-time to the provided timestamp.
         * 
         * @param packetLoss
         *            A packet loss ratio (must not be {@link Ratio#isNaN()
         *            NaN})
         * @param timestamp
         *            A time instant
         * @return this builder
         * @exception IllegalArgumentException
         *                If {@code packetLoss.isNaN() == true}
         */
        public B collectPacketLoss( Ratio packetLoss, Instant timestamp ) throws IllegalArgumentException;

        /**
         * Clears the current packet loss statistic dataset and sets its
         * last-update-time to the {@linkplain Instant#now() current time
         * instant}.
         * 
         * @return this builder
         */
        public default B resetPacketLoss()
        {
            return resetPacketLoss(Instant.now());
        }

        /**
         * Clears the current packet loss statistic dataset and sets its
         * last-update-time to the provided timestamp.
         * 
         * @param timestamp
         *            A time instant
         * @return this builder
         */
        public B resetPacketLoss( Instant timestamp );
    }

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static interface RateStatsBuilder<STATS, B extends RateStatsBuilder<STATS, B>>
        extends BaseStatsBuilder<STATS, B>
    {
        /**
         * Sets the current data transmission rate statistic to the provided
         * value and its last-update-time to the {@linkplain Instant#now()
         * current time instant}.
         * 
         * @param dataTransRate
         *            An information amount
         * @return this builder
         */
        public default B setDataTransmissionRate( InfoDouble dataTransRate )
        {
            return setDataTransmissionRate(dataTransRate, Instant.now());
        }

        /**
         * Sets the current data transmission rate statistic to the provided
         * value and its last-update-time to the provided timestamp.
         * 
         * @param dataTransRate
         *            An information amount
         * @param timestamp
         *            A time instant
         * @return this builder
         */
        public B setDataTransmissionRate( InfoDouble dataTransRate, Instant timestamp );

        /**
         * Sets the current data reception rate statistic to the provided value
         * and its last-update-time to the {@linkplain Instant#now() current
         * time instant}.
         * 
         * @param dataRecepRate
         *            An information amount
         * @return this builder
         */
        public default B setDataReceptionRate( InfoDouble dataRecepRate )
        {
            return setDataReceptionRate(dataRecepRate, Instant.now());
        }

        /**
         * Sets the current data reception rate statistic to the provided value
         * and its last-update-time to the provided timestamp.
         * 
         * @param dataRecepRate
         *            An information amount
         * @param timestamp
         *            A time instant
         * @return this builder
         */
        public B setDataReceptionRate( InfoDouble dataRecepRate, Instant timestamp );

        /**
         * Sets the current packet transmission rate statistic to the provided
         * value and its last-update-time to the {@linkplain Instant#now()
         * current time instant}.
         * 
         * @param pktTransRate
         *            A metric amount
         * @return this builder
         */
        public default B setPacketTransmissionRate( MetricDouble pktTransRate )
        {
            return setPacketTransmissionRate(pktTransRate, Instant.now());
        }

        /**
         * Sets the current packet transmission rate statistic to the provided
         * value and its last-update-time to the provided timestamp.
         * 
         * @param pktTransRate
         *            A metric amount
         * @param timestamp
         *            A time instant
         * @return this builder
         */
        public B setPacketTransmissionRate( MetricDouble pktTransRate, Instant timestamp );

        /**
         * Sets the current packet reception rate statistic to the provided
         * value and its last-update-time to the {@linkplain Instant#now()
         * current time instant}.
         * 
         * @param pktRecepRate
         *            A metric amount
         * @return this builder
         */
        public default B setPacketReceptionRate( MetricDouble pktRecepRate )
        {
            return setPacketReceptionRate(pktRecepRate, Instant.now());
        }

        /**
         * Sets the current packet reception rate statistic to the provided
         * value and its last-update-time to the provided timestamp.
         * 
         * @param pktRecepRate
         *            A metric amount
         * @param timestamp
         *            A time instant
         * @return this builder
         */
        public B setPacketReceptionRate( MetricDouble pktRecepRate, Instant timestamp );
    }

    // Create new abstract classes as needed

    @ReturnValuesAreNonnullByDefault
    static abstract class AbstractBaseStatsBuilder<STATS, B extends AbstractBaseStatsBuilder<STATS, B>>
        implements BaseStatsBuilder<STATS, B>
    {
        private long numUpdates;

        protected AbstractBaseStatsBuilder( long numUpdates )
        {
            this.numUpdates = numUpdates;
        }

        @Override
        public final B setNumUpdates( @Nonnegative long numUpdates ) throws IllegalArgumentException
        {
            Preconditions.checkArgument(numUpdates >= 0, "number of updates must be non-negative");
            this.numUpdates = numUpdates;
            return castThis();
        }

        @Override
        public final B incrementNumUpdates()
        {
            this.numUpdates = ExtraMath.addSaturated(this.numUpdates, 1);
            return castThis();
        }

        @Override
        public final B resetNumUpdates()
        {
            this.numUpdates = 0;
            return castThis();
        }

        protected final long getNumUpdates()
        {
            return numUpdates;
        }

        @SuppressWarnings( "unchecked" )
        protected final B castThis()
        {
            return (B)this;
        }
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    static abstract class AbstractLatencyStatsBuilder<STATS, B extends AbstractLatencyStatsBuilder<STATS, B>>
        extends AbstractBaseStatsBuilder<STATS, B> implements LatencyStatsBuilder<STATS, B>
    {
        private final HysteresibleLatency hystLat;

        protected AbstractLatencyStatsBuilder( int windowSize, double thresFactor )
        {
            super(0);
            this.hystLat = HysteresibleLatency.of(windowSize, thresFactor);
        }

        protected AbstractLatencyStatsBuilder( int windowSize,
                                               double thresFactor,
                                               Timed<TimeSummary> latency,
                                               long numUpdates )
        {
            super(numUpdates);
            this.hystLat = HysteresibleLatency.of(windowSize, thresFactor, latency);
        }

        @Override
        public final B collectLatency( TimeDouble latency, Instant timestamp ) throws IllegalArgumentException
        {
            hystLat.add(latency, timestamp);
            return castThis();
        }

        @Override
        public final B collectLatencies( Iterable<TimeDouble> latencies, Instant timestamp )
            throws IllegalArgumentException
        {
            hystLat.add(latencies, timestamp);
            return castThis();
        }

        @Override
        public final B resetLatency( Instant timestamp )
        {
            hystLat.reset(timestamp);
            return castThis();
        }

        @Override
        @OverridingMethodsMustInvokeSuper
        public B clear()
        {
            resetLatency();
            return castThis();
        }

        protected final Timed<TimeSummary> getLatencySnapshot()
        {
            return hystLat.get();
        }
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    static abstract class AbstractLatencyLossStatsBuilder<STATS, B extends AbstractLatencyLossStatsBuilder<STATS, B>>
        extends AbstractLatencyStatsBuilder<STATS, B> implements LossStatsBuilder<STATS, B>
    {
        private final HysteresibleLoss hystByteLoss;
        private final HysteresibleLoss hystPktLoss;

        protected AbstractLatencyLossStatsBuilder( int latWindowSize,
                                                   double latThresFactor,
                                                   int lossWindowSize,
                                                   double lossThresFactor )
        {
            super(latWindowSize, latThresFactor);
            this.hystByteLoss = HysteresibleLoss.of(lossWindowSize, lossThresFactor);
            this.hystPktLoss = HysteresibleLoss.of(lossWindowSize, lossThresFactor);
        }

        protected AbstractLatencyLossStatsBuilder( int latWindowSize,
                                                   double latThresFactor,
                                                   int lossWindowSize,
                                                   double lossThresFactor,
                                                   Timed<TimeSummary> latency,
                                                   Timed<RatioSummary> byteLoss,
                                                   Timed<RatioSummary> pktLoss,
                                                   long numUpdates )
        {
            super(latWindowSize, latThresFactor, latency, numUpdates);
            this.hystByteLoss = HysteresibleLoss.of(lossWindowSize, lossThresFactor, byteLoss);
            this.hystPktLoss = HysteresibleLoss.of(lossWindowSize, lossThresFactor, pktLoss);
        }

        @Override
        public final B collectByteLoss( Ratio byteLoss, Instant timestamp ) throws IllegalArgumentException
        {
            hystByteLoss.add(byteLoss, timestamp);
            return castThis();
        }

        @Override
        public final B resetByteLoss( Instant timestamp )
        {
            hystByteLoss.reset(timestamp);
            return castThis();
        }

        @Override
        public final B collectPacketLoss( Ratio packetLoss, Instant timestamp ) throws IllegalArgumentException
        {
            hystPktLoss.add(packetLoss, timestamp);
            return castThis();
        }

        @Override
        public final B resetPacketLoss( Instant timestamp )
        {
            hystPktLoss.reset(timestamp);
            return castThis();
        }

        @Override
        @OverridingMethodsMustInvokeSuper
        public B clear()
        {
            super.clear();
            resetByteLoss();
            resetPacketLoss();
            return castThis();
        }

        protected final Timed<RatioSummary> getByteLossSnapshot()
        {
            return hystByteLoss.get();
        }

        protected final Timed<RatioSummary> getPacketLossSnapshot()
        {
            return hystPktLoss.get();
        }
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    static abstract class AbstractRateStatsBuilder<STATS, B extends AbstractRateStatsBuilder<STATS, B>>
        extends AbstractBaseStatsBuilder<STATS, B> implements RateStatsBuilder<STATS, B>
    {
        private final HysteresibleDataRate   hystDataTxRate;
        private final HysteresibleDataRate   hystDataRecRate;
        private final HysteresiblePacketRate hystPktTxRate;
        private final HysteresiblePacketRate hystPktRecRate;

        protected AbstractRateStatsBuilder( double thresFactor )
        {
            super(0);
            this.hystDataTxRate = HysteresibleDataRate.of(thresFactor);
            this.hystDataRecRate = HysteresibleDataRate.of(thresFactor);
            this.hystPktTxRate = HysteresiblePacketRate.of(thresFactor);
            this.hystPktRecRate = HysteresiblePacketRate.of(thresFactor);
        }

        protected AbstractRateStatsBuilder( double thresFactor,
                                            Timed<InfoDouble> dataTxRate,
                                            Timed<InfoDouble> dataRecRate,
                                            Timed<MetricDouble> pktTxRate,
                                            Timed<MetricDouble> pktRecRate,
                                            long numUpdates )
        {
            super(numUpdates);
            this.hystDataTxRate = HysteresibleDataRate.of(thresFactor, dataTxRate);
            this.hystDataRecRate = HysteresibleDataRate.of(thresFactor, dataRecRate);
            this.hystPktTxRate = HysteresiblePacketRate.of(thresFactor, pktTxRate);
            this.hystPktRecRate = HysteresiblePacketRate.of(thresFactor, pktRecRate);
        }

        @Override
        public final B setDataTransmissionRate( InfoDouble dataTransRate, Instant timestamp )
        {
            hystDataTxRate.add(dataTransRate, timestamp);
            return castThis();
        }

        @Override
        public final B setDataReceptionRate( InfoDouble dataRecepRate, Instant timestamp )
        {
            hystDataRecRate.add(dataRecepRate, timestamp);
            return castThis();
        }

        @Override
        public final B setPacketTransmissionRate( MetricDouble pktTransRate, Instant timestamp )
        {
            hystPktTxRate.add(pktTransRate, timestamp);
            return castThis();
        }

        @Override
        public final B setPacketReceptionRate( MetricDouble pktRecepRate, Instant timestamp )
        {
            hystPktRecRate.add(pktRecepRate, timestamp);
            return castThis();
        }

        @Override
        @OverridingMethodsMustInvokeSuper
        public B clear()
        {
            setDataTransmissionRate(InfoDouble.absent());
            setDataReceptionRate(InfoDouble.absent());
            setPacketTransmissionRate(MetricDouble.absent());
            setPacketReceptionRate(MetricDouble.absent());
            return castThis();
        }

        protected final Timed<InfoDouble> getDataTxRateSnapshot()
        {
            return hystDataTxRate.get();
        }

        protected final Timed<InfoDouble> getDataRecRateSnapshot()
        {
            return hystDataRecRate.get();
        }

        protected final Timed<MetricDouble> getPacketTxRateSnapshot()
        {
            return hystPktTxRate.get();
        }

        protected final Timed<MetricDouble> getPacketRecRateSnapshot()
        {
            return hystPktRecRate.get();
        }
    }

    //@formatter:off
    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    static abstract class AbstractLatencyLossRateStatsBuilder<STATS, B extends AbstractLatencyLossRateStatsBuilder<STATS, B>>
        extends AbstractLatencyLossStatsBuilder<STATS, B> implements RateStatsBuilder<STATS, B>
    { //@formatter:on
        private final HysteresibleDataRate   hystDataTxRate;
        private final HysteresibleDataRate   hystDataRecRate;
        private final HysteresiblePacketRate hystPktTxRate;
        private final HysteresiblePacketRate hystPktRecRate;

        protected AbstractLatencyLossRateStatsBuilder( int latWindowSize,
                                                       double latThresFactor,
                                                       int lossWindowSize,
                                                       double lossThresFactor,
                                                       double rateThresFactor )
        {
            super(latWindowSize, latThresFactor, lossWindowSize, lossThresFactor);
            this.hystDataTxRate = HysteresibleDataRate.of(rateThresFactor);
            this.hystDataRecRate = HysteresibleDataRate.of(rateThresFactor);
            this.hystPktTxRate = HysteresiblePacketRate.of(rateThresFactor);
            this.hystPktRecRate = HysteresiblePacketRate.of(rateThresFactor);
        }

        protected AbstractLatencyLossRateStatsBuilder( int latWindowSize,
                                                       double latThresFactor,
                                                       int lossWindowSize,
                                                       double lossThresFactor,
                                                       double rateThresFactor,
                                                       Timed<TimeSummary> latency,
                                                       Timed<RatioSummary> byteLoss,
                                                       Timed<RatioSummary> pktLoss,
                                                       Timed<InfoDouble> dataTxRate,
                                                       Timed<InfoDouble> dataRecRate,
                                                       Timed<MetricDouble> pktTxRate,
                                                       Timed<MetricDouble> pktRecRate,
                                                       long numUpdates )
        {
            super(latWindowSize, latThresFactor, lossWindowSize, lossThresFactor,
                latency, byteLoss, pktLoss,
                numUpdates);
            this.hystDataTxRate = HysteresibleDataRate.of(rateThresFactor, dataTxRate);
            this.hystDataRecRate = HysteresibleDataRate.of(rateThresFactor, dataRecRate);
            this.hystPktTxRate = HysteresiblePacketRate.of(rateThresFactor, pktTxRate);
            this.hystPktRecRate = HysteresiblePacketRate.of(rateThresFactor, pktRecRate);
        }

        @Override
        public final B setDataTransmissionRate( InfoDouble dataTransRate, Instant timestamp )
        {
            hystDataTxRate.add(dataTransRate, timestamp);
            return castThis();
        }

        @Override
        public final B setDataReceptionRate( InfoDouble dataRecepRate, Instant timestamp )
        {
            hystDataRecRate.add(dataRecepRate, timestamp);
            return castThis();
        }

        @Override
        public final B setPacketTransmissionRate( MetricDouble pktTransRate, Instant timestamp )
        {
            hystPktTxRate.add(pktTransRate, timestamp);
            return castThis();
        }

        @Override
        public final B setPacketReceptionRate( MetricDouble pktRecepRate, Instant timestamp )
        {
            hystPktRecRate.add(pktRecepRate, timestamp);
            return castThis();
        }

        @Override
        @OverridingMethodsMustInvokeSuper
        public B clear()
        {
            super.clear();
            setDataTransmissionRate(InfoDouble.absent());
            setDataReceptionRate(InfoDouble.absent());
            setPacketTransmissionRate(MetricDouble.absent());
            setPacketReceptionRate(MetricDouble.absent());
            return castThis();
        }

        protected final Timed<InfoDouble> getDataTxRateSnapshot()
        {
            return hystDataTxRate.get();
        }

        protected final Timed<InfoDouble> getDataRecRateSnapshot()
        {
            return hystDataRecRate.get();
        }

        protected final Timed<MetricDouble> getPacketTxRateSnapshot()
        {
            return hystPktTxRate.get();
        }

        protected final Timed<MetricDouble> getPacketRecRateSnapshot()
        {
            return hystPktRecRate.get();
        }
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    static final class MetricRateBuilder implements BaseBuilder<Timed<MetricSummary>>
    {
        static MetricRateBuilder newBuilder( int rateWindowSize )
        {
            return new MetricRateBuilder(
                MetricSummary.newBuilder(rateWindowSize),
                Instant.now());
        }

        static MetricRateBuilder newBuilder( int rateWindowSize, Timed<MetricSummary> initialValue )
        {
            return new MetricRateBuilder(
                MetricSummary.newBuilder(rateWindowSize).addValue(initialValue.value().getLatest()),
                initialValue.timestamp());
        }

        private final MetricSummary.Builder builder;
        private Instant                     timestamp;

        private MetricRateBuilder( MetricSummary.Builder builder, Instant timestamp )
        {
            this.builder = builder;
            this.timestamp = timestamp;
        }

        void addValue( MetricDouble value, Instant timestamp )
        {
            this.builder.addValue(value);
            this.timestamp = timestamp;
        }

        void reset()
        {
            this.builder.reset();
            this.timestamp = Instant.now();
        }

        @Override
        public Timed<MetricSummary> build()
        {
            return Timed.of(builder.build(), timestamp);
        }
    }

    private StatsBuilders()
    {
        // not used
    }
}
