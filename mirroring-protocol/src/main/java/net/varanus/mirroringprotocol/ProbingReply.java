package net.varanus.mirroringprotocol;


import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Objects;
import java.util.function.Function;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import org.projectfloodlight.openflow.types.DatapathId;
import org.slf4j.Logger;

import net.varanus.mirroringprotocol.util.CollectorId;
import net.varanus.mirroringprotocol.util.TimedPacketSummary;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.functional.Possible;
import net.varanus.util.io.Serializers;
import net.varanus.util.io.exception.IOChannelReadException;
import net.varanus.util.io.exception.IOChannelWriteException;
import net.varanus.util.io.serializer.IOReader;
import net.varanus.util.io.serializer.IOWriter;
import net.varanus.util.lang.MoreObjects;
import net.varanus.util.openflow.types.BitMatch;
import net.varanus.util.openflow.types.DirectedNodePort;


/**
 * 
 */
@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class ProbingReply
{
    private final DirectedNodePort             switchPort;
    private final BitMatch                     bitMatch;
    private final Possible<TimedPacketSummary> timedPktSumm;

    public ProbingReply( DirectedNodePort switchPort, BitMatch bitMatch, Possible<TimedPacketSummary> timedPktSumm )
    {
        this.switchPort = Objects.requireNonNull(switchPort);
        this.bitMatch = Objects.requireNonNull(bitMatch);
        this.timedPktSumm = Objects.requireNonNull(timedPktSumm);
    }

    public DirectedNodePort getSwitchPort()
    {
        return switchPort;
    }

    public BitMatch getBitMatch()
    {
        return bitMatch;
    }

    public Possible<TimedPacketSummary> getTimedPacketSummary()
    {
        return timedPktSumm;
    }

    @Override
    public String toString()
    {
        return String.format("( %s, %s, %s )",
            switchPort,
            bitMatch,
            timedPktSumm);
    }

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class IO
    {
        private static final long REPLY_PREAMBLE = 0x2290ef2986c8b576L;

        public static IOWriter<ProbingReply> writer( CollectorId collectorId, Logger log )
        {
            MoreObjects.requireNonNull(collectorId, "collectorId", log, "log");
            return new IOWriter<ProbingReply>() {
                @Override
                public void write( ProbingReply reply, WritableByteChannel ch ) throws IOChannelWriteException
                {
                    final DirectedNodePort switchPort = reply.getSwitchPort();
                    final BitMatch bitMatch = reply.getBitMatch();
                    final Possible<TimedPacketSummary> timedPktSumm = reply.getTimedPacketSummary();

                    if (log.isTraceEnabled()) {
                        log.trace("Writing probing reply from collector {}: reply preamble 0x{}",
                            collectorId, Long.toHexString(REPLY_PREAMBLE));
                    }
                    Serializers.longWriter(ByteOrder.BIG_ENDIAN).writeLong(REPLY_PREAMBLE, ch);

                    log.trace("Writing probing reply from collector {}: switch-port {}", collectorId, switchPort);
                    DirectedNodePort.IO.writer().write(switchPort, ch);

                    log.trace("Writing probing reply from collector {}: bit match {}", collectorId, bitMatch);
                    BitMatch.IO.writer().write(bitMatch, ch);

                    log.trace("Writing probing reply from collector {}: timed packet summary {}",
                        collectorId, timedPktSumm);
                    Possible.IO.writer(TimedPacketSummary.IO.writer()).write(timedPktSumm, ch);
                }
            };
        }

        public static IOReader<ProbingReply> reader( CollectorId collectorId,
                                                     Function<DatapathId, String> idAliaser,
                                                     Logger log )
        {
            MoreObjects.requireNonNull(collectorId, "collectorId", idAliaser, "idAliaser", log, "log");
            return new IOReader<ProbingReply>() {
                @Override
                public ProbingReply read( ReadableByteChannel ch ) throws IOChannelReadException
                {
                    final long preamble = Serializers.longReader(ByteOrder.BIG_ENDIAN).readLong(ch);
                    if (preamble != REPLY_PREAMBLE) {
                        throw new IOChannelReadException(
                            String.format("expected reply preamble of %x from collector %s but found %x instead",
                                REPLY_PREAMBLE, collectorId, preamble));
                    }
                    if (log.isTraceEnabled()) {
                        log.trace("Read probing reply from collector {}: reply preamble 0x{}",
                            collectorId, Long.toHexString(preamble));
                    }

                    final DirectedNodePort switchPort = DirectedNodePort.IO.reader(idAliaser).read(ch);
                    log.trace("Read probing reply from collector {}: switch-port {}", collectorId, switchPort);

                    final BitMatch bitMatch = BitMatch.IO.reader().read(ch);
                    log.trace("Read probing reply from collector {}: bit match {}", collectorId, bitMatch);

                    final Possible<TimedPacketSummary> timedPktSumm = Possible.IO.reader(TimedPacketSummary.IO.reader())
                        .read(ch);
                    log.trace("Read probing reply from collector {}: timed packet summary {}",
                        collectorId, timedPktSumm);

                    return new ProbingReply(switchPort, bitMatch, timedPktSumm);
                }
            };
        }

        private IO()
        {
            // not used
        }
    }
}
