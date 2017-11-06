package net.varanus.util.functional.functions;

/**
 * @param <X>
 */
@FunctionalInterface
public interface ExceptionalRunnable<X extends Throwable>
{
    public void run() throws X;
}
