package net.varanus.xmlproxy.internal;


import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.io.ByteBuffers;
import net.varanus.util.io.ByteBuffers.BufferType;
import net.varanus.util.lang.SizeOf;
import net.varanus.util.time.TimeDouble;
import net.varanus.util.time.TimeDoubleUnit;
import net.varanus.util.unitvalue.si.InfoDouble;
import net.varanus.util.unitvalue.si.InfoDoubleUnit;
import net.varanus.util.unitvalue.si.InfoLong;


/**
 * 
 */
final class MininetShCmds
{
    static final Ping PING = new Ping();

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    static final class Ping
    {
        String key( MininetHost srcHost, MininetHost destHost )
        {
            return String.format("ping-%s-%s", srcHost.getAddress(), destHost.getAddress());
        }

        String startCommand( MininetHost srcHost, MininetHost destHost, InetSocketAddress outAddr )
        {
            // "start <key> <src_node> <host>:<port>
            // fping -l -Q 1 <dst_node>"
            return String.format("start %s %s %s:%d %s",
                key(srcHost, destHost), srcHost.getName(), outAddr.getHostString(), outAddr.getPort(),
                pingCmd(destHost));
        }

        String stopCommand( MininetHost srcHost, MininetHost destHost )
        {
            // "stop_custom <key> pkill -x -f 'fping -l -Q 1 <dst_node>'"
            return String.format("stop_custom %s %s",
                key(srcHost, destHost), pingPkillCmd(destHost));
        }

        private static String pingCmd( MininetHost destHost )
        {
            return String.format("fping -l -Q 1 %s", destHost.getAddress());
        }

        private static String pingPkillCmd( MininetHost destHost )
        {
            return String.format("pkill -x -f 'fping -l -Q 1 %s'", destHost.getAddress());
        }

        Optional<TimeDouble> parseResult( String res ) throws IllegalArgumentException
        {
            res = res.trim();
            if (MISS_LINE_FMT.matcher(res).matches()) {
                // <s> : xmt/rcv/%loss = <i>/<i>/<i>%
                return Optional.of(TimeDouble.absent());
            }
            else if (RTT_LINE_FMT.matcher(res).matches()) {
                // <s> : xmt/rcv/%loss = <i>/<i>/<i>%, min/avg/max = <f>/<f>/<f>
                Matcher m = AVG_RTT_GROUP_FMT.matcher(res);
                m.find();
                String avgRtt = m.group(1);
                return Optional.of(TimeDouble.parse(avgRtt, TimeDoubleUnit.MILLISECONDS));
            }
            else {
                // ignore
                return Optional.empty();
            }
        }

        private static final Pattern AVG_RTT_GROUP_FMT = Pattern.compile(".*/([\\d\\.]+)/");

        private static final Pattern MISS_LINE_FMT;
        private static final Pattern RTT_LINE_FMT;
        static { //@formatter:off
            MISS_LINE_FMT = Pattern.compile("^"
                                          + "[^\\s]+"         // (1+ any not whitespace)
                                          + "\\s:\\s"         // whitespace + ':' + whitespace
                                          + "xmt/rcv/%loss"   // "xmt/rcv/%loss"
                                          + "\\s=\\s"         // whitespace + '=' + whitespace
                                          + "\\d+/\\d+/\\d+%" // integer + '/' + integer + '/' + integer + '%'
                                          + "$");
            
            RTT_LINE_FMT = Pattern.compile("^"
                                         + "[^\\s]+"           // (1+ any not whitespace)
                                         + "\\s:\\s"           // whitespace + ':' + whitespace
                                         + "xmt/rcv/%loss"     // "xmt/rcv/%loss"
                                         + "\\s=\\s"           // whitespace + '=' + whitespace
                                         + "\\d+/\\d+/\\d+%"   // integer + '/' + integer + '/' + integer + '%'
                                         + ",\\s"              // ',' + whitespace
                                         + "min/avg/max"       // "min/avg/max"
                                         + "\\s=\\s"           // whitespace + '=' + whitespace
                                         + "[^/]+/[^/]+/[^/]+" // (1+ any not '/') + '/' + (1+ "") + '/' + (1+ "")
                                         + "$");
        }//@formatter:on

        private Ping()
        {
            // private constructor
        }
    }

