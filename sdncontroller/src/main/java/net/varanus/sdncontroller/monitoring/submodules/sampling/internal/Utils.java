package net.varanus.sdncontroller.monitoring.submodules.sampling.internal;


import static net.varanus.util.openflow.OFMessageUtils.containsActionWithType;
import static net.varanus.util.openflow.OFMessageUtils.containsInstructionWithType;
import static net.varanus.util.openflow.OFMessageUtils.getActionsWithoutTypes;
import static net.varanus.util.openflow.OFMessageUtils.getFirstActionWithType;
import static net.varanus.util.openflow.OFMessageUtils.getInstructionWithType;
import static net.varanus.util.openflow.OFMessageUtils.getInstructionsWithoutTypes;
import static net.varanus.util.openflow.OFMessageUtils.numberOfActionsWithType;
import static org.projectfloodlight.openflow.protocol.OFActionType.GROUP;
import static org.projectfloodlight.openflow.protocol.OFActionType.OUTPUT;
import static org.projectfloodlight.openflow.protocol.OFInstructionType.APPLY_ACTIONS;
import static org.projectfloodlight.openflow.protocol.OFInstructionType.GOTO_TABLE;
import static org.projectfloodlight.openflow.protocol.OFInstructionType.WRITE_ACTIONS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.CheckForNull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFInstructionType;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructionApplyActions;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructionGotoTable;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructionWriteActions;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.oxm.OFOxm;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.OFMetadata;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.OFVlanVidMatch;
import org.projectfloodlight.openflow.types.TableId;
import org.projectfloodlight.openflow.types.U32;
import org.projectfloodlight.openflow.types.U64;
import org.projectfloodlight.openflow.types.VlanVid;

import com.google.common.base.Preconditions;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.util.AppCookie;
import net.varanus.sdncontroller.util.Fields;
import net.varanus.sdncontroller.util.LinkUtils;
import net.varanus.sdncontroller.util.NiciraExtensionsUtils;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.openflow.MatchFieldUtils;
import net.varanus.util.openflow.MatchUtils;
import net.varanus.util.openflow.NodePortUtils;
import net.varanus.util.openflow.types.DirectedPortId;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
final class Utils
{
    private static final int SAMPLING_FLOWS_APP_ID    = 300;
    private static final int DEFAULT_FLOW_COOKIE_ID   = 0x4f3a24e7;
    private static final int EPHEMERAL_FLOW_COOKIE_ID = DEFAULT_FLOW_COOKIE_ID + 1;

    // this cookie is for identifying sampling flow-mods but is not used by
    // switches in modify/delete table operations (a null cookie mask is
    // provided, which is ignored in all flow-mod operations)
    private static final U64 DEFAULT_FLOW_COOKIE;
    private static final U64 DEFAULT_FLOW_COOKIE_MASK = U64.ZERO;

    // this cookie is for identifying sampling flow-mods and is used by
    // switches in modify/delete table operations to filter which flows are
    // changed/removed (an all-ones cookie mask is provided, which is used by
    // flow-modify and flow-delete operations to exactly match the cookie value)
    private static final U64 EPHEMERAL_FLOW_COOKIE;
    private static final U64 EPHEMERAL_FLOW_COOKIE_MASK = U64.NO_MASK;

    static {
        AppCookie.registerApp(SAMPLING_FLOWS_APP_ID, "varanus-sampling-flows");
        DEFAULT_FLOW_COOKIE = AppCookie.makeCookie(SAMPLING_FLOWS_APP_ID, DEFAULT_FLOW_COOKIE_ID);
        EPHEMERAL_FLOW_COOKIE = AppCookie.makeCookie(SAMPLING_FLOWS_APP_ID, EPHEMERAL_FLOW_COOKIE_ID);
    }

    private static final int DST_SAMPLING_TABLE_ID      = 0;
    private static final int MIN_EFFECTIVE_APP_TABLE_ID = DST_SAMPLING_TABLE_ID + 1;
    private static final int MIN_TAG_REGISTER_TABLE_ID  = MIN_EFFECTIVE_APP_TABLE_ID + 1;
    private static final int MIN_SRC_SAMPLING_TABLE_ID  = MIN_TAG_REGISTER_TABLE_ID + 1;
    private static final int MIN_REQUIRED_TABLES        = MIN_SRC_SAMPLING_TABLE_ID + 1;

