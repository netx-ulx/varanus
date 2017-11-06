package net.varanus.sdncontroller.monitoring.submodules.sampling.internal;


import static net.varanus.sdncontroller.monitoring.submodules.sampling.internal.Utils.destinationSamplingTableID;
import static net.varanus.sdncontroller.monitoring.submodules.sampling.internal.Utils.getPopDestinationTagActions;
import static net.varanus.sdncontroller.monitoring.submodules.sampling.internal.Utils.getPushSourceTagActions;
import static net.varanus.sdncontroller.monitoring.submodules.sampling.internal.Utils.getRegisterSourceTagActions;
import static net.varanus.sdncontroller.monitoring.submodules.sampling.internal.Utils.getSamplingTags;
import static net.varanus.sdncontroller.monitoring.submodules.sampling.internal.Utils.minEffectiveApplicationTableID;
import static net.varanus.sdncontroller.monitoring.submodules.sampling.internal.Utils.newDestinationMatch;
import static net.varanus.sdncontroller.monitoring.submodules.sampling.internal.Utils.newSourceMatch;
import static net.varanus.sdncontroller.monitoring.submodules.sampling.internal.Utils.newUntaggedDestinationMatch;
import static net.varanus.sdncontroller.monitoring.submodules.sampling.internal.Utils.sourceSamplingTableID;
import static net.varanus.sdncontroller.monitoring.submodules.sampling.internal.Utils.tagRegisterTableID;
import static net.varanus.sdncontroller.monitoring.submodules.sampling.internal.Utils.toDefaultSamplingFlow;
import static net.varanus.sdncontroller.monitoring.submodules.sampling.internal.Utils.toEphemeralSamplingFlow;
import static net.varanus.sdncontroller.monitoring.submodules.sampling.internal.Utils.withDestinationParams;
import static net.varanus.sdncontroller.monitoring.submodules.sampling.internal.Utils.withDestinationPort;
import static net.varanus.sdncontroller.monitoring.submodules.sampling.internal.Utils.withSourceParams;
import static net.varanus.sdncontroller.monitoring.submodules.sampling.internal.Utils.withSourcePort;
import static net.varanus.sdncontroller.util.LinkUtils.getDestDirection;
import static net.varanus.sdncontroller.util.LinkUtils.getSrcDirection;
import static net.varanus.util.openflow.OFMessageUtils.eternalFlow;
import static net.varanus.util.openflow.OFMessageUtils.notifyRemoval;
import static net.varanus.util.openflow.OFMessageUtils.withNoOverlap;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import javax.annotation.ParametersAreNonnullByDefault;

import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFFlowDelete;
import org.projectfloodlight.openflow.protocol.OFFlowDeleteStrict;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFFlowModify;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TableId;
import org.projectfloodlight.openflow.types.VlanVid;
import org.slf4j.Logger;

import net.floodlightcontroller.core.IOFSwitch;
import net.varanus.sdncontroller.logging.Logging;
import net.varanus.sdncontroller.types.DatapathLink;
import net.varanus.sdncontroller.types.EndpointKind;
import net.varanus.sdncontroller.types.FlowedLink;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.collect.builder.ImmutableListBuilder;
import net.varanus.util.functional.FunctionUtils;
import net.varanus.util.lang.Unsigned;
import net.varanus.util.openflow.NodePortUtils;
import net.varanus.util.openflow.types.DirectedNodePort;
import net.varanus.util.openflow.types.Flow;
import net.varanus.util.openflow.types.FlowDirectedNodePort;
import net.varanus.util.openflow.types.NodeId;
import net.varanus.util.openflow.types.PortId;
import net.varanus.util.openflow.types.TrafficDirection;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
final class SwitchComm
{
    private static final Logger LOG = Logging.monitoring.sampling.LOG;

