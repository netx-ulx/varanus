package net.varanus.util.openflow;


import java.util.Objects;

import javax.annotation.Nonnegative;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.ArpOpcode;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.ICMPv4Code;
import org.projectfloodlight.openflow.types.ICMPv4Type;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IPv6Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.TransportPort;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.functional.Possible;
import net.varanus.util.lang.SizeOf;


/**
 * 
 */
public final class PacketBits
{
    public static Possible<BitField> bitFieldFor( MatchField<?> field, Match match )
    {
        Objects.requireNonNull(match);
        switch (field.id) {
            case ETH_DST:
            case ETH_SRC:  // intended fall-through
            case ETH_TYPE: // ""
                return Eth.field(field);

            case IP_PROTO:
                return Eth.IPv4.field(field, match)
                    .or(() -> Eth.IPv6.field(field, match));

            case IPV4_SRC:
            case IPV4_DST: // intended fall-through
                return Eth.IPv4.field(field, match);

            case ICMPV4_TYPE:
            case ICMPV4_CODE: // intended fall-through
                return Eth.IPv4.ICMPv4.field(field, match);

            case TCP_SRC:
            case TCP_DST: // intended fall-through
                return Eth.IPv4.TCP.field(field, match)
                    .or(() -> Eth.IPv6.TCP.field(field, match));

            case UDP_SRC:
            case UDP_DST: // intended fall-through
                return Eth.IPv4.UDP.field(field, match)
                    .or(() -> Eth.IPv6.UDP.field(field, match));

            case ARP_OP:
            case ARP_SHA: // intended fall-through
            case ARP_SPA: // ""
            case ARP_THA: // ""
            case ARP_TPA: // ""
                return Eth.ARP.field(field, match);

            case IPV6_SRC:
            case IPV6_DST: // intended fall-through
                return Eth.IPv6.field(field, match);

            default:
                throw new UnsupportedOperationException(
                    String.format("no bit field is available for match field %s", field.id));
        }
    }

    // If we were to add VLAN headers, all of the Eth subtrees would have to be
    // copied and shifted by 2 bytes...
    // Also, we do not consider variable-length IPv4 and IPv6 headers when
    // defining the layer-4 protocols.

