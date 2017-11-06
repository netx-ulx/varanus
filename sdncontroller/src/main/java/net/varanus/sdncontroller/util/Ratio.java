package net.varanus.sdncontroller.util;


import static net.varanus.util.math.ExtraMath.signum;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.Objects;

import javax.annotation.CheckForNull;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import org.apache.commons.math3.fraction.BigFraction;
import org.apache.commons.math3.util.ArithmeticUtils;

import com.google.common.base.Preconditions;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;


/**
 * 
 */
@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class Ratio extends Number implements Comparable<Ratio>
{
    public static final Ratio ZERO = new Ratio(0, 1);

    public static final Ratio ONE       = new Ratio(1, 1);
    public static final Ratio MINUS_ONE = new Ratio(-1, 1);

    public static final Ratio ONE_HALF       = new Ratio(1, 2);
    public static final Ratio MINUS_ONE_HALF = new Ratio(-1, 2);

    public static final Ratio ONE_QUARTER       = new Ratio(1, 4);
    public static final Ratio MINUS_ONE_QUARTER = new Ratio(-1, 4);

    public static final Ratio THREE_QUARTERS       = new Ratio(3, 4);
    public static final Ratio MINUS_THREE_QUARTERS = new Ratio(-3, 4);

    public static final Ratio NaN               = new Ratio(0, 0);
    public static final Ratio POSITIVE_INFINITY = new Ratio(1, 0);
    public static final Ratio NEGATIVE_INFINITY = new Ratio(-1, 0);

    // for percentage calculations
    private static final Ratio ONE_HUNDRED = new Ratio(100, 1);

    private static final int DEFAULT_DECIMAL_PRECISION    = 3;
    private static final int DEFAULT_PERCENTAGE_PRECISION = 3;

    private static final long serialVersionUID = 3754778773278705859L;

    public static Ratio of( long numerator, long denominator )
    {
        // canonicalize the ratio
        // returns {canonicalNumerator, canonicalDenominator}
        long[] canon = canonicalize(numerator, denominator);

        // return constants for special ratio values
        Ratio cached = getCached(canon);
        if (cached != null)
            return cached;
        else
            return new Ratio(numerator, denominator, canon);
    }

    public static Ratio of( double value )
    {
        return of(value, Integer.MAX_VALUE);
    }

    public static Ratio of( double value, int maxDenominator )
    {
        if (Double.isFinite(value))
            return fromFraction(new BigFraction(value, validMaxDenominator(maxDenominator)));
        else if (value == Double.POSITIVE_INFINITY)
            return POSITIVE_INFINITY;
        else if (value == Double.NEGATIVE_INFINITY)
            return NEGATIVE_INFINITY;
        else if (Double.isNaN(value))
            return NaN;
        else
            throw new AssertionError();
    }

    private final long numerator;
    private final long denominator;

    private final long canonicalNumerator;
    private final long canonicalDenominator; // never negative

    private Ratio( long numerator, long denominator )
    {
        this(numerator, denominator, canonicalize(numerator, denominator));
    }

    private Ratio( long numerator, long denominator, long[] canon )
    {
        this.numerator = numerator;
        this.denominator = denominator;

        this.canonicalNumerator = canon[0];
        this.canonicalDenominator = canon[1];
    }

    public long numerator()
    {
        return numerator;
    }

    public long denominator()
    {
        return denominator;
    }

    public long canonicalNumerator()
    {
        return canonicalNumerator;
    }

    public long canonicalDenominator()
    {
        return canonicalDenominator;
    }

    public boolean isNaN()
    {
        return isNaN(canonicalNumerator, canonicalDenominator);
    }

    public boolean isInfinite()
    {
        return isInfinite(canonicalNumerator, canonicalDenominator);
    }

    public boolean isPositiveInfinity()
    {
        return isPositiveInfinity(canonicalNumerator, canonicalDenominator);
    }

    public boolean isNegativeInfinity()
    {
        return isNegativeInfinity(canonicalNumerator, canonicalDenominator);
    }

    public boolean isFinite()
    {
        return isFinite(canonicalNumerator, canonicalDenominator);
    }

    public boolean isZero()
    {
        return isZero(canonicalNumerator, canonicalDenominator);
    }

    public boolean isNegative()
    {
        return isNegative(canonicalNumerator, canonicalDenominator);
    }

    public boolean isPositive()
    {
        return isPositive(canonicalNumerator, canonicalDenominator);
    }

    public boolean isProperFraction()
    {
        return isProperFraction(canonicalNumerator, canonicalDenominator);
    }

    public BigDecimal bigDecimalValue()
    {
        return bigDecimalValue(MathContext.DECIMAL128);
    }

    public BigDecimal bigDecimalValue( int precision )
    {
        return bigDecimalValue(new MathContext(precision));
    }

    public BigDecimal bigDecimalValue( MathContext context )
    {
        BigDecimal bigNum = BigDecimal.valueOf(canonicalNumerator);
        BigDecimal bigDen = BigDecimal.valueOf(canonicalDenominator);
        return bigNum.divide(bigDen, context);
    }

    @Override
    public int intValue()
    {
        if (isNaN()) {
            throw new ArithmeticException("cannot represent NaN as an integer");
        }
        else if (isInfinite()) {
            throw new ArithmeticException("cannot represent an infinity as an integer");
        }
        else {
            return Math.toIntExact(longValue());
        }
    }

    @Override
    public long longValue()
    {
        if (isNaN()) {
            throw new ArithmeticException("cannot represent NaN as a long integer");
        }
        else if (isInfinite()) {
            throw new ArithmeticException("cannot represent an infinity as a long integer");
        }
        else {
            return canonicalNumerator / canonicalDenominator;
        }
    }

    @Override
    public float floatValue()
    {
        return (float)canonicalNumerator / canonicalDenominator;
    }

    @Override
    public double doubleValue()
    {
        return (double)canonicalNumerator / canonicalDenominator;
    }

    public int percentageIntValue()
    {
        if (isNaN()) {
            throw new ArithmeticException("cannot represent NaN as an integer percentage");
        }
        else if (isInfinite()) {
            throw new ArithmeticException("cannot represent an infinity as an integer percentage");
        }
        else {
            return this.times(ONE_HUNDRED).intValue();
        }
    }

    public long percentageLongValue()
    {
        if (isNaN()) {
            throw new ArithmeticException("cannot represent NaN as a long integer percentage");
        }
        else if (isInfinite()) {
            throw new ArithmeticException("cannot represent an infinity as a long integer percentage");
        }
        else {
            return this.times(ONE_HUNDRED).longValue();
        }
    }

    public float percentageFloatValue()
    {
        return this.times(ONE_HUNDRED).floatValue();
    }

    public double percentageDoubleValue()
    {
        return this.times(ONE_HUNDRED).doubleValue();
    }

    public Ratio plus( Ratio other )
    {
        if (this.isNaN() || other.isNaN()) {
            return NaN;
        }
        else if (this.isInfinite() && other.isInfinite()) {
            if (this.isNegative() ^ other.isNegative()) { // either this or
                                                          // other is negative
                return NaN; // +Inf + -Inf is undefined
            }
            else { // both are positive or both are negative
                return this; // or other, since they are the same
            }
        }
        else if (this.isInfinite()) { // other is finite
            return this;
        }
        else if (other.isInfinite()) { // this is finite
            return other;
        }
        else { // both are finite
            long thisNumer = this.canonicalNumerator;
            long thisDenom = this.canonicalDenominator;   // never zero

            long otherNumer = other.canonicalNumerator;
            long otherDenom = other.canonicalDenominator; // never zero

            // lowest common denominator
            long lcd = ArithmeticUtils.lcm(thisDenom, otherDenom);

            long thisFactor = lcd / thisDenom;   // always exact division
            long thisNumerEx = Math.multiplyExact(thisNumer, thisFactor);

            long otherFactor = lcd / otherDenom; // always exact division
            long otherNumerEx = Math.multiplyExact(otherNumer, otherFactor);

            return of(Math.addExact(thisNumerEx, otherNumerEx), lcd);
        }
    }

    public Ratio minus( Ratio other )
    {
        return this.plus(other.negate());
    }

    public Ratio times( Ratio other )
    {
        if (this.isNaN() || other.isNaN()) {
            return NaN;
        }
        else if (this.isInfinite() || other.isInfinite()) {
            if (this.isZero() || other.isZero()) {
                return NaN; // Inf x 0 is undefined
            }
            else if (this.isNegative() ^ other.isNegative()) {
                // either this or other is negative
                return NEGATIVE_INFINITY;
            }
            else {
                // both are positive or both are negative
                return POSITIVE_INFINITY;
            }
        }
        else {
            // both are finite
            return of(
                Math.multiplyExact(this.canonicalNumerator, other.canonicalNumerator),
                Math.multiplyExact(this.canonicalDenominator, other.canonicalDenominator));
        }
    }

    public Ratio dividedBy( Ratio other )
    {
        if (this.isZero() && other.isZero()) {
            return NaN; // 0 / 0 is undefined
        }
        else if (this.isInfinite() && other.isInfinite()) {
            return NaN; // +-Inf / +-Inf is undefined
        }
        else {
            return this.times(other.inverse());
        }
    }

    public Ratio abs()
    {
        return (isPositive() || isZero()) ? this : negate();
    }

    public Ratio negate()
    {
        return isNaN() ? NaN : of(-canonicalNumerator, canonicalDenominator);
    }

    public Ratio inverse()
    {
        return isNaN() ? NaN : of(canonicalDenominator, canonicalNumerator);
    }

    @Override
    public boolean equals( Object other )
    {
        return (other instanceof Ratio) && this.equals((Ratio)other);
    }

    public boolean equals( Ratio other )
    {
        return (other != null)
               && this.canonicalNumerator == other.canonicalNumerator
               && this.canonicalDenominator == other.canonicalDenominator;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(canonicalNumerator, canonicalDenominator);
    }

    @Override
    public int compareTo( Ratio other )
    {
        return this.bigDecimalValue().compareTo(other.bigDecimalValue());
    }

    @Override
    public String toString()
    {
        return numerator + "/" + denominator;
    }

    public String toDecimalString()
    {
        return toDecimalString(DEFAULT_DECIMAL_PRECISION);
    }

    public String toDecimalString( int precision )
    {
        if (isNaN()) {
            return "NaN";
        }
        else if (isPositiveInfinity()) {
            return "Infinity";
        }
        else if (isNegativeInfinity()) {
            return "-Infinity";
        }
        else {
            return bigDecimalValue(precision).toPlainString();
        }
    }

    public String toPercentageString()
    {
        return toPercentageString(DEFAULT_PERCENTAGE_PRECISION);
    }

    public String toPercentageString( int precision )
    {
        if (isNaN()) {
            return "NaN %";
        }
        else if (isPositiveInfinity()) {
            return "Infinity %";
        }
        else if (isNegativeInfinity()) {
            return "-Infinity %";
        }
        else {
            return bigDecimalValue(precision)
                .multiply(BigDecimal.valueOf(100), new MathContext(precision))
                .toPlainString() + " %";
        }
    }

    public String toCanonicalString()
    {
        if (isNaN()) {
            return "NaN";
        }
        else if (isPositiveInfinity()) {
            return "Infinity";
        }
        else if (isNegativeInfinity()) {
            return "-Infinity";
        }
        else if (canonicalNumerator == 0) {
            return "0";
        }
        else if (canonicalDenominator == 1) {
            return String.valueOf(canonicalNumerator);
        }
        else if (canonicalNumerator == canonicalDenominator) {
            return "1";
        }
        else {
            return canonicalNumerator + "/" + canonicalDenominator;
        }
    }

    private static long[] canonicalize( long numerator, long denominator )
    {
        final long canonicalNumerator;
        final long canonicalDenominator;

        if (numerator == 0 && denominator == 0) {
            // NaN
            canonicalNumerator = 0;
            canonicalDenominator = 0;
        }
        else if (numerator == 0) {
            // zero
            canonicalNumerator = 0;
            canonicalDenominator = 1;
        }
        else if (denominator == 0) {
            // +/- infinity
            canonicalNumerator = signum(numerator);
            canonicalDenominator = 0;
        }
        else if (numerator == denominator) {
            // one
            canonicalNumerator = 1;
            canonicalDenominator = 1;
        }
        else {
            BigInteger bigNumerator = BigInteger.valueOf(numerator);
            BigInteger bigDenominator = BigInteger.valueOf(denominator);
            BigInteger gcd = bigNumerator.gcd(bigDenominator);

            // reduce the ratio
            long reducedNumerator = bigNumerator.divide(gcd).longValue();
            long reducedDenominator = bigDenominator.divide(gcd).longValue();

            // canonicalize the signs
            if (reducedDenominator > 0) {
                // simple case, no need to flip the signs
                canonicalNumerator = reducedNumerator;
                canonicalDenominator = reducedDenominator;
            }
            else if (reducedNumerator == Long.MIN_VALUE || reducedDenominator == Long.MIN_VALUE) {
                // special case, cannot flip the sign of MIN_VALUE
                throw new ArithmeticException(
                    String.format(
                        "cannot canonicalize ratio of %d/%d due to integer overflow",
                        reducedNumerator,
                        reducedDenominator));
            }
            else {
                // flip the signs in order to make the denominator positive
                canonicalNumerator = -(reducedNumerator);
                canonicalDenominator = -(reducedDenominator);
            }
        }

        return new long[] {canonicalNumerator, canonicalDenominator};
    }

    private static final Ratio[] cached = {ZERO,
                                           ONE, MINUS_ONE,
                                           ONE_HALF, MINUS_ONE_HALF,
                                           ONE_QUARTER, MINUS_ONE_QUARTER,
                                           THREE_QUARTERS, MINUS_THREE_QUARTERS,
                                           NaN, POSITIVE_INFINITY, NEGATIVE_INFINITY};

    private static @CheckForNull Ratio getCached( long[] canon )
    {
        for (Ratio r : cached) {
            if ((canon[0] == r.canonicalNumerator)
                && (canon[1] == r.canonicalDenominator)) {
                return r;
            }
        }

        return null;
    }

    private static Ratio fromFraction( BigFraction frac )
    {
        long numerator = frac.getNumeratorAsLong();
        long denominator = frac.getDenominatorAsLong();
        long[] canon = {numerator, denominator};

        // return constants for special ratio values
        Ratio cached = getCached(canon);
        if (cached != null)
            return cached;
        else
            return new Ratio(numerator, denominator, canon);
    }

    private static int validMaxDenominator( int maxDenominator )
    {
        Preconditions.checkArgument(maxDenominator > 0, "max denominator must be positive");
        return maxDenominator;
    }

    // ALL METHODS BELOW ASSUME CANONICAL VALUES //

    private static boolean isNaN( long num, long den )
    {
        return (num == 0) && (den == 0);
    }

    private static boolean isInfinite( long num, long den )
    {
        return (num != 0) && (den == 0);
    }

    private static boolean isPositiveInfinity( long num, long den )
    {
        return (num > 0) && (den == 0);
    }

    private static boolean isNegativeInfinity( long num, long den )
    {
        return (num < 0) && (den == 0);
    }

    private static boolean isFinite( @SuppressWarnings( "unused" ) long num, long den )
    {
        return den != 0;
    }

    private static boolean isZero( long num, long den )
    {
        return (num == 0) && (den != 0);
    }

    private static boolean isNegative( long num, @SuppressWarnings( "unused" ) long den )
    {
        return num < 0; // -inf is also covered
    }

    private static boolean isPositive( long num, @SuppressWarnings( "unused" ) long den )
    {
        return num > 0; // +inf is also covered
    }

    private static boolean isProperFraction( long num, long den )
    {
        return (den != 0) && (num >= 0) && (den >= num);
    }
}
