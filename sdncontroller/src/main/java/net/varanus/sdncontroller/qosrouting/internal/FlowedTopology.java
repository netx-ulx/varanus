package net.varanus.sdncontroller.qosrouting.internal;


import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

import javax.annotation.ParametersAreNonnullByDefault;

import org.jgrapht.GraphPath;
import org.jgrapht.alg.AllDirectedPaths;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.alg.KShortestPaths;
import org.jgrapht.graph.DirectedPseudograph;
import org.jgrapht.graph.DirectedWeightedPseudograph;

import com.google.common.base.Preconditions;

import net.varanus.sdncontroller.linkstats.FlowedLinkStats;
import net.varanus.sdncontroller.qosrouting.FlowedRoute;
import net.varanus.sdncontroller.qosrouting.IFlowedConnectionMap;
import net.varanus.sdncontroller.types.DatapathConnection;
import net.varanus.sdncontroller.types.DatapathLink;
import net.varanus.sdncontroller.types.FlowedConnection;
import net.varanus.sdncontroller.types.FlowedLink;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.collect.CollectionUtils;
import net.varanus.util.graph.GraphUtils;
import net.varanus.util.lang.MoreObjects;
import net.varanus.util.openflow.types.Flow;
import net.varanus.util.openflow.types.NodeId;
import net.varanus.util.openflow.types.PortId;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
final class FlowedTopology
{
    private final Flow                               flow;
    private final Map<DatapathLink, FlowedLinkStats> statsMap;

    private DirectedPseudograph<NodeId, DatapathLink> topo;

    FlowedTopology( Flow flow, DirectedPseudograph<NodeId, DatapathLink> topo )
    {
        this.flow = Objects.requireNonNull(flow);
        this.statsMap = new HashMap<>();
        this.topo = Objects.requireNonNull(topo);
    }

    Flow getFlow()
    {
        return flow;
    }

    IFlowedConnectionMap getConnectionMap( DatapathConnection conn )
    {
        DirectedPseudograph<NodeId, DatapathLink> topo = this.topo;

        if (topo.containsVertex(conn.getEntryNodeId()) && topo.containsVertex(conn.getExitNodeId())) {
            return new ConnectionMap(
                conn.flowed(flow),
                GraphUtils.copyGraph(
                    topo,
                    Function.identity(),
                    link -> new StattedLink(link, CollectionUtils.get(statsMap, link, this::defaultStats)),
                    StattedTopology::new));
        }
        else {
            return new EmptyConnectionMap(conn.flowed(flow));
        }
    }

    private FlowedLinkStats defaultStats( DatapathLink link )
    {
        return FlowedLinkStats.absent(FlowedLink.of(link, flow));
    }

    void updateTopology( DirectedPseudograph<NodeId, DatapathLink> topo )
    {
        this.topo = Objects.requireNonNull(topo);
    }

