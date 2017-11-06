package net.varanus.util.net;


import net.varanus.util.time.TimeLong;


/**
 */
public final class ManagedRTT
{
    private volatile TimeLong rttEst;
    private volatile TimeLong rttVar;
    private volatile long     timerDelayNanos;

    private final Object lock;

    public ManagedRTT()
    {
        this.lock = new Object();
        reset();
    }

    public boolean hasSamples()
    {
        return rttEst.isPresent();
    }

    public TimeLong getRTTEst()
    {
        return rttEst;
    }

    public TimeLong getRTTVar()
    {
        return rttVar;
    }

    public TimeLong getTimerDelay()
    {
        return inNanos(timerDelayNanos);
    }

    public void newRTTSample( long rttSample )
    {
        synchronized (lock) {
            if (hasSamples()) {
                final long prevRTTVar = rttVar.inNanos();
                final long prevRTTEst = rttEst.inNanos();
                rttVar = inNanos(RTTUtils.updateRTTVariationNanos(prevRTTVar, prevRTTEst, rttSample));
                rttEst = inNanos(RTTUtils.updateRTTEstimationNanos(prevRTTEst, rttSample));
            }
            else {
                rttVar = inNanos(RTTUtils.initialRTTVariationNanos(rttSample));
                rttEst = inNanos(RTTUtils.initialRTTEstimationNanos(rttSample));
            }

            timerDelayNanos = RTTUtils.updateTimerDelayNanos(rttEst.inNanos(), rttVar.inNanos());
        }
    }

    public void timeout()
    {
        timerDelayNanos = RTTUtils.backoffTimerDelayNanos(timerDelayNanos);
    }

    public void reset()
    {
        synchronized (lock) {
            rttEst = TimeLong.absent();
            rttVar = TimeLong.absent();
            timerDelayNanos = RTTUtils.getDefaultTimerDelayNanos();
        }
    }

    private static TimeLong inNanos( long nanos )
    {
        return TimeLong.ofNanos(nanos);
    }
}
