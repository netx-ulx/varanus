package net.varanus.sdncontroller.util.module;


import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import javax.annotation.ParametersAreNonnullByDefault;

import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public abstract class AbstractFloodlightModule implements IFloodlightModule
{
    private final IModuleManager moduleManager;

    public AbstractFloodlightModule( IModuleManager moduleManager )
    {
        this.moduleManager = Objects.requireNonNull(moduleManager);
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices()
    {
        return Collections.emptyList();
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls()
    {
        return Collections.emptyMap();
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies()
    {
        return moduleManager.getModuleDependencies();
    }

    @Override
    public void init( FloodlightModuleContext context ) throws FloodlightModuleException
    {
        moduleManager.init(context, this.getClass());
    }

    @Override
    public void startUp( FloodlightModuleContext context ) throws FloodlightModuleException
    {
        moduleManager.startUp(context, this.getClass());
    }
}
