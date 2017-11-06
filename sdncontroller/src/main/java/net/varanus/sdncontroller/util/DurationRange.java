package net.varanus.sdncontroller.util;


import java.security.SecureRandom;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.base.Preconditions;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.lang.Comparables;
import net.varanus.util.security.SecureRandoms;
import net.varanus.util.time.TimeLong;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class DurationRange
{
    private final TimeLong min;
    private final TimeLong max;

    public DurationRange( TimeLong min, TimeLong max )
    {
        // also checks for null
        Preconditions.checkArgument(Comparables.aLEb(min, max), "min > max");
        this.min = min;
        this.max = max;
    }

    public TimeLong getMinDuration()
    {
        return min;
    }

    public TimeLong getMaxDuration()
    {
        return max;
    }

    public TimeLong getRandomDuration()
    {
        long minNanos = min.inNanos();
        long maxNanos = max.inNanos();
        long range = (maxNanos - minNanos) + 1;
        return TimeLong.ofNanos(minNanos + (randomNonNegativeLong() % range));
    }

    private static long randomNonNegativeLong()
    {
        SecureRandom rand = SecureRandoms.threadSafeSecureRandom();
        while (true) {
            long n = Math.abs(rand.nextLong());
            if (n != Long.MIN_VALUE) {
                return n;
            }
        }
    }

    @Override
    public String toString()
    {
        return "[" + min + ", " + max + "]";
    }
}
