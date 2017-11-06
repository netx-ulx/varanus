package net.varanus.sdncontroller.linkstats.sample;


import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import org.projectfloodlight.openflow.protocol.OFFlowRemoved;
import org.projectfloodlight.openflow.types.VlanVid;

import net.varanus.sdncontroller.types.FlowedLink;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.lang.MoreObjects;
import net.varanus.util.time.Timed;


/**
 * 
 */
@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class SwitchCounterSample implements LinkSampleBase<FlowedLink>
{
    public static SwitchCounterSample of( FlowedLink link,
                                          Timed<OFFlowRemoved> srcResult,
                                          Timed<OFFlowRemoved> destResult,
                                          Duration preSampDuration,
                                          Duration sampDuration,
                                          Duration postSampDuration,
                                          VlanVid sampTag )
    {
        MoreObjects.requireNonNull(
            link, "link",
            srcResult, "srcResult",
            destResult, "destResult",
            preSampDuration, "preSampDuration",
            sampDuration, "sampDuration",
            postSampDuration, "postSampDuration",
            sampTag, "sampTag");
        return new SwitchCounterSample(link, Optional.of(new Results(
            srcResult, destResult, preSampDuration, sampDuration, postSampDuration, sampTag)));
    }

    public static SwitchCounterSample noResults( FlowedLink link )
    {
        Objects.requireNonNull(link);
        return new SwitchCounterSample(link, Optional.empty());
    }

    private final FlowedLink        link;
    private final Optional<Results> results;

    private SwitchCounterSample( FlowedLink link, Optional<Results> results )
    {
        this.link = link;
        this.results = results;
    }

    @Override
    public final FlowedLink getLink()
    {
        return link;
    }

    @Override
    public boolean hasResults()
    {
        return results.isPresent();
    }

    public Timed<OFFlowRemoved> getSourceResult()
    {
        return results.orElseThrow(SwitchCounterSample::noResultsEx).srcResult;
    }

    public Timed<OFFlowRemoved> getDestinationResult()
    {
        return results.orElseThrow(SwitchCounterSample::noResultsEx).destResult;
    }

    public Duration getPreSamplingDuration()
    {
        return results.orElseThrow(SwitchCounterSample::noResultsEx).preSampDuration;
    }

    public Duration getSamplingDuration()
    {
        return results.orElseThrow(SwitchCounterSample::noResultsEx).sampDuration;
    }

    public Duration getPostSamplingDuration()
    {
        return results.orElseThrow(SwitchCounterSample::noResultsEx).postSampDuration;
    }

    public VlanVid getSamplingTag()
    {
        return results.orElseThrow(SwitchCounterSample::noResultsEx).sampTag;
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
        final Timed<OFFlowRemoved> srcResult;
        final Timed<OFFlowRemoved> destResult;
        final Duration             preSampDuration;
        final Duration             sampDuration;
        final Duration             postSampDuration;
        final VlanVid              sampTag;

        Results( Timed<OFFlowRemoved> srcResult,
                 Timed<OFFlowRemoved> destResult,
                 Duration preSampDuration,
                 Duration sampDuration,
                 Duration postSampDuration,
                 VlanVid sampTag )
        {
            this.srcResult = srcResult;
            this.destResult = destResult;
            this.preSampDuration = preSampDuration;
            this.sampDuration = sampDuration;
            this.postSampDuration = postSampDuration;
            this.sampTag = sampTag;
        }
    }
}
