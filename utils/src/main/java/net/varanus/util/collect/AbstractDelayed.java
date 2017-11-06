package net.varanus.util.collect;


import java.time.Duration;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;


/**
 * 
 */
public abstract class AbstractDelayed implements Delayed
{
    private static final AtomicLong idGen = new AtomicLong(Long.MIN_VALUE);

    private final long id;

    private long timeoutNanos;
    private long nextNanoTime;

    protected AbstractDelayed( Duration timeout )
    {
        this(toNanos(timeout));
    }

    protected AbstractDelayed( long timeout, TimeUnit unit )
    {
        this(toNanos(timeout, unit));
    }

    protected AbstractDelayed( long timeoutNanos )
    {
        this.id = idGen.getAndIncrement();

        setTimeoutNanos(timeoutNanos);
        resetTimer();
    }

    protected final void setTimeout( Duration timeout )
    {
        setTimeoutNanos(toNanos(timeout));
    }

    protected final void setTimeout( long timeout, TimeUnit unit )
    {
        setTimeoutNanos(toNanos(timeout, unit));
    }

    protected final void setTimeoutNanos( long timeoutNanos )
    {
        this.timeoutNanos = asNonNegative(timeoutNanos);
    }

    protected final void resetTimer()
    {
        this.nextNanoTime = System.nanoTime() + timeoutNanos;
    }

    public final long getDelayNanos()
    {
        return nextNanoTime - System.nanoTime();
    }

    @Override
    public final long getDelay( TimeUnit unit )
    {
        return unit.convert(getDelayNanos(), TimeUnit.NANOSECONDS);
    }

    @Override
    public int compareTo( Delayed other )
    {
        if (other instanceof AbstractDelayed) {
            AbstractDelayed otherD = (AbstractDelayed)other;
            long diff = this.nextNanoTime - otherD.nextNanoTime;
            if (diff < 0)
                return -1;
            else if (diff > 0)
                return 1;
            else
                return Long.compare(this.id, otherD.id);
        }
        else {
            return Long.compare(
                this.getDelay(TimeUnit.NANOSECONDS),
                other.getDelay(TimeUnit.NANOSECONDS));
        }
    }

    private static long toNanos( Duration duration )
    {
        return duration.toNanos();
    }

    private static long toNanos( long duration, TimeUnit unit )
    {
        return unit.toNanos(duration);
    }

    private static long asNonNegative( long n )
    {
        return (n < 0) ? 0 : n;
    }
}
