package net.varanus.util.concurrent;


import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.lang.MoreObjects;


/**
 * 
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class ConcurrencyUtils
{
    public static void runUntilInterrupted( InterruptibleRunnable runnable )
    {
        try {
            runnable.runInterruptibly();
        }
        catch (InterruptedException e) { /* ignore */ }
    }

    public static ThreadFactory defaultDaemonThreadFactory()
    {
        ThreadFactory defTF = Executors.defaultThreadFactory();
        return ( run ) -> {
            Thread t = defTF.newThread(run);
            t.setDaemon(true);
            return t;
        };
    }

    public static <T> CompletableFuture<T> toCompletableFuture( ListenableFuture<T> lisFuture )
    {
        Objects.requireNonNull(lisFuture);

        CompletableFuture<T> compFuture = new CompletableFuture<>();
        Futures.addCallback(lisFuture, buildCallback(compFuture));
        return compFuture;
    }

    public static <T> CompletableFuture<T> toCompletableFuture( ListenableFuture<T> lisFuture, Executor executor )
    {
        MoreObjects.requireNonNull(lisFuture, "lisFuture", executor, "executor");

        CompletableFuture<T> compFuture = new CompletableFuture<>();
        Futures.addCallback(lisFuture, buildCallback(compFuture), executor);
        return compFuture;
    }

    private static <T> FutureCallback<T> buildCallback( CompletableFuture<T> compFuture )
    {
        return new FutureCallback<T>() {

            @Override
            public void onSuccess( T result )
            {
                compFuture.complete(result);
            }

            @Override
            public void onFailure( Throwable t )
            {
                compFuture.completeExceptionally(t);
            }
        };
    }

    private ConcurrencyUtils()
    {
        // not used
    }
}
