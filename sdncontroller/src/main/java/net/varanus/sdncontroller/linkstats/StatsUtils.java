package net.varanus.sdncontroller.linkstats;


import static net.varanus.sdncontroller.util.stats.StatType.UNSAFE;

import java.time.Instant;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.sdncontroller.util.stats.Stat;
import net.varanus.sdncontroller.util.stats.StatType;
import net.varanus.sdncontroller.util.stats.Stat.StringFormat;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.functional.BasePossible;
import net.varanus.util.lang.Comparables;
import net.varanus.util.time.Timed;


/**
 * 
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
final class StatsUtils
{
    static <V extends BasePossible<V>> Stat<V> of( V value, Instant timestamp, StatType typeIfPresent )
    {
        return of(value, timestamp, typeIfPresent, String::valueOf);
    }

    static <V extends BasePossible<V>> Stat<V> of( V value,
                                                   Instant timestamp,
                                                   StatType typeIfPresent,
                                                   Function<? super V, String> valuePrinter )
    {
        StatType type = value.isPresent() ? typeIfPresent : UNSAFE;
        return Stat.of(value, timestamp, type, valuePrinter);
    }

    static <V extends BasePossible<V>> Stat<V> ofTimed( Timed<V> timed, StatType typeIfPresent )
    {
        return ofTimed(timed, typeIfPresent, String::valueOf);
    }

    static <V extends BasePossible<V>> Stat<V> ofTimed( Timed<V> timed,
                                                        StatType typeIfPresent,
                                                        Function<? super V, String> valuePrinter )
    {
        return of(timed.value(), timed.timestamp(), typeIfPresent, valuePrinter);
    }

    static <V extends BasePossible<V>> Stat<V> now( V value, StatType typeIfPresent )
    {
        return now(value, typeIfPresent, String::valueOf);
    }

    static <V extends BasePossible<V>> Stat<V> now( V value,
                                                    StatType typeIfPresent,
                                                    Function<? super V, String> valuePrinter )
    {
        return of(value, Instant.now(), typeIfPresent, valuePrinter);
    }

    static <V, U, R extends BasePossible<R>> Stat<R> ofCombined( Stat<V> stat1,
                                                                 Stat<U> stat2,
                                                                 StatType typeIfPresent,
                                                                 BiFunction<V, U, R> combiner )
    {
        return ofCombined(stat1, stat2, typeIfPresent, combiner, String::valueOf);
    }

    static <V, U, R extends BasePossible<R>> Stat<R> ofCombined( Stat<V> stat1,
                                                                 Stat<U> stat2,
                                                                 StatType typeIfPresent,
                                                                 BiFunction<V, U, R> combiner,
                                                                 Function<? super R, String> valuePrinter )
    {
        R value = combiner.apply(stat1.value(), stat2.value());
        Instant timestamp = Comparables.max(stat1.timestamp(), stat2.timestamp());
        return of(value, timestamp, typeIfPresent, valuePrinter);
    }

    static String mainStatToString( Stat<?> stat )
    {
        return stat.toString(StringFormat.VALUE_TYPE);
    }

    static String mainStatToPrettyString( Stat<?> stat )
    {
        return stat.toString(StringFormat.VALUE_TYPE);
    }

    static <V> String mainRxRateToPrettyString( Stat<V> rxRateStat,
                                                Stat<V> txRateStat,
                                                BiFunction<? super V, ? super V, String> valuePrinter )
    {
        return mainStatToPrettyString(rxRateStat.with(rxRate -> valuePrinter.apply(rxRate, txRateStat.value())));
    }

    static String subStatToString( Stat<?> stat )
    {
        return stat.toString(StringFormat.VALUE_TIME);
    }

    static String subStatToPrettyString( Stat<?> stat )
    {
        return stat.toString(StringFormat.VALUE_TYPE_TIME);
    }

    static <V> String subRxRateToPrettyString( Stat<V> rxRateStat,
                                               Stat<V> txRateStat,
                                               BiFunction<? super V, ? super V, String> valuePrinter )
    {
        return subStatToPrettyString(rxRateStat.with(rxRate -> valuePrinter.apply(rxRate, txRateStat.value())));
    }

    private StatsUtils()
    {
        // not used
    }
}
