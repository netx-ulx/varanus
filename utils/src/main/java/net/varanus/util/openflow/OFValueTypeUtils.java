package net.varanus.util.openflow;


import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.ParametersAreNonnullByDefault;

import org.projectfloodlight.openflow.exceptions.OFParseError;
import org.projectfloodlight.openflow.types.ArpOpcode;
import org.projectfloodlight.openflow.types.BundleId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.ICMPv4Code;
import org.projectfloodlight.openflow.types.ICMPv4Type;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IPv4AddressWithMask;
import org.projectfloodlight.openflow.types.IPv6Address;
import org.projectfloodlight.openflow.types.IPv6AddressWithMask;
import org.projectfloodlight.openflow.types.IPv6FlowLabel;
import org.projectfloodlight.openflow.types.IpDscp;
import org.projectfloodlight.openflow.types.IpEcn;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.Masked;
import org.projectfloodlight.openflow.types.OFBooleanValue;
import org.projectfloodlight.openflow.types.OFGroup;
import org.projectfloodlight.openflow.types.OFMetadata;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.OFValueType;
import org.projectfloodlight.openflow.types.OFVlanVidMatch;
import org.projectfloodlight.openflow.types.TableId;
import org.projectfloodlight.openflow.types.TransportPort;
import org.projectfloodlight.openflow.types.U16;
import org.projectfloodlight.openflow.types.U32;
import org.projectfloodlight.openflow.types.U64;
import org.projectfloodlight.openflow.types.U8;
import org.projectfloodlight.openflow.types.VlanPcp;
import org.projectfloodlight.openflow.types.VlanVid;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.lang.MoreObjects;
import net.varanus.util.lang.SizeOf;
import net.varanus.util.openflow.types.MaskType;
import net.varanus.util.text.StringUtils;


