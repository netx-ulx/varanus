package net.varanus.util.unitvalue.si;


import static net.varanus.util.unitvalue.si.InfoDoubleUnit.BITS;
import static net.varanus.util.unitvalue.si.InfoDoubleUnit.BYTES;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.functional.PossibleDouble;


/**
 * 
 */
@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class InfoDouble extends SIMultipleDoubleValue<InfoDoubleUnit, InfoDouble>
{
    private static final InfoDouble ABSENT = new InfoDouble(0, false);
    public static final InfoDouble  ZERO   = new InfoDouble(0, true);

    public static InfoDouble ofBits( double valBits )
    {
        return new InfoDouble(valBits, true);
    }

    public static InfoDouble ofBytes( double valBytes )
    {
        return new InfoDouble(BYTES.toBits(valBytes), true);
    }

    public static InfoDouble of( double val, InfoDoubleUnit unit )
    {
        return new InfoDouble(unit.toBits(val), true);
    }

    public static InfoDouble ofPossibleBits( PossibleDouble valBits )
    {
        return valBits.mapToObj(InfoDouble::ofBits).orElse(absent());
    }

    public static InfoDouble ofPossibleBytes( PossibleDouble valBytes )
    {
        return valBytes.mapToObj(InfoDouble::ofBytes).orElse(absent());
    }

    public static InfoDouble ofPossible( PossibleDouble val, InfoDoubleUnit unit )
    {
        return val.mapToObj(v -> InfoDouble.of(v, unit)).orElse(absent());
    }

    public static InfoDouble parseBits( String s )
    {
        return ofBits(Double.parseDouble(s));
    }

    public static InfoDouble parseBytes( String s )
    {
        return ofBytes(Double.parseDouble(s));
    }

    public static InfoDouble parse( String s, InfoDoubleUnit unit )
    {
        return of(Double.parseDouble(s), unit);
    }

    public static InfoDouble absent()
    {
        return ABSENT;
    }

    private InfoDouble( double bitsValue, boolean isPresent )
    {
        super(bitsValue, isPresent);
    }

    public double inBits()
    {
        return toBaseValue();
    }

    public PossibleDouble asPossibleInBits()
    {
        return asPossibleBase();
    }

    public double inBytes()
    {
        return in(BYTES);
    }

    public PossibleDouble asPossibleInBytes()
    {
        return asPossible(BYTES);
    }

    public InfoLong asLong()
    {
        return isPresent() ? InfoLong.ofBits(Math.round(inBits()))
                           : InfoLong.absent();
    }

    @Override
    public boolean equals( Object other )
    {
        return (other instanceof InfoDouble)
               && super.equals(other);
    }

    public boolean equals( InfoDouble other )
    {
        return super.equals(other);
    }

    @Override
    public int hashCode()
    {
        return super.hashCode();
    }

    @Override
    public String toString()
    {
        return toBitString();
    }

    public String toBitString()
    {
        return toSmartString(BITS);
    }

    public String toBitString( int precision )
    {
        return toSmartString(BITS, precision);
    }

    public String toByteString()
    {
        return toSmartString(BYTES);
    }

    public String toByteString( int precision )
    {
        return toSmartString(BYTES, precision);
    }

    public String toStringAnd( Object unitSuffix )
    {
        return toBitStringAnd(unitSuffix);
    }

    public String toBitStringAnd( Object unitSuffix )
    {
        return toSmartStringAnd(BITS, unitSuffix);
    }

    public String toBitStringAnd( int precision, Object unitSuffix )
    {
        return toSmartStringAnd(BITS, precision, unitSuffix);
    }

    public String toByteStringAnd( Object unitSuffix )
    {
        return toSmartStringAnd(BYTES, unitSuffix);
    }

    public String toByteStringAnd( int precision, Object unitSuffix )
    {
        return toSmartStringAnd(BYTES, precision, unitSuffix);
    }

    @Override
    protected InfoDoubleUnit baseUnit()
    {
        return BITS;
    }

    @Override
    protected InfoDouble buildAbsent()
    {
        return absent();
    }

    @Override
    protected InfoDouble buildPresent( double value, InfoDoubleUnit unit )
    {
        return of(value, unit);
    }

    @Override
    protected InfoDouble buildPresent( double baseValue )
    {
        return ofBits(baseValue);
    }
}
