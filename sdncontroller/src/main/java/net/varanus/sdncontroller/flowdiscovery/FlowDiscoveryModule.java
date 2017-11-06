package net.varanus.sdncontroller.flowdiscovery;


import net.varanus.sdncontroller.flowdiscovery.internal.FlowDiscoveryManager;
import net.varanus.sdncontroller.util.module.AbstractFloodlightModule;


/**
 * 
 */
public final class FlowDiscoveryModule extends AbstractFloodlightModule
{
    public FlowDiscoveryModule()
    {
        super(new FlowDiscoveryManager());
    }
}
