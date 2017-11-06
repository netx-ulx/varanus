package net.varanus.util.collect.builder;


import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;


/**
 * @param <T>
 *            The type of built objects
 */
@FunctionalInterface
@ReturnValuesAreNonnullByDefault
public interface BaseBuilder<T>
{
    public T build();
}
