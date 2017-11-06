package net.varanus.sdncontroller.linkstats.sample;


import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import net.varanus.mirroringprotocol.util.TimedPacketSummary;
import net.varanus.sdncontroller.types.DatapathLink;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.functional.Possible;
import net.varanus.util.lang.Comparables;
import net.varanus.util.lang.MoreObjects;
import net.varanus.util.openflow.types.BitMatch;
import net.varanus.util.time.Timed;


/**
 * 
 */
@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class SecureProbingSample implements LinkSampleBase<DatapathLink>
{
    public static SecureProbingSample of( BitMatch bitMatch,
                                          DatapathLink link,
                                          Duration collDuration,
                                          Timed<Possible<TimedPacketSummary>> srcSumm,
                                          Timed<Possible<TimedPacketSummary>> destSumm )
    {
        Instant collFinishTime = Comparables.max(srcSumm.timestamp(), destSumm.timestamp());
        return of(bitMatch, link, collDuration, collFinishTime, srcSumm.value(), destSumm.value());
    }

    public static SecureProbingSample of( BitMatch bitMatch,
                                          DatapathLink link,
                                          Duration collDuration,
                                          Instant collFinishTime,
                                          Possible<TimedPacketSummary> srcSumm,
                                          Possible<TimedPacketSummary> destSumm )
    {
        MoreObjects.requireNonNull(
            bitMatch, "bitMatch",
            link, "link",
            collDuration, "collDuration",
            srcSumm, "srcSumm",
            destSumm, "destSumm");
        return new SecureProbingSample(link, Optional.of(new Results(
            bitMatch, collDuration, collFinishTime, srcSumm, destSumm)));
    }

    public static SecureProbingSample noResults( DatapathLink link )
    {
        Objects.requireNonNull(link);
        return new SecureProbingSample(link, Optional.empty());
    }

    private final DatapathLink      link;
    private final Optional<Results> results;

    private SecureProbingSample( DatapathLink link, Optional<Results> results )
    {
        this.link = link;
        this.results = results;
    }

    @Override
    public DatapathLink getLink()
    {
        return link;
    }

    @Override
    public boolean hasResults()
    {
        return results.isPresent();
    }

    public BitMatch getBitMatch()
    {
        return results.orElseThrow(SecureProbingSample::noResultsEx).bitMatch;
    }

    public Duration getCollectDuration()
    {
        return results.orElseThrow(SecureProbingSample::noResultsEx).collDuration;
    }

    public Instant getCollectFinishingTime()
    {
        return results.orElseThrow(SecureProbingSample::noResultsEx).collFinishTime;
    }

    public Possible<TimedPacketSummary> getSourceSummary()
    {
        return results.orElseThrow(SecureProbingSample::noResultsEx).srcSumm;
    }

    public Possible<TimedPacketSummary> getDestinationSummary()
    {
        return results.orElseThrow(SecureProbingSample::noResultsEx).destSumm;
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
        final BitMatch                     bitMatch;
        final Duration                     collDuration;
        final Instant                      collFinishTime;
        final Possible<TimedPacketSummary> srcSumm;
        final Possible<TimedPacketSummary> destSumm;

        Results( BitMatch bitMatch,
                 Duration collDuration,
                 Instant collFinishTime,
                 Possible<TimedPacketSummary> srcSumm,
                 Possible<TimedPacketSummary> destSumm )
        {
            this.bitMatch = bitMatch;
            this.collDuration = collDuration;
            this.collFinishTime = collFinishTime;
            this.srcSumm = srcSumm;
            this.destSumm = destSumm;
        }
    }
}