    // offsets from number of flow tables
    private static final int SRC_SAMPLING_TABLE_ID_OFFSET      = -251;
    private static final int TAG_REGISTER_TABLE_ID_OFFSET      = SRC_SAMPLING_TABLE_ID_OFFSET - 1;
    private static final int MAX_EFFECTIVE_APP_TABLE_ID_OFFSET = TAG_REGISTER_TABLE_ID_OFFSET - 1;

    private static final List<VlanVid> SAMPLING_VLAN_TAGS = initSamplingVlanTags(4094 - 7, 4094);

    private static List<VlanVid> initSamplingVlanTags( int firstTag, int lastTag )
    {
        if (firstTag < Fields.MIN_VLAN_VID.getVlan()) {
            throw new IllegalArgumentException(
                String.format(
                    "first tag (%d) is below the minimum allowed (%d)",
                    firstTag,
                    Fields.MIN_VLAN_VID.getVlan()));
        }
        if (lastTag > Fields.MAX_VLAN_VID.getVlan()) {
            throw new IllegalArgumentException(
                String.format(
                    "last tag (%d) is above the maximum allowed (%d)",
                    lastTag,
                    Fields.MAX_VLAN_VID.getVlan()));
        }
        if (firstTag > lastTag) {
            throw new IllegalArgumentException(
                String.format(
                    "first tag (%d) is larger than last tag (%d)",
                    firstTag,
                    lastTag));
        }

        VlanVid[] tags = new VlanVid[(lastTag - firstTag) + 1];
        for (int tag = firstTag; tag <= lastTag; tag++) {
            tags[tag - firstTag] = VlanVid.ofVlan(tag);
        }

        return Collections.unmodifiableList(Arrays.asList(tags));
    }

    // ================ TABLE IDs ================ //

    static TableId minEffectiveApplicationTableID( IOFSwitch sw )
    {
        checkRequiredTables(sw, "minimum effective application table");
        return TableId.of(MIN_EFFECTIVE_APP_TABLE_ID);
    }

    static TableId maxEffectiveApplicationTableID( IOFSwitch sw )
    {
        checkRequiredTables(sw, "maximum effective application table");
        return TableId.of(sw.getNumTables() + MAX_EFFECTIVE_APP_TABLE_ID_OFFSET);
    }

    static TableId destinationSamplingTableID( IOFSwitch sw )
    {
        checkRequiredTables(sw, "destination sampling table");
        return TableId.of(DST_SAMPLING_TABLE_ID);
    }

    static TableId tagRegisterTableID( IOFSwitch sw )
    {
        checkRequiredTables(sw, "tag register table");
        return TableId.of(sw.getNumTables() + TAG_REGISTER_TABLE_ID_OFFSET);
    }

    static TableId sourceSamplingTableID( IOFSwitch sw )
    {
        checkRequiredTables(sw, "source sampling table");
        return TableId.of(sw.getNumTables() + SRC_SAMPLING_TABLE_ID_OFFSET);
    }

    private static void checkRequiredTables( IOFSwitch sw, String forWhat )
    {
        if (sw.getNumTables() < MIN_REQUIRED_TABLES) {
            throw new IllegalStateException(
                String.format(
                    "Required at least %d flow tables for %s, but switch %s only has %d tables",
                    MIN_REQUIRED_TABLES,
                    forWhat,
                    sw.getId(),
                    sw.getNumTables()));
        }
    }

    // ================ SAMPLING TAGS ================ //

    static List<VlanVid> getSamplingTags()
    {
        return SAMPLING_VLAN_TAGS;
    }

    // ================ SAMPLABLE FLOW ================ //

    static void validateSamplableMatch( Match match ) throws IllegalArgumentException
    {
        Preconditions.checkArgument(
            isValidSamplableMatch(match),
            "invalid samplable match: %s",
            getReasonForInvalidSamplableMatch(match));
    }

    static boolean isValidSamplableMatch( Match match )
    {
        return match.isFullyWildcarded(MatchField.IN_PORT)
               && match.isFullyWildcarded(MatchField.METADATA)
               && match.isFullyWildcarded(MatchField.VLAN_VID)
               && match.isFullyWildcarded(MatchField.NX_REG0);
    }

