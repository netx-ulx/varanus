package net.varanus.util.openflow;


import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;

import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFMessageReader;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.protocol.match.MatchFields;
import org.projectfloodlight.openflow.protocol.ver10.ChannelUtilsVer10;
import org.projectfloodlight.openflow.protocol.ver11.ChannelUtilsVer11;
import org.projectfloodlight.openflow.protocol.ver12.ChannelUtilsVer12;
import org.projectfloodlight.openflow.protocol.ver13.ChannelUtilsVer13;
import org.projectfloodlight.openflow.protocol.ver14.ChannelUtilsVer14;
import org.projectfloodlight.openflow.types.ArpOpcode;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.ICMPv4Code;
import org.projectfloodlight.openflow.types.ICMPv4Type;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IPv6Address;
import org.projectfloodlight.openflow.types.IpDscp;
import org.projectfloodlight.openflow.types.IpEcn;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.Masked;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.OFValueType;
import org.projectfloodlight.openflow.types.OFVlanVidMatch;
import org.projectfloodlight.openflow.types.TransportPort;
import org.projectfloodlight.openflow.types.U16;
import org.projectfloodlight.openflow.types.U8;
import org.projectfloodlight.openflow.types.VlanPcp;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.functional.StreamUtils;
import net.varanus.util.lang.MoreObjects;
import net.varanus.util.openflow.types.MaskType;
import net.varanus.util.openflow.types.MatchEntry;
import net.varanus.util.text.StringUtils;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class MatchUtils
{
    public static final String GEN_MATCH_FMT_DESCRIPT = "[<FIELD>=<VALUE>, <FIELD>=<VALUE>, ...]";
    public static final String VER_MATCH_FMT_DESCRIPT = "v##" + GEN_MATCH_FMT_DESCRIPT;

    public static final Pattern GEN_MATCH_FMT_PATTERN = Pattern.compile("\\[(?>[^,\\]]+(?>,[^,\\]]+)*)?\\]");
    public static final Pattern VER_MATCH_FMT_PATTERN = Pattern.compile("v\\d+" + GEN_MATCH_FMT_PATTERN);

    public static <T extends OFValueType<T>> Masked<T> asMasked( MatchField<?> field,
                                                                 T value,
                                                                 T mask,
                                                                 MaskType maskType )
    {
        if (maskType.isPartiallyMasked() && !MatchFieldUtils.supportsPartialMask(field))
            throw new UnsupportedOperationException(
                String.format("match field %s does not support partial masking", field.id));

        return OFValueTypeUtils.asMasked(value, mask);
    }

    public static Match parse( String s ) throws IllegalArgumentException
    {
        // be nice to the user and accept leading or trailing whitespace
        s = s.trim();

        if (s.startsWith("v10"))
            return parseFromEntries(s.substring(3), OFVersion.OF_10);
        else if (s.startsWith("v11"))
            return parseFromEntries(s.substring(3), OFVersion.OF_11);
        else if (s.startsWith("v12"))
            return parseFromEntries(s.substring(3), OFVersion.OF_12);
        else if (s.startsWith("v13"))
            return parseFromEntries(s.substring(3), OFVersion.OF_13);
        else if (s.startsWith("v14"))
            return parseFromEntries(s.substring(3), OFVersion.OF_14);
        else
            throw new IllegalArgumentException("match string must begin with version number 'v##'");
    }

    public static Match parse( String s, OFVersion ofVersion ) throws IllegalArgumentException
    {
        // be nice to the user and accept leading or trailing whitespace
        s = s.trim();

        return parseFromEntries(s, ofVersion);
    }

    private static Match parseFromEntries( String list, OFVersion ver ) throws IllegalArgumentException
    {
        Match.Builder builder = OFFactories.getFactory(ver).buildMatch();
        List<MatchEntry<?>> entries = StringUtils.parseList(list, MatchEntry::parse);
        entries.forEach(( entry ) -> entry.addToBuilder(builder));
        return builder.build();
    }

    public static Match fromPacketIn( OFPacketIn pIn )
    {
        byte[] data = pIn.getData();
        OFPort inPort = OFMessageUtils.getInPort(pIn);
        if (inPort == null) inPort = OFPort.ZERO;

        return loadFromPacket(data, inPort, OFFactories.getFactory(pIn.getVersion()));
    }

    /**
     * @param packetData
     * @param inputPort
     * @param factory
     * @return a {@code Match} instance
     */
    public static Match loadFromPacket( byte[] packetData, OFPort inputPort, OFFactory factory )
    {
        short scratch;
        int transportOffset = 34;
        ByteBuffer packetDataBB = ByteBuffer.wrap(packetData);
        int limit = packetDataBB.limit();
        assert (limit >= 14);

        Match.Builder builder = factory.buildMatch();

        if (!Objects.equals(inputPort, OFPort.ALL))
            builder.setExact(MatchField.IN_PORT, inputPort);

        // dl dst
        byte[] dataLayerDestination = new byte[6];
        packetDataBB.get(dataLayerDestination);
        builder.setExact(MatchField.ETH_DST, MacAddress.of(dataLayerDestination));
        // dl src
        byte[] dataLayerSource = new byte[6];
        packetDataBB.get(dataLayerSource);
        builder.setExact(MatchField.ETH_SRC, MacAddress.of(dataLayerSource));
        // dl type
        int dataLayerType = U16.f(packetDataBB.getShort());
        builder.setExact(MatchField.ETH_TYPE, EthType.of(dataLayerType));

        if (dataLayerType != 0x8100) {
            // bug
            builder.setExact(MatchField.VLAN_VID, OFVlanVidMatch.UNTAGGED);
            builder.setExact(MatchField.VLAN_PCP, VlanPcp.NONE);
        }
        else {
            // has vlan tag
            scratch = packetDataBB.getShort();
            builder.setExact(MatchField.VLAN_VID, OFVlanVidMatch.ofVlan(0xfff & scratch));
            builder.setExact(MatchField.VLAN_PCP, VlanPcp.of((byte)((0xe000 & scratch) >> 13)));
            dataLayerType = U16.f(packetDataBB.getShort());
        }

        short networkProtocol;
        int networkSource;
        int networkDestination;

        switch (dataLayerType) {
            case 0x0800:
                // ipv4
                // check packet length
                scratch = packetDataBB.get();
                scratch = (short)(0xf & scratch);
                transportOffset = (packetDataBB.position() - 1) + (scratch * 4);
                // nw tos (dscp+ecn)
                scratch = packetDataBB.get();
                builder.setExact(MatchField.IP_ECN, IpEcn.of((byte)(scratch & 0x03)));
                builder.setExact(MatchField.IP_DSCP, IpDscp.of((byte)((0xfc & scratch) >> 2)));
                // nw protocol
                packetDataBB.position(packetDataBB.position() + 7);
                networkProtocol = U8.f(packetDataBB.get());
                builder.setExact(MatchField.IP_PROTO, IpProtocol.of(networkProtocol));
                // nw src
                packetDataBB.position(packetDataBB.position() + 2);
                networkSource = packetDataBB.getInt();
                builder.setExact(MatchField.IPV4_SRC, IPv4Address.of(networkSource));
                // nw dst
                networkDestination = packetDataBB.getInt();
                builder.setExact(MatchField.IPV4_DST, IPv4Address.of(networkDestination));
                packetDataBB.position(transportOffset);

                int port;
                switch (networkProtocol) {
                    case 0x01:
                        // icmp
                        // type
                        short type = U8.f(packetDataBB.get());
                        builder.setExact(MatchField.ICMPV4_TYPE, ICMPv4Type.of(type));
                        // code
                        short code = U8.f(packetDataBB.get());
                        builder.setExact(MatchField.ICMPV4_CODE, ICMPv4Code.of(code));
                    break;
                    case 0x06:
                        // tcp
                        // tcp src
                        port = U16.f(packetDataBB.getShort());
                        builder.setExact(MatchField.TCP_SRC, TransportPort.of(port));
                        // tcp dest
                        port = U16.f(packetDataBB.getShort());
                        builder.setExact(MatchField.TCP_DST, TransportPort.of(port));
                    break;
                    case 0x11:
                        // udp
                        // udp src
                        port = U16.f(packetDataBB.getShort());
                        builder.setExact(MatchField.UDP_SRC, TransportPort.of(port));
                        // udp dest
                        port = U16.f(packetDataBB.getShort());
                        builder.setExact(MatchField.UDP_DST, TransportPort.of(port));
                    break;
                    default:
                    // Unknown network proto.
                    break;
                }
            break;
            case 0x0806:
                // arp
                int arpPos = packetDataBB.position();
                // opcode
                scratch = packetDataBB.getShort(arpPos + 6);
                builder.setExact(MatchField.ARP_OP, ArpOpcode.of(0xff & scratch));

                scratch = packetDataBB.getShort(arpPos + 2);
                // if ipv4 and addr len is 4
                if (scratch == 0x800 && packetDataBB.get(arpPos + 5) == 4) {
                    networkSource = packetDataBB.getInt(arpPos + 14);
                    networkDestination = packetDataBB.getInt(arpPos + 24);
                }
                else {
                    networkSource = 0;
                    networkDestination = 0;
                }
                builder.setExact(MatchField.ARP_SPA, IPv4Address.of(networkSource));
                builder.setExact(MatchField.ARP_TPA, IPv4Address.of(networkDestination));
            break;

            default:
            // Not ARP or IP.
            // Don't specify any network fields
            break;
        }

        return builder.build();
    }

    public static Match create( OFVersion ver, MatchEntry<?>... entries )
    {
        return create(OFFactories.getFactory(ver), Arrays.asList(entries));
    }

    public static Match create( OFFactory fact, MatchEntry<?>... entries )
    {
        return create(fact, Arrays.asList(entries));
    }

    public static Match create( OFVersion ver, Iterable<MatchEntry<?>> entries )
    {
        return create(OFFactories.getFactory(ver), entries);
    }

    public static Match create( OFFactory fact, Iterable<MatchEntry<?>> entries )
    {
        Match.Builder builder = fact.buildMatch();
        for (MatchEntry<?> e : entries) {
            e.addToBuilder(builder);
        }
        return builder.build();
    }

    public static Match create( OFVersion ver, Stream<MatchEntry<?>> entries )
    {
        return create(OFFactories.getFactory(ver), entries);
    }

    public static Match create( OFFactory fact, Stream<MatchEntry<?>> entries )
    {
        Match.Builder builder = fact.buildMatch();
        entries.forEach(e -> e.addToBuilder(builder));
        return builder.build();
    }

    public static Match.Builder builderFrom( Match match )
    {
        return convertToBuilder(match, match.getVersion());
    }

    public static Match convert( Match match, OFVersion ver )
    {
        return convert(match, OFFactories.getFactory(ver));
    }

    public static Match convert( Match match, OFFactory fact )
    {
        if (fact.getVersion().equals(match.getVersion()))
            return match; // shortcut
        else
            return convertToBuilder(match, fact).build();
    }

    public static Match.Builder convertToBuilder( Match match, OFVersion ver )
    {
        return convertToBuilder(match, OFFactories.getFactory(ver));
    }

    public static Match.Builder convertToBuilder( Match match, OFFactory fact )
    {
        Match.Builder builder = fact.buildMatch();
        MatchEntry.streamEntries(match).forEach(me -> me.addToBuilder(builder));
        return builder;
    }

    public static <T extends OFValueType<T>> boolean matches( Match match, MatchField<T> field, T value )
    {
        return MatchEntry.fromMatch(match, field).matches(value);
    }

    public static <T extends OFValueType<T>> boolean matchesExact( Match match, MatchField<T> field, T value )
    {
        return MatchEntry.fromMatch(match, field).matchesOnly(value);
    }

    public static <T extends OFValueType<T>> boolean matches( Match match, MatchEntry<T> entry )
    {
        return MatchEntry.fromMatch(match, entry.getField()).matchesAllOf(entry.getMasked());
    }

    public static boolean matchesAllOf( Match match, Match candidate )
    {
        MoreObjects.requireNonNull(match, "match", candidate, "candidate");
        if (match == candidate)
            return true; // optimization
        else
            return MatchEntry.streamEntries(candidate).allMatch(entry -> matches(match, entry));
    }

    public static OFMessageReader<Match> getReader( OFVersion ver )
    {
        switch (ver) {
            case OF_10:
                return ChannelUtilsVer10::readOFMatch;

            case OF_11:
                return ChannelUtilsVer11::readOFMatch;

            case OF_12:
                return ChannelUtilsVer12::readOFMatch;

            case OF_13:
                return ChannelUtilsVer13::readOFMatch;

            case OF_14:
                return ChannelUtilsVer14::readOFMatch;

            default:
                throw new AssertionError("Unexpected enum value");
        }
    }

    public static String toPrettyFormat( Match match )
    {
        /*
         * Partitions all match entries into those with pretty format and those
         * without, each partition further mapped by match field.
         */
        Map<Boolean, Map<MatchFields, MatchEntry<?>>> partition = MatchEntry.streamEntries(match)
            .collect(Collectors.partitioningBy(MatchUtils::hasPrettyField,
                StreamUtils.toMapCollector(
                    me -> me.getField().id,
                    Function.identity(),
                    () -> new EnumMap<>(MatchFields.class))));

        Map<MatchFields, MatchEntry<?>> pretty = partition.get(true);
        Map<MatchFields, MatchEntry<?>> nonPretty = partition.get(false);

        if (pretty.isEmpty() && nonPretty.isEmpty()) {
            return "[ * ]"; // match is all-wildcards
        }
        else {
            StringBuilder sb = new StringBuilder();
            boolean isFirstSection = true;

            MatchEntry<MacAddress> srcMac = extractEntry(pretty, MatchField.ETH_SRC);
            MatchEntry<MacAddress> destMac = extractEntry(pretty, MatchField.ETH_DST);
            if (!(srcMac.isWildcard() && destMac.isWildcard())) {
                isFirstSection = false;
                sb.append("ETH[")
                    .append(srcMac.getValueString()).append(" -> ").append(destMac.getValueString())
                    .append("]");
            }

            MatchEntry<EthType> ethType = extractEntry(pretty, MatchField.ETH_TYPE);
            if (!ethType.isWildcard()) {
                if (ethType.matchesOnly(EthType.IPv4)) {
                    if (!isFirstSection)
                        sb.append(" | ");
                    isFirstSection = false;
                    MatchEntry<IPv4Address> src = extractEntry(pretty, MatchField.IPV4_SRC);
                    MatchEntry<IPv4Address> dest = extractEntry(pretty, MatchField.IPV4_DST);
                    sb.append("IPv4[")
                        .append(src.getValueString()).append(" -> ").append(dest.getValueString())
                        .append("]");
                }
                else if (ethType.matchesOnly(EthType.IPv6)) {
                    if (!isFirstSection)
                        sb.append(" | ");
                    isFirstSection = false;
                    MatchEntry<IPv6Address> src = extractEntry(pretty, MatchField.IPV6_SRC);
                    MatchEntry<IPv6Address> dest = extractEntry(pretty, MatchField.IPV6_DST);
                    sb.append("IPv6[")
                        .append(src.getValueString()).append(" -> ").append(dest.getValueString())
                        .append("]");
                }
                else {
                    // put it back in the map to be printed later
                    pretty.put(MatchFields.ETH_TYPE, ethType);
                }
            }

            if (!(pretty.isEmpty() && nonPretty.isEmpty())) {
                if (!isFirstSection)
                    sb.append(" | ");
                sb.append(StringUtils.joinAllPS(", ", "[", "]",
                    Stream.concat(pretty.values().stream(), nonPretty.values().stream())));
            }

            return sb.toString();
        }
    }

    private static boolean hasPrettyField( MatchEntry<?> entry )
    {
        return PRETTY_FIELDS.contains(entry.getField().id);
    }

    @SuppressWarnings( "unchecked" )
    private static <T extends OFValueType<T>> MatchEntry<T> extractEntry( Map<MatchFields, MatchEntry<?>> map,
                                                                          MatchField<T> field )
    {
        MatchEntry<T> entry = (MatchEntry<T>)map.remove(field.id);
        return (entry != null) ? entry : MatchEntry.ofWildcard(field);
    }

    private static final Set<MatchFields> PRETTY_FIELDS = Collections.unmodifiableSet(EnumSet.of(
        MatchFields.ETH_DST, MatchFields.ETH_SRC, MatchFields.ETH_TYPE,
        MatchFields.IPV4_SRC, MatchFields.IPV4_DST,
        MatchFields.IPV6_SRC, MatchFields.IPV6_DST));

    private MatchUtils()
    {
        // not used
    }
}
