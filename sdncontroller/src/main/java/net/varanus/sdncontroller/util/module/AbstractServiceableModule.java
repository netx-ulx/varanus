package net.varanus.sdncontroller.util.module;


import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import javax.annotation.ParametersAreNonnullByDefault;

import net.floodlightcontroller.core.module.IFloodlightService;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public abstract class AbstractServiceableModule extends AbstractFloodlightModule
{
    private final Map<Class<? extends IFloodlightService>, IFloodlightService> serviceMap;

    public <T extends IModuleManager & IFloodlightService> AbstractServiceableModule( T moduleManager,
                                                                                      Class<? extends IFloodlightService> serviceClass )
    {
        this(moduleManager, serviceClass, moduleManager);
    }

    public AbstractServiceableModule( IModuleManager moduleManager,
                                      Class<? extends IFloodlightService> serviceClass,
                                      IFloodlightService service )
    {
        super(moduleManager);
        this.serviceMap = Collections.singletonMap(
            Objects.requireNonNull(serviceClass),
            Objects.requireNonNull(service));
    }

    public AbstractServiceableModule( IModuleManager moduleManager,
                                      Map<Class<? extends IFloodlightService>, IFloodlightService> serviceMap )
    {
        super(moduleManager);
        this.serviceMap = defensiveCopy(serviceMap);
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices()
    {
        return serviceMap.keySet();
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls()
    {
        return serviceMap;
    }

    private static <K, V> Map<K, V> defensiveCopy( Map<K, V> map )
    {
        Map<K, V> copy = new LinkedHashMap<>(map);
        if (copy.keySet().stream().anyMatch(Objects::isNull)
            || copy.values().stream().anyMatch(Objects::isNull)) {
            throw new NullPointerException();
        }
        else {
            return copy;
        }
    }
}
