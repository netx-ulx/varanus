/*
 * Copyright 2014 OpenRQ Team
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.varanus.util.io;


import java.nio.Buffer;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;
import java.util.function.IntFunction;

import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.checking.Indexables;
import net.varanus.util.lang.SizeOf;


/**
 * Class containing utility methods for {@link ByteBuffer} objects.
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class ByteBuffers
{
    /**
     * A type of {@code ByteBuffer}.
     */
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static enum BufferType
    {
        /**
         * The type of a {@code ByteBuffer} obtained via the method
         * {@link ByteBuffer#allocate(int)} or the methods
         * {@link ByteBuffer#wrap(byte[])} or
         * {@link ByteBuffer#wrap(byte[], int, int)}.
         */
        ARRAY_BACKED,

        /**
         * The type of a {@code ByteBuffer} obtained via the method
         * {@link ByteBuffer#allocateDirect(int)}.
         */
        DIRECT;

        public static BufferType of( ByteBuffer buf )
        {
            return buf.isDirect() ? DIRECT : ARRAY_BACKED;
        }
    }

    /**
     * The maximum capacity of cached buffers ({@value}).
     */
    public static final int MAX_CACHED_BUFFER_CAPACITY = 65536;

    /**
     * Allocates a new byte buffer.
     * 
     * @param capacity
     *            The capacity of the new buffer
     * @param type
     *            The type of the returned buffer
     * @return a new allocated byte buffer
     */
    public static ByteBuffer allocate( int capacity, BufferType type )
    {
        switch (type) {
            case ARRAY_BACKED:
                return ByteBuffer.allocate(capacity);

            case DIRECT:
                return ByteBuffer.allocateDirect(capacity);

            default:
                throw new AssertionError("unexpected enum type");
        }
    }

    public static IntFunction<ByteBuffer> allocator( BufferType type )
    {
        switch (type) {
            case ARRAY_BACKED:
                return ByteBuffer::allocate;

            case DIRECT:
                return ByteBuffer::allocateDirect;

            default:
                throw new AssertionError("unexpected enum type");
        }
    }

    /**
     * Returns a cached (per thread) byte buffer. The returned buffer's position
     * will be zero and its limit will be the specified capacity.
     * <p>
     * The specified capacity must not exceed
     * {@link #MAX_CACHED_BUFFER_CAPACITY}.
     * 
     * @param capacity
     *            The capacity of the cached buffer
     * @param type
     *            The type of the returned buffer
     * @return a cached (per thread) byte buffer
     * @exception IllegalArgumentException
     *                If
     *                {@code minCapacity > }{@link #MAX_CACHED_BUFFER_CAPACITY}
     */
    public static ByteBuffer getCached( int capacity, BufferType type )
    {
        Objects.requireNonNull(type);
        return _cachedBuffer(capacity, type);
    }

    public static IntFunction<ByteBuffer> cachedSupplier( BufferType type )
    {
        Objects.requireNonNull(type);
        return cap -> getCached(cap, type);
    }

    public static ByteBuffer putByte( ByteBuffer buf, byte b )
    {
        return buf.put(b);
    }

    public static ByteBuffer putByteAtIndex( int index, ByteBuffer buf, byte b )
    {
        return buf.put(index, b);
    }

    public static byte getByte( ByteBuffer buf )
    {
        return buf.get();
    }

    public static byte getByteAtIndex( int index, ByteBuffer buf )
    {
        return buf.get(index);
    }

    public static ByteBuffer putChar( ByteBuffer buf, char c, ByteOrder order )
    {
        Objects.requireNonNull(order);
        ByteOrder tmpOrder = buf.order();
        try {
            return buf.order(order).putChar(c);
        }
        finally {
            // always restore the original byte order
            buf.order(tmpOrder);
        }
    }

    public static ByteBuffer putCharAtIndex( int index, ByteBuffer buf, char c, ByteOrder order )
    {
        Objects.requireNonNull(order);
        ByteOrder tmpOrder = buf.order();
        try {
            return buf.order(order).putChar(index, c);
        }
        finally {
            // always restore the original byte order
            buf.order(tmpOrder);
        }
    }

    public static char getChar( ByteBuffer buf, ByteOrder order )
    {
        Objects.requireNonNull(order);
        ByteOrder tmpOrder = buf.order();
        try {
            return buf.order(order).getChar();
        }
        finally {
            // always restore the original byte order
            buf.order(tmpOrder);
        }
    }

    public static char getCharAtIndex( int index, ByteBuffer buf, ByteOrder order )
    {
        Objects.requireNonNull(order);
        ByteOrder tmpOrder = buf.order();
        try {
            return buf.order(order).getChar(index);
        }
        finally {
            // always restore the original byte order
            buf.order(tmpOrder);
        }
    }

    public static ByteBuffer putShort( ByteBuffer buf, short s, ByteOrder order )
    {
        Objects.requireNonNull(order);
        ByteOrder tmpOrder = buf.order();
        try {
            return buf.order(order).putShort(s);
        }
        finally {
            // always restore the original byte order
            buf.order(tmpOrder);
        }
    }

    public static ByteBuffer putShortAtIndex( int index, ByteBuffer buf, short s, ByteOrder order )
    {
        Objects.requireNonNull(order);
        ByteOrder tmpOrder = buf.order();
        try {
            return buf.order(order).putShort(index, s);
        }
        finally {
            // always restore the original byte order
            buf.order(tmpOrder);
        }
    }

    public static short getShort( ByteBuffer buf, ByteOrder order )
    {
        Objects.requireNonNull(order);
        ByteOrder tmpOrder = buf.order();
        try {
            return buf.order(order).getShort();
        }
        finally {
            // always restore the original byte order
            buf.order(tmpOrder);
        }
    }

    public static short getShortAtIndex( int index, ByteBuffer buf, ByteOrder order )
    {
        Objects.requireNonNull(order);
        ByteOrder tmpOrder = buf.order();
        try {
            return buf.order(order).getShort(index);
        }
        finally {
            // always restore the original byte order
            buf.order(tmpOrder);
        }
    }

    public static ByteBuffer putMedium( ByteBuffer buf, int m, ByteOrder order )
    {
        Objects.requireNonNull(order);
        if (buf.remaining() < SizeOf.MEDIUM)
            throw new BufferOverflowException();

        if (order.equals(ByteOrder.BIG_ENDIAN)) {
            return buf.put(int2(m)).put(int1(m)).put(int0(m));
        }
        else if (order.equals(ByteOrder.LITTLE_ENDIAN)) {
            return buf.put(int0(m)).put(int1(m)).put(int2(m));
        }
        else {
            throw new AssertionError("unexpected byte order value");
        }
    }

    public static ByteBuffer putMediumAtIndex( int index, ByteBuffer buf, int m, ByteOrder order )
    {
        Objects.requireNonNull(order);
        if ((buf.limit() - index) < SizeOf.MEDIUM)
            throw new BufferOverflowException();

        if (order.equals(ByteOrder.BIG_ENDIAN)) {
            return buf.put(index, int2(m)).put(index + 1, int1(m)).put(index + 2, int0(m));
        }
        else if (order.equals(ByteOrder.LITTLE_ENDIAN)) {
            return buf.put(index, int0(m)).put(index + 1, int1(m)).put(index + 2, int2(m));
        }
        else {
            throw new AssertionError("unexpected byte order value");
        }
    }

    public static int getMedium( ByteBuffer buf, ByteOrder order )
    {
        Objects.requireNonNull(order);
        if (buf.remaining() < SizeOf.MEDIUM)
            throw new BufferUnderflowException();

        if (order.equals(ByteOrder.BIG_ENDIAN)) {
            return makeIntB(buf.get(), buf.get(), buf.get());
        }
        else if (order.equals(ByteOrder.LITTLE_ENDIAN)) {
            return makeIntL(buf.get(), buf.get(), buf.get());
        }
        else {
            throw new AssertionError("unexpected byte order value");
        }
    }

    public static int getMediumAtIndex( int index, ByteBuffer buf, ByteOrder order )
    {
        Objects.requireNonNull(order);
        if ((buf.limit() - index) < SizeOf.MEDIUM)
            throw new BufferUnderflowException();

        if (order.equals(ByteOrder.BIG_ENDIAN)) {
            return makeIntB(buf.get(index), buf.get(index + 1), buf.get(index + 2));
        }
        else if (order.equals(ByteOrder.LITTLE_ENDIAN)) {
            return makeIntL(buf.get(index), buf.get(index + 1), buf.get(index + 2));
        }
        else {
            throw new AssertionError("unexpected byte order value");
        }
    }

    // copied and adapted from java.nio.Bits
    //@formatter:off
    private static byte int2(int x) { return (byte)(x >> 16); }
    private static byte int1(int x) { return (byte)(x >>  8); }
    private static byte int0(int x) { return (byte)(x      ); }
    
    private static int makeIntB(byte b2, byte b1, byte b0) {
        return (((b2 & 0xff) << 16) |
                ((b1 & 0xff) <<  8) |
                ((b0 & 0xff)      ));
    }
    
    private static int makeIntL(byte b0, byte b1, byte b2) {
        return (((b2 & 0xff) << 16) |
                ((b1 & 0xff) <<  8) |
                ((b0 & 0xff)      ));
    }
    //@formatter:on

    public static ByteBuffer putInt( ByteBuffer buf, int i, ByteOrder order )
    {
        Objects.requireNonNull(order);
        ByteOrder tmpOrder = buf.order();
        try {
            return buf.order(order).putInt(i);
        }
        finally {
            // always restore the original byte order
            buf.order(tmpOrder);
        }
    }

    public static ByteBuffer putIntAtIndex( int index, ByteBuffer buf, int i, ByteOrder order )
    {
        Objects.requireNonNull(order);
        ByteOrder tmpOrder = buf.order();
        try {
            return buf.order(order).putInt(index, i);
        }
        finally {
            // always restore the original byte order
            buf.order(tmpOrder);
        }
    }

    public static int getInt( ByteBuffer buf, ByteOrder order )
    {
        Objects.requireNonNull(order);
        ByteOrder tmpOrder = buf.order();
        try {
            return buf.order(order).getInt();
        }
        finally {
            // always restore the original byte order
            buf.order(tmpOrder);
        }
    }

    public static int getIntAtIndex( int index, ByteBuffer buf, ByteOrder order )
    {
        Objects.requireNonNull(order);
        ByteOrder tmpOrder = buf.order();
        try {
            return buf.order(order).getInt(index);
        }
        finally {
            // always restore the original byte order
            buf.order(tmpOrder);
        }
    }

    public static ByteBuffer putLong( ByteBuffer buf, long eL, ByteOrder order )
    {
        Objects.requireNonNull(order);
        ByteOrder tmpOrder = buf.order();
        try {
            return buf.order(order).putLong(eL);
        }
        finally {
            // always restore the original byte order
            buf.order(tmpOrder);
        }
    }

    public static ByteBuffer putLongAtIndex( int index, ByteBuffer buf, long eL, ByteOrder order )
    {
        Objects.requireNonNull(order);
        ByteOrder tmpOrder = buf.order();
        try {
            return buf.order(order).putLong(index, eL);
        }
        finally {
            // always restore the original byte order
            buf.order(tmpOrder);
        }
    }

    public static long getLong( ByteBuffer buf, ByteOrder order )
    {
        Objects.requireNonNull(order);
        ByteOrder tmpOrder = buf.order();
        try {
            return buf.order(order).getLong();
        }
        finally {
            // always restore the original byte order
            buf.order(tmpOrder);
        }
    }

    public static long getLongAtIndex( int index, ByteBuffer buf, ByteOrder order )
    {
        Objects.requireNonNull(order);
        ByteOrder tmpOrder = buf.order();
        try {
            return buf.order(order).getLong(index);
        }
        finally {
            // always restore the original byte order
            buf.order(tmpOrder);
        }
    }

    public static ByteBuffer putFloat( ByteBuffer buf, float f, ByteOrder order )
    {
        Objects.requireNonNull(order);
        ByteOrder tmpOrder = buf.order();
        try {
            return buf.order(order).putFloat(f);
        }
        finally {
            // always restore the original byte order
            buf.order(tmpOrder);
        }
    }

    public static ByteBuffer putFloatAtIndex( int index, ByteBuffer buf, float f, ByteOrder order )
    {
        Objects.requireNonNull(order);
        ByteOrder tmpOrder = buf.order();
        try {
            return buf.order(order).putFloat(index, f);
        }
        finally {
            // always restore the original byte order
            buf.order(tmpOrder);
        }
    }

    public static float getFloat( ByteBuffer buf, ByteOrder order )
    {
        Objects.requireNonNull(order);
        ByteOrder tmpOrder = buf.order();
        try {
            return buf.order(order).getFloat();
        }
        finally {
            // always restore the original byte order
            buf.order(tmpOrder);
        }
    }

    public static float getFloatAtIndex( int index, ByteBuffer buf, ByteOrder order )
    {
        Objects.requireNonNull(order);
        ByteOrder tmpOrder = buf.order();
        try {
            return buf.order(order).getFloat(index);
        }
        finally {
            // always restore the original byte order
            buf.order(tmpOrder);
        }
    }

    public static ByteBuffer putDouble( ByteBuffer buf, double d, ByteOrder order )
    {
        Objects.requireNonNull(order);
        ByteOrder tmpOrder = buf.order();
        try {
            return buf.order(order).putDouble(d);
        }
        finally {
            // always restore the original byte order
            buf.order(tmpOrder);
        }
    }

    public static ByteBuffer putDoubleAtIndex( int index, ByteBuffer buf, double d, ByteOrder order )
    {
        Objects.requireNonNull(order);
        ByteOrder tmpOrder = buf.order();
        try {
            return buf.order(order).putDouble(index, d);
        }
        finally {
            // always restore the original byte order
            buf.order(tmpOrder);
        }
    }

    public static double getDouble( ByteBuffer buf, ByteOrder order )
    {
        Objects.requireNonNull(order);
        ByteOrder tmpOrder = buf.order();
        try {
            return buf.order(order).getDouble();
        }
        finally {
            // always restore the original byte order
            buf.order(tmpOrder);
        }
    }

    public static double getDoubleAtIndex( int index, ByteBuffer buf, ByteOrder order )
    {
        Objects.requireNonNull(order);
        ByteOrder tmpOrder = buf.order();
        try {
            return buf.order(order).getDouble(index);
        }
        finally {
            // always restore the original byte order
            buf.order(tmpOrder);
        }
    }

    /**
     * Returns a {@linkplain ByteBuffer#slice() slice} of a byte buffer.
     * <p>
     * Calling this method has the same effect as calling
     * {@link #getSlice(ByteBuffer, int, BufferOperation) slice(buf,
     * buf.remaining(), BufferOperation.RESTORE_POSITION)}.
     * 
     * @param buf
     *            A byte buffer
     * @return a slice of a byte buffer
     */
    public static ByteBuffer getSlice( ByteBuffer buf )
    {
        return getSlice(buf, buf.remaining(), BufferOperation.RESTORE_POSITION);
    }

    /**
     * Returns a {@linkplain ByteBuffer#slice() slice} of a byte buffer.
     * <p>
     * Calling this method has the same effect as calling
     * {@link #getSlice(ByteBuffer, int, BufferOperation) slice(buf,
     * sliceSize, BufferOperation.RESTORE_POSITION)}.
     * 
     * @param buf
     *            A byte buffer
     * @param sliceSize
     *            The size of the slice
     * @return a slice of a byte buffer
     * @exception IllegalArgumentException
     *                If the size of the slice is negative
     * @exception BufferUnderflowException
     *                If the size of the slice exceeds the number of
     *                {@linkplain Buffer#remaining() available} bytes in
     *                the buffer
     */
    public static ByteBuffer getSlice( ByteBuffer buf, int sliceSize )
    {
        return getSlice(buf, sliceSize, BufferOperation.RESTORE_POSITION);
    }

    /**
     * Returns a {@linkplain ByteBuffer#slice() slice} of a byte buffer.
     * <p>
     * Calling this method has the same effect as calling
     * {@link #getSlice(ByteBuffer, int, BufferOperation) slice(buf,
     * buf.remaining(), op)}.
     * 
     * @param buf
     *            A byte buffer
     * @param op
     *            The operation to apply to the buffer after being sliced
     * @return a slice of a byte buffer
     */
    public static ByteBuffer getSlice( ByteBuffer buf, BufferOperation op )
    {
        return getSlice(buf, buf.remaining(), op);
    }

    /**
     * Returns a {@linkplain ByteBuffer#slice() slice} of a byte buffer. The
     * contents of the slice will begin at the buffer's current position and
     * will have the specified size in number of bytes.
     * <p>
     * <em>The buffer's new position after being sliced will be equal to its
     * original position plus the size of the slice</em> (before the provided
     * buffer operation is applied to the buffer).
     * 
     * @param buf
     *            A byte buffer
     * @param sliceSize
     *            The size of the slice
     * @param op
     *            The operation to apply to the buffer after being sliced
     * @return a slice of a byte buffer
     * @exception IllegalArgumentException
     *                If the size of the slice is negative
     * @exception BufferUnderflowException
     *                If the size of the slice exceeds the number of
     *                {@linkplain Buffer#remaining() available} bytes in
     *                the buffer
     */
    public static ByteBuffer getSlice( ByteBuffer buf, int sliceSize, BufferOperation op )
    {
        Objects.requireNonNull(op);

        final int pos = buf.position();
        final int lim = buf.limit();
        final int remaining = lim - pos;

        if (sliceSize < 0) throw new IllegalArgumentException("slice size is negative");
        if (remaining < sliceSize) throw new BufferUnderflowException();
        Objects.requireNonNull(op);

        final ByteBuffer slice;
        try {
            buf.limit(pos + sliceSize);
            slice = buf.slice();
        }
        finally {
            buf.limit(lim); // always restore the original limit
        }

        // only apply the operation if no exception was previously thrown
        op.apply(buf, pos, pos + sliceSize);
        return slice;
    }

    public static ByteBuffer getSliceAtIndex( int index, ByteBuffer buf )
    {
        final int pos = buf.position();
        try {
            buf.position(index);
            return getSlice(buf);
        }
        finally {
            buf.position(pos); // always restore the original position
        }
    }

    public static ByteBuffer getSliceAtIndex( int index, ByteBuffer buf, int size )
    {
        final int pos = buf.position();
        try {
            buf.position(index);
            return getSlice(buf, size);
        }
        finally {
            buf.position(pos); // always restore the original position
        }
    }

    /**
     * Copies the contents of a byte buffer to a newly created one.
     * <p>
     * Calling this method has the same effect as calling
     * {@link #getCopy(ByteBuffer, int, BufferOperation)
     * copyBuffer(buf, buf.remaining(), BufferOperation.ADVANCE_POSITION)}.
     * 
     * @param buf
     *            A byte buffer
     * @return a copy of a byte buffer
     */
    public static ByteBuffer getCopy( ByteBuffer buf )
    {
        return getCopy(buf, buf.remaining(), BufferOperation.ADVANCE_POSITION);
    }

    /**
     * Copies the contents of a byte buffer to a newly created one.
     * <p>
     * Calling this method has the same effect as calling
     * {@link #getCopy(ByteBuffer, int, BufferOperation)
     * copyBuffer(buf, copySize, BufferOperation.ADVANCE_POSITION)}.
     * 
     * @param buf
     *            A byte buffer
     * @param copySize
     *            The number of bytes to be copied from the buffer
     * @return a copy of a byte buffer
     * @exception IllegalArgumentException
     *                If the number of bytes to be copied is negative
     * @exception BufferUnderflowException
     *                If the number of bytes to be copied exceeds the number of
     *                {@linkplain Buffer#remaining()
     *                available} bytes in the buffer
     */
    public static ByteBuffer getCopy( ByteBuffer buf, int copySize )
    {
        return getCopy(buf, copySize, BufferOperation.ADVANCE_POSITION);
    }

    /**
     * Copies the contents of a byte buffer to a newly created one.
     * <p>
     * Calling this method has the same effect as calling
     * {@link #getCopy(ByteBuffer, int, BufferOperation)
     * copyBuffer(buf, buf.remaining(), op)}.
     * 
     * @param buf
     *            A byte buffer
     * @param op
     *            The operation to apply to the buffer after being copied
     * @return a copy of a byte buffer
     */
    public static ByteBuffer getCopy( ByteBuffer buf, BufferOperation op )
    {
        return getCopy(buf, buf.remaining(), op);
    }

    /**
     * Copies the contents of a byte buffer to a newly created one. The copy
     * will begin at the buffer's current position and will have the specified
     * size in number of bytes.
     * <p>
     * <em>The buffer's new position after being copied will be equal to its
     * original position plus the size of the copy</em> (before the provided
     * buffer operation is applied to the buffer).
     * <p>
     * The copy will have the same {@linkplain BufferType type} and
     * {@linkplain ByteBuffer#order() byte order} of the buffer.
     * 
     * @param buf
     *            A byte buffer
     * @param copySize
     *            The number of bytes to be copied from the buffer
     * @param op
     *            The operation to apply to the buffer after being copied
     * @return a copy of a byte buffer
     * @exception IllegalArgumentException
     *                If the number of bytes to be copied is negative
     * @exception BufferUnderflowException
     *                If the number of bytes to be copied exceeds the number of
     *                {@linkplain Buffer#remaining() available} bytes in the
     *                buffer
     */
    public static ByteBuffer getCopy( ByteBuffer buf, int copySize, BufferOperation op )
    {
        Objects.requireNonNull(op);

        final int srcPos = buf.position();
        final int srcLim = buf.limit();
        final int remaining = srcLim - srcPos;

        if (copySize < 0) throw new IllegalArgumentException("number of bytes to copy is negative");
        if (remaining < copySize) throw new BufferUnderflowException();
        Objects.requireNonNull(op);

        final ByteBuffer copy = allocate(copySize, BufferType.of(buf)).order(buf.order());
        try {
            buf.limit(srcPos + copySize);
            copy.put(buf);
            copy.rewind();
        }
        finally {
            buf.limit(srcLim); // always restore the original limit
        }

        // only apply the operation if no exception was previously thrown
        op.apply(buf, srcPos, buf.position());
        return copy;
    }

    /**
     * Copies an array of bytes to a newly created byte buffer.
     * <p>
     * Calling this method has the same effect as calling
     * {@link #getCopy(byte[], int, int, BufferType) getCopy(array, 0,
     * array.length, type)}.
     * 
     * @param array
     *            An array of bytes
     * @param type
     *            The type of the returned buffer
     * @return a byte buffer containing a copy of an array of bytes
     */
    public static ByteBuffer getCopy( byte[] array, BufferType type )
    {
        return getCopy(array, 0, array.length, type);
    }

    /**
     * Copies an array of bytes to a newly created byte buffer. The copy will
     * begin at the specified array position and will have the specified size in
     * number of bytes.
     * 
     * @param array
     *            An array of bytes
     * @param off
     *            The starting position of the copy
     * @param len
     *            The number of bytes to copy
     * @param type
     *            The type of the returned buffer
     * @return a byte buffer containing a copy of an array of bytes
     */
    public static ByteBuffer getCopy( byte[] array, int off, int len, BufferType type )
    {
        Indexables.checkOffsetLengthBounds(off, len, array.length);

        ByteBuffer copy = allocate(len, type);
        copy.put(array, off, len);
        copy.rewind();

        return copy;
    }

    public static byte[] getArrayCopy( ByteBuffer buf )
    {
        return getArrayCopy(buf, buf.remaining(), BufferOperation.ADVANCE_POSITION);
    }

    public static byte[] getArrayCopy( ByteBuffer buf, int copySize )
    {
        return getArrayCopy(buf, copySize, BufferOperation.ADVANCE_POSITION);
    }

    public static byte[] getArrayCopy( ByteBuffer buf, BufferOperation op )
    {
        return getArrayCopy(buf, buf.remaining(), op);
    }

    public static byte[] getArrayCopy( ByteBuffer buf, int copySize, BufferOperation op )
    {
        Objects.requireNonNull(op);

        final int srcPos = buf.position();
        final int srcLim = buf.limit();
        final int remaining = srcLim - srcPos;

        if (copySize < 0) throw new IllegalArgumentException("number of bytes to copy is negative");
        if (remaining < copySize) throw new BufferUnderflowException();
        Objects.requireNonNull(op);

        final ByteBuffer copy = allocate(copySize, BufferType.ARRAY_BACKED);
        try {
            buf.limit(srcPos + copySize);
            copy.put(buf);
        }
        finally {
            buf.limit(srcLim); // always restore the original limit
        }

        // only apply the operation if no exception was previously thrown
        op.apply(buf, srcPos, buf.position());
        return copy.array();
    }

    /**
     * Copies the contents of a source buffer into a destination buffer. The
     * number of bytes to copy is the number of {@linkplain Buffer#remaining()
     * available} bytes in the source buffer.
     * <p>
     * Calling this method has the same effect as calling
     * {@link #copy(ByteBuffer, BufferOperation, ByteBuffer, BufferOperation, int)
     * copyBuffer(src, BufferOperation.ADVANCE_POSITION, dst,
     * BufferOperation.ADVANCE_POSITION, src.remaining())}.
     * 
     * @param src
     *            The source buffer
     * @param dst
     *            The destination buffer
     * @exception BufferOverflowException
     *                If the number of {@linkplain Buffer#remaining() available}
     *                bytes in the source buffer exceeds the number of available
     *                bytes in the destination buffer
     */
    public static void copy( ByteBuffer src, ByteBuffer dst )
    {
        copy(src, BufferOperation.ADVANCE_POSITION, dst, BufferOperation.ADVANCE_POSITION, src.remaining());
    }

    /**
     * Copies the contents of a source buffer into a destination buffer. The
     * number of bytes to copy is the number of {@linkplain Buffer#remaining()
     * available} bytes in the source buffer.
     * <p>
     * Calling this method has the same effect as calling
     * {@link #copy(ByteBuffer, BufferOperation, ByteBuffer, BufferOperation, int)
     * copyBuffer(src, srcOp, dst, dstOp, src.remaining())}.
     * 
     * @param src
     *            The source buffer
     * @param srcOp
     *            The operation to apply to the source buffer after the copy
     * @param dst
     *            The destination buffer
     * @param dstOp
     *            The operation to apply to the destination buffer after the
     *            copy
     * @exception BufferOverflowException
     *                If the number of {@linkplain Buffer#remaining() available}
     *                bytes in the source buffer exceeds the number of available
     *                bytes in the destination buffer
     */
    public static void copy( ByteBuffer src, BufferOperation srcOp, ByteBuffer dst, BufferOperation dstOp )
    {
        copy(src, srcOp, dst, dstOp, src.remaining());
    }

    /**
     * Copies the contents of a source buffer into a destination buffer.
     * <p>
     * Calling this method has the same effect as calling
     * {@link #copy(ByteBuffer, BufferOperation, ByteBuffer, BufferOperation, int)
     * copyBuffer(src, BufferOperation.ADVANCE_POSITION, dst,
     * BufferOperation.ADVANCE_POSITION, copySize)}.
     * 
     * @param src
     *            The source buffer
     * @param dst
     *            The destination buffer
     * @param copySize
     *            The number of bytes to copy
     * @exception IllegalArgumentException
     *                If the number of bytes to copy is negative
     * @exception BufferUnderflowException
     *                If the number of bytes to copy exceeds the number of
     *                {@linkplain Buffer#remaining() available} bytes in the
     *                source buffer
     * @exception BufferOverflowException
     *                If the number of bytes to copy exceeds the number of
     *                {@linkplain Buffer#remaining() available} bytes in the
     *                destination buffer
     */
    public static void copy( ByteBuffer src, ByteBuffer dst, int copySize )
    {
        copy(src, BufferOperation.ADVANCE_POSITION, dst, BufferOperation.ADVANCE_POSITION, copySize);
    }

    /**
     * Copies the contents of a source buffer into a destination buffer.
     * <p>
     * The number of bytes to be copied is specified as a parameter. <em>At the
     * end of the copy, the positions of the source and destination buffers will
     * have advanced the specified number of bytes</em> (before the provided
     * buffer operations are applied to each buffer).
     * 
     * @param src
     *            The source buffer
     * @param srcOp
     *            The operation to apply to the source buffer after the copy
     * @param dst
     *            The destination buffer
     * @param dstOp
     *            The operation to apply to the destination buffer after the
     *            copy
     * @param copySize
     *            The number of bytes to copy
     * @exception IllegalArgumentException
     *                If the number of bytes to copy is negative
     * @exception BufferUnderflowException
     *                If the number of bytes to copy exceeds the number of
     *                {@linkplain Buffer#remaining() available} bytes in the
     *                source buffer
     * @exception BufferOverflowException
     *                If the number of bytes to copy exceeds the number of
     *                {@linkplain Buffer#remaining() available} bytes in the
     *                destination buffer
     */
    public static void copy( ByteBuffer src,
                             BufferOperation srcOp,
                             ByteBuffer dst,
                             BufferOperation dstOp,
                             int copySize )
    {
        Objects.requireNonNull(srcOp);
        Objects.requireNonNull(dstOp);

        final int srcPos = src.position();
        final int srcLim = src.limit();
        final int srcRemaining = srcLim - srcPos;

        final int dstPos = dst.position();
        final int dstLim = dst.limit();
        final int dstRemaining = dstLim - dstPos;

        if (copySize < 0) throw new IllegalArgumentException("number of bytes to copy is negative");
        if (srcRemaining < copySize) throw new BufferUnderflowException();
        if (dstRemaining < copySize) throw new BufferOverflowException();
        Objects.requireNonNull(srcOp);
        Objects.requireNonNull(dstOp);

        try {
            src.limit(srcPos + copySize);
            dst.limit(dstPos + copySize);
            dst.put(src);
        }
        finally {
            // always restore the original limits
            src.limit(srcLim);
            dst.limit(dstLim);
        }

        // only apply the operations if no exception was previously thrown
        srcOp.apply(src, srcPos, src.position());
        dstOp.apply(dst, dstPos, dst.position());
    }

    /**
     * Puts zeros in the provided buffer, starting at the current buffer
     * position.
     * <p>
     * Calling this method has the same effect as calling
     * {@link #putZeros(ByteBuffer, int, BufferOperation)
     * putZeros(dst, dst.remaining(), BufferOperation.ADVANCE_POSITION)}.
     * 
     * @param dst
     *            The buffer to put zeros into
     */
    public static void putZeros( ByteBuffer dst )
    {
        putZeros(dst, dst.remaining(), BufferOperation.ADVANCE_POSITION);
    }

    /**
     * Puts zeros in the provided buffer, starting at the current buffer
     * position.
     * <p>
     * Calling this method has the same effect as calling
     * {@link #putZeros(ByteBuffer, int, BufferOperation)
     * putZeros(dst, numZeros, BufferOperation.ADVANCE_POSITION)}.
     * 
     * @param dst
     *            The buffer to put zeros into
     * @param numZeros
     *            The number of zeros to put
     * @exception IllegalArgumentException
     *                If the number of zeros is negative
     * @exception BufferOverflowException
     *                If the number of zeros exceeds the number of
     *                {@linkplain Buffer#remaining() available} bytes in
     *                the buffer
     */
    public static void putZeros( ByteBuffer dst, int numZeros )
    {
        putZeros(dst, numZeros, BufferOperation.ADVANCE_POSITION);
    }

    /**
     * Puts zeros in the provided buffer, starting at the current buffer
     * position.
     * <p>
     * Calling this method has the same effect as calling
     * {@link #putZeros(ByteBuffer, int, BufferOperation)
     * putZeros(dst, dst.remaining(), op)}.
     * 
     * @param dst
     *            The buffer to put zeros into
     * @param op
     *            The operation to apply to the buffer after the put
     */
    public static void putZeros( ByteBuffer dst, BufferOperation op )
    {
        putZeros(dst, dst.remaining(), op);
    }

    /**
     * Puts zeros in the provided buffer, starting at the current buffer
     * position.
     * <p>
     * <em>At the end of the put, the position of the buffer will have advanced
     * the specified number of zeros</em> (before the provided buffer operation
     * is applied to the buffer).
     * 
     * @param dst
     *            The buffer to put zeros into
     * @param numZeros
     *            The number of zeros to put
     * @param op
     *            The operation to apply to the buffer after the put
     * @exception IllegalArgumentException
     *                If the number of zeros is negative
     * @exception BufferOverflowException
     *                If the number of zeros exceeds the number of
     *                {@linkplain Buffer#remaining() available} bytes in
     *                the buffer
     */
    public static void putZeros( ByteBuffer dst, int numZeros, BufferOperation op )
    {
        Objects.requireNonNull(op);

        final int pos = dst.position();
        final int lim = dst.limit();
        final int remaining = lim - pos;

        if (numZeros < 0) throw new IllegalArgumentException("number of zeros is negative");
        if (remaining < numZeros) throw new BufferOverflowException();
        Objects.requireNonNull(op);

        _putZeros(dst, numZeros);
        op.apply(dst, pos, dst.position());
    }

    /**
     * Puts zeros in the provided buffer, starting at the specified absolute
     * buffer offset.
     * 
     * @param index
     *            An absolute buffer index
     * @param dst
     *            The buffer to put zeros into
     * @param numZeros
     *            The number of zeros to put
     * @exception IllegalArgumentException
     *                If the number of zeros is negative
     * @exception IndexOutOfBoundsException
     *                If the offset is negative or not smaller than the buffer's
     *                limit
     * @exception BufferOverflowException
     *                If the number of zeros exceeds the number of available
     *                bytes between the specified offset and the buffer's limit
     */
    public static void putZerosAtIndex( int index, ByteBuffer dst, int numZeros )
    {
        if (numZeros < 0) throw new IllegalArgumentException("number of zeros is negative");

        final ByteBuffer offsetDst = (ByteBuffer)dst.duplicate().position(index);
        if (offsetDst.remaining() < numZeros) throw new BufferOverflowException();

        _putZeros(offsetDst, numZeros);
    }

    private static void _putZeros( ByteBuffer dst, final int numZeros )
    {
        int remZeros = numZeros;
        while (remZeros > 0) {
            final int amount = Math.min(remZeros, ZERO_BUFFER_CAPACITY);
            dst.put(_zeroBuffer(amount, BufferType.of(dst)));
            remZeros -= amount;
        }
    }

    /**
     * Converts the contents of the provided buffer to a string in hexadecimal
     * byte format.
     * <p>
     * Calling this method has the same effect as calling
     * {@link #toHexString(ByteBuffer, int, BufferOperation)
     * toHexString(buf, buf.remaining(), BufferOperation.RESTORE_POSITION)}.
     * 
     * @param buf
     *            A byte buffer
     * @return a string containing the contents of the provided buffer in
     *         hexadecimal byte format
     */
    public static String toHexString( ByteBuffer buf )
    {
        return toHexString(buf, buf.remaining(), BufferOperation.RESTORE_POSITION);
    }

    /**
     * Converts the contents of the provided buffer to a string in hexadecimal
     * byte format.
     * <p>
     * Calling this method has the same effect as calling
     * {@link #toHexString(ByteBuffer, int, BufferOperation)
     * toHexString(buf, numBytes, BufferOperation.RESTORE_POSITION)}.
     * 
     * @param buf
     *            A byte buffer
     * @param numBytes
     *            The number of bytes to convert to a string
     * @return a string containing the contents of the provided buffer in
     *         hexadecimal byte format
     * @exception IllegalArgumentException
     *                If the number of bytes to convert is negative
     * @exception BufferUnderflowException
     *                If the number of bytes to convert exceeds the number of
     *                {@linkplain Buffer#remaining() available} bytes in the
     *                buffer
     */
    public static String toHexString( ByteBuffer buf, int numBytes )
    {
        return toHexString(buf, numBytes, BufferOperation.RESTORE_POSITION);
    }

    /**
     * Converts the contents of the provided buffer to a string in hexadecimal
     * byte format.
     * <p>
     * Calling this method has the same effect as calling
     * {@link #toHexString(ByteBuffer, int, BufferOperation)
     * toHexString(buf, buf.remaining(), op)}.
     * 
     * @param buf
     *            A byte buffer
     * @param op
     *            The operation to apply to the buffer after being converted to
     *            a string
     * @return a string containing the contents of the provided buffer in
     *         hexadecimal byte format
     */
    public static String toHexString( ByteBuffer buf, BufferOperation op )
    {
        return toHexString(buf, buf.remaining(), op);
    }

    /**
     * Converts the contents of the provided buffer to a string in hexadecimal
     * byte format. The contents of the string will begin at the buffer's
     * current position and will have the specified size in number of bytes.
     * <p>
     * <em>The buffer's new position after being converted to a string will be
     * equal to its original position plus the number of bytes to convert</em>
     * (before the provided buffer operation is applied to the buffer).
     * <p>
     * Note that there is a maximum possible number of bytes to convert
     * ({@code Integer.MAX_VALUE / 2}) due to restrictions in the length of a
     * string.
     * 
     * @param buf
     *            A byte buffer
     * @param numBytes
     *            The number of bytes to convert to a string
     * @param op
     *            The operation to apply to the buffer after being converted to
     *            a string
     * @return a string containing the contents of the provided buffer in
     *         hexadecimal byte format
     * @exception IllegalArgumentException
     *                If the number of bytes to convert is negative, or exceeds
     *                the maximum possible number
     * @exception BufferUnderflowException
     *                If the number of bytes to convert exceeds the number of
     *                {@linkplain Buffer#remaining() available} bytes in the
     *                buffer
     */
    public static String toHexString( ByteBuffer buf, int numBytes, BufferOperation op )
    {
        Objects.requireNonNull(op);

        _checkNumChars(numBytes);
        final ByteBuffer slice = getSlice(buf, numBytes, op);
        return _bytesToHex(slice);
    }

    /**
     * Converts the contents of the provided buffer to a string in hexadecimal
     * byte format. The contents of the string will begin at the specified
     * absolute buffer offset and will have the specified size in number of
     * bytes.
     * <p>
     * Note that there is a maximum possible number of bytes to convert
     * ({@code Integer.MAX_VALUE / 2}) due to restrictions in the length of a
     * string.
     * 
     * @param index
     *            An absolute buffer index
     * @param buf
     *            A byte buffer
     * @param numBytes
     *            The number of bytes to convert to a string
     * @return a string containing the contents of the provided buffer in
     *         hexadecimal byte format
     * @exception IllegalArgumentException
     *                If the number of bytes to convert is negative, or exceeds
     *                the maximum possible number
     * @exception IndexOutOfBoundsException
     *                If the offset is negative or not smaller than the buffer's
     *                limit
     * @exception BufferUnderflowException
     *                If the number of bytes to convert exceeds the number of
     *                available bytes between the specified offset and the
     *                buffer's limit
     */
    public static String toHexStringAtIndex( int index, ByteBuffer buf, int numBytes )
    {
        _checkNumChars(numBytes);
        final ByteBuffer offsetBuf = (ByteBuffer)buf.duplicate().position(index);
        final ByteBuffer slice = getSlice(offsetBuf, numBytes);
        return _bytesToHex(slice);
    }

    private static void _checkNumChars( int numBytes )
    {
        if (numBytes < 0) throw new IllegalArgumentException("number of bytes to convert is negative");

        final int numChars = numBytes * 2;
        if (numChars < 0) { // overflow
            throw new IllegalArgumentException("number of bytes to convert exceeds the maximum possible number");
        }
    }

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    /*
     * Copied from StackOverflow at: http://stackoverflow.com/a/9855338
     * and adapted to ByteBuffer.
     */
    private static String _bytesToHex( ByteBuffer buf )
    {
        final int offset = buf.position();
        final int size = buf.remaining();
        final char[] hexChars = new char[size * 2];

        for (int j = 0; j < size; j++) {
            int v = buf.get(j + offset) & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }

        return new String(hexChars);
    }

    private static final int ZERO_BUFFER_CAPACITY = 4096;

    private static final ThreadLocal<ByteBuffer> ZERO_ARR_BUFFER = new ThreadLocal<ByteBuffer>() {

        @Override
        protected ByteBuffer initialValue()
        {
            return allocate(ZERO_BUFFER_CAPACITY, BufferType.ARRAY_BACKED);
        }
    };

    private static final ThreadLocal<ByteBuffer> ZERO_DIR_BUFFER = new ThreadLocal<ByteBuffer>() {

        @Override
        protected ByteBuffer initialValue()
        {
            return allocate(ZERO_BUFFER_CAPACITY, BufferType.DIRECT);
        }
    };

    private static ThreadLocal<ByteBuffer> zeroTL( BufferType type )
    {
        switch (type) {
            case ARRAY_BACKED:
                return ZERO_ARR_BUFFER;

            case DIRECT:
                return ZERO_DIR_BUFFER;

            default:
                throw new AssertionError("unexpected enum type");
        }
    }

    /*
     * Requires size <= ZERO_BUFFER_CAPACITY
     */
    private static ByteBuffer _zeroBuffer( int size, BufferType type )
    {
        ByteBuffer zeroBuf = zeroTL(type).get();
        zeroBuf.clear();
        zeroBuf.limit(size);
        return zeroBuf;
    }

    private static final int INITIAL_CACHED_BUFFER_SIZE = 256;

    private static final ThreadLocal<ByteBuffer> CACHED_ARR_BUFFER = new ThreadLocal<ByteBuffer>() {

        @Override
        protected ByteBuffer initialValue()
        {
            return allocate(INITIAL_CACHED_BUFFER_SIZE, BufferType.ARRAY_BACKED);
        }
    };

    private static final ThreadLocal<ByteBuffer> CACHED_DIR_BUFFER = new ThreadLocal<ByteBuffer>() {

        @Override
        protected ByteBuffer initialValue()
        {
            return allocate(INITIAL_CACHED_BUFFER_SIZE, BufferType.DIRECT);
        }
    };

    private static ThreadLocal<ByteBuffer> _cachedTL( BufferType type )
    {
        switch (type) {
            case ARRAY_BACKED:
                return CACHED_ARR_BUFFER;

            case DIRECT:
                return CACHED_DIR_BUFFER;

            default:
                throw new AssertionError("unexpected enum type");
        }
    }

    private static ByteBuffer _cachedBuffer( int capacity, BufferType type )
    {
        if (capacity > MAX_CACHED_BUFFER_CAPACITY) {
            throw new IllegalArgumentException("cached buffer capacity is too large");
        }

        ThreadLocal<ByteBuffer> tl = _cachedTL(type);

        ByteBuffer buf = tl.get();
        if (capacity > buf.capacity()) {
            buf = allocate(capacity, type);
            tl.set(buf);
        }

        buf.clear();
        buf.order(ByteOrder.BIG_ENDIAN);
        return getSlice(buf, capacity);
    }

    private ByteBuffers()
    {
        // not used
    }
}
