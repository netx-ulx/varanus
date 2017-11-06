package net.varanus.util.openflow.types;


import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;


/**
 * @param <T>
 *            The directable type
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public interface Directable<T extends Directable<T>>
{
    public Directed<T> directed( TrafficDirection direction );
}
