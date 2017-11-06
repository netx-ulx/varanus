package net.varanus.sdncontroller.linkstats.sample;


import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import net.varanus.mirroringprotocol.util.TimedPacketSummary;
import net.varanus.sdncontroller.types.FlowedLink;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.lang.Comparables;
import net.varanus.util.lang.MoreObjects;
import net.varanus.util.time.Timed;


/**
 * 
 */
@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class TrajectorySample implements LinkSampleBase<FlowedLink>
{
    public static TrajectorySample of( FlowedLink link,
                                       Duration collDuration,
                                       Timed<List<TimedPacketSummary>> srcSumms,
                                       Timed<Long> srcUmtchBytes,
                                       Timed<Long> srcUmtchPkts,
                                       Timed<List<TimedPacketSummary>> destSumms,
                                       Timed<Long> destUmtchBytes,
                                       Timed<Long> destUmtchPkts )
    {
        Instant collFinishTime = Comparables.max(srcSumms.timestamp(), destSumms.timestamp());
        return of(link, collDuration, collFinishTime,
            srcSumms.value(), srcUmtchBytes.value(), srcUmtchPkts.value(),
            destSumms.value(), destUmtchBytes.value(), destUmtchPkts.value());
    }

    public static TrajectorySample of( FlowedLink link,
                                       Duration collDuration,
                                       Instant collFinishTime,
                                       List<TimedPacketSummary> srcSumms,
                                       long srcUmtchBytes,
                                       long srcUmtchPkts,
                                       List<TimedPacketSummary> destSumms,
                                       long destUmtchBytes,
                                       long destUmtchPkts )
    {
        MoreObjects.requireNonNull(
            link, "link",
            collDuration, "collDuration",
            srcSumms, "srcSumms",
            destSumms, "destSumms");
        return new TrajectorySample(link, Optional.of(new Results(
            collDuration, collFinishTime,
            srcSumms, srcUmtchBytes, srcUmtchPkts,
            destSumms, destUmtchBytes, destUmtchPkts)));
    }

    public static TrajectorySample noResults( FlowedLink link )
    {
        Objects.requireNonNull(link);
        return new TrajectorySample(link, Optional.empty());
    }

    private final FlowedLink        link;
    private final Optional<Results> results;

    private TrajectorySample( FlowedLink link, Optional<Results> results )
    {
        this.link = link;
        this.results = results;
    }

    @Override
    public FlowedLink getLink()
    {
        return link;
    }

    @Override
    public boolean hasResults()
    {
        return results.isPresent();
    }

    public Duration getCollectDuration()
    {
        return results.orElseThrow(TrajectorySample::noResultsEx).collDuration;
    }

    public Instant getCollectFinishingTime()
    {
        return results.orElseThrow(TrajectorySample::noResultsEx).collFinishTime;
    }

    public List<TimedPacketSummary> getSourceSummaries()
    {
        return results.orElseThrow(TrajectorySample::noResultsEx).srcSumms;
    }

    public long getSourceUnmatchedBytes()
    {
        return results.orElseThrow(TrajectorySample::noResultsEx).srcUmtchBytes;
    }

    public long getSourceUnmatchedPackets()
    {
        return results.orElseThrow(TrajectorySample::noResultsEx).srcUmtchPkts;
    }

    public List<TimedPacketSummary> getDestinationSummaries()
    {
        return results.orElseThrow(TrajectorySample::noResultsEx).destSumms;
    }

    public long getDestinationUnmatchedBytes()
    {
        return results.orElseThrow(TrajectorySample::noResultsEx).destUmtchBytes;
    }

    public long getDestinationUnmatchedPackets()
    {
        return results.orElseThrow(TrajectorySample::noResultsEx).destUmtchPkts;
    }

    private static UnsupportedOperationException noResultsEx()
    {
        return new UnsupportedOperationException("sample has no results");
    }

    @Immutable
    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    private static final class Results
    {
        final Duration                 collDuration;
        final Instant                  collFinishTime;
        final List<TimedPacketSummary> srcSumms;
        final long                     srcUmtchBytes;
        final long                     srcUmtchPkts;
        final List<TimedPacketSummary> destSumms;
        final long                     destUmtchBytes;
        final long                     destUmtchPkts;

        Results( Duration collDuration,
                 Instant collFinishTime,
                 List<TimedPacketSummary> srcSumms,
                 long srcUmtchBytes,
                 long srcUmtchPkts,
                 List<TimedPacketSummary> destSumms,
                 long destUmtchBytes,
                 long destUmtchPkts )
        {
            this.collDuration = collDuration;
            this.collFinishTime = collFinishTime;
            this.srcSumms = srcSumms;
            this.srcUmtchBytes = srcUmtchBytes;
            this.srcUmtchPkts = srcUmtchPkts;
            this.destSumms = destSumms;
            this.destUmtchBytes = destUmtchBytes;
            this.destUmtchPkts = destUmtchPkts;
        }
    }
}
