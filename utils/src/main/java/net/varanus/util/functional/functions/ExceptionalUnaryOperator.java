package net.varanus.util.functional.functions;

/**
 * @param <T>
 * @param <X>
 */
@FunctionalInterface
public interface ExceptionalUnaryOperator<T, X extends Throwable> extends ExceptionalFunction<T, T, X>
{
    @Override
    public T apply( T t ) throws X;

    public static <T> ExceptionalUnaryOperator<T, ?> identity()
    {
        return t -> t;
    }
}
