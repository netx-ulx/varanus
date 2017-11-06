package net.varanus.sdncontroller.monitoring.util;


import java.util.Objects;

import javax.annotation.ParametersAreNonnullByDefault;

import net.floodlightcontroller.core.module.IFloodlightService;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class AbstractServiceableSubmodule extends AbstractMonitoringSubmodule
{
    private final Class<? extends IFloodlightService> serviceClass;
    private final IFloodlightService                  service;

    public <T extends ISubmoduleManager & IFloodlightService> AbstractServiceableSubmodule( T moduleManager,
                                                                                            Class<? extends IFloodlightService> serviceClass )
    {
        this(moduleManager, serviceClass, moduleManager);
    }

    public AbstractServiceableSubmodule( ISubmoduleManager moduleManager,
                                         Class<? extends IFloodlightService> serviceClass,
                                         IFloodlightService service )
    {
        super(moduleManager);
        this.serviceClass = Objects.requireNonNull(serviceClass);
        this.service = Objects.requireNonNull(service);
    }

    @Override
    public Class<? extends IFloodlightService> getModuleService()
    {
        return serviceClass;
    }

    @Override
    public IFloodlightService getServiceImpl()
    {
        return service;
    }
}
