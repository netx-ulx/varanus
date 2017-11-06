package net.varanus.sdncontroller.util;


import java.util.Arrays;
import java.util.List;

import javax.annotation.Nonnegative;
import javax.annotation.ParametersAreNonnullByDefault;

import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.U32;

import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.checking.Indexables;
import net.varanus.util.openflow.MatchFieldUtils;
import net.varanus.util.openflow.OFOxmUtils;


/**
 * 
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class NiciraExtensionsUtils
{

    public static int registerBitSize()
    {
        return 32;
    }

    public static boolean registerSupportsMask()
    {
        return true;
    }

    public static boolean isRegister( MatchField<?> field )
    {
        switch (field.id) {
            case NX_REG0:
            case NX_REG1:
            case NX_REG2:
            case NX_REG3:
            case NX_REG4:
            case NX_REG5:
            case NX_REG6:
            case NX_REG7:
                return true;

            default:
                return false;
        }
    }

    public static final int maxNumRegisterField()
    {
        return 7;
    }

    public static MatchField<U32> registerField( int i )
    {
        switch (i) {
            case 0:
                return MatchField.NX_REG0;
            case 1:
                return MatchField.NX_REG1;
            case 2:
                return MatchField.NX_REG2;
            case 3:
                return MatchField.NX_REG3;
            case 4:
                return MatchField.NX_REG4;
            case 5:
                return MatchField.NX_REG5;
            case 6:
                return MatchField.NX_REG6;
            case 7:
                return MatchField.NX_REG7;

            default:
                throw new IllegalArgumentException(
                    String.format("unsupported register number: %d", i));
        }
    }

    public static List<MatchField<?>> supportedRegisterFields()
    {
        return Arrays.asList(
            MatchField.NX_REG0,
            MatchField.NX_REG1,
            MatchField.NX_REG2,
            MatchField.NX_REG3,
            MatchField.NX_REG4,
            MatchField.NX_REG5,
            MatchField.NX_REG6,
            MatchField.NX_REG7);
    }

    public static long registerOxmHeader( OFVersion version, MatchField<U32> regField )
    {
        return registerOxmHeader(OFFactories.getFactory(version), regField);
    }

    public static long registerOxmHeader( OFFactory fact, MatchField<U32> regField )
    {
        if (!isRegister(regField))
            throw new IllegalArgumentException("match field is not a register");

        return fact.oxms().fromValue(U32.ZERO, regField).getTypeLen();
    }

    public static OFAction outputFromFieldAction( OFFactory fact, MatchField<?> field )
    {
        int bitSize = fieldBitSize(field);
        return outputFromFieldAction(fact, 0, field, 0, bitSize);
    }

    public static OFAction outputFromFieldAction( OFFactory fact, @Nonnegative int maxLength, MatchField<?> field )
    {
        int bitSize = fieldBitSize(field);
        return outputFromFieldAction(fact, maxLength, field, 0, bitSize);
    }

    public static OFAction outputFromFieldAction( OFFactory fact,
                                                  MatchField<?> field,
                                                  @Nonnegative int offset,
                                                  @Nonnegative int nBits )
    {
        return outputFromFieldAction(fact, 0, field, offset, nBits);
    }

    public static OFAction outputFromFieldAction( OFFactory fact,
                                                  @Nonnegative int maxLength,
                                                  MatchField<?> field,
                                                  @Nonnegative int offset,
                                                  @Nonnegative int nBits )
    {
        Indexables.checkLengthBounds(maxLength, "maximum length");
        Indexables.checkLengthBounds(nBits, "number of bits");
        Indexables.checkOffsetLengthBounds(offset, nBits, fieldBitSize(field));
        if (nBits == 0) {
            throw new IllegalArgumentException("number of bits must be positive");
        }

        return fact.actions().buildNxOutputReg()
            .setMaxLen(maxLength)
            .setSrc(oxmHeader(field, fact))
            .setOfsNbits((offset << 6) | (nBits - 1))
            .build();
    }

    public static OFAction copyFieldAction( OFFactory fact, MatchField<?> srcField, MatchField<?> destField )
    {
        int srcBitSize = fieldBitSize(srcField);
        int destBitSize = fieldBitSize(destField);
        if (srcBitSize > destBitSize) {
            throw new IllegalArgumentException(
                "cannot copy an entire source field to a destination field with a smaller bit size");
        }

        return copyFieldAction(fact, srcField, 0, destField, 0, srcBitSize);
    }

    public static OFAction copyFieldAction( OFFactory fact,
                                            MatchField<?> srcField,
                                            @Nonnegative int srcOffset,
                                            MatchField<?> destField,
                                            @Nonnegative int destOffset,
                                            @Nonnegative int nBits )
    {
        Indexables.checkLengthBounds(nBits, "number of bits");
        Indexables.checkOffsetLengthBounds(srcOffset, nBits, fieldBitSize(srcField));
        Indexables.checkOffsetLengthBounds(destOffset, nBits, fieldBitSize(destField));

        return fact.actions().buildNxRegMove()
            .setSrc(oxmHeader(srcField, fact))
            .setSrcOffset(srcOffset)
            .setDst(oxmHeader(destField, fact))
            .setDstOffset(destOffset)
            .setNBits(nBits)
            .build();
    }

    public static OFAction stackPushAction( OFFactory fact, MatchField<?> field )
    {
        int bitSize = fieldBitSize(field);
        return stackPushAction(fact, field, 0, bitSize);
    }

    public static OFAction stackPushAction( OFFactory fact,
                                            MatchField<?> field,
                                            @Nonnegative int offset,
                                            @Nonnegative int nBits )
    {
        Indexables.checkLengthBounds(nBits, "number of bits");
        Indexables.checkOffsetLengthBounds(offset, nBits, fieldBitSize(field));

        return fact.actions().buildNxStackPush()
            .setField(oxmHeader(field, fact))
            .setOffset(offset)
            .setNBits(nBits)
            .build();
    }

    public static OFAction stackPopAction( OFFactory fact, MatchField<?> field )
    {
        int bitSize = fieldBitSize(field);
        return stackPopAction(fact, field, 0, bitSize);
    }

    public static OFAction stackPopAction( OFFactory fact,
                                           MatchField<?> field,
                                           @Nonnegative int offset,
                                           @Nonnegative int nBits )
    {
        Indexables.checkLengthBounds(nBits, "number of bits");
        Indexables.checkOffsetLengthBounds(offset, nBits, fieldBitSize(field));

        return fact.actions().buildNxStackPop()
            .setField(oxmHeader(field, fact))
            .setOffset(offset)
            .setNBits(nBits)
            .build();
    }

    public static List<OFAction> swapFieldsActions( OFFactory fact,
                                                    MatchField<?> field1,
                                                    MatchField<?> field2 )
    {
        int bitSize1 = fieldBitSize(field1);
        int bitSize2 = fieldBitSize(field2);
        if (bitSize1 != bitSize2) {
            throw new IllegalArgumentException(
                "cannot swap two entire fields with different bit sizes");
        }

        return swapFieldsActions(fact, field1, 0, field2, 0, bitSize1);
    }

    public static List<OFAction> swapFieldsActions( OFFactory fact,
                                                    MatchField<?> field1,
                                                    @Nonnegative int offset1,
                                                    MatchField<?> field2,
                                                    @Nonnegative int offset2,
                                                    @Nonnegative int nBits )
    {
        Indexables.checkLengthBounds(nBits, "number of bits");
        Indexables.checkOffsetLengthBounds(offset1, nBits, fieldBitSize(field1));
        Indexables.checkOffsetLengthBounds(offset2, nBits, fieldBitSize(field2));

        return Arrays.asList(
            stackPushAction(fact, field2, offset2, nBits),
            copyFieldAction(fact, field1, offset1, field2, offset2, nBits),
            stackPopAction(fact, field1, offset1, nBits));
    }

    private static int fieldBitSize( MatchField<?> field )
    {
        return isRegister(field) ? registerBitSize()
                                 : MatchFieldUtils.bitSize(field);
    }

    @SuppressWarnings( "unchecked" )
    private static long oxmHeader( MatchField<?> field, OFFactory fact )
    {
        return isRegister(field) ? registerOxmHeader(fact, (MatchField<U32>)field)
                                 : OFOxmUtils.oxmHeader(field, fact);
    }

    private NiciraExtensionsUtils()
    {
        // not used
    }
}
