package net.varanus.sdncontroller.monitoring.submodules.sampling.internal;


import static net.varanus.sdncontroller.monitoring.submodules.sampling.internal.Utils.destinationSamplingTableID;
import static net.varanus.sdncontroller.monitoring.submodules.sampling.internal.Utils.getDestinationPort;
import static net.varanus.sdncontroller.monitoring.submodules.sampling.internal.Utils.getSourcePort;
import static net.varanus.sdncontroller.monitoring.submodules.sampling.internal.Utils.sourceSamplingTableID;
import static net.varanus.sdncontroller.monitoring.submodules.sampling.internal.Utils.withoutDestinationPort;
import static net.varanus.sdncontroller.monitoring.submodules.sampling.internal.Utils.withoutDestinationTag;
import static net.varanus.sdncontroller.monitoring.submodules.sampling.internal.Utils.withoutSourcePort;
import static net.varanus.sdncontroller.monitoring.submodules.sampling.internal.Utils.withoutSourceTag;

import java.util.Collection;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.projectfloodlight.openflow.protocol.OFFlowRemoved;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.types.TableId;
import org.slf4j.Logger;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.varanus.sdncontroller.alias.IAliasService;
import net.varanus.sdncontroller.logging.Logging;
import net.varanus.sdncontroller.monitoring.internal.IMonitoringModuleContext;
import net.varanus.sdncontroller.monitoring.util.ISubmoduleManager;
import net.varanus.sdncontroller.util.module.ModuleUtils;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.openflow.types.DirectedPortId;
import net.varanus.util.openflow.types.Flow;
import net.varanus.util.openflow.types.FlowDirectedNodePort;
import net.varanus.util.openflow.types.NodeId;
import net.varanus.util.time.Timed;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
final class SamplingResultListener implements IOFMessageListener, ISubmoduleManager
{
    private static final Logger LOG = Logging.monitoring.sampling.LOG;

    private final Sampler           sampler;
    private @Nullable IAliasService aliasService;

    SamplingResultListener( Sampler sampler )
    {
        this.sampler = sampler;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies()
    {
        return ModuleUtils.services(IAliasService.class, IFloodlightProviderService.class);
    }

    @Override
    public void init( IMonitoringModuleContext context )
    {
        this.aliasService = context.getServiceImpl(IAliasService.class);
        context.getServiceImpl(IFloodlightProviderService.class).addOFMessageListener(OFType.FLOW_REMOVED, this);
    }

    @Override
    public void startUp( IMonitoringModuleContext context )
    { /* do nothing */ }

    @Override
    public String getName()
    {
        return "varanus.monitoring.sampling";
    }

    @Override
    public boolean isCallbackOrderingPrereq( OFType type, String name )
    {
        return false;
    }

    @Override
    public boolean isCallbackOrderingPostreq( OFType type, String name )
    {
        return type.equals(OFType.FLOW_REMOVED);
    }

    @Override
    public Command receive( IOFSwitch sw, OFMessage msg, FloodlightContext cntx )
    {
        if (msg.getType().equals(OFType.FLOW_REMOVED)) {
            NodeId nodeId = NodeId.of(sw.getId(), aliasService::getSwitchAlias);
            Timed<OFFlowRemoved> flowRem = Timed.now((OFFlowRemoved)msg);
            TableId srcMonTable = sourceSamplingTableID(sw);
            TableId destMonTable = destinationSamplingTableID(sw);
            Flow sampFlow = Flow.of(flowRem.value().getMatch());

            if (flowRem.value().getTableId().equals(srcMonTable)) {
                receiveSourceSamplingResult(
                    nodeId,
                    sampFlow,
                    flowRem);
                return Command.STOP;
            }
            else if (flowRem.value().getTableId().equals(destMonTable)) {
                receiveDestinationSamplingResult(
                    nodeId,
                    sampFlow,
                    flowRem);
                return Command.STOP;
            }
            else {
                LOG.trace("Ignored received flow statistics (not from our tables) from switch {}", nodeId);
            }
        }

        return Command.CONTINUE;
    }

    private void receiveSourceSamplingResult( NodeId nodeId, Flow sampFlow, Timed<OFFlowRemoved> result )
    {
        LOG.trace("Received SRC flow statistics from switch {} with flow {}", nodeId, sampFlow);

        DirectedPortId port = getSourcePort(sampFlow.getMatch());
        if (port != null) {
            Match origMatch = withoutSourceTag(withoutSourcePort(sampFlow.getMatch())).build();
            FlowDirectedNodePort endpoint = FlowDirectedNodePort.of(nodeId, port, Flow.of(origMatch));

            sampler.receiveSamplingResult(endpoint, result);
        }
        else {
            LOG.warn(
                "Expected SRC port in flow statistics from switch {} with flow {}, but found none!",
                nodeId,
                sampFlow);
        }
    }

    private void receiveDestinationSamplingResult( NodeId nodeId, Flow sampFlow, Timed<OFFlowRemoved> result )
    {
        LOG.trace("Received DEST flow statistics from switch {} with flow {}", nodeId, sampFlow);

        DirectedPortId port = getDestinationPort(sampFlow.getMatch());
        if (port != null) {
            Match origMatch = withoutDestinationTag(withoutDestinationPort(sampFlow.getMatch())).build();
            FlowDirectedNodePort endpoint = FlowDirectedNodePort.of(nodeId, port, Flow.of(origMatch));

            sampler.receiveSamplingResult(endpoint, result);
        }
        else {
            LOG.warn(
                "Expected DEST port in flow statistics from switch {} with flow {}, but found none!",
                nodeId,
                sampFlow);
        }
    }
}
