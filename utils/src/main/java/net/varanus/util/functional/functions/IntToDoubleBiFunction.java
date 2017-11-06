package net.varanus.util.functional.functions;

/**
 * 
 */
@FunctionalInterface
public interface IntToDoubleBiFunction
{
    public double applyAsDouble( int left, int right );
}
