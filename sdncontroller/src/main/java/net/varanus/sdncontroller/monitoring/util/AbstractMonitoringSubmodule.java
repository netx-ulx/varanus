package net.varanus.sdncontroller.monitoring.util;


import java.util.Collection;
import java.util.Objects;

import javax.annotation.ParametersAreNonnullByDefault;

import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.varanus.sdncontroller.monitoring.internal.IMonitoringModuleContext;
import net.varanus.sdncontroller.monitoring.internal.IMonitoringSubmodule;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class AbstractMonitoringSubmodule implements IMonitoringSubmodule
{
    private final ISubmoduleManager moduleManager;

    public AbstractMonitoringSubmodule( ISubmoduleManager moduleManager )
    {
        this.moduleManager = Objects.requireNonNull(moduleManager);
    }

    @Override
    public Class<? extends IFloodlightService> getModuleService()
    {
        return null;
    }

    @Override
    public IFloodlightService getServiceImpl()
    {
        return null;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies()
    {
        return moduleManager.getModuleDependencies();
    }

    @Override
    public void init( IMonitoringModuleContext monitoringContext ) throws FloodlightModuleException
    {
        moduleManager.init(monitoringContext);
    }

    @Override
    public void startUp( IMonitoringModuleContext monitoringContext ) throws FloodlightModuleException
    {
        moduleManager.startUp(monitoringContext);
    }
}
