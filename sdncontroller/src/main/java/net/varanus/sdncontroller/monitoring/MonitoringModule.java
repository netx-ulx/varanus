package net.varanus.sdncontroller.monitoring;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.varanus.sdncontroller.monitoring.internal.IMonitoringSubmodule;
import net.varanus.sdncontroller.monitoring.internal.MonitoringManager;
import net.varanus.sdncontroller.monitoring.submodules.collectorhandler.CollectorHandlerModule;
import net.varanus.sdncontroller.monitoring.submodules.probing.LinkProbingModule;
import net.varanus.sdncontroller.monitoring.submodules.sampling.LinkSamplingModule;
import net.varanus.sdncontroller.monitoring.submodules.switches.SwitchMonitoringModule;
import net.varanus.sdncontroller.util.module.AbstractServiceableModule;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.lang.ClassInstantiationException;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class MonitoringModule extends AbstractServiceableModule
{
    private static final Iterable<Class<? extends IMonitoringSubmodule>> MONITORING_MODULES = getMonitoringModules();

    private static Iterable<Class<? extends IMonitoringSubmodule>> getMonitoringModules()
    {
        List<Class<? extends IMonitoringSubmodule>> modules = new ArrayList<>();

        // ==== Core modules (ORDERING MATTERS!!!) ==== //
        modules.add(CollectorHandlerModule.class);
        modules.add(LinkProbingModule.class);
        modules.add(LinkSamplingModule.class);
        modules.add(SwitchMonitoringModule.class);

        // ==== Testing modules ==== //

        return Collections.unmodifiableList(modules);
    }

    public MonitoringModule() throws ClassInstantiationException
    {
        super(new MonitoringManager(MONITORING_MODULES), IMonitoringService.class);
    }
}
