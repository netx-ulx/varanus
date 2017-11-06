package net.varanus.util.functional.functions;

/**
 * @param <R>
 */
@FunctionalInterface
public interface DoubleBiFunction<R>
{
    public R apply( double left, double right );
}
