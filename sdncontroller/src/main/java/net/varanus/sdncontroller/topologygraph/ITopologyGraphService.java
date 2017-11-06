package net.varanus.sdncontroller.topologygraph;


import javax.annotation.ParametersAreNonnullByDefault;

import org.jgrapht.graph.DirectedPseudograph;

import com.google.common.collect.ImmutableSet;

import net.floodlightcontroller.core.module.IFloodlightService;
import net.varanus.sdncontroller.types.DatapathLink;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.lang.MoreObjects;
import net.varanus.util.openflow.types.BidiNodePorts;
import net.varanus.util.openflow.types.NodeId;
import net.varanus.util.openflow.types.PortId;


/**
 * 
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public interface ITopologyGraphService extends IFloodlightService
{
    public TopologySnapshot getSnapshot();

    public boolean enableBidiLink( BidiNodePorts bidiLink ) throws IllegalStateException;

    public boolean disableBidiLink( BidiNodePorts bidiLink );

    public void suppressLinkDiscovery( PortId portId );

    public void addListener( ITopologyGraphListener listener );

    public void removeListener( ITopologyGraphListener listener );

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static interface TopologySnapshot
    {
        /**
         * The returned graph is a non-simple directed graph in which both graph
         * loops and multiple edges are permitted.
         * 
         * @return a {@code DirectedPseudograph} instance
         */
        public DirectedPseudograph<NodeId, DatapathLink> getGraph();

        public ImmutableSet<BidiNodePorts> getDisabledBidiLinks();

        public static TopologySnapshot of( final DirectedPseudograph<NodeId, DatapathLink> graph,
                                           final ImmutableSet<BidiNodePorts> disabledBidiLinks )
        {
            MoreObjects.requireNonNull(graph, "graph", disabledBidiLinks, "disabledBidiLinks");
            return new TopologySnapshot() {

                @Override
                public DirectedPseudograph<NodeId, DatapathLink> getGraph()
                {
                    return graph;
                }

                @Override
                public ImmutableSet<BidiNodePorts> getDisabledBidiLinks()
                {
                    return disabledBidiLinks;
                }
            };
        }
    }
}
