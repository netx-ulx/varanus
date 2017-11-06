package net.varanus.util.concurrent;

/**
 * 
 */
@FunctionalInterface
public interface InterruptibleRunnable extends Runnable
{
    public void runInterruptibly() throws InterruptedException;

    @Override
    public default void run()
    {
        ConcurrencyUtils.runUntilInterrupted(this);
    }
}
