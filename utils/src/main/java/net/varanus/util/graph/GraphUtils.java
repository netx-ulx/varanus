package net.varanus.util.graph;


import java.util.function.Function;
import java.util.function.Supplier;

import org.jgrapht.DirectedGraph;
import org.jgrapht.EdgeFactory;
import org.jgrapht.Graph;
import org.jgrapht.ListenableGraph;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.WeightedGraph;


/**
 * 
 */
public final class GraphUtils
{
    public static <V, E, G extends Graph<? super V, ? super E>> G copyGraph( Graph<V, E> src, Supplier<G> graphFactory )
    {
        return copyGraph(src, Function.identity(), Function.identity(), graphFactory);
    }

    public static <V0, E0, V, E, G extends Graph<? super V, ? super E>> G copyGraph( Graph<V0, E0> src,
                                                                                     Function<? super V0,
                                                                                              ? extends V> vertexMapper,
                                                                                     Function<? super E0,
                                                                                              ? extends E> edgeMapper,
                                                                                     Supplier<G> graphFactory )
    {
        G dest = graphFactory.get();
        src.vertexSet().stream().map(vertexMapper).forEachOrdered(dest::addVertex);
        src.edgeSet().forEach(e -> {
            V sv = vertexMapper.apply(src.getEdgeSource(e));
            V tv = vertexMapper.apply(src.getEdgeTarget(e));
            dest.addEdge(sv, tv, edgeMapper.apply(e));
        });
        return dest;
    }

    public static <V, E> Graph<V, E> emptyGraph()
    {
        return EmptyGraph.instance();
    }

    public static <V, E> UndirectedGraph<V, E> emptyUndirectedGraph()
    {
        return EmptyGraph.instance();
    }

    public static <V, E> DirectedGraph<V, E> emptyDirectedGraph()
    {
        return EmptyGraph.instance();
    }

    public static <V, E> WeightedGraph<V, E> emptyWeightedGraph()
    {
        return EmptyGraph.instance();
    }

    public static <V, E> ListenableGraph<V, E> emptyListenableGraph()
    {
        return EmptyGraph.instance();
    }

    public static <V, E> EdgeFactory<V, E> nullEdgeFactory()
    {
        return NullEdgeFactory.instance();
    }

    private GraphUtils()
    {
        // not used
    }
}