    static final Iperf IPERF = new Iperf();

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    static final class Iperf
    {
        String clientKey( MininetHost srcHost, MininetHost destHost )
        {
            return String.format("iperfcli-%s-%s", srcHost.getAddress(), destHost.getAddress());
        }

        String serverKey( MininetHost destHost )
        {
            return String.format("iperfsrv-%s", destHost.getAddress());
        }

        String clientStartNoOutputCommand( MininetHost srcHost, MininetHost destHost, InfoDouble bandwidth )
        {
            // "start_no_output <key> <src_node>
            // iperf -c <dst_node> -u -t 3600 -b <bits/s>"
            return String.format("start_no_output %s %s %s",
                clientKey(srcHost, destHost), srcHost.getName(),
                clientIperfCmd(destHost, bandwidth));
        }

        String serverStartCommand( MininetHost destHost, InetSocketAddress outAddr )
        {
            // "start <key> <dst_node> <host>:<port>
            // iperf -s -u -i 1 -f k"
            return String.format("start %s %s %s:%d %s",
                serverKey(destHost), destHost.getName(), outAddr.getHostString(), outAddr.getPort(),
                serverIperfCmd());
        }

        String clientStopCommand( MininetHost srcHost, MininetHost destHost )
        {
            // "stop_custom <key> pkill -f 'iperf -c <dst_node>*'"
            return String.format("stop_custom %s %s",
                clientKey(srcHost, destHost), clientIperfPkillCmd(destHost));
        }

        String serverStopCommand( MininetHost destHost )
        {
            // "stop_custom <key> pkill -x -f 'iperf -s -u -i 1 -f k'"
            return String.format("stop_custom %s %s",
                serverKey(destHost), serverIperfPkillCmd());
        }

        private static String clientIperfCmd( MininetHost destHost, InfoDouble bandwidth )
        {
            return String.format("iperf -c %s -u -t 3600 -b %d", destHost.getAddress(), bandwidth.asLong().inBits());
        }

        private static String clientIperfPkillCmd( MininetHost destHost )
        {
            return String.format("pkill -f 'iperf -c %s*'", destHost.getAddress());
        }

        private static String serverIperfCmd()
        {
            return "iperf -s -u -i 1 -f k";
        }

        private static String serverIperfPkillCmd()
        {
            return "pkill -x -f 'iperf -s -u -i 1 -f k'";
        }

        Optional<InfoLong> parseServerResult( String res ) throws IllegalArgumentException
        {
            res = res.trim();
            if (THRPT_LINE_FMT.matcher(res).matches()) {
                Matcher m = THRPT_GROUP_FMT.matcher(res);
                m.find();
                String thrpt = m.group(1);
                return Optional.of(InfoDouble.parse(thrpt,
                    InfoDoubleUnit.KILOBITS).asLong());
            }
            else {
                return Optional.empty();
            }
        }

        private static final Pattern THRPT_LINE_FMT  = Pattern.compile(".*Kbits/sec.*");
        private static final Pattern THRPT_GROUP_FMT = Pattern.compile("\\s([\\d\\.]+)\\s+Kbits/sec");

        private Iperf()
        {
            // private constructor
        }
    }

    static final TcpReplay TCP_REPLAY = new TcpReplay();

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    static final class TcpReplay
    {
        String key( MininetLinkInfo linkInfo )
        {
            return String.format("tcpreplay-%s-%s", linkInfo.getSrcName(), linkInfo.getSrcIface());
        }

        String startCommandNoOutput( MininetLinkInfo linkInfo, byte[] packet, InfoDouble rate )
        {
            // "start_no_output <key> <src_node>
            // echo -ne <pkt> > tmp ;
            // tcpreplay -i <iface> -l 0 -M <rate> --enable-file-cache -q tmp ;
            // rm -f tmp"
            return String.format("start_no_output %s %s %s",
                key(linkInfo), linkInfo.getSrcName(),
                tcpReplayCmd(linkInfo, packet, rate));
        }

        String stopCommand( MininetLinkInfo linkInfo )
        {
            // "stop_custom <key> pkill tcpreplay"
            return String.format("stop_custom %s %s",
                key(linkInfo), tcpReplayPkillCmd());
        }

        private static String tcpReplayCmd( MininetLinkInfo linkInfo, byte[] packet, InfoDouble rate )
        {
            return String.format("bash -c 'echo -ne \"%s\" > tmppcap ;"
                                 + " tcpreplay --intf1=\"%s\" --loop=0 --mbps=%.2f --enable-file-cache -q tmppcap ;"
                                 + " rm -f tmppcap'",
                toPcapString(packet),
                linkInfo.getSrcIface(), rate.in(InfoDoubleUnit.MEGABITS));
        }

        private static String tcpReplayPkillCmd()
        {
            return "pkill tcpreplay";
        }

        /*
         * This converts a raw Ethernet frame to a pcap capture of one packet,
         * according to the pcap format.
         * The resulting bytes are returned as a bash string literal \x00\x01...
         */
        private static String toPcapString( byte[] packet )
        {
            int pcapLen = PCAP_GLOBAL_HEADER_LEN + PCAP_RECORD_HEADER_LEN + packet.length;
            ByteBuffer buf = ByteBuffers.allocate(pcapLen, BufferType.ARRAY_BACKED);
            putGlobalHeader(buf);
            putRecordHeader(buf, packet.length);
            buf.put(packet);
            buf.flip();
            return toBashBytes(buf);
        }

        private static void putGlobalHeader( ByteBuffer buf )
        {
            buf.putInt(PCAP_GLOBAL_MAGIC_NUMBER);
            buf.putShort(PCAP_GLOBAL_VERSION_MAJOR);
            buf.putShort(PCAP_GLOBAL_VERSION_MINOR);
            buf.putInt(PCAP_GLOBAL_THISZONE);
            buf.putInt(PCAP_GLOBAL_SIGFIGS);
            buf.putInt(PCAP_GLOBAL_SNAPLEN);
            buf.putInt(PCAP_GLOBAL_NETWORK);
        }

        private static void putRecordHeader( ByteBuffer buf, int packetLen )
        {
            buf.putInt(PCAP_RECORD_TS_SEC);
            buf.putInt(PCAP_RECORD_TS_USEC);
            buf.putInt(packetLen); // incl_len
            buf.putInt(packetLen); // orig_len
        }

        private static String toBashBytes( ByteBuffer buf )
        {
            final int offset = buf.position();
            final int size = buf.remaining();
            final char[] bashChars = new char[size * 4];

            for (int j = 0; j < size; j++) {
                int v = buf.get(j + offset) & 0xFF;
                bashChars[j * 4 + 0] = '\\';
                bashChars[j * 4 + 1] = 'x';
                bashChars[j * 4 + 2] = hexArray[v >>> 4];
                bashChars[j * 4 + 3] = hexArray[v & 0x0F];
            }

            return new String(bashChars);
        }

        private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

        private static final int   PCAP_GLOBAL_HEADER_LEN    = SizeOf.INT + (2 * SizeOf.SHORT) + (4 * SizeOf.INT);
        private static final int   PCAP_GLOBAL_MAGIC_NUMBER  = 0xa1b2c3d4;
        private static final short PCAP_GLOBAL_VERSION_MAJOR = 2;
        private static final short PCAP_GLOBAL_VERSION_MINOR = 4;
        private static final int   PCAP_GLOBAL_THISZONE      = 0;
        private static final int   PCAP_GLOBAL_SIGFIGS       = 0;
        private static final int   PCAP_GLOBAL_SNAPLEN       = 65535;
        private static final int   PCAP_GLOBAL_NETWORK       = 1;

        private static final int PCAP_RECORD_HEADER_LEN = 4 * SizeOf.INT;
        private static final int PCAP_RECORD_TS_SEC     = 0;
        private static final int PCAP_RECORD_TS_USEC    = 0;

        private TcpReplay()
        {
            // private constructor
        }
    }

