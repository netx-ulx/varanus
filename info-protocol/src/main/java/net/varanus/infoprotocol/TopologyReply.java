package net.varanus.infoprotocol;


import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import org.jgrapht.UndirectedGraph;
import org.jgrapht.graph.Pseudograph;
import org.jgrapht.graph.UnmodifiableUndirectedGraph;
import org.projectfloodlight.openflow.types.DatapathId;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableSet;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.functional.Report;
import net.varanus.util.graph.GraphUtils;
import net.varanus.util.io.Serializers;
import net.varanus.util.io.exception.IOChannelReadException;
import net.varanus.util.io.exception.IOChannelWriteException;
import net.varanus.util.io.serializer.IOReader;
import net.varanus.util.io.serializer.IOWriter;
import net.varanus.util.lang.MoreObjects;
import net.varanus.util.openflow.types.BidiNodePorts;
import net.varanus.util.openflow.types.NodeId;


/**
 * 
 */
@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class TopologyReply extends Generic.Topo
{
    public static TopologyReply of( Iterable<NodeId> switches,
                                    Iterable<BidiNodePorts> links,
                                    ImmutableSet<BidiNodePorts> disabledLinks )
    {
        return new TopologyReply(Report.of(new Result(
            Objects.requireNonNull(switches),
            Objects.requireNonNull(links),
            Objects.requireNonNull(disabledLinks))));
    }

    public static TopologyReply ofError( String errorMsg )
    {
        return new TopologyReply(Report.ofError(errorMsg));
    }

    private final Report<Result> result;

    private TopologyReply( Report<Result> result )
    {
        this.result = result;
    }

    public boolean hasResult()
    {
        return result.hasValue();
    }

    public UndirectedGraph<NodeId, BidiNodePorts> getGraph() throws NoSuchElementException
    {
        return result.getValue().graph;
    }

    public ImmutableSet<BidiNodePorts> getDisabledLinks() throws NoSuchElementException
    {
        return result.getValue().disabledLinks;
    }

    public boolean hasError()
    {
        return result.hasError();
    }

    public String getError() throws NoSuchElementException
    {
        return result.getError();
    }

    @Override
    public String toString()
    {
        return String.format("( %s )", result);
    }

    @Immutable
    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    private static final class Result
    {
        final UnmodifiableUndirectedGraph<NodeId, BidiNodePorts> graph;
        final ImmutableSet<BidiNodePorts>                        disabledLinks;

        Result( Iterable<NodeId> switches, Iterable<BidiNodePorts> links, ImmutableSet<BidiNodePorts> disabledLinks )
        {
            Pseudograph<NodeId, BidiNodePorts> graph = new Pseudograph<>(GraphUtils.nullEdgeFactory());
            for (NodeId swId : switches) {
                graph.addVertex(swId);
            }
            for (BidiNodePorts link : links) {
                graph.addEdge(link.getMin().getNodeId(), link.getMax().getNodeId(), link);
            }
            this.graph = new UnmodifiableUndirectedGraph<>(graph);
            this.disabledLinks = disabledLinks;
        }

        @Override
        public String toString()
        {
            return String.format("switches%s, links%s, disabled_links%s",
                graph.vertexSet(),
                graph.edgeSet(),
                disabledLinks);
        }
    }

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class IO
    {
        private static final long REPLY_PREAMBLE = 0xb7b203f290b68461L;

        public static IOWriter<TopologyReply> writer( Logger log )
        {
            Objects.requireNonNull(log);
            return new IOWriter<TopologyReply>() {
                @Override
                public void write( TopologyReply reply, WritableByteChannel ch ) throws IOChannelWriteException
                {
                    if (log.isTraceEnabled()) {
                        log.trace("Writing topology reply from server: reply preamble 0x{}",
                            Long.toHexString(REPLY_PREAMBLE));
                    }
                    Serializers.longWriter(ByteOrder.BIG_ENDIAN).writeLong(REPLY_PREAMBLE, ch);

                    Report<Result> result = reply.result;
                    log.trace("Writing topology reply from server: {}", result);
                    Report.IO.writer(resultWriter()).write(result, ch);
                }
            };
        }

        public static IOReader<TopologyReply> reader( Function<DatapathId, String> idAliaser, Logger log )
        {
            MoreObjects.requireNonNull(idAliaser, "idAliaser", log, "log");
            return new IOReader<TopologyReply>() {
                @Override
                public TopologyReply read( ReadableByteChannel ch ) throws IOChannelReadException
                {
                    final long preamble = Serializers.longReader(ByteOrder.BIG_ENDIAN).readLong(ch);
                    if (preamble != REPLY_PREAMBLE) {
                        throw new IOChannelReadException(
                            String.format("expected reply preamble of %x from server but found %x instead",
                                REPLY_PREAMBLE, preamble));
                    }
                    if (log.isTraceEnabled()) {
                        log.trace("Read topology reply from server: reply preamble 0x{}",
                            Long.toHexString(preamble));
                    }

                    Report<Result> result = Report.IO.reader(resultReader(idAliaser)).read(ch);
                    log.trace("Read topology reply from server: {}", result);

                    return new TopologyReply(result);
                }
            };
        }

        private static IOWriter<Result> resultWriter()
        {
            return new IOWriter<Result>() {

                @Override
                public void write( Result res, WritableByteChannel ch ) throws IOChannelWriteException
                {
                    Serializers.colWriter(NodeId.IO.writer()).write(res.graph.vertexSet(), ch);
                    Serializers.colWriter(BidiNodePorts.IO.writer()).write(res.graph.edgeSet(), ch);
                    Serializers.colWriter(BidiNodePorts.IO.writer()).write(res.disabledLinks, ch);
                }
            };
        }

        private static IOReader<Result> resultReader( Function<DatapathId, String> idAliaser )
        {
            IOReader<NodeId> switchReader = NodeId.IO.reader(idAliaser);
            IOReader<BidiNodePorts> linkReader = BidiNodePorts.IO.reader(idAliaser);

            return new IOReader<Result>() {

                @Override
                public Result read( ReadableByteChannel ch ) throws IOChannelReadException
                {
                    List<NodeId> switches = Serializers.listReader(switchReader).read(ch);
                    List<BidiNodePorts> links = Serializers.listReader(linkReader).read(ch);
                    ImmutableSet<BidiNodePorts> disabledLinks = Serializers.immuSetReader(linkReader).read(ch);
                    return new Result(switches, links, disabledLinks);
                }
            };
        }

        private IO()
        {
            // not used
        }
    }
}
