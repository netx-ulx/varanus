package net.varanus.util.functional;


import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.BiPredicate;
import java.util.function.DoublePredicate;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.function.IntUnaryOperator;
import java.util.function.LongPredicate;
import java.util.function.LongUnaryOperator;
import java.util.function.Predicate;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import javax.annotation.Nullable;


/**
 * 
 */
public final class OptionalUtils
{
    public static <T> int compare( Optional<T> opt1, Optional<T> opt2, Comparator<? super T> comparator )
    {
        // absent values are less than present values
        return Comparator.comparing(( Optional<T> opt ) -> opt.orElse(null),
            Comparator.nullsFirst(Objects.requireNonNull(comparator)))
            .compare(opt1, opt2);
    }

    public static <T extends Comparable<? super T>> int compare( Optional<T> opt1, Optional<T> opt2 )
    {
        return compare(opt1, opt2, Comparator.naturalOrder());
    }

    public static int compare( OptionalInt opt1, OptionalInt opt2 )
    {
        // absent values are less than present values

        if (opt1.isPresent() && opt2.isPresent())
            return Integer.compare(opt1.getAsInt(), opt2.getAsInt());
        else if (opt1.isPresent()) // && !opt2.isPresent()
            return 1;
        else if (opt2.isPresent()) // && !opt1.isPresent()
            return -1;
        else
            return 0;
    }

    public static int compare( OptionalLong opt1, OptionalLong opt2 )
    {
        // absent values are less than present values

        if (opt1.isPresent() && opt2.isPresent())
            return Long.compare(opt1.getAsLong(), opt2.getAsLong());
        else if (opt1.isPresent()) // && !opt2.isPresent()
            return 1;
        else if (opt2.isPresent()) // && !opt1.isPresent()
            return -1;
        else
            return 0;
    }

    public static int compare( OptionalDouble opt1, OptionalDouble opt2 )
    {
        // absent values are less than present values

        if (opt1.isPresent() && opt2.isPresent())
            return Double.compare(opt1.getAsDouble(), opt2.getAsDouble());
        else if (opt1.isPresent()) // && !opt2.isPresent()
            return 1;
        else if (opt2.isPresent()) // && !opt1.isPresent()
            return -1;
        else
            return 0;
    }

    public static OptionalInt ofNullable( @Nullable Integer value )
    {
        return (value == null) ? OptionalInt.empty() : OptionalInt.of(value);
    }

    public static OptionalLong ofNullable( @Nullable Long value )
    {
        return (value == null) ? OptionalLong.empty() : OptionalLong.of(value);
    }

    public static OptionalDouble ofNullable( @Nullable Double value )
    {
        return (value == null) ? OptionalDouble.empty() : OptionalDouble.of(value);
    }

    public static <T> Stream<T> asStream( Optional<T> opt )
    {
        return opt.map(Stream::of).orElseGet(Stream::empty);
    }

    public static IntStream asStream( OptionalInt opt )
    {
        return opt.isPresent() ? IntStream.of(opt.getAsInt()) : IntStream.empty();
    }

    public static LongStream asStream( OptionalLong opt )
    {
        return opt.isPresent() ? LongStream.of(opt.getAsLong()) : LongStream.empty();
    }

    public static DoubleStream asStream( OptionalDouble opt )
    {
        return opt.isPresent() ? DoubleStream.of(opt.getAsDouble()) : DoubleStream.empty();
    }

    public static boolean contains( Optional<?> opt, @Nullable Object val )
    {
        return contains(opt, val, ( a, b ) -> a.equals(b));
    }

    public static <T, U> boolean contains( Optional<T> opt,
                                           @Nullable U val,
                                           BiPredicate<? super T, ? super U> predicate )
    {
        Objects.requireNonNull(predicate);
        if (val == null)
            return !opt.isPresent();
        else
            return opt.isPresent() && predicate.test(opt.get(), val);
    }

    public static boolean contains( OptionalInt opt, int val )
    {
        return opt.isPresent() && opt.getAsInt() == val;
    }

    public static boolean contains( OptionalLong opt, long val )
    {
        return opt.isPresent() && opt.getAsLong() == val;
    }

