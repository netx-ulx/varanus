package net.varanus.sdncontroller.activeforwarding;


import net.varanus.sdncontroller.activeforwarding.internal.ActiveForwardingManager;
import net.varanus.sdncontroller.util.module.AbstractServiceableModule;


/**
 * 
 */
public final class ActiveForwardingModule extends AbstractServiceableModule
{
    public ActiveForwardingModule()
    {
        super(new ActiveForwardingManager(), IActiveForwardingService.class);
    }
}