    /**
     * Ethernet at layer 2.
     */
    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class Eth
    {
        public static final BitHeader HEADER = new BitHeader(0, bytes(14));

        public static final BitField DEST = Eth.HEADER.fieldAt(0, MACADDR_BITLEN);
        public static final BitField SRC  = Eth.HEADER.fieldAt(bytes(6), MACADDR_BITLEN);
        public static final BitField TYPE = Eth.HEADER.fieldAt(bytes(12), ETHTYPE_BITLEN);

        public static Possible<BitField> field( MatchField<?> field )
        {
            switch (field.id) {
                case ETH_DST:
                    return Possible.of(Eth.DEST);
                case ETH_SRC:
                    return Possible.of(Eth.SRC);
                case ETH_TYPE:
                    return Possible.of(Eth.TYPE);
                default:
                    return Possible.absent();
            }
        }

        /**
         * IP version 4 (over Ethernet) at layer 3.
         */
        @FieldsAreNonnullByDefault
        @ParametersAreNonnullByDefault
        @ReturnValuesAreNonnullByDefault
        public static final class IPv4
        {
            public static final BitHeader HEADER = Eth.HEADER.nextHeader(bytes(20));

            // TODO add DSCP and ECN fields
            public static final BitField PROTO = IPv4.HEADER.fieldAt(bytes(9), IPPROTO_BITLEN);
            public static final BitField SRC   = IPv4.HEADER.fieldAt(bytes(12), IPV4ADDR_BITLEN);
            public static final BitField DEST  = IPv4.HEADER.fieldAt(bytes(16), IPV4ADDR_BITLEN);

            public static boolean hasPrerequisites( Match match )
            {
                return EthType.IPv4.equals(match.get(MatchField.ETH_TYPE));
            }

            public static Possible<BitField> field( MatchField<?> field, Match match )
            {
                if (hasPrerequisites(match)) {
                    switch (field.id) {
                        case IP_PROTO:
                            return Possible.of(IPv4.PROTO);
                        case IPV4_SRC:
                            return Possible.of(IPv4.SRC);
                        case IPV4_DST:
                            return Possible.of(IPv4.DEST);
                        default:
                            return Possible.absent();
                    }
                }
                else {
                    return Possible.absent();
                }
            }

            /**
             * ICMP version 4 (over IPv4) at layer 4.
             */
            @FieldsAreNonnullByDefault
            @ParametersAreNonnullByDefault
            @ReturnValuesAreNonnullByDefault
            public static final class ICMPv4
            {
                public static final BitHeader HEADER = IPv4.HEADER.nextHeader(bytes(8));

                public static final BitField TYPE = ICMPv4.HEADER.fieldAt(0, ICMPV4TYPE_BITLEN);
                public static final BitField CODE = ICMPv4.HEADER.fieldAt(bytes(1), ICMPV4CODE_BITLEN);

                public static boolean hasPrerequisites( Match match )
                {
                    return IPv4.hasPrerequisites(match)
                           && IpProtocol.ICMP.equals(match.get(MatchField.IP_PROTO));
                }

                public static Possible<BitField> field( MatchField<?> field, Match match )
                {
                    if (hasPrerequisites(match)) {
                        switch (field.id) {
                            case ICMPV4_TYPE:
                                return Possible.of(ICMPv4.TYPE);
                            case ICMPV4_CODE:
                                return Possible.of(ICMPv4.CODE);
                            default:
                                return Possible.absent();
                        }
                    }
                    else {
                        return Possible.absent();
                    }
                }

                private ICMPv4()
                {
                    // not used
                }
            }

            /**
             * TCP (over IPv4) at layer 4.
             */
            @FieldsAreNonnullByDefault
            @ParametersAreNonnullByDefault
            @ReturnValuesAreNonnullByDefault
            public static final class TCP
            {
                public static final BitHeader HEADER = IPv4.HEADER.nextHeader(bytes(20));

                public static final BitField SRC  = IPv4.TCP.HEADER.fieldAt(0, L4PORT_BITLEN);
                public static final BitField DEST = IPv4.TCP.HEADER.fieldAt(bytes(2), L4PORT_BITLEN);

                public static boolean hasPrerequisites( Match match )
                {
                    return IPv4.hasPrerequisites(match)
                           && IpProtocol.TCP.equals(match.get(MatchField.IP_PROTO));
                }

                public static Possible<BitField> field( MatchField<?> field, Match match )
                {
                    if (hasPrerequisites(match)) {
                        switch (field.id) {
                            case TCP_SRC:
                                return Possible.of(IPv4.TCP.SRC);
                            case TCP_DST:
                                return Possible.of(IPv4.TCP.DEST);
                            default:
                                return Possible.absent();
                        }
                    }
                    else {
                        return Possible.absent();
                    }
                }

                private TCP()
                {
                    // not used
                }
            }

            /**
             * UDP (over IPv4) at layer 4.
             */
            @FieldsAreNonnullByDefault
            @ParametersAreNonnullByDefault
            @ReturnValuesAreNonnullByDefault
            public static final class UDP
            {
                public static final BitHeader HEADER = IPv4.HEADER.nextHeader(bytes(8));

                public static final BitField SRC  = IPv4.UDP.HEADER.fieldAt(0, L4PORT_BITLEN);
                public static final BitField DEST = IPv4.UDP.HEADER.fieldAt(bytes(2), L4PORT_BITLEN);

                public static boolean hasPrerequisites( Match match )
                {
                    return IPv4.hasPrerequisites(match)
                           && IpProtocol.UDP.equals(match.get(MatchField.IP_PROTO));
                }

                public static Possible<BitField> field( MatchField<?> field, Match match )
                {
                    if (hasPrerequisites(match)) {
                        switch (field.id) {
                            case UDP_SRC:
                                return Possible.of(IPv4.UDP.SRC);
                            case UDP_DST:
                                return Possible.of(IPv4.UDP.DEST);
                            default:
                                return Possible.absent();
                        }
                    }
                    else {
                        return Possible.absent();
                    }
                }

                private UDP()
                {
                    // not used
                }
            }

            private IPv4()
            {
                // not used
            }
        }

        /**
         * ARP (over Ethernet) at layer 3.
         */
        @FieldsAreNonnullByDefault
        @ParametersAreNonnullByDefault
        @ReturnValuesAreNonnullByDefault
        public static final class ARP
        {
            public static final BitHeader HEADER = Eth.HEADER.nextHeader(bytes(28));

            public static final BitField OP  = ARP.HEADER.fieldAt(bytes(6), ARPOP_BITLEN);
            public static final BitField SHA = ARP.HEADER.fieldAt(bytes(8), MACADDR_BITLEN);
            public static final BitField SPA = ARP.HEADER.fieldAt(bytes(14), IPV4ADDR_BITLEN);
            public static final BitField THA = ARP.HEADER.fieldAt(bytes(18), MACADDR_BITLEN);
            public static final BitField TPA = ARP.HEADER.fieldAt(bytes(24), IPV4ADDR_BITLEN);

            public static boolean hasPrerequisites( Match match )
            {
                return EthType.ARP.equals(match.get(MatchField.ETH_TYPE));
            }

            public static Possible<BitField> field( MatchField<?> field, Match match )
            {
                if (hasPrerequisites(match)) {
                    switch (field.id) {
                        case ARP_OP:
                            return Possible.of(ARP.OP);
                        case ARP_SHA:
                            return Possible.of(ARP.SHA);
                        case ARP_SPA:
                            return Possible.of(ARP.SPA);
                        case ARP_THA:
                            return Possible.of(ARP.THA);
                        case ARP_TPA:
                            return Possible.of(ARP.TPA);
                        default:
                            return Possible.absent();
                    }
                }
                else {
                    return Possible.absent();
                }
            }

            private ARP()
            {
                // not used
            }
        }

        /**
         * IP version 6 (over Ethernet) at layer 3.
         */
        @FieldsAreNonnullByDefault
        @ParametersAreNonnullByDefault
        @ReturnValuesAreNonnullByDefault
        public static final class IPv6
        {
            public static final BitHeader HEADER = Eth.HEADER.nextHeader(bytes(40));

            // TODO add remaining IPv6 fields
            public static final BitField PROTO = IPv6.HEADER.fieldAt(bytes(6), IPPROTO_BITLEN);
            public static final BitField SRC   = IPv6.HEADER.fieldAt(bytes(8), IPV6ADDR_BITLEN);
            public static final BitField DEST  = IPv6.HEADER.fieldAt(bytes(24), IPV6ADDR_BITLEN);

            public static boolean hasPrerequisites( Match match )
            {
                return EthType.IPv6.equals(match.get(MatchField.ETH_TYPE));
            }

            public static Possible<BitField> field( MatchField<?> field, Match match )
            {
                if (hasPrerequisites(match)) {
                    switch (field.id) {
                        case IP_PROTO:
                            return Possible.of(IPv6.PROTO);
                        case IPV6_SRC:
                            return Possible.of(IPv6.SRC);
                        case IPV6_DST:
                            return Possible.of(IPv6.DEST);
                        default:
                            return Possible.absent();
                    }
                }
                else {
                    return Possible.absent();
                }
            }

            // TODO add ICMPv6

            /**
             * TCP (over IPv6) at layer 4.
             */
            @FieldsAreNonnullByDefault
            @ParametersAreNonnullByDefault
            @ReturnValuesAreNonnullByDefault
            public static final class TCP
            {
                public static final BitHeader HEADER = IPv6.HEADER.nextHeader(bytes(20));

                public static final BitField SRC  = IPv6.TCP.HEADER.fieldAt(0, L4PORT_BITLEN);
                public static final BitField DEST = IPv6.TCP.HEADER.fieldAt(bytes(2), L4PORT_BITLEN);

                public static boolean hasPrerequisites( Match match )
                {
                    return IPv6.hasPrerequisites(match)
                           && IpProtocol.TCP.equals(match.get(MatchField.IP_PROTO));
                }

                public static Possible<BitField> field( MatchField<?> field, Match match )
                {
                    if (hasPrerequisites(match)) {
                        switch (field.id) {
                            case TCP_SRC:
                                return Possible.of(IPv6.TCP.SRC);
                            case TCP_DST:
                                return Possible.of(IPv6.TCP.DEST);
                            default:
                                return Possible.absent();
                        }
                    }
                    else {
                        return Possible.absent();
                    }
                }

                private TCP()
                {
                    // not used
                }
            }

            /**
             * UDP (over IPv6) at layer 4.
             */
            @FieldsAreNonnullByDefault
            @ParametersAreNonnullByDefault
            @ReturnValuesAreNonnullByDefault
            public static final class UDP
            {
                public static final BitHeader HEADER = IPv6.HEADER.nextHeader(bytes(8));

                public static final BitField SRC  = IPv6.UDP.HEADER.fieldAt(0, L4PORT_BITLEN);
                public static final BitField DEST = IPv6.UDP.HEADER.fieldAt(bytes(2), L4PORT_BITLEN);

                public static boolean hasPrerequisites( Match match )
                {
                    return IPv6.hasPrerequisites(match)
                           && IpProtocol.UDP.equals(match.get(MatchField.IP_PROTO));
                }

                public static Possible<BitField> field( MatchField<?> field, Match match )
                {
                    if (hasPrerequisites(match)) {
                        switch (field.id) {
                            case UDP_SRC:
                                return Possible.of(IPv6.UDP.SRC);
                            case UDP_DST:
                                return Possible.of(IPv6.UDP.DEST);
                            default:
                                return Possible.absent();
                        }
                    }
                    else {
                        return Possible.absent();
                    }
                }

                private UDP()
                {
                    // not used
                }
            }

            private IPv6()
            {
                // not used
            }
        }

        // TODO add MPLS

        private Eth()
        {
            // not used
        }
    }