    static String getReasonForInvalidSamplableMatch( Match match )
    {
        if (!match.isFullyWildcarded(MatchField.IN_PORT)) {
            return "match cannot specify an in-port field";
        }
        else if (!match.isFullyWildcarded(MatchField.METADATA)) {
            return "match cannot specify a metadata field";
        }
        else if (!match.isFullyWildcarded(MatchField.VLAN_VID)) {
            return "match cannot specify a VLAN tag ID";
        }
        else if (!match.isFullyWildcarded(MatchField.NX_REG0)) {
            return "match cannot specify nicira register 0";
        }
        else {
            return "";
        }
    }

    // ================ SAMPLING FLOW ================ //

    // ---------------- DESTINATION PORT/TAG ---------------- //

    static @CheckForNull DirectedPortId getDestinationPort( Match match )
    {
        OFPort port = match.get(MatchField.IN_PORT);
        return (port != null) ? DirectedPortId.of(port, LinkUtils.getDestDirection())
                              : null;
    }

    static @CheckForNull VlanVid getDestinationTag( Match match )
    {
        OFVlanVidMatch vid = match.get(MatchField.VLAN_VID);
        return (vid != null) ? vid.getVlanVid()
                             : null;
    }

    static Match.Builder withDestinationPort( OFPort inPort, Match.Builder builder )
    {
        if (inPort == null || inPort.equals(OFPort.ANY)) {
            return withoutDestinationPort(builder);
        }
        else {
            return builder.setExact(
                MatchField.IN_PORT,
                inPort);
        }
    }

    static Match.Builder withDestinationTag( VlanVid tag, Match.Builder builder )
    {
        if (tag == null) {
            return withoutDestinationTag(builder);
        }
        else {
            return builder.setExact(
                MatchField.VLAN_VID,
                OFVlanVidMatch.ofVlanVid(tag));
        }
    }

    static Match.Builder withDestinationParams( OFPort inPort, VlanVid tag, Match.Builder builder )
    {
        return withDestinationTag(tag, withDestinationPort(inPort, builder));
    }

    static Match.Builder withoutDestinationPort( Match.Builder builder )
    {
        return builder.wildcard(MatchField.IN_PORT);
    }

    static Match.Builder withoutDestinationTag( Match.Builder builder )
    {
        return builder.wildcard(MatchField.VLAN_VID);
    }

    static Match.Builder withoutDestinationParams( Match.Builder builder )
    {
        return withoutDestinationTag(withoutDestinationPort(builder));
    }

    static Match.Builder untaggedDestinationMatch( Match.Builder builder )
    {
        return builder.setExact(MatchField.VLAN_VID, OFVlanVidMatch.UNTAGGED);
    }

    // ---------------- SOURCE PORT/TAG ---------------- //

    static @CheckForNull DirectedPortId getSourcePort( Match match )
    {
        OFMetadata meta = match.get(MatchField.METADATA);
        return (meta != null) ? DirectedPortId.ofInt(U32.t(meta.getValue().getValue()), LinkUtils.getSrcDirection())
                              : null;
    }

    static @CheckForNull VlanVid getSourceTag( Match match )
    {
        U32 reg0 = match.get(MatchField.NX_REG0);
        return (reg0 != null) ? VlanVid.ofVlan(reg0.getRaw())
                              : null;
    }

    static Match.Builder withSourcePort( OFPort outPort, Match.Builder builder )
    {
        if (outPort == null || outPort.equals(OFPort.ANY)) {
            return withoutSourcePort(builder);
        }
        else {
            return builder.setExact(
                MatchField.METADATA,
                OFMetadata.of(U64.of(U32.f(outPort.getPortNumber()))));
        }
    }

    static Match.Builder withSourceTag( VlanVid tag, Match.Builder builder )
    {
        if (tag == null) {
            return withoutSourceTag(builder);
        }
        else {
            return builder.setExact(
                MatchField.NX_REG0,
                U32.ofRaw(tag.getVlan()));
        }
    }

    static Match.Builder withSourceParams( OFPort outPort, VlanVid tag, Match.Builder builder )
    {
        return withSourceTag(tag, withSourcePort(outPort, builder));
    }

    static Match.Builder withoutSourcePort( Match.Builder builder )
    {
        return builder.wildcard(MatchField.METADATA);
    }

    static Match.Builder withoutSourceTag( Match.Builder builder )
    {
        return builder.wildcard(MatchField.NX_REG0);
    }

