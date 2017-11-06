package net.varanus.util.openflow;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.projectfloodlight.openflow.protocol.OFActionType;
import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFFlowModCommand;
import org.projectfloodlight.openflow.protocol.OFFlowModFlags;
import org.projectfloodlight.openflow.protocol.OFFlowRemoved;
import org.projectfloodlight.openflow.protocol.OFInstructionType;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFPortDesc;
import org.projectfloodlight.openflow.protocol.OFPortDescProp;
import org.projectfloodlight.openflow.protocol.OFPortDescPropEthernet;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.instruction.OFInstruction;
import org.projectfloodlight.openflow.protocol.instruction.OFInstructionApplyActions;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmMplsLabel;
import org.projectfloodlight.openflow.protocol.oxm.OFOxmMplsLabelMasked;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.OFVlanVidMatch;
import org.projectfloodlight.openflow.types.U32;
import org.projectfloodlight.openflow.types.VlanVid;

import net.varanus.util.lang.Comparables;


/**
 * 
 */
public final class OFMessageUtils
{
    public static OFPacketOut packetOutFromPacketIn( OFPacketIn pIn, OFPort outPort, int maxLen )
    {
        OFPacketOut.Builder builder = OFFactories.getFactory(pIn.getVersion()).buildPacketOut();

        builder.setBufferId(pIn.getBufferId());
        builder.setInPort(getInPort(pIn));

        OFAction outputAction = OFFactories.getFactory(pIn.getVersion()).actions().output(outPort, maxLen);
        builder.setActions(Collections.singletonList(outputAction));

        if (pIn.getBufferId().equals(OFBufferId.NO_BUFFER)) {
            builder.setData(pIn.getData());
        }

        return builder.build();
    }

    public static <T extends OFFlowMod.Builder> T newFlowModBuilder( OFFactory fact, OFFlowModCommand flowModCommand )
    {
        @SuppressWarnings( "unchecked" )
        T builder = (T)_newFlowModBuilder(fact, flowModCommand);
        return builder;
    }

    private static OFFlowMod.Builder _newFlowModBuilder( OFFactory fact, OFFlowModCommand flowModCommand )
    {
        switch (flowModCommand) {
            case ADD:
                return fact.buildFlowAdd();

            case DELETE:
                return fact.buildFlowDelete();

            case DELETE_STRICT:
                return fact.buildFlowDeleteStrict();

            case MODIFY:
                return fact.buildFlowModify();

            case MODIFY_STRICT:
                return fact.buildFlowModifyStrict();

            default:
                throw new AssertionError("unknown enum type");
        }
    }

    public static <T extends OFFlowMod> T eternalFlow( T flowMod )
    {
        @SuppressWarnings( "unchecked" )
        T castMod = (T)eternalFlow(flowMod.createBuilder()).build();
        return castMod;
    }

    public static <T extends OFFlowMod.Builder> T eternalFlow( T flowModBuilder )
    {
        @SuppressWarnings( "unchecked" )
        T castBuilder = (T)flowModBuilder.setHardTimeout(0).setIdleTimeout(0);
        return castBuilder;
    }

    public static OFFlowAdd withNoOverlap( OFFlowAdd flowAdd )
    {
        return withNoOverlap(flowAdd.createBuilder()).build();
    }

    public static OFFlowAdd.Builder withNoOverlap( OFFlowAdd.Builder flowAddBuilder )
    {
        Set<OFFlowModFlags> flags = flowAddBuilder.getFlags();
        if (flags == null || flags.isEmpty()) {
            flags = EnumSet.noneOf(OFFlowModFlags.class);
        }
        else {
            flags = EnumSet.copyOf(flags);
        }

        flags.add(OFFlowModFlags.CHECK_OVERLAP);
        flowAddBuilder.setFlags(flags);
        return flowAddBuilder;
    }

    public static OFFlowAdd notifyRemoval( OFFlowAdd flowAdd )
    {
        return notifyRemoval(flowAdd.createBuilder()).build();
    }

    public static OFFlowAdd.Builder notifyRemoval( OFFlowAdd.Builder flowAddBuilder )
    {
        Set<OFFlowModFlags> flags = flowAddBuilder.getFlags();
        if (flags == null || flags.isEmpty()) {
            flags = EnumSet.noneOf(OFFlowModFlags.class);
        }
        else {
            flags = EnumSet.copyOf(flags);
        }

        flags.add(OFFlowModFlags.SEND_FLOW_REM);
        flowAddBuilder.setFlags(flags);
        return flowAddBuilder;
    }