    // static final IperfTcp IPERF_TCP = new IperfTcp();
    //
    // @FieldsAreNonnullByDefault
    // @ParametersAreNonnullByDefault
    // @ReturnValuesAreNonnullByDefault
    // static final class IperfTcp extends Iperf
    // {
    // String clientStartNoOutputCommand( MininetHost srcHost, MininetHost
    // destHost, InfoDouble bandwidth )
    // {
    // // "start_no_output <key> <src_node>
    // // iperf3 -c <dst_node> -i 1 -t 3600 -b <bits/s>"
    // return String.format("start_no_output %s %s iperf3 -c %s -t 3600 -b %d",
    // clientKey(srcHost, destHost), srcHost.getName(),
    // destHost.getAddress(), bandwidth.asLong().inBits());
    // }
    //
    // String serverStartCommand( MininetHost destHost, InetSocketAddress
    // outAddr )
    // {
    // // "start <key> <dst_node> <host>:<port>
    // // iperf3 -s"
    // return String.format("start %s %s %s:%d iperf3 -s -i 1 -f k",
    // serverKey(destHost), destHost.getName(), outAddr.getHostString(),
    // outAddr.getPort());
    // }
    //
    // Optional<InfoLong> parseServerResult( String res ) throws
    // IllegalArgumentException
    // {
    // res = res.trim();
    // if (THRPT_LINE_FMT.matcher(res).matches()) {
    // Matcher m = THRPT_GROUP_FMT.matcher(res);
    // m.find();
    // String thrpt = m.group(1);
    // return Optional.of(InfoDouble.parse(thrpt,
    // InfoDoubleUnit.KILOBITS).asLong());
    // }
    // else {
    // return Optional.empty();
    // }
    // }
    //
    // private static final Pattern THRPT_LINE_FMT =
    // Pattern.compile(".*Kbits/sec$");
    // private static final Pattern THRPT_GROUP_FMT =
    // Pattern.compile("\\s([\\d\\.]+)\\s+Kbits/sec$");
    //
    // /*
    // * String clientStartCommand( MininetHost srcHost, MininetHost destHost,
    // * InfoDouble bandwidth, InetSocketAddress outAddr )
    // * {
    // * // "start <key> <src_node> <host>:<port>
    // * // iperf3 -c <dst_node> -i 1 -t 3600 -b <bits/s>"
    // * return
    // * String.
    // * format("start %s %s %s:%d iperf3 -c %s -t 3600 -b %d -i 1 -f k",
    // * clientKey(srcHost, destHost), srcHost.getName(),
    // * outAddr.getHostString(), outAddr.getPort(),
    // * destHost.getAddress(), bandwidth.asLong().inBits());
    // * }
    // */
    //
    // /*
    // * String serverStartNoOutputCommand( MininetHost destHost )
    // * {
    // * // "start_no_output <key> <dst_node>
    // * // iperf3 -s"
    // * return String.format("start_no_output %s %s iperf3 -s",
    // * serverKey(destHost), destHost.getName());
    // * }
    // */
    //
    // private IperfTcp()
    // {
    // // private constructor
    // }
    // }
}
