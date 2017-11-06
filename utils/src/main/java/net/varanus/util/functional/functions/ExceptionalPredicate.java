package net.varanus.util.functional.functions;


import java.util.Objects;


/**
 * @param <T>
 * @param <X>
 */
@FunctionalInterface
public interface ExceptionalPredicate<T, X extends Throwable>
{
    boolean test( T t ) throws X;

    default ExceptionalPredicate<T, X> and( ExceptionalPredicate<? super T, ? extends X> other )
    {
        Objects.requireNonNull(other);
        return ( t ) -> test(t) && other.test(t);
    }

    default ExceptionalPredicate<T, X> negate()
    {
        return ( t ) -> !test(t);
    }

    default ExceptionalPredicate<T, X> or( ExceptionalPredicate<? super T, ? extends X> other )
    {
        Objects.requireNonNull(other);
        return ( t ) -> test(t) || other.test(t);
    }
}
