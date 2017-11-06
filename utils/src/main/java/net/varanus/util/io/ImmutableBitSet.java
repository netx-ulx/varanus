package net.varanus.util.io;


import java.nio.ByteBuffer;
import java.nio.LongBuffer;
import java.util.BitSet;
import java.util.stream.IntStream;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;


/**
 * A class with the same public methods as {@link BitSet} except the "mutable"
 * ones and {@link BitSet#clone}.
 */
@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class ImmutableBitSet
{
    private static final ImmutableBitSet EMPTY = new ImmutableBitSet(new BitSet(0));

    public static ImmutableBitSet valueOf( BitSet set )
    {
        return new ImmutableBitSet((BitSet)set.clone());
    }

    public static ImmutableBitSet valueOf( long[] longs )
    {
        return new ImmutableBitSet(BitSet.valueOf(longs));
    }

    public static ImmutableBitSet valueOf( LongBuffer lb )
    {
        return new ImmutableBitSet(BitSet.valueOf(lb));
    }

    public static ImmutableBitSet valueOf( byte[] bytes )
    {
        return new ImmutableBitSet(BitSet.valueOf(bytes));
    }

    public static ImmutableBitSet valueOf( ByteBuffer bb )
    {
        return new ImmutableBitSet(BitSet.valueOf(bb));
    }

    public static ImmutableBitSet empty()
    {
        return EMPTY;
    }

    public static ImmutableBitSet full( int length )
    {
        BitSet full = new BitSet(length);
        full.set(0, length);
        return new ImmutableBitSet(full);
    }

    private final BitSet delegate;

    private ImmutableBitSet( BitSet delegate )
    {
        this.delegate = delegate;
    }

    public BitSet toMutable()
    {
        return (BitSet)delegate.clone();
    }

    public byte[] toByteArray()
    {
        return delegate.toByteArray();
    }

    public long[] toLongArray()
    {
        return delegate.toLongArray();
    }

    public boolean get( int bitIndex )
    {
        return delegate.get(bitIndex);
    }

    public BitSet get( int fromIndex, int toIndex )
    {
        return delegate.get(fromIndex, toIndex);
    }

    public int nextSetBit( int fromIndex )
    {
        return delegate.nextSetBit(fromIndex);
    }

    public int nextClearBit( int fromIndex )
    {
        return delegate.nextClearBit(fromIndex);
    }

    public int previousSetBit( int fromIndex )
    {
        return delegate.previousSetBit(fromIndex);
    }

    public int previousClearBit( int fromIndex )
    {
        return delegate.previousClearBit(fromIndex);
    }

    public int length()
    {
        return delegate.length();
    }

    public boolean isEmpty()
    {
        return delegate.isEmpty();
    }

    public boolean intersects( BitSet set )
    {
        return delegate.intersects(set);
    }

    public boolean intersects( ImmutableBitSet set )
    {
        return delegate.intersects(set.delegate);
    }

    public int cardinality()
    {
        return delegate.cardinality();
    }

    public int size()
    {
        return delegate.size();
    }

    public void copyInto( BitSet dest )
    {
        copyInto(dest, 0, this.length());
    }

    public void copyInto( BitSet dest, int destOff, int length )
    {
        BitUtils.copyBits(delegate, 0, dest, destOff, length);
    }

    /**
     * Like {@link BitSet#and(BitSet) BitSet.and} method but modifies the
     * {@code BitSet} argument instead of this instance.
     * 
     * @param other
     */
    public void otherAnd( BitSet other )
    {
        other.and(delegate);
    }

    /**
     * Like {@link BitSet#or(BitSet) BitSet.or} method but modifies the
     * {@code BitSet} argument instead of this instance.
     * 
     * @param other
     */
    public void otherOr( BitSet other )
    {
        other.or(delegate);
    }

    /**
     * Like {@link BitSet#xor(BitSet) BitSet.xor} method but modifies the
     * {@code BitSet} argument instead of this instance.
     * 
     * @param other
     */
    public void otherXor( BitSet other )
    {
        other.xor(delegate);
    }

    /**
     * Like {@link BitSet#andNot(BitSet) BitSet.andNot} method but modifies the
     * {@code BitSet} argument instead of this instance.
     * 
     * @param other
     */
    public void otherAndNot( BitSet other )
    {
        other.andNot(delegate);
    }

    @Override
    public int hashCode()
    {
        return delegate.hashCode();
    }

    @Override
    public boolean equals( Object other )
    {
        return (other instanceof ImmutableBitSet)
               && this.equals((ImmutableBitSet)other);
    }

    public boolean equals( ImmutableBitSet other )
    {
        return (other != null)
               && this.equalsBits(other.delegate);
    }

    public boolean equalsBits( BitSet set )
    {
        return delegate.equals(set);
    }

    @Override
    public String toString()
    {
        return delegate.toString();
    }

    public String toBinaryString()
    {
        return BitUtils.toBinaryString(delegate);
    }

    public String toHexString()
    {
        return BitUtils.toHexString(delegate);
    }

    public IntStream stream()
    {
        return delegate.stream();
    }
}
