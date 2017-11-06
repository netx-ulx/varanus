package net.varanus.util.functional.functions;


import java.util.Objects;


/**
 * @param <T>
 * @param <R>
 * @param <X>
 */
@FunctionalInterface
public interface ExceptionalFunction<T, R, X extends Throwable>
{
    public R apply( T t ) throws X;

    public default <V> ExceptionalFunction<V, R,
                                           X> compose( ExceptionalFunction<? super V, ? extends T, ? extends X> before )
    {
        Objects.requireNonNull(before);
        return ( V v ) -> apply(before.apply(v));
    }

    public default <V> ExceptionalFunction<T, V,
                                           X> andThen( ExceptionalFunction<? super R, ? extends V, ? extends X> after )
    {
        Objects.requireNonNull(after);
        return ( T t ) -> after.apply(apply(t));
    }

    public static <T, X extends Throwable> ExceptionalFunction<T, T, X> identity()
    {
        return t -> t;
    }
}
