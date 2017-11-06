package net.varanus.sdncontroller.topologygraph.event;


import java.util.Optional;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.linkdiscovery.internal.LinkInfo;
import net.varanus.sdncontroller.types.DatapathLink;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;


/**
 * 
 */
@ReturnValuesAreNonnullByDefault
public interface ITopologyLinkEvent
{
    public static enum LinkEventType
    {
        LINK_ADDED,
        LINK_UPDATED,
        LINK_REMOVED
    }

    public LinkEventType getType();

    public DatapathLink getLink();

    public Optional<LinkInfo> getLinkInfo();

    public Optional<IOFSwitch> getSrcIOFSwitch();

    public Optional<IOFSwitch> getDestIOFSwitch();
}