    public static String flowRemovedToString( OFFlowRemoved flowRem )
    {
        StringBuilder b = new StringBuilder(flowRem.getClass().getSimpleName());
        b.append("xid=").append(flowRem.getXid());
        b.append(", ");
        b.append("cookie=").append(flowRem.getCookie());
        b.append(", ");
        b.append("priority=").append(flowRem.getPriority());
        b.append(", ");
        // b.append("reason=").append(getFlowRemovedReason(flowRem));
        b.append("reason=").append(flowRem.getReason());
        b.append(", ");
        if (Comparables.aGTb(flowRem.getVersion(), OFVersion.OF_10)) {
            b.append("tableId=").append(flowRem.getTableId());
            b.append(", ");
        }
        b.append("durationSec=").append(flowRem.getDurationSec());
        b.append(", ");
        b.append("durationNsec=").append(flowRem.getDurationNsec());
        b.append(", ");
        b.append("idleTimeout=").append(flowRem.getIdleTimeout());
        b.append(", ");
        if (Comparables.aGTb(flowRem.getVersion(), OFVersion.OF_11)) {
            b.append("hardTimeout=").append(flowRem.getHardTimeout());
            b.append(", ");
        }
        b.append("packetCount=").append(flowRem.getPacketCount());
        b.append(", ");
        b.append("byteCount=").append(flowRem.getByteCount());
        b.append(", ");
        b.append("match=").append(flowRem.getMatch());
        b.append(")");
        return b.toString();
    }

    // public static OFFlowRemovedReason getFlowRemovedReason( OFFlowRemoved
    // flowRem )
    // {
    // switch (flowRem.getVersion()) {
    // case OF_10:
    // return
    // OFFlowRemovedReasonSerializerVer10.ofWireValue((byte)flowRem.getReason());
    //
    // case OF_11:
    // return
    // OFFlowRemovedReasonSerializerVer11.ofWireValue((byte)flowRem.getReason());
    //
    // case OF_12:
    // return
    // OFFlowRemovedReasonSerializerVer12.ofWireValue((byte)flowRem.getReason());
    //
    // case OF_13:
    // return
    // OFFlowRemovedReasonSerializerVer13.ofWireValue((byte)flowRem.getReason());
    //
    // case OF_14:
    // return
    // OFFlowRemovedReasonSerializerVer14.ofWireValue((byte)flowRem.getReason());
    //
    // default:
    // throw new UnsupportedOperationException(
    // String.format(
    // "OpenFlow version %s unsupported for OFFlowRemovedReasonSerializerVerXX",
    // flowRem.getVersion()));
    // }
    // }

    public static int numberOfInstructionsWithType( Iterable<? extends OFInstruction> instructions,
                                                    OFInstructionType type )
    {
        int count = 0;
        for (OFInstruction instr : instructions) {
            if (type.equals(instr.getType())) {
                count++;
            }
        }

        return count;
    }

    public static boolean containsInstructionWithType( Iterable<? extends OFInstruction> instructions,
                                                       OFInstructionType type )
    {
        return getInstructionWithType(instructions, type) != null;
    }

    @SuppressWarnings( "unchecked" )
    public static <T extends OFInstruction> T getInstructionWithType( Iterable<? extends OFInstruction> instructions,
                                                                      OFInstructionType type )
    {
        /*
         * OpenFlow specifies a set of instructions, so we only return the first
         * instruction we find (since we are dealing with a list here).
         */

        for (OFInstruction instr : instructions) {
            if (type.equals(instr.getType())) {
                return (T)instr;
            }
        }

        return null;
    }

    public static List<OFInstruction> getInstructionsWithTypes( Iterable<? extends OFInstruction> instructions,
                                                                OFInstructionType... types )
    {
        List<OFInstructionType> typeList = Arrays.asList(types);
        if (typeList.isEmpty()) {
            return getInstructionsWithTypes(instructions, Collections.<OFInstructionType>emptySet());
        }
        else {
            return getInstructionsWithTypes(instructions, EnumSet.copyOf(typeList));
        }
    }

    public static List<OFInstruction> getInstructionsWithTypes( Iterable<? extends OFInstruction> instructions,
                                                                Set<OFInstructionType> types )
    {
        List<OFInstruction> filtered = new ArrayList<>();
        for (OFInstruction inst : instructions) {
            if (types.contains(inst.getType())) {
                filtered.add(inst);
            }
        }

        return filtered;
    }

    public static List<OFInstruction> getInstructionsWithoutTypes( Iterable<? extends OFInstruction> instructions,
                                                                   OFInstructionType... excludedTypes )
    {
        List<OFInstructionType> typeList = Arrays.asList(excludedTypes);
        if (typeList.isEmpty()) {
            return getInstructionsWithoutTypes(instructions, Collections.<OFInstructionType>emptySet());
        }
        else {
            return getInstructionsWithoutTypes(instructions, EnumSet.copyOf(typeList));
        }
    }

