package net.varanus.sdncontroller.monitoring.internal;


import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.slf4j.Logger;

import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightModuleContext;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.linkdiscovery.ILinkDiscoveryService;
import net.floodlightcontroller.linkdiscovery.internal.LinkDiscoveryManager;
import net.varanus.sdncontroller.logging.Logging;
import net.varanus.sdncontroller.monitoring.IMonitoringService;
import net.varanus.sdncontroller.monitoring.MonitoringModule;
import net.varanus.sdncontroller.monitoring.submodules.sampling.ISamplingService;
import net.varanus.sdncontroller.util.module.IModuleManager;
import net.varanus.sdncontroller.util.module.ModuleUtils;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.lang.ClassInstantiationException;
import net.varanus.util.lang.ClassUtils;
import net.varanus.util.openflow.types.Flow;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
public final class MonitoringManager implements IModuleManager, IMonitoringService
{
    private static final Logger LOG = Logging.monitoring.LOG;

    private @Nullable MonitoringContext monitoringContext;
    private @Nullable ISamplingService  samplingService;

    public MonitoringManager( Iterable<Class<? extends IMonitoringSubmodule>> subModules )
                                                                                           throws ClassInstantiationException
    {
        this.monitoringContext = new MonitoringContext(subModules);
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies()
    {
        return monitoringContext.getModuleDependencies();
    }

    @Override
    public void init( FloodlightModuleContext context, Class<? extends IFloodlightModule> moduleClass )
        throws FloodlightModuleException
    {
        // FIXME MOTHER OF ALL HACKS!!! CHANGE THIS TO A FLOODLIGHT
        // RECOMPILATION
        final int lldpTimeout = 600;
        LOG.warn("==============================================================");
        LOG.warn(">>>>>>>>> LLDP TIMEOUT HACK IS ENABLED ({} seconds) <<<<<<<<", lldpTimeout);
        LOG.warn("==============================================================");
        ILinkDiscoveryService linkDiscService = ModuleUtils.getServiceImpl(context, ILinkDiscoveryService.class);
        LinkDiscoveryManager linkDiscManager = (LinkDiscoveryManager)linkDiscService;
        try {
            Field timeoutField = LinkDiscoveryManager.class.getDeclaredField("LINK_TIMEOUT");
            timeoutField.setAccessible(true);

            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(timeoutField, timeoutField.getModifiers() & ~Modifier.FINAL);

            timeoutField.setInt(linkDiscManager, lldpTimeout);
        }
        catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        monitoringContext.initContext(context);

        this.samplingService = monitoringContext.getServiceImpl(ISamplingService.class);
        monitoringContext.initMonitoringModules();
    }

    @Override
    public void startUp( FloodlightModuleContext context, Class<? extends IFloodlightModule> moduleClass )
        throws FloodlightModuleException
    {
        monitoringContext.startUpMonitoringModules();
    }

    @Override
    public boolean isValidMonitorableFlow( Flow flow )
    {
        return samplingService.isValidSamplableFlow(flow);
    }

    @Override
    public void validateMonitorableFlow( Flow flow )
    {
        samplingService.validateSamplableFlow(flow);
    }

    @Override
    public boolean startMonitoring( Flow flow )
    {
        return samplingService.startSampling(flow);
    }

    @Override
    public boolean stopMonitoring( Flow flow )
    {
        return samplingService.stopSampling(flow);
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    private static final class MonitoringContext implements IMonitoringModuleContext
    {
        private final List<IMonitoringSubmodule>                                     modules;
        private final Map<Class<? extends IFloodlightService>, IMonitoringSubmodule> modulesByService;

        // NOTE: access only through getContext() method
        private @Nullable IFloodlightModuleContext floodlightContext;

        MonitoringContext( Iterable<Class<? extends IMonitoringSubmodule>> subModules )
                                                                                        throws ClassInstantiationException
        {
            List<IMonitoringSubmodule> list = new ArrayList<>();
            Map<Class<? extends IFloodlightService>, IMonitoringSubmodule> map = new HashMap<>();
            for (Class<? extends IMonitoringSubmodule> moduleClass : subModules) {
                IMonitoringSubmodule module = ClassUtils.newClassInstance(moduleClass);
                list.add(module);
                // only put in map if module implements a service
                putMonitoringModule(map, module);
            }

            this.modules = Collections.unmodifiableList(list);
            this.modulesByService = Collections.unmodifiableMap(map);

            // initialized via method initContext()
            this.floodlightContext = null;
        }

        private IFloodlightModuleContext getContext()
        {
            return Objects.requireNonNull(floodlightContext, "monitoring context must be initialized first");
        }

        @Override
        public <T extends IFloodlightService> T getServiceImpl( Class<T> serviceClass )
        {
            IFloodlightModuleContext context = getContext();

            IMonitoringSubmodule module = modulesByService.get(serviceClass);
            if (module != null) {
                @SuppressWarnings( "unchecked" )
                T service = (T)module.getServiceImpl();
                if (service == null) {
                    throw new IllegalStateException(
                        String.format("no implementation was found for service %s", serviceClass.getName()));
                }

                return service;
            }
            else {
                return ModuleUtils.getServiceImpl(context, serviceClass);
            }
        }

        @Override
        public Collection<Class<? extends IFloodlightService>> getAllServices()
        {
            IFloodlightModuleContext context = getContext();

            List<Class<? extends IFloodlightService>> services = new ArrayList<>();
            services.addAll(context.getAllServices());
            services.addAll(modulesByService.keySet());
            return services;
        }

        @Override
        public Map<String, String> getConfigParams()
        {
            return getContext().getConfigParams(MonitoringModule.class);
        }

        /**
         * Initializes this context with a provided
         * {@code IFloodlightModuleContext} object.
         * 
         * @param context
         */
        void initContext( IFloodlightModuleContext context )
        {
            this.floodlightContext = Objects.requireNonNull(context);
        }

        /**
         * Initializes all the monitoring modules by calling method
         * {@code init()} on each one.
         * 
         * @throws FloodlightModuleException
         */
        void initMonitoringModules() throws FloodlightModuleException
        {
            for (IMonitoringSubmodule module : modules) {
                LOG.info("Initializing monitoring module {} ...", module.getClass());
                module.init(this);
            }
        }

        /**
         * Starts up all the monitoring modules by calling method
         * {@code startUp()} on each one.
         * 
         * @throws FloodlightModuleException
         */
        void startUpMonitoringModules() throws FloodlightModuleException
        {
            for (IMonitoringSubmodule module : modules) {
                LOG.info("Starting up monitoring module {} ...", module.getClass());
                module.startUp(this);
            }
        }

        Collection<Class<? extends IFloodlightService>> getModuleDependencies()
        {
            Set<Class<? extends IFloodlightService>> deps = new HashSet<>();
            for (IMonitoringSubmodule module : modules) {
                Collection<Class<? extends IFloodlightService>> moduleDeps = module.getModuleDependencies();
                // Check if the dependency is from one of our submodules and do
                // not show it to floodlight
                for (Class<? extends IFloodlightService> modDep : moduleDeps) {
                    if (!modulesByService.containsKey(modDep)) {
                        // It isn't on of our modules so let's add it
                        deps.add(modDep);
                    }
                }
            }
            return deps;
        }

        private static void putMonitoringModule( Map<Class<? extends IFloodlightService>, IMonitoringSubmodule> map,
                                                 IMonitoringSubmodule module )
        {
            Class<? extends IFloodlightService> service = module.getModuleService();
            if (service != null) {
                map.put(service, module);
            }
        }
    }
}
