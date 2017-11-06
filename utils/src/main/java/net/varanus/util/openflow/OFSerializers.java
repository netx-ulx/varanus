package net.varanus.util.openflow;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.IntFunction;

import javax.annotation.ParametersAreNonnullByDefault;

import org.projectfloodlight.openflow.exceptions.OFParseError;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.types.DatapathId;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledByteBufAllocator;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.functional.functions.ExceptionalConsumer;
import net.varanus.util.io.ByteBuffers;
import net.varanus.util.io.ByteBuffers.BufferType;
import net.varanus.util.io.DataIO;
import net.varanus.util.io.ExtraChannels;
import net.varanus.util.io.Serializers;
import net.varanus.util.io.exception.IOChannelReadException;
import net.varanus.util.io.exception.IOChannelWriteException;
import net.varanus.util.io.exception.IOReadException;
import net.varanus.util.io.exception.IOWriteException;
import net.varanus.util.io.serializer.IOReader;
import net.varanus.util.io.serializer.IOSerializer;
import net.varanus.util.io.serializer.IOWriter;


/**
 * 
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class OFSerializers
{
    public static IOSerializer<DatapathId> dpathIdSerializer()
    {
        return DatapathIdSerializer.INSTANCE;
    }

    private static enum DatapathIdSerializer implements IOSerializer<DatapathId>
    {
        INSTANCE;

        @Override
        public void write( DatapathId dpid, WritableByteChannel ch ) throws IOChannelWriteException
        {
            Serializers.longWriter(ByteOrder.BIG_ENDIAN).writeLong(dpid.getLong(), ch);
        }

        @Override
        public void write( DatapathId dpid, OutputStream out ) throws IOWriteException
        {
            Serializers.longWriter(ByteOrder.BIG_ENDIAN).writeLong(dpid.getLong(), out);
        }

        @Override
        public void write( DatapathId dpid, DataOutput out ) throws IOWriteException
        {
            Serializers.longWriter(ByteOrder.BIG_ENDIAN).writeLong(dpid.getLong(), out);
        }

        @Override
        public DatapathId read( ReadableByteChannel ch ) throws IOChannelReadException
        {
            return DatapathId.of(Serializers.longReader(ByteOrder.BIG_ENDIAN).readLong(ch));
        }

        @Override
        public DatapathId read( InputStream in ) throws IOReadException
        {
            return DatapathId.of(Serializers.longReader(ByteOrder.BIG_ENDIAN).readLong(in));
        }

        @Override
        public DatapathId read( DataInput in ) throws IOReadException
        {
            return DatapathId.of(Serializers.longReader(ByteOrder.BIG_ENDIAN).readLong(in));
        }
    }

    public static IOSerializer<Match> matchSerializer()
    {
        return MatchSerializer.INSTANCE;
    }

    private static enum MatchSerializer implements IOSerializer<Match>
    {
        INSTANCE;

        @Override
        public void write( Match match, WritableByteChannel ch ) throws IOChannelWriteException
        {
            Serializers.<OFVersion>enumWriter().write(match.getVersion(), ch);
            ByteBuf nettyBuf = Unpooled.directBuffer();
            try {
                match.writeTo(nettyBuf);
                nettyBufferWriter().write(nettyBuf, ch);
            }
            finally {
                nettyBuf.release();
            }
        }

        @Override
        public void write( Match match, OutputStream out ) throws IOWriteException
        {
            Serializers.<OFVersion>enumWriter().write(match.getVersion(), out);
            ByteBuf nettyBuf = Unpooled.buffer();
            try {
                match.writeTo(nettyBuf);
                nettyBufferWriter().write(nettyBuf, out);
            }
            finally {
                nettyBuf.release();
            }
        }

        @Override
        public void write( Match match, DataOutput out ) throws IOWriteException
        {
            Serializers.<OFVersion>enumWriter().write(match.getVersion(), out);
            ByteBuf nettyBuf = Unpooled.buffer();
            try {
                match.writeTo(nettyBuf);
                nettyBufferWriter().write(nettyBuf, out);
            }
            finally {
                nettyBuf.release();
            }
        }

        @Override
        public Match read( ReadableByteChannel ch ) throws IOChannelReadException
        {
            OFVersion ver = Serializers.enumReader(OFVersion.class).read(ch);
            ByteBuf nettyBuf = unpooledNettyBufferReader(BufferType.DIRECT).read(ch);
            try {
                return readMatch(ver, nettyBuf, IOChannelReadException::new);
            }
            finally {
                nettyBuf.release();
            }
        }

        @Override
        public Match read( InputStream in ) throws IOReadException
        {
            OFVersion ver = Serializers.enumReader(OFVersion.class).read(in);
            ByteBuf nettyBuf = unpooledNettyBufferReader(BufferType.ARRAY_BACKED).read(in);
            try {
                return readMatch(ver, nettyBuf, IOReadException::new);
            }
            finally {
                nettyBuf.release();
            }
        }

        @Override
        public Match read( DataInput in ) throws IOReadException
        {
            OFVersion ver = Serializers.enumReader(OFVersion.class).read(in);
            ByteBuf nettyBuf = unpooledNettyBufferReader(BufferType.ARRAY_BACKED).read(in);
            try {
                return readMatch(ver, nettyBuf, IOReadException::new);
            }
            finally {
                nettyBuf.release();
            }
        }

        private static <X extends IOReadException> Match readMatch( OFVersion ver,
                                                                    ByteBuf nettyBuf,
                                                                    Function<String, X> exFactory )
            throws X
        {
            try {
                return MatchUtils.getReader(ver).readFrom(nettyBuf);
            }
            catch (OFParseError e) {
                throw exFactory.apply(String.format("parsing error: %s", e.getMessage()));
            }
        }
    }

    public static IOWriter<ByteBuf> nettyBufferWriter()
    {
        return NettyBufferWriter.INSTANCE;
    }

    private static enum NettyBufferWriter implements IOWriter<ByteBuf>
    {
        INSTANCE;

        @Override
        public void write( ByteBuf nettyBuf, WritableByteChannel ch ) throws IOChannelWriteException
        {
            final int len = nettyBuf.readableBytes();
            Serializers.intWriter(ByteOrder.BIG_ENDIAN).writeInt(len, ch);

            if (ch instanceof GatheringByteChannel) {
                writeFromNettyBuffer(nettyBuf, len, (GatheringByteChannel)ch);
            }
            else {
                writeFromNioBuffers(
                    nettyBuf,
                    ByteBuffers.allocator(BufferType.DIRECT),
                    nioBuf -> ExtraChannels.writeBytes(ch, nioBuf));
            }
        }

        @Override
        public void write( ByteBuf nettyBuf, OutputStream out ) throws IOWriteException
        {
            final int len = nettyBuf.readableBytes();
            Serializers.intWriter(ByteOrder.BIG_ENDIAN).writeInt(len, out);

            writeFromNettyBuffer(nettyBuf, len, out);
        }

        @Override
        public void write( ByteBuf nettyBuf, DataOutput out ) throws IOWriteException
        {
            final int len = nettyBuf.readableBytes();
            Serializers.intWriter(ByteOrder.BIG_ENDIAN).writeInt(len, out);

            if (out instanceof OutputStream) {
                writeFromNettyBuffer(nettyBuf, len, (OutputStream)out);
            }
            else {
                writeFromNioBuffers(
                    nettyBuf,
                    ByteBuffers.allocator(BufferType.ARRAY_BACKED),
                    nioBuf -> DataIO.writeBytes(out, nioBuf));
            }
        }
    }

    public static IOReader<ByteBuf> nettyBufferReader( IntFunction<ByteBuf> bufFactory )
    {
        Objects.requireNonNull(bufFactory);
        return new IOReader<ByteBuf>() {

            @Override
            public ByteBuf read( ReadableByteChannel ch ) throws IOChannelReadException
            {
                final int len = validLength(Serializers.intReader(ByteOrder.BIG_ENDIAN).readInt(ch),
                    IOChannelReadException::new);
                final ByteBuf nettyBuf = bufFactory.apply(len);

                if (ch instanceof ScatteringByteChannel) {
                    readToNettyBuffer(nettyBuf, len, (ScatteringByteChannel)ch);
                }
                else {
                    readToNioBuffers(
                        nettyBuf,
                        ByteBuffers.allocator(BufferType.DIRECT),
                        nioBuf -> ExtraChannels.readBytes(ch, nioBuf));
                }

                return nettyBuf;
            }

            @Override
            public ByteBuf read( InputStream in ) throws IOReadException
            {
                final int len = validLength(Serializers.intReader(ByteOrder.BIG_ENDIAN).readInt(in),
                    IOReadException::new);
                final ByteBuf nettyBuf = bufFactory.apply(len);
                readToNettyBuffer(nettyBuf, len, in);
                return nettyBuf;
            }

            @Override
            public ByteBuf read( DataInput in ) throws IOReadException
            {
                final int len = validLength(Serializers.intReader(ByteOrder.BIG_ENDIAN).readInt(in),
                    IOReadException::new);
                final ByteBuf nettyBuf = bufFactory.apply(len);

                if (in instanceof InputStream) {
                    readToNettyBuffer(nettyBuf, len, (InputStream)in);
                }
                else {
                    readToNioBuffers(
                        nettyBuf,
                        ByteBuffers.allocator(BufferType.ARRAY_BACKED),
                        nioBuf -> DataIO.readBytes(in, nioBuf));
                }

                return nettyBuf;
            }

            private <X extends IOReadException> int validLength( int len, Function<String, X> exFactory ) throws X
            {
                if (len < 0)
                    throw exFactory.apply("read invalid negative length");
                else
                    return len;
            }
        };
    }

    private static void writeFromNettyBuffer( ByteBuf nettyBuf, int len, GatheringByteChannel ch )
        throws IOChannelWriteException
    {
        try {
            nettyBuf.readBytes(ch, len);
        }
        catch (IOException e) {
            throw new IOChannelWriteException(e);
        }
    }

    private static void writeFromNettyBuffer( ByteBuf nettyBuf, int len, OutputStream out ) throws IOWriteException
    {
        try {
            nettyBuf.readBytes(out, len);
        }
        catch (IOException e) {
            throw new IOWriteException(e);
        }
    }

    private static <X extends IOWriteException> void writeFromNioBuffers( ByteBuf nettyBuf,
                                                                          IntFunction<ByteBuffer> bufAlloc,
                                                                          ExceptionalConsumer<ByteBuffer, X> bufWriter )
        throws X
    {
        final int len = nettyBuf.readableBytes();

        if (nettyBuf.nioBufferCount() < 1) {
            ByteBuffer nioBuf = bufAlloc.apply(len);
            nettyBuf.readBytes(nioBuf);
            nioBuf.flip();
            bufWriter.accept(nioBuf);
        }
        else {
            for (ByteBuffer nioBuf : nettyBuf.nioBuffers(nettyBuf.readerIndex(), len)) {
                bufWriter.accept(nioBuf);
            }
            // artificially increment the buffer's position
            nettyBuf.readerIndex(nettyBuf.readerIndex() + len);
        }
    }

    private static void readToNettyBuffer( ByteBuf nettyBuf, int len, ScatteringByteChannel ch )
        throws IOChannelReadException
    {
        try {
            nettyBuf.writeBytes(ch, len);
        }
        catch (IOException e) {
            throw new IOChannelReadException(e);
        }
    }

    private static void readToNettyBuffer( ByteBuf nettyBuf, int len, InputStream in ) throws IOReadException
    {
        try {
            nettyBuf.writeBytes(in, len);
        }
        catch (IOException e) {
            throw new IOReadException(e);
        }
    }

    private static <X extends IOReadException> void readToNioBuffers( ByteBuf nettyBuf,
                                                                      IntFunction<ByteBuffer> bufAlloc,
                                                                      ExceptionalConsumer<ByteBuffer, X> bufReader )
        throws X
    {
        final int len = nettyBuf.writableBytes();

        if (nettyBuf.nioBufferCount() < 1) {
            ByteBuffer nioBuf = bufAlloc.apply(len);
            bufReader.accept(nioBuf);
            nioBuf.flip();
            nettyBuf.writeBytes(nioBuf);
        }
        else {
            for (ByteBuffer nioBuf : nettyBuf.nioBuffers(nettyBuf.writerIndex(), len)) {
                bufReader.accept(nioBuf);
            }
            // artificially increment the buffer's position
            nettyBuf.writerIndex(nettyBuf.writerIndex() + len);
        }
    }

    public static IOReader<ByteBuf> pooledNettyBufferReader( BufferType type )
    {
        switch (type) {
            case ARRAY_BACKED:
                return CachedNettyBufferReader.ARRAY_BACKED_POOLED.getReader();

            case DIRECT:
                return CachedNettyBufferReader.DIRECT_POOLED.getReader();

            default:
                throw new AssertionError("unexpected enum value");
        }
    }

    public static IOReader<ByteBuf> unpooledNettyBufferReader( BufferType type )
    {
        switch (type) {
            case ARRAY_BACKED:
                return CachedNettyBufferReader.ARRAY_BACKED_UNPOOLED.getReader();

            case DIRECT:
                return CachedNettyBufferReader.DIRECT_UNPOOLED.getReader();

            default:
                throw new AssertionError("unexpected enum value");
        }
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    private static enum CachedNettyBufferReader
    {
        ARRAY_BACKED_POOLED(PooledByteBufAllocator.DEFAULT::heapBuffer),
        DIRECT_POOLED(PooledByteBufAllocator.DEFAULT::directBuffer),
        ARRAY_BACKED_UNPOOLED(UnpooledByteBufAllocator.DEFAULT::heapBuffer),
        DIRECT_UNPOOLED(UnpooledByteBufAllocator.DEFAULT::directBuffer);

        private final IOReader<ByteBuf> reader;

        private CachedNettyBufferReader( IntFunction<ByteBuf> bufFactory )
        {
            this.reader = nettyBufferReader(bufFactory);
        }

        IOReader<ByteBuf> getReader()
        {
            return reader;
        }
    }

    private OFSerializers()
    {
        // not used
    }
}
