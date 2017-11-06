package net.varanus.sdncontroller.monitoring.submodules.switches;


import net.varanus.sdncontroller.monitoring.submodules.switches.internal.SwitchMonitoringManager;
import net.varanus.sdncontroller.monitoring.util.AbstractMonitoringSubmodule;


/**
 * 
 */
public final class SwitchMonitoringModule extends AbstractMonitoringSubmodule
{
    public SwitchMonitoringModule()
    {
        super(new SwitchMonitoringManager());
    }
}
