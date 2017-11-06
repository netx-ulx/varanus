package net.varanus.util.io;


import java.io.EOFException;
import java.io.IOException;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ByteChannel;
import java.nio.channels.Channel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Objects;
import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.io.exception.IOChannelReadException;
import net.varanus.util.io.exception.IOChannelWriteException;
import net.varanus.util.lang.SizeOf;


/**
 * Class containing utility methods not present in class
 * {@link java.nio.channels.Channels}.
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class ExtraChannels
{
    /**
     * Writes to a channel all the available bytes from a buffer. The buffer
     * will be consumed from its position to its limit.
     * <p>
     * Calling this method has the same effect as calling
     * {@link #writeBytes(WritableByteChannel, ByteBuffer, int, BufferOperation)
     * writeBytes(ch, buf, buf.remaining(), BufferOperation.ADVANCE_POSITION)}
     * 
     * @param ch
     *            The channel used to write bytes
     * @param buf
     *            The buffer containing the bytes to be written
     * @throws IOChannelWriteException
     *             If an I/O error occurs while writing
     */
    public static void writeBytes( WritableByteChannel ch, ByteBuffer buf ) throws IOChannelWriteException
    {
        writeBytes(ch, buf, buf.remaining(), BufferOperation.ADVANCE_POSITION);
    }

    /**
     * Writes to a channel a specific number of bytes from a buffer.
     * <p>
     * Calling this method has the same effect as calling
     * {@link #writeBytes(WritableByteChannel, ByteBuffer, int, BufferOperation)
     * writeBytes(ch, buf, numBytes, BufferOperation.ADVANCE_POSITION)}
     * 
     * @param ch
     *            The channel used to write bytes
     * @param buf
     *            The buffer containing the bytes to be written
     * @param numBytes
     *            The number of bytes to write
     * @throws IOChannelWriteException
     *             If an I/O error occurs while writing
     * @exception IllegalArgumentException
     *                If {@code numBytes} is negative
     * @exception BufferUnderflowException
     *                If the provided buffer does not have at least
     *                {@code numBytes} bytes available to write
     */
    public static void writeBytes( WritableByteChannel ch, ByteBuffer buf, int numBytes ) throws IOChannelWriteException
    {
        writeBytes(ch, buf, numBytes, BufferOperation.ADVANCE_POSITION);
    }

    /**
     * Writes to a channel all the available bytes from a buffer. The buffer
     * will be consumed from its position to its limit.
     * <p>
     * Calling this method has the same effect as calling
     * {@link #writeBytes(WritableByteChannel, ByteBuffer, int, BufferOperation)
     * writeBytes(ch, buf, buf.remaining(), op)}
     * 
     * @param ch
     *            The channel used to write bytes
     * @param buf
     *            The buffer containing the bytes to be written
     * @param op
     *            The operation to apply to the provided buffer after writing
     * @throws IOChannelWriteException
     *             If an I/O error occurs while writing
     */
    public static void writeBytes( WritableByteChannel ch, ByteBuffer buf, BufferOperation op )
        throws IOChannelWriteException
    {
        writeBytes(ch, buf, buf.remaining(), op);
    }

    /**
     * Writes to a channel a specific number of bytes from a buffer.
     * 
     * @param ch
     *            The channel used to write bytes
     * @param buf
     *            The buffer containing the bytes to be written
     * @param numBytes
     *            The number of bytes to write
     * @param op
     *            The operation to apply to the provided buffer after writing
     * @throws IOChannelWriteException
     *             If an I/O error occurs while writing
     * @exception IllegalArgumentException
     *                If {@code numBytes} is negative
     * @exception BufferUnderflowException
     *                If the provided buffer does not have at least
     *                {@code numBytes} bytes available to write
     */
    public static void writeBytes( WritableByteChannel ch,
                                   ByteBuffer buf,
                                   int numBytes,
                                   BufferOperation op )
        throws IOChannelWriteException
    {
        final int bufPos = buf.position();
        final int bufLim = buf.limit();
        final int remaining = bufLim - bufPos;

        if (numBytes < 0) throw new IllegalArgumentException("number of bytes is negative");
        if (remaining < numBytes) throw new BufferUnderflowException();
        Objects.requireNonNull(op);

        try {
            buf.limit(bufPos + numBytes);
            writeFully(ch, buf);
        }
        finally {
            buf.limit(bufLim); // always restore the original limit
        }

        // only apply the operation if no exception was previously thrown
        op.apply(buf, bufPos, buf.position());
    }

    /**
     * Writes a single byte to a channel.
     * 
     * @param ch
     *            The channel used to write a byte
     * @param b
     *            The byte value to be written
     * @throws IOChannelWriteException
     *             If an I/O error occurs while writing
     */
    public static void writeByte( WritableByteChannel ch, byte b ) throws IOChannelWriteException
    {
        ByteBuffer buf = tinyBuffer(SizeOf.BYTE);
        ByteBuffers.putByteAtIndex(0, buf, b);
        writeFully(ch, buf);
    }

    /**
     * Writes a single character to a channel.
     * 
     * @param ch
     *            The channel used to write a char
     * @param c
     *            The char value to be written
     * @param order
     *            The byte order in which to write the value
     * @throws IOChannelWriteException
     *             If an I/O error occurs while writing
     */
    public static void writeChar( WritableByteChannel ch, char c, ByteOrder order ) throws IOChannelWriteException
    {
        ByteBuffer buf = tinyBuffer(SizeOf.CHAR);
        ByteBuffers.putCharAtIndex(0, buf, c, order);
        writeFully(ch, buf);
    }

    /**
     * Writes a single short integer to a channel.
     * 
     * @param ch
     *            The channel used to write a short
     * @param s
     *            The short value to be written
     * @param order
     *            The byte order in which to write the value
     * @throws IOChannelWriteException
     *             If an I/O error occurs while writing
     */
    public static void writeShort( WritableByteChannel ch, short s, ByteOrder order ) throws IOChannelWriteException
    {
        ByteBuffer buf = tinyBuffer(SizeOf.SHORT);
        ByteBuffers.putShortAtIndex(0, buf, s, order);
        writeFully(ch, buf);
    }

    /**
     * Writes a single 24-bit medium integer to a channel.
     * 
     * @param ch
     *            The channel used to write a short
     * @param m
     *            The 24-bit medium value to be written
     * @param order
     *            The byte order in which to write the value
     * @throws IOChannelWriteException
     *             If an I/O error occurs while writing
     */
    public static void writeMedium( WritableByteChannel ch, int m, ByteOrder order ) throws IOChannelWriteException
    {
        ByteBuffer buf = tinyBuffer(SizeOf.MEDIUM);
        ByteBuffers.putMediumAtIndex(0, buf, m, order);
        writeFully(ch, buf);
    }

    /**
     * Writes a single integer to a channel.
     * 
     * @param ch
     *            The channel used to write an int
     * @param i
     *            The int value to be written
     * @param order
     *            The byte order in which to write the value
     * @throws IOChannelWriteException
     *             If an I/O error occurs while writing
     */
    public static void writeInt( WritableByteChannel ch, int i, ByteOrder order ) throws IOChannelWriteException
    {
        ByteBuffer buf = tinyBuffer(SizeOf.INT);
        ByteBuffers.putIntAtIndex(0, buf, i, order);
        writeFully(ch, buf);
    }

    /**
     * Writes a single long integer to a channel.
     * 
     * @param ch
     *            The channel used to write a long
     * @param eL
     *            The long value to be written
     * @param order
     *            The byte order in which to write the value
     * @throws IOChannelWriteException
     *             If an I/O error occurs while writing
     */
    public static void writeLong( WritableByteChannel ch, long eL, ByteOrder order ) throws IOChannelWriteException
    {
        ByteBuffer buf = tinyBuffer(SizeOf.LONG);
        ByteBuffers.putLongAtIndex(0, buf, eL, order);
        writeFully(ch, buf);
    }

    /**
     * Writes a single float to a channel.
     * 
     * @param ch
     *            The channel used to write a float
     * @param f
     *            The float value to be written
     * @param order
     *            The byte order in which to write the value
     * @throws IOChannelWriteException
     *             If an I/O error occurs while writing
     */
    public static void writeFloat( WritableByteChannel ch, float f, ByteOrder order ) throws IOChannelWriteException
    {
        ByteBuffer buf = tinyBuffer(SizeOf.FLOAT);
        ByteBuffers.putFloatAtIndex(0, buf, f, order);
        writeFully(ch, buf);
    }

    /**
     * Writes a single double to a channel.
     * 
     * @param ch
     *            The channel used to write a double
     * @param d
     *            The double value to be written
     * @param order
     *            The byte order in which to write the value
     * @throws IOChannelWriteException
     *             If an I/O error occurs while writing
     */
    public static void writeDouble( WritableByteChannel ch, double d, ByteOrder order ) throws IOChannelWriteException
    {
        ByteBuffer buf = tinyBuffer(SizeOf.DOUBLE);
        ByteBuffers.putDoubleAtIndex(0, buf, d, order);
        writeFully(ch, buf);
    }

    /**
     * Reads into a buffer bytes from a channel. The buffer will be filled from
     * its position to its limit.
     * <p>
     * Calling this method has the same effect as calling
     * {@link #readBytes(ReadableByteChannel, ByteBuffer, int, BufferOperation)
     * readBytes(ch, buf, buf.remaining(), BufferOperation.ADVANCE_POSITION)}
     * 
     * @param ch
     *            The channel used to read bytes from
     * @param buf
     *            The buffer used to store the read bytes
     * @throws IOChannelReadException
     *             If an I/O error occurs while reading
     */
    public static void readBytes( ReadableByteChannel ch, ByteBuffer buf ) throws IOChannelReadException
    {
        readBytes(ch, buf, buf.remaining(), BufferOperation.ADVANCE_POSITION);
    }

    /**
     * Reads into a buffer a specific number of bytes from a channel.
     * <p>
     * Calling this method has the same effect as calling
     * {@link #readBytes(ReadableByteChannel, ByteBuffer, int, BufferOperation)
     * readBytes(ch, buf, numBytes, BufferOperation.ADVANCE_POSITION)}
     * 
     * @param ch
     *            The channel used to read bytes from
     * @param buf
     *            The buffer used to store the read bytes
     * @param numBytes
     *            The number of bytes to read
     * @throws IOChannelReadException
     *             If an I/O error occurs while reading
     * @exception IllegalArgumentException
     *                If {@code numBytes} is negative
     * @exception BufferOverflowException
     *                If the provided buffer does not have at least
     *                {@code numBytes} bytes available for storage
     */
    public static void readBytes( ReadableByteChannel ch, ByteBuffer buf, int numBytes ) throws IOChannelReadException
    {
        readBytes(ch, buf, numBytes, BufferOperation.ADVANCE_POSITION);
    }

    /**
     * Reads into a buffer bytes from a channel. The buffer will be filled from
     * its position to its limit.
     * <p>
     * Calling this method has the same effect as calling
     * {@link #readBytes(ReadableByteChannel, ByteBuffer, int, BufferOperation)
     * readBytes(ch, buf, buf.remaining(), op)}
     * 
     * @param ch
     *            The channel used to read bytes from
     * @param buf
     *            The buffer used to store the read bytes
     * @param op
     *            The operation to apply to the provided buffer after reading
     * @throws IOChannelReadException
     *             If an I/O error occurs while reading
     */
    public static void readBytes( ReadableByteChannel ch, ByteBuffer buf, BufferOperation op )
        throws IOChannelReadException
    {
        readBytes(ch, buf, buf.remaining(), op);
    }

    /**
     * Reads into a buffer a specific number of bytes from a channel.
     * 
     * @param ch
     *            The channel used to read bytes from
     * @param buf
     *            The buffer used to store the read bytes
     * @param numBytes
     *            The number of bytes to read
     * @param op
     *            The operation to apply to the provided buffer after reading
     * @throws IOChannelReadException
     *             If an I/O error occurs while reading
     * @exception IllegalArgumentException
     *                If {@code numBytes} is negative
     * @exception BufferOverflowException
     *                If the provided buffer does not have at least
     *                {@code numBytes} bytes available for storage
     */
    public static void readBytes( ReadableByteChannel ch,
                                  ByteBuffer buf,
                                  int numBytes,
                                  BufferOperation op )
        throws IOChannelReadException
    {
        final int bufPos = buf.position();
        final int bufLim = buf.limit();
        final int remaining = bufLim - bufPos;

        if (numBytes < 0) throw new IllegalArgumentException("number of bytes is negative");
        if (remaining < numBytes) throw new BufferOverflowException();
        Objects.requireNonNull(op);

        try {
            buf.limit(bufPos + numBytes);
            readFully(ch, buf);
        }
        finally {
            buf.limit(bufLim); // always restore the original limit
        }

        // only apply the operation if no exception was previously thrown
        op.apply(buf, bufPos, buf.position());
    }

    /**
     * Reads a single byte from a channel.
     * 
     * @param ch
     *            The channel used to read the byte value
     * @return a byte value
     * @throws IOChannelReadException
     *             If an I/O error occurs while reading
     */
    public static byte readByte( ReadableByteChannel ch ) throws IOChannelReadException
    {
        ByteBuffer buf = tinyBuffer(SizeOf.BYTE);
        readFully(ch, buf);
        return ByteBuffers.getByteAtIndex(0, buf);
    }

    /**
     * Reads a single character from a channel.
     * 
     * @param ch
     *            The channel used to read the char value
     * @param order
     *            The byte order in which to write the value
     * @return a char value
     * @throws IOChannelReadException
     *             If an I/O error occurs while reading
     */
    public static char readChar( ReadableByteChannel ch, ByteOrder order ) throws IOChannelReadException
    {
        ByteBuffer buf = tinyBuffer(SizeOf.CHAR);
        readFully(ch, buf);
        return ByteBuffers.getCharAtIndex(0, buf, order);
    }

    /**
     * Reads a single short integer from a channel.
     * 
     * @param ch
     *            The channel used to read the short value
     * @param order
     *            The byte order in which to write the value
     * @return a short value
     * @throws IOChannelReadException
     *             If an I/O error occurs while reading
     */
    public static short readShort( ReadableByteChannel ch, ByteOrder order ) throws IOChannelReadException
    {
        ByteBuffer buf = tinyBuffer(SizeOf.SHORT);
        readFully(ch, buf);
        return ByteBuffers.getShortAtIndex(0, buf, order);
    }

    /**
     * Reads a single 24-bit medium integer from a channel.
     * 
     * @param ch
     *            The channel used to read the short value
     * @param order
     *            The byte order in which to write the value
     * @return a 24-bit medium value
     * @throws IOChannelReadException
     *             If an I/O error occurs while reading
     */
    public static int readMedium( ReadableByteChannel ch, ByteOrder order ) throws IOChannelReadException
    {
        ByteBuffer buf = tinyBuffer(SizeOf.MEDIUM);
        readFully(ch, buf);
        return ByteBuffers.getMediumAtIndex(0, buf, order);
    }

    /**
     * Reads a single integer from a channel.
     * 
     * @param ch
     *            The channel used to read the int value
     * @param order
     *            The byte order in which to write the value
     * @return an int value
     * @throws IOChannelReadException
     *             If an I/O error occurs while reading
     */
    public static int readInt( ReadableByteChannel ch, ByteOrder order ) throws IOChannelReadException
    {
        ByteBuffer buf = tinyBuffer(SizeOf.INT);
        readFully(ch, buf);
        return ByteBuffers.getIntAtIndex(0, buf, order);
    }

    /**
     * Reads a single long integer from a channel.
     * 
     * @param ch
     *            The channel used to read the long value
     * @param order
     *            The byte order in which to write the value
     * @return a long value
     * @throws IOChannelReadException
     *             If an I/O error occurs while reading
     */
    public static long readLong( ReadableByteChannel ch, ByteOrder order ) throws IOChannelReadException
    {
        ByteBuffer buf = tinyBuffer(SizeOf.LONG);
        readFully(ch, buf);
        return ByteBuffers.getLongAtIndex(0, buf, order);
    }

    /**
     * Reads a single float from a channel.
     * 
     * @param ch
     *            The channel used to read the float value
     * @param order
     *            The byte order in which to write the value
     * @return a float value
     * @throws IOChannelReadException
     *             If an I/O error occurs while reading
     */
    public static float readFloat( ReadableByteChannel ch, ByteOrder order ) throws IOChannelReadException
    {
        ByteBuffer buf = tinyBuffer(SizeOf.FLOAT);
        readFully(ch, buf);
        return ByteBuffers.getFloatAtIndex(0, buf, order);
    }

    /**
     * Reads a single double from a channel.
     * 
     * @param ch
     *            The channel used to read the double value
     * @param order
     *            The byte order in which to write the value
     * @return a double value
     * @throws IOChannelReadException
     *             If an I/O error occurs while reading
     */
    public static double readDouble( ReadableByteChannel ch, ByteOrder order ) throws IOChannelReadException
    {
        ByteBuffer buf = tinyBuffer(SizeOf.DOUBLE);
        readFully(ch, buf);
        return ByteBuffers.getDoubleAtIndex(0, buf, order);
    }

    public static TraceableWritableByteChannel getTraceable( WritableByteChannel ch, Consumer<ByteBuffer> tracer )
    {
        return new TraceableWritableByteChannel(Objects.requireNonNull(ch), Objects.requireNonNull(tracer));
    }

    public static TraceableReadableByteChannel getTraceable( ReadableByteChannel ch, Consumer<ByteBuffer> tracer )
    {
        return new TraceableReadableByteChannel(Objects.requireNonNull(ch), Objects.requireNonNull(tracer));
    }

    public static TraceableByteChannel getTraceable( ByteChannel ch,
                                                     Consumer<ByteBuffer> writeTracer,
                                                     Consumer<ByteBuffer> readTracer )
    {
        return new TraceableByteChannel(
            Objects.requireNonNull(ch),
            Objects.requireNonNull(writeTracer),
            Objects.requireNonNull(readTracer));
    }

    private static void writeFully( WritableByteChannel ch, ByteBuffer buf ) throws IOChannelWriteException
    {
        while (buf.hasRemaining()) {
            _write(ch, buf);
        }
    }

    private static void readFully( ReadableByteChannel ch, ByteBuffer buf ) throws IOChannelReadException
    {
        while (buf.hasRemaining()) {
            if (_read(ch, buf) == -1) {
                throw new IOChannelReadException(new EOFException());
            }
        }
    }

    private static int _write( WritableByteChannel ch, ByteBuffer buf ) throws IOChannelWriteException
    {
        try {
            return ch.write(buf);
        }
        catch (IOException e) {
            throw new IOChannelWriteException(e);
        }
    }

    private static int _read( ReadableByteChannel ch, ByteBuffer buf ) throws IOChannelReadException
    {
        try {
            return ch.read(buf);
        }
        catch (IOException e) {
            throw new IOChannelReadException(e);
        }
    }

    /**
     * Using a thread local of a tiny buffer is more efficient than allocating
     * the buffer each time.
     */
    private static final ThreadLocal<ByteBuffer> TINY_BUFFER_CACHE = new ThreadLocal<ByteBuffer>() {

        @Override
        protected ByteBuffer initialValue()
        {
            return ByteBuffer.allocateDirect(SizeOf.LONG);
        }
    };

    private static ByteBuffer tinyBuffer( int limit )
    {
        ByteBuffer buf = TINY_BUFFER_CACHE.get();
        buf.clear().limit(limit);
        return buf;
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    private static abstract class DelegatedChannel<C extends Channel> implements Channel
    {
        private final C delegate;

        protected DelegatedChannel( C delegate )
        {
            this.delegate = delegate;
        }

        public C delegate()
        {
            return delegate;
        }

        @Override
        public final boolean isOpen()
        {
            return delegate.isOpen();
        }

        @Override
        public final void close() throws IOException
        {
            delegate.close();
        }
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    public static final class TraceableWritableByteChannel
        extends DelegatedChannel<WritableByteChannel> implements WritableByteChannel
    {
        private final Consumer<ByteBuffer> tracer;

        private TraceableWritableByteChannel( WritableByteChannel delegate, Consumer<ByteBuffer> tracer )
        {
            super(delegate);
            this.tracer = tracer;
        }

        @Override
        public int write( ByteBuffer src ) throws IOException
        {
            return writeAndTrace(delegate(), src, tracer);
        }
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    public static final class TraceableReadableByteChannel
        extends DelegatedChannel<ReadableByteChannel> implements ReadableByteChannel
    {
        private final Consumer<ByteBuffer> tracer;

        private TraceableReadableByteChannel( ReadableByteChannel delegate, Consumer<ByteBuffer> tracer )
        {
            super(delegate);
            this.tracer = tracer;
        }

        @Override
        public int read( ByteBuffer dst ) throws IOException
        {
            return readAndTrace(delegate(), dst, tracer);
        }
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    public static final class TraceableByteChannel
        extends DelegatedChannel<ByteChannel> implements ByteChannel
    {
        private final Consumer<ByteBuffer> writeTracer;
        private final Consumer<ByteBuffer> readTracer;

        private TraceableByteChannel( ByteChannel delegate,
                                      Consumer<ByteBuffer> writeTracer,
                                      Consumer<ByteBuffer> readTracer )
        {
            super(delegate);
            this.writeTracer = writeTracer;
            this.readTracer = readTracer;
        }

        @Override
        public int write( ByteBuffer src ) throws IOException
        {
            return writeAndTrace(delegate(), src, writeTracer);
        }

        @Override
        public int read( ByteBuffer dst ) throws IOException
        {
            return readAndTrace(delegate(), dst, readTracer);
        }
    }

    private static int writeAndTrace( WritableByteChannel ch, ByteBuffer src, Consumer<ByteBuffer> tracer )
        throws IOException
    {
        int initPos = src.position();
        int numWritten = ch.write(src);
        if (numWritten > 0)
            tracer.accept(ByteBuffers.getSliceAtIndex(initPos, src, numWritten));
        return numWritten;
    }

    private static int readAndTrace( ReadableByteChannel ch, ByteBuffer dst, Consumer<ByteBuffer> tracer )
        throws IOException
    {
        int initPos = dst.position();
        int numRead = ch.read(dst);
        if (numRead > 0)
            tracer.accept(ByteBuffers.getSliceAtIndex(initPos, dst, numRead));
        return numRead;
    }

    private ExtraChannels()
    {
        // not used
    }
}
