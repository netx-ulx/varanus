package net.varanus.util.functional.functions;

/**
 * 
 */
@FunctionalInterface
public interface LongToDoubleBiFunction
{
    public double applyAsDouble( long left, long right );
}
