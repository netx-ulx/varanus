package net.varanus.util.functional.functions;

/**
 * 
 */
@FunctionalInterface
public interface DoubleToLongBiFunction
{
    public long applyAsLong( double left, double right );
}
