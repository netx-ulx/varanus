package net.varanus.util.collect;


import java.util.Collection;
import java.util.LinkedList;


/**
 * A fixed length queue that evicts the oldest element when trying to add past
 * the fixed limit.
 * 
 * @param <E>
 */
public class LimitedQueue<E> extends LinkedList<E>
{
    private static final long serialVersionUID = 7331072201706998872L;

    private final int limit;

    public LimitedQueue( int limit )
    {
        if (limit < 0) {
            throw new IllegalArgumentException("limit must be non-negative");
        }

        this.limit = limit;
    }

    /**
     * Constructs a new limited queue with the elements from the provided
     * collection initially inserted, and the limit
     * set to the size of the collection.
     * 
     * @param col
     */
    public LimitedQueue( Collection<E> col )
    {
        this(col.size());
        addAll(0, col);
    }

    public int getLimit()
    {
        return limit;
    }

    @Override
    public boolean add( E o )
    {
        boolean added = super.add(o);
        while (added && size() > limit) {
            super.remove();
        }
        return added;
    }

    @Override
    public boolean addAll( int index, Collection<? extends E> c )
    {
        boolean added = super.addAll(index, c);
        while (added && size() > limit) {
            super.remove();
        }
        return added;
    }
}
