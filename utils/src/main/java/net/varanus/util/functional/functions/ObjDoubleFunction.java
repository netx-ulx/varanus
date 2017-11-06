package net.varanus.util.functional.functions;

/**
 * @param <T>
 * @param <R>
 */
public interface ObjDoubleFunction<T, R>
{
    public R apply( T t, double value );
}