    static void installSamplingFlows( Optional<IOFSwitch> optSrcSw,
                                      Optional<IOFSwitch> optDestSw,
                                      FlowedLink flowedLink,
                                      VlanVid sampTag,
                                      BarrierHandler barrierHandler,
                                      Function<OFVersion, Optional<OFAction>> srcActionProv,
                                      Function<OFVersion, Optional<OFAction>> destActionProv )
        throws SwitchCommException
    {
        if (optSrcSw.isPresent() && optDestSw.isPresent()) {
            IOFSwitch destSw = optDestSw.get();
            IOFSwitch srcSw = optSrcSw.get();

            Match origMatch = flowedLink.getFlow().getMatch();
            OFPort inPort = flowedLink.getDestPort().getOFPort();
            OFPort outPort = flowedLink.getSrcPort().getOFPort();

            Match destMatch = withDestinationParams(inPort, sampTag, origMatch).build();
            FlowDirectedNodePort destEndpoint = FlowDirectedNodePort.of(
                flowedLink.getDestNode(),
                flowedLink.getDestPort(),
                Flow.of(destMatch),
                getDestDirection());

            Match srcMatch = withSourceParams(outPort, sampTag, origMatch).build();
            FlowDirectedNodePort srcEndpoint = FlowDirectedNodePort.of(
                flowedLink.getSrcNode(),
                flowedLink.getSrcPort(),
                Flow.of(srcMatch),
                getSrcDirection());

            installSampFlow(
                destSw,
                destEndpoint,
                getDestSamplingTableInstructions(
                    destSw,
                    true,
                    destActionProv),
                barrierHandler);

            installUntaggedDestinationFlow(
                destSw,
                destEndpoint.unflowed(),
                getDestSamplingTableInstructions(
                    destSw,
                    false,
                    destActionProv),
                barrierHandler);

            installSampFlow(
                srcSw,
                srcEndpoint,
                getSrcSamplingTableInstructions(
                    srcSw.getOFFactory(),
                    srcEndpoint.getPortId().getOFPort(),
                    srcActionProv),
                barrierHandler);

        }
        else if (!(optSrcSw.isPresent() || optDestSw.isPresent())) {
            throw new SwitchCommException(String.format(
                "Could not install sampling flows due to failed communication with SCR and DEST switches: %s and %s",
                flowedLink.getSrcNode(),
                flowedLink.getDestNode()));
        }
        else if (!optSrcSw.isPresent()) {
            throw new SwitchCommException(String.format(
                "Could not install sampling flows due to failed communication with SRC switch: %s",
                flowedLink.getSrcNode()));
        }
        else { // if (!optDestSw.isPresent()) {
            throw new SwitchCommException(String.format(
                "Could not install sampling flows due to failed communication with DEST switch: %s",
                flowedLink.getDestNode()));
        }
    }

    private static void installSampFlow( IOFSwitch sw,
                                         FlowDirectedNodePort endpoint,
                                         List<OFInstruction> instructions,
                                         BarrierHandler barrierHandler )
    {
        LOG.trace("Installing sampling flow for flowed-link endpoint {}", endpoint);

        TableId table = getSamplingTableID(sw, endpoint.getDirection());
        OFFactory fact = sw.getOFFactory();
        Flow flow = endpoint.getFlow();

        OFFlowAdd.Builder sampFlowBldr = fact.buildFlowAdd()
            .setTableId(table)
            .setPriority(3)
            .setMatch(flow.getMatch(fact))
            .setInstructions(instructions);

        sw.write(
            toEphemeralSamplingFlow(
                eternalFlow(
                    withNoOverlap(
                        notifyRemoval(
                            sampFlowBldr))))
                                .build());

        LOG.trace("Sending barrier request to switch {}", endpoint.getNodeId());
        barrierHandler.sendRequest(sw);
    }

    private static void installUntaggedDestinationFlow( IOFSwitch sw,
                                                        DirectedNodePort endpoint,
                                                        List<OFInstruction> instructions,
                                                        BarrierHandler barrierHandler )
    {
        LOG.trace("Installing untagged destination flow for link endpoint {}", endpoint);

        TableId table = destinationSamplingTableID(sw);
        OFFactory fact = sw.getOFFactory();
        Match match = newUntaggedDestinationMatch(endpoint.getPortId().getOFPort(), fact).build();

        OFFlowAdd.Builder untaggedFlowBldr = fact.buildFlowAdd()
            .setTableId(table)
            .setPriority(2)
            .setMatch(match)
            .setInstructions(instructions);

        sw.write(
            toEphemeralSamplingFlow(
                eternalFlow(
                    withNoOverlap(
                        untaggedFlowBldr)))
                            .build());

        LOG.trace("Sending barrier request to switch {}", endpoint.getNodeId());
        barrierHandler.sendRequest(sw);
    }

