package net.varanus.sdncontroller.linkstats.internal;


import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import net.varanus.sdncontroller.linkstats.FlowedLinkStats;
import net.varanus.sdncontroller.linkstats.GeneralLinkStats;
import net.varanus.sdncontroller.linkstats.GeneralLinkStats.LLDPProbingSubStats;
import net.varanus.sdncontroller.linkstats.GeneralLinkStats.LinkConfigSubStats;
import net.varanus.sdncontroller.linkstats.GeneralLinkStats.SecureProbingSubStats;
import net.varanus.sdncontroller.linkstats.GeneralLinkStats.SwitchesSubStats;
import net.varanus.sdncontroller.linkstats.sample.LLDPProbingSample;
import net.varanus.sdncontroller.linkstats.sample.SecureProbingSample;
import net.varanus.sdncontroller.types.DatapathLink;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.collect.builder.ImmutableListBuilder;
import net.varanus.util.lang.Comparables;
import net.varanus.util.openflow.types.Flow;
import net.varanus.util.time.Timed;
import net.varanus.util.unitvalue.si.InfoDouble;
import net.varanus.util.unitvalue.si.MetricDouble;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
final class GStats
{
    static GStats newStats( DatapathLink link,
                            int lldpLatWinSize,
                            int secProbeLatWinSize,
                            int secProbeLossWinSize,
                            int trajLatWinSize,
                            int trajLossWinSize,
                            int pktDropRateWinSize,
                            double hystThresFactor )
    {
        return new GStats(
            link,
            LCSubStats.newStats(),
            SwiSubStats.newStats(pktDropRateWinSize),
            LPSubStats.newStats(lldpLatWinSize, hystThresFactor),
            SPSubStats.newStats(secProbeLatWinSize, secProbeLossWinSize, hystThresFactor),
            trajLatWinSize, trajLossWinSize, hystThresFactor);
    }

    private final DatapathLink link;
    private final LCSubStats   linkCfgStats;
    private final SwiSubStats  swiStats;
    private final LPSubStats   lldpStats;
    private final SPSubStats   secProbeStats;

    private final int               trajLatWinSize;
    private final int               trajLossWinSize;
    private final double            hystThresFactor;
    private final Map<Flow, FStats> fStatsMap;

    private GStats( DatapathLink link,
                    LCSubStats linkCfgStats,
                    SwiSubStats swiStats,
                    LPSubStats lldpStats,
                    SPSubStats secProbeStats,
                    int trajLatWinSize,
                    int trajLossWinSize,
                    double hystThresFactor )
    {
        this.link = link;
        this.linkCfgStats = linkCfgStats;
        this.swiStats = swiStats;
        this.lldpStats = lldpStats;
        this.secProbeStats = secProbeStats;

        this.trajLatWinSize = trajLatWinSize;
        this.trajLossWinSize = trajLossWinSize;
        this.hystThresFactor = hystThresFactor;
        this.fStatsMap = new LinkedHashMap<>();
    }

    DatapathLink getLink()
    {
        return link;
    }

    GeneralLinkStats freeze()
    {
        return GeneralLinkStats.of(
            link,
            linkCfgStats.freeze(),
            swiStats.freeze(),
            lldpStats.freeze(),
            secProbeStats.freeze());
    }

    boolean updatePhysicalCapacity( Timed<InfoDouble> capacity )
    {
        return linkCfgStats.updatePhysicalCapacity(capacity);
    }

    boolean updateVirtualCapacity( Timed<InfoDouble> capacity ) throws IllegalArgumentException
    {
        return linkCfgStats.updateVirtualCapacity(capacity);
    }

    boolean updateSourcePacketDropRate( Timed<MetricDouble> pktDropRate )
    {
        return swiStats.updateSourcePacketDropRate(pktDropRate);
    }

    boolean updateDestinationPacketDropRate( Timed<MetricDouble> pktDropRate )
    {
        return swiStats.updateDestinationPacketDropRate(pktDropRate);
    }

    boolean update( LLDPProbingSample sample )
    {
        return lldpStats.update(sample);
    }

    boolean update( SecureProbingSample sample )
    {
        return secProbeStats.update(sample);
    }

    Optional<FStats> getFlowed( Flow flow )
    {
        return Optional.ofNullable(fStatsMap.get(flow));
    }

    Collection<FStats> getAllFlowed()
    {
        return Collections.unmodifiableCollection(fStatsMap.values());
    }

    ImmutableList<FlowedLinkStats> getAllFrozenFlowed()
    {
        return collectFlowedStats();
    }

    FStats computeFlowed( Flow flow )
    {
        return fStatsMap.computeIfAbsent(flow, this::createFlowedStats);
    }

    Optional<FStats> removeFlowed( Flow flow )
    {
        return Optional.ofNullable(fStatsMap.remove(flow));
    }

    ImmutableList<FlowedLinkStats> clear()
    {
        ImmutableList<FlowedLinkStats> last = collectFlowedStats();
        fStatsMap.clear();
        return last;
    }

    private FStats createFlowedStats( Flow flow )
    {
        return FStats.newStats(getLink().flowed(flow), this, trajLatWinSize, trajLossWinSize, hystThresFactor);
    }

