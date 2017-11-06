package net.varanus.sdncontroller.monitoring.submodules.sampling.internal;


import static net.varanus.sdncontroller.monitoring.submodules.sampling.internal.Utils.convertAppFlowMod;
import static net.varanus.sdncontroller.monitoring.submodules.sampling.internal.Utils.isDefaultSamplingFlow;
import static net.varanus.sdncontroller.monitoring.submodules.sampling.internal.Utils.isEphemeralSamplingFlow;

import java.util.Collection;
import java.util.Objects;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFGroupMod;
import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFType;
import org.slf4j.Logger;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.IPreOutgoingOFMessageHandler;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.varanus.sdncontroller.logging.Logging;
import net.varanus.sdncontroller.monitoring.internal.IMonitoringModuleContext;
import net.varanus.sdncontroller.monitoring.util.ISubmoduleManager;
import net.varanus.sdncontroller.util.module.ModuleUtils;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
final class OutgoingMessageListener implements ISubmoduleManager, IPreOutgoingOFMessageHandler
{
    private static final Logger LOG = Logging.monitoring.sampling.LOG;

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies()
    {
        return ModuleUtils.services(IFloodlightProviderService.class);
    }

    @Override
    public void init( IMonitoringModuleContext context )
    {
        context.getServiceImpl(IFloodlightProviderService.class)
            .addPreOutgoingOFMessageHandler(OFType.FLOW_MOD, this);
        context.getServiceImpl(IFloodlightProviderService.class)
            .addPreOutgoingOFMessageHandler(OFType.PACKET_OUT, this);
    }

    @Override
    public void startUp( IMonitoringModuleContext context )
    { /* do nothing */}

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
        return type.equals(OFType.FLOW_MOD) || type.equals(OFType.PACKET_OUT);
    }

    @Override
    public OFMessage handlePreOutgoing( IOFSwitch sw, OFMessage msg, FloodlightContext cntx )
    {
        if (msg.getType().equals(OFType.FLOW_MOD)) {
            return adaptFlowMod(sw, (OFFlowMod)msg);
        }
        else if (msg.getType().equals(OFType.PACKET_OUT)) {
            return adaptPacketOut(sw, (OFPacketOut)msg);
        }
        else {
            return pass(sw, msg, "not a flow-mod or packet-out");
        }
    }

    private static @Nullable OFMessage adaptFlowMod( IOFSwitch sw, OFFlowMod flowMod )
    {
        if (isDefaultSamplingFlow(flowMod)) {
            return pass(sw, flowMod, "is a default sampling flow");
        }
        else if (isEphemeralSamplingFlow(flowMod)) {
            return pass(sw, flowMod, "is an ephemeral sampling flow");
        }
        else {
            try {
                OFFlowMod converted = convertAppFlowMod(sw, flowMod);
                return replace(sw, flowMod, converted);
            }
            catch (IllegalArgumentException e) {
                return abort(sw, flowMod, e.getMessage());
            }
        }
    }

    private static @Nullable OFMessage adaptPacketOut( IOFSwitch sw, OFPacketOut packetOut )
    {
        try {
            // TODO
            // OFPacketOut converted = convertAppPacketOut(sw, packetOut);
            return pass(sw, packetOut, "packet-out is not adapted for now");
        }
        catch (IllegalArgumentException e) {
            return abort(sw, packetOut, e.getMessage());
        }
    }

    private static OFMessage pass( IOFSwitch sw, OFMessage msg, String reason )
    {
        LOG.trace(
            "Passed an unmodified outgoing message of type {} to switch {}: {}",
            new Object[] {typeOfMessage(msg), sw.getId(), reason});

        return msg;
    }

    private static OFMessage replace( IOFSwitch sw, OFMessage oldMsg, OFMessage newMsg )
    {
        LOG.debug("Replaced an outgoing message of type {} to switch {}", typeOfMessage(oldMsg), sw.getId());
        LOG.trace("Replaced message {} to {}", oldMsg, newMsg);

        return newMsg;
    }

    private static @Nullable OFMessage abort( IOFSwitch sw, OFMessage msg, String reason )
    {
        LOG.warn(
            "Aborted an outgoing message of type {} to switch {}: {}",
            new Object[] {typeOfMessage(msg), sw.getId(), reason});
        LOG.trace("Aborted message: {}", msg);

        return null;
    }

    private static String typeOfMessage( OFMessage msg )
    {
        switch (msg.getType()) {
            case FLOW_MOD:
                return String.format("%s (%s)", msg.getType(), ((OFFlowMod)msg).getCommand());

            case GROUP_MOD:
                return String.format("%s (%s)", msg.getType(), ((OFGroupMod)msg).getCommand());

            default:
                return Objects.toString(msg.getType());
        }
    }
}