    static void removeSamplingFlows( Optional<IOFSwitch> optSrcSw,
                                     Optional<IOFSwitch> optDestSw,
                                     FlowedLink flowedLink )
    {
        Match origMatch = flowedLink.getFlow().getMatch();
        OFPort inPort = flowedLink.getDestPort().getOFPort();
        OFPort outPort = flowedLink.getSrcPort().getOFPort();

        if (optSrcSw.isPresent()) {
            Match srcMatch = withSourcePort(outPort, origMatch).build();
            FlowDirectedNodePort srcEndpoint = FlowDirectedNodePort.of(
                flowedLink.getSrcNode(),
                flowedLink.getSrcPort(),
                Flow.of(srcMatch),
                getSrcDirection());

            removeSampFlow(optSrcSw.get(), srcEndpoint);
        }

        if (optDestSw.isPresent()) {
            Match destMatch = withDestinationPort(inPort, origMatch).build();
            FlowDirectedNodePort destEndpoint = FlowDirectedNodePort.of(
                flowedLink.getDestNode(),
                flowedLink.getDestPort(),
                Flow.of(destMatch),
                getDestDirection());

            removeSampFlow(optDestSw.get(), destEndpoint);
        }
    }

    private static void removeSampFlow( IOFSwitch sw, FlowDirectedNodePort endpoint )
    {
        TableId table = getSamplingTableID(sw, endpoint.getDirection());
        Flow flow = endpoint.getFlow();

        OFFactory fact = sw.getOFFactory();
        OFFlowDelete.Builder removeMonBldr = fact.buildFlowDelete()
            .setTableId(table)
            .setMatch(flow.getMatch(fact));

        // this will only delete ephemeral sampling flows
        // the default ones that only pop tags are not removed
        sw.write(
            toEphemeralSamplingFlow(
                removeMonBldr)
                    .build());
    }

    static void updateTag( Optional<IOFSwitch> optSrcSw, NodeId srcNodeId, PortId outPortId, VlanVid tag )
        throws SwitchCommException
    {
        if (optSrcSw.isPresent()) {
            IOFSwitch srcSw = optSrcSw.get();
            OFFactory fact = srcSw.getOFFactory();
            OFFlowModify.Builder regTagBldr = configureRegisterTagEntry(
                srcSw,
                outPortId.getOFPort(),
                tag,
                fact.buildFlowModify());

            LOG.trace("Updating tag in switch {} to VLAN tag {}", srcNodeId, tag);
            srcSw.write(
                toDefaultSamplingFlow(
                    regTagBldr)
                        .build());
        }
        else {
            throw new SwitchCommException(String.format(
                "Could not update tag due to failed communication with switch: %s", srcNodeId));
        }
    }

    static void purgeSamplingTables( Optional<IOFSwitch> optSw,
                                     NodeId nodeId,
                                     VlanVid sampTag,
                                     BarrierHandler barrierHandler )
        throws SwitchCommException
    {
        if (optSw.isPresent()) {
            IOFSwitch sw = optSw.get();

            OFFactory fact = sw.getOFFactory();

            LOG.trace("Purging source sampling flows in switch {} for VLAN tag {}", nodeId, sampTag);
            purgeSamplingTable(
                sw,
                sourceSamplingTableID(sw),
                newSourceMatch(sampTag, fact).build());

            LOG.trace("Purging untagged destination sampling flows in switch {}", nodeId);
            purgeSamplingTable(
                sw,
                destinationSamplingTableID(sw),
                newUntaggedDestinationMatch(fact).build());

            LOG.trace("Purging destination sampling flows in switch {} for VLAN tag {}", nodeId, sampTag);
            purgeSamplingTable(
                sw,
                destinationSamplingTableID(sw),
                newDestinationMatch(sampTag, fact).build());

            LOG.trace("Sending barrier request to switch {}", nodeId);
            barrierHandler.sendRequest(sw);
        }
        else {
            throw new SwitchCommException(String.format(
                "Could not purge sampling tables due to failed communication with switch: %s", nodeId));
        }
    }

