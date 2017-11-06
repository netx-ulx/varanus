package net.varanus.util.functional;


import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.DoubleFunction;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.IntSupplier;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.LongFunction;
import java.util.function.LongSupplier;
import java.util.function.LongToDoubleFunction;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;


/**
 * 
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class FunctionUtils
{
    public static <T> Consumer<T> ignore()
    {
        return ignore -> {/* do nothing */};
    }

    public static <T, U> BiConsumer<T, U> ignoreBi()
    {
        return ( ignore1, ignore2 ) -> {/* do nothing */};
    }

    @SuppressWarnings( "unchecked" )
    public static <T, R> Function<T, R> castFunction()
    {
        return t -> (R)t;
    }

    public static <T, R> Function<T, R> asFunction( Supplier<? extends R> supplier )
    {
        Objects.requireNonNull(supplier);
        return ignore -> supplier.get();
    }

    public static <R> IntFunction<R> asIntFunction( Supplier<? extends R> supplier )
    {
        Objects.requireNonNull(supplier);
        return ignore -> supplier.get();
    }

    public static <R> LongFunction<R> asLongFunction( Supplier<? extends R> supplier )
    {
        Objects.requireNonNull(supplier);
        return ignore -> supplier.get();
    }

    public static <R> DoubleFunction<R> asDoubleFunction( Supplier<? extends R> supplier )
    {
        Objects.requireNonNull(supplier);
        return ignore -> supplier.get();
    }

    public static <T> ToIntFunction<T> asToIntFunction( IntSupplier supplier )
    {
        Objects.requireNonNull(supplier);
        return ignore -> supplier.getAsInt();
    }

    public static IntUnaryOperator asIntUnaryOperator( IntSupplier supplier )
    {
        Objects.requireNonNull(supplier);
        return ignore -> supplier.getAsInt();
    }

    public static LongToIntFunction asLongToIntFunction( IntSupplier supplier )
    {
        Objects.requireNonNull(supplier);
        return ignore -> supplier.getAsInt();
    }

    public static DoubleToIntFunction asDoubleToIntFunction( IntSupplier supplier )
    {
        Objects.requireNonNull(supplier);
        return ignore -> supplier.getAsInt();
    }

    public static <T> ToLongFunction<T> asToLongFunction( LongSupplier supplier )
    {
        Objects.requireNonNull(supplier);
        return ignore -> supplier.getAsLong();
    }

    public static IntToLongFunction asIntToLongFunction( LongSupplier supplier )
    {
        Objects.requireNonNull(supplier);
        return ignore -> supplier.getAsLong();
    }

    public static LongUnaryOperator asLongUnaryOperator( LongSupplier supplier )
    {
        Objects.requireNonNull(supplier);
        return ignore -> supplier.getAsLong();
    }

    public static DoubleToLongFunction asDoubleToLongFunction( LongSupplier supplier )
    {
        Objects.requireNonNull(supplier);
        return ignore -> supplier.getAsLong();
    }

    public static <T> ToDoubleFunction<T> asToDoubleFunction( DoubleSupplier supplier )
    {
        Objects.requireNonNull(supplier);
        return ignore -> supplier.getAsDouble();
    }

    public static IntToDoubleFunction asIntToDoubleFunction( DoubleSupplier supplier )
    {
        Objects.requireNonNull(supplier);
        return ignore -> supplier.getAsDouble();
    }

    public static LongToDoubleFunction asLongToDoubleFunction( DoubleSupplier supplier )
    {
        Objects.requireNonNull(supplier);
        return ignore -> supplier.getAsDouble();
    }

    public static DoubleUnaryOperator asDoubleUnaryOperator( DoubleSupplier supplier )
    {
        Objects.requireNonNull(supplier);
        return ignore -> supplier.getAsDouble();
    }

    public static <T, R> Supplier<R> asSupplier( Function<? super T, ? extends R> function, @Nullable T input )
    {
        Objects.requireNonNull(function);
        return () -> function.apply(input);
    }

    public static <R> Supplier<R> asSupplierFromInt( IntFunction<? extends R> function, int input )
    {
        Objects.requireNonNull(function);
        return () -> function.apply(input);
    }

    public static <R> Supplier<R> asSupplierFromLong( LongFunction<? extends R> function, long input )
    {
        Objects.requireNonNull(function);
        return () -> function.apply(input);
    }

    public static <R> Supplier<R> asSupplierFromDouble( DoubleFunction<? extends R> function, double input )
    {
        Objects.requireNonNull(function);
        return () -> function.apply(input);
    }

    public static <T> IntSupplier asIntSupplierFromObj( ToIntFunction<? super T> function, @Nullable T input )
    {
        Objects.requireNonNull(function);
        return () -> function.applyAsInt(input);
    }

    public static IntSupplier asIntSupplier( IntUnaryOperator function, int input )
    {
        Objects.requireNonNull(function);
        return () -> function.applyAsInt(input);
    }

    public static IntSupplier asIntSupplierFromLong( LongToIntFunction function, long input )
    {
        Objects.requireNonNull(function);
        return () -> function.applyAsInt(input);
    }

    public static IntSupplier asIntSupplierFromDouble( DoubleToIntFunction function, double input )
    {
        Objects.requireNonNull(function);
        return () -> function.applyAsInt(input);
    }

    public static <T> LongSupplier asLongSupplierFromObj( ToLongFunction<? super T> function, @Nullable T input )
    {
        Objects.requireNonNull(function);
        return () -> function.applyAsLong(input);
    }

    public static LongSupplier asLongSupplierFromInt( IntToLongFunction function, int input )
    {
        Objects.requireNonNull(function);
        return () -> function.applyAsLong(input);
    }

    public static LongSupplier asLongSupplier( LongUnaryOperator function, long input )
    {
        Objects.requireNonNull(function);
        return () -> function.applyAsLong(input);
    }

    public static LongSupplier asLongSupplierFromDouble( DoubleToLongFunction function, double input )
    {
        Objects.requireNonNull(function);
        return () -> function.applyAsLong(input);
    }

    public static <T> DoubleSupplier asDoubleSupplierFromObj( ToDoubleFunction<? super T> function, @Nullable T input )
    {
        Objects.requireNonNull(function);
        return () -> function.applyAsDouble(input);
    }

    public static DoubleSupplier asDoubleSupplierFromInt( IntToDoubleFunction function, int input )
    {
        Objects.requireNonNull(function);
        return () -> function.applyAsDouble(input);
    }

    public static DoubleSupplier asDoubleSupplierFromLong( LongToDoubleFunction function, long input )
    {
        Objects.requireNonNull(function);
        return () -> function.applyAsDouble(input);
    }

    public static DoubleSupplier asDoubleSupplier( DoubleUnaryOperator function, double input )
    {
        Objects.requireNonNull(function);
        return () -> function.applyAsDouble(input);
    }

    private FunctionUtils()
    {
        // not used
    }
}