    static Match.Builder withoutSourceParams( Match.Builder builder )
    {
        return withoutSourceTag(withoutSourcePort(builder));
    }

    // ---------------- VARIOUS UTILS ---------------- //

    static Match.Builder newDestinationMatch( OFPort inPort, OFFactory fact )
    {
        Match.Builder builder = fact.buildMatch();
        return withDestinationPort(inPort, builder);
    }

    static Match.Builder newDestinationMatch( VlanVid tag, OFFactory fact )
    {
        Match.Builder builder = fact.buildMatch();
        return withDestinationTag(tag, builder);
    }

    static Match.Builder newDestinationMatch( OFPort inPort, VlanVid tag, OFFactory fact )
    {
        Match.Builder builder = fact.buildMatch();
        return withDestinationParams(inPort, tag, builder);
    }

    static Match.Builder newUntaggedDestinationMatch( OFFactory fact )
    {
        Match.Builder builder = fact.buildMatch();
        return untaggedDestinationMatch(builder);
    }

    static Match.Builder newUntaggedDestinationMatch( OFPort inPort, OFFactory fact )
    {
        return untaggedDestinationMatch(newDestinationMatch(inPort, fact));
    }

    static Match.Builder withDestinationPort( OFPort inPort, Match match )
    {
        Match.Builder builder = MatchUtils.builderFrom(match);
        return withDestinationPort(inPort, builder);
    }

    static Match.Builder withDestinationTag( VlanVid tag, Match match )
    {
        Match.Builder builder = MatchUtils.builderFrom(match);
        return withDestinationTag(tag, builder);
    }

    static Match.Builder withDestinationParams( OFPort inPort, VlanVid tag, Match match )
    {
        Match.Builder builder = MatchUtils.builderFrom(match);
        return withDestinationParams(inPort, tag, builder);
    }

    static Match.Builder withoutDestinationPort( Match match )
    {
        Match.Builder builder = MatchUtils.builderFrom(match);
        return withoutDestinationPort(builder);
    }

    static Match.Builder withoutDestinationTag( Match match )
    {
        Match.Builder builder = MatchUtils.builderFrom(match);
        return withoutDestinationTag(builder);
    }

    static Match.Builder withoutDestinationParams( Match match )
    {
        Match.Builder builder = MatchUtils.builderFrom(match);
        return withoutDestinationParams(builder);
    }

    static Match.Builder untaggedDestinationMatch( Match match )
    {
        Match.Builder builder = MatchUtils.builderFrom(match);
        return untaggedDestinationMatch(builder);
    }

    static Match.Builder newSourceMatch( OFPort outPort, OFFactory fact )
    {
        Match.Builder builder = fact.buildMatch();
        return withSourcePort(outPort, builder);
    }

    static Match.Builder newSourceMatch( VlanVid tag, OFFactory fact )
    {
        Match.Builder builder = fact.buildMatch();
        return withSourceTag(tag, builder);
    }

    static Match.Builder newSourceMatch( OFPort outPort, VlanVid tag, OFFactory fact )
    {
        Match.Builder builder = fact.buildMatch();
        return withSourceParams(outPort, tag, builder);
    }

    static Match.Builder withSourcePort( OFPort outPort, Match match )
    {
        Match.Builder builder = MatchUtils.builderFrom(match);
        return withSourcePort(outPort, builder);
    }

    static Match.Builder withSourceTag( VlanVid tag, Match match )
    {
        Match.Builder builder = MatchUtils.builderFrom(match);
        return withSourceTag(tag, builder);
    }

    static Match.Builder withSourceParams( OFPort outPort, VlanVid tag, Match match )
    {
        Match.Builder builder = MatchUtils.builderFrom(match);
        return withSourceParams(outPort, tag, builder);
    }

    static Match.Builder withoutSourcePort( Match match )
    {
        Match.Builder builder = MatchUtils.builderFrom(match);
        return withoutSourcePort(builder);
    }

    static Match.Builder withoutSourceTag( Match match )
    {
        Match.Builder builder = MatchUtils.builderFrom(match);
        return withoutSourceTag(builder);
    }

    static Match.Builder withoutSourceParams( Match match )
    {
        Match.Builder builder = MatchUtils.builderFrom(match);
        return withoutSourceParams(builder);
    }