    public static boolean contains( OptionalDouble opt, double val )
    {
        return opt.isPresent() && opt.getAsDouble() == val;
    }

    public static <T, U> boolean areEqual( Optional<T> opt1,
                                           Optional<U> opt2,
                                           BiPredicate<? super T, ? super U> predicate )
    {
        Objects.requireNonNull(predicate);
        return (opt1.isPresent() == opt2.isPresent())
               && (!opt1.isPresent() || predicate.test(opt1.get(), opt2.get()));
    }

    public static <T> boolean test( Optional<T> opt, Predicate<? super T> predicate )
    {
        Objects.requireNonNull(predicate);
        return opt.isPresent() && predicate.test(opt.get());
    }

    public static <T> boolean testNullable( Optional<T> opt, Predicate<? super T> predicate )
    {
        return predicate.test(opt.orElse(null));
    }

    public static boolean test( OptionalInt opt, IntPredicate predicate )
    {
        Objects.requireNonNull(predicate);
        return opt.isPresent() && predicate.test(opt.getAsInt());
    }

    public static boolean test( OptionalLong opt, LongPredicate predicate )
    {
        Objects.requireNonNull(predicate);
        return opt.isPresent() && predicate.test(opt.getAsLong());
    }

    public static boolean test( OptionalDouble opt, DoublePredicate predicate )
    {
        Objects.requireNonNull(predicate);
        return opt.isPresent() && predicate.test(opt.getAsDouble());
    }

    public static OptionalInt filter( OptionalInt opt, IntPredicate predicate )
    {
        Objects.requireNonNull(predicate);
        if (!opt.isPresent())
            return opt;
        else
            return predicate.test(opt.getAsInt()) ? opt : OptionalInt.empty();
    }

    public static OptionalLong filter( OptionalLong opt, LongPredicate predicate )
    {
        Objects.requireNonNull(predicate);
        if (!opt.isPresent())
            return opt;
        else
            return predicate.test(opt.getAsLong()) ? opt : OptionalLong.empty();
    }

    public static OptionalDouble filter( OptionalDouble opt, DoublePredicate predicate )
    {
        Objects.requireNonNull(predicate);
        if (!opt.isPresent())
            return opt;
        else
            return predicate.test(opt.getAsDouble()) ? opt : OptionalDouble.empty();
    }

    @SuppressWarnings( "unchecked" )
    public static <T, U> Optional<U> identityPreservingMap( Optional<T> opt, Function<? super T, ? extends U> mapper )
    {
        Objects.requireNonNull(mapper);
        if (!opt.isPresent()) {
            return (Optional<U>)opt;
        }
        else {
            final T original = opt.get();
            final U mapped = mapper.apply(original);
            return (mapped == original) ? (Optional<U>)opt : Optional.ofNullable(mapped);
        }
    }

    public static OptionalInt identityPreservingMap( OptionalInt opt, IntUnaryOperator mapper )
    {
        Objects.requireNonNull(mapper);
        if (!opt.isPresent()) {
            return opt;
        }
        else {
            final int original = opt.getAsInt();
            final int mapped = mapper.applyAsInt(original);
            return (mapped == original) ? opt : OptionalInt.of(mapped);
        }
    }

    public static OptionalLong identityPreservingMap( OptionalLong opt, LongUnaryOperator mapper )
    {
        Objects.requireNonNull(mapper);
        if (!opt.isPresent()) {
            return opt;
        }
        else {
            final long original = opt.getAsLong();
            final long mapped = mapper.applyAsLong(original);
            return (mapped == original) ? opt : OptionalLong.of(mapped);
        }
    }

    public static OptionalDouble identityPreservingMap( OptionalDouble opt, DoubleUnaryOperator mapper )
    {
        Objects.requireNonNull(mapper);
        if (!opt.isPresent()) {
            return opt;
        }
        else {
            final double original = opt.getAsDouble();
            final double mapped = mapper.applyAsDouble(original);
            return (mapped == original) ? opt : OptionalDouble.of(mapped);
        }
    }

    private OptionalUtils()
    {
        // not used
    }
}
