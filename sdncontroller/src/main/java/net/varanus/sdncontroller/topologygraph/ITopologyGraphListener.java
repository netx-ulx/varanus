package net.varanus.sdncontroller.topologygraph;


import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.sdncontroller.topologygraph.event.ITopologyLinkEvent;
import net.varanus.sdncontroller.topologygraph.event.ITopologyNodeEvent;
import net.varanus.sdncontroller.topologygraph.event.ITopologyPortEvent;


/**
 * 
 */
@ParametersAreNonnullByDefault
public interface ITopologyGraphListener
{
    public void onNodeEvent( ITopologyNodeEvent event );

    public void onPortEvent( ITopologyPortEvent event );

    public void onLinkEvent( ITopologyLinkEvent event );
}
