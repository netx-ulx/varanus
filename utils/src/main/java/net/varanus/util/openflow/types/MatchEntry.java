package net.varanus.util.openflow.types;


import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.Masked;
import org.projectfloodlight.openflow.types.OFValueType;

import com.google.common.collect.ImmutableList;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.collect.builder.ImmutableListBuilder;
import net.varanus.util.functional.StreamUtils;
import net.varanus.util.openflow.MatchFieldUtils;
import net.varanus.util.openflow.MatchUtils;
import net.varanus.util.openflow.PacketBits;
import net.varanus.util.openflow.PacketBits.BitField;
import net.varanus.util.openflow.types.BitMatch.BitMasked;


/**
 * @param <T>
 */
@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class MatchEntry<T extends OFValueType<T>> implements Comparable<MatchEntry<?>>
{
    private static final String  ENTRY_FMT_DESCRIPT  = "<FIELD>=<VALUE>";
    private static final Pattern ENTRY_FMT_PATTERN   = Pattern.compile("[^=]+=[^=]+");
    private static final int     ENTRY_SPLIT_LIMIT   = 2;
    private static final Pattern ENTRY_SPLIT_PATTERN = Pattern.compile("=");

    public static <T extends OFValueType<T>> MatchEntry<T> ofExact( MatchField<T> field, T value )
    {
        return new MatchEntry<>(
            Objects.requireNonNull(field),
            Objects.requireNonNull(value),
            MatchFieldUtils.getExactMask(value),
            MaskType.NONE);
    }

    public static <T extends OFValueType<T>> MatchEntry<T> ofMasked( MatchField<T> field, Masked<T> masked )
    {
        return ofMasked(field, masked.getValue(), masked.getMask());
    }

    public static <T extends OFValueType<T>> MatchEntry<T> ofMasked( MatchField<T> field, T value, T mask )
    {
        return new MatchEntry<>(
            Objects.requireNonNull(field),
            Objects.requireNonNull(value),
            Objects.requireNonNull(mask),
            MaskType.PARTIAL);
    }

    public static <T extends OFValueType<T>> MatchEntry<T> ofWildcard( MatchField<T> field )
    {
        return new MatchEntry<>(
            Objects.requireNonNull(field),
            MatchFieldUtils.getDefaultValue(field),
            MatchFieldUtils.getWildcardMask(field),
            MaskType.FULL);
    }

    public static <T extends OFValueType<T>> MatchEntry<T> fromMatch( Match match, MatchField<T> field )
    {
        Objects.requireNonNull(field);

        if (match.supports(field) && match.isExact(field)) {
            return ofExact(field, match.get(field));
        }
        else if (match.supports(field) && match.isFullyWildcarded(field)) {
            return ofWildcard(field);
        }
        else if (match.supportsMasked(field) && match.isPartiallyMasked(field)) {
            return ofMasked(field, match.getMasked(field));
        }
        else {
            throw new UnsupportedOperationException(
                String.format("match does not support field %s", field.id));
        }
    }

    public static Stream<MatchEntry<?>> streamEntries( Match match )
    {
        Stream<MatchField<?>> fields = StreamUtils.toSequentialStream(match.getMatchFields());
        Stream<MatchEntry<?>> entries = fields.map(( field ) -> fromMatch(match, field));
        return entries.sorted();
    }

    public static ImmutableList<MatchEntry<?>> listEntries( Match match )
    {
        return ImmutableListBuilder.<MatchEntry<?>>create().addEach(streamEntries(match)).build();
    }

    public static MatchEntry<?> parse( String str ) throws IllegalArgumentException
    {
        // be nice to the user and accept leading or trailing whitespace
        str = str.trim();

        if (ENTRY_FMT_PATTERN.matcher(str).matches()) {
            String[] split = ENTRY_SPLIT_PATTERN.split(str, ENTRY_SPLIT_LIMIT);
            try {
                MatchField<?> field = MatchFieldUtils.parseField(split[0].trim());
                String something = split[1].trim();
                if (something.equals("*"))
                    return ofWildcard(field);
                else if (MatchFieldUtils.isMaskedString(something))
                    return parseMasked(something, field);
                else
                    return parseExact(something, field);
            }
            catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                    String.format("error while parsing match entry: %s", e.getMessage()),
                    e);
            }
        }
        else {
            throw new IllegalArgumentException(
                String.format(
                    "malformed match entry (must have the form '%s')",
                    ENTRY_FMT_DESCRIPT));
        }
    }

    private static <T extends OFValueType<T>> MatchEntry<T> parseExact( String str, MatchField<T> field )
        throws IllegalArgumentException
    {
        try {
            T value = MatchFieldUtils.parseValue(str, field);
            return ofExact(field, value);
        }
        catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                String.format("invalid value for field %s: %s", field.id, e.getMessage()),
                e);
        }
    }

    private static <T extends OFValueType<T>> MatchEntry<T> parseMasked( String str, MatchField<T> field )
        throws IllegalArgumentException
    {
        try {
            Masked<T> masked = MatchFieldUtils.parseMasked(str, field);
            return ofMasked(field, masked);
        }
        catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                String.format("invalid masked value for field %s: %s", field.id, e.getMessage()),
                e);
        }
    }

    private final MatchField<T> field;
    private final Masked<T>     masked;
    private final MaskType      maskType;

    private MatchEntry( MatchField<T> field, T value, T mask, MaskType maskType )
    {
        this.field = field;
        this.masked = MatchUtils.asMasked(field, value, mask, maskType);
        this.maskType = maskType;
    }

    public final MatchField<T> getField()
    {
        return field;
    }

    public T getValue()
    {
        return masked.getValue();
    }

    public T getMask()
    {
        return masked.getMask();
    }

    public Masked<T> getMasked()
    {
        return masked;
    }

    public MaskType getMaskType()
    {
        return maskType;
    }

    public boolean isExact()
    {
        return maskType.isExact();
    }

    public boolean isPartiallyMasked()
    {
        return maskType.isPartiallyMasked();
    }

    public boolean isWildcard()
    {
        return maskType.isWildcard();
    }

    public void addToBuilder( Match.Builder builder )
    {
        if (this.isExact()) {
            builder.setExact(field, getValue());
        }
        else if (this.isPartiallyMasked()) {
            builder.setMasked(field, getValue(), getMask());
        }
        else { // if (this.isWildcard()) {
            builder.wildcard(field);
        }
    }

    public boolean matches( T value )
    {
        return masked.matches(Objects.requireNonNull(value));
    }

    public boolean matchesOnly( T value )
    {
        return value.equals(getValue()) && this.isExact();
    }

    public boolean matchesAllOf( Masked<T> masked )
    {
        return this.matchesAllOf(masked.getValue(), masked.getMask());
    }

    public boolean matchesAllOf( T value, T mask )
    {
        return this.matches(value) && mask.applyMask(getMask()).equals(getMask());
    }

    public String getValueString()
    {
        return valueForString().toString();
    }

    public BitMasked getBitMasked( Match match )
    {
        BitField bitField = PacketBits.bitFieldFor(field, match)
            .orElseThrow(() -> new IllegalArgumentException("cannot obtain a bit field from the provided match"));

        if (this.isExact()) {
            return BitMasked.ofExact(bitField,
                MatchFieldUtils.toBytes(getValue()));
        }
        else if (this.isPartiallyMasked()) {
            return BitMasked.of(bitField,
                MatchFieldUtils.toBytes(getValue()),
                MatchFieldUtils.toBytes(getMask()));
        }
        else { // if (this.isWildcard()) {
            return BitMasked.ofWildcard(bitField);
        }
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public int compareTo( MatchEntry<?> other )
    {
        int res = this.getField().id.compareTo(other.getField().id);
        if (res != 0)
            return res;
        else
            return this.masked.compareTo(((MatchEntry<T>)other).masked);
    }

    @Override
    public boolean equals( Object other )
    {
        return (other instanceof MatchEntry<?>)
               && this.equals((MatchEntry<?>)other);
    }

    public boolean equals( MatchEntry<?> other )
    {
        return (other != null)
               && this.field.id.equals(other.field.id)
               && this.masked.equals(other.masked);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(field.id, masked);
    }

    @Override
    public String toString()
    {
        return String.format("%s=%s",
            field.id.name().toLowerCase(),
            valueForString());
    }

    private Object valueForString()
    {
        return this.isExact() ? getValue() : (this.isPartiallyMasked() ? masked : "*");
    }
}
