package net.varanus.xmlproxy.internal;


import static net.varanus.util.time.TimeDoubleUnit.MICROSECONDS;
import static net.varanus.util.time.TimeDoubleUnit.MILLISECONDS;
import static net.varanus.util.time.TimeDoubleUnit.SECONDS;
import static net.varanus.util.unitvalue.si.InfoLongUnit.BITS;
import static net.varanus.util.unitvalue.si.InfoLongUnit.GIGABITS;
import static net.varanus.util.unitvalue.si.InfoLongUnit.KILOBITS;
import static net.varanus.util.unitvalue.si.InfoLongUnit.MEGABITS;
import static net.varanus.util.unitvalue.si.InfoLongUnit.TERABITS;

import java.util.List;
import java.util.Scanner;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import com.google.common.base.Preconditions;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.text.StringUtils;
import net.varanus.util.time.TimeDouble;
import net.varanus.util.time.TimeLong;
import net.varanus.util.unitvalue.si.InfoDouble;
import net.varanus.util.unitvalue.si.InfoDoubleUnit;
import net.varanus.util.unitvalue.si.InfoLong;
import net.varanus.util.unitvalue.si.InfoLongUnit;
import net.varanus.xmlproxy.util.Percentage;


/**
 * 
 */
@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
final class MininetLinkInfo
{
    static MininetLinkInfo parse( String s ) throws IllegalArgumentException
    {
        List<String> tuple = StringUtils.parseList(s, ";", "{", "}");
        if (tuple.size() != 6) {
            throw new IllegalArgumentException(String.format("malformed link info (expected a 6-tuple); received: %s",
                tuple));
        }
        else {
            String srcName = tuple.get(0).trim();
            String srcIface = tuple.get(1).trim();
            String destName = tuple.get(2).trim();
            String destIface = tuple.get(3).trim();
            BandwidthQoS bandQoS = BandwidthQoS.parse(tuple.get(4).trim());
            NetemQoS netemQoS = NetemQoS.parse(tuple.get(5).trim());

            return new MininetLinkInfo(srcName, srcIface, destName, destIface, bandQoS, netemQoS);
        }
    }

    private final String       srcName;
    private final String       srcIface;
    private final String       destName;
    private final String       destIface;
    private final BandwidthQoS bandQoS;
    private final NetemQoS     netemQoS;

    MininetLinkInfo( String srcName,
                     String srcIface,
                     String destName,
                     String destIface,
                     BandwidthQoS bandQoS,
                     NetemQoS netemQoS )
    {
        this.srcName = srcName;
        this.srcIface = srcIface;
        this.destName = destName;
        this.destIface = destIface;
        this.bandQoS = bandQoS;
        this.netemQoS = netemQoS;
    }

    String getSrcName()
    {
        return srcName;
    }

    String getSrcIface()
    {
        return srcIface;
    }

    String getDestName()
    {
        return destName;
    }

    String getDestIface()
    {
        return destIface;
    }

    BandwidthQoS getBandwidthQoS()
    {
        return bandQoS;
    }

    NetemQoS getNetemQoS()
    {
        return netemQoS;
    }

    @Override
    public String toString()
    {
        return String.format("%s-%s -> %s-%s (bandwidth%s, netem%s)",
            srcName, srcIface, destName, destIface, bandQoS, netemQoS);
    }

