package net.varanus.sdncontroller.monitoring.submodules.sampling.internal;


import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.types.VlanVid;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.varanus.sdncontroller.monitoring.internal.IMonitoringModuleContext;
import net.varanus.sdncontroller.monitoring.submodules.sampling.internal.SwitchComm.SwitchCommException;
import net.varanus.sdncontroller.monitoring.util.ISubmoduleManager;
import net.varanus.sdncontroller.types.FlowedLink;
import net.varanus.sdncontroller.util.module.ModuleUtils;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.openflow.types.NodeId;
import net.varanus.util.openflow.types.PortId;


/**
 * 
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
final class SwitchCommHelper implements ISubmoduleManager
{
    private @Nullable IOFSwitchService switchService;

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies()
    {
        return ModuleUtils.services(IOFSwitchService.class);
    }

    @Override
    public void init( IMonitoringModuleContext context )
    {
        this.switchService = context.getServiceImpl(IOFSwitchService.class);
    }

    @Override
    public void startUp( IMonitoringModuleContext context )
    { /* do nothing */ }

    void installSamplingFlows( FlowedLink flowedLink,
                               VlanVid sampTag,
                               BarrierHandler barrierHandler,
                               Function<OFVersion, Optional<OFAction>> srcActionProv,
                               Function<OFVersion, Optional<OFAction>> destActionProv )
        throws SwitchCommException
    {
        SwitchComm.installSamplingFlows(
            getIOFSwitch(flowedLink.getSrcNode()),
            getIOFSwitch(flowedLink.getDestNode()),
            flowedLink,
            sampTag,
            barrierHandler,
            srcActionProv,
            destActionProv);
    }

    void removeSamplingFlows( FlowedLink flowedLink )
    {
        SwitchComm.removeSamplingFlows(
            getIOFSwitch(flowedLink.getSrcNode()),
            getIOFSwitch(flowedLink.getDestNode()),
            flowedLink);
    }

    void updateTag( NodeId srcNodeId, PortId outPortId, VlanVid tag ) throws SwitchCommException
    {
        SwitchComm.updateTag(getIOFSwitch(srcNodeId), srcNodeId, outPortId, tag);
    }

    void purgeSamplingTables( NodeId nodeId, VlanVid sampTag, BarrierHandler barrierHandler ) throws SwitchCommException
    {
        SwitchComm.purgeSamplingTables(getIOFSwitch(nodeId), nodeId, sampTag, barrierHandler);
    }

    void sendBarrier( NodeId nodeId, BarrierHandler barrierHandler ) throws SwitchCommException
    {
        SwitchComm.sendBarrier(getIOFSwitch(nodeId), nodeId, barrierHandler);
    }

    private Optional<IOFSwitch> getIOFSwitch( NodeId nodeId )
    {
        return Optional.ofNullable(switchService.getSwitch(nodeId.getDpid()));
    }
}