    // ================ SAMPLING ACTIONS ================= //

    static List<OFAction> getRegisterSourceTagActions( OFFactory fact, VlanVid tag )
    {
        U32 regValue = U32.ofRaw(tag.getVlan());
        MatchField<U32> regField = NiciraExtensionsUtils.registerField(0);
        OFOxm<U32> regOxm = fact.oxms().fromValue(regValue, regField);
        return Collections.singletonList(fact.actions().setField(regOxm));
    }

    static List<OFAction> getPushSourceTagActions( OFFactory fact )
    {
        MatchField<?> srcField = NiciraExtensionsUtils.registerField(0);
        MatchField<?> destField = MatchField.VLAN_VID;
        int tagBitSize = MatchFieldUtils.bitSize(MatchField.VLAN_VID);

        return Arrays.asList(
            fact.actions().pushVlan(EthType.VLAN_FRAME),
            NiciraExtensionsUtils.copyFieldAction(fact, srcField, 0, destField, 0, tagBitSize));
    }

    static List<OFAction> getPopDestinationTagActions( OFFactory fact )
    {
        return Collections.singletonList(fact.actions().popVlan());
    }

    // ================ FLOW-MOD ================ //

    static boolean isDefaultSamplingFlow( OFFlowMod flowMod )
    {
        U64 cookie = flowMod.getCookie();
        U64 mask = flowMod.getCookieMask();

        return (cookie.equals(DEFAULT_FLOW_COOKIE) && mask.equals(DEFAULT_FLOW_COOKIE_MASK));
    }

    static boolean isEphemeralSamplingFlow( OFFlowMod flowMod )
    {
        U64 cookie = flowMod.getCookie();
        U64 mask = flowMod.getCookieMask();

        return (cookie.equals(EPHEMERAL_FLOW_COOKIE) && mask.equals(EPHEMERAL_FLOW_COOKIE_MASK));
    }

    @SuppressWarnings( "unchecked" )
    static <T extends OFFlowMod.Builder> T toDefaultSamplingFlow( T builder )
    {
        return (T)builder.setCookie(DEFAULT_FLOW_COOKIE).setCookieMask(DEFAULT_FLOW_COOKIE_MASK);
    }

    @SuppressWarnings( "unchecked" )
    static <T extends OFFlowMod.Builder> T toEphemeralSamplingFlow( T builder )
    {
        return (T)builder.setCookie(EPHEMERAL_FLOW_COOKIE).setCookieMask(EPHEMERAL_FLOW_COOKIE_MASK);
    }

    static <T extends OFFlowMod> T convertAppFlowMod( IOFSwitch sw, T appFlowMod )
        throws IllegalArgumentException
    {
        checkAppFlowCookie(appFlowMod);
        checkAppFlowCookieMask(appFlowMod);

        switch (appFlowMod.getCommand()) {
            case ADD:
            case MODIFY:
            case MODIFY_STRICT:
                return convertAppFlowAddOrModify(sw, appFlowMod);

            case DELETE:
            case DELETE_STRICT:
                return convertAppFlowDelete(sw, appFlowMod);

            default:
                throw new AssertionError("unexpected enum value");
        }
    }

    private static void checkAppFlowCookie( OFFlowMod appFlowMod ) throws IllegalArgumentException
    {
        if (appFlowMod.getCookie().equals(DEFAULT_FLOW_COOKIE)) {
            throw new IllegalArgumentException(
                String.format(
                    "cookie value %s is not supported (is default sampling flow cookie)",
                    appFlowMod.getCookie()));
        }

        if (appFlowMod.getCookie().equals(EPHEMERAL_FLOW_COOKIE)) {
            throw new IllegalArgumentException(
                String.format(
                    "cookie value %s is not supported (is ephemeral sampling flow cookie)",
                    appFlowMod.getCookie()));
        }
    }

    private static void checkAppFlowCookieMask( OFFlowMod appFlowMod ) throws IllegalArgumentException
    {
        switch (appFlowMod.getCommand()) {
            case ADD:
            // nothing to check since cookie mask is ignored
            break;

            case MODIFY:
            case MODIFY_STRICT:
            case DELETE:
            case DELETE_STRICT:
                if (!(appFlowMod.getCookieMask().equals(U64.FULL_MASK)
                      || appFlowMod.getCookieMask().equals(U64.NO_MASK))) {
                    throw new IllegalArgumentException(
                        String.format(
                            "cookie mask value %s is not supported (only all-zeros or all-ones are permitted)",
                            appFlowMod.getCookieMask()));
                }
            break;

            default:
                throw new AssertionError("unexpected enum value");
        }
    }

