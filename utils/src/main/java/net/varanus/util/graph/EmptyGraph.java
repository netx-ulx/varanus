package net.varanus.util.graph;


import java.util.Collections;
import java.util.Set;

import org.jgrapht.DirectedGraph;
import org.jgrapht.EdgeFactory;
import org.jgrapht.ListenableGraph;
import org.jgrapht.UndirectedGraph;
import org.jgrapht.WeightedGraph;
import org.jgrapht.event.GraphListener;
import org.jgrapht.event.VertexSetListener;
import org.jgrapht.graph.AbstractGraph;


/**
 * @param <V>
 * @param <E>
 */
final class EmptyGraph<V, E> extends AbstractGraph<V, E>
    implements UndirectedGraph<V, E>, DirectedGraph<V, E>, WeightedGraph<V, E>, ListenableGraph<V, E>
{
    private static final String UNMODIFIABLE = "empty graph is unmodifiable";

    private static final EmptyGraph<?, ?> INSTANCE = new EmptyGraph<>();

    @SuppressWarnings( "unchecked" )
    static <V, E> EmptyGraph<V, E> instance()
    {
        return (EmptyGraph<V, E>)INSTANCE;
    }

    @Override
    public Set<E> getAllEdges( V sourceVertex, V targetVertex )
    {
        return Collections.emptySet();
    }

    @Override
    public E getEdge( V sourceVertex, V targetVertex )
    {
        return null;
    }

    @Override
    public EdgeFactory<V, E> getEdgeFactory()
    {
        return NullEdgeFactory.instance();
    }

    @Override
    public E addEdge( V sourceVertex, V targetVertex )
    {
        throw new UnsupportedOperationException(UNMODIFIABLE);
    }

    @Override
    public boolean addEdge( V sourceVertex, V targetVertex, E e )
    {
        throw new UnsupportedOperationException(UNMODIFIABLE);
    }

    @Override
    public boolean addVertex( V v )
    {
        throw new UnsupportedOperationException(UNMODIFIABLE);
    }

    @Override
    public boolean containsEdge( E e )
    {
        return false;
    }

    @Override
    public boolean containsVertex( V v )
    {
        return false;
    }

    @Override
    public Set<E> edgeSet()
    {
        return Collections.emptySet();
    }

    @Override
    public Set<E> edgesOf( V vertex )
    {
        return Collections.emptySet();
    }

    @Override
    public E removeEdge( V sourceVertex, V targetVertex )
    {
        throw new UnsupportedOperationException(UNMODIFIABLE);
    }

    @Override
    public boolean removeEdge( E e )
    {
        throw new UnsupportedOperationException(UNMODIFIABLE);
    }

    @Override
    public boolean removeVertex( V v )
    {
        throw new UnsupportedOperationException(UNMODIFIABLE);
    }

    @Override
    public Set<V> vertexSet()
    {
        return Collections.emptySet();
    }

    @Override
    public V getEdgeSource( E e )
    {
        return null;
    }

    @Override
    public V getEdgeTarget( E e )
    {
        return null;
    }

    @Override
    public double getEdgeWeight( E e )
    {
        return WeightedGraph.DEFAULT_EDGE_WEIGHT;
    }

    @Override
    public void setEdgeWeight( E e, double weight )
    {
        // do nothing
    }

    @Override
    public int degreeOf( V vertex )
    {
        return 0;
    }

    @Override
    public int inDegreeOf( V vertex )
    {
        return 0;
    }

    @Override
    public Set<E> incomingEdgesOf( V vertex )
    {
        return Collections.emptySet();
    }

    @Override
    public int outDegreeOf( V vertex )
    {
        return 0;
    }

    @Override
    public Set<E> outgoingEdgesOf( V vertex )
    {
        return Collections.emptySet();
    }

    @Override
    public void addGraphListener( GraphListener<V, E> l )
    {
        // do nothing
    }

    @Override
    public void addVertexSetListener( VertexSetListener<V> l )
    {
        // do nothing
    }

    @Override
    public void removeGraphListener( GraphListener<V, E> l )
    {
        // do nothing
    }

    @Override
    public void removeVertexSetListener( VertexSetListener<V> l )
    {
        // do nothing
    }
}
