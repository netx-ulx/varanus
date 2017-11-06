package net.varanus.sdncontroller.types;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.PrimitiveSinkable;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.hash.PrimitiveSink;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.collect.CommonPair;
import net.varanus.util.collect.PairSlide;
import net.varanus.util.collect.builder.BaseBuilder;
import net.varanus.util.collect.builder.ImmutableListBuilder;
import net.varanus.util.io.Serializers;
import net.varanus.util.io.exception.IOChannelReadException;
import net.varanus.util.io.exception.IOChannelWriteException;
import net.varanus.util.io.exception.IOReadException;
import net.varanus.util.io.exception.IOWriteException;
import net.varanus.util.io.serializer.IOReader;
import net.varanus.util.io.serializer.IOWriter;
import net.varanus.util.lang.Comparators;
import net.varanus.util.openflow.types.Flow;
import net.varanus.util.openflow.types.Flowable;
import net.varanus.util.openflow.types.NodeId;
import net.varanus.util.openflow.types.PortId;
import net.varanus.util.text.StringUtils;


/**
 * 
 */
@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class DatapathPath implements Flowable<DatapathPath>, PrimitiveSinkable, Comparable<DatapathPath>
{
    public static HopBuilder newBuilderFromHops()
    {
        return new HopBuilder();
    }

    public static LinkBuilder newBuilderFromLinks( PortId entryPortId, PortId exitPortId )
    {
        return new LinkBuilder(entryPortId, exitPortId);
    }

    public static DatapathPath ofHops( Stream<DatapathHop> hops )
    {
        return newBuilderFromHops().addHops(hops).build();
    }

    public static DatapathPath ofHops( Iterable<DatapathHop> hops )
    {
        return newBuilderFromHops().addHops(hops).build();
    }

    public static DatapathPath ofHops( DatapathHop... hops )
    {
        return newBuilderFromHops().addHops(hops).build();
    }

    public static DatapathPath ofLinks( PortId entryPort, PortId exitPort, Stream<DatapathLink> links )
    {
        return newBuilderFromLinks(entryPort, exitPort).addLinks(links).build();
    }

    public static DatapathPath ofLinks( PortId entryPort, PortId exitPort, Iterable<DatapathLink> links )
    {
        return newBuilderFromLinks(entryPort, exitPort).addLinks(links).build();
    }

    public static DatapathPath ofLinks( PortId entryPort, PortId exitPort, DatapathLink... links )
    {
        return newBuilderFromLinks(entryPort, exitPort).addLinks(links).build();
    }

    // hops represent a sufficient state of this path
    private final ImmutableList<DatapathHop> hops;
    // for convenience, links are explicitly stored as well
    private final ImmutableList<DatapathLink> links;

    private DatapathPath( ImmutableList<DatapathHop> hops )
    {
        Preconditions.checkArgument(!hops.isEmpty(), "must pass at least one hop");
        this.hops = hops;
        this.links = buildLinks(hops);
    }

    private DatapathPath( PortId entryPortId, PortId exitPortId, ImmutableList<DatapathLink> links )
    {
        Preconditions.checkArgument(!links.isEmpty(), "must pass at least one link");
        this.hops = buildHops(entryPortId, exitPortId, links);
        this.links = links;
    }

    private static ImmutableList<DatapathLink> buildLinks( ImmutableList<DatapathHop> hops )
    {
        ImmutableListBuilder<DatapathLink> linkBuilder = ImmutableListBuilder.create();
        for (CommonPair<DatapathHop> pair : PairSlide.over(hops)) {
            linkBuilder.add(DatapathLink.betweenHops(pair.getFirst(), pair.getSecond()));
        }
        return linkBuilder.build();
    }

    // requires non-empty links
    private static ImmutableList<DatapathHop> buildHops( PortId entryPortId,
                                                         PortId exitPortId,
                                                         ImmutableList<DatapathLink> links )
    {
        ImmutableListBuilder<DatapathHop> hopBuilder = ImmutableListBuilder.create();
        hopBuilder.add(DatapathHop.beforeLink(entryPortId, links.get(0)));
        for (CommonPair<DatapathLink> pair : PairSlide.over(links)) {
            hopBuilder.add(DatapathHop.betweenLinks(pair.getFirst(), pair.getSecond()));
        }
        hopBuilder.add(DatapathHop.afterLink(links.get(links.size() - 1), exitPortId));
        return hopBuilder.build();
    }

    @Override
    public FlowedPath flowed( Flow flow )
    {
        return FlowedPath.of(this, flow);
    }

    public DatapathConnection getConnection()
    {
        DatapathHop entryHop = hops.get(0);
        NodeId entryNodeId = entryHop.getNodeId();
        PortId entryPortId = entryHop.getInPortId();
        DatapathHop exitHop = hops.get(hops.size() - 1);
        NodeId exitNodeId = exitHop.getNodeId();
        PortId exitPortId = exitHop.getOutPortId();
        return DatapathConnection.of(entryNodeId, entryPortId, exitNodeId, exitPortId);
    }

    public boolean hasLinks()
    {
        return !links.isEmpty();
    }

    public int numberOfLinks()
    {
        return links.size();
    }

    public ImmutableList<DatapathLink> getLinks()
    {
        return links;
    }

    public int numberOfHops()
    {
        return hops.size(); // always positive
    }

    public ImmutableList<DatapathHop> getHops()
    {
        return hops;
    }

    public DatapathPath reversed()
    {
        return newBuilderFromHops().addHops(
            hops.reverse().stream().map(DatapathHop::reversed))
            .build();
    }

    @Override
    public boolean equals( Object other )
    {
        return (other instanceof DatapathPath)
               && this.equals((DatapathPath)other);
    }

    public boolean equals( DatapathPath other )
    {
        return (other != null)
               && this.hops.equals(other.hops);
    }

    @Override
    public int hashCode()
    {
        return hops.hashCode();
    }

    @Override
    public int compareTo( DatapathPath other )
    {
        return Comparators.<DatapathHop>comparingIterables()
            .compare(this.hops, other.hops);
    }

    @Override
    public void putTo( PrimitiveSink sink )
    {
        for (DatapathHop hop : hops) {
            hop.putTo(sink);
        }
    }

    @Override
    public String toString()
    {
        return StringUtils.joinAll(" > ", hops);
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class HopBuilder implements BaseBuilder<DatapathPath>
    {
        private final ImmutableListBuilder<DatapathHop> builder;

        private HopBuilder()
        {
            this.builder = ImmutableListBuilder.create();
        }

        public HopBuilder addHop( DatapathHop hop )
        {
            builder.add(Objects.requireNonNull(hop));
            return this;
        }

        public HopBuilder addHops( Stream<DatapathHop> hops )
        {
            hops.forEachOrdered(this::addHop);
            return this;
        }

        public HopBuilder addHops( Iterable<DatapathHop> hops )
        {
            hops.forEach(this::addHop);
            return this;
        }

        public HopBuilder addHops( DatapathHop... hops )
        {
            return addHops(Stream.of(hops));
        }

        public HopBuilder clear()
        {
            builder.clear();
            return this;
        }

        @Override
        public DatapathPath build()
        {
            return new DatapathPath(builder.build());
        }
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class LinkBuilder implements BaseBuilder<DatapathPath>
    {
        private final PortId                             entryPortId;
        private final PortId                             exitPortId;
        private final ImmutableListBuilder<DatapathLink> builder;
        private @Nullable DatapathLink                   lastLink;

        private LinkBuilder( PortId entryPortId, PortId exitPortId )
        {
            this.entryPortId = entryPortId;
            this.exitPortId = exitPortId;
            this.builder = ImmutableListBuilder.create();
            this.lastLink = null;
        }

        public LinkBuilder addLink( DatapathLink link )
        {
            Objects.requireNonNull(link);
            DatapathLink lastLink = this.lastLink;
            if (lastLink != null)
                Preconditions.checkArgument(link.succeeds(lastLink),
                    "link %s must succeed last one %s", link, lastLink);
            builder.add(link);
            this.lastLink = link;
            return this;
        }

        public LinkBuilder addLinks( Stream<DatapathLink> links )
        {
            links.forEachOrdered(this::addLink);
            return this;
        }

        public LinkBuilder addLinks( Iterable<DatapathLink> links )
        {
            links.forEach(this::addLink);
            return this;
        }

        public LinkBuilder addLinks( DatapathLink... links )
        {
            return addLinks(Stream.of(links));
        }

        public LinkBuilder clear()
        {
            builder.clear();
            lastLink = null;
            return this;
        }

        @Override
        public DatapathPath build()
        {
            return new DatapathPath(entryPortId, exitPortId, builder.build());
        }
    }

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class IO
    {
        public static IOWriter<DatapathPath> writer()
        {
            return DatapathPathWriter.INSTANCE;
        }

        private static enum DatapathPathWriter implements IOWriter<DatapathPath>
        {
            INSTANCE;

            @Override
            public void write( DatapathPath path, WritableByteChannel ch ) throws IOChannelWriteException
            {
                Serializers.colWriter(DatapathHop.IO.writer()).write(path.hops, ch);
            }

            @Override
            public void write( DatapathPath path, OutputStream out ) throws IOWriteException
            {
                Serializers.colWriter(DatapathHop.IO.writer()).write(path.hops, out);
            }

            @Override
            public void write( DatapathPath path, DataOutput out ) throws IOWriteException
            {
                Serializers.colWriter(DatapathHop.IO.writer()).write(path.hops, out);
            }
        }

        public static IOReader<DatapathPath> reader()
        {
            return reader(NodeId.NIL_ID_ALIASER);
        }

        public static IOReader<DatapathPath> reader( Function<DatapathId, String> idAliaser )
        {
            Objects.requireNonNull(idAliaser);
            return new IOReader<DatapathPath>() {

                @Override
                public DatapathPath read( ReadableByteChannel ch ) throws IOChannelReadException
                {
                    return new DatapathPath(Serializers.immuListReader(DatapathHop.IO.reader(idAliaser)).read(ch));
                }

                @Override
                public DatapathPath read( InputStream in ) throws IOReadException
                {
                    return new DatapathPath(Serializers.immuListReader(DatapathHop.IO.reader(idAliaser)).read(in));
                }

                @Override
                public DatapathPath read( DataInput in ) throws IOReadException
                {
                    return new DatapathPath(Serializers.immuListReader(DatapathHop.IO.reader(idAliaser)).read(in));
                }
            };
        }

        private IO()
        {
            // not used
        }
    }
}