    /**
     * A network packet header with specific bit dimensions.
     */
    @Immutable
    @ReturnValuesAreNonnullByDefault
    public static final class BitHeader
    {
        private final int bitStart;
        private final int bitLength;

        /**
         * Constructs a new {@code BitHeader} with the provided bit dimensions.
         * 
         * @param bitStart
         *            The bit-position where the header begins in a network
         *            packet
         * @param bitLength
         *            The size of the header in number of bits
         * @exception IllegalArgumentException
         *                If {@code bitStart < 0} or {@code bitLength < 0}
         * @exception ArithmeticException
         *                If {@code (bitStart + bitLength)} overflows
         */
        public BitHeader( @Nonnegative int bitStart, @Nonnegative int bitLength )
                                                                                  throws IllegalArgumentException,
                                                                                  ArithmeticException
        {
            nonNegative(bitStart, "header bit-start");
            nonNegative(bitLength, "header bit-length");
            noOverflow(bitStart, bitLength, "header bit-start", "header bit-length");
            this.bitStart = bitStart;
            this.bitLength = bitLength;
        }

        /**
         * Returns the bit-position where this header begins in a network
         * packet.
         * 
         * @return a non-negative {@code int}
         */
        public int bitStart()
        {
            return bitStart;
        }

        /**
         * Returns the bit position immediately after the last bit of this
         * header in a network packet.
         * 
         * @return {@code bitStart() + bitLength()}
         */
        public int bitEnd()
        {
            // this never overflows because it is checked at construction
            return bitStart + bitLength;
        }

