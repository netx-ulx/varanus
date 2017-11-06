package net.varanus.util.collect;


import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;


/**
 * A pair of elements.
 * 
 * @param <A>
 *            The type of the first element.
 * @param <B>
 *            The type of the second element
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public interface Pair<A, B>
{
    /**
     * Returns an immutable pair containing the provided elements.
     * 
     * @param first
     *            The first element of this pair
     * @param second
     *            The second element of this pair
     * @return an immutable pair containing the provided elements
     */
    public static <A, B> Pair<A, B> of( final @Nullable A first, final @Nullable B second )
    {
        return new AbstractPair<A, B>() {

            @Override
            public A getFirst()
            {
                return first;
            }

            @Override
            public B getSecond()
            {
                return second;
            }
        };
    }

    /**
     * Returns the first element of this pair.
     * 
     * @return the first element of this pair
     */
    public @Nullable A getFirst();

    /**
     * Returns the second element of this pair.
     * 
     * @return the second element of this pair
     */
    public @Nullable B getSecond();

    public default String toTupleString()
    {
        return toTupleString(", ", "(", ")");
    }

    public default String toTupleString( String separator )
    {
        return toTupleString(separator, "(", ")");
    }

    public default String toTupleString( String separator, String prefix, String suffix )
    {
        String pref = (prefix != null) ? prefix : "";
        String frst = String.valueOf(this.getFirst());
        String sepa = (separator != null) ? separator : "";
        String scnd = String.valueOf(this.getSecond());
        String suff = (suffix != null) ? suffix : "";

        return pref + frst + sepa + scnd + suff;
    }

    public default void accept( BiConsumer<A, B> consumer )
    {
        consumer.accept(getFirst(), getSecond());
    }

    public default @Nullable <R> R apply( BiFunction<A, B, R> function )
    {
        return function.apply(getFirst(), getSecond());
    }

    public default boolean test( BiPredicate<A, B> predicate )
    {
        return predicate.test(getFirst(), getSecond());
    }

    public default <A2, B2> Pair<A2, B2> map( Function<A, A2> firstMapper, Function<B, B2> secondMapper )
    {
        return Pair.of(firstMapper.apply(getFirst()), secondMapper.apply(getSecond()));
    }

    public default <A2, B2> Pair<A2, B2> flatMap( BiFunction<A, B, Pair<A2, B2>> mapper )
    {
        return mapper.apply(getFirst(), getSecond());
    }
}