    void updateLinkStatistics( DatapathLink link, FlowedLinkStats stats )
    {
        MoreObjects.requireNonNull(link, "link", stats, "stats");
        statsMap.put(link, stats);
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    private static final class ConnectionMap implements IFlowedConnectionMap
    {
        private final FlowedConnection conn;
        private final StattedTopology  stattedTopo;

        ConnectionMap( FlowedConnection conn, StattedTopology stattedTopo )
        {
            this.conn = conn;
            this.stattedTopo = stattedTopo;
        }

        @Override
        public FlowedConnection getConnection()
        {
            return conn;
        }

        @Override
        public Optional<FlowedRoute> getBestRoute( ToDoubleFunction<FlowedLinkStats> linkWeigher )
        {
            Objects.requireNonNull(linkWeigher);

            DijkstraShortestPath<NodeId, WeightedStattedLink> dsp = new DijkstraShortestPath<>(
                weightedTopology(linkWeigher),
                getSourceNode(),
                getTargetNode());
            GraphPath<NodeId, WeightedStattedLink> bestPath = dsp.getPath();

            if (bestPath == null)
                return Optional.empty();
            else
                return Optional.of(makeRoute(bestPath));
        }

        @Override
        public List<FlowedRoute> getBestRoutes( int maxRoutes, ToDoubleFunction<FlowedLinkStats> linkWeigher )
        {
            Objects.requireNonNull(linkWeigher);

            if (maxRoutes < 1) {
                return Collections.emptyList();
            }
            else {
                KShortestPaths<NodeId, WeightedStattedLink> ksp = new KShortestPaths<>(
                    weightedTopology(linkWeigher),
                    getSourceNode(),
                    maxRoutes);
                List<GraphPath<NodeId, WeightedStattedLink>> bestPaths = ksp.getPaths(getTargetNode());

                return CollectionUtils.toList(bestPaths, this::makeRoute);
            }
        }

        @Override
        public List<FlowedRoute> getAllRoutes( ToDoubleFunction<FlowedLinkStats> linkWeigher )
        {
            // FIXME THIS DOES NOT WORK FOR SOME REASON !!!

            AllDirectedPaths<NodeId, WeightedStattedLink> adp = new AllDirectedPaths<>(weightedTopology(linkWeigher));
            List<GraphPath<NodeId, WeightedStattedLink>> allPaths = adp.getAllPaths(
                getSourceNode(),
                getTargetNode(),
                true,  // simple paths only
                null); // no max-length

            return CollectionUtils.toList(allPaths, this::makeRoute);
        }

        private WeightedTopology weightedTopology( ToDoubleFunction<FlowedLinkStats> linkWeigher )
        {
            return GraphUtils.copyGraph(
                stattedTopo,
                Function.identity(),
                link -> WeightedStattedLink.of(link, linkWeigher),
                WeightedTopology::new);
        }

        private NodeId getSourceNode()
        {
            return conn.getEntryNodeId();
        }

        private NodeId getTargetNode()
        {
            return conn.getExitNodeId();
        }

        private FlowedRoute makeRoute( GraphPath<NodeId, WeightedStattedLink> gPath )
        {
            PortId entryPortId = conn.getEntryPortId();
            PortId exitPortId = conn.getExitPortId();
            Flow flow = conn.getFlow();
            FlowedRoute.Builder builder = FlowedRoute.newBuilder(entryPortId, exitPortId, flow);
            gPath.getEdgeList().forEach(link -> builder.addLinkWithStats(link.getStats(), link.getWeight()));
            return builder.build();
        }
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    private static class StattedLink
    {
        private final DatapathLink    link;
        private final FlowedLinkStats stats;

        StattedLink( DatapathLink link, FlowedLinkStats stats )
        {
            this.link = link;
            this.stats = stats;
        }

        DatapathLink getLink()
        {
            return link;
        }

        FlowedLinkStats getStats()
        {
            return stats;
        }

        @Override
        public final boolean equals( Object other )
        {
            return (other instanceof StattedLink) && this.equals((StattedLink)other);
        }

        public final boolean equals( StattedLink other )
        {
            return (other != null) && this.link.equals(other.link);
        }

        @Override
        public final int hashCode()
        {
            return link.hashCode();
        }
    }

    @ParametersAreNonnullByDefault
    private static final class WeightedStattedLink extends StattedLink
    {
        static WeightedStattedLink of( StattedLink statted, ToDoubleFunction<FlowedLinkStats> linkWeigher )
        {
            DatapathLink link = statted.getLink();
            FlowedLinkStats stats = statted.getStats();
            double weight = linkWeigher.applyAsDouble(stats);
            return new WeightedStattedLink(link, stats, weight);
        }

        private final double weight;

        WeightedStattedLink( DatapathLink link, FlowedLinkStats stats, double weight )
        {
            super(link, stats);
            Preconditions.checkArgument(!Double.isNaN(weight), "NaN weights are not allowed");
            Preconditions.checkArgument(weight >= 0, "negative weights are not allowed");
            this.weight = weight;
        }

        double getWeight()
        {
            return weight;
        }
    }

    private static final class StattedTopology extends DirectedPseudograph<NodeId, StattedLink>
    {
        private static final long serialVersionUID = 1L;

        StattedTopology()
        {
            super(GraphUtils.nullEdgeFactory());
        }
    }

    @ParametersAreNonnullByDefault
    private static final class WeightedTopology extends DirectedWeightedPseudograph<NodeId, WeightedStattedLink>
    {
        private static final long serialVersionUID = 1L;

        WeightedTopology()
        {
            super(GraphUtils.nullEdgeFactory());
        }

        @Override
        public double getEdgeWeight( WeightedStattedLink e )
        {
            return e.getWeight();
        }
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    private static final class EmptyConnectionMap implements IFlowedConnectionMap
    {
        private final FlowedConnection conn;

        EmptyConnectionMap( FlowedConnection conn )
        {
            this.conn = conn;
        }

        @Override
        public FlowedConnection getConnection()
        {
            return conn;
        }

        @Override
        public Optional<FlowedRoute> getBestRoute( ToDoubleFunction<FlowedLinkStats> linkWeighter )
        {
            Objects.requireNonNull(linkWeighter);
            return Optional.empty();
        }

        @Override
        public List<FlowedRoute> getBestRoutes( int maxRoutes, ToDoubleFunction<FlowedLinkStats> linkWeighter )
        {
            Objects.requireNonNull(linkWeighter);
            return Collections.emptyList();
        }

        @Override
        public List<FlowedRoute> getAllRoutes( ToDoubleFunction<FlowedLinkStats> linkWeighter )
        {
            return Collections.emptyList();
        }
    }
}
