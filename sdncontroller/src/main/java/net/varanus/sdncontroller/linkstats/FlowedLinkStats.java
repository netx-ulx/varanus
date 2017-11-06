package net.varanus.sdncontroller.linkstats;


import static net.varanus.sdncontroller.util.stats.StatType.SAFE;
import static net.varanus.sdncontroller.util.stats.StatType.UNSAFE;

import java.time.Instant;
import java.util.Objects;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import com.google.common.base.Preconditions;

import net.varanus.sdncontroller.linkstats.Hysteresis.HysteresibleDataRate;
import net.varanus.sdncontroller.linkstats.Hysteresis.HysteresiblePacketRate;
import net.varanus.sdncontroller.linkstats.StatsBuilders.AbstractLatencyLossRateStatsBuilder;
import net.varanus.sdncontroller.linkstats.StatsBuilders.AbstractRateStatsBuilder;
import net.varanus.sdncontroller.types.FlowedLink;
import net.varanus.sdncontroller.util.Ratio;
import net.varanus.sdncontroller.util.RatioSummary;
import net.varanus.sdncontroller.util.TimeSummary;
import net.varanus.sdncontroller.util.stats.Stat;
import net.varanus.sdncontroller.util.stats.StatType;
import net.varanus.sdncontroller.util.stats.StatValuePrinters;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.functional.Possible;
import net.varanus.util.lang.MoreObjects;
import net.varanus.util.math.ExtraMath;
import net.varanus.util.math.HysteresibleDouble;
import net.varanus.util.text.StringUtils;
import net.varanus.util.time.TimeLong;
import net.varanus.util.time.Timed;
import net.varanus.util.unitvalue.si.InfoDouble;
import net.varanus.util.unitvalue.si.InfoLong;
import net.varanus.util.unitvalue.si.MetricDouble;
import net.varanus.util.unitvalue.si.MetricLong;


/**
 * Flow-aware network statistics for a {@link FlowedLink}.
 */