    private static void purgeSamplingTable( IOFSwitch sw, TableId table, Match match )
    {
        OFFactory fact = sw.getOFFactory();

        OFFlowDelete.Builder purgeBldr = fact.buildFlowDelete()
            .setTableId(table)
            .setMatch(match);

        // this will only delete ephemeral sampling flows
        // the default ones that only pop tags are not removed
        sw.write(
            toEphemeralSamplingFlow(
                purgeBldr)
                    .build());
    }

    static void sendBarrier( Optional<IOFSwitch> optSw, NodeId nodeId, BarrierHandler barrierHandler )
        throws SwitchCommException
    {
        if (optSw.isPresent()) {
            LOG.trace("Sending barrier request to switch {}", nodeId);
            barrierHandler.sendRequest(optSw.get());
        }
        else {
            throw new SwitchCommException(String.format(
                "Could not send barrier due to failed communication with switch %s", nodeId));
        }
    }

    static void handleAddedSwitch( Optional<IOFSwitch> optSw, NodeId nodeId ) throws SwitchCommException
    {
        if (optSw.isPresent()) {
            IOFSwitch sw = optSw.get();
            setupInitialDestSamplingTable(sw, nodeId);
            setupInitialTagRegisterTable(sw, nodeId);
            setupInitialSrcSamplingTable(sw, nodeId);
            setupInitialSwitchPorts(sw, nodeId);
        }
        else {
            throw new SwitchCommException(String.format(
                "Could not handle added switch due to failed communication with switch %s", nodeId));
        }
    }

    private static void setupInitialDestSamplingTable( IOFSwitch sw, NodeId nodeId )
    {
        LOG.debug("Setting up destination sampling table for switch {}", nodeId);

        TableId destMonTable = destinationSamplingTableID(sw);
        OFFactory fact = sw.getOFFactory();

        // First, clear the destination sampling table
        OFFlowDelete.Builder clearBldr = fact.buildFlowDelete()
            .setTableId(destMonTable)
            .setMatch(
                fact.matchWildcardAll());

        sw.write(
            toDefaultSamplingFlow(
                clearBldr)
                    .build());

        // Finally, setup a default entry for the non-tagged traffic
        OFFlowAdd.Builder defaultBldr = fact.buildFlowAdd()
            .setTableId(destMonTable)
            .setPriority(0)
            .setMatch(
                fact.matchWildcardAll())
            .setInstructions(
                getDefDestSamplingTableInstructions(sw, false));

        sw.write(
            toDefaultSamplingFlow(
                eternalFlow(
                    withNoOverlap(
                        defaultBldr)))
                            .build());
    }

    private static void setupInitialTagRegisterTable( IOFSwitch sw, NodeId nodeId )
    {
        LOG.debug("Setting up tag register table for switch {}", nodeId);

        TableId tagRegTable = tagRegisterTableID(sw);
        OFFactory fact = sw.getOFFactory();

        // Clear the tag register table
        OFFlowDelete.Builder clearBldr = fact.buildFlowDelete()
            .setTableId(tagRegTable)
            .setMatch(
                fact.matchWildcardAll());

        sw.write(
            toDefaultSamplingFlow(
                clearBldr)
                    .build());
    }

    private static void setupInitialSrcSamplingTable( IOFSwitch sw, NodeId nodeId )
    {
        LOG.debug("Setting up source sampling table for switch {}", nodeId);

        TableId srcMonTable = sourceSamplingTableID(sw);
        OFFactory fact = sw.getOFFactory();

        // Clear the source sampling table
        OFFlowDelete.Builder clearBldr = fact.buildFlowDelete()
            .setTableId(srcMonTable)
            .setMatch(
                fact.matchWildcardAll());

        sw.write(
            toDefaultSamplingFlow(
                clearBldr)
                    .build());
    }

