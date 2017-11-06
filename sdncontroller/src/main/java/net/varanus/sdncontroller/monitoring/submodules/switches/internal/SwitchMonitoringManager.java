package net.varanus.sdncontroller.monitoring.submodules.switches.internal;


import java.util.Collection;

import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.varanus.sdncontroller.monitoring.internal.IMonitoringModuleContext;
import net.varanus.sdncontroller.monitoring.util.ISubmoduleManager;
import net.varanus.sdncontroller.util.module.ModuleUtils;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;


/**
 * 
 */
@FieldsAreNonnullByDefault
public final class SwitchMonitoringManager implements ISubmoduleManager
{
    private final SwitchMonitor          monitor;
    private final TopologyListener topoListener;

    public SwitchMonitoringManager()
    {
        this.monitor = new SwitchMonitor();
        this.topoListener = new TopologyListener(monitor);
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies()
    {
        return ModuleUtils.services(
            monitor.getModuleDependencies(),
            topoListener.getModuleDependencies());
    }

    @Override
    public void init( IMonitoringModuleContext context ) throws FloodlightModuleException
    {
        monitor.init(context);
        topoListener.init(context);
    }

    @Override
    public void startUp( IMonitoringModuleContext context ) throws FloodlightModuleException
    {
        monitor.startUp(context);
        monitor.init(context);
    }
}
