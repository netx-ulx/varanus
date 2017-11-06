package net.varanus.sdncontroller.monitoring.submodules.collectorhandler;


import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import javax.annotation.ParametersAreNonnullByDefault;

import net.floodlightcontroller.core.module.IFloodlightService;
import net.varanus.sdncontroller.linkstats.sample.SecureProbingSample;
import net.varanus.sdncontroller.linkstats.sample.TrajectorySample;
import net.varanus.sdncontroller.types.DatapathLink;
import net.varanus.sdncontroller.types.FlowedLink;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.openflow.types.BitMatch;
import net.varanus.util.openflow.types.NodeId;
import net.varanus.util.openflow.types.PortId;


/**
 * 
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public interface ICollectorHandlerService extends IFloodlightService
{
    public boolean hasCollector( NodeId nodeId );

    public default boolean hasNecessaryCollectors( DatapathLink link )
    {
        return hasCollector(link.getSrcNode()) && hasCollector(link.getDestNode());
    }

    public default boolean hasNecessaryCollectors( FlowedLink flowedLink )
    {
        return hasNecessaryCollectors(flowedLink.unflowed());
    }

    public CompletableFuture<TrajectorySample> sendSamplingRequest( FlowedLink flowedLink, Duration collDuration );

    public CompletableFuture<SecureProbingSample> sendProbingRequest( BitMatch bitMatch,
                                                                      DatapathLink link,
                                                                      Duration collDuration );

    public PortId getSamplingPort();
}
