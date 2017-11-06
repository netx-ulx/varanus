package net.varanus.util.time;


import java.time.Duration;
import java.util.concurrent.TimeUnit;

import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;


/**
 * 
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class TimeUtils
{
    public static String toSmartDurationString( Duration duration )
    {
        return toSmartDurationString(duration.toNanos());
    }

    public static String toSmartDurationString( long duration, TimeUnit unit )
    {
        return toSmartDurationString(unit.toNanos(duration));
    }

    public static String toSmartDurationString( double duration, TimeDoubleUnit unit )
    {
        return toSmartDurationString(unit.toNanosAsLong(duration));
    }

    static String toSmartDurationString( double totalNanos )
    {
        return toSmartDurationString(TimeDoubleUnit.NANOSECONDS.toNanosAsLong(totalNanos));
    }

    static String toSmartDurationString( final long totalNanos )
    {
        if (totalNanos < 0) {
            throw new IllegalArgumentException("invalid negative duration");
        }

        final long approxHours = TimeUnit.NANOSECONDS.toHours(totalNanos);
        final long approxMinutes = TimeUnit.NANOSECONDS.toMinutes(totalNanos);
        final long approxSeconds = TimeUnit.NANOSECONDS.toSeconds(totalNanos);
        final long approxMillis = TimeUnit.NANOSECONDS.toMillis(totalNanos);

        final long extraMinutesUnderHour = approxMinutes - TimeUnit.HOURS.toMinutes(approxHours);
        final long extraSecondsUnderMinute = approxSeconds - TimeUnit.MINUTES.toSeconds(approxMinutes);
        final long extraMillisUnderSecond = approxMillis - TimeUnit.SECONDS.toMillis(approxSeconds);
        final long extraNanosUnderMilli = totalNanos - TimeUnit.MILLISECONDS.toNanos(approxMillis);

        StringBuilder buf = new StringBuilder(32);
        // put Hh if positive hours
        if (approxHours > 0) {
            buf.append(approxHours).append(abbreviatedUnit(TimeUnit.HOURS));
        }
        // put Mm if positive remainder minutes
        if (extraMinutesUnderHour > 0) {
            buf.append(extraMinutesUnderHour).append(abbreviatedUnit(TimeUnit.MINUTES));
        }

        // try to cut at XhYm
        if (extraSecondsUnderMinute == 0
            && extraMillisUnderSecond == 0
            && extraNanosUnderMilli == 0
            && buf.length() > 2) {
            return buf.toString();
        }

        // put Xs if positive remainder seconds
        if (extraSecondsUnderMinute > 0) {
            buf.append(extraSecondsUnderMinute).append(abbreviatedUnit(TimeUnit.SECONDS));
        }

        // try to cut at [Xh][Ym]Zs
        if (extraMillisUnderSecond == 0
            && extraNanosUnderMilli == 0
            && buf.length() > 0) {
            return buf.toString();
        }

        // put X remainder milliseconds
        buf.append(extraMillisUnderSecond);
        // put .Y if positive remainder nanoseconds
        if (extraNanosUnderMilli > 0) {
            final long nanosPerMilli = TimeUnit.MILLISECONDS.toNanos(1);
            final int pos = buf.length();

            buf.append(extraNanosUnderMilli + nanosPerMilli);
            while (buf.charAt(buf.length() - 1) == '0') {
                buf.setLength(buf.length() - 1); // cut tailing zeros
            }
            buf.setCharAt(pos, '.');
        }
        // put "ms"
        buf.append(abbreviatedUnit(TimeUnit.MILLISECONDS));

        return buf.toString();
    }

    public static String abbreviatedUnit( TimeUnit unit )
    {
        switch (unit) {
            case NANOSECONDS:
                return "ns";

            case MICROSECONDS:
                return "\u00B5s";

            case MILLISECONDS:
                return "ms";

            case SECONDS:
                return "s";

            case MINUTES:
                return "m";

            case HOURS:
                return "h";

            case DAYS:
                return "d";

            default:
                throw new AssertionError("unknown enum value");
        }
    }

    public static String abbreviatedUnit( TimeDoubleUnit unit )
    {
        return abbreviatedUnit(unit.toTimeUnit());
    }

    private TimeUtils()
    {
        // not used
    }
}
