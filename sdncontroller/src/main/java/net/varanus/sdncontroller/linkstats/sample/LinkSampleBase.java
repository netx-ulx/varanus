package net.varanus.sdncontroller.linkstats.sample;


import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;


/**
 * @param <LINK>
 *            The type of the link
 */
@ReturnValuesAreNonnullByDefault
public interface LinkSampleBase<LINK>
{
    public LINK getLink();

    public boolean hasResults();
}
