package net.varanus.sdncontroller.util;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.ICMPv4Code;
import org.projectfloodlight.openflow.types.ICMPv4Type;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IPv6Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFVlanVidMatch;
import org.projectfloodlight.openflow.types.TableId;
import org.projectfloodlight.openflow.types.TransportPort;
import org.projectfloodlight.openflow.types.U128;
import org.projectfloodlight.openflow.types.U16;
import org.projectfloodlight.openflow.types.U32;
import org.projectfloodlight.openflow.types.U64;
import org.projectfloodlight.openflow.types.U8;
import org.projectfloodlight.openflow.types.VlanPcp;
import org.projectfloodlight.openflow.types.VlanVid;

import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.ICMP;


/**
 * 
 */
public final class Fields
{
    public static final U128 MAX_U128 = U128.of(0xFF_FF_FF_FF_FF_FFL, 0xFF_FF_FF_FF_FF_FFL);
    public static final U128 MIN_U128 = U128.ZERO;

    public static final U64 MAX_U64 = U64.NO_MASK;
    public static final U64 MIN_U64 = U64.ZERO;

    public static final U32 MAX_U32 = U32.NO_MASK;
    public static final U32 MIN_U32 = U32.ZERO;

    public static final U16 MAX_U16 = U16.NO_MASK;
    public static final U16 MIN_U16 = U16.ZERO;

    public static final U8 MAX_U8 = U8.NO_MASK;
    public static final U8 MIN_U8 = U8.ZERO;

    public static final int     SIZE_OF_TABLE_ID = 1;
    public static final TableId MIN_TABLE_ID     = TableId.of(0x00);
    public static final TableId MAX_TABLE_ID     = TableId.of(0xFF);

    public static final int        SIZE_OF_MAC_ADDRESS = 6;
    public static final MacAddress MIN_MAC_ADDRESS     = MacAddress.of(0x00_00_00_00_00_00L);
    public static final MacAddress MAX_MAC_ADDRESS     = MacAddress.of(0xFF_FF_FF_FF_FF_FFL);

    public static final int     SIZE_OF_VLAN_VID = 2;
    public static final VlanVid MIN_VLAN_VID     = VlanVid.ofVlan(0x00_00);
    public static final VlanVid MAX_VLAN_VID     = VlanVid.ofVlan(0x0F_FF);

    public static final int         SIZE_OF_IPv4_ADDRESS = 4;
    public static final IPv4Address MIN_IPv4_ADDRESS     = IPv4Address.of(0x00_00_00_00);
    public static final IPv4Address MAX_IPv4_ADDRESS     = IPv4Address.of(0xFF_FF_FF_FF);

    public static final int         SIZE_OF_IPv6_ADDRESS = 16;
    public static final IPv6Address MIN_IPv6_ADDRESS     =
        IPv6Address.of(0x00_00_00_00_00_00_00_00L, 0x00_00_00_00_00_00_00_00L);
    public static final IPv6Address MAX_IPv6_ADDRESS     =
        IPv6Address.of(0xFF_FF_FF_FF_FF_FF_FF_FFL, 0xFF_FF_FF_FF_FF_FF_FF_FFL);

    public static final int           SIZE_OF_TRANSPORT_PORT = 2;
    public static final TransportPort MIN_TRANSPORT_PORT     = TransportPort.of(0x00_00);
    public static final TransportPort MAX_TRANSPORT_PORT     = TransportPort.of(0xFF_FF);

    public static final int        SIZE_OF_ICMPv4_TYPE = 1;
    public static final ICMPv4Type MIN_ICMPv4_TYPE     = ICMPv4Type.of((short)0x00);
    public static final ICMPv4Type MAX_ICMPv4_TYPE     = ICMPv4Type.of((short)0xFF);

    public static final int        SIZE_OF_ICMPv4_CODE = 1;
    public static final ICMPv4Code MIN_ICMPv4_CODE     = ICMPv4Code.of((short)0x00);
    public static final ICMPv4Code MAX_ICMPv4_CODE     = ICMPv4Code.of((short)0xFF);