    private ImmutableList<FlowedLinkStats> collectFlowedStats()
    {
        ImmutableListBuilder<FlowedLinkStats> builder = ImmutableListBuilder.create();
        return builder.addEach(fStatsMap.values().stream()
            .map(FStats::freeze))
            .build();
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    private static final class LCSubStats
    {
        static LCSubStats newStats()
        {
            return new LCSubStats(LinkConfigSubStats.newBuilder());
        }

        private final LinkConfigSubStats.Builder statsBuilder;
        private volatile LinkConfigSubStats      latestStats;
        private final Object                     updateLock;

        private LCSubStats( LinkConfigSubStats.Builder statsBuilder )
        {
            this.statsBuilder = statsBuilder;
            this.latestStats = statsBuilder.build();
            this.updateLock = new Object();
        }

        LinkConfigSubStats freeze()
        {
            return latestStats;
        }

        boolean updatePhysicalCapacity( Timed<InfoDouble> capacity )
        {
            synchronized (updateLock) {
                statsBuilder.setPhyDataCapacity(capacity.value(), capacity.timestamp());

                LinkConfigSubStats prevStats = this.latestStats;
                LinkConfigSubStats newStats = statsBuilder.build();
                this.latestStats = newStats;

                return !prevStats.hasSameCoreStats(newStats);
            }
        }

        boolean updateVirtualCapacity( Timed<InfoDouble> capacity ) throws IllegalArgumentException
        {
            synchronized (updateLock) {
                checkVirtualCapacity(capacity.value());
                statsBuilder.setVirDataCapacity(capacity.value(), capacity.timestamp());

                LinkConfigSubStats prevStats = this.latestStats;
                LinkConfigSubStats newStats = statsBuilder.build();
                this.latestStats = newStats;

                return !prevStats.hasSameCoreStats(newStats);
            }
        }

        // NOTE: call only when holding updateLock
        private void checkVirtualCapacity( InfoDouble virCapacity ) throws IllegalArgumentException
        {
            InfoDouble phyCapacity = this.latestStats.getPhyDataCapacity().value();
            Preconditions.checkArgument(phyCapacity.isPresent(),
                "virtual capacity can only be set when a physical capacity is present");

            if (virCapacity.isPresent()) {
                Preconditions.checkArgument(virCapacity.inBits() != 0, "virtual capacity value cannot be 0 b/s");
                Preconditions.checkArgument(Comparables.aLEb(virCapacity, phyCapacity),
                    "virtual capacity (%s) must not exceed the physical capacity (%s)", virCapacity, phyCapacity);
            }
        }
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    private static final class SwiSubStats
    {
        static SwiSubStats newStats( int pktDropRateWinSize )
        {
            return new SwiSubStats(SwitchesSubStats.newBuilder(pktDropRateWinSize));
        }

        private final SwitchesSubStats.Builder statsBuilder;
        private volatile SwitchesSubStats      latestStats;
        private final Object                   updateLock;

        private SwiSubStats( SwitchesSubStats.Builder statsBuilder )
        {
            this.statsBuilder = statsBuilder;
            this.latestStats = statsBuilder.build();
            this.updateLock = new Object();
        }

        SwitchesSubStats freeze()
        {
            return latestStats;
        }

        boolean updateSourcePacketDropRate( Timed<MetricDouble> pktDropRate )
        {
            synchronized (updateLock) {
                statsBuilder.setSourcePacketDropRate(pktDropRate.value(), pktDropRate.timestamp());

                SwitchesSubStats prevStats = this.latestStats;
                SwitchesSubStats newStats = statsBuilder.build();
                this.latestStats = newStats;

                return !prevStats.hasSameCoreStats(newStats);
            }
        }

        boolean updateDestinationPacketDropRate( Timed<MetricDouble> pktDropRate )
        {
            synchronized (updateLock) {
                statsBuilder.setDestinationPacketDropRate(pktDropRate.value(), pktDropRate.timestamp());

                SwitchesSubStats prevStats = this.latestStats;
                SwitchesSubStats newStats = statsBuilder.build();
                this.latestStats = newStats;

                return !prevStats.hasSameCoreStats(newStats);
            }
        }
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    private static final class LPSubStats
    {
        static LPSubStats newStats( int latWinSize, double hystThresFactor )
        {
            return new LPSubStats(LLDPProbingSubStats.newBuilder(latWinSize, hystThresFactor));
        }

        private final LLDPProbingSubStats.Builder statsBuilder;
        private volatile LLDPProbingSubStats      latestStats;
        private final Object                      updateLock;

        private LPSubStats( LLDPProbingSubStats.Builder statsBuilder )
        {
            this.statsBuilder = statsBuilder;
            this.latestStats = statsBuilder.build();
            this.updateLock = new Object();
        }

        LLDPProbingSubStats freeze()
        {
            return latestStats;
        }

        boolean update( LLDPProbingSample sample )
        {
            synchronized (updateLock) {
                StatsCalculator.calcStatistics(statsBuilder, sample);

                LLDPProbingSubStats prevStats = this.latestStats;
                LLDPProbingSubStats newStats = statsBuilder.build();
                this.latestStats = newStats;

                return !prevStats.hasSameCoreStats(newStats);
            }
        }
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    private static final class SPSubStats
    {
        static SPSubStats newStats( int latWinSize, int lossWinSize, double hystThresFactor )
        {
            return new SPSubStats(
                SecureProbingSubStats.newBuilder(latWinSize, hystThresFactor, lossWinSize, hystThresFactor));
        }

        private final SecureProbingSubStats.Builder statsBuilder;
        private volatile SecureProbingSubStats      latestStats;
        private final Object                        updateLock;

        private SPSubStats( SecureProbingSubStats.Builder statsBuilder )
        {
            this.statsBuilder = statsBuilder;
            this.latestStats = statsBuilder.build();
            this.updateLock = new Object();
        }

        SecureProbingSubStats freeze()
        {
            return latestStats;
        }

        boolean update( SecureProbingSample sample )
        {
            synchronized (updateLock) {
                StatsCalculator.calcStatistics(statsBuilder, sample);

                SecureProbingSubStats prevStats = this.latestStats;
                SecureProbingSubStats newStats = statsBuilder.build();
                this.latestStats = newStats;

                return !prevStats.hasSameCoreStats(newStats);
            }
        }
    }
}
