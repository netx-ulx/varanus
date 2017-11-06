package net.varanus.util.io;


import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.functional.functions.ExceptionalConsumer;
import net.varanus.util.functional.functions.ExceptionalFunction;
import net.varanus.util.functional.functions.ExceptionalPredicate;
import net.varanus.util.functional.functions.ExceptionalRunnable;
import net.varanus.util.functional.functions.ExceptionalSupplier;


/**
 * 
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class IOFunctionUtils
{
    public static <T> Consumer<T> uncheckedConsumer( ExceptionalConsumer<? super T, ? extends IOException> exConsumer )
    {
        Objects.requireNonNull(exConsumer);
        return ( t ) -> {
            try {
                exConsumer.accept(t);
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }

    public static <T, R> Function<T, R> uncheckedFunction( ExceptionalFunction<? super T, ? extends R,
                                                                               ? extends IOException> exFunction )
    {
        Objects.requireNonNull(exFunction);
        return ( t ) -> {
            try {
                return exFunction.apply(t);
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }

    public static <T> Predicate<T> uncheckedPredicate( ExceptionalPredicate<? super T,
                                                                            ? extends IOException> exPredicate )
    {
        Objects.requireNonNull(exPredicate);
        return ( t ) -> {
            try {
                return exPredicate.test(t);
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }

    public static Runnable uncheckedRunnable( ExceptionalRunnable<? extends IOException> exRunnable )
    {
        Objects.requireNonNull(exRunnable);
        return () -> {
            try {
                exRunnable.run();
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }

    public static <T> Supplier<T> uncheckedSupplier( ExceptionalSupplier<? extends T,
                                                                         ? extends IOException> exSupplier )
    {
        Objects.requireNonNull(exSupplier);
        return () -> {
            try {
                return exSupplier.get();
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        };
    }

    private IOFunctionUtils()
    {
        // not used
    }
}
