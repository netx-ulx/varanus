package net.varanus.util.functional.functions;

/**
 * @param <R>
 */
@FunctionalInterface
public interface IntBiFunction<R>
{
    public R apply( int left, int right );
}
