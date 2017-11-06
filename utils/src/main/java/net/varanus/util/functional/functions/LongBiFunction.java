package net.varanus.util.functional.functions;

/**
 * @param <R>
 */
@FunctionalInterface
public interface LongBiFunction<R>
{
    public R apply( long left, long right );
}
