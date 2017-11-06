package net.varanus.sdncontroller.util;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.ICMPv4Code;
import org.projectfloodlight.openflow.types.ICMPv4Type;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFVlanVidMatch;
import org.projectfloodlight.openflow.types.TransportPort;
import org.projectfloodlight.openflow.types.VlanPcp;
import org.projectfloodlight.openflow.util.HexString;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.ImmutableClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;
import com.google.common.collect.Sets;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.packet.Data;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.ICMP;
import net.floodlightcontroller.packet.IPacket;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.TCP;
import net.floodlightcontroller.packet.UDP;


/**
 * 
 */
public final class IPacketUtils
{
    public static String toString( IPacket packet )
    {
        StringBuilder sb = new StringBuilder();

        for (;;) {
            putIPacketAsString(packet, sb);
            packet = packet.getPayload();
            if (packet == null) {
                break;
            }
            else {
                sb.append("|");
            }
        }

        return sb.toString();
    }

    private static void putIPacketAsString( IPacket packet, StringBuilder sb )
    {
        if (packet instanceof Ethernet) {
            putEthernetAsString((Ethernet)packet, sb);
        }
        else if (packet instanceof IPv4) {
            putIPv4AsString((IPv4)packet, sb);
        }
        else if (packet instanceof ICMP) {
            putICMPAsString((ICMP)packet, sb);
        }
        else if (packet instanceof UDP) {
            putUDPAsString((UDP)packet, sb);
        }
        else if (packet instanceof TCP) {
            putTCPAsString((TCP)packet, sb);
        }
        else if (packet instanceof Data) {
            putDataAsString((Data)packet, sb);
        }
        else {
            sb.append("UNKNOWN");
        }
    }

    public static Ethernet getPacketInEthFrame( OFPacketIn pIn, FloodlightContext cntx )
    {
        Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
        if (eth == null) {
            eth = new Ethernet();
            eth.deserialize(pIn.getData(), 0, pIn.getData().length);
        }

        return eth;
    }

    public static IPacket fromMatch( Match match )
    {
        return fromMatch(match, new byte[0]);
    }

    /**
     * Returns an {@code IPacket} that matches the given flow rule match.
     * <p>
     * The returned packet contains a {@code Data} instance with the provided
     * payload bytes.
     * 
     * @param match
     * @param payload
     * @return an {@code IPacket} that matches the given flow rule match
     */
    public static IPacket fromMatch( Match match, byte[] payload )
    {
        Ethernet eth = newEthernet(match);
        if (eth.getEtherType().equals(EthType.IPv4)) {
            IPv4 ip = newIPv4(match);
            eth.setPayload(ip);
            if (ip.getProtocol().equals(IpProtocol.ICMP)) {
                ICMP icmp = newICMP(match);
                ip.setPayload(icmp);
                icmp.setPayload(new Data(payload));
            }
            else if (ip.getProtocol().equals(IpProtocol.UDP)) {
                UDP udp = newUDP(match);
                ip.setPayload(udp);
                udp.setPayload(new Data(payload));
            }
            else if (ip.getProtocol().equals(IpProtocol.TCP)) {
                TCP tcp = newTCP(match);
                ip.setPayload(tcp);
                tcp.setPayload(new Data(payload));
            }
            else {
                ip.setPayload(new Data(payload));
            }
        }
        else {
            eth.setPayload(new Data(payload));
        }

        return eth;
    }

    public static IPacket getDeepestPayload( IPacket packet )
    {
        while (packet.getPayload() != null) {
            packet = packet.getPayload();
        }

        return packet;
    }

    private static final ClassToInstanceMap<Object>    DEFAULTS;
    private static final Map<Class<?>, Supportable<?>> SUPPORTED;

