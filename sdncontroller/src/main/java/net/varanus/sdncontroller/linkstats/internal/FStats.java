package net.varanus.sdncontroller.linkstats.internal;


import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.sdncontroller.linkstats.FlowedLinkStats;
import net.varanus.sdncontroller.linkstats.FlowedLinkStats.SwitchCounterSubStats;
import net.varanus.sdncontroller.linkstats.FlowedLinkStats.TrajectorySubStats;
import net.varanus.sdncontroller.linkstats.sample.SwitchCounterSample;
import net.varanus.sdncontroller.linkstats.sample.TrajectorySample;
import net.varanus.sdncontroller.types.FlowedLink;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
final class FStats
{
    static FStats newStats( FlowedLink flowedLink,
                            GStats gStats,
                            int trajLatWinSize,
                            int trajLossWinSize,
                            double hystThresFactor )
    {
        return new FStats(
            flowedLink,
            SCSubStats.newStats(),
            TSubStats.newStats(trajLatWinSize, trajLossWinSize, hystThresFactor),
            gStats);
    }

    private final FlowedLink flowedLink;
    private final SCSubStats counterStats;
    private final TSubStats  trajectStats;
    private final GStats     gStats;

    private FStats( FlowedLink flowedLink, SCSubStats counterStats, TSubStats trajectStats, GStats gStats )
    {
        this.flowedLink = flowedLink;
        this.counterStats = counterStats;
        this.trajectStats = trajectStats;
        this.gStats = gStats;
    }

    FlowedLink getLink()
    {
        return flowedLink;
    }

    GStats getGStats()
    {
        return gStats;
    }

    FlowedLinkStats freeze()
    {
        return FlowedLinkStats.of(
            flowedLink,
            counterStats.freeze(),
            trajectStats.freeze(),
            gStats.freeze());
    }

    boolean update( SwitchCounterSample sample )
    {
        return counterStats.update(sample);
    }

    boolean update( TrajectorySample sample )
    {
        return trajectStats.update(sample);
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    private static final class SCSubStats
    {
        static SCSubStats newStats()
        {
            return new SCSubStats(SwitchCounterSubStats.newBuilder());
        }

        private final SwitchCounterSubStats.Builder statsBuilder;
        private volatile SwitchCounterSubStats      latestStats;
        private final Object                        updateLock;

        private SCSubStats( SwitchCounterSubStats.Builder statsBuilder )
        {
            this.statsBuilder = statsBuilder;
            this.latestStats = statsBuilder.build();
            this.updateLock = new Object();
        }

        SwitchCounterSubStats freeze()
        {
            return latestStats;
        }

        boolean update( SwitchCounterSample sample )
        {
            synchronized (updateLock) {
                StatsCalculator.calcStatistics(statsBuilder, sample);

                SwitchCounterSubStats prevStats = this.latestStats;
                SwitchCounterSubStats newStats = statsBuilder.build();
                this.latestStats = newStats;

                return !prevStats.hasSameCoreStats(newStats);
            }
        }
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    private static final class TSubStats
    {
        static TSubStats newStats( int trajLatWinSize, int trajLossWinSize, double hystThresFactor )
        {
            return new TSubStats(TrajectorySubStats.newBuilder(
                trajLatWinSize, hystThresFactor,
                trajLossWinSize, hystThresFactor,
                hystThresFactor));
        }

        private final TrajectorySubStats.Builder statsBuilder;
        private volatile TrajectorySubStats      latestStats;
        private final Object                     updateLock;

        private TSubStats( TrajectorySubStats.Builder statsBuilder )
        {
            this.statsBuilder = statsBuilder;
            this.latestStats = statsBuilder.build();
            this.updateLock = new Object();
        }

        TrajectorySubStats freeze()
        {
            return latestStats;
        }

        boolean update( TrajectorySample sample )
        {
            synchronized (updateLock) {
                StatsCalculator.calcStatistics(statsBuilder, sample);

                TrajectorySubStats prevStats = this.latestStats;
                TrajectorySubStats newStats = statsBuilder.build();
                this.latestStats = newStats;

                return !prevStats.hasSameCoreStats(newStats);
            }
        }
    }
}
