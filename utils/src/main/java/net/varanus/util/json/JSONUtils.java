package net.varanus.util.json;


import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import net.varanus.util.collect.CollectionUtils;


/**
 * 
 */
public final class JSONUtils
{
    public static <T> T parseGeneric( String s, TypeReference<T> objType ) throws IllegalArgumentException
    {
        // be nice to the user and accept leading or trailing whitespace
        s = s.trim();

        try {
            ObjectMapper codec = new ObjectMapper();
            return codec.readValue(s, objType);
        }
        catch (IOException e) {
            throw new IllegalArgumentException(
                String.format("error while parsing a JSON construct: %s", e.getMessage()),
                e);
        }
    }

    public static List<String> parseList( String s ) throws IllegalArgumentException
    {
        // be nice to the user and accept leading or trailing whitespace
        s = s.trim();

        try {
            ObjectMapper codec = new ObjectMapper();
            TypeReference<?> type = new TypeReference<ArrayList<String>>() {/**/};
            return codec.readValue(s, type);
        }
        catch (IOException e) {
            throw new IllegalArgumentException(
                String.format("error while parsing a list from JSON: %s", e.getMessage()),
                e);
        }
    }

    public static Set<String> parseSet( String s ) throws IllegalArgumentException
    {
        // be nice to the user and accept leading or trailing whitespace
        s = s.trim();

        try {
            ObjectMapper codec = new ObjectMapper();
            TypeReference<?> type = new TypeReference<LinkedHashSet<String>>() {/**/};
            return codec.readValue(s, type);
        }
        catch (IOException e) {
            throw new IllegalArgumentException(
                String.format("error while parsing a set from JSON: %s", e.getMessage()),
                e);
        }
    }

    public static Map<String, String> parseMap( String s ) throws IllegalArgumentException
    {
        // be nice to the user and accept leading or trailing whitespace
        s = s.trim();

        try {
            ObjectMapper codec = new ObjectMapper();
            TypeReference<?> type = new TypeReference<LinkedHashMap<String, String>>() {/**/};
            return codec.readValue(s, type);
        }
        catch (IOException e) {
            throw new IllegalArgumentException(
                String.format("error while parsing a map from JSON: %s", e.getMessage()),
                e);
        }
    }

    public static Map<String, List<String>> parseMapOfLists( String s ) throws IllegalArgumentException
    {
        // be nice to the user and accept leading or trailing whitespace
        s = s.trim();

        try {
            ObjectMapper codec = new ObjectMapper();
            TypeReference<?> type = new TypeReference<LinkedHashMap<String, ArrayList<String>>>() {/**/};
            return codec.readValue(s, type);
        }
        catch (IOException e) {
            throw new IllegalArgumentException(
                String.format("error while parsing a map of lists from JSON: %s", e.getMessage()),
                e);
        }
    }

    public static Map<String, Set<String>> parseMapOfSets( String s ) throws IllegalArgumentException
    {
        // be nice to the user and accept leading or trailing whitespace
        s = s.trim();

        try {
            ObjectMapper codec = new ObjectMapper();
            TypeReference<?> type = new TypeReference<LinkedHashMap<String, LinkedHashSet<String>>>() {/**/};
            return codec.readValue(s, type);
        }
        catch (IOException e) {
            throw new IllegalArgumentException(
                String.format("error while parsing a map of sets from JSON: %s", e.getMessage()),
                e);
        }
    }

    public static Map<String, Map<String, String>> parseMapOfMaps( String s ) throws IllegalArgumentException
    {
        // be nice to the user and accept leading or trailing whitespace
        s = s.trim();

        try {
            ObjectMapper codec = new ObjectMapper();
            TypeReference<?> type = new TypeReference<LinkedHashMap<String, LinkedHashMap<String, String>>>() {/**/};
            return codec.readValue(s, type);
        }
        catch (IOException e) {
            throw new IllegalArgumentException(
                String.format("error while parsing a map of maps from JSON: %s", e.getMessage()),
                e);
        }
    }

    public static <E> List<E> parseList( String s, Function<String, E> mapper ) throws IllegalArgumentException
    {
        return CollectionUtils.toList(parseList(s), mapper);
    }

    public static <E> Set<E> parseSet( String s, Function<String, E> mapper ) throws IllegalArgumentException
    {
        return CollectionUtils.toSet(parseSet(s), mapper);
    }

    public static <K, V> Map<K, V> parseMap( String s,
                                             Function<String, ? extends K> keyMapper,
                                             Function<String, ? extends V> valMapper )
        throws IllegalArgumentException
    {
        return CollectionUtils.toMap(parseMap(s), keyMapper, valMapper);
    }

    public static <K, V> Map<K, List<V>> parseMapOfLists( String s,
                                                          Function<String, ? extends K> keyMapper,
                                                          Function<String, ? extends V> valMapper )
        throws IllegalArgumentException
    {
        return CollectionUtils.toMap(
            parseMapOfLists(s),
            keyMapper,
            list -> CollectionUtils.toList(list, valMapper));
    }

    public static <K, V> Map<K, Set<V>> parseMapOfSets( String s,
                                                        Function<String, ? extends K> keyMapper,
                                                        Function<String, ? extends V> valMapper )
        throws IllegalArgumentException
    {
        return CollectionUtils.toMap(
            parseMapOfSets(s),
            keyMapper,
            set -> CollectionUtils.toSet(set, valMapper));
    }

    public static <R, C, V> Map<R, Map<C, V>> parseMapOfMaps( String s,
                                                              Function<String, ? extends R> rowMapper,
                                                              Function<String, ? extends C> colMapper,
                                                              Function<String, ? extends V> valMapper )
        throws IllegalArgumentException
    {
        return CollectionUtils.toMap(
            parseMapOfMaps(s),
            rowMapper,
            map -> CollectionUtils.toMap(map, colMapper, valMapper));
    }

    private JSONUtils()
    {
        // not used
    }
}
