package net.varanus.mirroringprotocol.util;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Objects;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashCodes;
import com.google.common.hash.Hasher;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.io.ExtraChannels;
import net.varanus.util.io.Serializers;
import net.varanus.util.io.ByteBuffers.BufferType;
import net.varanus.util.io.exception.IOChannelReadException;
import net.varanus.util.io.exception.IOChannelWriteException;
import net.varanus.util.io.serializer.IOReader;
import net.varanus.util.io.serializer.IOSerializer;
import net.varanus.util.io.serializer.IOWriter;


/**
 * 
 */
@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class PacketSummary
{
    public static PacketSummary hashPacket( Hasher hasher, byte[] pkt )
    {
        return hashPacket(hasher, pkt, 0, pkt.length);
    }

    public static PacketSummary hashPacket( Hasher hasher, byte[] pkt, int off, int len )
    {
        hasher.putBytes(pkt, off, len);
        HashCode hash = hasher.hash();
        return new PacketSummary(hash, len);
    }

    private final HashCode hash;
    private final int      pktLength;

    private PacketSummary( HashCode hash, int pktLength )
    {
        this.hash = Objects.requireNonNull(hash);
        this.pktLength = pktLength;
    }

    public HashCode getHash()
    {
        return hash;
    }

    public int length()
    {
        return pktLength;
    }

    @Override
    public boolean equals( Object other )
    {
        return (other instanceof PacketSummary)
               && this.equals((PacketSummary)other);
    }

    public boolean equals( PacketSummary other )
    {
        return (other != null)
               && this.hash.equals(other.hash);
    }

    @Override
    public int hashCode()
    {
        return hash.hashCode();
    }

    @Override
    public String toString()
    {
        return String.format("(%d) 0x%s", pktLength, hash);
    }

    @ReturnValuesAreNonnullByDefault
    public static final class IO
    {
        public static IOWriter<PacketSummary> writer()
        {
            return Serial.INSTANCE;
        }

        public static IOReader<PacketSummary> reader()
        {
            return Serial.INSTANCE;
        }

        private static enum Serial implements IOSerializer<PacketSummary>
        {
            INSTANCE;

            @Override
            public void write( PacketSummary summ, WritableByteChannel ch ) throws IOChannelWriteException
            {
                HashCodeSerializer.INSTANCE.write(summ.hash, ch);
                ExtraChannels.writeShort(ch, (short)summ.pktLength, ByteOrder.BIG_ENDIAN);
            }

            @Override
            public PacketSummary read( ReadableByteChannel ch ) throws IOChannelReadException
            {
                HashCode hash = HashCodeSerializer.INSTANCE.read(ch);
                int pktLength = Short.toUnsignedInt(ExtraChannels.readShort(ch, ByteOrder.BIG_ENDIAN));
                return new PacketSummary(hash, pktLength);
            }

            private static enum HashCodeSerializer implements IOSerializer<HashCode>
            {
                INSTANCE;

                @Override
                public void write( HashCode hash, WritableByteChannel ch ) throws IOChannelWriteException
                {
                    ByteBuffer buf = ByteBuffer.wrap(hash.asBytes());
                    Serializers.bufferWriter().write(buf, ch);
                }

                @Override
                public HashCode read( ReadableByteChannel ch ) throws IOChannelReadException
                {
                    ByteBuffer buf = Serializers.allocatedBufferReader(BufferType.ARRAY_BACKED).read(ch);
                    return HashCodes.fromBytes(buf.array());
                }
            }
        }

        private IO()
        {
            // not used
        }
    }
}
