package net.varanus.sdncontroller.monitoring.internal;


import java.util.Collection;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import net.floodlightcontroller.core.module.IFloodlightService;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;


/**
 *
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public interface IMonitoringModuleContext
{
    public <T extends IFloodlightService> T getServiceImpl( Class<T> service );

    public Collection<Class<? extends IFloodlightService>> getAllServices();

    public Map<String, String> getConfigParams();
}
