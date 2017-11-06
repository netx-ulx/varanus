package net.varanus.util.graph;


import org.jgrapht.EdgeFactory;


/**
 * @param <V>
 * @param <E>
 */
final class NullEdgeFactory<V, E> implements EdgeFactory<V, E>
{
    private static final NullEdgeFactory<?, ?> INSTANCE = new NullEdgeFactory<>();

    @SuppressWarnings( "unchecked" )
    static <V, E> NullEdgeFactory<V, E> instance()
    {
        return (NullEdgeFactory<V, E>)INSTANCE;
    }

    private NullEdgeFactory()
    {
        // private use only
    }

    @Override
    public E createEdge( V sourceVertex, V targetVertex )
    {
        return null;
    }
}
