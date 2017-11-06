package net.varanus.util.text;


import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.base.Strings;

import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.collect.CommonPair;
import net.varanus.util.collect.Pair;
import net.varanus.util.lang.MoreObjects;
import net.varanus.util.lang.Unsigned;


/**
 * 
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class StringUtils
{
    private static final String GENERIC_PREFIX_PATTERN          = "^(\\+|-)?%s\\w+";
    private static final String GENERIC_UNSIGNED_PREFIX_PATTERN = "^\\+?%s\\w+";

    private static final int     BINARY_RADIX                   = 2;
    private static final String  BINARY_PREFIX                  = "0b";
    private static final Pattern BINARY_PREFIX_PATTERN          = Pattern.compile(
        String.format(GENERIC_PREFIX_PATTERN, BINARY_PREFIX));
    private static final Pattern BINARY_UNSIGNED_PREFIX_PATTERN = Pattern.compile(
        String.format(GENERIC_UNSIGNED_PREFIX_PATTERN, BINARY_PREFIX));

    private static final int     HEXADECIMAL_RADIX                   = 16;
    private static final String  HEXADECIMAL_PREFIX                  = "0x";
    private static final Pattern HEXADECIMAL_PREFIX_PATTERN          = Pattern.compile(
        String.format(GENERIC_PREFIX_PATTERN, HEXADECIMAL_PREFIX));
    private static final Pattern HEXADECIMAL_UNSIGNED_PREFIX_PATTERN = Pattern.compile(
        String.format(GENERIC_UNSIGNED_PREFIX_PATTERN, HEXADECIMAL_PREFIX));

    private static final int     OCTAL_RADIX                   = 8;
    private static final String  OCTAL_PREFIX                  = "0";
    private static final Pattern OCTAL_PREFIX_PATTERN          = Pattern.compile(
        String.format(GENERIC_PREFIX_PATTERN, OCTAL_PREFIX));
    private static final Pattern OCTAL_UNSIGNED_PREFIX_PATTERN = Pattern.compile(
        String.format(GENERIC_UNSIGNED_PREFIX_PATTERN, OCTAL_PREFIX));

    /**
     * Parses a byte value from an input string. The value's radix is
     * automatically inferred from the string's prefix (following a possible
     * sign character):
     * <ul>
     * <li>Radix 2 (binary) if the string begins with
     * [{@code '+'}|{@code '-'}]{@code '0b'}</li>
     * <li>Radix 16 (hexadecimal) if the string begins with
     * [{@code '+'}|{@code '-'}]{@code '0x'}</li>
     * <li>Radix 8 (octal) if the string begins with
     * [{@code '+'}|{@code '-'}]{@code '0'}</li>
     * <li>Radix 10 (decimal) otherwise</li>
     * </ul>
     * <p>
     * After the radix is inferred, the value is parsed using the
     * {@link Byte#parseByte(String, int)} method from the input string with the
     * radix characters excluded.
     * 
     * @param str
     *            The input string to be parsed
     * @return a parsed byte value
     * @exception NumberFormatException
     *                If the string does not contain a parsable {@code byte}.
     */
    public static byte parseByte( String str ) throws NumberFormatException
    {
        Objects.requireNonNull(str);
        String[] strHolder = {str};
        int radix = processNumberString(strHolder);
        return Byte.parseByte(strHolder[0], radix);
    }

    /**
     * Parses an unsigned byte value from an input string. The value's radix is
     * automatically inferred from the string's prefix (following a possible
     * sign character):
     * <ul>
     * <li>Radix 2 (binary) if the string begins with
     * [{@code '+'}]{@code '0b'}</li>
     * <li>Radix 16 (hexadecimal) if the string begins with
     * [{@code '+'}]{@code '0x'}</li>
     * <li>Radix 8 (octal) if the string begins with
     * [{@code '+'}]{@code '0'}</li>
     * <li>Radix 10 (decimal) otherwise</li>
     * </ul>
     * <p>
     * After the radix is inferred, the value is parsed using the
     * {@link Integer#parseUnsignedInt(String, int)} method from the input
     * string with the radix characters excluded, and checked if it is
     * within the bounds of an unsigned byte.
     * 
     * @param str
     *            The input string to be parsed
     * @return a parsed unsigned byte value
     * @exception NumberFormatException
     *                If the string does not contain a parsable {@code byte}.
     */
    public static byte parseUnsignedByte( String str ) throws NumberFormatException
    {
        Objects.requireNonNull(str);
        String[] strHolder = {str};
        int radix = processNumberString(strHolder);
        int u8 = Integer.parseUnsignedInt(strHolder[0], radix);
        if (u8 > Unsigned.MAX_BYTE)
            throw new NumberFormatException(
                String.format("String value %s exceeds range of unsigned byte.", str));
        return (byte)u8;
    }

    /**
     * Parses a short value from an input string. The value's radix is
     * automatically inferred from the string's prefix (following a possible
     * sign character):
     * <ul>
     * <li>Radix 2 (binary) if the string begins with
     * [{@code '+'}|{@code '-'}]{@code '0b'}</li>
     * <li>Radix 16 (hexadecimal) if the string begins with
     * [{@code '+'}|{@code '-'}]{@code '0x'}</li>
     * <li>Radix 8 (octal) if the string begins with
     * [{@code '+'}|{@code '-'}]{@code '0'}</li>
     * <li>Radix 10 (decimal) otherwise</li>
     * </ul>
     * <p>
     * After the radix is inferred, the value is parsed using the
     * {@link Short#parseShort(String, int)} method from the input string with
     * the radix characters excluded.
     * 
     * @param str
     *            The input string to be parsed
     * @return a parsed short value
     * @exception NumberFormatException
     *                If the string does not contain a parsable {@code short}.
     */
    public static short parseShort( String str ) throws NumberFormatException
    {
        Objects.requireNonNull(str);
        String[] strHolder = {str};
        int radix = processNumberString(strHolder);
        return Short.parseShort(strHolder[0], radix);
    }

    /**
     * Parses an unsigned short value from an input string. The value's radix is
     * automatically inferred from the string's prefix (following a possible
     * sign character):
     * <ul>
     * <li>Radix 2 (binary) if the string begins with
     * [{@code '+'}]{@code '0b'}</li>
     * <li>Radix 16 (hexadecimal) if the string begins with
     * [{@code '+'}]{@code '0x'}</li>
     * <li>Radix 8 (octal) if the string begins with
     * [{@code '+'}]{@code '0'}</li>
     * <li>Radix 10 (decimal) otherwise</li>
     * </ul>
     * <p>
     * After the radix is inferred, the value is parsed using the
     * {@link Integer#parseUnsignedInt(String, int)} method from the input
     * string with the radix characters excluded, and checked if it is
     * within the bounds of an unsigned short.
     * 
     * @param str
     *            The input string to be parsed
     * @return a parsed unsigned short value
     * @exception NumberFormatException
     *                If the string does not contain a parsable {@code short}.
     */
    public static short parseUnsignedShort( String str ) throws NumberFormatException
    {
        Objects.requireNonNull(str);
        String[] strHolder = {str};
        int radix = processNumberString(strHolder);
        int u16 = Integer.parseUnsignedInt(strHolder[0], radix);
        if (u16 > Unsigned.MAX_SHORT)
            throw new NumberFormatException(
                String.format("String value %s exceeds range of unsigned short.", str));
        return (short)u16;
    }

    /**
     * Parses an int value from an input string. The value's radix is
     * automatically inferred from the string's prefix (following a possible
     * sign character):
     * <ul>
     * <li>Radix 2 (binary) if the string begins with
     * [{@code '+'}|{@code '-'}]{@code '0b'}</li>
     * <li>Radix 16 (hexadecimal) if the string begins with
     * [{@code '+'}|{@code '-'}]{@code '0x'}</li>
     * <li>Radix 8 (octal) if the string begins with
     * [{@code '+'}|{@code '-'}]{@code '0'}</li>
     * <li>Radix 10 (decimal) otherwise</li>
     * </ul>
     * <p>
     * After the radix is inferred, the value is parsed using the
     * {@link Integer#parseInt(String, int)} method from the input string with
     * the radix characters excluded.
     * 
     * @param str
     *            The input string to be parsed
     * @return a parsed int value
     * @exception NumberFormatException
     *                If the string does not contain a parsable {@code int}.
     */
    public static int parseInt( String str ) throws NumberFormatException
    {
        Objects.requireNonNull(str);
        String[] strHolder = {str};
        int radix = processNumberString(strHolder);
        return Integer.parseInt(strHolder[0], radix);
    }

    /**
     * Parses an unsigned int value from an input string. The value's radix is
     * automatically inferred from the string's prefix (following a possible
     * sign character):
     * <ul>
     * <li>Radix 2 (binary) if the string begins with
     * [{@code '+'}]{@code '0b'}</li>
     * <li>Radix 16 (hexadecimal) if the string begins with
     * [{@code '+'}]{@code '0x'}</li>
     * <li>Radix 8 (octal) if the string begins with
     * [{@code '+'}]{@code '0'}</li>
     * <li>Radix 10 (decimal) otherwise</li>
     * </ul>
     * <p>
     * After the radix is inferred, the value is parsed using the
     * {@link Integer#parseUnsignedInt(String, int)} method from the input
     * string with the radix characters excluded.
     * 
     * @param str
     *            The input string to be parsed
     * @return a parsed unsigned int value
     * @exception NumberFormatException
     *                If the string does not contain a parsable {@code int}.
     */
    public static int parseUnsignedInt( String str ) throws NumberFormatException
    {
        Objects.requireNonNull(str);
        String[] strHolder = {str};
        int radix = processUnsignedNumberString(strHolder);
        return Integer.parseUnsignedInt(strHolder[0], radix);
    }

    /**
     * Parses a long value from an input string. The value's radix is
     * automatically inferred from the string's prefix (following a possible
     * sign character):
     * <ul>
     * <li>Radix 2 (binary) if the string begins with
     * [{@code '+'}|{@code '-'}]{@code '0b'}</li>
     * <li>Radix 16 (hexadecimal) if the string begins with
     * [{@code '+'}|{@code '-'}]{@code '0x'}</li>
     * <li>Radix 8 (octal) if the string begins with
     * [{@code '+'}|{@code '-'}]{@code '0'}</li>
     * <li>Radix 10 (decimal) otherwise</li>
     * </ul>
     * <p>
     * After the radix is inferred, the value is parsed using the
     * {@link Long#parseLong(String, int)} method from the input string with
     * the radix characters excluded.
     * 
     * @param str
     *            The input string to be parsed
     * @return a parsed long value
     * @exception NumberFormatException
     *                If the string does not contain a parsable {@code long}.
     */
    public static long parseLong( String str ) throws NumberFormatException
    {
        Objects.requireNonNull(str);
        String[] strHolder = {str};
        int radix = processNumberString(strHolder);
        return Long.parseLong(strHolder[0], radix);
    }

    /**
     * Parses an unsigned long value from an input string. The value's radix is
     * automatically inferred from the string's prefix (following a possible
     * sign character):
     * <ul>
     * <li>Radix 2 (binary) if the string begins with
     * [{@code '+'}]{@code '0b'}</li>
     * <li>Radix 16 (hexadecimal) if the string begins with
     * [{@code '+'}]{@code '0x'}</li>
     * <li>Radix 8 (octal) if the string begins with
     * [{@code '+'}]{@code '0'}</li>
     * <li>Radix 10 (decimal) otherwise</li>
     * </ul>
     * <p>
     * After the radix is inferred, the value is parsed using the
     * {@link Long#parseUnsignedLong(String, int)} method from the input
     * string with the radix characters excluded.
     * 
     * @param str
     *            The input string to be parsed
     * @return a parsed unsigned long value
     * @exception NumberFormatException
     *                If the string does not contain a parsable {@code long}.
     */
    public static long parseUnsignedLong( String str ) throws NumberFormatException
    {
        Objects.requireNonNull(str);
        String[] strHolder = {str};
        int radix = processUnsignedNumberString(strHolder);
        return Long.parseUnsignedLong(strHolder[0], radix);
    }

    private static int processNumberString( String[] strHolder )
    {
        final String str = strHolder[0];

        if (BINARY_PREFIX_PATTERN.matcher(str).matches()) {
            strHolder[0] = str.replaceFirst(BINARY_PREFIX, "");
            return BINARY_RADIX;
        }
        else if (HEXADECIMAL_PREFIX_PATTERN.matcher(str).matches()) {
            strHolder[0] = str.replaceFirst(HEXADECIMAL_PREFIX, "");
            return HEXADECIMAL_RADIX;
        }
        else if (OCTAL_PREFIX_PATTERN.matcher(str).matches()) {
            strHolder[0] = str.replaceFirst(OCTAL_PREFIX, "");
            return OCTAL_RADIX;
        }
        else {
            return 10;
        }
    }

    private static int processUnsignedNumberString( String[] strHolder )
    {
        final String str = strHolder[0];

        if (BINARY_UNSIGNED_PREFIX_PATTERN.matcher(str).matches()) {
            strHolder[0] = str.replaceFirst(BINARY_PREFIX, "");
            return BINARY_RADIX;
        }
        else if (HEXADECIMAL_UNSIGNED_PREFIX_PATTERN.matcher(str).matches()) {
            strHolder[0] = str.replaceFirst(HEXADECIMAL_PREFIX, "");
            return HEXADECIMAL_RADIX;
        }
        else if (OCTAL_UNSIGNED_PREFIX_PATTERN.matcher(str).matches()) {
            strHolder[0] = str.replaceFirst(OCTAL_PREFIX, "");
            return OCTAL_RADIX;
        }
        else {
            return 10;
        }
    }

    @SuppressWarnings( "unchecked" )
    public static <T extends Number> T convertToNumber( String value, Class<T> numberClass )
        throws NumberFormatException
    {
        MoreObjects.requireNonNull(value, "value", numberClass, "numberClass");

        if (canBeCastTo(numberClass, Byte.class) || canBeCastTo(numberClass, Byte.TYPE)) {
            return (T)Byte.valueOf(parseByte(value));
        }
        else if (canBeCastTo(numberClass, Short.class) || canBeCastTo(numberClass, Short.TYPE)) {
            return (T)Short.valueOf(parseShort(value));
        }
        else if (canBeCastTo(numberClass, Integer.class) || canBeCastTo(numberClass, Integer.TYPE)) {
            return (T)Integer.valueOf(parseInt(value));
        }
        else if (canBeCastTo(numberClass, Long.class) || canBeCastTo(numberClass, Long.TYPE)) {
            return (T)Long.valueOf(parseLong(value));
        }
        else if (canBeCastTo(numberClass, Float.class) || canBeCastTo(numberClass, Float.TYPE)) {
            return (T)Float.valueOf(value);
        }
        else if (canBeCastTo(numberClass, Double.class) || canBeCastTo(numberClass, Double.TYPE)) {
            return (T)Double.valueOf(value);
        }
        else if (canBeCastTo(numberClass, BigInteger.class)) {
            return (T)new BigInteger(value);
        }
        else if (canBeCastTo(numberClass, BigDecimal.class)) {
            return (T)new BigDecimal(value);
        }
        else {
            throw new UnsupportedOperationException("Number class not supported");
        }
    }

    @SuppressWarnings( "unchecked" )
    public static <T extends Number> T convertToNumber( String value, Class<T> numberClass, int radix )
        throws NumberFormatException
    {
        MoreObjects.requireNonNull(value, "value", numberClass, "numberClass");
        if (radix < Character.MIN_RADIX || radix > Character.MAX_RADIX)
            throw new IllegalArgumentException("invalid radix");

        if (canBeCastTo(numberClass, Byte.class) || canBeCastTo(numberClass, Byte.TYPE)) {
            return (T)Byte.valueOf(value, radix);
        }
        else if (canBeCastTo(numberClass, Short.class) || canBeCastTo(numberClass, Short.TYPE)) {
            return (T)Short.valueOf(value, radix);
        }
        else if (canBeCastTo(numberClass, Integer.class) || canBeCastTo(numberClass, Integer.TYPE)) {
            return (T)Integer.valueOf(value, radix);
        }
        else if (canBeCastTo(numberClass, Long.class) || canBeCastTo(numberClass, Long.TYPE)) {
            return (T)Long.valueOf(value, radix);
        }
        else if (canBeCastTo(numberClass, BigInteger.class)) {
            return (T)new BigInteger(value, radix);
        }
        else {
            throw new UnsupportedOperationException("Number class not supported");
        }
    }

    private static boolean canBeCastTo( Class<?> zuper, Class<?> clazz )
    {
        return zuper.isAssignableFrom(clazz);
    }

    public static Boolean convertToBoolean( String value ) throws IllegalArgumentException
    {
        switch (value.toLowerCase()) {
            case "true":
            case "yes":
            case "1":
                return true;

            case "false":
            case "no":
            case "0":
                return false;

            default:
                throw new IllegalArgumentException(
                    String.format("malformed boolean value %s", value));
        }
    }

    public static String parseStringLiteral( String s ) throws IllegalArgumentException
    {
        if ((s.startsWith("'") && s.endsWith("'")) || (s.startsWith("\"") && s.endsWith("\"")))
            return s.substring(1, s.length() - 1);
        else
            throw new IllegalArgumentException("not a valid string literal");
    }

    private static final String  DEF_PAIR_PREFIX       = "(";
    private static final String  DEF_PAIR_SUFFIX       = ")";
    private static final String  DEF_COLLECTION_PREFIX = "[";
    private static final String  DEF_COLLECTION_SUFFIX = "]";
    private static final Pattern DEF_SPLIT_PATTERN     = Pattern.compile(",");

    public static <A, B, P extends Pair<A, B>> P parsePair( String str,
                                                            Function<String, A> firstMapper,
                                                            Function<String, B> secondMapper,
                                                            BiFunction<A, B, P> pairFactory,
                                                            Pattern splitRegex,
                                                            String prefix,
                                                            String suffix )
        throws IllegalArgumentException
    {
        MoreObjects.requireNonNull(
            firstMapper, "firstMapper",
            secondMapper, "secondMapper",
            pairFactory, "pairFactory",
            splitRegex, "splitRegex",
            prefix, "prefix",
            suffix, "suffix");

        if (str.length() >= (prefix.length() + suffix.length()) && str.startsWith(prefix) && str.endsWith(suffix)) {
            str = str.substring(prefix.length(), str.length() - suffix.length());
            String[] split = splitRegex.split(str);
            if (split.length == 2) {
                A first = firstMapper.apply(split[0]);
                B second = secondMapper.apply(split[1]);
                return pairFactory.apply(first, second);
            }
            else {
                throw new IllegalArgumentException(
                    String.format("expected exactly 2 elements separated by regex '%s' but found %d instead",
                        splitRegex.pattern(), split.length));
            }
        }
        else {
            throw new IllegalArgumentException(String.format("pair elements must be within \"%s\" and \"%s\"",
                prefix, suffix));
        }
    }

    public static <A, B, P extends Pair<A, B>> P parsePair( String str,
                                                            Function<String, A> firstMapper,
                                                            Function<String, B> secondMapper,
                                                            BiFunction<A, B, P> pairFactory,
                                                            String splitRegex,
                                                            String prefix,
                                                            String suffix )
        throws IllegalArgumentException
    {
        return parsePair(str, firstMapper, secondMapper, pairFactory, Pattern.compile(splitRegex), prefix, suffix);
    }

    public static <A, B, P extends Pair<A, B>> P parsePair( String str,
                                                            Function<String, A> firstMapper,
                                                            Function<String, B> secondMapper,
                                                            BiFunction<A, B, P> pairFactory )
        throws IllegalArgumentException
    {
        return parsePair(str, firstMapper, secondMapper, pairFactory,
            DEF_SPLIT_PATTERN, DEF_PAIR_PREFIX, DEF_PAIR_SUFFIX);
    }

    public static CommonPair<String> parsePair( String str, Pattern splitRegex, String prefix, String suffix )
        throws IllegalArgumentException
    {
        return parsePair(str, Function.identity(), Function.identity(), CommonPair::of, splitRegex, prefix, suffix);
    }

    public static CommonPair<String> parsePair( String str, String splitRegex, String prefix, String suffix )
        throws IllegalArgumentException
    {
        return parsePair(str, Function.identity(), Function.identity(), CommonPair::of, splitRegex, prefix, suffix);
    }

    public static CommonPair<String> parsePair( String str ) throws IllegalArgumentException
    {
        return parsePair(str, DEF_SPLIT_PATTERN, DEF_PAIR_PREFIX, DEF_PAIR_SUFFIX);
    }

    public static <T, C extends Collection<T>> C parseCollection( String str,
                                                                  Function<String, T> mapper,
                                                                  Supplier<C> colFactory,
                                                                  Pattern splitRegex,
                                                                  String prefix,
                                                                  String suffix )
        throws IllegalArgumentException
    {
        MoreObjects.requireNonNull(
            mapper, "mapper",
            colFactory, "colFactory",
            splitRegex, "splitRegex",
            prefix, "prefix",
            suffix, "suffix");

        if (str.length() >= (prefix.length() + suffix.length()) && str.startsWith(prefix) && str.endsWith(suffix)) {
            str = str.substring(prefix.length(), str.length() - suffix.length());
            if (str.isEmpty())
                return colFactory.get();
            else {
                // include trailing empty strings
                String[] split = splitRegex.split(str, -1);
                return Stream.of(split)
                    .map(mapper)
                    .collect(Collectors.toCollection(colFactory));
            }
        }
        else {
            throw new IllegalArgumentException(String.format("collection elements must be within \"%s\" and \"%s\"",
                prefix, suffix));
        }
    }

    public static <T, C extends Collection<T>> C parseCollection( String str,
                                                                  Function<String, T> mapper,
                                                                  Supplier<C> colFactory,
                                                                  String splitRegex,
                                                                  String prefix,
                                                                  String suffix )
        throws IllegalArgumentException
    {
        return parseCollection(str, mapper, colFactory, Pattern.compile(splitRegex), prefix, suffix);
    }

    public static <T, C extends Collection<T>> C parseCollection( String str,
                                                                  Function<String, T> mapper,
                                                                  Supplier<C> colFactory )
        throws IllegalArgumentException
    {
        return parseCollection(str, mapper, colFactory,
            DEF_SPLIT_PATTERN, DEF_COLLECTION_PREFIX, DEF_COLLECTION_SUFFIX);
    }

    public static <C extends Collection<String>> C parseCollection( String str,
                                                                    Supplier<C> colFactory,
                                                                    Pattern splitRegex,
                                                                    String prefix,
                                                                    String suffix )
        throws IllegalArgumentException
    {
        return parseCollection(str, Function.identity(), colFactory, splitRegex, prefix, suffix);
    }

    public static <C extends Collection<String>> C parseCollection( String str,
                                                                    Supplier<C> colFactory,
                                                                    String splitRegex,
                                                                    String prefix,
                                                                    String suffix )
        throws IllegalArgumentException
    {
        return parseCollection(str, Function.identity(), colFactory, splitRegex, prefix, suffix);
    }

    public static <C extends Collection<String>> C parseCollection( String str, Supplier<C> colFactory )
        throws IllegalArgumentException
    {
        return parseCollection(str, colFactory, DEF_SPLIT_PATTERN, DEF_COLLECTION_PREFIX, DEF_COLLECTION_SUFFIX);
    }

    public static <T> List<T> parseList( String str,
                                         Function<String, T> mapper,
                                         Pattern splitRegex,
                                         String prefix,
                                         String suffix )
        throws IllegalArgumentException
    {
        try {
            return parseCollection(str, mapper, ArrayList::new, splitRegex, prefix, suffix);
        }
        catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                String.format("error while parsing list: %s", e.getMessage()),
                e);
        }
    }

    public static <T> List<T> parseList( String str,
                                         Function<String, T> mapper,
                                         String splitRegex,
                                         String prefix,
                                         String suffix )
        throws IllegalArgumentException
    {
        try {
            return parseCollection(str, mapper, ArrayList::new, splitRegex, prefix, suffix);
        }
        catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                String.format("error while parsing list: %s", e.getMessage()),
                e);
        }
    }

    public static <T> List<T> parseList( String str, Function<String, T> mapper ) throws IllegalArgumentException
    {
        return parseList(str, mapper, DEF_SPLIT_PATTERN, DEF_COLLECTION_PREFIX, DEF_COLLECTION_SUFFIX);
    }

    public static List<String> parseList( String str,
                                          Pattern splitRegex,
                                          String prefix,
                                          String suffix )
        throws IllegalArgumentException
    {
        return parseList(str, Function.identity(), splitRegex, prefix, suffix);
    }

    public static List<String> parseList( String str,
                                          String splitRegex,
                                          String prefix,
                                          String suffix )
        throws IllegalArgumentException
    {
        return parseList(str, Function.identity(), splitRegex, prefix, suffix);
    }

    public static List<String> parseList( String str ) throws IllegalArgumentException
    {
        return parseList(str, Function.identity());
    }

    public static <T> Set<T> parseSet( String str,
                                       Function<String, T> mapper,
                                       Pattern splitRegex,
                                       String prefix,
                                       String suffix )
        throws IllegalArgumentException
    {
        try {
            return parseCollection(str, mapper, LinkedHashSet::new, splitRegex, prefix, suffix);
        }
        catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                String.format("error while parsing list: %s", e.getMessage()),
                e);
        }
    }

    public static <T> Set<T> parseSet( String str,
                                       Function<String, T> mapper,
                                       String splitRegex,
                                       String prefix,
                                       String suffix )
        throws IllegalArgumentException
    {
        try {
            return parseCollection(str, mapper, LinkedHashSet::new, splitRegex, prefix, suffix);
        }
        catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                String.format("error while parsing list: %s", e.getMessage()),
                e);
        }
    }

    public static <T> Set<T> parseSet( String str, Function<String, T> mapper ) throws IllegalArgumentException
    {
        return parseSet(str, mapper, DEF_SPLIT_PATTERN, DEF_COLLECTION_PREFIX, DEF_COLLECTION_SUFFIX);
    }

    public static Set<String> parseSet( String str,
                                        Pattern splitRegex,
                                        String prefix,
                                        String suffix )
        throws IllegalArgumentException
    {
        return parseSet(str, Function.identity(), splitRegex, prefix, suffix);
    }

    public static Set<String> parseSet( String str,
                                        String splitRegex,
                                        String prefix,
                                        String suffix )
        throws IllegalArgumentException
    {
        return parseSet(str, Function.identity(), splitRegex, prefix, suffix);
    }

    public static Set<String> parseSet( String str ) throws IllegalArgumentException
    {
        return parseSet(str, Function.identity());
    }

    private final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    /*
     * Copied from StackOverflow at: http://stackoverflow.com/a/9855338
     */
    public static String bytesToHex( byte[] bytes )
    {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static String getExceptionString( Throwable t )
    {
        return Objects.toString(t.getMessage(), t.toString());
    }

    public static String getExceptionCauseString( Throwable t )
    {
        return getExceptionString(Objects.requireNonNull(t.getCause(), "null cause"));
    }

    public static StringBuilder setChars( StringBuilder sb, int fromIndex, int toIndex, char ch )
    {
        if (fromIndex < 0) {
            throw new IllegalArgumentException("fromIndex < 0");
        }
        else if (fromIndex > toIndex) {
            throw new IllegalArgumentException("fromIndex > toIndex");
        }
        else if (toIndex > fromIndex) {
            sb.setLength(toIndex);
            for (int i = fromIndex; i < toIndex; i++) {
                sb.setCharAt(i, ch);
            }
        }

        return sb;
    }

    public static String leftJust( String s, int width )
    {
        return leftJust(s, width, ' ');
    }

    public static String leftJust( String s, int width, char fillChar )
    {
        return Strings.padEnd(s, width, fillChar);
    }

    public static String rightJust( String s, int width )
    {
        return rightJust(s, width, ' ');
    }

    public static String rightJust( String s, int width, char fillChar )
    {
        return Strings.padStart(s, width, fillChar);
    }

    public static StringJoiner addAll( StringJoiner joiner, Stream<?> stream )
    {
        stream.forEachOrdered(e -> joiner.add(Objects.toString(e, null)));
        return joiner;
    }

    public static StringJoiner addAll( StringJoiner joiner, Iterable<?> elements )
    {
        for (Object e : elements) {
            joiner.add(Objects.toString(e, null));
        }

        return joiner;
    }

    @SafeVarargs
    @SuppressWarnings( "varargs" )
    public static <T> StringJoiner addAll( StringJoiner joiner, T... elements )
    {
        return addAll(joiner, Arrays.asList(elements));
    }

    public static StringJoiner addAll( StringJoiner joiner, Map<?, ?> map )
    {
        return addAll(joiner, map.entrySet());
    }

    public static String joinAll( CharSequence delimiter, Stream<?> stream )
    {
        return addAll(new StringJoiner(delimiter), stream).toString();
    }

    public static String joinAllPS( CharSequence delimiter, CharSequence prefix, CharSequence suffix, Stream<?> stream )
    {
        return addAll(new StringJoiner(delimiter, prefix, suffix), stream).toString();
    }

    public static String joinAll( CharSequence delimiter, Iterable<?> elements )
    {
        return addAll(new StringJoiner(delimiter), elements).toString();
    }

    public static String joinAllPS( CharSequence delimiter,
                                    CharSequence prefix,
                                    CharSequence suffix,
                                    Iterable<?> elements )
    {
        return addAll(new StringJoiner(delimiter, prefix, suffix), elements).toString();
    }

    @SafeVarargs
    @SuppressWarnings( "varargs" )
    public static <T> String joinAll( CharSequence delimiter, T... elements )
    {
        return addAll(new StringJoiner(delimiter), elements).toString();
    }

    @SafeVarargs
    @SuppressWarnings( "varargs" )
    public static <T> String joinAllPS( CharSequence delimiter,
                                        CharSequence prefix,
                                        CharSequence suffix,
                                        T... elements )
    {
        return addAll(new StringJoiner(delimiter, prefix, suffix), elements).toString();
    }

    public static String joinAll( CharSequence delimiter, Map<?, ?> map )
    {
        return addAll(new StringJoiner(delimiter), map).toString();
    }

    public static String joinAllPS( CharSequence delimiter,
                                    CharSequence prefix,
                                    CharSequence suffix,
                                    Map<?, ?> map )
    {
        return addAll(new StringJoiner(delimiter, prefix, suffix), map).toString();
    }

    public static StringJoiner linesJoiner()
    {
        return new StringJoiner(System.lineSeparator());
    }

    public static StringJoiner linesJoiner( CharSequence prefix, CharSequence suffix )
    {
        return new StringJoiner(System.lineSeparator(), prefix, suffix);
    }

    public static String joinAllInLines( Stream<?> stream )
    {
        return addAll(linesJoiner(), stream).toString();
    }

    public static String joinAllInLinesPS( CharSequence prefix, CharSequence suffix, Stream<?> stream )
    {
        return addAll(linesJoiner(prefix, suffix), stream).toString();
    }

    public static String joinAllInLines( Iterable<?> elements )
    {
        return addAll(linesJoiner(), elements).toString();
    }

    public static String joinAllInLinesPS( CharSequence prefix, CharSequence suffix, Iterable<?> elements )
    {
        return addAll(linesJoiner(prefix, suffix), elements).toString();
    }

    @SafeVarargs
    @SuppressWarnings( "varargs" )
    public static <T> String joinAllInLines( T... elements )
    {
        return addAll(linesJoiner(), elements).toString();
    }

    @SafeVarargs
    @SuppressWarnings( "varargs" )
    public static <T> String joinAllInLinesPS( CharSequence prefix, CharSequence suffix, T... elements )
    {
        return addAll(linesJoiner(prefix, suffix), elements).toString();
    }

    public static String joinAllInLines( Map<?, ?> map )
    {
        return addAll(linesJoiner(), map).toString();
    }

    public static String joinAllInLinesPS( CharSequence prefix, CharSequence suffix, Map<?, ?> map )
    {
        return addAll(linesJoiner(prefix, suffix), map).toString();
    }

    private StringUtils()
    {
        // not used
    }
}
