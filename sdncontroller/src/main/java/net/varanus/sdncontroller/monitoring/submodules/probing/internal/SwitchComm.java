package net.varanus.sdncontroller.monitoring.submodules.probing.internal;


import static net.varanus.util.openflow.OFMessageUtils.eternalFlow;
import static net.varanus.util.openflow.OFMessageUtils.withNoOverlap;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.annotation.ParametersAreNonnullByDefault;

import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFFlowDelete;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TableId;
import org.projectfloodlight.openflow.types.U64;
import org.slf4j.Logger;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.util.AppCookie;
import net.floodlightcontroller.packet.IPacket;
import net.varanus.sdncontroller.logging.Logging;
import net.varanus.sdncontroller.util.IPacketUtils;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.lang.Unsigned;
import net.varanus.util.openflow.types.Flow;
import net.varanus.util.openflow.types.NodeId;
import net.varanus.util.openflow.types.NodePort;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
final class SwitchComm
{
    private static final int PROBING_FLOWS_APP_ID   = 301;
    private static final int PROBING_FLOW_COOKIE_ID = 0x85426e9c;

    private static final U64 PROBING_FLOW_COOKIE;

    static {
        AppCookie.registerApp(PROBING_FLOWS_APP_ID, "varanus-probing-flows");
        PROBING_FLOW_COOKIE = AppCookie.makeCookie(PROBING_FLOWS_APP_ID, PROBING_FLOW_COOKIE_ID);
    }

    private static final Logger LOG = Logging.monitoring.probing.LOG;

    static void handleAddedSwitch( Optional<IOFSwitch> optSw, NodeId nodeId, Flow probFlow, OFPort sampPort )
        throws SwitchCommException
    {
        if (optSw.isPresent()) {
            IOFSwitch sw = optSw.get();
            setupProbingFlow(sw, nodeId, probFlow, sampPort);
        }
        else {
            throw new SwitchCommException(String.format(
                "Could not handle added switch due to failed communication with switch %s", nodeId));
        }
    }

    static void sendProbePacket( IOFSwitch srcSw, NodePort srcNodePort, IPacket pkt, OFPort sampPort )
    {
        if (LOG.isTraceEnabled())
            LOG.trace("Sending probe packet {} from {}", IPacketUtils.toString(pkt), srcNodePort);

        OFFactory fact = srcSw.getOFFactory();
        OFPort outPort = srcNodePort.getPortId().getOFPort();

        OFPacketOut pktOut = fact.buildPacketOut()
            .setData(pkt.serialize())
            .setActions(getProbeSrcOutputActions(fact, outPort, sampPort))
            .build();

        srcSw.write(pktOut);
    }

    private static void setupProbingFlow( IOFSwitch destSw, NodeId destId, Flow probFlow, OFPort sampPort )
    {
        LOG.debug("Setting up probing flow for destination switch {}", destId);

        TableId probFlowTable = TableId.ZERO;
        OFFactory fact = destSw.getOFFactory();

        // First, clear any existing probing flows
        OFFlowDelete.Builder clearBldr = fact.buildFlowDelete()
            .setTableId(probFlowTable)
            .setMatch(
                fact.matchWildcardAll());

        destSw.write(
            toProbingFlow(
                clearBldr)
                    .build());

        // Finally, setup an entry for the probe packets
        OFFlowAdd.Builder probeBldr = fact.buildFlowAdd()
            .setTableId(probFlowTable)
            .setPriority(4)
            .setMatch(
                probFlow.getMatch(fact))
            .setInstructions(
                getProbeDestInstructions(fact, sampPort));

        destSw.write(
            toProbingFlow(
                eternalFlow(
                    withNoOverlap(
                        probeBldr)))
                            .build());
    }

    @SuppressWarnings( "unchecked" )
    private static <T extends OFFlowMod.Builder> T toProbingFlow( T builder )
    {
        return (T)builder.setCookie(PROBING_FLOW_COOKIE).setCookieMask(U64.NO_MASK);
    }

    private static List<OFInstruction> getProbeDestInstructions( OFFactory fact, OFPort outPort )
    {
        return Collections.singletonList(
            fact.instructions().applyActions(Collections.singletonList(
                fact.actions().output(outPort, Unsigned.MAX_SHORT))));
    }

    private static List<OFAction> getProbeSrcOutputActions( OFFactory fact, OFPort outPort, OFPort sampPort )
    {
        return Arrays.asList(
            fact.actions().output(sampPort, Unsigned.MAX_SHORT),
            fact.actions().output(outPort, Unsigned.MAX_SHORT));
    }

    static final class SwitchCommException extends Exception
    {
        private static final long serialVersionUID = 1L;

        private SwitchCommException( String message )
        {
            // stack trace will not be written on throwing
            super(message, null, true, false);
        }
    }

    private SwitchComm()
    {
        // not used
    }
}
