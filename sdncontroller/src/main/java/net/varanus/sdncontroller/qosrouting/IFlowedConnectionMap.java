package net.varanus.sdncontroller.qosrouting;


import java.util.List;
import java.util.Optional;
import java.util.function.ToDoubleFunction;

import javax.annotation.ParametersAreNonnullByDefault;

import org.jgrapht.WeightedGraph;

import net.varanus.sdncontroller.linkstats.FlowedLinkStats;
import net.varanus.sdncontroller.types.FlowedConnection;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;


/**
 * 
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public interface IFlowedConnectionMap
{
    public FlowedConnection getConnection();

    public Optional<FlowedRoute> getBestRoute( ToDoubleFunction<FlowedLinkStats> linkWeigher );

    public default List<FlowedRoute> getBestRoutes( ToDoubleFunction<FlowedLinkStats> linkWeigher )
    {
        return getBestRoutes(Integer.MAX_VALUE, linkWeigher);
    }

    public List<FlowedRoute> getBestRoutes( int maxRoutes, ToDoubleFunction<FlowedLinkStats> linkWeigher );

    public default List<FlowedRoute> getAllRoutes()
    {
        return getAllRoutes(stats -> WeightedGraph.DEFAULT_EDGE_WEIGHT);
    }

    public List<FlowedRoute> getAllRoutes( ToDoubleFunction<FlowedLinkStats> linkWeigher );
}
