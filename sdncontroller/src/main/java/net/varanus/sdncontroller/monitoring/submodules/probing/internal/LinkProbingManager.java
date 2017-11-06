package net.varanus.sdncontroller.monitoring.submodules.probing.internal;


import java.util.Collection;

import javax.annotation.ParametersAreNonnullByDefault;

import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.varanus.sdncontroller.monitoring.internal.IMonitoringModuleContext;
import net.varanus.sdncontroller.monitoring.submodules.probing.IProbingService;
import net.varanus.sdncontroller.monitoring.util.ISubmoduleManager;
import net.varanus.sdncontroller.util.module.ModuleUtils;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.openflow.types.Flow;


/**
 *
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class LinkProbingManager implements ISubmoduleManager, IProbingService
{
    private final Prober           prober;
    private final TopologyListener topoListener;

    public LinkProbingManager()
    {
        this.prober = new Prober();
        this.topoListener = new TopologyListener(prober);
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies()
    {
        return ModuleUtils.services(
            prober.getModuleDependencies(),
            topoListener.getModuleDependencies());
    }

    @Override
    public void init( IMonitoringModuleContext context ) throws FloodlightModuleException
    {
        prober.init(context);
        topoListener.init(context);
    }

    @Override
    public void startUp( IMonitoringModuleContext context ) throws FloodlightModuleException
    {
        prober.startUp(context);
        topoListener.startUp(context);
    }

    @Override
    public Flow getProbeBaseFlow()
    {
        return prober.getProbeBaseFlow();
    }
}
