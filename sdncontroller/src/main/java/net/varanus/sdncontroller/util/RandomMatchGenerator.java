package net.varanus.sdncontroller.util;


import static net.varanus.sdncontroller.util.Fields.MAX_ICMPv4_CODE;
import static net.varanus.sdncontroller.util.Fields.MAX_ICMPv4_TYPE;
import static net.varanus.sdncontroller.util.Fields.MAX_TRANSPORT_PORT;
import static net.varanus.sdncontroller.util.Fields.MIN_ICMPv4_CODE;
import static net.varanus.sdncontroller.util.Fields.MIN_ICMPv4_TYPE;
import static net.varanus.sdncontroller.util.Fields.MIN_TRANSPORT_PORT;
import static net.varanus.sdncontroller.util.RandomMatchGenerator.L2Option.LOCALLY_ADMINISTERED_MACS;
import static net.varanus.sdncontroller.util.RandomMatchGenerator.L2Option.UNICAST_MACS;
import static net.varanus.sdncontroller.util.RandomMatchGenerator.L3Option.PRIVATE_IPv4_ADDRESSES;
import static net.varanus.sdncontroller.util.RandomMatchGenerator.L3Option.USE_IPv4;
import static net.varanus.util.lang.Comparables.aGTb;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;

import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.ICMPv4Code;
import org.projectfloodlight.openflow.types.ICMPv4Type;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.TransportPort;

import net.varanus.util.security.SecureRandoms;


/**
 * 
 */
public final class RandomMatchGenerator
{
    public static enum L2Option
    {
        UNICAST_MACS,
        LOCALLY_ADMINISTERED_MACS,
    }

    public static enum L3Option
    {
        USE_IPv4,
        PRIVATE_IPv4_ADDRESSES,
    }

    public static enum L4Option
    {
        USE_UDP,
        USE_TCP,
        USE_ICMP
    }

    private static final OFVersion MATCH_VERSION = OFVersion.OF_14;

    private static final byte UNICAST_MAC_ADDR_AND_MASK      = (byte)0b11111110;
    private static final byte LOCALLY_ADMIN_MAC_ADDR_OR_MASK = 0b00000010;

    private static final IPv4AddressMultiRange PRIVATE_IPv4_ADDRS;

    static {
        PRIVATE_IPv4_ADDRS = new IPv4AddressMultiRange(
            addressRangeOf("10.0.0.0", "10.255.255.255"),
            addressRangeOf("172.16.0.0", "172.31.255.255"),
            addressRangeOf("192.168.0.0", "192.168.255.255"));
    }

    private static IPv4AddressRange addressRangeOf( String start, String end )
    {
        return new IPv4AddressRange(IPv4Address.of(start), IPv4Address.of(end));
    }

    private final Random        rand;
    private final Set<L2Option> l2Opts;
    private final Set<L3Option> l3Opts;
    private final Set<L4Option> l4Opts;

    public RandomMatchGenerator( Set<L2Option> l2Opts, Set<L3Option> l3Opts, Set<L4Option> l4Opts )
    {
        this.rand = SecureRandoms.newStrongSeedSecureRandom(true);
        this.l2Opts = Objects.requireNonNull(l2Opts);
        this.l3Opts = Objects.requireNonNull(l3Opts);
        this.l4Opts = Objects.requireNonNull(l4Opts);
    }

    public Match newMatch()
    {
        Match.Builder builder = OFFactories.getFactory(MATCH_VERSION).buildMatch();
        putL2Fields(builder);
        putL3Fields(builder);
        putL4Fields(builder);
        return builder.build();
    }

    private void putL2Fields( Match.Builder builder )
    {
        builder.setExact(MatchField.ETH_SRC, newMACAddress());
        builder.setExact(MatchField.ETH_DST, newMACAddress());
    }

    private void putL3Fields( Match.Builder builder )
    {
        if (l3Opts.contains(USE_IPv4)) {
            builder.setExact(MatchField.ETH_TYPE, EthType.IPv4);
            builder.setExact(MatchField.IPV4_SRC, newIPv4Address());
            builder.setExact(MatchField.IPV4_DST, newIPv4Address());
        }
    }

    private void putL4Fields( Match.Builder builder )
    {
        final int numL4Opts = l4Opts.size();
        if (numL4Opts > 0) {
            int optIndex = rand.nextInt(numL4Opts);
            L4Option opt = l4Opts.toArray(new L4Option[numL4Opts])[optIndex];
            switch (opt) {
                case USE_ICMP:
                    if (l3Opts.contains(USE_IPv4)) {
                        builder.setExact(MatchField.IP_PROTO, IpProtocol.ICMP);
                        builder.setExact(MatchField.ICMPV4_TYPE, randomICMPv4Type(rand));
                        builder.setExact(MatchField.ICMPV4_CODE, randomICMPv4Code(rand));
                    }
                break;

                case USE_TCP:
                    if (l3Opts.contains(USE_IPv4)) {
                        builder.setExact(MatchField.IP_PROTO, IpProtocol.TCP);
                        builder.setExact(MatchField.TCP_SRC, randomTransportPort(rand));
                        builder.setExact(MatchField.TCP_DST, randomTransportPort(rand));
                    }
                break;

                case USE_UDP:
                    if (l3Opts.contains(USE_IPv4)) {
                        builder.setExact(MatchField.IP_PROTO, IpProtocol.UDP);
                        builder.setExact(MatchField.UDP_SRC, randomTransportPort(rand));
                        builder.setExact(MatchField.UDP_DST, randomTransportPort(rand));
                    }
                break;

                default:
                    throw new AssertionError("unknown enum type");
            }
        }
    }

