package net.varanus.infoprotocol;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Objects;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import org.slf4j.Logger;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.io.BufferOperation;
import net.varanus.util.io.ByteBuffers;
import net.varanus.util.io.ByteBuffers.BufferType;
import net.varanus.util.io.Serializers;
import net.varanus.util.io.exception.IOChannelReadException;
import net.varanus.util.io.exception.IOChannelWriteException;
import net.varanus.util.io.serializer.IOReader;
import net.varanus.util.io.serializer.IOWriter;
import net.varanus.util.openflow.types.Flow;


/**
 * 
 */
@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class PacketRequest extends Generic.Packet
{
    private static final ByteBuffer EMPTY_PAYLOAD = ByteBuffer.wrap(new byte[0]);

    public static PacketRequest of( Flow flow )
    {
        return new PacketRequest(
            Objects.requireNonNull(flow),
            EMPTY_PAYLOAD);
    }

    public static PacketRequest of( Flow flow, ByteBuffer payload )
    {
        return new PacketRequest(
            Objects.requireNonNull(flow),
            ByteBuffers.getCopy(payload, BufferOperation.RESTORE_POSITION));
    }

    public static PacketRequest of( Flow flow, byte[] payload )
    {
        return new PacketRequest(
            Objects.requireNonNull(flow),
            ByteBuffers.getCopy(payload, BufferType.DIRECT));
    }

    private final Flow       flow;
    private final ByteBuffer payload;

    private PacketRequest( Flow flow, ByteBuffer payload )
    {
        this.flow = flow;
        this.payload = payload;
    }

    public Flow getFlow()
    {
        return flow;
    }

    public ByteBuffer getPayload()
    {
        return readOnlyPayload();
    }

    @Override
    public String toString()
    {
        return String.format("( flow %s; payload[%s] )", flow, ByteBuffers.toHexString(readOnlyPayload()));
    }

    private ByteBuffer readOnlyPayload()
    {
        return payload.asReadOnlyBuffer();
    }

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class IO
    {
        private static final long REQUEST_PREAMBLE = 0x3628dbdda0b7cfaaL;

        public static IOWriter<PacketRequest> writer( Logger log )
        {
            Objects.requireNonNull(log);
            return new IOWriter<PacketRequest>() {
                @Override
                public void write( PacketRequest request, WritableByteChannel ch ) throws IOChannelWriteException
                {
                    if (log.isTraceEnabled()) {
                        log.trace("Writing packet request to server: request preamble 0x{}",
                            Long.toHexString(REQUEST_PREAMBLE));
                    }
                    Serializers.longWriter(ByteOrder.BIG_ENDIAN).writeLong(REQUEST_PREAMBLE, ch);

                    Flow flow = request.flow;
                    log.trace("Writing packet request to server: flow {}", flow);
                    Flow.IO.writer().write(flow, ch);

                    ByteBuffer payload = request.readOnlyPayload();
                    log.trace("Writing packet request to server: payload[{}]", ByteBuffers.toHexString(payload));
                    Serializers.bufferWriter().write(payload, ch);
                }
            };
        }

        public static IOReader<PacketRequest> reader( Logger log )
        {
            Objects.requireNonNull(log);
            return new IOReader<PacketRequest>() {
                @Override
                public PacketRequest read( ReadableByteChannel ch ) throws IOChannelReadException
                {
                    long preamble = Serializers.longReader(ByteOrder.BIG_ENDIAN).readLong(ch);
                    if (preamble != REQUEST_PREAMBLE) {
                        throw new IOChannelReadException(
                            String.format("expected request preamble of %x to server but found %x instead",
                                REQUEST_PREAMBLE, preamble));
                    }
                    if (log.isTraceEnabled()) {
                        log.trace("Read packet request to server: request preamble 0x{}",
                            Long.toHexString(preamble));
                    }

                    Flow flow = Flow.IO.reader().read(ch);
                    log.trace("Read packet request to server: flow {}", flow);

                    ByteBuffer payload = Serializers.allocatedBufferReader(BufferType.ARRAY_BACKED).read(ch);
                    log.trace("Read packet request to server: payload[{}]", ByteBuffers.toHexString(payload));

                    return new PacketRequest(flow, payload);
                }
            };
        }

        private IO()
        {
            // not used
        }
    }
}
