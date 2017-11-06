package net.varanus.util.openflow.types;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.BitSet;
import java.util.Objects;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.checking.Indexables;
import net.varanus.util.collect.builder.BaseBuilder;
import net.varanus.util.io.BitUtils;
import net.varanus.util.io.ImmutableBitSet;
import net.varanus.util.io.Serializers;
import net.varanus.util.io.ByteBuffers.BufferType;
import net.varanus.util.io.exception.IOChannelReadException;
import net.varanus.util.io.exception.IOChannelWriteException;
import net.varanus.util.io.exception.IOReadException;
import net.varanus.util.io.exception.IOWriteException;
import net.varanus.util.io.serializer.IOReader;
import net.varanus.util.io.serializer.IOSerializer;
import net.varanus.util.io.serializer.IOWriter;
import net.varanus.util.openflow.PacketBits.BitField;


/**
 * 
 */
@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class BitMatch
{
    private static final BitMatch ANYTHING = new BitMatch(BitUtils.zerosImmutable(), BitUtils.zerosImmutable());

    public static BitMatch of( BitSet valueBits, BitSet maskBits )
    {
        return new BitMatch(
            ImmutableBitSet.valueOf(valueBits),
            ImmutableBitSet.valueOf(maskBits));
    }

    public static BitMatch of( byte[] value, byte[] mask )
    {
        return new BitMatch(
            ImmutableBitSet.valueOf(value),
            ImmutableBitSet.valueOf(mask));
    }

    public static BitMatch of( ByteBuffer value, ByteBuffer mask )
    {
        return new BitMatch(
            ImmutableBitSet.valueOf(value),
            ImmutableBitSet.valueOf(mask));
    }

    public static BitMatch anything()
    {
        return ANYTHING;
    }

    public static Builder newBuilder()
    {
        return new Builder();
    }

    private final ImmutableBitSet valueBits;
    private final ImmutableBitSet maskBits;

    private BitMatch( ImmutableBitSet valueBits, ImmutableBitSet maskBits )
    {
        if (!areValidValueAndMask(valueBits, maskBits))
            throw new IllegalArgumentException("value/mask bits are invalid");

        this.valueBits = valueBits;
        this.maskBits = maskBits;
    }

    public int bitLength()
    {
        return valueBits.length();
    }

    public ImmutableBitSet getValue()
    {
        return valueBits;
    }

    public ImmutableBitSet getMask()
    {
        return maskBits;
    }

    public boolean matchesPacket( byte[] packet )
    {
        if (bitLength(packet.length) < this.bitLength())
            return false; // optimization
        else
            return valueMatchesBitsMutating(BitSet.valueOf(packet));
    }

    public boolean matchesPacket( byte[] packet, int off, int len )
    {
        Indexables.checkOffsetLengthBounds(off, len, packet.length);
        if (bitLength(len) < this.bitLength())
            return false; // optimization
        else
            return valueMatchesBitsMutating(BitSet.valueOf(ByteBuffer.wrap(packet, off, len)));
    }

    public boolean matchesPacket( ByteBuffer packet )
    {
        if (bitLength(packet.remaining()) < this.bitLength())
            return false; // optimization
        else
            return valueMatchesBitsMutating(BitSet.valueOf(packet));
    }

    public boolean matchesBits( BitSet bits )
    {
        if (bits.length() < this.bitLength())
            return false; // optimization
        else
            return valueMatchesBitsMutating((BitSet)bits.clone());
    }

    public boolean matchesBits( ImmutableBitSet bits )
    {
        if (bits.length() < this.bitLength())
            return false; // optimization
        else
            return valueMatchesBitsMutating(bits.toMutable());
    }

    public boolean matchesAllOf( BitMatch other )
    {
        return valueMatchesBitsMutating(other.valueBits.toMutable())
               && maskMatchesBitsMutating(other.maskBits.toMutable());
    }

    private boolean valueMatchesBitsMutating( BitSet bits )
    {
        maskBits.otherAnd(bits);
        return valueBits.equalsBits(bits);
    }

    private boolean maskMatchesBitsMutating( BitSet bits )
    {
        maskBits.otherAnd(bits);
        return maskBits.equalsBits(bits);
    }

    public Builder createBuilder()
    {
        return new Builder(valueBits.toMutable(), maskBits.toMutable());
    }

    @Override
    public boolean equals( Object other )
    {
        return (other instanceof BitMatch)
               && this.equals((BitMatch)other);
    }

    public boolean equals( BitMatch other )
    {
        return (other != null)
               && this.valueBits.equals(other.valueBits)
               && this.maskBits.equals(other.maskBits);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(valueBits, maskBits);
    }

    @Override
    public String toString()
    {
        return String.format("%s/%s", valueBits.toHexString(), maskBits.toHexString());
    }

    private static int bitLength( int byteLength )
    {
        return Math.multiplyExact(byteLength, Byte.SIZE);
    }

    private static boolean areValidValueAndMask( ImmutableBitSet value, ImmutableBitSet mask )
    {
        // this tests if the following is true:
        // (mask OR value) == mask
        BitSet bits = mask.toMutable();
        value.otherOr(bits);
        return mask.equalsBits(bits);
    }

    @Immutable
    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class BitMasked
    {
        public static BitMasked of( BitField field, BitSet valueBits, BitSet maskBits )
        {
            return new BitMasked(
                Objects.requireNonNull(field),
                ImmutableBitSet.valueOf(valueBits),
                ImmutableBitSet.valueOf(maskBits));
        }

        public static BitMasked of( BitField field, byte[] value, byte[] mask )
        {
            return new BitMasked(
                Objects.requireNonNull(field),
                ImmutableBitSet.valueOf(value),
                ImmutableBitSet.valueOf(mask));
        }

        public static BitMasked of( BitField field, ByteBuffer value, ByteBuffer mask )
        {
            return new BitMasked(
                Objects.requireNonNull(field),
                ImmutableBitSet.valueOf(value),
                ImmutableBitSet.valueOf(mask));
        }

        public static BitMasked ofExact( BitField field, BitSet valueBits )
        {
            return new BitMasked(
                Objects.requireNonNull(field),
                ImmutableBitSet.valueOf(valueBits),
                BitUtils.onesImmutable(field.bitLength()));
        }

        public static BitMasked ofExact( BitField field, byte[] value )
        {
            return new BitMasked(
                Objects.requireNonNull(field),
                ImmutableBitSet.valueOf(value),
                BitUtils.onesImmutable(field.bitLength()));
        }

        public static BitMasked ofExact( BitField field, ByteBuffer value )
        {
            return new BitMasked(
                Objects.requireNonNull(field),
                ImmutableBitSet.valueOf(value),
                BitUtils.onesImmutable(field.bitLength()));
        }

        public static BitMasked ofWildcard( BitField field )
        {
            return new BitMasked(
                Objects.requireNonNull(field),
                BitUtils.zerosImmutable(),
                BitUtils.zerosImmutable());
        }

        private final BitField field;
        final ImmutableBitSet  valueBits;
        final ImmutableBitSet  maskBits;

        private BitMasked( BitField field, ImmutableBitSet valueBits, ImmutableBitSet maskBits )
        {
            if (!areValidValueAndMask(valueBits, maskBits))
                throw new IllegalArgumentException("value/mask bits are invalid");
            if (valueBits.length() > field.bitLength())
                throw new IllegalArgumentException("value bit-length exceeds field bit-length");

            this.field = field;
            this.valueBits = valueBits;
            this.maskBits = maskBits;
        }

        public BitField getField()
        {
            return field;
        }

        public ImmutableBitSet getValueBits()
        {
            return valueBits;
        }

        public ImmutableBitSet getMaskBits()
        {
            return maskBits;
        }

        @Override
        public boolean equals( Object other )
        {
            return (other instanceof BitMasked)
                   && this.equals((BitMasked)other);
        }

        public boolean equals( BitMasked other )
        {
            return (other != null)
                   && this.field.equals(other.field)
                   && this.valueBits.equals(other.valueBits)
                   && this.maskBits.equals(other.maskBits);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(field, valueBits, maskBits);
        }

        @Override
        public String toString()
        {
            return String.format("%s/%s @[%d, %d)",
                valueBits.toHexString(),
                maskBits.toHexString(),
                field.bitStart(),
                field.bitEnd());
        }
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class Builder implements BaseBuilder<BitMatch>
    {
        private final BitSet valueBits;
        private final BitSet maskBits;

        private Builder()
        {
            this(new BitSet(), new BitSet());
        }

        private Builder( BitSet valueBits, BitSet maskBits )
        {
            this.valueBits = valueBits;
            this.maskBits = maskBits;
        }

        public Builder add( BitMasked masked )
        {
            int start = masked.getField().bitStart();
            int length = masked.getField().bitLength();
            ImmutableBitSet value = masked.valueBits;
            ImmutableBitSet mask = masked.maskBits;

            value.copyInto(valueBits, start, length);
            mask.copyInto(maskBits, start, length);
            return this;
        }

        public Builder reset()
        {
            valueBits.clear();
            maskBits.clear();
            return this;
        }

        @Override
        public BitMatch build()
        {
            return new BitMatch(
                ImmutableBitSet.valueOf(valueBits),
                ImmutableBitSet.valueOf(maskBits));
        }
    }

    @ReturnValuesAreNonnullByDefault
    public static final class IO
    {
        public static IOWriter<BitMatch> writer()
        {
            return BitMatchSerializer.INSTANCE;
        }

        public static IOReader<BitMatch> reader()
        {
            return BitMatchSerializer.INSTANCE;
        }

        private static enum BitMatchSerializer implements IOSerializer<BitMatch>
        {
            INSTANCE;

            @Override
            public void write( BitMatch bm, WritableByteChannel ch ) throws IOChannelWriteException
            {
                ByteBuffer valueBuf = ByteBuffer.wrap(bm.valueBits.toByteArray());
                ByteBuffer maskBuf = ByteBuffer.wrap(bm.maskBits.toByteArray());
                Serializers.bufferWriter().write(valueBuf, ch);
                Serializers.bufferWriter().write(maskBuf, ch);
            }

            @Override
            public void write( BitMatch bm, OutputStream out ) throws IOWriteException
            {
                ByteBuffer valueBuf = ByteBuffer.wrap(bm.valueBits.toByteArray());
                ByteBuffer maskBuf = ByteBuffer.wrap(bm.maskBits.toByteArray());
                Serializers.bufferWriter().write(valueBuf, out);
                Serializers.bufferWriter().write(maskBuf, out);
            }

            @Override
            public void write( BitMatch bm, DataOutput out ) throws IOWriteException
            {
                ByteBuffer valueBuf = ByteBuffer.wrap(bm.valueBits.toByteArray());
                ByteBuffer maskBuf = ByteBuffer.wrap(bm.maskBits.toByteArray());
                Serializers.bufferWriter().write(valueBuf, out);
                Serializers.bufferWriter().write(maskBuf, out);
            }

            @Override
            public BitMatch read( ReadableByteChannel ch ) throws IOChannelReadException
            {
                ByteBuffer valueBuf = BUF_READER.read(ch);
                ByteBuffer maskBuf = BUF_READER.read(ch);
                return BitMatch.of(valueBuf, maskBuf);
            }

            @Override
            public BitMatch read( InputStream in ) throws IOReadException
            {
                ByteBuffer valueBuf = BUF_READER.read(in);
                ByteBuffer maskBuf = BUF_READER.read(in);
                return BitMatch.of(valueBuf, maskBuf);
            }

            @Override
            public BitMatch read( DataInput in ) throws IOReadException
            {
                ByteBuffer valueBuf = BUF_READER.read(in);
                ByteBuffer maskBuf = BUF_READER.read(in);
                return BitMatch.of(valueBuf, maskBuf);
            }

            private static final IOReader<ByteBuffer> BUF_READER =
                Serializers.allocatedBufferReader(BufferType.ARRAY_BACKED);
        }

        private IO()
        {
            // not used
        }
    }
}
