package net.varanus.util.lang;


import java.util.Objects;

import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;


/**
 * Utility methods that deal with {@link Comparable} objects.
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class Comparables
{
    /**
     * Returns the minimum of both values, or the first value if both have the
     * same order.
     * 
     * @param x
     *            A {@code Comparable} value
     * @param y
     *            A {@code Comparable} value
     * @return the minimum of both values, or the first value if both have the
     *         same order
     */
    public static <T extends Comparable<? super T>> T min( T x, T y )
    {
        return aLEb(x, y) ? x : y;
    }

    public static <T extends Comparable<? super T>> T min( T a, T b, T c )
    {
        return min(min(a, b), c);
    }

    public static <T extends Comparable<? super T>> T min( T a, T b, T c, T d )
    {
        return min(min(min(a, b), c), d);
    }

    public static <T extends Comparable<? super T>> T min( T a, T b, T c, T d, T e )
    {
        return min(min(min(min(a, b), c), d), e);
    }

    @SafeVarargs
    @SuppressWarnings( "varargs" )
    public static <T extends Comparable<? super T>> T min( T a, T b, T c, T d, T e, T... rest )
    {
        T min = min(a, b, c, d, e);
        for (T t : rest) {
            min = min(min, t);
        }
        return min;
    }

    /**
     * Returns the maximum of both values, or the first value if both have the
     * same order.
     * 
     * @param x
     *            A {@code Comparable} value
     * @param y
     *            A {@code Comparable} value
     * @return the maximum of both values, or the first value if both have the
     *         same order
     */
    public static <T extends Comparable<? super T>> T max( T x, T y )
    {
        return aGEb(x, y) ? x : y;
    }

    public static <T extends Comparable<? super T>> T max( T a, T b, T c )
    {
        return max(max(a, b), c);
    }

    public static <T extends Comparable<? super T>> T max( T a, T b, T c, T d )
    {
        return max(max(max(a, b), c), d);
    }

    public static <T extends Comparable<? super T>> T max( T a, T b, T c, T d, T e )
    {
        return max(max(max(max(a, b), c), d), e);
    }

    @SafeVarargs
    @SuppressWarnings( "varargs" )
    public static <T extends Comparable<? super T>> T max( T a, T b, T c, T d, T e, T... rest )
    {
        T max = max(a, b, c, d, e);
        for (T t : rest) {
            max = max(max, t);
        }
        return max;
    }

    /**
     * Indicates whether {@code x < y}.
     * 
     * @param x
     *            A {@code Comparable} value
     * @param y
     *            A {@code Comparable} value
     * @return {@code true} iff {@code x < y}
     */
    public static <T extends Comparable<? super T>> boolean aLTb( T x, T y )
    {
        return x.compareTo(nn(y)) < 0;
    }

    /**
     * Indicates whether {@code x <= y}.
     * 
     * @param x
     *            A {@code Comparable} value
     * @param y
     *            A {@code Comparable} value
     * @return {@code true} iff {@code x <= y}
     */
    public static <T extends Comparable<? super T>> boolean aLEb( T x, T y )
    {
        return x.compareTo(nn(y)) <= 0;
    }

    /**
     * Indicates whether {@code x == y}.
     * 
     * @param x
     *            A {@code Comparable} value
     * @param y
     *            A {@code Comparable} value
     * @return {@code true} iff {@code x == y}
     */
    public static <T extends Comparable<? super T>> boolean aEQb( T x, T y )
    {
        return x.compareTo(nn(y)) == 0;
    }

    /**
     * Indicates whether {@code x >= y}.
     * 
     * @param x
     *            A {@code Comparable} value
     * @param y
     *            A {@code Comparable} value
     * @return {@code true} iff {@code x >= y}
     */
    public static <T extends Comparable<? super T>> boolean aGEb( T x, T y )
    {
        return x.compareTo(nn(y)) >= 0;
    }

    /**
     * Indicates whether {@code x > y}.
     * 
     * @param x
     *            A {@code Comparable} value
     * @param y
     *            A {@code Comparable} value
     * @return {@code true} iff {@code x > y}
     */
    public static <T extends Comparable<? super T>> boolean aGTb( T x, T y )
    {
        return x.compareTo(nn(y)) > 0;
    }

    private static <T> T nn( T obj )
    {
        return Objects.requireNonNull(obj);
    }

    private Comparables()
    {
        // not used
    }
}
