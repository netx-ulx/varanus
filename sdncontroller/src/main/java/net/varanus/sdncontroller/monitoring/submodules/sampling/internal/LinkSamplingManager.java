package net.varanus.sdncontroller.monitoring.submodules.sampling.internal;


import java.util.Collection;

import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.varanus.sdncontroller.monitoring.internal.IMonitoringModuleContext;
import net.varanus.sdncontroller.monitoring.submodules.sampling.ISamplingService;
import net.varanus.sdncontroller.monitoring.util.ISubmoduleManager;
import net.varanus.sdncontroller.util.module.ModuleUtils;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.openflow.types.Flow;


/**
 * 
 */
@FieldsAreNonnullByDefault
public final class LinkSamplingManager implements ISamplingService, ISubmoduleManager
{
    private final OutgoingMessageListener outMessageListener;
    private final SwitchCommHelper        switchComm;
    private final Sampler                 sampler;
    private final SamplingResultListener  sampResultListener;
    private final TopologyListener        topoListener;

    public LinkSamplingManager()
    {
        this.outMessageListener = new OutgoingMessageListener();
        this.switchComm = new SwitchCommHelper();
        this.sampler = new Sampler(switchComm);
        this.sampResultListener = new SamplingResultListener(sampler);
        this.topoListener = new TopologyListener(sampler);
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies()
    {
        return ModuleUtils.services(
            outMessageListener.getModuleDependencies(),
            switchComm.getModuleDependencies(),
            sampler.getModuleDependencies(),
            sampResultListener.getModuleDependencies(),
            topoListener.getModuleDependencies());
    }

    @Override
    public void init( IMonitoringModuleContext context ) throws FloodlightModuleException
    {
        outMessageListener.init(context);
        switchComm.init(context);
        sampler.init(context);
        sampResultListener.init(context);
        topoListener.init(context);
    }

    @Override
    public void startUp( IMonitoringModuleContext context ) throws FloodlightModuleException
    {
        outMessageListener.startUp(context);
        switchComm.startUp(context);
        sampler.startUp(context);
        sampResultListener.startUp(context);
        topoListener.startUp(context);
    }

    @Override
    public boolean isValidSamplableFlow( Flow flow )
    {
        return sampler.isValidSamplableFlow(flow);
    }

    @Override
    public void validateSamplableFlow( Flow flow ) throws IllegalArgumentException
    {
        sampler.validateSamplableFlow(flow);
    }

    @Override
    public boolean startSampling( Flow flow ) throws IllegalArgumentException
    {
        return sampler.startSamplingFlow(flow);
    }

    @Override
    public boolean stopSampling( Flow flow ) throws IllegalArgumentException
    {
        return sampler.stopSamplingFlow(flow);
    }
}
