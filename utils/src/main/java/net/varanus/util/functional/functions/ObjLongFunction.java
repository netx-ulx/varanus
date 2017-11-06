package net.varanus.util.functional.functions;

/**
 * @param <T>
 * @param <R>
 */
public interface ObjLongFunction<T, R>
{
    public R apply( T t, long value );
}
