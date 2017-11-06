package net.varanus.util.functional.functions;

/**
 * 
 */
@FunctionalInterface
public interface IntToLongBiFunction
{
    public long applyAsLong( int left, int right );
}
