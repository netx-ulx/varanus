package net.varanus.util.functional.functions;

@FunctionalInterface
/**
 * @param <T>
 * @param <X>
 */
public interface ExceptionalSupplier<T, X extends Throwable>
{
    public T get() throws X;
}
