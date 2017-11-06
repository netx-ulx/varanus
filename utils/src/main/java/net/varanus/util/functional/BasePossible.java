package net.varanus.util.functional;


import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;


/**
 * @param <P>
 *            The type of the possible
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public interface BasePossible<P extends BasePossible<P>>
{
    public boolean isPresent();

    public P ifAbsent( Runnable action );

    public P ifPresent( Runnable action );
}