    private static void setupInitialSwitchPorts( IOFSwitch sw, NodeId nodeId )
    {
        LOG.debug("Setting up default flow entries in tag register table for initial ports of switch {}", nodeId);

        for (OFPortDesc portDesc : sw.getPorts()) {
            handleNewSwitchPort(sw, nodeId, portDesc.getPortNo());
        }
    }

    private static void handleNewSwitchPort( IOFSwitch sw, NodeId nodeId, OFPort port )
    {
        if (!NodePortUtils.isSpecialPort(port)) {
            // Create a default flow entry in the tag register table that simply
            // outputs the packet
            // via this port, without registering any tag.

            // When this port is later known to be part of an inter-switch link,
            // the flow entry for
            // that port is modified to register a tag and go to the next table.
            LOG.trace(
                "Setting up default flow entry in tag register table for output port {} in switch {}",
                port,
                nodeId);

            OFFactory fact = sw.getOFFactory();
            OFFlowAdd.Builder noTagBldr = configureDefRegisterTagEntry(
                sw,
                port,
                fact.buildFlowAdd());

            sw.write(
                eternalFlow(
                    withNoOverlap(
                        toDefaultSamplingFlow(noTagBldr)
                            .build())));

        }
    }

    static void handleAddedSwitchPort( Optional<IOFSwitch> optSw, NodeId nodeId, PortId portId )
        throws SwitchCommException
    {
        if (optSw.isPresent()) {
            handleNewSwitchPort(optSw.get(), nodeId, portId.getOFPort());
        }
        else {
            throw new SwitchCommException(String.format(
                "Could not handle added switch port due to failed communication with switch %s", nodeId));
        }
    }

    static void handleAddedLink( Optional<IOFSwitch> optSrcSw, Optional<IOFSwitch> optDestSw, DatapathLink link )
        throws SwitchCommException
    {
        if (optSrcSw.isPresent() && optDestSw.isPresent()) {
            setupLinkedDestSamplingTable(optDestSw.get(), link);
            setupLinkedSrcSamplingTable(optSrcSw.get(), link);
        }
        else if (!(optSrcSw.isPresent() || optDestSw.isPresent())) {
            throw new SwitchCommException(String.format(
                "Could not handle added link due to failed communication with SRC and DEST switches: %s and %s",
                link.getSrcNode(),
                link.getDestNode()));
        }
        else if (!optSrcSw.isPresent()) {
            throw new SwitchCommException(String.format(
                "Could not handle added link due to failed communication with SRC switch: %s", link.getSrcNode()));
        }
        else { // if (!optDestSw.isPresent()) {
            throw new SwitchCommException(String.format(
                "Could not handle added link due to failed communication with DEST switch: %s",
                link.getDestNode()));
        }
    }

    private static void setupLinkedDestSamplingTable( IOFSwitch destSwitch, DatapathLink link )
    {
        // Setup a VLAN tag popping entry for traffic containing each of the
        // sampling VLAN tags for this in-port
        LOG.trace(
            "Adding destination sampling table entries for link endpoint {}",
            link.getDestEndpoint());

        OFFactory fact = destSwitch.getOFFactory();
        TableId destMonTable = destinationSamplingTableID(destSwitch);
        OFPort inPort = link.getDestPort().getOFPort();

        for (VlanVid tag : getSamplingTags()) {
            OFFlowAdd.Builder popVlanBldr = fact.buildFlowAdd()
                .setTableId(destMonTable)
                .setPriority(1)
                .setMatch(
                    newDestinationMatch(inPort, tag, fact)
                        .build())
                .setInstructions(
                    getDefDestSamplingTableInstructions(destSwitch, true));

            destSwitch.write(
                toDefaultSamplingFlow(
                    eternalFlow(
                        withNoOverlap(
                            popVlanBldr)))
                                .build());
        }
    }