    public static final EthType BSN_ETHER_TYPE = EthType.of(U16.ofRaw(Ethernet.TYPE_BSN).getValue());

    public static long getSaturatedLong( U64 uLong )
    {
        long asLong = uLong.getValue();
        return (asLong >= 0) ? asLong
                             : Long.MAX_VALUE;
    }

    public static int getSaturatedInt( U32 uInt )
    {
        int asInt = uInt.getRaw();
        return (asInt >= 0) ? asInt
                            : Integer.MAX_VALUE;
    }

    public static short getSaturatedShort( U16 uShort )
    {
        short asShort = uShort.getRaw();
        return (asShort >= 0) ? asShort
                              : Short.MAX_VALUE;
    }

    public static byte getSaturatedByte( U8 uByte )
    {
        byte asByte = uByte.getRaw();
        return (asByte >= 0) ? asByte
                             : Byte.MAX_VALUE;
    }

    public static boolean isUnicastMACAddress( MacAddress macAddr )
    {
        return (macAddr.getLong() & 0x010000000000L) == 0L;
    }

    public static EthType getVlanAwareEtherType( Ethernet eth )
    {
        if (eth.getVlanID() == Ethernet.VLAN_UNTAGGED) {
            return eth.getEtherType();
        }
        else {
            return EthType.VLAN_FRAME;
        }
    }

    public static VlanVid getVlanID( Match match )
    {
        OFVlanVidMatch vidMatch = match.get(MatchField.VLAN_VID);
        return (vidMatch != null ? vidMatch.getVlanVid() : null);
    }

    public static OFVlanVidMatch getVlanIDMatch( Ethernet eth )
    {
        return OFVlanVidMatch.ofRawVid(eth.getVlanID());
    }

    public static VlanVid getVlanID( Ethernet eth )
    {
        int vid = U16.f(eth.getVlanID());
        return VlanVid.ofVlan(vid);
    }

    public static VlanPcp getVlanPriorityCode( Ethernet eth )
    {
        return VlanPcp.of(eth.getPriorityCode());
    }

    public static ICMPv4Type getIcmpType( ICMP icmp )
    {
        short type = U8.f(icmp.getIcmpType());
        return ICMPv4Type.of(type);
    }

    public static ICMPv4Code getIcmpCode( ICMP icmp )
    {
        short code = U8.f(icmp.getIcmpCode());
        return ICMPv4Code.of(code);
    }

    public static SortedSet<EthType> getAllEtherTypes()
    {
        return new TreeSet<>(ETHTYPE_NAMES.keySet());
    }

    public static SortedSet<IpProtocol> getAllIPProtocols()
    {
        return new TreeSet<>(IPPROTO_NAMES.keySet());
    }

    public static SortedSet<ICMPv4Type> getAllICMPv4Types()
    {
        return new TreeSet<>(ICMPTYPE_NAMES.keySet());
    }

    public static String getEtherTypeName( EthType type )
    {
        if (type == null) {
            return "";
        }
        else {
            String name = ETHTYPE_NAMES.get(type);
            return (name != null ? name
                                 : String.format("unknown (%X)", type.getValue()));
        }
    }

    public static String getIPProtoName( IpProtocol proto )
    {
        if (proto == null) {
            return "";
        }
        else {
            String name = IPPROTO_NAMES.get(proto);
            return (name != null ? name
                                 : String.format("unknown (%X)", proto.getIpProtocolNumber() & 0xFFFF));
        }
    }

    public static String getICMPTypeName( ICMPv4Type type )
    {
        if (type == null) {
            return "";
        }
        else {
            String name = ICMPTYPE_NAMES.get(type);
            return (name != null ? name
                                 : String.format("unknown (%X)", type.getType() & 0xFFFF));
        }
    }

