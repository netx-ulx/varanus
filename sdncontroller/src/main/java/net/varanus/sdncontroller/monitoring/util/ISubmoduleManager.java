package net.varanus.sdncontroller.monitoring.util;


import java.util.Collection;

import javax.annotation.ParametersAreNonnullByDefault;

import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.varanus.sdncontroller.monitoring.internal.IMonitoringModuleContext;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;


/**
 * 
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public interface ISubmoduleManager
{
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies();

    public void init( IMonitoringModuleContext context ) throws FloodlightModuleException;

    public void startUp( IMonitoringModuleContext context ) throws FloodlightModuleException;
}
