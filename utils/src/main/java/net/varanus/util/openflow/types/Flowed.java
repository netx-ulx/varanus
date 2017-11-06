package net.varanus.util.openflow.types;


import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;


/**
 * @param <F>
 *            The flowable type
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public interface Flowed<F extends Flowable<F>>
{
    public F unflowed();

    public Flow getFlow();

    public default boolean contains( F unflowed )
    {
        return unflowed.equals(this.unflowed());
    }

    public default boolean containsFlow( Flow flow )
    {
        return flow.equals(this.getFlow());
    }
}