    // TODO add more fields if necessary
    static {
        MutableClassToInstanceMap<Object> defs = MutableClassToInstanceMap.create();
        defs.putInstance(MacAddress.class, MacAddress.NONE);
        defs.putInstance(EthType.class, EthType.NONE);
        defs.putInstance(OFVlanVidMatch.class, OFVlanVidMatch.NONE);
        defs.putInstance(VlanPcp.class, VlanPcp.NONE);
        defs.putInstance(IPv4Address.class, IPv4Address.NONE);
        defs.putInstance(IpProtocol.class, IpProtocol.NONE);
        defs.putInstance(ICMPv4Type.class, ICMPv4Type.NONE);
        defs.putInstance(ICMPv4Code.class, ICMPv4Code.NONE);
        defs.putInstance(TransportPort.class, TransportPort.NONE);
        DEFAULTS = ImmutableClassToInstanceMap.copyOf(defs);

        Map<Class<?>, Supportable<?>> supp = new HashMap<>();
        supp.put(MacAddress.class, allExceptNull());
        supp.put(EthType.class, Supportable.of(Sets.newHashSet(EthType.NONE, EthType.IPv4)));
        supp.put(OFVlanVidMatch.class, allExceptNull());
        supp.put(VlanPcp.class, allExceptNull());
        supp.put(IPv4Address.class, allExceptNull());
        supp.put(
            IpProtocol.class,
            Supportable.of(Sets.newHashSet(IpProtocol.NONE, IpProtocol.ICMP, IpProtocol.UDP, IpProtocol.TCP)));
        supp.put(ICMPv4Type.class, allExceptNull());
        supp.put(ICMPv4Code.class, allExceptNull());
        supp.put(TransportPort.class, allExceptNull());
        SUPPORTED = Collections.unmodifiableMap(supp);
    }

    private static <T> Supportable<T> allExceptNull()
    {
        return Supportable.ofComplementOf(Collections.singleton((T)null));
    }

    private static <T> T defaultable( T value, Class<T> klass )
    {
        return (value != null ? value : defaultOf(klass));
    }

    private static <T> T defaultOf( Class<T> klass )
    {
        return Objects.requireNonNull(DEFAULTS.getInstance(klass));
    }

    private static <T> T supportable( T value, Class<T> klass )
    {
        return (supportableOf(klass).supports(value) ? value : defaultable(value, klass));
    }

    private static <T> Supportable<T> supportableOf( Class<T> klass )
    {
        @SuppressWarnings( "unchecked" )
        Supportable<T> sup = (Supportable<T>)Objects.requireNonNull(SUPPORTED.get(klass));
        return sup;
    }

    private static Ethernet newEthernet( Match match )
    {
        MacAddress srcAddr = supportable(match.get(MatchField.ETH_SRC), MacAddress.class);
        MacAddress dstAddr = supportable(match.get(MatchField.ETH_DST), MacAddress.class);
        EthType type = supportable(match.get(MatchField.ETH_TYPE), EthType.class);
        OFVlanVidMatch vlanID = supportable(match.get(MatchField.VLAN_VID), OFVlanVidMatch.class);
        VlanPcp vlanPrio = supportable(match.get(MatchField.VLAN_PCP), VlanPcp.class);

        return new Ethernet()
            .setSourceMACAddress(srcAddr)
            .setDestinationMACAddress(dstAddr)
            .setEtherType(type)
            .setVlanID(vlanID.getVlan())
            .setPriorityCode(vlanPrio.getValue());
    }

    private static void putEthernetAsString( Ethernet eth, StringBuilder sb )
    {
        sb.append("Eth[");
        sb.append("src=").append(eth.getSourceMACAddress());
        sb.append(", dst=").append(eth.getDestinationMACAddress());
        sb.append(", type=").append(eth.getEtherType())
            .append(" (" + Fields.getEtherTypeName(eth.getEtherType()) + ")");
        if (eth.getVlanID() != Ethernet.VLAN_UNTAGGED) {
            sb.append(", vlan_id=").append(Fields.getVlanID(eth));
            sb.append(", vlan_pri=").append(Fields.getVlanPriorityCode(eth));
        }
        sb.append(']');
    }