        /**
         * Returns the size of this header in number of bits.
         * 
         * @return a non-negative {@code int}
         */
        public int bitLength()
        {
            return bitLength;
        }

        /**
         * Returns the minimum number of bytes required to hold this header.
         * 
         * @return a non-negative {@code int}
         */
        public int bytes()
        {
            return SizeOf.bitsInBytes(bitLength);
        }

        /**
         * Returns a new {@code BitHeader} that begins immediately after this
         * header and has the specified bit length.
         * 
         * @param bitLength
         *            The size of the new header in number of bits
         * @return a new {@code BitHeader} instance
         * @exception IllegalArgumentException
         *                If {@code bitLength < 0}
         * @exception ArithmeticException
         *                If {@code (this.bitEnd() + bitLength)} overflows
         */
        public BitHeader nextHeader( @Nonnegative int bitLength ) throws IllegalArgumentException, ArithmeticException
        {
            return new BitHeader(this.bitEnd(), bitLength);
        }

        /**
         * Returns a new {@code BitMatchField} for this header, starting at the
         * provided bit offset and with the specified bit length.
         * 
         * @param bitOffset
         *            A bit offset from this header's start where the field
         *            begins
         * @param bitLength
         *            The size of the field in number of bits
         * @return a new {@code BitMatchField} istance
         * @exception IllegalArgumentException
         *                If {@code bitOffset < 0} or {@code bitLength < 0}
         * @exception ArithmeticException
         *                If
         *                {@code (this.bitStart() + bitOffset + bitLength)}
         *                overflows
         * @exception IndexOutOfBoundsException
         *                If the field bit-length exceeds the header limit
         */
        public BitField fieldAt( @Nonnegative int bitOffset, @Nonnegative int bitLength )
            throws IllegalArgumentException, ArithmeticException, IndexOutOfBoundsException
        {
            return new BitField(this, bitOffset, bitLength);
        }