    private static final Map<EthType, String>    ETHTYPE_NAMES;
    private static final Map<IpProtocol, String> IPPROTO_NAMES;
    private static final Map<ICMPv4Type, String> ICMPTYPE_NAMES;

    static {
        Map<EthType, String> ethTypeNames = new HashMap<>();
        ethTypeNames.put(EthType.NONE, "none");
        ethTypeNames.put(EthType.IPv4, "IPv4");
        ethTypeNames.put(EthType.ARP, "ARP");
        ethTypeNames.put(EthType.WAKE_ON_LAN, "WAKE_ON_LAN");
        ethTypeNames.put(EthType.TRILL, "TRILL");
        ethTypeNames.put(EthType.DECNET_IV, "DECNET_IV");
        ethTypeNames.put(EthType.REV_ARP, "REV_ARP");
        ethTypeNames.put(EthType.APPLE_TALK, "APPLE_TALK");
        ethTypeNames.put(EthType.APPLE_TALK_ARP, "APPLE_TALK_ARP");
        ethTypeNames.put(EthType.VLAN_FRAME, "VLAN_FRAME");
        ethTypeNames.put(EthType.IPX_8137, "IPX_8137");
        ethTypeNames.put(EthType.IPX_8138, "IPX_8138");
        ethTypeNames.put(EthType.QNX, "QNX");
        ethTypeNames.put(EthType.IPv6, "IPv6");
        ethTypeNames.put(EthType.ETH_FLOW, "ETH_FLOW");
        ethTypeNames.put(EthType.SLOW_PROTOCOLS, "SLOW_PROTOCOLS");
        ethTypeNames.put(EthType.COBRANET, "COBRANET");
        ethTypeNames.put(EthType.MPLS_UNICAST, "MPLS_UNICAST");
        ethTypeNames.put(EthType.MPLS_MULTICAST, "MPLS_MULTICAST");
        ethTypeNames.put(EthType.PPPoE_DISCOVERY, "PPPoE_DISCOVERY");
        ethTypeNames.put(EthType.PPPoE_SESSION, "PPPoE_SESSION");
        ethTypeNames.put(EthType.JUMBO_FRAMES, "JUMBO_FRAMES");
        ethTypeNames.put(EthType.HOMEPLUG_10, "HOMEPLUG_10");
        ethTypeNames.put(EthType.EAP_OVER_LAN, "EAP_OVER_LAN");
        ethTypeNames.put(EthType.PROFINET, "PROFINET");
        ethTypeNames.put(EthType.HYPERSCSI, "HYPERSCSI");
        ethTypeNames.put(EthType.ATA_OVER_ETH, "ATA_OVER_ETH");
        ethTypeNames.put(EthType.ETHERCAT, "ETHERCAT");
        ethTypeNames.put(EthType.BRIDGING, "BRIDGING");
        ethTypeNames.put(EthType.POWERLINK, "POWERLINK");
        ethTypeNames.put(EthType.LLDP, "LLDP");
        ethTypeNames.put(EthType.SERCOS, "SERCOS");
        ethTypeNames.put(EthType.HOMEPLUG_AV, "HOMEPLUG_AV");
        ethTypeNames.put(EthType.MRP, "MRP");
        ethTypeNames.put(EthType.MAC_SEC, "MAC_SEC");
        ethTypeNames.put(EthType.PTP, "PTP");
        ethTypeNames.put(EthType.CFM, "CFM");
        ethTypeNames.put(EthType.FCoE, "FCoE");
        ethTypeNames.put(EthType.FCoE_INIT, "FCoE_INIT");
        ethTypeNames.put(EthType.RoCE, "RoCE");
        ethTypeNames.put(EthType.HSR, "HSR");
        ethTypeNames.put(EthType.CONF_TEST, "CONF_TEST");
        ethTypeNames.put(EthType.Q_IN_Q, "Q_IN_Q");
        ethTypeNames.put(EthType.LLT, "LLT");
        ethTypeNames.put(BSN_ETHER_TYPE, "BSN");
        ETHTYPE_NAMES = Collections.unmodifiableMap(ethTypeNames);

        Map<IpProtocol, String> ipProtoNames = new HashMap<>();
        ipProtoNames.put(IpProtocol.HOPOPT, "none/HOPOPT");
        ipProtoNames.put(IpProtocol.ICMP, "ICMP");
        ipProtoNames.put(IpProtocol.IGMP, "IGMP");
        ipProtoNames.put(IpProtocol.GGP, "GGP");
        ipProtoNames.put(IpProtocol.IPv4, "IPv4");
        ipProtoNames.put(IpProtocol.ST, "ST");
        ipProtoNames.put(IpProtocol.TCP, "TCP");
        ipProtoNames.put(IpProtocol.CBT, "CBT");
        ipProtoNames.put(IpProtocol.EGP, "EGP");
        ipProtoNames.put(IpProtocol.IGP, "IGP");
        ipProtoNames.put(IpProtocol.BBN_RCC_MON, "BBN_RCC_MON");
        ipProtoNames.put(IpProtocol.NVP_II, "NVP_II");
        ipProtoNames.put(IpProtocol.PUP, "PUP");
        ipProtoNames.put(IpProtocol.ARGUS, "ARGUS");
        ipProtoNames.put(IpProtocol.EMCON, "EMCON");
        ipProtoNames.put(IpProtocol.XNET, "XNET");
        ipProtoNames.put(IpProtocol.CHAOS, "CHAOS");
        ipProtoNames.put(IpProtocol.UDP, "UDP");
        ipProtoNames.put(IpProtocol.MUX, "MUX");
        ipProtoNames.put(IpProtocol.DCN_MEAS, "DCN_MEAS");
        ipProtoNames.put(IpProtocol.HMP, "HMP");
        ipProtoNames.put(IpProtocol.PRM, "PRM");
        ipProtoNames.put(IpProtocol.XNS_IDP, "XNS_IDP");
        ipProtoNames.put(IpProtocol.TRUNK_1, "TRUNK_1");
        ipProtoNames.put(IpProtocol.TRUNK_2, "TRUNK_2");
        ipProtoNames.put(IpProtocol.LEAF_1, "LEAF_1");
        ipProtoNames.put(IpProtocol.LEAF_2, "LEAF_2");
        ipProtoNames.put(IpProtocol.RDP, "RDP");
        ipProtoNames.put(IpProtocol.IRTP, "IRTP");
        ipProtoNames.put(IpProtocol.ISO_TP4, "ISO_TP4");
        ipProtoNames.put(IpProtocol.NETBLT, "NETBLT");
        ipProtoNames.put(IpProtocol.MFE_NSP, "MFE_NSP");
        ipProtoNames.put(IpProtocol.MERIT_INP, "MERIT_INP");
        ipProtoNames.put(IpProtocol.DCCP, "DCCP");
        ipProtoNames.put(IpProtocol._3PC, "3PC");
        ipProtoNames.put(IpProtocol.IDPR, "IDPR");
        ipProtoNames.put(IpProtocol.XTP, "XTP");
        ipProtoNames.put(IpProtocol.DDP, "DDP");
        ipProtoNames.put(IpProtocol.IDPR_CMTP, "IDPR_CMTP");
        ipProtoNames.put(IpProtocol.TP_PP, "TP_PP");
        ipProtoNames.put(IpProtocol.IL, "IL");
        ipProtoNames.put(IpProtocol.IPv6, "IPv6");
        ipProtoNames.put(IpProtocol.SDRP, "SDRP");
        ipProtoNames.put(IpProtocol.IPv6_ROUTE, "IPv6_ROUTE");
        ipProtoNames.put(IpProtocol.IPv6_FRAG, "IPv6_FRAG");
        ipProtoNames.put(IpProtocol.IDRP, "IDRP");
        ipProtoNames.put(IpProtocol.RSVP, "RSVP");
        ipProtoNames.put(IpProtocol.GRE, "GRE");
        ipProtoNames.put(IpProtocol.MHRP, "MHRP");
        ipProtoNames.put(IpProtocol.BNA, "BNA");
        ipProtoNames.put(IpProtocol.ESP, "ESP");
        ipProtoNames.put(IpProtocol.AH, "AH");
        ipProtoNames.put(IpProtocol.I_NLSP, "I_NLSP");
        ipProtoNames.put(IpProtocol.SWIPE, "SWIPE");
        ipProtoNames.put(IpProtocol.NARP, "NARP");
        ipProtoNames.put(IpProtocol.MOBILE, "MOBILE");
        ipProtoNames.put(IpProtocol.TLSP, "TLSP");
        ipProtoNames.put(IpProtocol.SKIP, "SKIP");
        ipProtoNames.put(IpProtocol.IPv6_ICMP, "IPv6_ICMP");
        ipProtoNames.put(IpProtocol.IPv6_NO_NXT, "IPv6_NO_NXT");
        ipProtoNames.put(IpProtocol.IPv6_OPTS, "IPv6_OPTS");
        ipProtoNames.put(IpProtocol.HOST_INTERNAL, "HOST_INTERNAL");
        ipProtoNames.put(IpProtocol.CFTP, "CFTP");
        ipProtoNames.put(IpProtocol.LOCAL_NET, "LOCAL_NET");
        ipProtoNames.put(IpProtocol.SAT_EXPAK, "SAT_EXPAK");
        ipProtoNames.put(IpProtocol.KRYPTOLAN, "KRYPTOLAN");
        ipProtoNames.put(IpProtocol.RVD, "RVD");
        ipProtoNames.put(IpProtocol.IPPC, "IPPC");
        ipProtoNames.put(IpProtocol.DIST_FS, "DIST_FS");
        ipProtoNames.put(IpProtocol.SAT_MON, "SAT_MON");
        ipProtoNames.put(IpProtocol.VISA, "VISA");
        ipProtoNames.put(IpProtocol.IPCV, "IPCV");
        ipProtoNames.put(IpProtocol.CPNX, "CPNX");
        ipProtoNames.put(IpProtocol.CPHB, "CPHB");
        ipProtoNames.put(IpProtocol.WSN, "WSN");
        ipProtoNames.put(IpProtocol.PVP, "PVP");
        ipProtoNames.put(IpProtocol.BR_SAT_MON, "BR_SAT_MON");
        ipProtoNames.put(IpProtocol.SUN_ND, "SUN_ND");
        ipProtoNames.put(IpProtocol.WB_MON, "WB_MON");
        ipProtoNames.put(IpProtocol.WB_EXPAK, "WB_EXPAK");
        ipProtoNames.put(IpProtocol.ISO_IP, "ISO_IP");
        ipProtoNames.put(IpProtocol.VMTP, "VMTP");
        ipProtoNames.put(IpProtocol.SECURE_VMTP, "SECURE_VMTP");
        ipProtoNames.put(IpProtocol.VINES, "VINES");
        ipProtoNames.put(IpProtocol.TTP_IPTM, "TTP_IPTM");
        ipProtoNames.put(IpProtocol.NSFNET_IGP, "NSFNET_IGP");
        ipProtoNames.put(IpProtocol.DGP, "DGP");
        ipProtoNames.put(IpProtocol.TCF, "TCF");
        ipProtoNames.put(IpProtocol.EIGRP, "EIGRP");
        ipProtoNames.put(IpProtocol.OSPF, "OSPF");
        ipProtoNames.put(IpProtocol.Sprite_RPC, "Sprite_RPC");
        ipProtoNames.put(IpProtocol.LARP, "LARP");
        ipProtoNames.put(IpProtocol.MTP, "MTP");
        ipProtoNames.put(IpProtocol.AX_25, "AX_25");
        ipProtoNames.put(IpProtocol.IPIP, "IPIP");
        ipProtoNames.put(IpProtocol.MICP, "MICP");
        ipProtoNames.put(IpProtocol.SCC_SP, "SCC_SP");
        ipProtoNames.put(IpProtocol.ETHERIP, "ETHERIP");
        ipProtoNames.put(IpProtocol.ENCAP, "ENCAP");
        ipProtoNames.put(IpProtocol.PRIVATE_ENCRYPT, "PRIVATE_ENCRYPT");
        ipProtoNames.put(IpProtocol.GMTP, "GMTP");
        ipProtoNames.put(IpProtocol.IFMP, "IFMP");
        ipProtoNames.put(IpProtocol.PNNI, "PNNI");
        ipProtoNames.put(IpProtocol.PIM, "PIM");
        ipProtoNames.put(IpProtocol.ARIS, "ARIS");
        ipProtoNames.put(IpProtocol.SCPS, "SCPS");
        ipProtoNames.put(IpProtocol.QNX, "QNX");
        ipProtoNames.put(IpProtocol.A_N, "A_N");
        ipProtoNames.put(IpProtocol.IP_COMP, "IP_COMP");
        ipProtoNames.put(IpProtocol.SNP, "SNP");
        ipProtoNames.put(IpProtocol.COMPAQ_PEER, "COMPAQ_PEER");
        ipProtoNames.put(IpProtocol.IPX_IN_IP, "IPX_IN_IP");
        ipProtoNames.put(IpProtocol.VRRP, "VRRP");
        ipProtoNames.put(IpProtocol.PGM, "PGM");
        ipProtoNames.put(IpProtocol.ZERO_HOP, "ZERO_HOP");
        ipProtoNames.put(IpProtocol.L2TP, "L2TP");
        ipProtoNames.put(IpProtocol.DDX, "DDX");
        ipProtoNames.put(IpProtocol.IATP, "IATP");
        ipProtoNames.put(IpProtocol.STP, "STP");
        ipProtoNames.put(IpProtocol.SRP, "SRP");
        ipProtoNames.put(IpProtocol.UTI, "UTI");
        ipProtoNames.put(IpProtocol.SMP, "SMP");
        ipProtoNames.put(IpProtocol.SM, "SM");
        ipProtoNames.put(IpProtocol.PTP, "PTP");
        ipProtoNames.put(IpProtocol.IS_IS_OVER_IPv4, "IS_IS_OVER_IPv4");
        ipProtoNames.put(IpProtocol.FIRE, "FIRE");
        ipProtoNames.put(IpProtocol.CRTP, "CRTP");
        ipProtoNames.put(IpProtocol.CRUDP, "CRUDP");
        ipProtoNames.put(IpProtocol.SSCOPMCE, "SSCOPMCE");
        ipProtoNames.put(IpProtocol.IPLT, "IPLT");
        ipProtoNames.put(IpProtocol.SPS, "SPS");
        ipProtoNames.put(IpProtocol.PIPE, "PIPE");
        ipProtoNames.put(IpProtocol.SCTP, "SCTP");
        ipProtoNames.put(IpProtocol.FC, "FC");
        ipProtoNames.put(IpProtocol.RSVP_E2E_IGNORE, "RSVP_E2E_IGNORE");
        ipProtoNames.put(IpProtocol.MOBILITY_HEADER, "MOBILITY_HEADER");
        ipProtoNames.put(IpProtocol.UDP_LITE, "UDP_LITE");
        ipProtoNames.put(IpProtocol.MPLS_IN_IP, "MPLS_IN_IP");
        ipProtoNames.put(IpProtocol.MANET, "MANET");
        ipProtoNames.put(IpProtocol.HIP, "HIP");
        ipProtoNames.put(IpProtocol.SHIM6, "SHIM6");
        IPPROTO_NAMES = Collections.unmodifiableMap(ipProtoNames);

        Map<ICMPv4Type, String> icmpTypeNames = new HashMap<>();
        icmpTypeNames.put(ICMPv4Type.ECHO_REPLY, "none/ECHO_REPLY");
        icmpTypeNames.put(ICMPv4Type.DESTINATION_UNREACHABLE, "DESTINATION_UNREACHABLE");
        icmpTypeNames.put(ICMPv4Type.SOURCE_QUENCH, "SOURCE_QUENCH");
        icmpTypeNames.put(ICMPv4Type.REDIRECT, "REDIRECT");
        icmpTypeNames.put(ICMPv4Type.ALTERNATE_HOST_ADDRESS, "ALTERNATE_HOST_ADDRESS");
        icmpTypeNames.put(ICMPv4Type.ECHO, "ECHO");
        icmpTypeNames.put(ICMPv4Type.ROUTER_ADVERTISEMENT, "ROUTER_ADVERTISEMENT");
        icmpTypeNames.put(ICMPv4Type.ROUTER_SOLICITATION, "ROUTER_SOLICITATION");
        icmpTypeNames.put(ICMPv4Type.TIME_EXCEEDED, "TIME_EXCEEDED");
        icmpTypeNames.put(ICMPv4Type.PARAMETER_PROBLEM, "PARAMETER_PROBLEM");
        icmpTypeNames.put(ICMPv4Type.TIMESTAMP, "TIMESTAMP");
        icmpTypeNames.put(ICMPv4Type.TIMESTAMP_REPLY, "TIMESTAMP_REPLY");
        icmpTypeNames.put(ICMPv4Type.INFORMATION_REQUEST, "INFORMATION_REQUEST");
        icmpTypeNames.put(ICMPv4Type.INFORMATION_REPLY, "INFORMATION_REPLY");
        icmpTypeNames.put(ICMPv4Type.ADDRESS_MASK_REQUEST, "ADDRESS_MASK_REQUEST");
        icmpTypeNames.put(ICMPv4Type.ADDRESS_MASK_REPLY, "ADDRESS_MASK_REPLY");
        icmpTypeNames.put(ICMPv4Type.TRACEROUTE, "TRACEROUTE");
        icmpTypeNames.put(ICMPv4Type.DATAGRAM_CONVERSION_ERROR, "DATAGRAM_CONVERSION_ERROR");
        icmpTypeNames.put(ICMPv4Type.MOBILE_HOST_REDIRECT, "MOBILE_HOST_REDIRECT");
        icmpTypeNames.put(ICMPv4Type.IPV6_WHERE_ARE_YOU, "IPV6_WHERE_ARE_YOU");
        icmpTypeNames.put(ICMPv4Type.IPV6_I_AM_HERE, "IPV6_I_AM_HERE");
        icmpTypeNames.put(ICMPv4Type.MOBILE_REGISTRATION_REQUEST, "MOBILE_REGISTRATION_REQUEST");
        icmpTypeNames.put(ICMPv4Type.MOBILE_REGISTRATION_REPLY, "MOBILE_REGISTRATION_REPLY");
        icmpTypeNames.put(ICMPv4Type.DOMAIN_NAME_REQUEST, "DOMAIN_NAME_REQUEST");
        icmpTypeNames.put(ICMPv4Type.DOMAIN_NAME_REPLY, "DOMAIN_NAME_REPLY");
        icmpTypeNames.put(ICMPv4Type.SKIP, "SKIP");
        icmpTypeNames.put(ICMPv4Type.PHOTURIS, "PHOTURIS");
        icmpTypeNames.put(ICMPv4Type.EXPERIMENTAL_MOBILITY, "EXPERIMENTAL_MOBILITY");
        ICMPTYPE_NAMES = Collections.unmodifiableMap(icmpTypeNames);
    }

    private Fields()
    {
        // not used
    }
}
