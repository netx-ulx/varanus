package net.varanus.sdncontroller.topologygraph.event;


import java.util.Optional;

import org.projectfloodlight.openflow.protocol.OFPortDesc;

import net.floodlightcontroller.core.IOFSwitch;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.openflow.types.NodePort;


/**
 * 
 */
@ReturnValuesAreNonnullByDefault
public interface ITopologyPortEvent
{
    public static enum PortEventType
    {
        PORT_ADDED,
        PORT_ACTIVATED,
        PORT_UPDATED,
        PORT_DEACTIVATED,
        PORT_REMOVED
    }

    public PortEventType getType();

    public NodePort getNodePort();

    public OFPortDesc getOFPortDesc();

    public Optional<IOFSwitch> getIOFSwitch();
}
