package net.varanus.sdncontroller.linkstats;


import static net.varanus.sdncontroller.util.stats.StatType.SAFE;
import static net.varanus.sdncontroller.util.stats.StatType.UNSAFE;

import java.time.Instant;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import net.varanus.sdncontroller.linkstats.StatsBuilders.AbstractBaseStatsBuilder;
import net.varanus.sdncontroller.linkstats.StatsBuilders.AbstractLatencyLossStatsBuilder;
import net.varanus.sdncontroller.linkstats.StatsBuilders.AbstractLatencyStatsBuilder;
import net.varanus.sdncontroller.linkstats.StatsBuilders.MetricRateBuilder;
import net.varanus.sdncontroller.types.DatapathLink;
import net.varanus.sdncontroller.util.MetricSummary;
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
import net.varanus.util.time.Timed;
import net.varanus.util.unitvalue.si.InfoDouble;
import net.varanus.util.unitvalue.si.InfoLongUnit;
import net.varanus.util.unitvalue.si.MetricDouble;


/**
 * Flow-agnostic network statistics for a {@link DatapathLink}.
 */
@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class GeneralLinkStats
{
    /**
     * Returns new general link statistics.
     * 
     * @param link
     *            The datapath-link associated to the statistics
     * @param linkCfgSubStats
     *            Link-configuration-based sub-statistics
     * @param switchesSubStats
     *            Switch-based sub-statistics
     * @param lldpProbingSubStats
     *            LLDP-probing-based sub-statistics
     * @param secProbingSubStats
     *            Secure-probing-based sub-statistics
     * @return a new {@code GeneralLinkStats} instance
     */
    public static GeneralLinkStats of( DatapathLink link,
                                       LinkConfigSubStats linkCfgSubStats,
                                       SwitchesSubStats switchesSubStats,
                                       LLDPProbingSubStats lldpProbingSubStats,
                                       SecureProbingSubStats secProbingSubStats )
    {
        MoreObjects.requireNonNull(
            link, "link",
            linkCfgSubStats, "linkCfgSubStats",
            switchesSubStats, "switchesSubStats",
            lldpProbingSubStats, "lldpProbingSubStats",
            secProbingSubStats, "secProbingSubStats");
        return new GeneralLinkStats(link, linkCfgSubStats, switchesSubStats, lldpProbingSubStats, secProbingSubStats);
    }

    /**
     * Returns general link statistics that are completely absent.
     * 
     * @param link
     *            The datapath-link associated to the statistics
     * @return a new {@code GeneralLinkStats} instance with absent values
     */
    public static GeneralLinkStats absent( DatapathLink link )
    {
        Objects.requireNonNull(link);
        return new GeneralLinkStats(link,
            LinkConfigSubStats.absent(),
            SwitchesSubStats.absent(),
            LLDPProbingSubStats.absent(),
            SecureProbingSubStats.absent());
    }

    private final DatapathLink          link;
    private final LinkConfigSubStats    linkCfg;
    private final SwitchesSubStats      switches;
    private final LLDPProbingSubStats   lldpProbing;
    private final SecureProbingSubStats secProbing;

    private GeneralLinkStats( DatapathLink link,
                              LinkConfigSubStats linkCfg,
                              SwitchesSubStats switches,
                              LLDPProbingSubStats lldpProbing,
                              SecureProbingSubStats secProbing )
    {
        this.link = link;
        this.linkCfg = linkCfg;
        this.switches = switches;
        this.lldpProbing = lldpProbing;
        this.secProbing = secProbing;
    }

    /**
     * Returns the datapath-link associated to these statistics.
     * 
     * @return a {@code DatapathLink} instance
     */
    public DatapathLink getLink()
    {
        return link;
    }

    /**
     * Returns the data capacity (at the source) of the datapath-link. More
     * specifically, the {@linkplain LinkConfigSubStats#getVirDataCapacity()
     * virtual} capacity if it is present, or the
     * {@linkplain LinkConfigSubStats#getPhyDataCapacity() physical} capacity
     * otherwise.
     * <p>
     * The returned statistic is {@linkplain StatType#SAFE safe} only if its
     * value is present, otherwise it is {@linkplain StatType#UNSAFE unsafe}.
     * 
     * @return a {@code Stat<InfoDouble>} value
     */
    public Stat<InfoDouble> getDataCapacity()
    {
        if (linkCfg.getVirDataCapacity().value().isPresent())
            return linkCfg.getVirDataCapacity();
        else
            return linkCfg.getPhyDataCapacity();
    }

    /**
     * Returns the estimated packet drop rate at the source switch of the
     * datapath-link.
     * <p>
     * The returned statistic is {@linkplain StatType#SAFE safe} only if its
     * value is present, otherwise it is {@linkplain StatType#UNSAFE unsafe}.
     * 
     * @return a {@code Stat<MetricSummary>} value
     */
    public Stat<MetricSummary> getSourcePacketDropRate()
    {
        return switches.getSourcePacketDropRate();
    }

    /**
     * Returns the estimated packet drop rate at the destination switch of the
     * datapath-link.
     * <p>
     * The returned statistic is {@linkplain StatType#SAFE safe} only if its
     * value is present, otherwise it is {@linkplain StatType#UNSAFE unsafe}.
     * 
     * @return a {@code Stat<MetricSummary>} value
     */
    public Stat<MetricSummary> getDestinationPacketDropRate()
    {
        return switches.getDestinationPacketDropRate();
    }

    /**
     * Returns the positive difference (>= 0) between the data capacity and the
     * provided used bandwidth statistic.
     * <p>
     * The returned statistic is {@linkplain StatType#SAFE safe} only if
     * both the provided used bandwidth and data capacity are secure; otherwise
     * it is {@linkplain StatType#UNSAFE unsafe}.
     * <p>
     * The time-stamp for the returned statistic is the largest of the
     * time-stamps for the provided used bandwidth and data capacity.
     * 
     * @param usedBandwidth
     *            Statistic value for an amount of used bandwidth
     * @return a {@code Stat<InfoDouble>} value
     */
    public Stat<InfoDouble> getAvailableBandwidth( Stat<InfoDouble> usedBandwidth )
    {
        BinaryOperator<InfoDouble> combiner = ( capa, used ) -> capa.posDiff(used);

        return Stat.newBuilder(getDataCapacity())
            .combineWith(usedBandwidth, combiner)
            .build();
    }

    /**
     * Returns the proportion of the provided used bandwidth statistic relative
     * to the data capacity.
     * <p>
     * The returned statistic is {@linkplain StatType#SAFE safe} only if
     * both the provided used bandwidth and data capacity are secure; otherwise
     * it is {@linkplain StatType#UNSAFE unsafe}.
     * <p>
     * The time-stamp for the returned statistic is the largest of the
     * time-stamps for the provided used bandwidth and data capacity.
     * 
     * @param usedBandwidth
     *            Statistic value for an amount of used bandwidth
     * @return a {@code Stat<Possible<Ratio>} value
     */
    public Stat<Possible<Ratio>> getDataUtilization( Stat<InfoDouble> usedBandwidth )
    {
        BiFunction<InfoDouble, InfoDouble, Possible<Ratio>> mixer =
            ( capa, used ) -> used.asLong().coMap(capa.asLong(), InfoLongUnit.BITS,
                ( usedBits, capaBits ) -> Ratio.of(usedBits, capaBits));

        return Stat.newBuilder(getDataCapacity())
            .mixWith(usedBandwidth, mixer, StatValuePrinters::ratio)
            .build();
    }

    /**
     * Returns the estimated latency of the datapath-link.
     * <p>
     * The returned statistic is {@linkplain StatType#SAFE safe} only if its
     * value is present and it was obtained from a secure probing mechanism;
     * otherwise it is {@linkplain StatType#UNSAFE unsafe}.
     * 
     * @return a {@code Stat<TimeSummary>} value
     */
    public Stat<TimeSummary> getLatency()
    {
        if (secProbing.getLatency().value().isPresent())
            return secProbing.getLatency();
        else
            return lldpProbing.getLatency();
    }

    /**
     * Returns the estimated byte loss of the datapath-link.
     * <p>
     * The returned statistic is {@linkplain StatType#SAFE safe} only if its
     * value is present, otherwise it is {@linkplain StatType#UNSAFE unsafe}.
     * 
     * @return a {@code Stat<RatioSummary>} value
     */
    public Stat<RatioSummary> getByteLoss()
    {
        return secProbing.getByteLoss();
    }

    /**
     * Returns the estimated packet loss of the datapath-link.
     * <p>
     * The returned statistic is {@linkplain StatType#SAFE safe} only if its
     * value is present, otherwise it is {@linkplain StatType#UNSAFE unsafe}.
     * 
     * @return a {@code Stat<RatioSummary>} value
     */
    public Stat<RatioSummary> getPacketLoss()
    {
        return secProbing.getPacketLoss();
    }

    /**
     * Returns the total number of times these statistics were updated.
     * <p>
     * More specifically, this method returns the sum of the number of updates
     * of each sub-statistics, saturated to {@link Long#MAX_VALUE}.
     * 
     * @return a {@code long} value
     */
    public long getTotalUpdates()
    {
        long total = 0;
        total = ExtraMath.addSaturated(total, linkCfg.getNumUpdates());
        total = ExtraMath.addSaturated(total, switches.getNumUpdates());
        total = ExtraMath.addSaturated(total, lldpProbing.getNumUpdates());
        total = ExtraMath.addSaturated(total, secProbing.getNumUpdates());
        return total;
    }

    /**
     * Returns sub-statistics obtained from a datapath-link configuration.
     * 
     * @return a {@code LinkConfigSubStats} object
     */
    public LinkConfigSubStats linkConfig()
    {
        return linkCfg;
    }

    /**
     * Returns sub-statistics specific to each switch of the datapath-link.
     * 
     * @return a {@code SwitchesSubStats} object
     */
    public SwitchesSubStats switches()
    {
        return switches;
    }

    /**
     * Returns sub-statistics obtained from an LLDP-probing mechanism.
     * 
     * @return an {@code LLDPProbingSubStats} object
     */
    public LLDPProbingSubStats lldpProbing()
    {
        return lldpProbing;
    }

    /**
     * Returns sub-statistics obtained from a secure-probing mechanism.
     * 
     * @return a {@code SecureProbingSubStats} object
     */
    public SecureProbingSubStats secureProbing()
    {
        return secProbing;
    }

    /**
     * Indicates whether this instance has the same core statistics as the
     * provided instance.
     * <p>
     * The core statistics are the core statistics of every sub-statistics.
     * 
     * @param other
     *            A {@code GeneralLinkStats} instance
     * @return {@code true} iff this instance has the same core statistics as
     *         the provided instance
     */
    public boolean hasSameCoreStats( GeneralLinkStats other )
    {
        return this.linkCfg.hasSameCoreStats(other.linkCfg)
               && this.switches.hasSameCoreStats(other.switches)
               && this.lldpProbing.hasSameCoreStats(other.lldpProbing)
               && this.secProbing.hasSameCoreStats(other.secProbing);

    }

    @Override
    public boolean equals( Object other )
    {
        return (other instanceof GeneralLinkStats)
               && this.equals((GeneralLinkStats)other);
    }

    public boolean equals( GeneralLinkStats other )
    {
        return (other != null)
               && this.link.equals(other.link)
               && this.linkCfg.equals(other.linkCfg)
               && this.switches.equals(other.switches)
               && this.lldpProbing.equals(other.lldpProbing)
               && this.secProbing.equals(other.secProbing);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(link, linkCfg, switches, lldpProbing, secProbing);
    }

    @Override
    public String toString()
    {
        return StringUtils.joinAllPS(", ", "GeneralLinkStats{", "}",
            "link=" + link,
            "data_capacity=" + StatsUtils.mainStatToString(getDataCapacity()),
            "src_packet_drop_rate=" + StatsUtils.mainStatToString(getSourcePacketDropRate()),
            "dest_packet_drop_rate=" + StatsUtils.mainStatToString(getDestinationPacketDropRate()),
            "latency=" + StatsUtils.mainStatToString(getLatency()),
            "latency_latest=" + getLatency().value().latestToString(),
            "byte_loss=" + StatsUtils.mainStatToString(getByteLoss()),
            "byte_loss_latest=" + getByteLoss().value().latestToString(),
            "packet_loss=" + StatsUtils.mainStatToString(getPacketLoss()),
            "packet_loss_latest=" + getPacketLoss().value().latestToString(),
            "total_num_updates=" + getTotalUpdates(),
            linkCfg,
            switches,
            lldpProbing,
            secProbing);
    }

    /**
     * Returns a summary of these statistics in a prettified format.
     *
     * @return a {@code String}
     */
    public String toPrettyString()
    {
        return StringUtils.joinAllInLines(
            "======================= General link statistics ================================",
            "Datapath-link       : " + link,
            "Data capacity       : " + StatsUtils.mainStatToPrettyString(getDataCapacity()),
            "",
            "Src dropped packets : " + StatsUtils.mainStatToPrettyString(getSourcePacketDropRate()),
            "Dest dropped packets: " + StatsUtils.mainStatToPrettyString(getDestinationPacketDropRate()),
            "",
            "Latency             : " + StatsUtils.mainStatToPrettyString(getLatency()),
            "Latency (latest)    : " + getLatency().value().latestToString(),
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
     * Flow-agnostic sub-statistics obtained from a datapath-link configuration.
     */
    @Immutable
    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class LinkConfigSubStats
    {
        private static final LinkConfigSubStats ABSENT = newBuilder().build();

        /**
         * Returns a new builder for link-configuration sub-statistics.
         * 
         * @return a new builder for {@code LinkConfigSubStats} instances
         */
        public static Builder newBuilder()
        {
            return new Builder();
        }

        /**
         * Returns link-configuration sub-statistics that are completely absent.
         * 
         * @return a new {@code LinkConfigSubStats} instance with absent values
         */
        public static LinkConfigSubStats absent()
        {
            return ABSENT;
        }

        private final Stat<InfoDouble> phyDataCapacity;
        private final Stat<InfoDouble> virDataCapacity;
        private final long             numUpdates;

        private LinkConfigSubStats( Timed<InfoDouble> phyDataCapacity,
                                    Timed<InfoDouble> virDataCapacity,
                                    long numUpdates )
        {
            this.phyDataCapacity = StatsUtils.ofTimed(phyDataCapacity, SAFE, StatValuePrinters::dataPerSecond);
            this.virDataCapacity = StatsUtils.ofTimed(virDataCapacity, SAFE, StatValuePrinters::dataPerSecond);
            this.numUpdates = numUpdates;
        }

        /**
         * Returns the physical data capacity (at the source) of the
         * datapath-link. This value represents the "current speed" of the link
         * provided via OpenFlow.
         * <p>
         * This is a {@linkplain StatType#SAFE safe} sub-statistic if its
         * value is present, otherwise it is {@linkplain StatType#UNSAFE
         * unsafe}.
         * 
         * @return a {@code Stat<InfoDouble>} value
         */
        public Stat<InfoDouble> getPhyDataCapacity()
        {
            return phyDataCapacity;
        }

        /**
         * Returns the virtual data capacity (at the source) of the
         * datapath-link. Unlike the {@linkplain #getPhyDataCapacity()
         * physical capacity}, this value may be configured to an arbitrary
         * value at most equal to the physical capacity.
         * <p>
         * This is a {@linkplain StatType#SAFE safe} sub-statistic if its
         * value is present, otherwise it is {@linkplain StatType#UNSAFE
         * unsafe}.
         * 
         * @return a {@code Stat<InfoDouble>} value
         */
        public Stat<InfoDouble> getVirDataCapacity()
        {
            return virDataCapacity;
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
         * @return a builder for {@code LinkConfigSubStats} instances
         */
        public Builder createBuilder()
        {
            return new Builder(phyDataCapacity, virDataCapacity, numUpdates);
        }

        /**
         * Indicates whether this instance has the same core statistics as the
         * provided instance.
         * <p>
         * The core statistics are:
         * <ul>
         * <li>{@linkplain #getPhyDataCapacity() physical data capacity}</li>
         * <li>{@linkplain #getVirDataCapacity() virtual data capacity}</li>
         * </ul>
         * 
         * @param other
         *            A {@code LinkConfigSubStats} instance
         * @return {@code true} iff this instance has the same core statistics
         *         as the provided instance
         */
        public boolean hasSameCoreStats( LinkConfigSubStats other )
        {
            return this.phyDataCapacity.equals(other.phyDataCapacity)
                   && this.virDataCapacity.equals(other.virDataCapacity);

        }

        @Override
        public boolean equals( Object other )
        {
            return (other instanceof LinkConfigSubStats)
                   && this.equals((LinkConfigSubStats)other);
        }

        public boolean equals( LinkConfigSubStats other )
        {
            return (other != null)
                   && this.hasSameCoreStats(other)
                   && this.numUpdates == other.numUpdates;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(phyDataCapacity, virDataCapacity, numUpdates);
        }

        @Override
        public String toString()
        {
            return StringUtils.joinAllPS(", ", "LinkConfigSubStats[", "]",
                "phy_data_capacity=" + StatsUtils.subStatToString(phyDataCapacity),
                "vir_data_capacity=" + StatsUtils.subStatToString(virDataCapacity),
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
                "----------------------- Link-configuration sub-statistics ----------------------",
                "Data capacity (phy) : " + StatsUtils.subStatToPrettyString(phyDataCapacity),
                "Data capacity (vir) : " + StatsUtils.subStatToPrettyString(virDataCapacity),
                "",
                "Number of updates   : " + numUpdates,
                "--------------------------------------------------------------------------------");
        }

        /**
         * A builder for link-configuration sub-statistics.
         */
        @FieldsAreNonnullByDefault
        @ParametersAreNonnullByDefault
        @ReturnValuesAreNonnullByDefault
        public static final class Builder extends AbstractBaseStatsBuilder<LinkConfigSubStats, Builder>
        {
            private Timed<InfoDouble> phyDataCapacity;
            private Timed<InfoDouble> virDataCapacity;

            Builder()
            {
                super(0);
                this.phyDataCapacity = Timed.now(InfoDouble.absent());
                this.virDataCapacity = Timed.now(InfoDouble.absent());
            }

            Builder( Timed<InfoDouble> phyDataCapacity, Timed<InfoDouble> virDataCapacity, long numUpdates )
            {
                super(numUpdates);
                this.phyDataCapacity = phyDataCapacity;
                this.virDataCapacity = virDataCapacity;
            }

            /**
             * Sets the current physical data capacity statistic to the provided
             * value and its last-update-time to the {@linkplain Instant#now()
             * current time instant}.
             * 
             * @param phyDataCapacity
             *            An information amount
             * @return this builder
             */
            public Builder setPhyDataCapacity( InfoDouble phyDataCapacity )
            {
                return setPhyDataCapacity(phyDataCapacity, Instant.now());
            }

            /**
             * Sets the current physical data capacity statistic to the provided
             * value and its last-update-time to the provided timestamp.
             * 
             * @param phyDataCapacity
             *            An information amount
             * @param timestamp
             *            A time instant
             * @return this builder
             */
            public Builder setPhyDataCapacity( InfoDouble phyDataCapacity, Instant timestamp )
            {
                this.phyDataCapacity =
                    Timed.of(Objects.requireNonNull(phyDataCapacity), Objects.requireNonNull(timestamp));
                return this;
            }

            /**
             * Sets the current virtual data capacity statistic to the provided
             * value and its last-update-time to the {@linkplain Instant#now()
             * current time instant}.
             * 
             * @param virDataCapacity
             *            An information amount
             * @return this builder
             */
            public Builder setVirDataCapacity( InfoDouble virDataCapacity )
            {
                return setVirDataCapacity(virDataCapacity, Instant.now());
            }

            /**
             * Sets the current virtual data capacity statistic to the provided
             * value and its last-update-time to the provided timestamp.
             * 
             * @param virDataCapacity
             *            An information amount
             * @param timestamp
             *            A time instant
             * @return this builder
             */
            public Builder setVirDataCapacity( InfoDouble virDataCapacity, Instant timestamp )
            {
                this.virDataCapacity =
                    Timed.of(Objects.requireNonNull(virDataCapacity), Objects.requireNonNull(timestamp));
                return this;
            }

            @Override
            public Builder clear()
            {
                this.phyDataCapacity = Timed.now(InfoDouble.absent());
                this.virDataCapacity = Timed.now(InfoDouble.absent());
                return this;
            }

            /**
             * {@inheritDoc}
             * 
             * @return a new {@code LinkConfigSubStats} instance
             */
            @Override
            public LinkConfigSubStats build()
            {
                return new LinkConfigSubStats(phyDataCapacity, virDataCapacity, getNumUpdates());
            }
        }
    }

    /**
     * Flow-agnostic sub-statistics obtained from the switches in the
     * datapath-link.
     */
    @Immutable
    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class SwitchesSubStats
    {
        private static final SwitchesSubStats ABSENT = newBuilder().build();

        /**
         * Returns a new builder for switch-specific sub-statistics.
         * 
         * @return a new builder for {@code SwitchesSubStats} instances
         */
        public static Builder newBuilder()
        {
            return new Builder(MetricSummary.Builder.DEFAULT_WINDOW_SIZE);
        }

        /**
         * Returns a new builder for switch-specific sub-statistics.
         * 
         * @param rateWindowSize
         *            The size of the window of the packet drop rate moving
         *            averages
         * @return a new builder for {@code SwitchesSubStats} instances
         */
        public static Builder newBuilder( int rateWindowSize )
        {
            return new Builder(rateWindowSize);
        }

        /**
         * Returns switch-specific sub-statistics that are completely absent.
         * 
         * @return a new {@code SwitchesSubStats} instance with absent values
         */
        public static SwitchesSubStats absent()
        {
            return ABSENT;
        }

        private final Stat<MetricSummary> srcPktDropRate;
        private final Stat<MetricSummary> destPktDropRate;
        private final long                numUpdates;

        private SwitchesSubStats( Timed<MetricSummary> srcPktDropRate,
                                  Timed<MetricSummary> destPktDropRate,
                                  long numUpdates )
        {
            this.srcPktDropRate = StatsUtils.ofTimed(srcPktDropRate, SAFE,
                summ -> summ.toString(StatValuePrinters::packetsPerSecond));
            this.destPktDropRate = StatsUtils.ofTimed(destPktDropRate, SAFE,
                summ -> summ.toString(StatValuePrinters::packetsPerSecond));
            this.numUpdates = numUpdates;
        }

        /**
         * Returns the estimated packet drop rate at the source switch of the
         * datapath-link.
         * <p>
         * This is a {@linkplain StatType#SAFE safe} sub-statistic if its
         * value is present, otherwise it is {@linkplain StatType#UNSAFE
         * unsafe}.
         * 
         * @return a {@code Stat<MetricSummary>} value
         */
        public Stat<MetricSummary> getSourcePacketDropRate()
        {
            return srcPktDropRate;
        }

        /**
         * Returns the estimated packet drop rate at the destination switch of
         * the datapath-link.
         * <p>
         * This is a {@linkplain StatType#SAFE safe} sub-statistic if its
         * value is present, otherwise it is {@linkplain StatType#UNSAFE
         * unsafe}.
         * 
         * @return a {@code Stat<MetricSummary>} value
         */
        public Stat<MetricSummary> getDestinationPacketDropRate()
        {
            return destPktDropRate;
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
         * @return a builder for {@code SwitchesSubStats} instances
         */
        public Builder createBuilder()
        {
            return new Builder(
                MetricSummary.Builder.DEFAULT_WINDOW_SIZE,
                srcPktDropRate, destPktDropRate, numUpdates);
        }

        /**
         * Returns a new builder initialized with this instance's values.
         * 
         * @param rateWindowSize
         *            The size of the window of the packet drop rate moving
         *            averages
         * @return a builder for {@code SwitchesSubStats} instances
         */
        public Builder createBuilder( int rateWindowSize )
        {
            return new Builder(
                rateWindowSize,
                srcPktDropRate, destPktDropRate, numUpdates);
        }

        /**
         * Indicates whether this instance has the same core statistics as the
         * provided instance.
         * <p>
         * The core statistics are:
         * <ul>
         * <li>{@linkplain #getSourcePacketDropRate() source packet drop
         * rate}</li>
         * <li>{@linkplain #getDestinationPacketDropRate() destination packet
         * drop rate}</li>
         * </ul>
         * 
         * @param other
         *            A {@code SwitchesSubStats} instance
         * @return {@code true} iff this instance has the same core statistics
         *         as the provided instance
         */
        public boolean hasSameCoreStats( SwitchesSubStats other )
        {
            return this.srcPktDropRate.equals(other.srcPktDropRate)
                   && this.destPktDropRate.equals(other.destPktDropRate);

        }

        @Override
        public boolean equals( Object other )
        {
            return (other instanceof SwitchesSubStats)
                   && this.equals((SwitchesSubStats)other);
        }

        public boolean equals( SwitchesSubStats other )
        {
            return (other != null)
                   && this.hasSameCoreStats(other)
                   && this.numUpdates == other.numUpdates;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(srcPktDropRate, destPktDropRate, numUpdates);
        }

        @Override
        public String toString()
        {
            return StringUtils.joinAllPS(", ", "SwitchesSubStats[", "]",
                "src_packet_drop_rate=" + StatsUtils.subStatToString(srcPktDropRate),
                "dest_packet_drop_rate=" + StatsUtils.subStatToString(destPktDropRate),
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
                "------------------------- Switch-specific sub-statistics ------------------------",
                "Src dropped packets : " + StatsUtils.subStatToPrettyString(srcPktDropRate),
                "Dest dropped packets: " + StatsUtils.subStatToPrettyString(destPktDropRate),
                "",
                "Number of updates   : " + numUpdates,
                "--------------------------------------------------------------------------------");
        }

        /**
         * A builder for switch-specific sub-statistics.
         */
        @FieldsAreNonnullByDefault
        @ParametersAreNonnullByDefault
        @ReturnValuesAreNonnullByDefault
        public static final class Builder extends AbstractBaseStatsBuilder<SwitchesSubStats, Builder>
        {
            private final MetricRateBuilder srcPktDropRateBldr;
            private final MetricRateBuilder destPktDropRateBldr;

            Builder( int rateWindowSize )
            {
                super(0);
                this.srcPktDropRateBldr = MetricRateBuilder.newBuilder(rateWindowSize);
                this.destPktDropRateBldr = MetricRateBuilder.newBuilder(rateWindowSize);
            }

            Builder( int rateWindowSize,
                     Timed<MetricSummary> srcPktDropRate,
                     Timed<MetricSummary> destPktDropRate,
                     long numUpdates )
            {
                super(numUpdates);
                this.srcPktDropRateBldr = MetricRateBuilder.newBuilder(rateWindowSize, srcPktDropRate);
                this.destPktDropRateBldr = MetricRateBuilder.newBuilder(rateWindowSize, destPktDropRate);
            }

            /**
             * Sets the current source packet drop rate statistic to the
             * provided value and its last-update-time to the
             * {@linkplain Instant#now() current time instant}.
             * 
             * @param srcPktDropRate
             *            A metric amount
             * @return this builder
             */
            public Builder setSourcePacketDropRate( MetricDouble srcPktDropRate )
            {
                return setSourcePacketDropRate(srcPktDropRate, Instant.now());
            }

            /**
             * Sets the current source packet drop rate statistic to the
             * provided value and its last-update-time to the provided
             * timestamp.
             * 
             * @param srcPktDropRate
             *            A metric amount
             * @param timestamp
             *            A time instant
             * @return this builder
             */
            public Builder setSourcePacketDropRate( MetricDouble srcPktDropRate, Instant timestamp )
            {
                srcPktDropRateBldr.addValue(Objects.requireNonNull(srcPktDropRate), timestamp);
                return this;
            }

            /**
             * Sets the current destination packet drop rate statistic to the
             * provided value and its last-update-time to the
             * {@linkplain Instant#now() current time instant}.
             * 
             * @param destPktDropRate
             *            A metric amount
             * @return this builder
             */
            public Builder setDestinationPacketDropRate( MetricDouble destPktDropRate )
            {
                return setDestinationPacketDropRate(destPktDropRate, Instant.now());
            }

            /**
             * Sets the current destination packet drop rate statistic to the
             * provided value and its last-update-time to the provided
             * timestamp.
             * 
             * @param destPktDropRate
             *            A metric amount
             * @param timestamp
             *            A time instant
             * @return this builder
             */
            public Builder setDestinationPacketDropRate( MetricDouble destPktDropRate, Instant timestamp )
            {
                destPktDropRateBldr.addValue(Objects.requireNonNull(destPktDropRate), timestamp);
                return this;
            }

            @Override
            public Builder clear()
            {
                srcPktDropRateBldr.reset();
                destPktDropRateBldr.reset();
                return this;
            }

            /**
             * {@inheritDoc}
             * 
             * @return a new {@code SwitchesSubStats} instance
             */
            @Override
            public SwitchesSubStats build()
            {
                return new SwitchesSubStats(srcPktDropRateBldr.build(), destPktDropRateBldr.build(), getNumUpdates());
            }
        }
    }

    /**
     * Flow-agnostic sub-statistics obtained from an LLDP-probing mechanism.
     */
    @Immutable
    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class LLDPProbingSubStats
    {
        private static final LLDPProbingSubStats ABSENT = newBuilder().build();

        /**
         * Returns a new builder for LLDP-probing sub-statistics.
         * 
         * @return a new builder for {@code LLDPProbingSubStats} instances
         */
        public static Builder newBuilder()
        {
            return newBuilder(TimeSummary.Builder.DEFAULT_WINDOW_SIZE, HysteresibleDouble.DEFAULT_THRESHOLD_FACTOR);
        }

        /**
         * Returns a new builder for LLDP-probing sub-statistics.
         * 
         * @param latWindowSize
         *            The size of the window of the latency moving average
         * @param latThresFactor
         *            The hysteresis threshold factor of the mean latency
         * @return a new builder for {@code LLDPProbingSubStats} instances
         */
        public static Builder newBuilder( int latWindowSize, double latThresFactor )
        {
            return new Builder(latWindowSize, latThresFactor);
        }

        /**
         * Returns LLDP-probing sub-statistics that are completely absent.
         * 
         * @return a new {@code LLDPProbingSubStats} instance with absent values
         */
        public static LLDPProbingSubStats absent()
        {
            return ABSENT;
        }

        private final Stat<TimeSummary> latency;
        private final long              numUpdates;

        private LLDPProbingSubStats( Timed<TimeSummary> latency, long numUpdates )
        {
            this.latency = StatsUtils.ofTimed(latency, UNSAFE);
            this.numUpdates = numUpdates;
        }

        /**
         * Returns the estimated latency of the datapath-link.
         * <p>
         * This is an {@linkplain StatType#UNSAFE unsafe} sub-statistic.
         * 
         * @return a {@code Stat<TimeSummary>} instance
         */
        public Stat<TimeSummary> getLatency()
        {
            return latency;
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
         * @return a builder for {@code LLDPProbingSubStats} instances
         */
        public Builder createBuilder()
        {
            return createBuilder(TimeSummary.Builder.DEFAULT_WINDOW_SIZE, HysteresibleDouble.DEFAULT_THRESHOLD_FACTOR);
        }

        /**
         * Returns a new builder initialized with this instance's values.
         * 
         * @param latWindowSize
         *            The size of the window of the latency moving average
         * @param latThresFactor
         *            The hysteresis threshold factor of the mean latency
         * @return a builder for {@code LLDPProbingSubStats} instances
         */
        public Builder createBuilder( int latWindowSize, double latThresFactor )
        {
            return new Builder(latWindowSize, latThresFactor, latency, numUpdates);
        }

        /**
         * Indicates whether this instance has the same core statistics as the
         * provided instance.
         * <p>
         * The core statistics are:
         * <ul>
         * <li>{@linkplain #getLatency() latency}</li>
         * </ul>
         * 
         * @param other
         *            A {@code LLDPProbingSubStats} instance
         * @return {@code true} iff this instance has the same core statistics
         *         as the provided instance
         */
        public boolean hasSameCoreStats( LLDPProbingSubStats other )
        {
            return this.latency.equals(other.latency);

        }

        @Override
        public boolean equals( Object other )
        {
            return (other instanceof LLDPProbingSubStats)
                   && this.equals((LLDPProbingSubStats)other);
        }

        public boolean equals( LLDPProbingSubStats other )
        {
            return (other != null)
                   && this.hasSameCoreStats(other)
                   && this.numUpdates == other.numUpdates;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(latency, numUpdates);
        }

        @Override
        public String toString()
        {
            return StringUtils.joinAllPS(", ", "LLDPProbingSubStats[", "]",
                "latency=" + StatsUtils.subStatToString(latency),
                "latency_latest=" + latency.value().latestToString(),
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
                "----------------------- LLDP-probing sub-statistics ----------------------------",
                "Latency             : " + StatsUtils.subStatToPrettyString(latency),
                "Latency (latest)    : " + latency.value().latestToString(),
                "",
                "Number of updates   : " + numUpdates,
                "--------------------------------------------------------------------------------");
        }

        /**
         * A builder for LLDP-probing sub-statistics.
         */
        @ParametersAreNonnullByDefault
        public static final class Builder extends AbstractLatencyStatsBuilder<LLDPProbingSubStats, Builder>
        {
            Builder( int latWindowSize, double latThresFactor )
            {
                super(latWindowSize, latThresFactor);
            }

            Builder( int latWindowSize, double latThresFactor, Timed<TimeSummary> latency, long numUpdates )
            {
                super(latWindowSize, latThresFactor, latency, numUpdates);
            }

            /**
             * {@inheritDoc}
             * 
             * @return a new {@code LLDPProbingSubStats} instance
             */
            @Override
            public LLDPProbingSubStats build()
            {
                return new LLDPProbingSubStats(getLatencySnapshot(), getNumUpdates());
            }
        }
    }

    /**
     * Flow-agnostic sub-statistics obtained from a secure-probing mechanism.
     */
    @Immutable
    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class SecureProbingSubStats
    {
        private static final SecureProbingSubStats ABSENT = newBuilder().build();

        /**
         * Returns a new builder for secure-probing sub-statistics.
         * 
         * @return a new builder for {@code SecureProbingSubStats} instances
         */
        public static Builder newBuilder()
        {
            return newBuilder(
                TimeSummary.Builder.DEFAULT_WINDOW_SIZE,
                HysteresibleDouble.DEFAULT_THRESHOLD_FACTOR,
                RatioSummary.Builder.DEFAULT_WINDOW_SIZE,
                HysteresibleDouble.DEFAULT_THRESHOLD_FACTOR);
        }

        /**
         * Returns a new builder for secure-probing sub-statistics.
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
         * @return a new builder for {@code SecureProbingSubStats} instances
         */
        public static Builder newBuilder( int latWindowSize,
                                          double latThresFactor,
                                          int lossWindowSize,
                                          double lossThresFactor )
        {
            return new Builder(latWindowSize, latThresFactor, lossWindowSize, lossThresFactor);
        }

        /**
         * Returns secure-probing sub-statistics that are completely absent.
         * 
         * @return a new {@code SecureProbingSubStats} instance with absent
         *         values
         */
        public static SecureProbingSubStats absent()
        {
            return ABSENT;
        }

        private final Stat<TimeSummary>  latency;
        private final Stat<RatioSummary> byteLoss;
        private final Stat<RatioSummary> pktLoss;
        private final long               numUpdates;

        private SecureProbingSubStats( Timed<TimeSummary> latency,
                                       Timed<RatioSummary> byteLoss,
                                       Timed<RatioSummary> pktLoss,
                                       long numUpdates )
        {
            this.latency = StatsUtils.ofTimed(latency, SAFE);
            this.byteLoss = StatsUtils.ofTimed(byteLoss, SAFE);
            this.pktLoss = StatsUtils.ofTimed(pktLoss, SAFE);
            this.numUpdates = numUpdates;
        }

        /**
         * Returns the estimated latency of the datapath-link.
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
         * Returns the estimated byte loss of the datapath-link.
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
         * Returns the estimated packet loss of the datapath-link.
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
         * @return a builder for {@code SecureProbingSubStats} instances
         */
        public Builder createBuilder()
        {
            return createBuilder(
                TimeSummary.Builder.DEFAULT_WINDOW_SIZE,
                HysteresibleDouble.DEFAULT_THRESHOLD_FACTOR,
                RatioSummary.Builder.DEFAULT_WINDOW_SIZE,
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
         * @return a builder for {@code SecureProbingSubStats} instances
         */
        public Builder createBuilder( int latWindowSize,
                                      double latThresFactor,
                                      int lossWindowSize,
                                      double lossThresFactor )
        {
            return new Builder(latWindowSize, latThresFactor, lossWindowSize, lossThresFactor,
                latency, byteLoss, pktLoss,
                numUpdates);
        }

        /**
         * Indicates whether this instance has the same core statistics as the
         * provided instance.
         * <p>
         * The core statistics are:
         * <ul>
         * <li>{@linkplain #getLatency() latency}</li>
         * <li>{@linkplain #getByteLoss() byte loss}</li>
         * <li>{@linkplain #getPacketLoss() packet loss}</li>
         * </ul>
         * 
         * @param other
         *            A {@code SecureProbingSubStats} instance
         * @return {@code true} iff this instance has the same core statistics
         *         as the provided instance
         */
        public boolean hasSameCoreStats( SecureProbingSubStats other )
        {
            return this.latency.equals(other.latency)
                   && this.byteLoss.equals(other.byteLoss)
                   && this.pktLoss.equals(other.pktLoss);

        }

        @Override
        public boolean equals( Object other )
        {
            return (other instanceof SecureProbingSubStats)
                   && this.equals((SecureProbingSubStats)other);
        }

        public boolean equals( SecureProbingSubStats other )
        {
            return (other != null)
                   && this.hasSameCoreStats(other)
                   && this.numUpdates == other.numUpdates;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(latency, byteLoss, pktLoss, numUpdates);
        }

        @Override
        public String toString()
        {
            return StringUtils.joinAllPS(", ", "SecureProbingSubStats[", "]",
                "latency=" + StatsUtils.subStatToString(latency),
                "latency_latest=" + latency.value().latestToString(),
                "byte_loss=" + StatsUtils.subStatToString(byteLoss),
                "byte_loss_latest=" + byteLoss.value().latestToString(),
                "packet_loss=" + StatsUtils.subStatToString(pktLoss),
                "packet_loss_latest=" + pktLoss.value().latestToString(),
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
                "----------------------- Secure-probing sub-statistics --------------------------",
                "Latency             : " + StatsUtils.subStatToPrettyString(latency),
                "Latency (latest)    : " + latency.value().latestToString(),
                "",
                "Byte loss           : " + StatsUtils.subStatToPrettyString(byteLoss),
                "Byte loss (latest)  : " + byteLoss.value().latestToString(),
                "Packet loss         : " + StatsUtils.subStatToPrettyString(pktLoss),
                "Packet loss (latest): " + pktLoss.value().latestToString(),
                "",
                "Number of updates   : " + numUpdates,
                "--------------------------------------------------------------------------------");
        }

        /**
         * A builder for secure-probing sub-statistics.
         */
        @ParametersAreNonnullByDefault
        public static final class Builder extends AbstractLatencyLossStatsBuilder<SecureProbingSubStats, Builder>
        {
            Builder( int latWindowSize, double latThresFactor, int lossWindowSize, double lossThresFactor )
            {
                super(latWindowSize, latThresFactor, lossWindowSize, lossThresFactor);
            }

            Builder( int latWindowSize,
                     double latThresFactor,
                     int lossWindowSize,
                     double lossThresFactor,
                     Timed<TimeSummary> latency,
                     Timed<RatioSummary> byteLoss,
                     Timed<RatioSummary> pktLoss,
                     long numUpdates )
            {
                super(latWindowSize, latThresFactor, lossWindowSize, lossThresFactor,
                    latency, byteLoss, pktLoss,
                    numUpdates);
            }

            /**
             * {@inheritDoc}
             * 
             * @return a new {@code SecureProbingSubStats} instance
             */
            @Override
            public SecureProbingSubStats build()
            {
                return new SecureProbingSubStats(
                    getLatencySnapshot(),
                    getByteLossSnapshot(),
                    getPacketLossSnapshot(),
                    getNumUpdates());
            }
        }
    }
}
