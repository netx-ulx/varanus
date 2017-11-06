package net.varanus.util.openflow;


import javax.annotation.ParametersAreNonnullByDefault;

import org.projectfloodlight.openflow.exceptions.OFParseError;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.match.MatchFields;
import org.projectfloodlight.openflow.types.ArpOpcode;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.ICMPv4Code;
import org.projectfloodlight.openflow.types.ICMPv4Type;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IPv6Address;
import org.projectfloodlight.openflow.types.IPv6FlowLabel;
import org.projectfloodlight.openflow.types.IpDscp;
import org.projectfloodlight.openflow.types.IpEcn;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.Masked;
import org.projectfloodlight.openflow.types.OFBooleanValue;
import org.projectfloodlight.openflow.types.OFMetadata;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.OFValueType;
import org.projectfloodlight.openflow.types.OFVlanVidMatch;
import org.projectfloodlight.openflow.types.TransportPort;
import org.projectfloodlight.openflow.types.U16;
import org.projectfloodlight.openflow.types.U32;
import org.projectfloodlight.openflow.types.U64;
import org.projectfloodlight.openflow.types.U8;
import org.projectfloodlight.openflow.types.VlanPcp;

import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;


/**
 * 
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class MatchFieldUtils
{
    public static MatchField<?> parseField( String str ) throws IllegalArgumentException
    {
        return fieldOf(MatchFields.valueOf(str.toUpperCase()));
    }

    public static MatchField<?> fieldOf( MatchFields id )
    {
        switch (id) {
            case ARP_OP:
                return MatchField.ARP_OP;
            case ARP_SHA:
                return MatchField.ARP_SHA;
            case ARP_SPA:
                return MatchField.ARP_SPA;
            case ARP_THA:
                return MatchField.ARP_THA;
            case ARP_TPA:
                return MatchField.ARP_TPA;
            case ETH_DST:
                return MatchField.ETH_DST;
            case ETH_SRC:
                return MatchField.ETH_SRC;
            case ETH_TYPE:
                return MatchField.ETH_TYPE;
            case ICMPV4_CODE:
                return MatchField.ICMPV4_CODE;
            case ICMPV4_TYPE:
                return MatchField.ICMPV4_TYPE;
            case ICMPV6_CODE:
                return MatchField.ICMPV6_CODE;
            case ICMPV6_TYPE:
                return MatchField.ICMPV6_TYPE;
            case IN_PHY_PORT:
                return MatchField.IN_PHY_PORT;
            case IN_PORT:
                return MatchField.IN_PORT;
            case IP_DSCP:
                return MatchField.IP_DSCP;
            case IP_ECN:
                return MatchField.IP_ECN;
            case IP_PROTO:
                return MatchField.IP_PROTO;
            case IPV4_DST:
                return MatchField.IPV4_DST;
            case IPV4_SRC:
                return MatchField.IPV4_SRC;
            case IPV6_DST:
                return MatchField.IPV6_DST;
            case IPV6_EXTHDR:
                return MatchField.IPV6_EXTHDR;
            case IPV6_FLABEL:
                return MatchField.IPV6_FLABEL;
            case IPV6_ND_SLL:
                return MatchField.IPV6_ND_SLL;
            case IPV6_ND_TARGET:
                return MatchField.IPV6_ND_TARGET;
            case IPV6_ND_TLL:
                return MatchField.IPV6_ND_TLL;
            case IPV6_SRC:
                return MatchField.IPV6_SRC;
            case METADATA:
                return MatchField.METADATA;
            case MPLS_BOS:
                return MatchField.MPLS_BOS;
            case MPLS_LABEL:
                return MatchField.MPLS_LABEL;
            case MPLS_TC:
                return MatchField.MPLS_TC;
            case PBB_UCA:
                return MatchField.PBB_UCA;
            case SCTP_DST:
                return MatchField.SCTP_DST;
            case SCTP_SRC:
                return MatchField.SCTP_SRC;
            case TCP_DST:
                return MatchField.TCP_DST;
            case TCP_SRC:
                return MatchField.TCP_SRC;
            case TUNNEL_ID:
                return MatchField.TUNNEL_ID;
            case TUNNEL_IPV4_DST:
                return MatchField.TUNNEL_IPV4_DST;
            case TUNNEL_IPV4_SRC:
                return MatchField.TUNNEL_IPV4_SRC;
            case UDP_DST:
                return MatchField.UDP_DST;
            case UDP_SRC:
                return MatchField.UDP_SRC;
            case VLAN_PCP:
                return MatchField.VLAN_PCP;
            case VLAN_VID:
                return MatchField.VLAN_VID;
            default:
                throw new UnsupportedOperationException("unsupported match field");
        }
    }

    public static <T extends OFValueType<T>> T parseValue( String str, MatchField<T> field )
    {
        return OFValueTypeUtils.parseValue(str, typeClass(field));
    }

    public static boolean isMaskedString( String str )
    {
        return OFValueTypeUtils.isMaskedString(str);
    }

    public static <T extends OFValueType<T>> Masked<T> parseMasked( String str, MatchField<T> field )
    {
        return OFValueTypeUtils.parseMasked(str, typeClass(field));
    }

    public static int bitSize( MatchField<?> field )
    {
        return OFValueTypeUtils.bitSize(typeClass(field));
    }

    public static <T extends OFValueType<T>> byte[] toBytes( T value )
    {
        return OFValueTypeUtils.toBytes(value);
    }

    public static <T extends OFValueType<T>> T fromBytes( byte[] bytes, MatchField<T> field ) throws OFParseError
    {
        return OFValueTypeUtils.fromBytes(bytes, typeClass(field));
    }

    public static <T extends OFValueType<T>> T getDefaultValue( T value )
    {
        return OFValueTypeUtils.defaultValue(value);
    }

    public static <T extends OFValueType<T>> T getDefaultValue( MatchField<T> field )
    {
        return OFValueTypeUtils.defaultValue(typeClass(field));
    }

    public static <T extends OFValueType<T>> T getWildcardMask( T value )
    {
        return OFValueTypeUtils.wildcardMask(value);
    }

    public static <T extends OFValueType<T>> T getWildcardMask( MatchField<T> field )
    {
        return OFValueTypeUtils.wildcardMask(typeClass(field));
    }

    public static <T extends OFValueType<T>> T getExactMask( T value )
    {
        return OFValueTypeUtils.exactMask(value);
    }

    public static <T extends OFValueType<T>> T getExactMask( MatchField<T> field )
    {
        return OFValueTypeUtils.exactMask(typeClass(field));
    }

    @SuppressWarnings( "unchecked" )
    public static <T extends OFValueType<T>> Class<T> typeClass( MatchField<T> field )
    {
        switch (field.id) {
            case ARP_OP:
                return (Class<T>)ArpOpcode.class;
            case ARP_SHA:
                return (Class<T>)MacAddress.class;
            case ARP_SPA:
                return (Class<T>)IPv4Address.class;
            case ARP_THA:
                return (Class<T>)MacAddress.class;
            case ARP_TPA:
                return (Class<T>)IPv4Address.class;
            case ETH_DST:
                return (Class<T>)MacAddress.class;
            case ETH_SRC:
                return (Class<T>)MacAddress.class;
            case ETH_TYPE:
                return (Class<T>)EthType.class;
            case ICMPV4_CODE:
                return (Class<T>)ICMPv4Code.class;
            case ICMPV4_TYPE:
                return (Class<T>)ICMPv4Type.class;
            case ICMPV6_CODE:
                return (Class<T>)U8.class;
            case ICMPV6_TYPE:
                return (Class<T>)U8.class;
            case IN_PHY_PORT:
                return (Class<T>)OFPort.class;
            case IN_PORT:
                return (Class<T>)OFPort.class;
            case IP_DSCP:
                return (Class<T>)IpDscp.class;
            case IP_ECN:
                return (Class<T>)IpEcn.class;
            case IP_PROTO:
                return (Class<T>)IpProtocol.class;
            case IPV4_DST:
                return (Class<T>)IPv4Address.class;
            case IPV4_SRC:
                return (Class<T>)IPv4Address.class;
            case IPV6_DST:
                return (Class<T>)IPv6Address.class;
            case IPV6_EXTHDR:
                return (Class<T>)U16.class;
            case IPV6_FLABEL:
                return (Class<T>)IPv6FlowLabel.class;
            case IPV6_ND_SLL:
                return (Class<T>)MacAddress.class;
            case IPV6_ND_TARGET:
                return (Class<T>)IPv6Address.class;
            case IPV6_ND_TLL:
                return (Class<T>)MacAddress.class;
            case IPV6_SRC:
                return (Class<T>)IPv6Address.class;
            case METADATA:
                return (Class<T>)OFMetadata.class;
            case MPLS_BOS:
                return (Class<T>)OFBooleanValue.class;
            case MPLS_LABEL:
                return (Class<T>)U32.class;
            case MPLS_TC:
                return (Class<T>)U8.class;
            case PBB_UCA:
                return (Class<T>)OFBooleanValue.class;
            case SCTP_DST:
                return (Class<T>)TransportPort.class;
            case SCTP_SRC:
                return (Class<T>)TransportPort.class;
            case TCP_DST:
                return (Class<T>)TransportPort.class;
            case TCP_SRC:
                return (Class<T>)TransportPort.class;
            case TUNNEL_ID:
                return (Class<T>)U64.class;
            case TUNNEL_IPV4_DST:
                return (Class<T>)IPv4Address.class;
            case TUNNEL_IPV4_SRC:
                return (Class<T>)IPv4Address.class;
            case UDP_DST:
                return (Class<T>)TransportPort.class;
            case UDP_SRC:
                return (Class<T>)TransportPort.class;
            case VLAN_PCP:
                return (Class<T>)VlanPcp.class;
            case VLAN_VID:
                return (Class<T>)OFVlanVidMatch.class;
            default:
                throw new UnsupportedOperationException("unsupported match field");
        }
    }

    public static boolean supportsPartialMask( MatchField<?> field )
    {
        switch (field.id) {
            case ETH_DST:
            case ETH_SRC:
            case VLAN_VID:
            case IPV4_SRC:
            case IPV4_DST:
            case ARP_SPA:
            case ARP_TPA:
            case ARP_SHA:
            case ARP_THA:
            case IPV6_SRC:
            case IPV6_DST:
            case IPV6_FLABEL:
                // PBB_ISID:
            case IPV6_EXTHDR:
                return true;

            default:
                return false;
        }
    }

    private MatchFieldUtils()
    {
        // not used
    }
}
