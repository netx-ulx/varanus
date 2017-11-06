package net.varanus.sdncontroller.linkstats.sample;


import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import net.varanus.sdncontroller.types.DatapathLink;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.lang.MoreObjects;
import net.varanus.util.time.TimeDouble;
import net.varanus.util.time.Timed;


/**
 * 
 */
@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class LLDPProbingSample implements LinkSampleBase<DatapathLink>
{
    public static LLDPProbingSample of( DatapathLink link, Timed<TimeDouble> latency )
    {
        MoreObjects.requireNonNull(link, "link", latency, "latency");
        return new LLDPProbingSample(link, Optional.of(new Results(latency)));
    }

    public static LLDPProbingSample noResults( DatapathLink link )
    {
        Objects.requireNonNull(link);
        return new LLDPProbingSample(link, Optional.empty());
    }

    private final DatapathLink      link;
    private final Optional<Results> results;

    private LLDPProbingSample( DatapathLink link, Optional<Results> results )
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

    public TimeDouble getLatency()
    {
        return results.orElseThrow(LLDPProbingSample::noResultsEx).latency.value();
    }

    public Instant getLatencyTimestamp()
    {
        return results.orElseThrow(LLDPProbingSample::noResultsEx).latency.timestamp();
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
        final Timed<TimeDouble> latency;

        Results( Timed<TimeDouble> latency )
        {
            this.latency = latency;
        }
    }
}
