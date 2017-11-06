package net.varanus.util.functional.functions;

/**
 * 
 */
@FunctionalInterface
public interface LongToIntBiFunction
{
    public int applyAsInt( long left, long right );
}