    private MacAddress newMACAddress()
    {
        byte[] macBytes = randomMACAddressBytes(rand);

        if (l2Opts.containsAll(EnumSet.of(UNICAST_MACS, LOCALLY_ADMINISTERED_MACS))) {
            makeUnicastMACAddress(macBytes);
            makeLocalAdminMACAddress(macBytes);
        }
        else if (l2Opts.contains(UNICAST_MACS)) {
            makeUnicastMACAddress(macBytes);
        }
        else if (l2Opts.contains(LOCALLY_ADMINISTERED_MACS)) {
            makeLocalAdminMACAddress(macBytes);
        }

        return MacAddress.of(macBytes);
    }

    private IPv4Address newIPv4Address()
    {
        if (l3Opts.contains(PRIVATE_IPv4_ADDRESSES)) {
            return randomPrivateIPv4Address(rand);
        }
        else {
            return randomIPv4Address(rand);
        }
    }

    private static IPv4Address randomIPv4Address( Random rand )
    {
        return IPv4Address.of(rand.nextInt());
    }

    private static IPv4Address randomPrivateIPv4Address( Random rand )
    {
        return PRIVATE_IPv4_ADDRS.randomAddress(rand);
    }

    private static TransportPort randomTransportPort( Random rand )
    {
        return TransportPort.of(randomInt(rand, MIN_TRANSPORT_PORT.getPort(), MAX_TRANSPORT_PORT.getPort()));
    }

    private static ICMPv4Type randomICMPv4Type( Random rand )
    {
        return ICMPv4Type.of((short)randomInt(rand, MIN_ICMPv4_TYPE.getType(), MAX_ICMPv4_TYPE.getType()));
    }

    private static ICMPv4Code randomICMPv4Code( Random rand )
    {
        return ICMPv4Code.of((short)randomInt(rand, MIN_ICMPv4_CODE.getCode(), MAX_ICMPv4_CODE.getCode()));
    }

    // from inclusive to exclusive
    private static int randomInt( Random rand, int from, int to )
    {
        return from + rand.nextInt(to - from);
    }

    private static byte[] randomMACAddressBytes( Random rand )
    {
        byte[] bytes = new byte[Fields.SIZE_OF_MAC_ADDRESS];
        rand.nextBytes(bytes);
        return bytes;
    }

    // MAC bytes must in big-endian form!
    private static byte[] makeUnicastMACAddress( byte[] macBytes )
    {
        macBytes[0] = (byte)(macBytes[0] & UNICAST_MAC_ADDR_AND_MASK);
        return macBytes;
    }

    // MAC bytes must in big-endian form!
    private static byte[] makeLocalAdminMACAddress( byte[] macBytes )
    {
        macBytes[0] = (byte)(macBytes[0] | LOCALLY_ADMIN_MAC_ADDR_OR_MASK);
        return macBytes;
    }

    private static final class IPv4AddressMultiRange
    {
        private final List<IPv4AddressRange> ranges;
        private final int                    totalRangeSize;

        IPv4AddressMultiRange( IPv4AddressRange... ranges )
        {
            this.ranges = asList(ranges);
            this.totalRangeSize = getValidTotalRangeSize(this.ranges);
        }

        IPv4Address randomAddress( Random rand )
        {
            final int randInt = rand.nextInt(totalRangeSize);

            int sizeAcc = 0;
            IPv4AddressRange targetRange = null;
            // we always have at least one range
            for (IPv4AddressRange range : ranges) {
                targetRange = range;

                sizeAcc += range.size();
                if (sizeAcc > randInt) {
                    break;
                }
            }

            final int targetAddrIndex = sizeAcc - randInt;
            return IPv4Address.of(Objects.requireNonNull(targetRange).start.getInt() + targetAddrIndex);
        }

        private static List<IPv4AddressRange> asList( IPv4AddressRange[] ranges )
        {
            List<IPv4AddressRange> list = Arrays.asList(ranges);
            if (list.isEmpty()) {
                throw new IllegalArgumentException("no ranges were provided");
            }

            return list;
        }

        private static int getValidTotalRangeSize( List<IPv4AddressRange> ranges )
        {
            int totalSize = 0;
            for (IPv4AddressRange range : ranges) {
                totalSize += range.size();
                if (totalSize < 0) { // overflows integer
                    throw new IllegalArgumentException("total range size is too large");
                }
            }

            return totalSize;
        }
    }

    private static final class IPv4AddressRange
    {
        final IPv4Address start;
        final IPv4Address end;

        IPv4AddressRange( IPv4Address start, IPv4Address end )
        {
            if (aGTb(start, end)) {
                throw new IllegalArgumentException("start address is larger than end address");
            }
            if (1 + end.getInt() - start.getInt() < 0) { // overflows integer
                throw new IllegalArgumentException("range is too large");
            }

            this.start = start;
            this.end = end;
        }

        int size()
        {
            return 1 + end.getInt() - start.getInt();
        }
    }
}
