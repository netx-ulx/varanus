package net.varanus.sdncontroller.staticforwarding;


import net.varanus.sdncontroller.staticforwarding.internal.StaticForwardingManager;
import net.varanus.sdncontroller.util.module.AbstractFloodlightModule;


/**
 * 
 */
public final class StaticForwardingModule extends AbstractFloodlightModule
{
    public StaticForwardingModule()
    {
        super(new StaticForwardingManager());
    }
}