    /*
     * Common code for flow add/modify/modify-strict
     */
    private static <T extends OFFlowMod> T convertAppFlowAddOrModify( IOFSwitch sw, T appFlowMod )
        throws IllegalArgumentException
    {
        OFFactory fact = sw.getOFFactory();
        List<OFInstruction> appInstructions = appFlowMod.getInstructions();

        // first check if there are any unsupported instructions
        if (containsInstructionWithType(appInstructions, OFInstructionType.WRITE_METADATA)) {
            throw new IllegalArgumentException("write-metadata instruction is not supported");
        }

        // check if there are any output actions in a write-actions instruction,
        // and reject if so
        OFInstructionWriteActions writeActions = getInstructionWithType(appInstructions, WRITE_ACTIONS);
        if (writeActions != null && containsActionWithType(writeActions.getActions(), OUTPUT)) {
            throw new IllegalArgumentException("write-actions with output action is not supported");
        }

        // handle apply-actions and output action separately
        OFInstructionApplyActions applyActions = getInstructionWithType(appInstructions, APPLY_ACTIONS);
        List<OFAction> appActions = (applyActions != null) ? applyActions.getActions()
                                                           : Collections.<OFAction>emptyList();

        // we can only pass one value in the metadata (i.e. one output port)
        if (numberOfActionsWithType(appActions, OUTPUT) > 1) {
            throw new IllegalArgumentException("more than one output action is not supported");
        }

        // check for unsupported actions
        if (containsActionWithType(appActions, GROUP)) {
            throw new IllegalArgumentException("group action is not supported");
        }

        // retrieve the first output action, if it exists
        OFActionOutput output = getFirstActionWithType(appActions, OUTPUT);

        // handle goto-table separately
        OFInstructionGotoTable gotoTable = getInstructionWithType(appInstructions, GOTO_TABLE);

        // to be initialized exactly once later
        final List<OFInstruction> newInstructions;

        // check this special case because we only support
        // one output action xor one goto-table instruction
        if (output != null && gotoTable != null) {
            throw new IllegalArgumentException(
                "output action and goto-table instruction are not supported together");
        }
        else if (output != null) {
            OFPort outPort = output.getPort();
            if (outPort.equals(OFPort.IN_PORT)) {
                throw new IllegalArgumentException("output port IN_PORT is not supported"); // FIXME?
            }
            else if (outPort.equals(OFPort.ALL)) {
                throw new IllegalArgumentException("output port ALL is not supported"); // FIXME?
            }
            else if (outPort.equals(OFPort.FLOOD)) {
                throw new IllegalArgumentException("output port FLOOD is not supported"); // FIXME?
            }
            else if (NodePortUtils.isSpecialPort(outPort)) {
                // if the output port is another kind of special port, then do
                // nothing
                newInstructions = appInstructions;
            }
            else {
                // remove the output action from the list
                List<OFAction> purgedActions = getActionsWithoutTypes(appActions, OUTPUT);
                if (purgedActions.isEmpty()) {
                    // if there are no actions left, do not include the
                    // apply-actions instruction
                    newInstructions = getInstructionsWithoutTypes(appInstructions, APPLY_ACTIONS);
                }
                else {
                    // if there are other actions besides output, we need to
                    // replace the old apply-actions instruction with a new one
                    // containing the purged actions
                    newInstructions = new ArrayList<>();
                    for (OFInstruction instr : appInstructions) {
                        if (instr.getType().equals(APPLY_ACTIONS)) {
                            newInstructions.add(fact.instructions().applyActions(purgedActions));
                        }
                        else {
                            newInstructions.add(instr);
                        }
                    }
                }

                // write the output port in the metadata to be matched in
                // subsequent tables
                U64 meta = U64.of(outPort.getPortNumber());
                newInstructions.add(fact.instructions().writeMetadata(meta, U64.NO_MASK));

                // add a goto-table instruction pointing to the tag register
                // table
                newInstructions.add(fact.instructions().gotoTable(tagRegisterTableID(sw)));
            }
        }
        else if (gotoTable != null) {
            // must check if the next table is not a sampling table
            TableId newAppTable = convertAppTableID(sw, gotoTable.getTableId(), "goto-table");

            // replace the old goto-table instruction with the new one
            newInstructions = new ArrayList<>();
            for (OFInstruction instr : appInstructions) {
                if (instr.getType().equals(GOTO_TABLE)) {
                    newInstructions.add(fact.instructions().gotoTable(newAppTable));
                }
                else {
                    newInstructions.add(instr);
                }
            }
        }
        else {
            // if there is no goto-table instruction nor output action, then do
            // nothing
            newInstructions = appInstructions;
        }

        // must offset the application table
        TableId newAppTable = convertAppTableID(sw, appFlowMod.getTableId(), appFlowMod.getCommand());
        OFFlowMod.Builder flowAddBldr = appFlowMod.createBuilder()
            .setTableId(newAppTable)
            .setInstructions(newInstructions);

        @SuppressWarnings( "unchecked" )
        T newFlowMod = (T)flowAddBldr.build();
        return newFlowMod;
    }

