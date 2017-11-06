package net.varanus.mirroringprotocol;


import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import org.projectfloodlight.openflow.types.DatapathId;
import org.slf4j.Logger;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import net.varanus.mirroringprotocol.util.CollectorId;
import net.varanus.mirroringprotocol.util.PacketSummary;
import net.varanus.mirroringprotocol.util.TimedPacketSummary;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.collect.builder.ImmutableListBuilder;
import net.varanus.util.io.ExtraChannels;
import net.varanus.util.io.Serializers;
import net.varanus.util.io.exception.IOChannelReadException;
import net.varanus.util.io.exception.IOChannelWriteException;
import net.varanus.util.io.serializer.ChannelReader;
import net.varanus.util.io.serializer.ChannelSerializer;
import net.varanus.util.io.serializer.ChannelWriter;
import net.varanus.util.io.serializer.IOReader;
import net.varanus.util.io.serializer.IOWriter;
import net.varanus.util.io.serializerint.IOIntReader;
import net.varanus.util.io.serializerint.IOIntWriter;
import net.varanus.util.io.serializerlong.IOLongReader;
import net.varanus.util.io.serializerlong.IOLongWriter;
import net.varanus.util.lang.Comparables;
import net.varanus.util.lang.MoreObjects;
import net.varanus.util.lang.Unsigned;
import net.varanus.util.openflow.types.DirectedNodePort;
import net.varanus.util.openflow.types.Flow;
import net.varanus.util.time.TimeUtils;


/**
 * 
 */
