package net.varanus.util.math;


import java.math.BigInteger;

import net.varanus.util.lang.Comparables;


/**
 * Defines arithmetical functions not present in JDK or guava or
 * apache.commons.math.
 */
public final class ExtraMath
{
    public static int signum( int x )
    {
        return (x > 0) ? 1 : (x < 0) ? -1 : 0;
    }

    public static int signum( long x )
    {
        return (x > 0L) ? 1 : (x < 0L) ? -1 : 0;
    }

    /**
     * Returns the positive modulus of the {@code int} arguments.
     * 
     * @param x
     *            The dividend
     * @param y
     *            The divisor
     * @return a positive modulus
     */
    public static int positiveMod( int x, int y )
    {
        return (Math.floorMod(x, y) + Math.abs(y)) % Math.abs(y);
    }

    /**
     * Returns {@code (a + b) % mod} while avoiding integer overflow.
     * 
     * @param x
     *            the left hand value
     * @param y
     *            the right hand value
     * @param mod
     *            the modulo value
     * @return {@code (a + b) % mod}
     */
    public static int addModulo( int x, int y, int mod )
    {
        return (int)(((long)x + y) % mod);
    }

    /**
     * Returns {@code (a + b) % mod} while avoiding long integer overflow.
     * 
     * @param x
     *            the left hand value
     * @param y
     *            the right hand value
     * @param mod
     *            the modulo value
     * @return {@code (a + b) % mod}
     */
    public static int addModulo( long x, long y, int mod )
    {
        final long naiveSum = x + y;

        // HD 2-12 Overflow iff both arguments have the opposite sign of the
        // result
        if (((x ^ naiveSum) & (y ^ naiveSum)) < 0) {
            return BigInteger.valueOf(x)
                .add(BigInteger.valueOf(y))
                .remainder(BigInteger.valueOf(mod))
                .intValue();
        }
        else {
            return (int)(naiveSum % mod);
        }
    }

    public static int addSaturated( int x, int y )
    {
        /*
         * Code adapted from java.lang.Math.addExact(int, int)
         */

        final int r = x + y;
        if (((x ^ r) & (y ^ r)) < 0) {
            // overflow case
            return saturate((long)x + y);
        }

        // simple case
        return r;
    }

    public static long addSaturated( long x, long y )
    {
        /*
         * Code adapted from java.lang.Math.addExact(int, int)
         */

        final long r = x + y;
        if (((x ^ r) & (y ^ r)) < 0) {
            // overflow case
            return saturate(BigInteger.valueOf(x).add(BigInteger.valueOf(y)));
        }

        // simple case
        return r;
    }

    public static int multiplySaturated( int x, int y )
    {
        /*
         * Code adapted from java.lang.Math.multiplyExact(int, int)
         */

        final long r = (long)x * (long)y;
        if ((int)r != r) {
            // overflow case
            return saturate(r);
        }

        // simple case
        return (int)r;
    }

    public static long multiplySaturated( long x, long y )
    {
        /*
         * Code adapted from java.lang.Math.multiplyExact(long, long)
         */

        final long r = x * y;
        final long ax = Math.abs(x);
        final long ay = Math.abs(y);
        if (((ax | ay) >>> 31 != 0)) {
            // Some bits greater than 2^31 that might cause overflow
            // Check the result using the divide operator
            // and check for the special case of Long.MIN_VALUE * -1
            if (((y != 0) && (r / y != x))
                || (x == Long.MIN_VALUE && y == -1)) {
                // overflow case
                return saturate(BigInteger.valueOf(x).multiply(BigInteger.valueOf(y)));
            }
        }

        // simple case
        return r;
    }

    // Requires an overflowed int value
    private static int saturate( long r )
    {
        if (r < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        else if (r > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        else {
            throw new AssertionError("should not happen");
        }
    }

    // Requires an overflowed long value
    private static long saturate( BigInteger r )
    {
        if (Comparables.aLTb(r, BigInteger.valueOf(Long.MIN_VALUE))) {
            return Long.MIN_VALUE;
        }
        else if (Comparables.aGTb(r, BigInteger.valueOf(Long.MAX_VALUE))) {
            return Long.MAX_VALUE;
        }
        else {
            throw new AssertionError("should not happen");
        }
    }

    private ExtraMath()
    {
        // not used
    }
}
