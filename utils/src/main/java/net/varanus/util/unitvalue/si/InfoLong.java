package net.varanus.util.unitvalue.si;


import static net.varanus.util.unitvalue.si.InfoLongUnit.BITS;
import static net.varanus.util.unitvalue.si.InfoLongUnit.BYTES;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.functional.PossibleLong;
import net.varanus.util.text.StringUtils;


/**
 * 
 */
@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class InfoLong extends SIMultipleLongValue<InfoLongUnit, InfoLong>
{
    private static final InfoLong ABSENT = new InfoLong(0, false);
    public static final InfoLong  ZERO   = new InfoLong(0, true);

    public static InfoLong absent()
    {
        return ABSENT;
    }

    public static InfoLong ofBits( long valBits )
    {
        return new InfoLong(valBits, true);
    }

    public static InfoLong ofBytes( long valBytes )
    {
        return new InfoLong(BYTES.toBits(valBytes), true);
    }

    public static InfoLong of( long val, InfoLongUnit unit )
    {
        return new InfoLong(unit.toBits(val), true);
    }

    public static InfoLong ofPossibleBits( PossibleLong valBits )
    {
        return valBits.mapToObj(InfoLong::ofBits).orElse(absent());
    }

    public static InfoLong ofPossibleBytes( PossibleLong valBytes )
    {
        return valBytes.mapToObj(InfoLong::ofBytes).orElse(absent());
    }

    public static InfoLong ofPossible( PossibleLong val, InfoLongUnit unit )
    {
        return val.mapToObj(v -> InfoLong.of(v, unit)).orElse(absent());
    }

    public static InfoLong parseBits( String s )
    {
        return ofBits(StringUtils.parseLong(s));
    }

    public static InfoLong parseBytes( String s )
    {
        return ofBytes(StringUtils.parseLong(s));
    }

    public static InfoLong parse( String s, InfoLongUnit unit )
    {
        return of(StringUtils.parseLong(s), unit);
    }

    private InfoLong( long bitsValue, boolean isPresent )
    {
        super(bitsValue, isPresent);
    }

    public long inBits()
    {
        return toBaseValue();
    }

    public PossibleLong asPossibleInBits()
    {
        return asPossibleBase();
    }

    public long inBytes()
    {
        return in(BYTES);
    }

    public PossibleLong asPossibleInBytes()
    {
        return asPossible(BYTES);
    }

    public InfoDouble asDouble()
    {
        return isPresent() ? InfoDouble.ofBits(inBits())
                           : InfoDouble.absent();
    }

    @Override
    public boolean equals( Object other )
    {
        return (other instanceof InfoLong)
               && super.equals(other);
    }

    public boolean equals( InfoLong other )
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

    public String toByteString()
    {
        return toSmartString(BYTES);
    }

    public String toStringAnd( Object unitSuffix )
    {
        return toBitStringAnd(unitSuffix);
    }

    public String toBitStringAnd( Object unitSuffix )
    {
        return toSmartStringAnd(BITS, unitSuffix);
    }

    public String toByteStringAnd( Object unitSuffix )
    {
        return toSmartStringAnd(BYTES, unitSuffix);
    }

    @Override
    protected InfoLongUnit baseUnit()
    {
        return BITS;
    }

    @Override
    protected InfoLong buildAbsent()
    {
        return absent();
    }

    @Override
    protected InfoLong buildPresent( long value, InfoLongUnit unit )
    {
        return of(value, unit);
    }

    @Override
    protected InfoLong buildPresent( long baseValue )
    {
        return ofBits(baseValue);
    }
}