    private static void setupLinkedSrcSamplingTable( IOFSwitch srcSwitch, DatapathLink link )
    {
        // Write a default source sampling table entry associated with the
        // source port to push the vlan tag that is written in the previous
        // table (see below) and outputs the packet through that port
        LOG.trace(
            "Setting up default source sampling table entry for link endpoint {}",
            link.getSrcEndpoint());

        OFFactory fact = srcSwitch.getOFFactory();
        TableId srcMonTable = sourceSamplingTableID(srcSwitch);
        OFPort outPort = link.getSrcPort().getOFPort();

        OFFlowAdd.Builder outputBldr = fact.buildFlowAdd()
            .setTableId(srcMonTable)
            .setPriority(1)
            .setMatch(
                newSourceMatch(outPort, fact)
                    .build())
            .setInstructions(
                getDefSrcSamplingTableInstructions(fact, outPort));

        srcSwitch.write(
            toDefaultSamplingFlow(
                eternalFlow(
                    withNoOverlap(
                        outputBldr)))
                            .build());

        // Modify the tag register table entry associated with the source port
        // to register VLAN tags before outputting packets through that port
        // (done in the source sampling table)
        LOG.trace(
            "Setting up tag register table entry for link endpoint {}",
            link.getSrcEndpoint());

        VlanVid tag = getSamplingTags().get(0);
        OFFlowModify.Builder regTagBldr = configureRegisterTagEntry(
            srcSwitch,
            outPort,
            tag,
            fact.buildFlowModify());

        srcSwitch.write(
            toDefaultSamplingFlow(
                regTagBldr)
                    .build());
    }

    static void handleRemovedLink( Optional<IOFSwitch> optSrcSw, Optional<IOFSwitch> optDestSw, DatapathLink link )
    {
        if (optSrcSw.isPresent())
            revertSrcSamplingTable(optSrcSw.get(), link);
        if (optDestSw.isPresent())
            revertDestSamplingTable(optDestSw.get(), link);
    }

    private static void revertSrcSamplingTable( IOFSwitch srcSw, DatapathLink link )
    {
        // Modify the tag register table entry associated with the source port
        // to no longer register VLAN tags before outputting packets through
        // that port
        LOG.trace(
            "Reverting tag register table entry for link endpoint {}",
            link.getSrcEndpoint());

        OFFactory fact = srcSw.getOFFactory();
        TableId srcMonTable = sourceSamplingTableID(srcSw);
        OFPort outPort = link.getSrcPort().getOFPort();

        OFFlowModify.Builder noTagBldr = configureDefRegisterTagEntry(
            srcSw,
            outPort,
            fact.buildFlowModify());

        srcSw.write(
            toDefaultSamplingFlow(
                noTagBldr)
                    .build());

        // Remove the default source sampling table entry associated with the
        // source port
        LOG.trace(
            "Removing default source sampling table entry for link endpoint {}",
            link.getSrcEndpoint());

        OFFlowDeleteStrict.Builder outputBldr = fact.buildFlowDeleteStrict()
            .setTableId(srcMonTable)
            .setPriority(1)
            .setMatch(
                newSourceMatch(outPort, fact)
                    .build());

        srcSw.write(
            toDefaultSamplingFlow(
                outputBldr)
                    .build());
    }

    private static void revertDestSamplingTable( IOFSwitch destSw, DatapathLink link )
    {
        // Remove the VLAN tag popping entry for traffic containing each of the
        // sampling VLAN tags for this in-port
        LOG.trace(
            "Removing destination sampling table entries for link endpoint {}",
            link.getDestEndpoint());

        OFFactory fact = destSw.getOFFactory();
        TableId destMonTable = destinationSamplingTableID(destSw);
        OFPort inPort = link.getDestPort().getOFPort();

        for (VlanVid tag : getSamplingTags()) {
            OFFlowDeleteStrict.Builder popVlanBldr = fact.buildFlowDeleteStrict()
                .setTableId(destMonTable)
                .setPriority(1)
                .setMatch(
                    newDestinationMatch(inPort, tag, fact)
                        .build());

            destSw.write(
                toDefaultSamplingFlow(
                    popVlanBldr)
                        .build());
        }
    }

