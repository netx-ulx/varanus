package net.varanus.sdncontroller.monitoring.internal;


import java.util.Collection;

import javax.annotation.CheckForNull;
import javax.annotation.ParametersAreNonnullByDefault;

import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;


/**
 * 
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public interface IMonitoringSubmodule
{
    public @CheckForNull Class<? extends IFloodlightService> getModuleService();

    public @CheckForNull IFloodlightService getServiceImpl();

    public Collection<Class<? extends IFloodlightService>> getModuleDependencies();

    public void init( IMonitoringModuleContext monitoringContext ) throws FloodlightModuleException;

    public void startUp( IMonitoringModuleContext monitoringContext ) throws FloodlightModuleException;
}
