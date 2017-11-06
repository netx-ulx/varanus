package net.varanus.util.openflow.types;


import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;


/**
 * @param <T>
 *            The flowable type
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public interface Flowable<T extends Flowable<T>>
{
    public Flowed<T> flowed( Flow flow );
}