    private static <T extends OFFlowMod.Builder> T configureDefRegisterTagEntry( IOFSwitch sw,
                                                                                 OFPort outPort,
                                                                                 T builder )
    {
        OFFactory fact = sw.getOFFactory();
        TableId tagRegTable = tagRegisterTableID(sw);

        builder
            .setTableId(tagRegTable)
            .setPriority(1)
            .setMatch(
                newSourceMatch(outPort, fact)
                    .build())
            .setInstructions(
                Collections.singletonList(
                    fact.instructions().applyActions(
                        Collections.singletonList(
                            fact.actions().buildOutput()
                                .setPort(outPort)
                                .build()))));

        return builder;
    }

    private static <T extends OFFlowMod.Builder> T configureRegisterTagEntry( IOFSwitch sw,
                                                                              OFPort outPort,
                                                                              VlanVid tag,
                                                                              T builder )
    {
        OFFactory fact = sw.getOFFactory();
        TableId tagRegTable = tagRegisterTableID(sw);
        TableId srcMonTable = sourceSamplingTableID(sw);

        builder
            .setTableId(tagRegTable)
            .setPriority(1)
            .setMatch(
                newSourceMatch(outPort, fact)
                    .build())
            .setInstructions(
                Arrays.asList(
                    fact.instructions().applyActions(getRegisterSourceTagActions(fact, tag)),
                    fact.instructions().gotoTable(srcMonTable)));

        return builder;
    }

    private static TableId getSamplingTableID( IOFSwitch sw, TrafficDirection direction )
    {
        switch (EndpointKind.ofDirection(direction)) {
            case SOURCE:
                return sourceSamplingTableID(sw);

            case DESTINATION:
                return destinationSamplingTableID(sw);

            default:
                throw new AssertionError("unexpected enum value");
        }
    }

    private static List<OFInstruction> getDefDestSamplingTableInstructions( IOFSwitch sw, boolean popTag )
    {
        return _destMonInstructions(sw, popTag, FunctionUtils.asFunction(Optional::empty));
    }

    private static List<OFInstruction> getDestSamplingTableInstructions( IOFSwitch sw,
                                                                         boolean popTag,
                                                                         Function<OFVersion,
                                                                                  Optional<OFAction>> actionProv )
    {
        return _destMonInstructions(sw, popTag, actionProv);
    }

    private static List<OFInstruction> _destMonInstructions( IOFSwitch sw,
                                                             boolean popTag,
                                                             Function<OFVersion, Optional<OFAction>> actionProv )
    {
        OFFactory fact = sw.getOFFactory();
        TableId firstAppTable = minEffectiveApplicationTableID(sw);

        return ImmutableListBuilder.<OFInstruction>create()
            .add(
                fact.instructions().applyActions(
                    ImmutableListBuilder.<OFAction>create()
                        .addAllIf(popTag, getPopDestinationTagActions(fact))
                        .addIfPresent(actionProv.apply(fact.getVersion()))
                        .build()))
            .add(fact.instructions().gotoTable(firstAppTable))
            .build();
    }

    private static List<OFInstruction> getDefSrcSamplingTableInstructions( OFFactory fact, OFPort outPort )
    {
        return _srcMonInstructions(fact, outPort, FunctionUtils.asFunction(Optional::empty));
    }

    private static List<OFInstruction> getSrcSamplingTableInstructions( OFFactory fact,
                                                                        OFPort outPort,
                                                                        Function<OFVersion,
                                                                                 Optional<OFAction>> actionProv )
    {
        return _srcMonInstructions(fact, outPort, actionProv);
    }

    private static List<OFInstruction> _srcMonInstructions( OFFactory fact,
                                                            OFPort outPort,
                                                            Function<OFVersion, Optional<OFAction>> actionProv )
    {
        return Collections.singletonList(
            fact.instructions().applyActions(
                ImmutableListBuilder.<OFAction>create()
                    .addIfPresent(actionProv.apply(fact.getVersion()))
                    .addAll(getPushSourceTagActions(fact))
                    .add(fact.actions().output(outPort, Unsigned.MAX_SHORT))
                    .build()));
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