        @Override
        public boolean equals( Object other )
        {
            return (other instanceof BitHeader)
                   && this.equals((BitHeader)other);
        }

        public boolean equals( BitHeader other )
        {
            return (other != null)
                   && this.bitStart == other.bitStart
                   && this.bitLength == other.bitLength;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(bitStart, bitLength);
        }

        @Override
        public String toString()
        {
            return "BitHeader[" + bitStart() + ", " + bitEnd() + ")";
        }
    }

    /**
     * A network packet header field with specific bit dimensions.
     */
    @Immutable
    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class BitField
    {
        private final BitHeader header;
        private final int       headerBitOffset;
        private final int       bitLength;

        /**
         * Constructs a new {@code BitMatchField} with the provided bit
         * dimensions.
         * 
         * @param header
         *            A network packet header where the field is located
         * @param headerBitOffset
         *            A bit offset from the header start where the field
         *            begins
         * @param bitLength
         *            The size of the field in number of bits
         * @exception IllegalArgumentException
         *                If {@code headerBitOffset < 0} or
         *                {@code bitLength < 0}
         * @exception ArithmeticException
         *                If
         *                {@code (header.bitStart() + headerBitOffset + bitLength)}
         *                overflows
         * @exception IndexOutOfBoundsException
         *                If the field bit-length exceeds the header limit
         */
        public BitField( BitHeader header, @Nonnegative int headerBitOffset, @Nonnegative int bitLength )
                                                                                                          throws IllegalArgumentException,
                                                                                                          ArithmeticException,
                                                                                                          IndexOutOfBoundsException
        {
            nonNegative(headerBitOffset, "header bit-offset");
            nonNegative(bitLength, "bit-length");
            noOverflow(headerBitOffset, bitLength, "header bit-offset", "bit-length");
            noOverflow(header.bitStart(), headerBitOffset, bitLength,
                "header bit-start", "header bit-offset", "bit-length");

            this.header = header;
            this.headerBitOffset = headerBitOffset;
            this.bitLength = bitLength;

            if (this.bitEnd() > header.bitEnd())
                throw new IndexOutOfBoundsException("field bit-length exceeds header limit");
        }

        /**
         * Returns the network packet header where this field is located.
         * 
         * @return a {@code BitHeader} instance
         */
        public BitHeader header()
        {
            return header;
        }

        /**
         * Returns the bit offset from the header start where this field begins.
         * 
         * @return a non-negative {@code int}
         */
        public int headerBitOffset()
        {
            return headerBitOffset;
        }

        /**
         * Returns the bit-position where this field begins in a network packet.
         * 
         * @return {@code header().bitStart() + headerBitOffset()}
         */
        public int bitStart()
        {
            // this never overflows because it is checked at construction
            return header.bitStart() + headerBitOffset;
        }

        /**
         * Returns the bit position immediately after the last bit of this field
         * in a network packet.
         * 
         * @return {@code bitStart() + bitLength()}
         */
        public int bitEnd()
        {
            // this never overflows because it is checked at construction
            return bitStart() + bitLength;
        }

        /**
         * Returns the size of this field in number of bits.
         * 
         * @return a non-negative {@code int}
         */
        public int bitLength()
        {
            return bitLength;
        }

        /**
         * Returns the minimum number of bytes required to hold this field.
         * 
         * @return a non-negative {@code int}
         */
        public int bytes()
        {
            return SizeOf.bitsInBytes(bitLength);
        }

        @Override
        public boolean equals( Object other )
        {
            return (other instanceof BitField)
                   && this.equals((BitField)other);
        }

        public boolean equals( BitField other )
        {
            return (other != null)
                   && this.header.equals(other.header)
                   && this.headerBitOffset == other.headerBitOffset
                   && this.bitLength == other.bitLength;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(header, headerBitOffset, bitLength);
        }

        @Override
        public String toString()
        {
            return "BitMatchField[" + bitStart() + ", " + bitEnd() + ")";
        }
    }

