package net.varanus.util.io;


import java.nio.ByteBuffer;
import java.util.BitSet;

import net.varanus.util.checking.Indexables;
import net.varanus.util.lang.MoreObjects;
import net.varanus.util.text.StringUtils;


/**
 * 
 */
public final class BitUtils
{
    public static BitSet ones( int length )
    {
        BitSet bits = new BitSet(length);
        bits.set(0, length);
        return bits;
    }

    public static BitSet zeros()
    {
        return new BitSet(0);
    }

    public static ImmutableBitSet onesImmutable( int length )
    {
        return ImmutableBitSet.full(length);
    }

    public static ImmutableBitSet zerosImmutable()
    {
        return ImmutableBitSet.empty();
    }

    public static void copyBits( BitSet src, int srcIndex, BitSet dest, int destIndex, int length )
    {
        Indexables.checkLengthBounds(length);
        Indexables.checkOffsetLengthBounds(srcIndex, length, Integer.MAX_VALUE);
        Indexables.checkOffsetLengthBounds(destIndex, length, Integer.MAX_VALUE);
        MoreObjects.requireNonNull(src, "src", dest, "dest");

        dest.clear(destIndex, destIndex + length);
        src.stream()
            .filter(i -> (srcIndex <= i && i < (srcIndex + length)))
            .forEachOrdered(i -> dest.set(destIndex + i));
    }

    public static String toBinaryString( BitSet bits )
    {
        int len = bits.length();
        if (len == 0) {
            return "0";
        }
        else {
            StringBuilder sb = new StringBuilder(len);
            StringUtils.setChars(sb, 0, len, '0');
            bits.stream().forEachOrdered(i -> sb.setCharAt(i, '1'));
            return sb.toString();
        }
    }

    public static String toHexString( BitSet bits )
    {
        int len = bits.length();
        if (len == 0)
            return "0";
        else
            return ByteBuffers.toHexString(ByteBuffer.wrap(bits.toByteArray()));
    }

    private BitUtils()
    {
        // not used
    }
}
