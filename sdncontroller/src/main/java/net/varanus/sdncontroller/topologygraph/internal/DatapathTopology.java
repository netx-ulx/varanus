package net.varanus.sdncontroller.topologygraph.internal;


import java.util.Optional;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import org.jgrapht.Graphs;
import org.jgrapht.graph.DirectedPseudograph;

import net.varanus.sdncontroller.types.DatapathLink;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.graph.GraphUtils;
import net.varanus.util.openflow.types.NodeId;


/**
 * 
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
final class DatapathTopology extends DirectedPseudograph<NodeId, DatapathLink>
{
    private static final long serialVersionUID = 1L;

    DatapathTopology()
    {
        super(GraphUtils.nullEdgeFactory());
    }

    DirectedPseudograph<NodeId, DatapathLink> getSnapshot()
    {
        DatapathTopology copy = new DatapathTopology();
        Graphs.addGraph(copy, this);
        return copy;
    }

    boolean addNode( NodeId nodeId )
    {
        return addVertex(nodeId);
    }

    boolean containsNode( NodeId nodeId )
    {
        return containsVertex(nodeId);
    }

    Optional<Set<DatapathLink>> removeNode( NodeId nodeId )
    {
        Set<DatapathLink> links = edgesOf(nodeId);
        if (removeVertex(nodeId))
            return Optional.of(links);
        else
            return Optional.empty();
    }

    boolean addLink( DatapathLink link )
    {
        return addEdge(link.getSrcNode(), link.getDestNode(), link);
    }

    boolean containsLink( DatapathLink link )
    {
        return containsEdge(link);
    }

    boolean removeLink( DatapathLink link )
    {
        return removeEdge(link);
    }
}