    /*
     * Common code for flow delete/delete-strict
     */
    private static <T extends OFFlowMod> T convertAppFlowDelete( IOFSwitch sw, T appFlowDel )
    {
        TableId newAppTable = convertAppTableID(sw, appFlowDel.getTableId(), appFlowDel.getCommand());
        OFFlowMod.Builder flowDelBldr = appFlowDel.createBuilder()
            .setTableId(newAppTable);

        // FIXME: an app-flow will never have an output action because we
        // replace it with a write-metadata instruction, so for now we remove
        // this condition
        flowDelBldr.setOutPort(OFPort.ANY);

        /*
         * XXX This only makes sense if we can match on flows that have
         * write_metadata instructions with specific metadata value, which we
         * cannot, so...
         */
        // OFPort outPort = flowDelBldr.getOutPort();
        // if (!(outPort == null || outPort.equals(OFPort.ANY) ||
        // Fields.isSpecialPort(outPort))) {
        // Match.Builder matchBldr =
        // MatchUtils.builderFromMatch(flowDelBldr.getMatch());
        // if (!matchBldr.isFullyWildcarded(MatchField.METADATA)) {
        // throw new IllegalArgumentException(
        // "matching simultaneously on metadata and output port is
        // unsupported");
        // }
        //
        // // match on metadata instead of out-port
        // OFMetadata meta = OFMetadata.ofRaw(outPort.getPortNumber());
        // flowDelBldr
        // .setMatch(matchBldr
        // .setExact(MatchField.METADATA, meta)
        // .build())
        // .setOutPort(OFPort.ANY); // clear the output port match
        // }

        @SuppressWarnings( "unchecked" )
        T newFlowDel = (T)flowDelBldr.build();
        return newFlowDel;
    }

    private static TableId convertAppTableID( IOFSwitch sw, TableId appTable, Object forWhat )
        throws IllegalArgumentException
    {
        if (appTable.equals(TableId.ALL)) {
            throw new IllegalArgumentException(
                String.format(
                    "table ID of ALL %sis not supported",
                    forWhatPhrase(forWhat)));
        }
        else {
            final int minEffectAppTableID = minEffectiveApplicationTableID(sw).getValue();
            final int maxEffectAppTableID = maxEffectiveApplicationTableID(sw).getValue();

            int convertedAppTableID = minEffectAppTableID + appTable.getValue();
            if (convertedAppTableID - maxEffectAppTableID > 0) {
                throw new IllegalArgumentException(
                    String.format(
                        "table ID of %d %sis not supported (max allowed ID is %d)",
                        appTable.getValue(),
                        forWhatPhrase(forWhat),
                        (maxEffectAppTableID - minEffectAppTableID)));
            }

            return TableId.of(convertedAppTableID);
        }
    }

    private static String forWhatPhrase( Object forWhat )
    {
        return (forWhat == null ? "" : String.format("for %s ", forWhat));
    }

    // ================ PACKET-OUT ================ //

    static OFPacketOut convertAppPacketOut( @SuppressWarnings( "unused" ) IOFSwitch sw, OFPacketOut packetOut )
    {
        // FIXME for now, allow packet-outs to always exit without VLAN tags
        return packetOut;
    }

    private Utils()
    {
        // not used
    }
}
