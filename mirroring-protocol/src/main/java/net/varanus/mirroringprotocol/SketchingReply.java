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
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.io.Serializers;
import net.varanus.util.io.exception.IOChannelReadException;
import net.varanus.util.io.exception.IOChannelWriteException;
import net.varanus.util.io.serializer.IOReader;
import net.varanus.util.io.serializer.IOWriter;
import net.varanus.util.lang.MoreObjects;
import net.varanus.util.openflow.types.DirectedNodePort;
import net.varanus.util.openflow.types.Flow;


/**
 * 
 */
@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class SketchingReply
{
    private final DirectedNodePort switchPort;
    private final Flow             flow;
    // TODO add sketch data structure and algorithms

    public SketchingReply( DirectedNodePort switchPort,
                           Flow flow )
    {
        this.switchPort = Objects.requireNonNull(switchPort);
        this.flow = Objects.requireNonNull(flow);
    }

    public DirectedNodePort getSwitchPort()
    {
        return switchPort;
    }

    public Flow getFlow()
    {
        return flow;
    }

    @Override
    public String toString()
    {
        return String.format("( %s, %s )",
            switchPort,
            flow);
    }

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class IO
    {
        private static final long REPLY_PREAMBLE = 0x076afeb9cd34ac3dL;

        public static IOWriter<SketchingReply> writer( CollectorId collectorId,
                                                       Logger log )
        {
            MoreObjects.requireNonNull(collectorId, "collectorId", log, "log");
            return new IOWriter<SketchingReply>() {
                @Override
                public void write( SketchingReply reply, WritableByteChannel ch ) throws IOChannelWriteException
                {
                    final DirectedNodePort switchPort = reply.getSwitchPort();
                    final Flow flow = reply.getFlow();

                    if (log.isTraceEnabled()) {
                        log.trace("Writing sketching reply from collector {}: reply preamble 0x{}",
                            collectorId, Long.toHexString(REPLY_PREAMBLE));
                    }
                    Serializers.longWriter(ByteOrder.BIG_ENDIAN).writeLong(REPLY_PREAMBLE, ch);

                    log.trace("Writing sketching reply from collector {}: switch-port {}", collectorId, switchPort);
                    DirectedNodePort.IO.writer().write(switchPort, ch);

                    log.trace("Writing sketching reply from collector {}: flow {}", collectorId, flow);
                    Flow.IO.writer().write(flow, ch);
                }
            };
        }

        public static IOReader<SketchingReply> reader( CollectorId collectorId,
                                                       Function<DatapathId, String> idAliaser,
                                                       Logger log )
        {
            MoreObjects.requireNonNull(collectorId, "collectorId", idAliaser, "idAliaser", log, "log");
            return new IOReader<SketchingReply>() {
                @Override
                public SketchingReply read( ReadableByteChannel ch ) throws IOChannelReadException
                {
                    final long preamble = Serializers.longReader(ByteOrder.BIG_ENDIAN).readLong(ch);
                    if (preamble != REPLY_PREAMBLE) {
                        throw new IOChannelReadException(
                            String.format("expected reply preamble of %x from collector %s but found %x instead",
                                REPLY_PREAMBLE, collectorId, preamble));
                    }
                    if (log.isTraceEnabled()) {
                        log.trace("Read sketching reply from collector {}: reply preamble 0x{}",
                            collectorId, Long.toHexString(preamble));
                    }

                    final DirectedNodePort switchPort = DirectedNodePort.IO.reader(idAliaser).read(ch);
                    log.trace("Read sketching reply from collector {}: switch-port {}", collectorId, switchPort);

                    final Flow flow = Flow.IO.reader().read(ch);
                    log.trace("Read sketching reply from collector {}: flow {}", collectorId, flow);

                    return new SketchingReply(switchPort, flow);
                }
            };
        }
    }
}