@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class SamplingReply
{
    private final DirectedNodePort                  switchPort;
    private final Flow                              flow;
    private final ImmutableList<TimedPacketSummary> timedPktSumms;
    private final long                              unmatchedBytes;
    private final long                              unmatchedPkts;

    public SamplingReply( DirectedNodePort switchPort,
                          Flow flow,
                          ImmutableList<TimedPacketSummary> timedPktSumms,
                          long unmatchedBytes,
                          long unmatchedPkts )
    {
        this.switchPort = Objects.requireNonNull(switchPort);
        this.flow = Objects.requireNonNull(flow);
        this.timedPktSumms = Objects.requireNonNull(timedPktSumms);
        this.unmatchedBytes = unmatchedBytes;
        this.unmatchedPkts = unmatchedPkts;
    }

    public DirectedNodePort getSwitchPort()
    {
        return switchPort;
    }

    public Flow getFlow()
    {
        return flow;
    }

    public ImmutableList<TimedPacketSummary> getTimedPacketSummaries()
    {
        return timedPktSumms;
    }

    public long getUnmatchedBytes()
    {
        return unmatchedBytes;
    }

    public long getUnmatchedPackets()
    {
        return unmatchedPkts;
    }

    @Override
    public String toString()
    {
        return toString(false);
    }

    public String toString( boolean includeSummaries )
    {
        if (includeSummaries) {
            return String.format("( %s, %s, unmatched[bytes=%d, packets=%d], matched%s )",
                switchPort,
                flow,
                unmatchedBytes,
                unmatchedPkts,
                timedPktSumms);
        }
        else {
            return String.format("( %s, %s, unmatched[bytes=%d, packets=%d], matched[%d packets] )",
                switchPort,
                flow,
                unmatchedBytes,
                unmatchedPkts,
                timedPktSumms.size());
        }
    }

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class IO
    {
        public static enum CompressionStrategy
        {
            MINIMIZE_MESSAGE_SIZE,
            MAXIMIZE_TIME_PRECISION;
        }

        private static final long REPLY_PREAMBLE = 0xf7ecbd38ebacb04dL;

        private static IOIntWriter  INT_WRITER  = Serializers.intWriter(ByteOrder.BIG_ENDIAN);
        private static IOIntReader  INT_READER  = Serializers.intReader(ByteOrder.BIG_ENDIAN);
        private static IOLongWriter LONG_WRITER = Serializers.longWriter(ByteOrder.BIG_ENDIAN);
        private static IOLongReader LONG_READER = Serializers.longReader(ByteOrder.BIG_ENDIAN);

        public static IOWriter<SamplingReply> writer( CollectorId collectorId,
                                                      CompressionStrategy compStrat,
                                                      Logger log )
        {
            MoreObjects.requireNonNull(collectorId, "collectorId", compStrat, "compStrat", log, "log");
            return new IOWriter<SamplingReply>() {
                @Override
                public void write( SamplingReply reply, WritableByteChannel ch ) throws IOChannelWriteException
                {

                    if (log.isTraceEnabled()) {
                        log.trace("Writing sampling reply from collector {}: reply preamble 0x{}",
                            collectorId, Long.toHexString(REPLY_PREAMBLE));
                    }
                    LONG_WRITER.writeLong(REPLY_PREAMBLE, ch);

                    final DirectedNodePort switchPort = reply.getSwitchPort();
                    log.trace("Writing sampling reply from collector {}: switch-port {}", collectorId, switchPort);
                    DirectedNodePort.IO.writer().write(switchPort, ch);

                    final Flow flow = reply.getFlow();
                    log.trace("Writing sampling reply from collector {}: flow {}", collectorId, flow);
                    Flow.IO.writer().write(flow, ch);

                    final List<TimedPacketSummary> timedPktSumms = reply.getTimedPacketSummaries();
                    log.trace("Writing sampling reply from collector {}: {} timed packet summaries",
                        collectorId, timedPktSumms.size());
                    INT_WRITER.writeInt(timedPktSumms.size(), ch);

                    Optional<TimedPacketSummary> firstTimed = timedPktSumms.stream().findFirst();
                    if (firstTimed.isPresent()) {
                        log.trace("Writing sampling reply from collector {}: initial timed packet summary {}",
                            collectorId, firstTimed.get());
                        TimedPacketSummary.IO.writer().write(firstTimed.get(), ch);
                    }

                    List<IntervalPacketSummary> intervals = new ArrayList<>();
                    Duration maxInterval = Duration.ZERO;

                    Iterator<TimedPacketSummary> iter = timedPktSumms.iterator();
                    if (iter.hasNext()) {
                        TimedPacketSummary previous = iter.next();
                        while (iter.hasNext()) {
                            final TimedPacketSummary current = iter.next();
                            IntervalPacketSummary inter =
                                IntervalPacketSummary.fromTimed(current, previous.timestamp());

                            intervals.add(inter);
                            maxInterval = Comparables.max(inter.interval(), maxInterval);

                            previous = current;
                        }
                    }

                    if (!intervals.isEmpty()) {
                        IntervalCoding coding = IntervalCoding.fromTimeInterval(maxInterval, compStrat);
                        if (log.isTraceEnabled()) {
                            log.trace(
                                "Writing sampling reply from collector {}: {} time intervals using a coding scheme of {}",
                                new Object[] {collectorId, intervals.size(), coding});
                        }
                        Serializers.<IntervalCoding>enumWriter().write(coding, ch);

                        // if (log.isTraceEnabled()) {
                        // log.trace("Writing sampling reply from collector {}:
                        // time intervals {}",
                        // collectorId, toResumedString(intervals));
                        // }
                        ChannelWriter<IntervalPacketSummary> interWriter = IntervalPacketSummary.writer(coding);
                        for (IntervalPacketSummary inter : intervals) {
                            interWriter.write(inter, ch);
                        }
                    }

                    final long unmatchedBytes = reply.getUnmatchedBytes();
                    log.trace("Writing sampling reply from collector {}: {} unmatched bytes",
                        collectorId, unmatchedBytes);
                    LONG_WRITER.writeLong(unmatchedBytes, ch);

                    final long unmatchedPkts = reply.getUnmatchedPackets();
                    log.trace("Writing sampling reply from collector {}: {} unmatched packets",
                        collectorId, unmatchedPkts);
                    LONG_WRITER.writeLong(unmatchedPkts, ch);
                }
            };
        }

        public static IOReader<SamplingReply> reader( CollectorId collectorId,
                                                      Function<DatapathId, String> idAliaser,
                                                      CompressionStrategy compStrat,
                                                      Logger log )
        {
            MoreObjects.requireNonNull(
                collectorId, "collectorId",
                idAliaser, "idAliaser",
                compStrat, "compStrat",
                log, "log");
            return new IOReader<SamplingReply>() {
                @Override
                public SamplingReply read( ReadableByteChannel ch ) throws IOChannelReadException
                {
                    final long preamble = LONG_READER.readLong(ch);
                    if (preamble != REPLY_PREAMBLE) {
                        throw new IOChannelReadException(
                            String.format("expected reply preamble of %x from collector %s but found %x instead",
                                REPLY_PREAMBLE, collectorId, preamble));
                    }
                    if (log.isTraceEnabled()) {
                        log.trace("Read sampling reply from collector {}: reply preamble 0x{}",
                            collectorId, Long.toHexString(preamble));
                    }

                    final DirectedNodePort switchPort = DirectedNodePort.IO.reader(idAliaser).read(ch);
                    log.trace("Read sampling reply from collector {}: switch-port {}", collectorId, switchPort);

                    final Flow flow = Flow.IO.reader().read(ch);
                    log.trace("Read sampling reply from collector {}: flow {}", collectorId, flow);

                    final int numTimed = INT_READER.readInt(ch);
                    if (numTimed < 0) {
                        throw new IOChannelReadException(
                            String.format(
                                "received unexpected negative number of timed packet summaries from collector %s: %d",
                                collectorId, numTimed));
                    }
                    log.trace("Reading sampling reply from collector {}: {} timed packet summaries",
                        collectorId, numTimed);

                    final ImmutableListBuilder<TimedPacketSummary> timedPktSumms = ImmutableListBuilder.create();
                    if (numTimed > 0) {
                        TimedPacketSummary baseTimed = TimedPacketSummary.IO.reader().read(ch);
                        log.trace("Read sampling reply from collector {}: initial timed packet summary {}",
                            collectorId, baseTimed);
                        timedPktSumms.add(baseTimed);

                        int numIntervals = numTimed - 1;
                        if (numIntervals > 0) {
                            IntervalCoding coding = Serializers.enumReader(IntervalCoding.class).read(ch);
                            if (log.isTraceEnabled()) {
                                log.trace(
                                    "Reading sampling reply from collector {}: {} time intervals using a coding scheme of {}",
                                    new Object[] {collectorId, numIntervals, coding});
                            }

                            ChannelReader<IntervalPacketSummary> interReader = IntervalPacketSummary.reader(coding);
                            List<IntervalPacketSummary> intervals = new ArrayList<>(numIntervals);
                            for (int i = 0; i < numIntervals; i++) {
                                intervals.add(interReader.read(ch));
                            }
                            // if (log.isTraceEnabled()) {
                            // log.trace("Read sampling reply from collector {}:
                            // time intervals {}",
                            // collectorId, toResumedString(intervals));
                            // }

                            TimedPacketSummary previous = baseTimed;
                            for (IntervalPacketSummary inter : intervals) {
                                TimedPacketSummary current = IntervalPacketSummary.toTimed(inter, previous.timestamp());
                                timedPktSumms.add(current);
                                previous = current;
                            }
                        }
                    }

                    final long unmatchedBytes = LONG_READER.readLong(ch);
                    if (unmatchedBytes < 0) {
                        throw new IOChannelReadException(
                            String.format(
                                "received unexpected negative number of unmatched bytes from collector %s: %d",
                                collectorId, unmatchedBytes));
                    }
                    log.trace("Reading sampling reply from collector {}: {} unmatched bytes",
                        collectorId, unmatchedBytes);

                    final long unmatchedPkts = LONG_READER.readLong(ch);
                    if (unmatchedPkts < 0) {
                        throw new IOChannelReadException(
                            String.format(
                                "received unexpected negative number of unmatched packets from collector %s: %d",
                                collectorId, unmatchedPkts));
                    }
                    log.trace("Reading sampling reply from collector {}: {} unmatched packets",
                        collectorId, unmatchedPkts);

                    return new SamplingReply(switchPort, flow, timedPktSumms.build(), unmatchedBytes, unmatchedPkts);
                }
            };
        }

        @FieldsAreNonnullByDefault
        @ParametersAreNonnullByDefault
        @ReturnValuesAreNonnullByDefault
        private static final class IntervalPacketSummary
        {
            static IntervalPacketSummary fromTimed( TimedPacketSummary timed, Instant prevTime )
            {
                Duration interval = Duration.between(prevTime, timed.timestamp());
                if (interval.isNegative()) interval = Duration.ZERO;
                return new IntervalPacketSummary(timed.value(), interval);
            }

            static TimedPacketSummary toTimed( IntervalPacketSummary inter, Instant prevTimestamp )
            {
                Instant timestamp = prevTimestamp.plus(inter.interval());
                return new TimedPacketSummary(inter.value(), timestamp);
            }

            static ChannelWriter<IntervalPacketSummary> writer( IntervalCoding coding )
            {
                return ( wi, ch ) -> {
                    PacketSummary.IO.writer().write(wi.value(), ch);
                    coding.durationSerializer().write(wi.interval(), ch);
                };
            }

            static ChannelReader<IntervalPacketSummary> reader( IntervalCoding coding )
            {
                return ( ch ) -> {
                    PacketSummary summary = PacketSummary.IO.reader().read(ch);
                    Duration interval = coding.durationSerializer().read(ch);
                    return new IntervalPacketSummary(summary, interval);
                };
            }

            private final PacketSummary summary;
            private final Duration      interval;

            private IntervalPacketSummary( PacketSummary summary, Duration interval )
            {
                this.summary = summary;
                this.interval = interval;
            }

            PacketSummary value()
            {
                return summary;
            }

            Duration interval()
            {
                return interval;
            }

            @Override
            public String toString()
            {
                return String.format("%s @{+%s}",
                    value(),
                    TimeUtils.toSmartDurationString(interval()));
            }
        }

        @FieldsAreNonnullByDefault
        @ParametersAreNonnullByDefault
        @ReturnValuesAreNonnullByDefault
        private static enum IntervalCoding
        {
        //@formatter:off
            NANOS_1_BYTE(
                1,
                Unsigned.MAX_BYTE,
                new ChannelSerializer<Duration>() {
                    @Override
                    public void write( Duration dur, WritableByteChannel ch ) throws IOChannelWriteException
                    {
                        long nanos = dur.toNanos();
                        byte compressedNanos = (byte)nanos;
                        ExtraChannels.writeByte(ch, compressedNanos);
                    }
                    @Override
                    public Duration read( ReadableByteChannel ch ) throws IOChannelReadException
                    {
                        byte compressedNanos = ExtraChannels.readByte(ch);
                        long nanos = Byte.toUnsignedLong(compressedNanos);
                        return Duration.ofNanos(nanos);
                    }
                }),

            NANOS_2_BYTES(
                2,
                Unsigned.MAX_SHORT,
                new ChannelSerializer<Duration>() {
                    @Override
                    public void write( Duration dur, WritableByteChannel ch ) throws IOChannelWriteException
                    {
                        long nanos = dur.toNanos();
                        short compressedNanos = (short)nanos;
                        ExtraChannels.writeShort(ch, compressedNanos, ByteOrder.BIG_ENDIAN);
                    }
                    @Override
                    public Duration read( ReadableByteChannel ch ) throws IOChannelReadException
                    {
                        short compressedNanos = ExtraChannels.readShort(ch, ByteOrder.BIG_ENDIAN);
                        long nanos = Short.toUnsignedLong(compressedNanos);
                        return Duration.ofNanos(nanos);
                    }
                }),

            NANOS_3_BYTES(
                3,
                Unsigned.MAX_MEDIUM,
                new ChannelSerializer<Duration>() {
                    @Override
                    public void write( Duration dur, WritableByteChannel ch ) throws IOChannelWriteException
                    {
                        long nanos = dur.toNanos();
                        int compressedNanos = (int)nanos;
                        ExtraChannels.writeMedium(ch, compressedNanos, ByteOrder.BIG_ENDIAN);
                    }
                    @Override
                    public Duration read( ReadableByteChannel ch ) throws IOChannelReadException
                    {
                        int compressedNanos = ExtraChannels.readMedium(ch, ByteOrder.BIG_ENDIAN);
                        long nanos = Integer.toUnsignedLong(compressedNanos);
                        return Duration.ofNanos(nanos);
                    }
                }),

            NANOS_4_BYTES(
                4,
                Unsigned.MAX_INT,
                new ChannelSerializer<Duration>() {
                    @Override
                    public void write( Duration dur, WritableByteChannel ch ) throws IOChannelWriteException
                    {
                        long nanos = dur.toNanos();
                        int compressedNanos = (int)nanos;
                        ExtraChannels.writeInt(ch, compressedNanos, ByteOrder.BIG_ENDIAN);
                    }
                    @Override
                    public Duration read( ReadableByteChannel ch ) throws IOChannelReadException
                    {
                        int compressedNanos = ExtraChannels.readInt(ch, ByteOrder.BIG_ENDIAN);
                        long nanos = Integer.toUnsignedLong(compressedNanos);
                        return Duration.ofNanos(nanos);
                    }
                }),

            NANOS_8_BYTES(
                8,
                Long.MAX_VALUE,
                new ChannelSerializer<Duration>() {
                    @Override
                    public void write( Duration dur, WritableByteChannel ch ) throws IOChannelWriteException
                    {
                        long nanos = dur.toNanos();
                        ExtraChannels.writeLong(ch, nanos, ByteOrder.BIG_ENDIAN);
                    }
                    @Override
                    public Duration read( ReadableByteChannel ch ) throws IOChannelReadException
                    {
                        long nanos = ExtraChannels.readLong(ch, ByteOrder.BIG_ENDIAN);
                        return Duration.ofNanos(nanos);
                    }
                }),

            MICROS_1_BYTE(
                1,
                TimeUnit.MICROSECONDS.toNanos(Unsigned.MAX_BYTE),
                new ChannelSerializer<Duration>() {
                    @Override
                    public void write( Duration dur, WritableByteChannel ch ) throws IOChannelWriteException
                    {
                        long micros = TimeUnit.NANOSECONDS.toMicros(dur.toNanos());
                        byte compressedMicros = (byte)micros;
                        ExtraChannels.writeByte(ch, compressedMicros);
                    }
                    @Override
                    public Duration read( ReadableByteChannel ch ) throws IOChannelReadException
                    {
                        byte compressedMicros = ExtraChannels.readByte(ch);
                        long micros = Byte.toUnsignedLong(compressedMicros);
                        return Duration.of(micros, ChronoUnit.MICROS);
                    }
                }),

            MICROS_2_BYTES(
                2,
                TimeUnit.MICROSECONDS.toNanos(Unsigned.MAX_SHORT),
                new ChannelSerializer<Duration>() {
                    @Override
                    public void write( Duration dur, WritableByteChannel ch ) throws IOChannelWriteException
                    {
                        long micros = TimeUnit.NANOSECONDS.toMicros(dur.toNanos());
                        short compressedMicros = (short)micros;
                        ExtraChannels.writeShort(ch, compressedMicros, ByteOrder.BIG_ENDIAN);
                    }
                    @Override
                    public Duration read( ReadableByteChannel ch ) throws IOChannelReadException
                    {
                        short compressedMicros = ExtraChannels.readShort(ch, ByteOrder.BIG_ENDIAN);
                        long micros = Short.toUnsignedLong(compressedMicros);
                        return Duration.of(micros, ChronoUnit.MICROS);
                    }
                }),

            MICROS_3_BYTES(
                3,
                TimeUnit.MICROSECONDS.toNanos(Unsigned.MAX_MEDIUM),
                new ChannelSerializer<Duration>() {
                    @Override
                    public void write( Duration dur, WritableByteChannel ch ) throws IOChannelWriteException
                    {
                        long micros = TimeUnit.NANOSECONDS.toMicros(dur.toNanos());
                        int compressedMicros = (int)micros;
                        ExtraChannels.writeMedium(ch, compressedMicros, ByteOrder.BIG_ENDIAN);
                    }
                    @Override
                    public Duration read( ReadableByteChannel ch ) throws IOChannelReadException
                    {
                        int compressedMicros = ExtraChannels.readMedium(ch, ByteOrder.BIG_ENDIAN);
                        long micros = Integer.toUnsignedLong(compressedMicros);
                        return Duration.of(micros, ChronoUnit.MICROS);
                    }
                }),

            MICROS_4_BYTES(
                4,
                TimeUnit.MICROSECONDS.toNanos(Unsigned.MAX_INT),
                new ChannelSerializer<Duration>() {
                    @Override
                    public void write( Duration dur, WritableByteChannel ch ) throws IOChannelWriteException
                    {
                        long micros = TimeUnit.NANOSECONDS.toMicros(dur.toNanos());
                        int compressedMicros = (int)micros;
                        ExtraChannels.writeInt(ch, compressedMicros, ByteOrder.BIG_ENDIAN);
                    }
                    @Override
                    public Duration read( ReadableByteChannel ch ) throws IOChannelReadException
                    {
                        int compressedMicros = ExtraChannels.readInt(ch, ByteOrder.BIG_ENDIAN);
                        long micros = Integer.toUnsignedLong(compressedMicros);
                        return Duration.of(micros, ChronoUnit.MICROS);
                    }
                });
            //@formatter:on

            private static final Iterable<IntervalCoding> orderedByValueByteLength;
            private static final Iterable<IntervalCoding> orderedByMaxValueInNanos;
            static {
                IntervalCoding[] byByteLen = IntervalCoding.values();
                Arrays.sort(byByteLen, Comparator.comparing(IntervalCoding::valueByteLength));
                orderedByValueByteLength = Arrays.asList(byByteLen);

                IntervalCoding[] byMaxNanos = IntervalCoding.values();
                Arrays.sort(byMaxNanos, Comparator.comparing(IntervalCoding::maxValueInNanos));
                orderedByMaxValueInNanos = Arrays.asList(byMaxNanos);
            }

            static IntervalCoding fromTimeInterval( Duration interval, CompressionStrategy compStrat )
            {
                Preconditions.checkArgument(!interval.isNegative(), "invalid negative time interval of %s", interval);

                long deltaNanos = interval.toNanos();
                for (IntervalCoding coding : getOrderedValues(compStrat)) {
                    if (deltaNanos < coding.maxValueInNanos()) {
                        return coding;
                    }
                }

                throw new IllegalArgumentException(
                    String.format("no coding of time intervals was found for the interval of %s",
                        interval));
            }

            private static Iterable<IntervalCoding> getOrderedValues( CompressionStrategy compStrat )
            {
                switch (compStrat) {
                    case MINIMIZE_MESSAGE_SIZE:
                        return orderedByValueByteLength;

                    case MAXIMIZE_TIME_PRECISION:
                        return orderedByMaxValueInNanos;

                    default:
                        throw new AssertionError("unexpected enum value");
                }
            }

            private final int                         byteLength;
            private final long                        maxNanos;
            private final ChannelSerializer<Duration> durSerializer;

            private IntervalCoding( int byteLength, long maxNanos, ChannelSerializer<Duration> durSerializer )
            {
                this.byteLength = byteLength;
                this.maxNanos = maxNanos;
                this.durSerializer = durSerializer;
            }

            int valueByteLength()
            {
                return byteLength;
            }

            long maxValueInNanos()
            {
                return maxNanos;
            }

            ChannelSerializer<Duration> durationSerializer()
            {
                return durSerializer;
            }
        }

        // private static String toResumedString( Collection<?> col )
        // {
        // final int maxElements = Math.min(6, col.size());
        // final int numPrefixElements = IntMath.divide(maxElements, 2,
        // RoundingMode.CEILING);
        // final int numSuffixElements = maxElements - numPrefixElements;
        //
        // Stream<?> prefixStream = col.stream().limit(numPrefixElements);
        // Stream<?> suffixStream = col.stream().skip(col.size() -
        // numSuffixElements);
        // Stream<?> dotsStream = (maxElements > 0) ? Stream.of("...") :
        // Stream.empty();
        //
        // Stream<?> resumedStream = Stream.concat(Stream.concat(prefixStream,
        // dotsStream), suffixStream);
        // return String.format("(size = %d)%s", col.size(),
        // StreamUtils.toString(resumedStream));
        // }

        private IO()
        {
            // not used
        }
    }
}
