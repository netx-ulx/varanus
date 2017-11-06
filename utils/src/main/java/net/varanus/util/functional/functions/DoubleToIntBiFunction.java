package net.varanus.util.functional.functions;

/**
 * 
 */
@FunctionalInterface
public interface DoubleToIntBiFunction
{
    public int applyAsInt( double left, double right );
}