    public static List<OFInstruction> getInstructionsWithoutTypes( Iterable<? extends OFInstruction> instructions,
                                                                   Set<OFInstructionType> excludedTypes )
    {
        List<OFInstruction> filtered = new ArrayList<>();
        for (OFInstruction inst : instructions) {
            if (!excludedTypes.contains(inst.getType())) {
                filtered.add(inst);
            }
        }

        return filtered;
    }

    public static int numberOfActionsWithType( Iterable<? extends OFAction> actions, OFActionType type )
    {
        int count = 0;
        for (OFAction action : actions) {
            if (type.equals(action.getType())) {
                count++;
            }
        }

        return count;
    }

    public static boolean containsActionWithType( Iterable<? extends OFAction> actions, OFActionType type )
    {
        return getFirstActionWithType(actions, type) != null;
    }

    @SuppressWarnings( "unchecked" )
    public static <T extends OFAction> T getFirstActionWithType( Iterable<? extends OFAction> actions,
                                                                 OFActionType type )
    {
        for (OFAction action : actions) {
            if (type.equals(action.getType())) {
                return (T)action;
            }
        }

        return null;
    }

    public static List<OFAction> getActionsWithTypes( Iterable<? extends OFAction> actions,
                                                      OFActionType... types )
    {
        List<OFActionType> typeList = Arrays.asList(types);
        if (typeList.isEmpty()) {
            return getActionsWithTypes(actions, Collections.<OFActionType>emptySet());
        }
        else {
            return getActionsWithTypes(actions, EnumSet.copyOf(typeList));
        }
    }

    public static List<OFAction> getActionsWithTypes( Iterable<? extends OFAction> actions,
                                                      Set<OFActionType> types )
    {
        List<OFAction> filtered = new ArrayList<>();
        for (OFAction action : actions) {
            if (types.contains(action.getType())) {
                filtered.add(action);
            }
        }

        return filtered;
    }

    public static List<OFAction> getActionsWithoutTypes( Iterable<? extends OFAction> actions,
                                                         OFActionType... excludedTypes )
    {
        List<OFActionType> typeList = Arrays.asList(excludedTypes);
        if (typeList.isEmpty()) {
            return getActionsWithoutTypes(actions, Collections.<OFActionType>emptySet());
        }
        else {
            return getActionsWithoutTypes(actions, EnumSet.copyOf(typeList));
        }
    }

    public static List<OFAction> getActionsWithoutTypes( Iterable<? extends OFAction> actions,
                                                         Set<OFActionType> excludedTypes )
    {
        List<OFAction> filtered = new ArrayList<>();
        for (OFAction action : actions) {
            if (!excludedTypes.contains(action.getType())) {
                filtered.add(action);
            }
        }

        return filtered;
    }

    public static List<OFAction> getListOfActions( OFFlowMod flowMod )
    {
        switch (flowMod.getVersion()) {
            case OF_10:
                return flowMod.getActions();

            case OF_11: // intended fall-through
            case OF_12: // intended fall-through
            case OF_13: // intended fall-through
            case OF_14:
                OFInstructionApplyActions applyActions = getInstructionWithType(
                    flowMod.getInstructions(),
                    OFInstructionType.APPLY_ACTIONS);
                return (applyActions != null ? applyActions.getActions() : null);

            default:
                throw new UnsupportedOperationException(
                    String.format("OF version %s not supported by this method", flowMod.getVersion()));
        }
    }

    public static List<OFAction> getListOfActions( OFFlowMod.Builder flowModBuilder )
    {
        switch (flowModBuilder.getVersion()) {
            case OF_10:
                return flowModBuilder.getActions();

            case OF_11: // intended fall-through
            case OF_12: // intended fall-through
            case OF_13: // intended fall-through
            case OF_14:
                OFInstructionApplyActions applyActions = getInstructionWithType(
                    flowModBuilder.getInstructions(),
                    OFInstructionType.APPLY_ACTIONS);
                return (applyActions != null ? applyActions.getActions() : null);

            default:
                throw new UnsupportedOperationException(
                    String.format("OF version %s not supported by this method", flowModBuilder.getVersion()));
        }
    }

    public static OFPort getInPort( OFPacketIn pIn )
    {
        switch (pIn.getVersion()) {
            case OF_10: // intended fall-through
            case OF_11:
                return pIn.getInPort();

            case OF_12: // intended fall-through
            case OF_13: // ...
            case OF_14:
                if (pIn.getMatch().supports(MatchField.IN_PORT))
                    return pIn.getMatch().get(MatchField.IN_PORT);
                else
                    throw new UnsupportedOperationException(
                        String.format(
                            "Property in_port is not supported in PacketIn with version %s",
                            pIn.getVersion()));

            default:
                throw new AssertionError("unknown enum value");
        }
    }

