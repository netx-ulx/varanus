package net.varanus.util.openflow.types;


import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;


/**
 * @param <D>
 *            The flowable type
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public interface Directed<D extends Directable<D>>
{
    public D undirected();

    public TrafficDirection getDirection();

    public default boolean contains( D undirected )
    {
        return undirected.equals(this.undirected());
    }

    public default boolean containsDirection( TrafficDirection direction )
    {
        return direction.equals(this.getDirection());
    }
}