    private static IPv4 newIPv4( Match match )
    {
        IPv4Address srcAddr = supportable(match.get(MatchField.IPV4_SRC), IPv4Address.class);
        IPv4Address dstAddr = supportable(match.get(MatchField.IPV4_DST), IPv4Address.class);
        IpProtocol ipProto = supportable(match.get(MatchField.IP_PROTO), IpProtocol.class);

        return new IPv4()
            .setSourceAddress(srcAddr)
            .setDestinationAddress(dstAddr)
            .setProtocol(ipProto);
    }

    private static void putIPv4AsString( IPv4 ip, StringBuilder sb )
    {
        sb.append("IPv4[");
        sb.append("src=").append(ip.getSourceAddress());
        sb.append(", dst=").append(ip.getDestinationAddress());
        sb.append(", proto=").append(ip.getProtocol()).append(" (" + Fields.getIPProtoName(ip.getProtocol()) + ")");
        sb.append(']');
    }

    private static ICMP newICMP( Match match )
    {
        ICMPv4Type type = supportable(match.get(MatchField.ICMPV4_TYPE), ICMPv4Type.class);
        ICMPv4Code code = supportable(match.get(MatchField.ICMPV4_CODE), ICMPv4Code.class);

        return new ICMP()
            .setIcmpType((byte)type.getType())
            .setIcmpCode((byte)code.getCode());
    }

    private static void putICMPAsString( ICMP icmp, StringBuilder sb )
    {
        ICMPv4Type type = Fields.getIcmpType(icmp);
        ICMPv4Code code = Fields.getIcmpCode(icmp);

        sb.append("ICMPv4[");
        sb.append("type=").append(type).append(" (" + Fields.getICMPTypeName(type) + ")");
        sb.append(", code=").append(code);
        sb.append(']');
    }

    private static UDP newUDP( Match match )
    {
        TransportPort srcPort = supportable(match.get(MatchField.UDP_SRC), TransportPort.class);
        TransportPort dstPort = supportable(match.get(MatchField.UDP_DST), TransportPort.class);

        return new UDP()
            .setSourcePort(srcPort)
            .setDestinationPort(dstPort);
    }

    private static void putUDPAsString( UDP udp, StringBuilder sb )
    {
        sb.append("UDP[");
        sb.append("src=").append(udp.getSourcePort());
        sb.append(", dst=").append(udp.getDestinationPort());
        sb.append(']');
    }

    private static TCP newTCP( Match match )
    {
        TransportPort srcPort = supportable(match.get(MatchField.TCP_SRC), TransportPort.class);
        TransportPort dstPort = supportable(match.get(MatchField.TCP_DST), TransportPort.class);

        return new TCP()
            .setSourcePort(srcPort)
            .setDestinationPort(dstPort);
    }

    private static void putTCPAsString( TCP tcp, StringBuilder sb )
    {
        sb.append("TCP[");
        sb.append("src=").append(tcp.getSourcePort());
        sb.append(", dst=").append(tcp.getDestinationPort());
        sb.append(']');
    }

    private static void putDataAsString( Data packet, StringBuilder sb )
    {
        sb.append("DATA[");
        sb.append(HexString.toHexString(packet.getData()));
        sb.append(']');
    }

    private static final class Supportable<T>
    {
        static <T> Supportable<T> of( Set<T> set )
        {
            return new Supportable<>(set, false);
        }

        static <T> Supportable<T> ofComplementOf( Set<T> set )
        {
            return new Supportable<>(set, true);
        }

        private final Set<T>  set;
        private final boolean complement;

        private Supportable( Set<T> set, boolean complement )
        {
            this.set = Objects.requireNonNull(set);
            this.complement = complement;
        }

        boolean supports( T el )
        {
            boolean contains = set.contains(el);
            return (complement ? !contains : contains);
        }
    }

    private IPacketUtils()
    {
        // not used
    }
}
