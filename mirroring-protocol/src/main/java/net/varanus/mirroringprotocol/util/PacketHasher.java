package net.varanus.mirroringprotocol.util;


import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Objects;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.checking.Indexables;
import net.varanus.util.io.BufferOperation;
import net.varanus.util.io.ByteBuffers;
import net.varanus.util.io.ByteBuffers.BufferType;
import net.varanus.util.io.ImmutableBitSet;
import net.varanus.util.io.Serializers;
import net.varanus.util.io.exception.IOChannelReadException;
import net.varanus.util.io.exception.IOChannelWriteException;
import net.varanus.util.io.serializer.IOReader;
import net.varanus.util.io.serializer.IOWriter;
import net.varanus.util.lang.SizeOf;


/**
 * 
 */
@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class PacketHasher
{
    public static PacketHasher of( HashFunction hashFunction, byte[] mask )
    {
        checkMaskLength(mask.length);
        return new PacketHasher(
            Objects.requireNonNull(hashFunction),
            Arrays.copyOf(mask, mask.length));
    }

    public static PacketHasher of( HashFunction hashFunction, byte[] mask, int off, int len )
    {
        checkMaskLength(len);
        return new PacketHasher(
            Objects.requireNonNull(hashFunction),
            Arrays.copyOfRange(mask, off, off + len));
    }

    public static PacketHasher of( HashFunction hashFunction, ByteBuffer mask )
    {
        checkMaskLength(mask.remaining());
        return new PacketHasher(
            Objects.requireNonNull(hashFunction),
            ByteBuffers.getArrayCopy(mask, BufferOperation.RESTORE_POSITION));
    }

    public static PacketHasher of( HashFunction hashFunction, BitSet mask )
    {
        checkMaskLength(SizeOf.bitsInBytes(mask.length()));
        return new PacketHasher(
            Objects.requireNonNull(hashFunction),
            mask.toByteArray());
    }

    public static PacketHasher of( HashFunction hashFunction, ImmutableBitSet mask )
    {
        checkMaskLength(SizeOf.bitsInBytes(mask.length()));
        return new PacketHasher(
            Objects.requireNonNull(hashFunction),
            mask.toByteArray());
    }

    public static PacketHasher forWholePacket( HashFunction hashFunction )
    {
        return new PacketHasher(
            Objects.requireNonNull(hashFunction),
            EMPTY_MASK);
    }

    private static void checkMaskLength( int length )
    {
        if (length < 1)
            throw new IllegalArgumentException("mask must have at least one byte");
    }

    private static final byte[] EMPTY_MASK = new byte[0];

    private final HashFunction hashFunction;
    private final byte[]       mask;

    private PacketHasher( HashFunction hashFunction, byte[] mask )
    {
        this.hashFunction = hashFunction;
        this.mask = mask;
    }

    public HashCode hash( byte[] pkt )
    {
        return hash(pkt, 0, pkt.length);
    }

    public HashCode hash( byte[] pkt, int off, int len )
    {
        Indexables.checkOffsetLengthBounds(off, len, pkt.length);

        HashCode hash = tryWithNoMask(pkt, off, len);
        if (hash != null) {
            return hash;
        }
        else {
            // need to apply mask, must copy packet bytes first
            int numBytes = Math.min(len, mask.length);
            byte[] bytes = Arrays.copyOfRange(pkt, off, off + numBytes);
            return maskAndHash(bytes, 0, numBytes);
        }
    }

    public HashCode hashNoCopy( byte[] pkt )
    {
        return hashNoCopy(pkt, 0, pkt.length);
    }

    public HashCode hashNoCopy( byte[] pkt, int off, int len )
    {
        Indexables.checkOffsetLengthBounds(off, len, pkt.length);

        HashCode hash = tryWithNoMask(pkt, off, len);
        if (hash != null) {
            return hash;
        }
        else {
            // need to apply mask, packet bytes are modified
            int numBytes = Math.min(len, mask.length);
            return maskAndHash(pkt, off, numBytes);
        }
    }

    // NOTE: requires valid off and len
    private @Nullable HashCode tryWithNoMask( byte[] pkt, int off, int len )
    {
        // if we do not have a mask, then feed the packet bytes directly
        if (mask.length == 0)
            return hashFunction.hashBytes(pkt, off, len);
        else
            return null;
    }

    // NOTE: requires valid off and len, and len <= mask.length
    private HashCode maskAndHash( byte[] bytes, int off, int len )
    {
        for (int i = 0; i < len; i++) {
            bytes[off + i] &= mask[i];
        }
        return hashFunction.hashBytes(bytes, off, len);
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class IO
    {
        public static IOWriter<PacketHasher> writer()
        {
            return new IOWriter<PacketHasher>() {
                @Override
                public void write( PacketHasher ph, WritableByteChannel ch ) throws IOChannelWriteException
                {
                    ByteBuffer mask = ByteBuffer.wrap(ph.mask);
                    Serializers.bufferWriter().write(mask, ch);
                }
            };
        }

        public static IOReader<PacketHasher> reader( HashFunction hashFunction )
        {
            Objects.requireNonNull(hashFunction);
            return new IOReader<PacketHasher>() {
                @Override
                public PacketHasher read( ReadableByteChannel ch ) throws IOChannelReadException
                {
                    ByteBuffer mask = Serializers.allocatedBufferReader(BufferType.ARRAY_BACKED).read(ch);
                    return new PacketHasher(hashFunction, mask.array());
                }
            };
        }

        private IO()
        {
            // not used
        }
    }
}
