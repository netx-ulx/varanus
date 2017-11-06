package net.varanus.sdncontroller.util.module;


import java.util.AbstractMap;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;

import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModuleContext;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.collect.CollectionUtils;
import net.varanus.util.collect.builder.LinkedHashMapBuilder;
import net.varanus.util.collect.builder.LinkedHashSetBuilder;
import net.varanus.util.text.BooleanProperty;
import net.varanus.util.text.CustomProperty;
import net.varanus.util.text.IntProperty;
import net.varanus.util.text.LongProperty;


/**
 * 
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class ModuleUtils
{
    /**
     * Returns a new thread-safe insertion-ordered set for listener objects.
     * 
     * @return a thread-safe insertion-ordered set
     */
    public static <L> Set<L> newListenerSet()
    {
        return Collections.synchronizedSet(new LinkedHashSet<>());
    }

    public static Set<Class<? extends IFloodlightService>> services( Class<? extends IFloodlightService> service )
    {
        return LinkedHashSetBuilder.build(service);
    }

    @SafeVarargs
    @SuppressWarnings( "varargs" )
    public static Set<Class<? extends IFloodlightService>> services( Class<? extends IFloodlightService>... services )
    {
        return LinkedHashSetBuilder.build(services);
    }

    public static Set<Class<? extends IFloodlightService>> services( Collection<Class<? extends IFloodlightService>> services )
    {
        return LinkedHashSetBuilder.build(services);
    }

    @SafeVarargs
    @SuppressWarnings( "varargs" )
    public static Set<Class<? extends IFloodlightService>> services( Collection<Class<? extends IFloodlightService>> services,
                                                                     Class<? extends IFloodlightService>... moreServices )
    {
        int totalSize = Math.addExact(services.size(), moreServices.length);
        int capacity = CollectionUtils.initialMapCapacity(totalSize);
        return LinkedHashSetBuilder.<Class<? extends IFloodlightService>>create(capacity)
            .addAll(services)
            .add(moreServices)
            .build();
    }

    @SafeVarargs
    @SuppressWarnings( "varargs" )
    public static Set<Class<? extends IFloodlightService>> services( Collection<Class<? extends IFloodlightService>>... services )
    {
        int totalSize = combinedSize(services);
        int capacity = CollectionUtils.initialMapCapacity(totalSize);
        LinkedHashSetBuilder<Class<? extends IFloodlightService>> builder = LinkedHashSetBuilder.create(capacity);
        for (Collection<Class<? extends IFloodlightService>> servs : services) {
            builder.addAll(servs);
        }
        return builder.build();
    }

    public static Map<Class<? extends IFloodlightService>,
                      IFloodlightService> serviceImpls( Class<? extends IFloodlightService> service,
                                                        IFloodlightService serviceImpl )
    {
        return LinkedHashMapBuilder.build(service, serviceImpl);
    }

    @SafeVarargs
    @SuppressWarnings( "varargs" )
    public static Map<Class<? extends IFloodlightService>,
                      IFloodlightService> serviceImpls( ServiceSpec<? extends IFloodlightService>... specs )
    {
        int capacity = CollectionUtils.initialMapCapacity(specs.length);
        return LinkedHashMapBuilder.<Class<? extends IFloodlightService>, IFloodlightService>create(capacity)
            .put(specs)
            .build();
    }

    public static <T extends IFloodlightService> ServiceSpec<T> spec( Class<T> serviceClass, T serviceImpl )
    {
        return new ServiceSpec<>(serviceClass, serviceImpl);
    }

    /**
     * Returns a non-null service implementation.
     * 
     * @param context
     * @param service
     * @return a service implementation
     * @exception IllegalStateException
     *                If there is not an implemented service with the specified
     *                class
     */
    public static <T extends IFloodlightService> T getServiceImpl( IFloodlightModuleContext context, Class<T> service )
    {
        T impl = context.getServiceImpl(Objects.requireNonNull(service));
        if (impl == null) {
            throw new IllegalStateException(
                String.format("no implementation was found for service %s", service.getName()));
        }
        return impl;
    }

    public static boolean readBooleanProperty( Map<String, String> params, BooleanProperty boolProp )
        throws FloodlightModuleException
    {
        try {
            return boolProp.readBoolean(params);
        }
        catch (IllegalArgumentException e) {
            throw new FloodlightModuleException(e.getMessage());
        }
    }

    public static int readIntProperty( Map<String, String> params, IntProperty intProp )
        throws FloodlightModuleException
    {
        try {
            return intProp.readInt(params);
        }
        catch (IllegalArgumentException e) {
            throw new FloodlightModuleException(e.getMessage());
        }
    }

    public static long readLongProperty( Map<String, String> params, LongProperty longProp )
        throws FloodlightModuleException
    {
        try {
            return longProp.readLong(params);
        }
        catch (IllegalArgumentException e) {
            throw new FloodlightModuleException(e.getMessage());
        }
    }

    public static <T> T readCustomProperty( Map<String, String> params, CustomProperty<T> customProp )
        throws FloodlightModuleException
    {
        try {
            return customProp.readProperty(params);
        }
        catch (IllegalArgumentException e) {
            throw new FloodlightModuleException(e.getMessage());
        }
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class ServiceSpec<T extends IFloodlightService>
        extends AbstractMap.SimpleImmutableEntry<Class<T>, T>
    {
        private static final long serialVersionUID = -682663377954949768L;

        public ServiceSpec( Class<T> serviceClass, T serviceImpl )
        {
            super(Objects.requireNonNull(serviceClass), Objects.requireNonNull(serviceImpl));
        }

        public Class<T> getServiceClass()
        {
            return getKey();
        }

        public T getServiceImpl()
        {
            return getValue();
        }
    }

    private static int combinedSize( Collection<?>[] colls )
    {
        return Stream.of(colls)
            .mapToInt(Collection::size)
            .reduce(0, Math::addExact);
    }

    private ModuleUtils()
    {
        // not used
    }
}
