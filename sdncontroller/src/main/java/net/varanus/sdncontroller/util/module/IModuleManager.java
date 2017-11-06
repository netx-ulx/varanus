package net.varanus.sdncontroller.util.module;


import java.util.Collection;

import javax.annotation.ParametersAreNonnullByDefault;

import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;


/**
 * 
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public interface IModuleManager
{
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies();

    public void init( FloodlightModuleContext context, Class<? extends IFloodlightModule> moduleClass )
        throws FloodlightModuleException;

    public void startUp( FloodlightModuleContext context, Class<? extends IFloodlightModule> moduleClass )
        throws FloodlightModuleException;
}
