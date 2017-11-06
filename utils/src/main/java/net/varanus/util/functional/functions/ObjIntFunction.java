package net.varanus.util.functional.functions;

/**
 * @param <T>
 * @param <R>
 */
public interface ObjIntFunction<T, R>
{
    public R apply( T t, int value );
}
