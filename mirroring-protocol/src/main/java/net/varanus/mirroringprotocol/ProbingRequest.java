package net.varanus.mirroringprotocol;


import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Function;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import org.projectfloodlight.openflow.types.DatapathId;
import org.slf4j.Logger;

import net.varanus.mirroringprotocol.util.CollectorId;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.io.Serializers;
import net.varanus.util.io.exception.IOChannelReadException;
import net.varanus.util.io.exception.IOChannelWriteException;
import net.varanus.util.io.serializer.IOReader;
import net.varanus.util.io.serializer.IOWriter;
import net.varanus.util.lang.MoreObjects;
import net.varanus.util.openflow.types.BitMatch;
import net.varanus.util.openflow.types.DirectedNodePort;
import net.varanus.util.time.TimeUtils;


/**
 * 
 */
@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class ProbingRequest
{
    private final DirectedNodePort switchPort;
    private final BitMatch         bitMatch;
    private final Duration         collDuration;

    public ProbingRequest( DirectedNodePort switchPort, BitMatch bitMatch, Duration collDuration )
    {
        this.switchPort = Objects.requireNonNull(switchPort);
        this.bitMatch = Objects.requireNonNull(bitMatch);
        this.collDuration = Objects.requireNonNull(collDuration);
    }

    public DirectedNodePort getSwitchPort()
    {
        return switchPort;
    }

    public BitMatch getBitMatch()
    {
        return bitMatch;
    }

    public Duration getCollectDuration()
    {
        return collDuration;
    }

    @Override
    public String toString()
    {
        return String.format("( %s, %s, %s )",
            switchPort,
            bitMatch,
            TimeUtils.toSmartDurationString(collDuration));
    }

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class IO
    {
        private static final long REQUEST_PREAMBLE = 0x66f0b6e64c8545ddL;

        public static IOWriter<ProbingRequest> writer( CollectorId collectorId, Logger log )
        {
            MoreObjects.requireNonNull(collectorId, "collectorId", log, "log");
            return new IOWriter<ProbingRequest>() {
                @Override
                public void write( ProbingRequest request, WritableByteChannel ch ) throws IOChannelWriteException
                {
                    final DirectedNodePort switchPort = request.getSwitchPort();
                    final BitMatch bitMatch = request.getBitMatch();
                    final Duration colDuration = request.getCollectDuration();

                    if (log.isTraceEnabled()) {
                        log.trace("Writing probing request to collector {}: request preamble 0x{}",
                            collectorId, Long.toHexString(REQUEST_PREAMBLE));
                    }
                    Serializers.longWriter(ByteOrder.BIG_ENDIAN).writeLong(REQUEST_PREAMBLE, ch);

                    log.trace("Writing probing request to collector {}: switch-port {}", collectorId, switchPort);
                    DirectedNodePort.IO.writer().write(switchPort, ch);

                    log.trace("Writing probing request to collector {}: bit match {}", collectorId, bitMatch);
                    BitMatch.IO.writer().write(bitMatch, ch);

                    if (log.isTraceEnabled()) {
                        log.trace("Writing probing request to collector {}: collect duration of {}",
                            collectorId, TimeUtils.toSmartDurationString(colDuration));
                    }
                    Serializers.durationWriter().write(colDuration, ch);
                }
            };
        }

        public static IOReader<ProbingRequest> reader( CollectorId collectorId,
                                                       Function<DatapathId, String> idAliaser,
                                                       Logger log )
        {
            MoreObjects.requireNonNull(collectorId, "collectorId", idAliaser, "idAliaser", log, "log");
            return new IOReader<ProbingRequest>() {
                @Override
                public ProbingRequest read( ReadableByteChannel ch ) throws IOChannelReadException
                {
                    final long preamble = Serializers.longReader(ByteOrder.BIG_ENDIAN).readLong(ch);
                    if (preamble != REQUEST_PREAMBLE) {
                        throw new IOChannelReadException(
                            String.format("expected request preamble of %x to collector %s but found %x instead",
                                REQUEST_PREAMBLE, collectorId, preamble));
                    }
                    if (log.isTraceEnabled()) {
                        log.trace("Read probing request to collector {}: request preamble 0x{}",
                            collectorId, Long.toHexString(preamble));
                    }

                    final DirectedNodePort switchPort = DirectedNodePort.IO.reader(idAliaser).read(ch);
                    log.trace("Read probing request to collector {}: switch-port {}", collectorId, switchPort);

                    final BitMatch bitMatch = BitMatch.IO.reader().read(ch);
                    log.trace("Read probing request to collector {}: bit match {}", collectorId, bitMatch);

                    final Duration colDuration = Serializers.durationReader().read(ch);
                    if (log.isTraceEnabled()) {
                        log.trace("Read probing request to collector {}: collect duration of {}",
                            collectorId, TimeUtils.toSmartDurationString(colDuration));
                    }

                    return new ProbingRequest(switchPort, bitMatch, colDuration);
                }
            };
        }

        private IO()
        {
            // not used
        }
    }
}