@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class FlowedLinkStats
{
    /**
     * Returns new flowed link statistics.
     * 
     * @param link
     *            The flowed-link associated to the statistics
     * @param counterSubStats
     *            Switch-counter-based sub-statistics
     * @param trajectorySubStats
     *            Trajectory-based sub-statistics
     * @param generalStats
     *            General link statistics
     * @return a new {@code FlowedLinkStats} instance
     */
    public static FlowedLinkStats of( FlowedLink link,
                                      SwitchCounterSubStats counterSubStats,
                                      TrajectorySubStats trajectorySubStats,
                                      GeneralLinkStats generalStats )
    {
        MoreObjects.requireNonNull(
            link, "link",
            counterSubStats, "counterSubStats",
            trajectorySubStats, "trajectorySubStats",
            generalStats, "generalStats");
        Preconditions.checkArgument(link.contains(generalStats.getLink()),
            "general link statistics must be associated to a datapath-link contained in the provided flowed-link");
        return new FlowedLinkStats(link, counterSubStats, trajectorySubStats, generalStats);
    }

    /**
     * Returns flowed link statistics that are completely absent.
     * 
     * @param link
     *            The flowed-link associated to the statistics
     * @return a new {@code FlowedLinkStats} instance with absent values
     */
    public static FlowedLinkStats absent( FlowedLink link )
    {
        Objects.requireNonNull(link);
        return new FlowedLinkStats(link,
            SwitchCounterSubStats.absent(),
            TrajectorySubStats.absent(),
            GeneralLinkStats.absent(link.unflowed()));
    }

    private final FlowedLink            link;
    private final SwitchCounterSubStats switchCounter;
    private final TrajectorySubStats    trajectory;
    private final GeneralLinkStats      general;

    private FlowedLinkStats( FlowedLink link,
                             SwitchCounterSubStats switchCounter,
                             TrajectorySubStats trajectory,
                             GeneralLinkStats general )
    {
        this.link = link;
        this.switchCounter = switchCounter;
        this.trajectory = trajectory;
        this.general = general;
    }

    /**
     * Returns the flowed-link associated to these statistics.
     * 
     * @return a {@code FlowedLink} object
     */
    public FlowedLink getLink()
    {
        return link;
    }

    /**
     * Returns the estimated latency of the flowed-link.
     * <p>
     * The returned statistic is {@linkplain StatType#SAFE safe} only if its
     * value is present and it was either obtained from trajectory sampling or a
     * secure probing mechanism; otherwise it is {@linkplain StatType#UNSAFE
     * unsafe}.
     * 
     * @return a {@code Stat<TimeSummary>} instance
     */
    public Stat<TimeSummary> getLatency()
    {
        if (trajectory.getLatency().value().isPresent())
            return trajectory.getLatency();
        else
            return general.getLatency();
    }

    /**
     * Returns the estimated data rate per second of traffic traversing the
     * flowed-link.
     * <p>
     * The returned statistic is {@linkplain StatType#SAFE safe} only if its
     * value is present and it was obtained from trajectory sampling; otherwise
     * it is {@linkplain StatType#UNSAFE unsafe}.
     * 
     * @return a {@code Stat<InfoDouble>} value
     */
    public Stat<InfoDouble> getThroughput()
    {
        if (trajectory.getDataThroughput().value().isPresent())
            return trajectory.getDataThroughput();
        else
            return switchCounter.getDataThroughput();
    }

    /**
     * Returns the data capacity (at the source) of the datapath-link contained
     * in the flowed-link.
     * <p>
     * The returned statistic is {@linkplain StatType#SAFE safe} only if its
     * value is present, otherwise it is {@linkplain StatType#UNSAFE unsafe}.
     * 
     * @return a {@code Stat<InfoDouble>} value
     */
    public Stat<InfoDouble> getDataCapacity()
    {
        return general.getDataCapacity();
    }

    /**
     * Returns the positive difference (>= 0) between the data capacity and the
     * throughput.
     * <p>
     * The returned statistic is {@linkplain StatType#SAFE safe} only if
     * both data capacity and throughput are secure; otherwise it is
     * {@linkplain StatType#UNSAFE unsafe}.
     * 
     * @return a {@code Stat<InfoDouble>} value
     */
    public Stat<InfoDouble> getAvailableBandwidth()
    {
        return general.getAvailableBandwidth(getThroughput());
    }

    /**
     * Returns the proportion of the throughput relative to the data capacity.
     * <p>
     * The returned statistic is {@linkplain StatType#SAFE safe} only if
     * both data capacity and throughput are secure; otherwise it is
     * {@linkplain StatType#UNSAFE unsafe}.
     * 
     * @return a {@code Stat<Possible<Ratio>>} value
     */
    public Stat<Possible<Ratio>> getDataUtilization()
    {
        return general.getDataUtilization(getThroughput());
    }

    /**
     * Returns the estimated byte loss of the flowed-link.
     * <p>
     * The returned statistic is {@linkplain StatType#SAFE safe} only if its
     * value is present and it was either obtained from trajectory sampling or a
     * secure probing mechanism; otherwise it is {@linkplain StatType#UNSAFE
     * unsafe}.
     * 
     * @return a {@code Stat<RatioSummary>} value
     */
    public Stat<RatioSummary> getByteLoss()
    {
        if (trajectory.getByteLoss().value().isPresent())
            return trajectory.getByteLoss();
        else if (general.getByteLoss().value().isPresent())
            return general.getByteLoss();
        else
            return switchCounter.getByteLoss();
    }

    /**
     * Returns the estimated packet loss of the flowed-link.
     * <p>
     * The returned statistic is {@linkplain StatType#SAFE safe} only if its
     * value is present and it was either obtained from trajectory sampling or a
     * secure probing mechanism; otherwise it is {@linkplain StatType#UNSAFE
     * unsafe}.
     * 
     * @return a {@code Stat<RatioSummary>} value
     */
    public Stat<RatioSummary> getPacketLoss()
    {
        if (trajectory.getPacketLoss().value().isPresent())
            return trajectory.getPacketLoss();
        else if (general.getPacketLoss().value().isPresent())
            return general.getPacketLoss();
        else
            return switchCounter.getPacketLoss();
    }

    /**
     * Returns the total number of times these statistics were updated.
     * <p>
     * More specifically, this method returns the sum of the number of updates
     * of each sub-statistics and general statistics, saturated to
     * {@link Long#MAX_VALUE}.
     * 
     * @return a {@code long} value
     */
    public long getTotalUpdates()
    {
        long total = 0;
        total = ExtraMath.addSaturated(total, switchCounter.getNumUpdates());
        total = ExtraMath.addSaturated(total, trajectory.getNumUpdates());
        total = ExtraMath.addSaturated(total, general.getTotalUpdates());
        return total;
    }

    /**
     * Returns sub-statistics obtained from switch counters.
     * 
     * @return an {@code SwitchCounterSubStats} object
     */
    public SwitchCounterSubStats switchCounter()
    {
        return switchCounter;
    }

    /**
     * Returns sub-statistics obtained from trajectory sampling.
     * 
     * @return an {@code TrajectorySubStats} object
     */
    public TrajectorySubStats trajectory()
    {
        return trajectory;
    }

    /**
     * Returns the general statistics of the datapath-link containing the
     * flowed-link.
     * 
     * @return an {@code GeneralLinkStats} object
     */
    public GeneralLinkStats general()
    {
        return general;
    }

    /**
     * Indicates whether this instance has the same core statistics as the
     * provided instance.
     * <p>
     * The core statistics are the core statistics of every sub-statistics and
     * general link statistics.
     * 
     * @param other
     *            A {@code FlowedLinkStats} instance
     * @return {@code true} iff this instance has the same core statistics as
     *         the provided instance
     */
    public boolean hasSameCoreStats( FlowedLinkStats other )
    {
        return this.switchCounter.hasSameCoreStats(other.switchCounter)
               && this.trajectory.hasSameCoreStats(other.trajectory)
               && this.general.hasSameCoreStats(other.general);
    }

    @Override
    public boolean equals( Object other )
    {
        return (other instanceof FlowedLinkStats)
               && this.equals((FlowedLinkStats)other);
    }

    public boolean equals( FlowedLinkStats other )
    {
        return (other != null)
               && this.link.equals(other.link)
               && this.switchCounter.equals(other.switchCounter)
               && this.trajectory.equals(other.trajectory)
               && this.general.equals(other.general);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(link, switchCounter, trajectory, general);
    }

    @Override
    public String toString()
    {
        return StringUtils.joinAllPS(", ", "FlowedLinkStats{", "}",
            "flowed_link=" + link,
            "latency=" + StatsUtils.mainStatToString(getLatency()),
            "latency_latest=" + getLatency().value().latestToString(),
            "throughput=" + StatsUtils.mainStatToString(getThroughput()),
            "data_capacity=" + StatsUtils.mainStatToString(getDataCapacity()),
            "available_bandwidth=" + StatsUtils.mainStatToString(getAvailableBandwidth()),
            "data_utilization=" + StatsUtils.mainStatToString(getDataUtilization()),
            "byte_loss=" + StatsUtils.mainStatToString(getByteLoss()),
            "byte_loss_latest=" + getByteLoss().value().latestToString(),
            "packet_loss=" + StatsUtils.mainStatToString(getPacketLoss()),
            "packet_loss_latest=" + getPacketLoss().value().latestToString(),
            "total_num_updates=" + getTotalUpdates(),
            switchCounter,
            trajectory,
            general);
    }

    /**
     * Returns a summary of these statistics in a prettified format.
     *
     * @return a {@code String}
     */
    public String toPrettyString()
    {
        return StringUtils.joinAllInLines(
            "===================== Flowed-link statistics ===================================",
            "Flowed-link         : " + link,
            "",
            "Latency             : " + StatsUtils.mainStatToPrettyString(getLatency()),
            "Latency (latest)    : " + getLatency().value().latestToString(),
            "",
            "Throughput          : " + StatsUtils.mainStatToPrettyString(getThroughput()),
            "Data capacity       : " + StatsUtils.mainStatToPrettyString(getDataCapacity()),
            "Available bandwidth : " + StatsUtils.mainStatToPrettyString(getAvailableBandwidth()),
            "Data utilization    : " + StatsUtils.mainStatToPrettyString(getDataUtilization()),
            "",
            "Byte loss           : " + StatsUtils.mainStatToPrettyString(getByteLoss()),
            "Byte loss (latest)  : " + getByteLoss().value().latestToString(),
            "Packet loss         : " + StatsUtils.mainStatToPrettyString(getPacketLoss()),
            "Packet loss (latest): " + getPacketLoss().value().latestToString(),
            "",
            "Total updates       : " + getTotalUpdates(),
            "================================================================================");
    }

    /**
     * Flow-aware sub-statistics obtained from switch counters.
     */
    @Immutable
    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class SwitchCounterSubStats
    {
        private static final SwitchCounterSubStats ABSENT = newBuilder().build();

        /**
         * Returns a new builder for switch-counter sub-statistics.
         * 
         * @return a new builder for {@code SwitchCounterSubStats} instances
         */
        public static Builder newBuilder()
        {
            return newBuilder(HysteresibleDouble.DEFAULT_THRESHOLD_FACTOR);
        }

        /**
         * Returns a new builder for switch-counter sub-statistics.
         * 
         * @param rateThresFactor
         *            The hysteresis threshold factor of the (data/packet) rates
         * @return a new builder for {@code SwitchCounterSubStats} instances
         */
        public static Builder newBuilder( double rateThresFactor )
        {
            return new Builder(rateThresFactor);
        }

        /**
         * Returns switch-counter sub-statistics that are completely absent.
         * 
         * @return a new {@code SwitchCounterSubStats} instance with absent
         *         values
         */
        public static SwitchCounterSubStats absent()
        {
            return ABSENT;
        }

        private final Stat<InfoDouble>   dataTxRate;
        private final Stat<InfoDouble>   dataRecRate;
        private final Stat<MetricDouble> pktTxRate;
        private final Stat<MetricDouble> pktRecRate;
        private final TimeLong           lastRoundDuration;
        private final long               numUpdates;

        private SwitchCounterSubStats( Timed<InfoDouble> dataTxRate,
                                       Timed<InfoDouble> dataRecRate,
                                       Timed<MetricDouble> pktTxRate,
                                       Timed<MetricDouble> pktRecRate,
                                       TimeLong lastRoundDuration,
                                       long numUpdates )
        {

            this.dataTxRate = StatsUtils.ofTimed(dataTxRate, SAFE, StatValuePrinters::dataPerSecond);
            this.dataRecRate = StatsUtils.ofTimed(dataRecRate, SAFE, StatValuePrinters::dataPerSecond);
            this.pktTxRate = StatsUtils.ofTimed(pktTxRate, SAFE, StatValuePrinters::packetsPerSecond);
            this.pktRecRate = StatsUtils.ofTimed(pktRecRate, SAFE, StatValuePrinters::packetsPerSecond);
            this.lastRoundDuration = lastRoundDuration;
            this.numUpdates = numUpdates;
        }

        /**
         * Returns the estimated data rate per second of traffic exiting the
         * source endpoint of the flowed-link.
         * <p>
         * This is a {@linkplain StatType#SAFE safe} sub-statistic if its
         * value is present, otherwise it is {@linkplain StatType#UNSAFE
         * unsafe}.
         * 
         * @return a {@code Stat<InfoDouble>} value
         */
        public Stat<InfoDouble> getDataTransmissionRate()
        {
            return dataTxRate;
        }

        /**
         * Returns the estimated data rate per second of traffic entering the
         * destination endpoint of the flowed-link.
         * <p>
         * This is a {@linkplain StatType#SAFE safe} sub-statistic if its
         * value is present, otherwise it is {@linkplain StatType#UNSAFE
         * unsafe}.
         * 
         * @return a {@code Stat<InfoDouble>} value
         */
        public Stat<InfoDouble> getDataReceptionRate()
        {
            return dataRecRate;
        }

        /**
         * Returns the estimated packet rate per second of traffic exiting the
         * source endpoint of the flowed-link.
         * <p>
         * This is a {@linkplain StatType#SAFE safe} sub-statistic if its
         * value is present, otherwise it is {@linkplain StatType#UNSAFE
         * unsafe}.
         * 
         * @return a {@code Stat<MetricDouble>} value
         */
        public Stat<MetricDouble> getPacketTransmissionRate()
        {
            return pktTxRate;
        }

        /**
         * Returns the estimated packet rate per second of traffic entering the
         * destination endpoint of the flowed-link.
         * <p>
         * This is a {@linkplain StatType#SAFE safe} sub-statistic if its
         * value is present, otherwise it is {@linkplain StatType#UNSAFE
         * unsafe}.
         * 
         * @return a {@code Stat<MetricDouble>} value
         */
        public Stat<MetricDouble> getPacketReceptionRate()
        {
            return pktRecRate;
        }

        /**
         * Returns the estimated data rate per second of traffic traversing the
         * flowed-link.
         * <p>
         * This is an {@linkplain StatType#UNSAFE unsafe} sub-statistic.
         * 
         * @return a {@code Stat<InfoDouble>} value
         */
        public Stat<InfoDouble> getDataThroughput()
        {
            return dataRecRate.with(UNSAFE);
        }

        /**
         * Returns the estimated packet rate per second of traffic traversing
         * the flowed-link.
         * <p>
         * This is an {@linkplain StatType#UNSAFE unsafe} sub-statistic.
         * 
         * @return a {@code Stat<MetricDouble>} value
         */
        public Stat<MetricDouble> getPacketThroughput()
        {
            return pktRecRate.with(UNSAFE);
        }

        /**
         * Returns the estimated byte loss of the flowed-link.
         * <p>
         * This is an {@linkplain StatType#UNSAFE unsafe} sub-statistic.
         * 
         * @return a {@code Stat<RatioSummary>} value
         */
        public Stat<RatioSummary> getByteLoss()
        {
            return StatsUtils.ofCombined(dataTxRate, dataRecRate, UNSAFE,
                ( txRate, recRate ) -> {
                    // lost == (txRate - rxRate) if txRate > rxRate else 0
                    InfoLong lost = txRate.asLong().posDiff(recRate.asLong());
                    if (lost.isPresent() && txRate.isPresent()) {
                        Ratio r = Ratio.of(lost.inBytes(), txRate.asLong().inBytes());
                        return r.isNaN() ? RatioSummary.absent() : RatioSummary.of(r);
                    }
                    else {
                        return RatioSummary.absent();
                    }
                },
                RatioSummary::latestToString);
        }

        /**
         * Returns the estimated packet loss of the flowed-link.
         * <p>
         * This is an {@linkplain StatType#UNSAFE unsafe} sub-statistic.
         * 
         * @return a {@code Stat<RatioSummary>} value
         */
        public Stat<RatioSummary> getPacketLoss()
        {
            return StatsUtils.ofCombined(pktTxRate, pktRecRate, UNSAFE,
                ( txRate, recRate ) -> {
                    // lost == (txRate - rxRate) if txRate > rxRate else 0
                    MetricLong lost = txRate.asLong().posDiff(recRate.asLong());
                    if (lost.isPresent() && txRate.isPresent()) {
                        Ratio r = Ratio.of(lost.inUnits(), txRate.asLong().inUnits());
                        return r.isNaN() ? RatioSummary.absent() : RatioSummary.of(r);
                    }
                    else {
                        return RatioSummary.absent();
                    }
                },
                RatioSummary::latestToString);
        }

        /**
         * Returns the duration of the last sampling round.
         * 
         * @return a {@code TimeLong} value
         */
        public TimeLong getLastRoundDuration()
        {
            return lastRoundDuration;
        }

        /**
         * Returns the number of times these sub-statistics were updated,
         * saturated to {@link Long#MAX_VALUE}.
         * 
         * @return a {@code long} value
         */
        public long getNumUpdates()
        {
            return numUpdates;
        }

        /**
         * Returns a new builder initialized with this instance's values.
         * 
         * @return a builder for {@code SwitchCounterSubStats} instances
         */
        public Builder createBuilder()
        {
            return createBuilder(HysteresibleDouble.DEFAULT_THRESHOLD_FACTOR);
        }

        /**
         * Returns a new builder initialized with this instance's values.
         * 
         * @param rateThresFactor
         *            The hysteresis threshold factor of the (data/packet) rates
         * @return a builder for {@code SwitchCounterSubStats} instances
         */
        public Builder createBuilder( double rateThresFactor )
        {
            return new Builder(
                rateThresFactor,
                dataTxRate, dataRecRate,
                pktTxRate, pktRecRate,
                lastRoundDuration,
                numUpdates);
        }

        /**
         * Indicates whether this instance has the same core statistics as the
         * provided instance.
         * <p>
         * The core statistics are:
         * <ul>
         * <li>{@linkplain #getDataTransmissionRate() data transmission
         * rate}</li>
         * <li>{@linkplain #getDataReceptionRate() data reception rate}</li>
         * <li>{@linkplain #getPacketTransmissionRate() packet transmission
         * rate}</li>
         * <li>{@linkplain #getPacketReceptionRate() packet reception rate}</li>
         * </ul>
         * 
         * @param other
         *            A {@code SwitchCounterSubStats} instance
         * @return {@code true} iff this instance has the same core statistics
         *         as the provided instance
         */
        public boolean hasSameCoreStats( SwitchCounterSubStats other )
        {
            return this.dataTxRate.equals(other.dataTxRate)
                   && this.dataRecRate.equals(other.dataRecRate)
                   && this.pktTxRate.equals(other.pktTxRate)
                   && this.pktRecRate.equals(other.pktRecRate);
        }

        @Override
        public boolean equals( Object other )
        {
            return (other instanceof SwitchCounterSubStats)
                   && this.equals((SwitchCounterSubStats)other);
        }

        public boolean equals( SwitchCounterSubStats other )
        {
            return (other != null)
                   && this.hasSameCoreStats(other)
                   && this.lastRoundDuration.equals(other.lastRoundDuration)
                   && this.numUpdates == other.numUpdates;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(
                dataTxRate, dataRecRate,
                pktTxRate, pktRecRate,
                lastRoundDuration,
                numUpdates);
        }

        @Override
        public String toString()
        {
            return StringUtils.joinAllPS(", ", "SwitchCounterSubStats[", "]",
                "data_tx_rate=" + StatsUtils.subStatToString(dataTxRate),
                "data_rx_rate=" + StatsUtils.subStatToString(dataRecRate),
                "packet_tx_rate=" + StatsUtils.subStatToString(pktTxRate),
                "packet_rx_rate=" + StatsUtils.subStatToString(pktRecRate),
                "data_throughput=" + StatsUtils.subStatToString(getDataThroughput()),
                "packet_throughput=" + StatsUtils.subStatToString(getPacketThroughput()),
                "byte_loss=" + StatsUtils.subStatToString(getByteLoss()),
                "byte_loss_latest=" + getByteLoss().value().latestToString(),
                "packet_loss=" + StatsUtils.subStatToString(getPacketLoss()),
                "packet_loss_latest=" + getPacketLoss().value().latestToString(),
                "last_round_duration=" + lastRoundDuration,
                "num_updates=" + numUpdates);
        }

        /**
         * Returns a summary of these sub-statistics in a prettified format.
         *
         * @return a {@code String}
         */
        public String toPrettyString()
        {
            return StringUtils.joinAllInLines(
                "--------------------- Switch-counter sub-statistics ----------------------------",
                "Data rate (tx)      : " + StatsUtils.subStatToPrettyString(dataTxRate),
                "Data rate (rx)      : "
                                                                                         + StatsUtils
                                                                                             .subRxRateToPrettyString(
                                                                                                 dataRecRate,
                                                                                                 dataTxRate,
                                                                                                 StatValuePrinters::prettyDataRecepRate),
                "Data throughput     : " + StatsUtils.subStatToPrettyString(getDataThroughput()),
                "Byte loss           : " + StatsUtils.subStatToPrettyString(getByteLoss()),
                "Byte loss (latest)  : " + getByteLoss().value().latestToString(),
                "",
                "Packet rate (tx)    : " + StatsUtils.subStatToPrettyString(pktTxRate),
                "Packet rate (rx)    : "
                                                                                        + StatsUtils
                                                                                            .subRxRateToPrettyString(
                                                                                                pktRecRate, pktTxRate,
                                                                                                StatValuePrinters::prettyPacketRecepRate),
                "Packet throughput   : " + StatsUtils.subStatToPrettyString(getPacketThroughput()),
                "Packet loss         : " + StatsUtils.subStatToPrettyString(getPacketLoss()),
                "Packet loss (latest): " + getPacketLoss().value().latestToString(),
                "",
                "Round duration      : " + lastRoundDuration,
                "Number of updates   : " + numUpdates,
                "--------------------------------------------------------------------------------");
        }

        /**
         * A builder for switch-counter sub-statistics.
         */
        @FieldsAreNonnullByDefault
        @ParametersAreNonnullByDefault
        @ReturnValuesAreNonnullByDefault
        public static final class Builder extends AbstractRateStatsBuilder<SwitchCounterSubStats, Builder>
        {
            private TimeLong lastRoundDuration;

            Builder( double thresFactor )
            {
                super(thresFactor);
                this.lastRoundDuration = TimeLong.absent();
            }

            Builder( double thresFactor,
                     Timed<InfoDouble> dataTxRate,
                     Timed<InfoDouble> dataRecRate,
                     Timed<MetricDouble> pktTxRate,
                     Timed<MetricDouble> pktRecRate,
                     TimeLong lastRoundDuration,
                     long numUpdates )
            {
                super(thresFactor, dataTxRate, dataRecRate, pktTxRate, pktRecRate, numUpdates);
                this.lastRoundDuration = lastRoundDuration;
            }

            /**
             * Sets the last round duration to the provided value.
             * 
             * @param lastRoundDuration
             *            a time duration
             * @return this builder
             */
            public Builder setLastRoundDuration( TimeLong lastRoundDuration )
            {
                this.lastRoundDuration = Objects.requireNonNull(lastRoundDuration);
                return this;
            }

            @Override
            public Builder clear()
            {
                super.clear();
                this.lastRoundDuration = TimeLong.absent();
                return this;
            }

            /**
             * {@inheritDoc}
             * 
             * @return a new {@code SwitchCounterSubStats} instance
             */
            @Override
            public SwitchCounterSubStats build()
            {
                return new SwitchCounterSubStats(
                    getDataTxRateSnapshot(), getDataRecRateSnapshot(),
                    getPacketTxRateSnapshot(), getPacketRecRateSnapshot(),
                    lastRoundDuration,
                    getNumUpdates());
            }
        }
    }

    /**
     * Flow-aware sub-statistics obtained from trajectory sampling of traffic.
     */
    @Immutable
    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class TrajectorySubStats
    {
        private static final TrajectorySubStats ABSENT = newBuilder().build();

        /**
         * Returns a new builder for trajectory sub-statistics.
         * 
         * @return a new builder for {@code TrajectorySubStats} instances
         */
        public static Builder newBuilder()
        {
            return newBuilder(
                TimeSummary.Builder.DEFAULT_WINDOW_SIZE,
                HysteresibleDouble.DEFAULT_THRESHOLD_FACTOR,
                RatioSummary.Builder.DEFAULT_WINDOW_SIZE,
                HysteresibleDouble.DEFAULT_THRESHOLD_FACTOR,
                HysteresibleDouble.DEFAULT_THRESHOLD_FACTOR);
        }

        /**
         * Returns a new builder for trajectory sub-statistics.
         * 
         * @param latWindowSize
         *            The size of the window of the latency moving average
         * @param latThresFactor
         *            The hysteresis threshold factor of the mean latency
         * @param lossWindowSize
         *            The size of the window of the (byte/packet) loss moving
         *            average
         * @param lossThresFactor
         *            The hysteresis threshold factor of the mean (byte/packet)
         *            loss
         * @param rateThresFactor
         *            The hysteresis threshold factor of the (data/packet) rates
         * @return a new builder for {@code TrajectorySubStats} instances
         */
        public static Builder newBuilder( int latWindowSize,
                                          double latThresFactor,
                                          int lossWindowSize,
                                          double lossThresFactor,
                                          double rateThresFactor )
        {
            return new Builder(latWindowSize, latThresFactor, lossWindowSize, lossThresFactor, rateThresFactor);
        }

        /**
         * Returns trajectory sub-statistics that are completely absent.
         * 
         * @return a new {@code TrajectorySubStats} instance with absent values
         */
        public static TrajectorySubStats absent()
        {
            return ABSENT;
        }

        private final Stat<TimeSummary>  latency;
        private final Stat<InfoDouble>   dataThroughput;
        private final Stat<MetricDouble> pktThroughput;
        private final Stat<RatioSummary> byteLoss;
        private final Stat<RatioSummary> pktLoss;
        private final Stat<InfoDouble>   dataTxRate;
        private final Stat<InfoDouble>   dataRecRate;
        private final Stat<MetricDouble> pktTxRate;
        private final Stat<MetricDouble> pktRecRate;
        private final Stat<InfoDouble>   umtchDataTxRate;
        private final Stat<InfoDouble>   umtchDataRecRate;
        private final Stat<MetricDouble> umtchPktTxRate;
        private final Stat<MetricDouble> umtchPktRecRate;
        private final TimeLong           lastRoundDuration;
        private final TimeLong           lastRoundTxDuration;
        private final TimeLong           lastRoundRecDuration;
        private final long               numUpdates;

        private TrajectorySubStats( Timed<TimeSummary> latency,
                                    Timed<InfoDouble> dataThroughput,
                                    Timed<MetricDouble> pktThroughput,
                                    Timed<RatioSummary> byteLoss,
                                    Timed<RatioSummary> pktLoss,
                                    Timed<InfoDouble> dataTxRate,
                                    Timed<InfoDouble> dataRecRate,
                                    Timed<MetricDouble> pktTxRate,
                                    Timed<MetricDouble> pktRecRate,
                                    Timed<InfoDouble> umtchDataTxRate,
                                    Timed<InfoDouble> umtchDataRecRate,
                                    Timed<MetricDouble> umtchPktTxRate,
                                    Timed<MetricDouble> umtchPktRecRate,
                                    TimeLong lastRoundDuration,
                                    TimeLong lastRoundTxDuration,
                                    TimeLong lastRoundRecDuration,
                                    long numUpdates )
        {
            this.latency = StatsUtils.ofTimed(latency, SAFE);
            this.dataThroughput = StatsUtils.ofTimed(dataThroughput, SAFE, StatValuePrinters::dataPerSecond);
            this.pktThroughput = StatsUtils.ofTimed(pktThroughput, SAFE, StatValuePrinters::packetsPerSecond);
            this.byteLoss = StatsUtils.ofTimed(byteLoss, SAFE);
            this.pktLoss = StatsUtils.ofTimed(pktLoss, SAFE);
            this.dataTxRate = StatsUtils.ofTimed(dataTxRate, SAFE, StatValuePrinters::dataPerSecond);
            this.dataRecRate = StatsUtils.ofTimed(dataRecRate, SAFE, StatValuePrinters::dataPerSecond);
            this.pktTxRate = StatsUtils.ofTimed(pktTxRate, SAFE, StatValuePrinters::packetsPerSecond);
            this.pktRecRate = StatsUtils.ofTimed(pktRecRate, SAFE, StatValuePrinters::packetsPerSecond);
            this.umtchDataTxRate = StatsUtils.ofTimed(umtchDataTxRate, SAFE, StatValuePrinters::dataPerSecond);
            this.umtchDataRecRate = StatsUtils.ofTimed(umtchDataRecRate, SAFE, StatValuePrinters::dataPerSecond);
            this.umtchPktTxRate = StatsUtils.ofTimed(umtchPktTxRate, SAFE, StatValuePrinters::packetsPerSecond);
            this.umtchPktRecRate = StatsUtils.ofTimed(umtchPktRecRate, SAFE, StatValuePrinters::packetsPerSecond);
            this.lastRoundDuration = lastRoundDuration;
            this.lastRoundTxDuration = lastRoundTxDuration;
            this.lastRoundRecDuration = lastRoundRecDuration;
            this.numUpdates = numUpdates;
        }

        /**
         * Returns the estimated latency of the flowed-link.
         * <p>
         * This is a {@linkplain StatType#SAFE safe} sub-statistic if its
         * value is present, otherwise it is {@linkplain StatType#UNSAFE
         * unsafe}.
         * 
         * @return a {@code Stat<TimeSummary>} value
         */
        public Stat<TimeSummary> getLatency()
        {
            return latency;
        }

        /**
         * Returns the estimated data rate per second of traffic traversing the
         * flowed-link.
         * <p>
         * This is a {@linkplain StatType#SAFE safe} sub-statistic if its
         * value is present, otherwise it is {@linkplain StatType#UNSAFE
         * unsafe}.
         * 
         * @return a {@code Stat<InfoDouble>} value
         */
        public Stat<InfoDouble> getDataThroughput()
        {
            return dataThroughput;
        }

        /**
         * Returns the estimated packet rate per second of traffic traversing
         * the flowed-link.
         * <p>
         * This is a {@linkplain StatType#SAFE safe} sub-statistic if its
         * value is present, otherwise it is {@linkplain StatType#UNSAFE
         * unsafe}.
         * 
         * @return a {@code Stat<MetricDouble>} value
         */
        public Stat<MetricDouble> getPacketThroughput()
        {
            return pktThroughput;
        }

        /**
         * Returns the estimated byte loss of the flowed-link.
         * <p>
         * This is a {@linkplain StatType#SAFE safe} sub-statistic if its
         * value is present, otherwise it is {@linkplain StatType#UNSAFE
         * unsafe}.
         * 
         * @return a {@code Stat<RatioSummary>} value
         */
        public Stat<RatioSummary> getByteLoss()
        {
            return byteLoss;
        }

        /**
         * Returns the estimated packet loss of the flowed-link.
         * <p>
         * This is a {@linkplain StatType#SAFE safe} sub-statistic if its
         * value is present, otherwise it is {@linkplain StatType#UNSAFE
         * unsafe}.
         * 
         * @return a {@code Stat<RatioSummary>} value
         */
        public Stat<RatioSummary> getPacketLoss()
        {
            return pktLoss;
        }

        /**
         * Returns the estimated data rate per second of traffic exiting the
         * source endpoint of the flowed-link.
         * <p>
         * This is a {@linkplain StatType#SAFE safe} sub-statistic if its
         * value is present, otherwise it is {@linkplain StatType#UNSAFE
         * unsafe}.
         * 
         * @return a {@code Stat<InfoDouble>} value
         */
        public Stat<InfoDouble> getDataTransmissionRate()
        {
            return dataTxRate;
        }

        /**
         * Returns the estimated data rate per second of traffic entering the
         * destination endpoint of the flowed-link.
         * <p>
         * This is a {@linkplain StatType#SAFE safe} sub-statistic if its
         * value is present, otherwise it is {@linkplain StatType#UNSAFE
         * unsafe}.
         * 
         * @return a {@code Stat<InfoDouble>} value
         */
        public Stat<InfoDouble> getDataReceptionRate()
        {
            return dataRecRate;
        }

        /**
         * Returns the estimated packet rate per second of traffic exiting the
         * source endpoint of the flowed-link.
         * <p>
         * This is a {@linkplain StatType#SAFE safe} sub-statistic if its
         * value is present, otherwise it is {@linkplain StatType#UNSAFE
         * unsafe}.
         * 
         * @return a {@code Stat<MetricDouble>} value
         */
        public Stat<MetricDouble> getPacketTransmissionRate()
        {
            return pktTxRate;
        }

        /**
         * Returns the estimated packet rate per second of traffic entering the
         * destination endpoint of the flowed-link.
         * <p>
         * This is a {@linkplain StatType#SAFE safe} sub-statistic if its
         * value is present, otherwise it is {@linkplain StatType#UNSAFE
         * unsafe}.
         * 
         * @return a {@code Stat<MetricDouble>} value
         */
        public Stat<MetricDouble> getPacketReceptionRate()
        {
            return pktRecRate;
        }

        /**
         * Returns the estimated data rate per second of unmatched traffic
         * exiting the source endpoint of the flowed-link.
         * <p>
         * This is a {@linkplain StatType#SAFE safe} sub-statistic if its
         * value is present, otherwise it is {@linkplain StatType#UNSAFE
         * unsafe}.
         * 
         * @return a {@code Stat<InfoDouble>} value
         */
        public Stat<InfoDouble> getUnmatchedDataTransmissionRate()
        {
            return umtchDataTxRate;
        }

        /**
         * Returns the estimated data rate per second of unmatched traffic
         * entering the destination endpoint of the flowed-link.
         * <p>
         * This is a {@linkplain StatType#SAFE safe} sub-statistic if its
         * value is present, otherwise it is {@linkplain StatType#UNSAFE
         * unsafe}.
         * 
         * @return a {@code Stat<InfoDouble>} value
         */
        public Stat<InfoDouble> getUnmatchedDataReceptionRate()
        {
            return umtchDataRecRate;
        }

        /**
         * Returns the estimated packet rate per second of unmatched traffic
         * exiting the source endpoint of the flowed-link.
         * <p>
         * This is a {@linkplain StatType#SAFE safe} sub-statistic if its
         * value is present, otherwise it is {@linkplain StatType#UNSAFE
         * unsafe}.
         * 
         * @return a {@code Stat<MetricDouble>} value
         */
        public Stat<MetricDouble> getUnmatchedPacketTransmissionRate()
        {
            return umtchPktTxRate;
        }

        /**
         * Returns the estimated packet rate per second of unmatched traffic
         * entering the destination endpoint of the flowed-link.
         * <p>
         * This is a {@linkplain StatType#SAFE safe} sub-statistic if its
         * value is present, otherwise it is {@linkplain StatType#UNSAFE
         * unsafe}.
         * 
         * @return a {@code Stat<MetricDouble>} value
         */
        public Stat<MetricDouble> getUnmatchedPacketReceptionRate()
        {
            return umtchPktRecRate;
        }

        /**
         * Returns the duration of the last sampling round.
         * 
         * @return an {@code TimeLong} value
         */
        public TimeLong getLastRoundDuration()
        {
            return lastRoundDuration;
        }

        /**
         * Returns the duration of the flow transmission in the last sampling
         * round.
         * 
         * @return an {@code TimeLong} value
         */
        public TimeLong getLastRoundTxDuration()
        {
            return lastRoundTxDuration;
        }

        /**
         * Returns the duration of the flow reception in the last sampling
         * round.
         * 
         * @return an {@code TimeLong} value
         */
        public TimeLong getLastRoundRxDuration()
        {
            return lastRoundRecDuration;
        }

        /**
         * Returns the number of times these sub-statistics were updated,
         * saturated to {@link Long#MAX_VALUE}.
         * 
         * @return a {@code long} value
         */
        public long getNumUpdates()
        {
            return numUpdates;
        }

        /**
         * Returns a new builder initialized with this instance's values.
         * 
         * @return a builder for {@code TrajectorySubStats} instances
         */
        public Builder createBuilder()
        {
            return createBuilder(
                TimeSummary.Builder.DEFAULT_WINDOW_SIZE,
                HysteresibleDouble.DEFAULT_THRESHOLD_FACTOR,
                RatioSummary.Builder.DEFAULT_WINDOW_SIZE,
                HysteresibleDouble.DEFAULT_THRESHOLD_FACTOR,
                HysteresibleDouble.DEFAULT_THRESHOLD_FACTOR);
        }

        /**
         * Returns a new builder initialized with this instance's values.
         * 
         * @param latWindowSize
         *            The size of the window of the latency moving average
         * @param latThresFactor
         *            The hysteresis threshold factor of the mean latency
         * @param lossWindowSize
         *            The size of the window of the (byte/packet) loss moving
         *            average
         * @param lossThresFactor
         *            The hysteresis threshold factor of the mean (byte/packet)
         *            loss
         * @param rateThresFactor
         *            The hysteresis threshold factor of the (data/packet) rates
         * @return a builder for {@code TrajectorySubStats} instances
         */
        public Builder createBuilder( int latWindowSize,
                                      double latThresFactor,
                                      int lossWindowSize,
                                      double lossThresFactor,
                                      double rateThresFactor )
        {
            return new Builder(
                latWindowSize, latThresFactor, lossWindowSize, lossThresFactor, rateThresFactor,
                latency,
                dataThroughput, pktThroughput,
                byteLoss, pktLoss,
                dataTxRate, dataRecRate,
                pktTxRate, pktRecRate,
                umtchDataTxRate, umtchDataRecRate,
                umtchPktTxRate, umtchPktRecRate,
                lastRoundDuration, lastRoundTxDuration, lastRoundRecDuration,
                numUpdates);
        }

        /**
         * Indicates whether this instance has the same core statistics as the
         * provided instance.
         * <p>
         * The core statistics are:
         * <ul>
         * <li>{@linkplain #getLatency() latency}</li>
         * <li>{@linkplain #getDataThroughput() data throughput}</li>
         * <li>{@linkplain #getPacketThroughput() packet throughput}</li>
         * <li>{@linkplain #getByteLoss() byte loss}</li>
         * <li>{@linkplain #getPacketLoss() packet loss}</li>
         * <li>{@linkplain #getDataTransmissionRate() data transmission
         * rate}</li>
         * <li>{@linkplain #getDataReceptionRate() data reception rate}</li>
         * <li>{@linkplain #getPacketTransmissionRate() packet transmission
         * rate}</li>
         * <li>{@linkplain #getPacketReceptionRate() packet reception rate}</li>
         * <li>{@linkplain #getUnmatchedDataTransmissionRate() unmatched data
         * transmission rate}</li>
         * <li>{@linkplain #getUnmatchedDataReceptionRate() unmatched data
         * reception rate}</li>
         * <li>{@linkplain #getUnmatchedPacketTransmissionRate() unmatched
         * packet transmission rate}</li>
         * <li>{@linkplain #getUnmatchedPacketReceptionRate() unmatched packet
         * reception rate}</li>
         * </ul>
         * 
         * @param other
         *            A {@code TrajectorySubStats} instance
         * @return {@code true} iff this instance has the same core statistics
         *         as the provided instance
         */
        public boolean hasSameCoreStats( TrajectorySubStats other )
        {
            return this.latency.equals(other.latency)
                   && this.dataThroughput.equals(other.dataThroughput)
                   && this.pktThroughput.equals(other.pktThroughput)
                   && this.byteLoss.equals(other.byteLoss)
                   && this.pktLoss.equals(other.pktLoss)
                   && this.dataTxRate.equals(other.dataTxRate)
                   && this.dataRecRate.equals(other.dataRecRate)
                   && this.pktTxRate.equals(other.pktTxRate)
                   && this.pktRecRate.equals(other.pktRecRate)
                   && this.umtchDataTxRate.equals(other.umtchDataTxRate)
                   && this.umtchDataRecRate.equals(other.umtchDataRecRate)
                   && this.umtchPktTxRate.equals(other.umtchPktTxRate)
                   && this.umtchPktRecRate.equals(other.umtchPktRecRate);
        }

        @Override
        public boolean equals( Object other )
        {
            return (other instanceof TrajectorySubStats)
                   && this.equals((TrajectorySubStats)other);
        }

        public boolean equals( TrajectorySubStats other )
        {
            return (other != null)
                   && this.hasSameCoreStats(other)
                   && this.lastRoundDuration.equals(other.lastRoundDuration)
                   && this.lastRoundTxDuration.equals(other.lastRoundTxDuration)
                   && this.lastRoundRecDuration.equals(other.lastRoundRecDuration)
                   && this.numUpdates == other.numUpdates;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(
                latency,
                dataThroughput, pktThroughput,
                byteLoss, pktLoss,
                dataTxRate, dataRecRate,
                pktTxRate, pktRecRate,
                umtchDataTxRate, umtchDataRecRate,
                umtchPktTxRate, umtchPktRecRate,
                lastRoundDuration, lastRoundTxDuration, lastRoundRecDuration,
                numUpdates);
        }

        @Override
        public String toString()
        {
            return StringUtils.joinAllPS(", ", "TrajectorySubStats[", "]",
                "latency=" + StatsUtils.subStatToString(latency),
                "latency_latest=" + latency.value().latestToString(),
                "data_throughput=" + StatsUtils.subStatToString(dataThroughput),
                "packet_throughput=" + StatsUtils.subStatToString(pktThroughput),
                "byte_loss=" + StatsUtils.subStatToString(byteLoss),
                "byte_loss_latest=" + byteLoss.value().latestToString(),
                "packet_loss=" + StatsUtils.subStatToString(pktLoss),
                "packet_loss_latest=" + pktLoss.value().latestToString(),
                "data_tx_rate=" + StatsUtils.subStatToString(dataTxRate),
                "data_rx_rate=" + StatsUtils.subStatToString(dataRecRate),
                "packet_tx_rate=" + StatsUtils.subStatToString(pktTxRate),
                "packet_rx_rate=" + StatsUtils.subStatToString(pktRecRate),
                "unmatched_data_tx_rate=" + StatsUtils.subStatToString(umtchDataTxRate),
                "unmatched_data_rx_rate=" + StatsUtils.subStatToString(umtchDataRecRate),
                "unmatched_packet_tx_rate=" + StatsUtils.subStatToString(umtchPktTxRate),
                "unmatched_packet_rx_rate=" + StatsUtils.subStatToString(umtchPktRecRate),
                "last_round_duration=" + lastRoundDuration,
                "last_round_tx_duration=" + lastRoundTxDuration,
                "last_round_rx_duration=" + lastRoundRecDuration,
                "num_updates=" + numUpdates);
        }

        /**
         * Returns a summary of these sub-statistics in a prettified format.
         *
         * @return a {@code String}
         */
        public String toPrettyString()
        {
            return StringUtils.joinAllInLines(
                "--------------------- Trajectory-based statistics ------------------------------",
                "Latency             : " + StatsUtils.subStatToPrettyString(latency),
                "Latency (latest)    : " + latency.value().latestToString(),
                "",
                "Data throughput     : " + StatsUtils.subStatToPrettyString(dataThroughput),
                "Packet throughput   : " + StatsUtils.subStatToPrettyString(pktThroughput),
                "Byte loss           : " + StatsUtils.subStatToPrettyString(byteLoss),
                "Byte loss (latest)  : " + byteLoss.value().latestToString(),
                "Packet loss         : " + StatsUtils.subStatToPrettyString(pktLoss),
                "Packet loss (latest): " + pktLoss.value().latestToString(),
                "",
                "Data rate (tx)      : " + StatsUtils.subStatToPrettyString(dataTxRate),
                "Data rate (rx)      : "
                                                                                         + StatsUtils
                                                                                             .subRxRateToPrettyString(
                                                                                                 dataRecRate,
                                                                                                 dataTxRate,
                                                                                                 StatValuePrinters::prettyDataRecepRate),
                "Packet rate (tx)    : " + StatsUtils.subStatToPrettyString(pktTxRate),
                "Packet rate (rx)    : "
                                                                                        + StatsUtils
                                                                                            .subRxRateToPrettyString(
                                                                                                pktRecRate, pktTxRate,
                                                                                                StatValuePrinters::prettyPacketRecepRate),
                "",
                "Un. data rate (tx)  : " + StatsUtils.subStatToPrettyString(umtchDataTxRate),
                "Un. data rate (rx)  : "
                                                                                              + StatsUtils
                                                                                                  .subRxRateToPrettyString(
                                                                                                      umtchDataRecRate,
                                                                                                      umtchDataTxRate,
                                                                                                      StatValuePrinters::prettyDataRecepRate),
                "Un. packet rate (tx): " + StatsUtils.subStatToPrettyString(umtchPktTxRate),
                "Un. packet rate (rx): "
                                                                                             + StatsUtils
                                                                                                 .subRxRateToPrettyString(
                                                                                                     umtchPktRecRate,
                                                                                                     umtchPktTxRate,
                                                                                                     StatValuePrinters::prettyPacketRecepRate),
                "",
                "Round duration      : " + lastRoundDuration,
                "Round duration (tx) : " + lastRoundTxDuration,
                "Round duration (rx) : " + lastRoundRecDuration,
                "Number of updates   : " + numUpdates,
                "--------------------------------------------------------------------------------");
        }

        /**
         * A builder for trajectory sub-statistics.
         */
        @FieldsAreNonnullByDefault
        @ParametersAreNonnullByDefault
        @ReturnValuesAreNonnullByDefault
        public static final class Builder extends AbstractLatencyLossRateStatsBuilder<TrajectorySubStats, Builder>
        {
            private HysteresibleDataRate   hystDataThroughput;
            private HysteresiblePacketRate hystPktThroughput;
            private HysteresibleDataRate   hystUmtchDataTxRate;
            private HysteresibleDataRate   hystUmtchDataRecRate;
            private HysteresiblePacketRate hystUmtchPktTxRate;
            private HysteresiblePacketRate hystUmtchPktRecRate;
            private TimeLong               lastRoundDuration;
            private TimeLong               lastRoundTxDuration;
            private TimeLong               lastRoundRecDuration;

            Builder( int latWindowSize,
                     double latThresFactor,
                     int lossWindowSize,
                     double lossThresFactor,
                     double rateThresFactor )
            {
                super(latWindowSize, latThresFactor, lossWindowSize, lossThresFactor, rateThresFactor);
                this.hystDataThroughput = HysteresibleDataRate.of(rateThresFactor);
                this.hystPktThroughput = HysteresiblePacketRate.of(rateThresFactor);
                this.hystUmtchDataTxRate = HysteresibleDataRate.of(rateThresFactor);
                this.hystUmtchDataRecRate = HysteresibleDataRate.of(rateThresFactor);
                this.hystUmtchPktTxRate = HysteresiblePacketRate.of(rateThresFactor);
                this.hystUmtchPktRecRate = HysteresiblePacketRate.of(rateThresFactor);
                this.lastRoundDuration = TimeLong.absent();
                this.lastRoundTxDuration = TimeLong.absent();
                this.lastRoundRecDuration = TimeLong.absent();
            }

            Builder( int latWindowSize,
                     double latThresFactor,
                     int lossWindowSize,
                     double lossThresFactor,
                     double rateThresFactor,
                     Timed<TimeSummary> latency,
                     Timed<InfoDouble> dataThroughput,
                     Timed<MetricDouble> pktThroughput,
                     Timed<RatioSummary> byteLoss,
                     Timed<RatioSummary> pktLoss,
                     Timed<InfoDouble> dataTxRate,
                     Timed<InfoDouble> dataRecRate,
                     Timed<MetricDouble> pktTxRate,
                     Timed<MetricDouble> pktRecRate,
                     Timed<InfoDouble> umtchDataTxRate,
                     Timed<InfoDouble> umtchDataRecRate,
                     Timed<MetricDouble> umtchPktTxRate,
                     Timed<MetricDouble> umtchPktRecRate,
                     TimeLong lastRoundDuration,
                     TimeLong lastRoundTxDuration,
                     TimeLong lastRoundRxDuration,
                     long numUpdates )
            {
                super(latWindowSize, latThresFactor, lossWindowSize, lossThresFactor, rateThresFactor,
                    latency,
                    byteLoss, pktLoss,
                    dataTxRate, dataRecRate,
                    pktTxRate, pktRecRate,
                    numUpdates);
                this.hystDataThroughput = HysteresibleDataRate.of(rateThresFactor, dataThroughput);
                this.hystPktThroughput = HysteresiblePacketRate.of(rateThresFactor, pktThroughput);
                this.hystUmtchDataTxRate = HysteresibleDataRate.of(rateThresFactor, umtchDataTxRate);
                this.hystUmtchDataRecRate = HysteresibleDataRate.of(rateThresFactor, umtchDataRecRate);
                this.hystUmtchPktTxRate = HysteresiblePacketRate.of(rateThresFactor, umtchPktTxRate);
                this.hystUmtchPktRecRate = HysteresiblePacketRate.of(rateThresFactor, umtchPktRecRate);
                this.lastRoundDuration = lastRoundDuration;
                this.lastRoundTxDuration = lastRoundTxDuration;
                this.lastRoundRecDuration = lastRoundRxDuration;
            }

            /**
             * Sets the current data throughput statistic to the provided value
             * and its last-update-time to the {@linkplain Instant#now() current
             * time instant}.
             * 
             * @param dataThroughput
             *            An information amount
             * @return this builder
             */
            public Builder setDataThroughput( InfoDouble dataThroughput )
            {
                return setDataThroughput(dataThroughput, Instant.now());
            }

            /**
             * Sets the current data throughput statistic to the provided value
             * and its last-update-time to the provided timestamp.
             * 
             * @param dataThroughput
             *            An information amount
             * @param timestamp
             *            A time instant
             * @return this builder
             */
            public Builder setDataThroughput( InfoDouble dataThroughput, Instant timestamp )
            {
                hystDataThroughput.add(dataThroughput, timestamp);
                return this;
            }

            /**
             * Sets the current packet throughput statistic to the provided
             * value and its last-update-time to the {@linkplain Instant#now()
             * current time instant}.
             * 
             * @param pktThroughput
             *            A metric amount
             * @return this builder
             */
            public Builder setPacketThroughput( MetricDouble pktThroughput )
            {
                return setPacketThroughput(pktThroughput, Instant.now());
            }

            /**
             * Sets the current packet throughput statistic to the provided
             * value and its last-update-time to the provided timestamp.
             * 
             * @param pktThroughput
             *            A metric amount
             * @param timestamp
             *            A time instant
             * @return this builder
             */
            public Builder setPacketThroughput( MetricDouble pktThroughput, Instant timestamp )
            {
                hystPktThroughput.add(pktThroughput, timestamp);
                return this;
            }

            /**
             * Sets the current unmatched data transmission rate statistic to
             * the provided value and its last-update-time to the
             * {@linkplain Instant#now() current time instant}.
             * 
             * @param umtchDataTxRate
             *            An information amount
             * @return this builder
             */
            public Builder setUnmatchedDataTransmissionRate( InfoDouble umtchDataTxRate )
            {
                return setUnmatchedDataTransmissionRate(umtchDataTxRate, Instant.now());
            }

            /**
             * Sets the current unmatched data transmission rate statistic to
             * the provided value and its last-update-time to the provided
             * timestamp.
             * 
             * @param umtchDataTxRate
             *            An information amount
             * @param timestamp
             *            A time instant
             * @return this builder
             */
            public Builder setUnmatchedDataTransmissionRate( InfoDouble umtchDataTxRate, Instant timestamp )
            {
                hystUmtchDataTxRate.add(umtchDataTxRate, timestamp);
                return this;
            }

            /**
             * Sets the current unmatched data reception rate statistic to the
             * provided value and its last-update-time to the
             * {@linkplain Instant#now() current time instant}.
             * 
             * @param umtchDataRecRate
             *            An information amount
             * @return this builder
             */
            public Builder setUnmatchedDataReceptionRate( InfoDouble umtchDataRecRate )
            {
                return setUnmatchedDataReceptionRate(umtchDataRecRate, Instant.now());
            }

            /**
             * Sets the current unmatched data reception rate statistic to the
             * provided value and its last-update-time to the provided
             * timestamp.
             * 
             * @param umtchDataRecRate
             *            An information amount
             * @param timestamp
             *            A time instant
             * @return this builder
             */
            public Builder setUnmatchedDataReceptionRate( InfoDouble umtchDataRecRate, Instant timestamp )
            {
                hystUmtchDataRecRate.add(umtchDataRecRate, timestamp);
                return this;
            }

            /**
             * Sets the current unmatched packet transmission rate statistic to
             * the provided value and its last-update-time to the
             * {@linkplain Instant#now() current time instant}.
             * 
             * @param umtchPktTxRate
             *            A metric amount
             * @return this builder
             */
            public Builder setUnmatchedPacketTransmissionRate( MetricDouble umtchPktTxRate )
            {
                return setUnmatchedPacketTransmissionRate(umtchPktTxRate, Instant.now());
            }

            /**
             * Sets the current unmatched packet transmission rate statistic to
             * the provided value and its last-update-time to the provided
             * timestamp.
             * 
             * @param umtchPktTxRate
             *            A metric amount
             * @param timestamp
             *            A time instant
             * @return this builder
             */
            public Builder setUnmatchedPacketTransmissionRate( MetricDouble umtchPktTxRate, Instant timestamp )
            {
                hystUmtchPktTxRate.add(umtchPktTxRate, timestamp);
                return this;
            }

            /**
             * Sets the current unmatched packet reception rate statistic to the
             * provided value and its last-update-time to the
             * {@linkplain Instant#now() current time instant}.
             * 
             * @param umtchPktRecRate
             *            A metric amount
             * @return this builder
             */
            public Builder setUnmatchedPacketReceptionRate( MetricDouble umtchPktRecRate )
            {
                return setUnmatchedPacketReceptionRate(umtchPktRecRate, Instant.now());
            }

            /**
             * Sets the current unmatched packet reception rate statistic to the
             * provided value and its last-update-time to the provided
             * timestamp.
             * 
             * @param umtchPktRecRate
             *            A metric amount
             * @param timestamp
             *            A time instant
             * @return this builder
             */
            public Builder setUnmatchedPacketReceptionRate( MetricDouble umtchPktRecRate, Instant timestamp )
            {
                hystUmtchPktRecRate.add(umtchPktRecRate, timestamp);
                return this;
            }

            /**
             * Sets the last round duration to the provided value.
             * 
             * @param lastRoundDuration
             *            a time duration
             * @return this builder
             */
            public Builder setLastRoundDuration( TimeLong lastRoundDuration )
            {
                this.lastRoundDuration = Objects.requireNonNull(lastRoundDuration);
                return this;
            }

            /**
             * Sets the duration of the flow transmission in the last sampling
             * round to the provided value.
             * 
             * @param lastRoundTxDuration
             *            a time duration
             * @return this builder
             */
            public Builder setLastRoundTxDuration( TimeLong lastRoundTxDuration )
            {
                this.lastRoundTxDuration = Objects.requireNonNull(lastRoundTxDuration);
                return this;
            }

            /**
             * Sets the duration of the flow reception in the last sampling
             * round to the provided value.
             * 
             * @param lastRoundRxDuration
             *            a time duration
             * @return this builder
             */
            public Builder setLastRoundRxDuration( TimeLong lastRoundRxDuration )
            {
                this.lastRoundRecDuration = Objects.requireNonNull(lastRoundRxDuration);
                return this;
            }

            @Override
            public Builder clear()
            {
                super.clear();
                setDataThroughput(InfoDouble.absent());
                setPacketThroughput(MetricDouble.absent());
                setUnmatchedDataTransmissionRate(InfoDouble.absent());
                setUnmatchedDataReceptionRate(InfoDouble.absent());
                setUnmatchedPacketTransmissionRate(MetricDouble.absent());
                setUnmatchedPacketReceptionRate(MetricDouble.absent());
                this.lastRoundDuration = TimeLong.absent();
                this.lastRoundTxDuration = TimeLong.absent();
                this.lastRoundRecDuration = TimeLong.absent();
                return this;
            }

            /**
             * {@inheritDoc}
             * 
             * @return a new {@code TrajectorySubStats} instance
             */
            @Override
            public TrajectorySubStats build()
            {
                return new TrajectorySubStats(
                    getLatencySnapshot(),
                    hystDataThroughput.get(), hystPktThroughput.get(),
                    getByteLossSnapshot(), getPacketLossSnapshot(),
                    getDataTxRateSnapshot(), getDataRecRateSnapshot(),
                    getPacketTxRateSnapshot(), getPacketRecRateSnapshot(),
                    hystUmtchDataTxRate.get(), hystUmtchDataRecRate.get(),
                    hystUmtchPktTxRate.get(), hystUmtchPktRecRate.get(),
                    lastRoundDuration, lastRoundTxDuration, lastRoundRecDuration,
                    getNumUpdates());
            }
        }
    }
}
