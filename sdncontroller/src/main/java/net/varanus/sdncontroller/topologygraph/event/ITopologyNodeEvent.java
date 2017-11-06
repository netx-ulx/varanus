package net.varanus.sdncontroller.topologygraph.event;


import java.util.Optional;

import net.floodlightcontroller.core.IOFSwitch;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.openflow.types.NodeId;


/**
 * 
 */
@ReturnValuesAreNonnullByDefault
public interface ITopologyNodeEvent
{
    public static enum NodeEventType
    {
        NODE_ADDED,
        NODE_ACTIVATED,
        NODE_UPDATED,
        NODE_DEACTIVATED,
        NODE_REMOVED
    }

    public NodeEventType getType();

    public NodeId getNodeId();

    public Optional<IOFSwitch> getIOFSwitch();
}