    private static final int MACADDR_BITLEN    = OFValueTypeUtils.bitSize(MacAddress.class);
    private static final int ETHTYPE_BITLEN    = OFValueTypeUtils.bitSize(EthType.class);
    private static final int IPPROTO_BITLEN    = OFValueTypeUtils.bitSize(IpProtocol.class);
    private static final int IPV4ADDR_BITLEN   = OFValueTypeUtils.bitSize(IPv4Address.class);
    private static final int ICMPV4TYPE_BITLEN = OFValueTypeUtils.bitSize(ICMPv4Type.class);
    private static final int ICMPV4CODE_BITLEN = OFValueTypeUtils.bitSize(ICMPv4Code.class);
    private static final int L4PORT_BITLEN     = OFValueTypeUtils.bitSize(TransportPort.class);
    private static final int ARPOP_BITLEN      = OFValueTypeUtils.bitSize(ArpOpcode.class);
    private static final int IPV6ADDR_BITLEN   = OFValueTypeUtils.bitSize(IPv6Address.class);

    private static int bytes( int bytes )
    {
        return Math.multiplyExact(bytes, Byte.SIZE);
    }

    private static void nonNegative( int i, String name ) throws IllegalArgumentException
    {
        if (i < 0) {
            throw new IllegalArgumentException(
                String.format("%s cannot be negative (value was %d)", name, i));
        }
    }

    // requires non-negative a and b
    private static void noOverflow( int a, int b, String aName, String bName ) throws ArithmeticException
    {
        if (a > Integer.MAX_VALUE - b) {
            throw new ArithmeticException(
                String.format("(%s) + (%s) overflows", aName, bName));
        }
    }

    // requires non-negative a, b and c
    private static void noOverflow( int a, int b, int c, String aName, String bName, String cName )
        throws ArithmeticException
    {
        if (a > Integer.MAX_VALUE - b || (a + b) > Integer.MAX_VALUE - c) {
            throw new ArithmeticException(
                String.format("(%s) + (%s) + (%s) overflows", aName, bName, cName));
        }
    }

    private PacketBits()
    {
        // not used
    }
}