/**
 * 
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
final class OFValueTypeUtils
{
    @SuppressWarnings( "unchecked" )
    static <T extends OFValueType<T>> Masked<T> asMasked( T value, T mask )
    {
        MoreObjects.requireNonNull(value, "value", mask, "mask");

        if ((value instanceof IPv4Address) && (mask instanceof IPv4Address)) {
            return (Masked<T>)IPv4AddressWithMask.of((IPv4Address)value, (IPv4Address)mask);
        }
        else if ((value instanceof IPv6Address) && (mask instanceof IPv6Address)) {
            return (Masked<T>)IPv6AddressWithMask.of((IPv6Address)value, (IPv6Address)mask);
        }
        else {
            return Masked.of(value, mask);
        }
    }

    static <T extends OFValueType<T>> T parseValue( String str, Class<? extends T> typeClass )
        throws IllegalArgumentException
    {
        Objects.requireNonNull(str);

        if (typeClass.equals(ArpOpcode.class)) {
            return typeClass.cast(
                ArpOpcode.of(StringUtils.parseUnsignedInt(str)));
        }
        else if (typeClass.equals(BundleId.class)) {
            return typeClass.cast(
                BundleId.of(StringUtils.parseUnsignedInt(str)));
        }
        else if (typeClass.equals(EthType.class)) {
            return typeClass.cast(
                EthType.of(StringUtils.parseUnsignedInt(str)));
        }
        else if (typeClass.equals(ICMPv4Code.class)) {
            return typeClass.cast(
                ICMPv4Code.of(StringUtils.parseUnsignedShort(str)));
        }
        else if (typeClass.equals(ICMPv4Type.class)) {
            return typeClass.cast(
                ICMPv4Type.of(StringUtils.parseUnsignedShort(str)));
        }
        else if (typeClass.equals(IPv4Address.class)) {
            return typeClass.cast(
                IPv4Address.of(str));
        }
        else if (typeClass.equals(IPv6Address.class)) {
            return typeClass.cast(
                IPv6Address.of(str));
        }
        else if (typeClass.equals(IpDscp.class)) {
            return typeClass.cast(
                IpDscp.of(StringUtils.parseUnsignedByte(str)));
        }
        else if (typeClass.equals(IpEcn.class)) {
            return typeClass.cast(
                IpEcn.of(StringUtils.parseUnsignedByte(str)));
        }
        else if (typeClass.equals(IpProtocol.class)) {
            return typeClass.cast(
                IpProtocol.of(StringUtils.parseUnsignedShort(str)));
        }
        else if (typeClass.equals(IPv6FlowLabel.class)) {
            return typeClass.cast(
                IPv6FlowLabel.of(StringUtils.parseUnsignedInt(str)));
        }
        else if (typeClass.equals(MacAddress.class)) {
            return typeClass.cast(
                MacAddress.of(str));
        }
        else if (typeClass.equals(OFBooleanValue.class)) {
            return typeClass.cast(
                OFBooleanValue.of(StringUtils.convertToBoolean(str)));
        }
        else if (typeClass.equals(OFGroup.class)) {
            final OFGroup group;
            switch (str.toLowerCase()) {
                case "all":
                    group = OFGroup.ALL;
                break;

                case "any":
                    group = OFGroup.ANY;
                break;

                default:
                    group = OFGroup.of(StringUtils.parseUnsignedInt(str));
                break;
            }
            return typeClass.cast(group);
        }
        else if (typeClass.equals(OFMetadata.class)) {
            return typeClass.cast(
                OFMetadata.ofRaw(StringUtils.parseUnsignedLong(str)));
        }
        else if (typeClass.equals(OFPort.class)) {
            final OFPort port;
            switch (str.toLowerCase()) {
                case "all":
                    port = OFPort.ALL;
                break;

                case "any":
                    port = OFPort.ANY;
                break;

                case "controller":
                    port = OFPort.CONTROLLER;
                break;

                case "in_port":
                    port = OFPort.IN_PORT;
                break;

                case "flood":
                    port = OFPort.FLOOD;
                break;

                case "local":
                    port = OFPort.LOCAL;
                break;

                case "normal":
                    port = OFPort.NORMAL;
                break;

                case "table":
                    port = OFPort.TABLE;
                break;

                default:
                    port = OFPort.of(StringUtils.parseUnsignedInt(str));
                break;
            }
            return typeClass.cast(port);
        }
        else if (typeClass.equals(OFVlanVidMatch.class)) {
            return typeClass.cast(
                OFVlanVidMatch.ofVlanVid(parseValue(str, VlanVid.class)));
        }
        else if (typeClass.equals(TableId.class)) {
            return typeClass.cast(
                TableId.of(StringUtils.parseUnsignedInt(str)));
        }
        else if (typeClass.equals(TransportPort.class)) {
            return typeClass.cast(
                TransportPort.of(StringUtils.parseUnsignedInt(str)));
        }
        else if (typeClass.equals(U8.class)) {
            return typeClass.cast(
                U8.ofRaw(StringUtils.parseUnsignedByte(str)));
        }
        else if (typeClass.equals(U16.class)) {
            return typeClass.cast(
                U16.ofRaw(StringUtils.parseUnsignedShort(str)));
        }
        else if (typeClass.equals(U32.class)) {
            return typeClass.cast(
                U32.ofRaw(StringUtils.parseUnsignedInt(str)));
        }
        else if (typeClass.equals(U64.class)) {
            return typeClass.cast(
                U64.ofRaw(StringUtils.parseUnsignedLong(str)));
        }
        else if (typeClass.equals(VlanPcp.class)) {
            return typeClass.cast(
                VlanPcp.of(StringUtils.parseUnsignedByte(str)));
        }
        else if (typeClass.equals(VlanVid.class)) {
            return typeClass.cast(
                VlanVid.ofVlan(StringUtils.parseUnsignedInt(str)));
        }
        else if (Masked.class.isAssignableFrom(typeClass)) {
            throw new UnsupportedOperationException("masked type cannot be parsed");
        }
        else {
            // TODO maybe also put the BSN types
            throw new UnsupportedOperationException("unsupported OF type");
        }
    }

    private static final char MASK_SEPARATOR_CHAR = '/';

    static boolean isMaskedString( String str )
    {
        return str.indexOf(MASK_SEPARATOR_CHAR) != -1;
    }

    @SuppressWarnings( "unchecked" )
    static <T extends OFValueType<T>> Masked<T> parseMasked( String str, Class<? extends T> typeClass )
        throws IllegalArgumentException
    {
        if (!isMaskedString(str))
            throw new IllegalArgumentException("string does not have a masked value");

        if (typeClass.equals(IPv4Address.class)) {
            return (Masked<T>)IPv4AddressWithMask.of(str);
        }
        else if (typeClass.equals(IPv6Address.class)) {
            return (Masked<T>)IPv6AddressWithMask.of(str);
        }
        else if (Masked.class.isAssignableFrom(typeClass)) {
            throw new UnsupportedOperationException("masked type cannot be parsed (use value class instead)");
        }
        else {
            String[] valueAndMask = str.split(String.valueOf(MASK_SEPARATOR_CHAR), 2);
            T value = parseValue(valueAndMask[0], typeClass);
            T mask = parseValue(valueAndMask[1], typeClass);
            return Masked.of(value, mask);
        }
    }

    static int bitSize( Class<? extends OFValueType<?>> typeClass )
    {
        if (typeClass.equals(ArpOpcode.class)) {
            return ofBytes(2);
        }
        else if (typeClass.equals(BundleId.class)) {
            return ofBytes(4);
        }
        else if (typeClass.equals(EthType.class)) {
            return ofBytes(2);
        }
        else if (typeClass.equals(ICMPv4Code.class)) {
            return ofBytes(1);
        }
        else if (typeClass.equals(ICMPv4Type.class)) {
            return ofBytes(1);
        }
        else if (typeClass.equals(IPv4Address.class)) {
            return ofBytes(4);
        }
        else if (typeClass.equals(IPv6Address.class)) {
            return ofBytes(16);
        }
        else if (typeClass.equals(IpDscp.class)) {
            return ofBytes(1);
        }
        else if (typeClass.equals(IpEcn.class)) {
            return ofBytes(1);
        }
        else if (typeClass.equals(IpProtocol.class)) {
            return ofBytes(1);
        }
        else if (typeClass.equals(IPv6FlowLabel.class)) {
            return ofBytes(4);
        }
        else if (typeClass.equals(MacAddress.class)) {
            return ofBytes(6);
        }
        else if (typeClass.equals(OFBooleanValue.class)) {
            return 1;
        }
        else if (typeClass.equals(OFGroup.class)) {
            return ofBytes(4);
        }
        else if (typeClass.equals(OFMetadata.class)) {
            return ofBytes(8);
        }
        else if (typeClass.equals(OFPort.class)) {
            return ofBytes(4);
        }
        else if (typeClass.equals(OFVlanVidMatch.class)) {
            return bitSize(VlanVid.class);
        }
        else if (typeClass.equals(TableId.class)) {
            return ofBytes(1);
        }
        else if (typeClass.equals(TransportPort.class)) {
            return ofBytes(2);
        }
        else if (typeClass.equals(U8.class)) {
            return 8;
        }
        else if (typeClass.equals(U16.class)) {
            return 16;
        }
        else if (typeClass.equals(U32.class)) {
            return 32;
        }
        else if (typeClass.equals(U64.class)) {
            return 64;
        }
        else if (typeClass.equals(VlanPcp.class)) {
            return 3;
        }
        else if (typeClass.equals(VlanVid.class)) {
            return 12;
        }
        else if (Masked.class.isAssignableFrom(typeClass)) {
            throw new UnsupportedOperationException(
                "masked type bit size not supported (multiply the value type by 2)");
        }
        else {
            // TODO maybe also put the BSN types
            throw new UnsupportedOperationException("unsupported OF type");
        }
    }

    static <T extends OFValueType<T>> byte[] toBytes( T type )
    {
        if (type instanceof ArpOpcode) {
            byte[] bytes = new byte[2];
            ((ArpOpcode)type).write2Bytes(asWritableBuf(bytes));
            return bytes;
        }
        else if (type instanceof BundleId) {
            byte[] bytes = new byte[4];
            ((BundleId)type).write4Bytes(asWritableBuf(bytes));
            return bytes;
        }
        else if (type instanceof EthType) {
            byte[] bytes = new byte[2];
            ((EthType)type).write2Bytes(asWritableBuf(bytes));
            return bytes;
        }
        else if (type instanceof ICMPv4Code) {
            byte[] bytes = new byte[1];
            ((ICMPv4Code)type).writeByte(asWritableBuf(bytes));
            return bytes;
        }
        else if (type instanceof ICMPv4Type) {
            byte[] bytes = new byte[1];
            ((ICMPv4Type)type).writeByte(asWritableBuf(bytes));
            return bytes;
        }
        else if (type instanceof IPv4Address) {
            byte[] bytes = new byte[4];
            ((IPv4Address)type).write4Bytes(asWritableBuf(bytes));
            return bytes;
        }
        else if (type instanceof IPv6Address) {
            byte[] bytes = new byte[16];
            ((IPv6Address)type).write16Bytes(asWritableBuf(bytes));
            return bytes;
        }
        else if (type instanceof IpDscp) {
            byte[] bytes = new byte[1];
            ((IpDscp)type).writeByte(asWritableBuf(bytes));
            return bytes;
        }
        else if (type instanceof IpEcn) {
            byte[] bytes = new byte[1];
            ((IpEcn)type).writeByte(asWritableBuf(bytes));
            return bytes;
        }
        else if (type instanceof IpProtocol) {
            byte[] bytes = new byte[1];
            ((IpProtocol)type).writeByte(asWritableBuf(bytes));
            return bytes;
        }
        else if (type instanceof IPv6FlowLabel) {
            byte[] bytes = new byte[4];
            ((IPv6FlowLabel)type).write4Bytes(asWritableBuf(bytes));
            return bytes;
        }
        else if (type instanceof MacAddress) {
            byte[] bytes = new byte[6];
            ((MacAddress)type).write6Bytes(asWritableBuf(bytes));
            return bytes;
        }
        else if (type instanceof OFBooleanValue) {
            byte[] bytes = new byte[1];
            ((OFBooleanValue)type).writeTo(asWritableBuf(bytes));
            return bytes;
        }
        else if (type instanceof OFGroup) {
            byte[] bytes = new byte[4];
            ((OFGroup)type).write4Bytes(asWritableBuf(bytes));
            return bytes;
        }
        else if (type instanceof OFMetadata) {
            byte[] bytes = new byte[8];
            ((OFMetadata)type).write8Bytes(asWritableBuf(bytes));
            return bytes;
        }
        else if (type instanceof OFPort) {
            byte[] bytes = new byte[4];
            ((OFPort)type).write4Bytes(asWritableBuf(bytes));
            return bytes;
        }
        else if (type instanceof OFVlanVidMatch) {
            return toBytes(((OFVlanVidMatch)type).getVlanVid());
        }
        else if (type instanceof TableId) {
            byte[] bytes = new byte[1];
            ((TableId)type).writeByte(asWritableBuf(bytes));
            return bytes;
        }
        else if (type instanceof TransportPort) {
            byte[] bytes = new byte[2];
            ((TransportPort)type).write2Bytes(asWritableBuf(bytes));
            return bytes;
        }
        else if (type instanceof U8) {
            byte[] bytes = new byte[SizeOf.bitsInBytes(8)];
            ((U8)type).writeTo(asWritableBuf(bytes));
            return bytes;
        }
        else if (type instanceof U16) {
            byte[] bytes = new byte[SizeOf.bitsInBytes(16)];
            ((U16)type).writeTo(asWritableBuf(bytes));
            return bytes;
        }
        else if (type instanceof U32) {
            byte[] bytes = new byte[SizeOf.bitsInBytes(32)];
            ((U32)type).writeTo(asWritableBuf(bytes));
            return bytes;
        }
        else if (type instanceof U64) {
            byte[] bytes = new byte[SizeOf.bitsInBytes(64)];
            ((U64)type).writeTo(asWritableBuf(bytes));
            return bytes;
        }
        else if (type instanceof VlanPcp) {
            byte[] bytes = new byte[1];
            ((VlanPcp)type).writeByte(asWritableBuf(bytes));
            return bytes;
        }
        else if (type instanceof VlanVid) {
            byte[] bytes = new byte[2];
            ((VlanVid)type).write2Bytes(asWritableBuf(bytes));
            return bytes;
        }
        else if (type instanceof Masked<?>) {
            throw new UnsupportedOperationException(
                "cannot transform masked type to bytes (use its value and mask separately)");
        }
        else {
            // TODO maybe also put the BSN types
            throw new UnsupportedOperationException("unsupported OF type");
        }
    }

    static <T extends OFValueType<T>> T fromBytes( byte[] bytes, Class<? extends T> typeClass )
        throws OFParseError
    {
        if (typeClass.equals(ArpOpcode.class)) {
            return typeClass.cast(
                ArpOpcode.read2Bytes(asReadableBuf(bytes, 2)));
        }
        else if (typeClass.equals(BundleId.class)) {
            return typeClass.cast(
                BundleId.read4Bytes(asReadableBuf(bytes, 4)));
        }
        else if (typeClass.equals(EthType.class)) {
            return typeClass.cast(
                EthType.read2Bytes(asReadableBuf(bytes, 2)));
        }
        else if (typeClass.equals(ICMPv4Code.class)) {
            return typeClass.cast(
                ICMPv4Code.readByte(asReadableBuf(bytes, 1)));
        }
        else if (typeClass.equals(ICMPv4Type.class)) {
            return typeClass.cast(
                ICMPv4Type.readByte(asReadableBuf(bytes, 1)));
        }
        else if (typeClass.equals(IPv4Address.class)) {
            return typeClass.cast(
                IPv4Address.read4Bytes(asReadableBuf(bytes, 4)));
        }
        else if (typeClass.equals(IPv6Address.class)) {
            return typeClass.cast(
                IPv6Address.read16Bytes(asReadableBuf(bytes, 16)));
        }
        else if (typeClass.equals(IpDscp.class)) {
            return typeClass.cast(
                IpDscp.readByte(asReadableBuf(bytes, 1)));
        }
        else if (typeClass.equals(IpEcn.class)) {
            return typeClass.cast(
                IpEcn.readByte(asReadableBuf(bytes, 1)));
        }
        else if (typeClass.equals(IpProtocol.class)) {
            return typeClass.cast(
                IpProtocol.readByte(asReadableBuf(bytes, 1)));
        }
        else if (typeClass.equals(IPv6FlowLabel.class)) {
            return typeClass.cast(
                IPv6FlowLabel.read4Bytes(asReadableBuf(bytes, 4)));
        }
        else if (typeClass.equals(MacAddress.class)) {
            return typeClass.cast(
                MacAddress.read6Bytes(asReadableBuf(bytes, 6)));
        }
        else if (typeClass.equals(OFBooleanValue.class)) {
            // class OFBooleanValue.Reader is not visible so we must do this
            return typeClass.cast(
                OFBooleanValue.of(asReadableBuf(bytes, 1).readByte() != 0));
        }
        else if (typeClass.equals(OFGroup.class)) {
            return typeClass.cast(
                OFGroup.read4Bytes(asReadableBuf(bytes, 4)));
        }
        else if (typeClass.equals(OFMetadata.class)) {
            return typeClass.cast(
                OFMetadata.read8Bytes(asReadableBuf(bytes, 8)));
        }
        else if (typeClass.equals(OFPort.class)) {
            return typeClass.cast(
                OFPort.read4Bytes(asReadableBuf(bytes, 4)));
        }
        else if (typeClass.equals(OFVlanVidMatch.class)) {
            return typeClass.cast(
                OFVlanVidMatch.ofVlanVid(fromBytes(bytes, VlanVid.class)));
        }
        else if (typeClass.equals(TableId.class)) {
            return typeClass.cast(
                TableId.readByte(asReadableBuf(bytes, 1)));
        }
        else if (typeClass.equals(TransportPort.class)) {
            return typeClass.cast(
                TransportPort.read2Bytes(asReadableBuf(bytes, 2)));
        }
        else if (typeClass.equals(U8.class)) {
            // class U8.Reader is not visible so we must do this
            return typeClass.cast(
                U8.ofRaw(asReadableBuf(bytes, SizeOf.bitsInBytes(8)).readByte()));
        }
        else if (typeClass.equals(U16.class)) {
            // class U16.Reader is not visible so we must do this
            return typeClass.cast(
                U16.ofRaw(asReadableBuf(bytes, SizeOf.bitsInBytes(16)).readShort()));
        }
        else if (typeClass.equals(U32.class)) {
            // class U32.Reader is not visible so we must do this
            return typeClass.cast(
                U32.ofRaw(asReadableBuf(bytes, SizeOf.bitsInBytes(32)).readInt()));
        }
        else if (typeClass.equals(U64.class)) {
            // class U64.Reader is not visible so we must do this
            return typeClass.cast(
                U64.ofRaw(asReadableBuf(bytes, SizeOf.bitsInBytes(64)).readLong()));
        }
        else if (typeClass.equals(VlanPcp.class)) {
            return typeClass.cast(
                VlanPcp.readByte(asReadableBuf(bytes, 1)));
        }
        else if (typeClass.equals(VlanVid.class)) {
            return typeClass.cast(
                VlanVid.read2Bytes(asReadableBuf(bytes, 2)));
        }
        else if (Masked.class.isAssignableFrom(typeClass)) {
            throw new UnsupportedOperationException(
                "cannot transform bytes to masked type (transform to value and mask separately)");
        }
        else {
            // TODO maybe also put the BSN types
            throw new UnsupportedOperationException("unsupported OF type");
        }
    }

    private static ByteBuf asWritableBuf( byte[] array )
    {
        // a wrapped buffer has initial readIndex=0 and writeIndex=capacity
        ByteBuf buf = Unpooled.wrappedBuffer(array);
        buf.writerIndex(0);
        return buf;
    }

    private static ByteBuf asReadableBuf( byte[] array, int minLength )
    {
        // a new buffer has initial readIndex=0 and writeIndex=0
        ByteBuf buf = Unpooled.buffer(minLength);
        buf.writeBytes(array);
        buf.writeZero(buf.writableBytes()); // fill with zeros if required
        return buf;
    }

    @SuppressWarnings( "unchecked" )
    static <T extends OFValueType<T>> T defaultValue( T value )
    {
        return defaultValue((Class<T>)value.getClass());
    }

    static <T extends OFValueType<T>> T defaultValue( Class<? extends T> typeClass )
    {
        return fromMaskType(typeClass, Optional.empty());
    }

    @SuppressWarnings( "unchecked" )
    static <T extends OFValueType<T>> T wildcardMask( T value )
    {
        return wildcardMask((Class<T>)value.getClass());
    }

    static <T extends OFValueType<T>> T wildcardMask( Class<? extends T> typeClass )
    {
        return fromMaskType(typeClass, Optional.of(MaskType.FULL));
    }

    @SuppressWarnings( "unchecked" )
    static <T extends OFValueType<T>> T exactMask( T value )
    {
        return exactMask((Class<T>)value.getClass());
    }

    static <T extends OFValueType<T>> T exactMask( Class<? extends T> typeClass )
    {
        return fromMaskType(typeClass, Optional.of(MaskType.NONE));
    }

    private static <T extends OFValueType<T>> T fromMaskType( Class<? extends T> typeClass,
                                                              Optional<MaskType> maskType )
    {
        if (maskType.isPresent() && maskType.get().isPartiallyMasked()) {
            throw new IllegalArgumentException("illegal partial mask");
        }

        if (typeClass.equals(ArpOpcode.class)) {
            return typeClass.cast(
                !maskType.isPresent() ? ArpOpcode.NONE
                                      : maskType.get().isWildcard() ? ArpOpcode.FULL_MASK
                                                                    : ArpOpcode.NO_MASK);
        }
        else if (typeClass.equals(BundleId.class)) {
            return typeClass.cast(
                !maskType.isPresent() ? BundleId.NONE
                                      : maskType.get().isWildcard() ? BundleId.FULL_MASK
                                                                    : BundleId.NO_MASK);
        }
        else if (typeClass.equals(EthType.class)) {
            return typeClass.cast(
                !maskType.isPresent() ? EthType.NONE
                                      : maskType.get().isWildcard() ? EthType.FULL_MASK
                                                                    : EthType.NO_MASK);
        }
        else if (typeClass.equals(ICMPv4Code.class)) {
            return typeClass.cast(
                !maskType.isPresent() ? ICMPv4Code.NONE
                                      : maskType.get().isWildcard() ? ICMPv4Code.FULL_MASK
                                                                    : ICMPv4Code.NO_MASK);
        }
        else if (typeClass.equals(ICMPv4Type.class)) {
            return typeClass.cast(
                !maskType.isPresent() ? ICMPv4Type.NONE
                                      : maskType.get().isWildcard() ? ICMPv4Type.FULL_MASK
                                                                    : ICMPv4Type.NO_MASK);
        }
        else if (typeClass.equals(IPv4Address.class)) {
            return typeClass.cast(
                !maskType.isPresent() ? IPv4Address.NONE
                                      : maskType.get().isWildcard() ? IPv4Address.FULL_MASK
                                                                    : IPv4Address.NO_MASK);
        }
        else if (typeClass.equals(IPv6Address.class)) {
            return typeClass.cast(
                !maskType.isPresent() ? IPv6Address.NONE
                                      : maskType.get().isWildcard() ? IPv6Address.FULL_MASK
                                                                    : IPv6Address.NO_MASK);
        }
        else if (typeClass.equals(IpDscp.class)) {
            return typeClass.cast(
                !maskType.isPresent() ? IpDscp.NONE
                                      : maskType.get().isWildcard() ? IpDscp.FULL_MASK
                                                                    : IpDscp.NO_MASK);
        }
        else if (typeClass.equals(IpEcn.class)) {
            return typeClass.cast(
                !maskType.isPresent() ? IpEcn.NONE
                                      : maskType.get().isWildcard() ? IpEcn.FULL_MASK
                                                                    : IpEcn.NO_MASK);
        }
        else if (typeClass.equals(IpProtocol.class)) {
            return typeClass.cast(
                !maskType.isPresent() ? IpProtocol.NONE
                                      : maskType.get().isWildcard() ? IpProtocol.FULL_MASK
                                                                    : IP_PROTO_NO_MASK);
        }
        else if (typeClass.equals(IPv6FlowLabel.class)) {
            return typeClass.cast(
                !maskType.isPresent() ? IPv6FlowLabel.NONE
                                      : maskType.get().isWildcard() ? IPv6FlowLabel.FULL_MASK
                                                                    : IPv6FlowLabel.NO_MASK);
        }
        else if (typeClass.equals(MacAddress.class)) {
            return typeClass.cast(
                !maskType.isPresent() ? MacAddress.NONE
                                      : maskType.get().isWildcard() ? MacAddress.FULL_MASK
                                                                    : MacAddress.NO_MASK);
        }
        else if (typeClass.equals(OFBooleanValue.class)) {
            return typeClass.cast(
                !maskType.isPresent() ? OFBooleanValue.FALSE
                                      : maskType.get().isWildcard() ? OFBooleanValue.FULL_MASK
                                                                    : OFBooleanValue.NO_MASK);
        }
        else if (typeClass.equals(OFGroup.class)) {
            return typeClass.cast(
                !maskType.isPresent() ? OFGroup.ZERO
                                      : maskType.get().isWildcard() ? OFGroup.FULL_MASK
                                                                    : OFGroup.NO_MASK);
        }
        else if (typeClass.equals(OFMetadata.class)) {
            return typeClass.cast(
                !maskType.isPresent() ? OFMetadata.NONE
                                      : maskType.get().isWildcard() ? OFMetadata.FULL_MASK
                                                                    : OFMetadata.NO_MASK);
        }
        else if (typeClass.equals(OFPort.class)) {
            return typeClass.cast(
                !maskType.isPresent() ? OFPort.ZERO
                                      : maskType.get().isWildcard() ? OFPort.FULL_MASK
                                                                    : OFPort.NO_MASK);
        }
        else if (typeClass.equals(OFVlanVidMatch.class)) {
            return typeClass.cast(
                !maskType.isPresent() ? OFVlanVidMatch.NONE
                                      : maskType.get().isWildcard() ? OFVlanVidMatch.FULL_MASK
                                                                    : OFVlanVidMatch.NO_MASK);
        }
        else if (typeClass.equals(TableId.class)) {
            return typeClass.cast(
                !maskType.isPresent() ? TableId.NONE
                                      : maskType.get().isWildcard() ? TableId.ZERO
                                                                    : TableId.ALL);
        }
        else if (typeClass.equals(TransportPort.class)) {
            return typeClass.cast(
                !maskType.isPresent() ? TransportPort.NONE
                                      : maskType.get().isWildcard() ? TransportPort.FULL_MASK
                                                                    : TransportPort.NO_MASK);
        }
        else if (typeClass.equals(U8.class)) {
            return typeClass.cast(
                !maskType.isPresent() ? U8.ZERO
                                      : maskType.get().isWildcard() ? U8.FULL_MASK
                                                                    : U8.NO_MASK);
        }
        else if (typeClass.equals(U16.class)) {
            return typeClass.cast(
                !maskType.isPresent() ? U16.ZERO
                                      : maskType.get().isWildcard() ? U16.FULL_MASK
                                                                    : U16.NO_MASK);
        }
        else if (typeClass.equals(U32.class)) {
            return typeClass.cast(
                !maskType.isPresent() ? U32.ZERO
                                      : maskType.get().isWildcard() ? U32.FULL_MASK
                                                                    : U32.NO_MASK);
        }
        else if (typeClass.equals(U64.class)) {
            return typeClass.cast(
                !maskType.isPresent() ? U64.ZERO
                                      : maskType.get().isWildcard() ? U64.FULL_MASK
                                                                    : U64.NO_MASK);
        }
        else if (typeClass.equals(VlanPcp.class)) {
            return typeClass.cast(
                !maskType.isPresent() ? VlanPcp.NONE
                                      : maskType.get().isWildcard() ? VlanPcp.FULL_MASK
                                                                    : VlanPcp.NO_MASK);
        }
        else if (typeClass.equals(VlanVid.class)) {
            return typeClass.cast(
                !maskType.isPresent() ? VlanVid.ZERO
                                      : maskType.get().isWildcard() ? VlanVid.FULL_MASK
                                                                    : VlanVid.NO_MASK);
        }
        else if (Masked.class.isAssignableFrom(Masked.class)) {
            throw new UnsupportedOperationException("masked type not supported");
        }
        else {
            // TODO maybe also put the BSN types
            throw new UnsupportedOperationException("unsupported OF type");
        }
    }

    private static int ofBytes( int b )
    {
        return b * Byte.SIZE;
    }

    // FIXME This is only a workaround for a bug in IpProtocol class
    private static final IpProtocol IP_PROTO_NO_MASK;
    static {
        try {
            Constructor<IpProtocol> constr = IpProtocol.class.getDeclaredConstructor(short.class);
            constr.setAccessible(true);
            IP_PROTO_NO_MASK = constr.newInstance((short)0xff);
        }
        catch (NoSuchMethodException
               | SecurityException
               | InstantiationException
               | IllegalAccessException
               | IllegalArgumentException
               | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
    // FIXME This is only a workaround for a bug in IpProtocol class

    private OFValueTypeUtils()
    {
        // not used
    }
}
