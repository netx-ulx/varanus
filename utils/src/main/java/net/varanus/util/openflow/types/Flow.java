package net.varanus.util.openflow.types;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.OFValueType;
import org.projectfloodlight.openflow.types.PrimitiveSinkable;

import com.google.common.collect.ImmutableList;
import com.google.common.hash.PrimitiveSink;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.io.exception.IOChannelReadException;
import net.varanus.util.io.exception.IOChannelWriteException;
import net.varanus.util.io.exception.IOReadException;
import net.varanus.util.io.exception.IOWriteException;
import net.varanus.util.io.serializer.IOReader;
import net.varanus.util.io.serializer.IOSerializer;
import net.varanus.util.io.serializer.IOWriter;
import net.varanus.util.lang.Comparators;
import net.varanus.util.openflow.MatchUtils;
import net.varanus.util.openflow.OFSerializers;


/**
 * A {@code Flow} consists of a version-agnostic {@link Match} which uniquely
 * identifies a flow rule in an OpenFlow-enabled switch.
 * 
 * @implNote This class stores a {@code Match} object converted to the highest
 *           supported OpenFlow version in order to allow for version-agnostic
 *           {@code Flow} comparison.
 */
@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class Flow implements PrimitiveSinkable, Comparable<Flow>
{
    private static final OFVersion HIGHEST_VERSION = OFVersion.values()[OFVersion.values().length - 1];

    /**
     * The default flow with an all-wildcard match.
     */
    public static final Flow DEFAULT = new Flow(OFFactories.getFactory(HIGHEST_VERSION).matchWildcardAll());

    /**
     * Constructs a new {@code Flow} with the provided match.
     * 
     * @param match
     *            A {@code Match} object
     * @return a new {@code Flow} object
     */
    public static Flow of( Match match )
    {
        return new Flow(MatchUtils.convert(match, HIGHEST_VERSION));
    }

    /**
     * Constructs a new {@code Flow} with provided match entries.
     * 
     * @param entries
     *            {@code MatchEntry} objects
     * @return a new {@code Flow} object
     */
    public static Flow of( MatchEntry<?>... entries )
    {
        return new Flow(MatchUtils.create(HIGHEST_VERSION, entries));
    }

    /**
     * Constructs a new {@code Flow} with provided match entries.
     * 
     * @param entries
     *            {@code MatchEntry} objects
     * @return a new {@code Flow} object
     */
    public static Flow of( Iterable<MatchEntry<?>> entries )
    {
        return new Flow(MatchUtils.create(HIGHEST_VERSION, entries));
    }

    /**
     * Constructs a new {@code Flow} with provided match entries.
     * 
     * @param entries
     *            a stream of {@code MatchEntry} objects
     * @return a new {@code Flow} object
     */
    public static Flow of( Stream<MatchEntry<?>> entries )
    {
        return new Flow(MatchUtils.create(HIGHEST_VERSION, entries));
    }

    /**
     * Constructs a new {@code Flow} with the match parsed from the provided
     * string.
     * 
     * @param s
     *            A string to be parsed into a {@code Match} object
     * @return a new {@code Flow} object
     * @exception IllegalArgumentException
     *                If the provided string does not represent a valid
     *                {@code Match} object
     */
    public static Flow parse( String s )
    {
        return of(MatchUtils.parse(s));
    }

    /**
     * Constructs a new {@code Flow} with the match parsed from the provided
     * string and {@code OFVersion}.
     * 
     * @param s
     *            A string to be parsed into a {@code Match} object
     * @param version
     *            The OF version used to parse the match from the string
     * @return a new {@code Flow} object
     * @exception IllegalArgumentException
     *                If the provided string does not represent a valid
     *                {@code Match} object
     */
    public static Flow parse( String s, OFVersion version )
    {
        return of(MatchUtils.parse(s, version));
    }

    public static boolean areInclusive( Flow a, Flow b )
    {
        return a.matchesAllOf(b) || b.matchesAllOf(a);
    }

    private final Match match;

    // these are lazily initialized
    private @Nullable ImmutableList<MatchEntry<?>> matchEntries = null;
    private @Nullable BitMatch                     bitMatch     = null;

    private Flow( Match match )
    {
        this.match = match;
    }

    /**
     * Returns this flow's match.
     * <p>
     * Note: the returned match will have the highest available OpenFlow
     * version.
     * 
     * @return a {@code Match} object
     */
    public Match getMatch()
    {
        return match;
    }

    /**
     * Returns this flow's match converted to the provided OpenFlow version.
     * 
     * @param version
     *            An OpenFlow version
     * @return a {@code Match} object
     */
    public Match getMatch( OFVersion version )
    {
        return MatchUtils.convert(match, version);
    }

    /**
     * Returns this flow's match converted using the provided OpenFlow factory.
     * 
     * @param fact
     *            An OpenFlow factory
     * @return a {@code Match} object
     */
    public Match getMatch( OFFactory fact )
    {
        return MatchUtils.convert(match, fact);
    }

    /**
     * Returns whether this flow is fully wildcarded or not.
     * 
     * @return a {@code boolean} value
     */
    public boolean isFullyWildcarded()
    {
        for (MatchEntry<?> entry : getMatchEntries()) {
            if (!entry.isWildcard()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns an immutable list with this flow's match entries.
     * 
     * @return an {@code ImmutableList} object
     */
    public ImmutableList<MatchEntry<?>> getMatchEntries()
    {
        return matchEntries();
    }

    /**
     * Returns this flow's match entry for the provided match field.
     * 
     * @param field
     *            An OpenFlow match field
     * @return a {@code MatchEntry} object
     */
    public <T extends OFValueType<T>> MatchEntry<T> getMatchEntry( MatchField<T> field )
    {
        return MatchEntry.fromMatch(match, field);
    }

    /**
     * Returns this flow's match in a bit format that allows for direct matching
     * against packet bytes/bits.
     * 
     * @return a {@code BitMatch} object
     */
    public BitMatch getBitMatch()
    {
        return bitMatch();
    }

    /**
     * Returns {@code true} if this flow matches all the possible packets that
     * are also matched by the provided flow; returns {@code false} otherwise.
     * <p>
     * More specifically, let {@code A} be the set of all possible packets that
     * are matched by this flow, and let {@code B} be the set of all possible
     * packets that are matched by the provided flow. This method returns
     * {@code true} if, and only if, the intersection of {@code A} and {@code B}
     * is {@code B}.
     * 
     * @param other
     *            Another {@code Flow} object
     * @return {@code true} if, and only if, this flow matches all the possible
     *         packets that are also matched by the provided flow
     */
    public boolean matchesAllOf( Flow other )
    {
        return MatchUtils.matchesAllOf(this.match, other.match);
    }

    @Override
    public void putTo( PrimitiveSink sink )
    {
        match.putTo(sink);
    }

    @Override
    public int compareTo( Flow other )
    {
        return Comparators.<MatchEntry<?>>comparingIterables()
            .compare(this.getMatchEntries(), other.getMatchEntries());
    }

    @Override
    public boolean equals( Object other )
    {
        return (other instanceof Flow) && this.equals((Flow)other);
    }

    public boolean equals( Flow other )
    {
        return (other != null)
               && (this.match.equals(other.match));
    }

    @Override
    public int hashCode()
    {
        return match.hashCode();
    }

    @Override
    public String toString()
    {
        return MatchUtils.toPrettyFormat(match);
    }

    public String toMatchString()
    {
        return matchEntries().toString();
    }

    private ImmutableList<MatchEntry<?>> matchEntries()
    {
        ImmutableList<MatchEntry<?>> entries = this.matchEntries;
        if (entries == null)
            this.matchEntries = entries = MatchEntry.listEntries(match);
        return entries;
    }

    private BitMatch bitMatch()
    {
        BitMatch bitMatch = this.bitMatch;
        if (bitMatch == null)
            this.bitMatch = bitMatch = createBitMatch();
        return bitMatch;
    }

    private BitMatch createBitMatch()
    {
        BitMatch.Builder builder = BitMatch.newBuilder();
        matchEntries().forEach(entry -> builder.add(entry.getBitMasked(match)));
        return builder.build();
    }

    @ReturnValuesAreNonnullByDefault
    public static final class IO
    {
        public static IOWriter<Flow> writer()
        {
            return FlowSerializer.INSTANCE;
        }

        public static IOReader<Flow> reader()
        {
            return FlowSerializer.INSTANCE;
        }

        private static enum FlowSerializer implements IOSerializer<Flow>
        {
            INSTANCE;

            @Override
            public void write( Flow flow, WritableByteChannel ch ) throws IOChannelWriteException
            {
                OFSerializers.matchSerializer().write(flow.match, ch);
            }

            @Override
            public void write( Flow flow, OutputStream out ) throws IOWriteException
            {
                OFSerializers.matchSerializer().write(flow.match, out);
            }

            @Override
            public void write( Flow flow, DataOutput out ) throws IOWriteException
            {
                OFSerializers.matchSerializer().write(flow.match, out);
            }

            @Override
            public Flow read( ReadableByteChannel ch ) throws IOChannelReadException
            {
                Match match = OFSerializers.matchSerializer().read(ch);
                return of(match);
            }

            @Override
            public Flow read( InputStream in ) throws IOReadException
            {
                Match match = OFSerializers.matchSerializer().read(in);
                return of(match);
            }

            @Override
            public Flow read( DataInput in ) throws IOReadException
            {
                Match match = OFSerializers.matchSerializer().read(in);
                return of(match);
            }
        }

        private IO()
        {
            // not used
        }
    }
}
