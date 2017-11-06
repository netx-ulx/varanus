package net.varanus.util.io;


import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.io.ByteBuffers.BufferType;
import net.varanus.util.io.exception.IOReadException;
import net.varanus.util.io.exception.IOWriteException;
import net.varanus.util.lang.SizeOf;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class IOStreams
{
    /**
     * Writes to an output stream all the available bytes from a buffer. The
     * buffer will be consumed from its position to its limit.
     * <p>
     * Calling this method has the same effect as calling
     * {@link #writeBytes(OutputStream, ByteBuffer, int, BufferOperation)
     * writeBytes(ch, buf, buf.remaining(), BufferOperation.ADVANCE_POSITION)}
     * 
     * @param out
     *            The output stream used to write bytes
     * @param buf
     *            The buffer containing the bytes to be written
     * @throws IOWriteException
     *             If an I/O error occurs while writing
     */
    public static void writeBytes( OutputStream out, ByteBuffer buf ) throws IOWriteException
    {
        writeBytes(out, buf, buf.remaining(), BufferOperation.ADVANCE_POSITION);
    }

    /**
     * Writes to an output stream a specific number of bytes from a buffer.
     * <p>
     * Calling this method has the same effect as calling
     * {@link #writeBytes(OutputStream, ByteBuffer, int, BufferOperation)
     * writeBytes(ch, buf, numBytes, BufferOperation.ADVANCE_POSITION)}
     * 
     * @param out
     *            The output stream used to write bytes
     * @param buf
     *            The buffer containing the bytes to be written
     * @param numBytes
     *            The number of bytes to write
     * @throws IOWriteException
     *             If an I/O error occurs while writing
     * @exception IllegalArgumentException
     *                If {@code numBytes} is negative
     * @exception BufferUnderflowException
     *                If the provided buffer does not have at least
     *                {@code numBytes} bytes available to write
     */
    public static void writeBytes( OutputStream out, ByteBuffer buf, int numBytes ) throws IOWriteException
    {
        writeBytes(out, buf, numBytes, BufferOperation.ADVANCE_POSITION);
    }

    /**
     * Writes to an output stream all the available bytes from a buffer. The
     * buffer will be consumed from its position to its limit.
     * <p>
     * Calling this method has the same effect as calling
     * {@link #writeBytes(OutputStream, ByteBuffer, int, BufferOperation)
     * writeBytes(ch, buf, buf.remaining(), op)}
     * 
     * @param out
     *            The output stream used to write bytes
     * @param buf
     *            The buffer containing the bytes to be written
     * @param op
     *            The operation to apply to the provided buffer after writing
     * @throws IOWriteException
     *             If an I/O error occurs while writing
     */
    public static void writeBytes( OutputStream out, ByteBuffer buf, BufferOperation op ) throws IOWriteException
    {
        writeBytes(out, buf, buf.remaining(), op);
    }

    /**
     * Writes to an output stream a specific number of bytes from a buffer.
     * 
     * @param out
     *            The output stream used to write bytes
     * @param buf
     *            The buffer containing the bytes to be written
     * @param numBytes
     *            The number of bytes to write
     * @param op
     *            The operation to apply to the provided buffer after writing
     * @throws IOWriteException
     *             If an I/O error occurs while writing
     * @exception IllegalArgumentException
     *                If {@code numBytes} is negative
     * @exception BufferUnderflowException
     *                If the provided buffer does not have at least
     *                {@code numBytes} bytes available to write
     */
    public static void writeBytes( OutputStream out, ByteBuffer buf, int numBytes, BufferOperation op )
        throws IOWriteException
    {
        final int bufPos = buf.position();
        final int bufLim = buf.limit();
        final int remaining = bufLim - bufPos;

        if (numBytes < 0) throw new IllegalArgumentException("number of bytes is negative");
        if (remaining < numBytes) throw new BufferUnderflowException();
        Objects.requireNonNull(op);

        try {
            buf.limit(bufPos + numBytes);
            writeFully(out, buf);
        }
        finally {
            buf.limit(bufLim); // always restore the original limit
        }

        // only apply the operation if no exception was previously thrown
        op.apply(buf, bufPos, buf.position());
    }

    /**
     * Writes a single byte to an output stream.
     * 
     * @param out
     *            The output stream used to write a byte
     * @param b
     *            The byte value to be written
     * @throws IOWriteException
     *             If an I/O error occurs while writing
     */
    public static void writeByte( OutputStream out, byte b ) throws IOWriteException
    {
        _write(out, b);
    }

    /**
     * Writes a single character to an output stream.
     * 
     * @param out
     *            The output stream used to write a char
     * @param c
     *            The char value to be written
     * @param order
     *            The byte order in which to write the value
     * @throws IOWriteException
     *             If an I/O error occurs while writing
     */
    public static void writeChar( OutputStream out, char c, ByteOrder order ) throws IOWriteException
    {
        ByteBuffer buf = tinyBuffer(SizeOf.CHAR);
        ByteBuffers.putCharAtIndex(0, buf, c, order);
        writeFully(out, buf);
    }

    /**
     * Writes a single short integer to an output stream.
     * 
     * @param out
     *            The output stream used to write a short
     * @param s
     *            The short value to be written
     * @param order
     *            The byte order in which to write the value
     * @throws IOWriteException
     *             If an I/O error occurs while writing
     */
    public static void writeShort( OutputStream out, short s, ByteOrder order ) throws IOWriteException
    {
        ByteBuffer buf = tinyBuffer(SizeOf.SHORT);
        ByteBuffers.putShortAtIndex(0, buf, s, order);
        writeFully(out, buf);
    }

    /**
     * Writes a single 24-bit medium integer to an output stream.
     * 
     * @param out
     *            The output stream used to write a short
     * @param m
     *            The 24-bit medium value to be written
     * @param order
     *            The byte order in which to write the value
     * @throws IOWriteException
     *             If an I/O error occurs while writing
     */
    public static void writeMedium( OutputStream out, int m, ByteOrder order ) throws IOWriteException
    {
        ByteBuffer buf = tinyBuffer(SizeOf.MEDIUM);
        ByteBuffers.putMediumAtIndex(0, buf, m, order);
        writeFully(out, buf);
    }

    /**
     * Writes a single integer to an output stream.
     * 
     * @param out
     *            The output stream used to write an int
     * @param i
     *            The int value to be written
     * @param order
     *            The byte order in which to write the value
     * @throws IOWriteException
     *             If an I/O error occurs while writing
     */
    public static void writeInt( OutputStream out, int i, ByteOrder order ) throws IOWriteException
    {
        ByteBuffer buf = tinyBuffer(SizeOf.INT);
        ByteBuffers.putIntAtIndex(0, buf, i, order);
        writeFully(out, buf);
    }

    /**
     * Writes a single long integer to an output stream.
     * 
     * @param out
     *            The output stream used to write a long
     * @param eL
     *            The long value to be written
     * @param order
     *            The byte order in which to write the value
     * @throws IOWriteException
     *             If an I/O error occurs while writing
     */
    public static void writeLong( OutputStream out, long eL, ByteOrder order ) throws IOWriteException
    {
        ByteBuffer buf = tinyBuffer(SizeOf.LONG);
        ByteBuffers.putLongAtIndex(0, buf, eL, order);
        writeFully(out, buf);
    }

    /**
     * Writes a single float to an output stream.
     * 
     * @param out
     *            The output stream used to write a float
     * @param f
     *            The float value to be written
     * @param order
     *            The byte order in which to write the value
     * @throws IOWriteException
     *             If an I/O error occurs while writing
     */
    public static void writeFloat( OutputStream out, float f, ByteOrder order ) throws IOWriteException
    {
        ByteBuffer buf = tinyBuffer(SizeOf.FLOAT);
        ByteBuffers.putFloatAtIndex(0, buf, f, order);
        writeFully(out, buf);
    }

    /**
     * Writes a single double to an output stream.
     * 
     * @param out
     *            The output stream used to write a double
     * @param d
     *            The double value to be written
     * @param order
     *            The byte order in which to write the value
     * @throws IOWriteException
     *             If an I/O error occurs while writing
     */
    public static void writeDouble( OutputStream out, double d, ByteOrder order ) throws IOWriteException
    {
        ByteBuffer buf = tinyBuffer(SizeOf.DOUBLE);
        ByteBuffers.putDoubleAtIndex(0, buf, d, order);
        writeFully(out, buf);
    }

    /**
     * Reads into a buffer bytes from an input stream. The buffer will be filled
     * from its position to its limit.
     * <p>
     * Calling this method has the same effect as calling
     * {@link #readBytes(InputStream, ByteBuffer, int, BufferOperation)
     * readBytes(ch, buf, buf.remaining(), BufferOperation.ADVANCE_POSITION)}
     * 
     * @param in
     *            The input stream used to read bytes from
     * @param buf
     *            The buffer used to store the read bytes
     * @throws IOReadException
     *             If an I/O error occurs while reading
     */
    public static void readBytes( InputStream in, ByteBuffer buf ) throws IOReadException
    {
        readBytes(in, buf, buf.remaining(), BufferOperation.ADVANCE_POSITION);
    }

    /**
     * Reads into a buffer a specific number of bytes from an input stream.
     * <p>
     * Calling this method has the same effect as calling
     * {@link #readBytes(InputStream, ByteBuffer, int, BufferOperation)
     * readBytes(ch, buf, numBytes, BufferOperation.ADVANCE_POSITION)}
     * 
     * @param in
     *            The input stream used to read bytes from
     * @param buf
     *            The buffer used to store the read bytes
     * @param numBytes
     *            The number of bytes to read
     * @throws IOReadException
     *             If an I/O error occurs while reading
     * @exception IllegalArgumentException
     *                If {@code numBytes} is negative
     * @exception BufferOverflowException
     *                If the provided buffer does not have at least
     *                {@code numBytes} bytes available for storage
     */
    public static void readBytes( InputStream in, ByteBuffer buf, int numBytes ) throws IOReadException
    {
        readBytes(in, buf, numBytes, BufferOperation.ADVANCE_POSITION);
    }

    /**
     * Reads into a buffer bytes from an input stream. The buffer will be filled
     * from its position to its limit.
     * <p>
     * Calling this method has the same effect as calling
     * {@link #readBytes(InputStream, ByteBuffer, int, BufferOperation)
     * readBytes(ch, buf, buf.remaining(), op)}
     * 
     * @param in
     *            The input stream used to read bytes from
     * @param buf
     *            The buffer used to store the read bytes
     * @param op
     *            The operation to apply to the provided buffer after reading
     * @throws IOReadException
     *             If an I/O error occurs while reading
     */
    public static void readBytes( InputStream in, ByteBuffer buf, BufferOperation op ) throws IOReadException
    {
        readBytes(in, buf, buf.remaining(), op);
    }

    /**
     * Reads into a buffer a specific number of bytes from an input stream.
     * 
     * @param in
     *            The input stream used to read bytes from
     * @param buf
     *            The buffer used to store the read bytes
     * @param numBytes
     *            The number of bytes to read
     * @param op
     *            The operation to apply to the provided buffer after reading
     * @throws IOReadException
     *             If an I/O error occurs while reading
     * @exception IllegalArgumentException
     *                If {@code numBytes} is negative
     * @exception BufferOverflowException
     *                If the provided buffer does not have at least
     *                {@code numBytes} bytes available for storage
     */
    public static void readBytes( InputStream in, ByteBuffer buf, int numBytes, BufferOperation op )
        throws IOReadException
    {
        final int bufPos = buf.position();
        final int bufLim = buf.limit();
        final int remaining = bufLim - bufPos;

        if (numBytes < 0) throw new IllegalArgumentException("number of bytes is negative");
        if (remaining < numBytes) throw new BufferOverflowException();
        Objects.requireNonNull(op);

        try {
            buf.limit(bufPos + numBytes);
            readFully(in, buf);
        }
        finally {
            buf.limit(bufLim); // always restore the original limit
        }

        // only apply the operation if no exception was previously thrown
        op.apply(buf, bufPos, buf.position());
    }

    /**
     * Reads a single byte from an input stream.
     * 
     * @param in
     *            The input stream used to read the byte value
     * @return a byte value
     * @throws IOReadException
     *             If an I/O error occurs while reading
     */
    public static byte readByte( InputStream in ) throws IOReadException
    {
        int r = _read(in);
        if (r == -1)
            throw new IOReadException(new EOFException());
        return (byte)r;
    }

    /**
     * Reads a single character from an input stream.
     * 
     * @param in
     *            The input stream used to read the char value
     * @param order
     *            The byte order in which to write the value
     * @return a char value
     * @throws IOReadException
     *             If an I/O error occurs while reading
     */
    public static char readChar( InputStream in, ByteOrder order ) throws IOReadException
    {
        ByteBuffer buf = tinyBuffer(SizeOf.CHAR);
        readFully(in, buf);
        return ByteBuffers.getCharAtIndex(0, buf, order);
    }

    /**
     * Reads a single short integer from an input stream.
     * 
     * @param in
     *            The input stream used to read the short value
     * @param order
     *            The byte order in which to write the value
     * @return a short value
     * @throws IOReadException
     *             If an I/O error occurs while reading
     */
    public static short readShort( InputStream in, ByteOrder order ) throws IOReadException
    {
        ByteBuffer buf = tinyBuffer(SizeOf.SHORT);
        readFully(in, buf);
        return ByteBuffers.getShortAtIndex(0, buf, order);
    }

    /**
     * Reads a single 24-bit medium integer from an input stream.
     * 
     * @param in
     *            The input stream used to read the short value
     * @param order
     *            The byte order in which to write the value
     * @return a 24-bit medium value
     * @throws IOReadException
     *             If an I/O error occurs while reading
     */
    public static int readMedium( InputStream in, ByteOrder order ) throws IOReadException
    {
        ByteBuffer buf = tinyBuffer(SizeOf.MEDIUM);
        readFully(in, buf);
        return ByteBuffers.getMediumAtIndex(0, buf, order);
    }

    /**
     * Reads a single integer from an input stream.
     * 
     * @param in
     *            The input stream used to read the int value
     * @param order
     *            The byte order in which to write the value
     * @return an int value
     * @throws IOReadException
     *             If an I/O error occurs while reading
     */
    public static int readInt( InputStream in, ByteOrder order ) throws IOReadException
    {
        ByteBuffer buf = tinyBuffer(SizeOf.INT);
        readFully(in, buf);
        return ByteBuffers.getIntAtIndex(0, buf, order);
    }

    /**
     * Reads a single long integer from an input stream.
     * 
     * @param in
     *            The input stream used to read the long value
     * @param order
     *            The byte order in which to write the value
     * @return a long value
     * @throws IOReadException
     *             If an I/O error occurs while reading
     */
    public static long readLong( InputStream in, ByteOrder order ) throws IOReadException
    {
        ByteBuffer buf = tinyBuffer(SizeOf.LONG);
        readFully(in, buf);
        return ByteBuffers.getLongAtIndex(0, buf, order);
    }

    /**
     * Reads a single float from an input stream.
     * 
     * @param in
     *            The input stream used to read the float value
     * @param order
     *            The byte order in which to write the value
     * @return a float value
     * @throws IOReadException
     *             If an I/O error occurs while reading
     */
    public static float readFloat( InputStream in, ByteOrder order ) throws IOReadException
    {
        ByteBuffer buf = tinyBuffer(SizeOf.FLOAT);
        readFully(in, buf);
        return ByteBuffers.getFloatAtIndex(0, buf, order);
    }

    /**
     * Reads a single double from an input stream.
     * 
     * @param in
     *            The input stream used to read the double value
     * @param order
     *            The byte order in which to write the value
     * @return a double value
     * @throws IOReadException
     *             If an I/O error occurs while reading
     */
    public static double readDouble( InputStream in, ByteOrder order ) throws IOReadException
    {
        ByteBuffer buf = tinyBuffer(SizeOf.DOUBLE);
        readFully(in, buf);
        return ByteBuffers.getDoubleAtIndex(0, buf, order);
    }

    private static void writeFully( OutputStream out, ByteBuffer buf ) throws IOWriteException
    {
        if (buf.hasArray()) {
            writeFullyFromArrayBacked(out, buf);
            buf.position(buf.limit()); // artificially advance the position
        }
        else {
            final int tmpCap = 1024;
            final ByteBuffer tmp = ByteBuffers.getCached(tmpCap, BufferType.ARRAY_BACKED);

            while (buf.hasRemaining()) {
                final int batchSize = Math.min(tmpCap, buf.remaining());

                tmp.clear();
                ByteBuffers.copy(buf, tmp, batchSize); // position advances

                tmp.flip();
                writeFullyFromArrayBacked(out, tmp);
            }
        }
    }

    private static void writeFullyFromArrayBacked( OutputStream out, ByteBuffer buf ) throws IOWriteException
    {
        byte[] arr = buf.array();
        int off = buf.position() + buf.arrayOffset();
        int len = buf.remaining();
        _write(out, arr, off, len);
    }

    private static void readFully( InputStream in, ByteBuffer buf ) throws IOReadException
    {
        if (buf.hasArray()) {
            readFullyToArrayBacked(in, buf);
            buf.position(buf.limit()); // artificially advance the position
        }
        else {
            final int tmpCap = 1024;
            final ByteBuffer tmp = ByteBuffers.getCached(tmpCap, BufferType.ARRAY_BACKED);

            while (buf.hasRemaining()) {
                final int batchSize = Math.min(tmpCap, buf.remaining());

                tmp.clear().limit(batchSize);
                readFullyToArrayBacked(in, tmp);

                tmp.rewind();
                ByteBuffers.copy(tmp, buf); // buf's position advances
            }
        }
    }

    private static void readFullyToArrayBacked( InputStream in, ByteBuffer buf ) throws IOReadException
    {
        byte[] arr = buf.array();
        int off = buf.position() + buf.arrayOffset();
        int len = buf.remaining();
        while (len > 0) {
            int r = _read(in, arr, off, len);
            if (r == -1)
                throw new IOReadException(new EOFException());
            off += r;
            len -= r;
        }
    }

    private static void _write( OutputStream out, byte b ) throws IOWriteException
    {
        try {
            out.write(b);
        }
        catch (IOException e) {
            throw new IOWriteException(e);
        }
    }

    private static void _write( OutputStream out, byte[] arr, int off, int len ) throws IOWriteException
    {
        try {
            out.write(arr, off, len);
        }
        catch (IOException e) {
            throw new IOWriteException(e);
        }
    }

    private static int _read( InputStream in ) throws IOReadException
    {
        try {
            return in.read();
        }
        catch (IOException e) {
            throw new IOReadException(e);
        }
    }

    private static int _read( InputStream in, byte[] arr, int off, int len ) throws IOReadException
    {
        try {
            return in.read(arr, off, len);
        }
        catch (IOException e) {
            throw new IOReadException(e);
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
            return ByteBuffer.allocate(SizeOf.LONG);
        }
    };

    private static ByteBuffer tinyBuffer( int limit )
    {
        ByteBuffer buf = TINY_BUFFER_CACHE.get();
        buf.clear().limit(limit);
        return buf;
    }

    private IOStreams()
    {
        // not used
    }
}