    public static long getCurrSpeedFromPortDesc( OFPortDesc portDesc )
    {
        switch (portDesc.getVersion()) {
            case OF_10: // intended fall-through
            case OF_11: // ...
            case OF_12: // ...
            case OF_13:
                return portDesc.getCurrSpeed();

            case OF_14:
                for (OFPortDescProp prop : portDesc.getProperties()) {
                    if (prop instanceof OFPortDescPropEthernet) {
                        return ((OFPortDescPropEthernet)prop).getCurrSpeed();
                    }
                }
                throw new UnsupportedOperationException("OFPortDesc does not support current speed property");

            default:
                throw new AssertionError("unknown enum value");

        }
    }

    public static List<OFAction> addPushVLANFrameSetVID( OFFactory fact, List<OFAction> actions, VlanVid vid )
    {
        actions.add(getPushVLANFrameAction(fact));
        actions.add(getSetVIDAction(fact, vid));
        return actions;
    }

    private static OFAction getPushVLANFrameAction( OFFactory fact )
    {
        return fact.actions().pushVlan(EthType.VLAN_FRAME);
    }

    private static OFAction getSetVIDAction( OFFactory fact, VlanVid vid )
    {
        switch (fact.getVersion()) {
            case OF_10: // intended fall-through
            case OF_11:
                return fact.actions().setVlanVid(vid);

            case OF_12: // intended fall-through
            case OF_13: // ...
            case OF_14:
                return fact.actions().setField(fact.oxms().vlanVid(OFVlanVidMatch.ofVlanVid(vid)));

            default:
                throw new AssertionError("unknown enum value");
        }
    }

    public static List<OFAction> addPushSetUnicastMPLSLabel( OFFactory fact,
                                                             List<OFAction> actions,
                                                             U32 mplsLabel )
    {
        OFOxmMplsLabel oxmMPLSLabel = fact.oxms().mplsLabel(mplsLabel);
        return addPushSetUnicastMPLSLabel(fact, actions, oxmMPLSLabel);
    }

    public static List<OFAction> addPushSetUnicastMPLSLabel( OFFactory fact,
                                                             List<OFAction> actions,
                                                             U32 mplsLabel,
                                                             U32 mplsMask )
    {
        OFOxmMplsLabelMasked oxmMaskedMPLSLabel = fact.oxms().mplsLabelMasked(mplsLabel, mplsMask);
        return addPushSetUnicastMPLSLabel(fact, actions, oxmMaskedMPLSLabel);
    }

    public static List<OFAction> addPushSetUnicastMPLSLabel( OFFactory fact,
                                                             List<OFAction> actions,
                                                             OFOxmMplsLabel mplsLabel )
    {
        actions.add(getPushMPLSLabelAction(fact, EthType.MPLS_UNICAST));
        actions.add(getSetMPLSLabelAction(fact, mplsLabel));
        return actions;
    }

    public static List<OFAction> addPushSetUnicastMPLSLabel( OFFactory fact,
                                                             List<OFAction> actions,
                                                             OFOxmMplsLabelMasked maskedMPLSLabel )
    {
        actions.add(getPushMPLSLabelAction(fact, EthType.MPLS_UNICAST));
        actions.add(getSetMaskedMPLSLabelAction(fact, maskedMPLSLabel));
        return actions;
    }

    private static OFAction getPushMPLSLabelAction( OFFactory fact, EthType ethType )
    {
        return fact.actions().pushMpls(ethType);
    }

    private static OFAction getSetMPLSLabelAction( OFFactory fact, OFOxmMplsLabel mplsLabel )
    {
        switch (fact.getVersion()) {
            case OF_10: // intended fall-through
            case OF_11:
                return fact.actions().setMplsLabel(mplsLabel.getValue().getValue());

            case OF_12: // intended fall-through
            case OF_13: // ...
            case OF_14:
                return fact.actions().setField(mplsLabel);

            default:
                throw new AssertionError("unknown enum value");
        }
    }

    private static OFAction getSetMaskedMPLSLabelAction( OFFactory fact, OFOxmMplsLabelMasked maskedMPLSLabel )
    {
        switch (fact.getVersion()) {
            case OF_10: // intended fall-through
            case OF_11:
                throw new UnsupportedOperationException(
                    String.format("Masked MPLS is not supported in OF version %s", fact.getVersion()));

            case OF_12: // intended fall-through
            case OF_13: // ...
            case OF_14:
                return fact.actions().setField(maskedMPLSLabel);

            default:
                throw new AssertionError("unknown enum value");
        }
    }

    private OFMessageUtils()
    {
        // not used
    }
}