    @Immutable
    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    static final class BandwidthQoS
    {
        private static final BandwidthQoS ABSENT = new BandwidthQoS(InfoDouble.absent());

        static BandwidthQoS absent()
        {
            return ABSENT;
        }

        static BandwidthQoS of( InfoDouble bandwidth ) throws IllegalArgumentException
        {
            boolean isZero = bandwidth.test(InfoDoubleUnit.BITS, ( bits ) -> bits == 0);
            Preconditions.checkArgument(!isZero, "cannot set up a bandwidth of 0 b/s");
            return new BandwidthQoS(bandwidth);
        }

        static BandwidthQoS parse( String qos ) throws IllegalArgumentException
        {
            final InfoDouble bw;
            if (qos.isEmpty())
                bw = InfoDouble.absent();
            else if (qos.endsWith("Tbit"))
                bw = InfoLong.parse(qos.substring(0, qos.length() - 4), TERABITS).asDouble();
            else if (qos.endsWith("Gbit"))
                bw = InfoLong.parse(qos.substring(0, qos.length() - 4), GIGABITS).asDouble();
            else if (qos.endsWith("Mbit"))
                bw = InfoLong.parse(qos.substring(0, qos.length() - 4), MEGABITS).asDouble();
            else if (qos.endsWith("Kbit"))
                bw = InfoLong.parse(qos.substring(0, qos.length() - 4), KILOBITS).asDouble();
            else if (qos.endsWith("bit"))
                bw = InfoLong.parse(qos.substring(0, qos.length() - 3), BITS).asDouble();
            else
                throw new IllegalArgumentException("malformed link QoS (invalid bandwidth suffix)");

            return of(bw);
        }

        private final InfoDouble bandwidth;

        private BandwidthQoS( InfoDouble bandwidth )
        {
            this.bandwidth = bandwidth;
        }

        public InfoDouble getBandwidth()
        {
            return bandwidth;
        }

        public String getCommandArg()
        {
            return bandwidth.asLong().map(InfoLongUnit.BITS, String::valueOf).orElse("");
        }

        @Override
        public String toString()
        {
            return "[" + getCommandArg() + "]";
        }
    }

    @Immutable
    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    static final class NetemQoS
    {
        private static NetemQoS ABSENT = new NetemQoS(TimeLong.absent(), Percentage.absent());

        static NetemQoS absent()
        {
            return ABSENT;
        }

        static NetemQoS parse( String qos ) throws IllegalArgumentException
        {
            TimeLong delay = TimeLong.absent();
            Percentage lossRate = Percentage.absent();
            try (Scanner scan = new Scanner(qos)) {
                while (scan.hasNext()) {
                    String type = scan.next();
                    if (type.equals("delay")) {
                        if (scan.hasNext()) {
                            String val = scan.next();
                            if (val.endsWith("us"))
                                delay = TimeDouble.parse(val.substring(0, val.length() - 2), MICROSECONDS).asLong();
                            else if (val.endsWith("ms"))
                                delay = TimeDouble.parse(val.substring(0, val.length() - 2), MILLISECONDS).asLong();
                            else if (val.endsWith("s"))
                                delay = TimeDouble.parse(val.substring(0, val.length() - 1), SECONDS).asLong();
                            else
                                throw new IllegalArgumentException("malformed link QoS (invalid delay suffix)");
                        }
                        else {
                            throw new IllegalArgumentException("malformed link QoS (missing delay value)");
                        }
                    }
                    else if (type.equals("loss")) {
                        if (scan.hasNext()) {
                            String val = scan.next();
                            if (val.endsWith("%"))
                                lossRate = Percentage.of(Integer.parseInt(val.substring(0, val.length() - 1)));
                            else
                                throw new IllegalArgumentException("malformed link QoS (invalid loss rate suffix)");
                        }
                        else {
                            throw new IllegalArgumentException("malformed link QoS (missing loss rate value)");
                        }
                    }
                    else {
                        throw new IllegalArgumentException("malformed link QoS (invalid argument type)");
                    }
                }
            }
            return new NetemQoS(delay, lossRate);
        }

        private final TimeLong   delay;
        private final Percentage lossRate;
        private final String     cmdArgs;

        NetemQoS( TimeLong delay, Percentage lossRate )
        {
            this.delay = delay;
            this.lossRate = lossRate;
            this.cmdArgs = buildCommandArgs(delay, lossRate);
        }

        TimeLong getDelay()
        {
            return delay;
        }

        Percentage getLossRate()
        {
            return lossRate;
        }

        String getCommandArgs()
        {
            return cmdArgs;
        }

        @Override
        public String toString()
        {
            return "[" + getCommandArgs() + "]";
        }

        private static String buildCommandArgs( TimeLong delay, Percentage lossRate )
        {
            StringJoiner join = new StringJoiner(" ");
            delay.ifPresent(TimeUnit.MILLISECONDS, ( millis ) -> {
                join.add(String.format("delay %dms", millis));
            });
            lossRate.ifPresent(( percent ) -> {
                join.add(String.format("loss random %d%%", percent));
            });
            return join.toString();
        }
    }
}
