package net.varanus.util.net;


import java.util.concurrent.TimeUnit;

import org.apache.commons.math3.fraction.BigFraction;


/**
 * This class helps updating an RTT estimation, RTT variation and timeout value
 * according to sampled RTTs.
 * <p>
 * The implementation is based on the calculation of TCP's retransmission timer.
 */
public final class RTTUtils
{
    // this applies before the first RTT sample measurement
    private static final long DEFAULT_TIMER_DELAY_NANOS = TimeUnit.SECONDS.toNanos(3L);

    private static final long MIN_TIMER_DELAY_NANOS   = TimeUnit.SECONDS.toNanos(1L);
    private static final long MAX_TIMER_DELAY_NANOS   = TimeUnit.SECONDS.toNanos(60L);
    private static final long TIMER_GRANULARITY_NANOS = TimeUnit.MILLISECONDS.toNanos(1L);

    private static final long[] TIMER_BACKOFF_VALUES_NANOS = {TimeUnit.SECONDS.toNanos(1L),
                                                              TimeUnit.SECONDS.toNanos(2L),
                                                              TimeUnit.SECONDS.toNanos(4L),
                                                              TimeUnit.SECONDS.toNanos(8L),
                                                              TimeUnit.SECONDS.toNanos(16L),
                                                              TimeUnit.SECONDS.toNanos(32L),
                                                              TimeUnit.SECONDS.toNanos(60L)
    };

    private static final BigFraction ALPHA           = new BigFraction(1L, 8L);
    private static final BigFraction ONE_MINUS_ALPHA = BigFraction.ONE.subtract(ALPHA);
    private static final BigFraction BETA            = new BigFraction(1L, 4L);
    private static final BigFraction ONE_MINUS_BETA  = BigFraction.ONE.subtract(BETA);

    /**
     * Returns the initial RTT estimation, given a first RTT sample.
     * <p>
     * All values are in nanoseconds.
     * 
     * @param rttSample
     *            A measured RTT value
     * @return the initial RTT estimation
     */
    public static long initialRTTEstimationNanos( long rttSample )
    {
        return rttSample;
    }

    /**
     * Returns the initial RTT variation, given a first RTT sample.
     * <p>
     * All values are in nanoseconds.
     * 
     * @param rttSample
     *            A measured RTT value
     * @return the initial RTT variation
     */
    public static long initialRTTVariationNanos( long rttSample )
    {
        return rttSample / 2L;
    }

    /**
     * Returns an updated RTT variation, given previous RTT parameters and an
     * RTT sample.
     * <p>
     * All values are in nanoseconds.
     * 
     * @param prevRTTVar
     *            The previously obtained value of the RTT variation
     * @param prevRTTEst
     *            The previously obtained value of the RTT estimation
     * @param rttSample
     *            A measured RTT value
     * @return an updated RTT variation
     */
    public static long updateRTTVariationNanos( long prevRTTVar, long prevRTTEst, long rttSample )
    {
        // RTTvar <- (1 - beta) * RTTvar + beta * |RTTest - RTTsamp|
        final long rttDist = Math.abs(prevRTTEst - rttSample);
        return ONE_MINUS_BETA.multiply(prevRTTVar).add(BETA.multiply(rttDist)).longValue();
    }

    /**
     * Returns an updated RTT estimation, given previous RTT parameters and an
     * RTT sample.
     * <p>
     * All values are in nanoseconds.
     * 
     * @param prevRTTEst
     *            The previously obtained value of the RTT estimation
     * @param rttSample
     *            A measured RTT value
     * @return an updated RTT variation
     */
    public static long updateRTTEstimationNanos( long prevRTTEst, long rttSample )
    {
        // RTTest <- (1 - alpha) * RTTest + alpha * RTTsamp
        return ONE_MINUS_ALPHA.multiply(prevRTTEst).add(ALPHA.multiply(rttSample)).longValue();
    }

    /**
     * Returns the default timer value in nanoseconds.
     * 
     * @return the default timer value in nanoseconds
     */
    public static long getDefaultTimerDelayNanos()
    {
        return DEFAULT_TIMER_DELAY_NANOS;
    }

    /**
     * Returns the updated timer value, given an RTT estimation and RTT
     * variation.
     * <p>
     * All values are in nanoseconds.
     * 
     * @param rttEst
     *            An RTT estimation
     * @param rttVar
     *            An RTT variation
     * @return the updated timer value
     */
    public static long updateTimerDelayNanos( long rttEst, long rttVar )
    {
        // TimerDelay <- RTTest + max(1ms, 4 * RTTvar)
        // TimerDelay >= 1s
        // TimerDelay <= 60s
        final long timerDelay = rttEst + Math.max(TIMER_GRANULARITY_NANOS, 4L * rttVar);
        return boundTimerDelay(timerDelay);
    }

    /**
     * Returns a backed-off timer value (e.g., after a timeout).
     * 
     * @param prevTimerDelay
     *            The previous timer value
     * @return a backed-off timer value
     */
    public static long backoffTimerDelayNanos( long prevTimerDelay )
    {
        final int last = TIMER_BACKOFF_VALUES_NANOS.length - 1;
        for (int i = 0; i < last; i++) {
            if (prevTimerDelay <= TIMER_BACKOFF_VALUES_NANOS[i]) {
                return TIMER_BACKOFF_VALUES_NANOS[i + 1];
            }
        }
        return TIMER_BACKOFF_VALUES_NANOS[last];
    }

    private static long boundTimerDelay( long timerDelay )
    {
        if (timerDelay < MIN_TIMER_DELAY_NANOS)
            return MIN_TIMER_DELAY_NANOS;
        else if (timerDelay > MAX_TIMER_DELAY_NANOS)
            return MAX_TIMER_DELAY_NANOS;
        else return timerDelay;
    }

    private RTTUtils()
    {
        // not used
    }
}
