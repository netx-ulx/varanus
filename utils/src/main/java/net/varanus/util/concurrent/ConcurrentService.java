package net.varanus.util.concurrent;


import static net.varanus.util.concurrent.ConcurrencyUtils.runUntilInterrupted;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.Service;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.collect.CollectionUtils;
import net.varanus.util.functional.FunctionUtils;


/**
 * A single threaded {@link Service} that stops upon JVM shutdown (using a
 * shutdown hook) and requires the implementation of an
 * {@linkplain #runInterruptibly() interruptible method}.
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public abstract class ConcurrentService extends AbstractExecutionThreadService implements InterruptibleRunnable
{
    private static final ConcurrentMap<String, AtomicLong> serviceNameIds = new ConcurrentHashMap<>();

    private static String getServiceThreadName( ConcurrentService service )
    {
        String name = String.valueOf(service.getServiceName());
        AtomicLong ids = CollectionUtils.computeIfAbsent(serviceNameIds, name, AtomicLong::new);
        long id = ids.incrementAndGet();
        return ((id > 1) ? (name + "-" + id) : name) + "-thread";
    }

    private final ExecutorService               executor;
    private final ShutdownWatcher               shutdownWatcher;
    private final BiConsumer<String, Throwable> exceptionLogger;

    public ConcurrentService()
    {
        this(Executors.defaultThreadFactory(), FunctionUtils.ignoreBi());
    }

    public ConcurrentService( ThreadFactory threadFact )
    {
        this(threadFact, FunctionUtils.ignoreBi());
    }

    public ConcurrentService( BiConsumer<String, Throwable> exceptionLogger )
    {
        this(Executors.defaultThreadFactory(), exceptionLogger);
    }

    public ConcurrentService( ThreadFactory threadFact, BiConsumer<String, Throwable> exceptionLogger )
    {
        this.executor = Executors.newSingleThreadExecutor(adaptFactory(threadFact));
        this.shutdownWatcher = new ShutdownWatcher();
        this.exceptionLogger = Objects.requireNonNull(exceptionLogger);
        addListener(shutdownWatcher);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ConcurrentService.this.stop();
            runUntilInterrupted(() -> {
                ConcurrentService.this.waitForShutdown(5, TimeUnit.SECONDS);
            });
        }));
    }

    private ThreadFactory adaptFactory( final ThreadFactory threadFact )
    {
        Objects.requireNonNull(threadFact);
        final String tName = getServiceThreadName(this);
        return ( r ) -> {
            Thread t = threadFact.newThread(r);
            t.setName(tName);
            return t;
        };
    }

    @Override
    protected final Executor executor()
    {
        return executor;
    }

    protected final BiConsumer<String, Throwable> exceptionLogger()
    {
        return exceptionLogger;
    }

    @Override
    public final void run()
    {
        try {
            runUntilInterrupted(this);
        }
        catch (Throwable t) {
            exceptionLogger.accept(
                String.format("Exception on %s: %s", getServiceName(), t.getMessage()),
                t);
            throw t;
        }
    }

    @Override
    protected final void triggerShutdown()
    {
        executor.shutdownNow();
    }

    public final void addListener( Listener listener )
    {
        addListener(listener, MoreExecutors.sameThreadExecutor());
    }

    public final void waitForShutdown() throws InterruptedException
    {
        shutdownWatcher.waitForShutdown();
    }

    public final boolean waitForShutdown( long timeout, TimeUnit unit ) throws InterruptedException
    {
        return shutdownWatcher.waitForShutdown(timeout, unit);
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    private static final class ShutdownWatcher implements Service.Listener
    {
        private final CountDownLatch shutdownLatch;

        ShutdownWatcher()
        {
            this.shutdownLatch = new CountDownLatch(1);
        }

        void waitForShutdown() throws InterruptedException
        {
            shutdownLatch.await();
        }

        boolean waitForShutdown( long timeout, TimeUnit unit ) throws InterruptedException
        {
            return shutdownLatch.await(timeout, unit);
        }

        @Override
        public void terminated( State from )
        {
            shutdown();
        }

        @Override
        public void failed( State from, Throwable failure )
        {
            shutdown();
        }

        private void shutdown()
        {
            shutdownLatch.countDown();
        }

        @Override
        public void starting()
        { /* not used */ }

        @Override
        public void running()
        { /* not used */ }

        @Override
        public void stopping( State from )
        { /* not used */ }
    }
}
