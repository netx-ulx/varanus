package net.varanus.infoprotocol;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.NoSuchElementException;
import java.util.Objects;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import org.slf4j.Logger;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.functional.Report;
import net.varanus.util.io.BufferOperation;
import net.varanus.util.io.ByteBuffers;
import net.varanus.util.io.ByteBuffers.BufferType;
import net.varanus.util.io.Serializers;
import net.varanus.util.io.exception.IOChannelReadException;
import net.varanus.util.io.exception.IOChannelWriteException;
import net.varanus.util.io.serializer.IOReader;
import net.varanus.util.io.serializer.IOWriter;


/**
 * 
 */
@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class PacketReply extends Generic.Packet
{
    public static PacketReply of( ByteBuffer packet )
    {
        return new PacketReply(Report.of(new Result(
            ByteBuffers.getCopy(packet, BufferOperation.RESTORE_POSITION))));
    }

    public static PacketReply of( byte[] packet )
    {
        return new PacketReply(Report.of(new Result(
            ByteBuffers.getCopy(packet, BufferType.DIRECT))));
    }

    public static PacketReply ofError( String errorMsg )
    {
        return new PacketReply(Report.ofError(errorMsg));
    }

    private final Report<Result> result;

    private PacketReply( Report<Result> result )
    {
        this.result = result;
    }

    public boolean hasResult()
    {
        return result.hasValue();
    }

    public ByteBuffer getPacket() throws NoSuchElementException
    {
        return result.getValue().readOnlyPacket();
    }

    public boolean hasError()
    {
        return result.hasError();
    }

    public String getError() throws NoSuchElementException
    {
        return result.getError();
    }

    @Override
    public String toString()
    {
        return String.format("( %s )", result);
    }

    @Immutable
    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    private static final class Result
    {
        final ByteBuffer packet;

        Result( ByteBuffer packet )
        {
            this.packet = packet;
        }

        ByteBuffer readOnlyPacket()
        {
            return packet.asReadOnlyBuffer();
        }

        @Override
        public String toString()
        {
            return String.format("bytes[%s]", ByteBuffers.toHexString(readOnlyPacket()));
        }
    }

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class IO
    {
        private static final long REPLY_PREAMBLE = 0xdbcdd8488d88da2bL;

        public static IOWriter<PacketReply> writer( Logger log )
        {
            Objects.requireNonNull(log);
            return new IOWriter<PacketReply>() {
                @Override
                public void write( PacketReply reply, WritableByteChannel ch ) throws IOChannelWriteException
                {
                    if (log.isTraceEnabled()) {
                        log.trace("Writing packet reply from server: reply preamble 0x{}",
                            Long.toHexString(REPLY_PREAMBLE));
                    }
                    Serializers.longWriter(ByteOrder.BIG_ENDIAN).writeLong(REPLY_PREAMBLE, ch);

                    Report<Result> result = reply.result;
                    log.trace("Writing packet reply from server: {}", result);
                    Report.IO.writer(resultWriter()).write(result, ch);
                }
            };
        }

        public static IOReader<PacketReply> reader( Logger log )
        {
            Objects.requireNonNull(log);
            return new IOReader<PacketReply>() {
                @Override
                public PacketReply read( ReadableByteChannel ch ) throws IOChannelReadException
                {
                    long preamble = Serializers.longReader(ByteOrder.BIG_ENDIAN).readLong(ch);
                    if (preamble != REPLY_PREAMBLE) {
                        throw new IOChannelReadException(
                            String.format("expected reply preamble of %x from server but found %x instead",
                                REPLY_PREAMBLE, preamble));
                    }
                    if (log.isTraceEnabled()) {
                        log.trace("Read packet reply from server: request preamble 0x{}",
                            Long.toHexString(preamble));
                    }

                    Report<Result> result = Report.IO.reader(resultReader()).read(ch);
                    log.trace("Read packet reply from server: {}", result);

                    return new PacketReply(result);
                }
            };
        }

        private static IOWriter<Result> resultWriter()
        {
            return new IOWriter<Result>() {

                @Override
                public void write( Result res, WritableByteChannel ch ) throws IOChannelWriteException
                {
                    Serializers.bufferWriter().write(res.readOnlyPacket(), ch);
                }
            };
        }

        private static IOReader<Result> resultReader()
        {
            return new IOReader<Result>() {

                @Override
                public Result read( ReadableByteChannel ch ) throws IOChannelReadException
                {
                    final ByteBuffer packet = Serializers.allocatedBufferReader(BufferType.ARRAY_BACKED).read(ch);

                    return new Result(packet);
                }
            };
        }

        private IO()
        {
            // not used
        }
    }
}
