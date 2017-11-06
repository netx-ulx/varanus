package net.varanus.util.functional.functions;


import java.util.Objects;


/**
 * @param <T>
 * @param <X>
 */
@FunctionalInterface
public interface ExceptionalConsumer<T, X extends Throwable>
{
    public void accept( T t ) throws X;

    public default ExceptionalConsumer<T, X> andThen( ExceptionalConsumer<? super T, ? extends X> after )
    {
        Objects.requireNonNull(after);
        return ( T t ) -> {
            accept(t);
            after.accept(t);
        };
    }
}
