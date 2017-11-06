package net.varanus.sdncontroller.types;


import static net.varanus.sdncontroller.util.LinkUtils.getDestDirection;
import static net.varanus.sdncontroller.util.LinkUtils.getSrcDirection;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Function;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.PrimitiveSinkable;
import org.projectfloodlight.openflow.types.U64;

import com.google.common.hash.PrimitiveSink;

import net.floodlightcontroller.routing.Link;
import net.varanus.sdncontroller.util.LinkUtils;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.io.exception.IOChannelReadException;
import net.varanus.util.io.exception.IOChannelWriteException;
import net.varanus.util.io.exception.IOReadException;
import net.varanus.util.io.exception.IOWriteException;
import net.varanus.util.io.serializer.IOReader;
import net.varanus.util.io.serializer.IOWriter;
import net.varanus.util.lang.Comparables;
import net.varanus.util.openflow.types.BidiNodePorts;
import net.varanus.util.openflow.types.DirectedNodePort;
import net.varanus.util.openflow.types.Flow;
import net.varanus.util.openflow.types.Flowable;
import net.varanus.util.openflow.types.NodeId;
import net.varanus.util.openflow.types.NodePort;
import net.varanus.util.openflow.types.PortId;
import net.varanus.util.openflow.types.UnidiNodePorts;


/**
 * Like an immutable {@link Link} with no latency.
 */
@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class DatapathLink implements Flowable<DatapathLink>, PrimitiveSinkable, Comparable<DatapathLink>
{
    public static DatapathLink of( NodeId srcNodeId, PortId srcPortId, NodeId destNodeId, PortId destPortId )
    {
        return new DatapathLink(
            Objects.requireNonNull(srcNodeId),
            Objects.requireNonNull(srcPortId),
            Objects.requireNonNull(destNodeId),
            Objects.requireNonNull(destPortId));
    }

    public static DatapathLink of( NodePort srcNodePort, NodePort destNodePort )
    {
        return of(
            srcNodePort.getNodeId(),
            srcNodePort.getPortId(),
            destNodePort.getNodeId(),
            destNodePort.getPortId());
    }

    public static DatapathLink of( UnidiNodePorts unidiNodePorts )
    {
        return of(unidiNodePorts.getSource(), unidiNodePorts.getTarget());
    }

    public static DatapathLink betweenHops( DatapathHop fromHop, DatapathHop toHop )
    {
        return of(
            fromHop.getNodeId(),
            fromHop.getOutPortId(),
            toHop.getNodeId(),
            toHop.getInPortId());
    }

    public static DatapathLink fromMutableLink( Link link, Function<DatapathId, String> idAliaser )
    {
        return of(
            NodeId.of(link.getSrc(), idAliaser),
            PortId.of(link.getSrcPort()),
            NodeId.of(link.getDst(), idAliaser),
            PortId.of(link.getDstPort()));
    }

    public static DatapathLink fromMutableLink( Link link )
    {
        return fromMutableLink(link, NodeId.NIL_ID_ALIASER);
    }

    public static DatapathLink parse( String s, Function<DatapathId, String> idAliaser )
    {
        return LinkUtils.parseLink(s, idAliaser);
    }

    public static DatapathLink parse( String s )
    {
        return parse(s, NodeId.NIL_ID_ALIASER);
    }

    private final NodeId srcNodeId;
    private final PortId srcPortId;
    private final NodeId destNodeId;
    private final PortId destPortId;

    private DatapathLink( NodeId srcNodeId, PortId srcPortId, NodeId destNodeId, PortId destPortId )
    {
        this.srcNodeId = srcNodeId;
        this.srcPortId = srcPortId;
        this.destNodeId = destNodeId;
        this.destPortId = destPortId;
    }

    @Override
    public FlowedLink flowed( Flow flow )
    {
        return FlowedLink.of(this, flow);
    }

    public NodeId getSrcNode()
    {
        return srcNodeId;
    }

    public PortId getSrcPort()
    {
        return srcPortId;
    }

    public DirectedNodePort getSrcEndpoint()
    {
        return NodePort.of(srcNodeId, srcPortId).directed(getSrcDirection());
    }

    public NodeId getDestNode()
    {
        return destNodeId;
    }

    public PortId getDestPort()
    {
        return destPortId;
    }

    public DirectedNodePort getDestEndpoint()
    {
        return NodePort.of(destNodeId, destPortId).directed(getDestDirection());
    }

    public NodeId getNode( EndpointKind kind )
    {
        switch (kind) {
            case SOURCE:
                return srcNodeId;

            case DESTINATION:
                return destNodeId;

            default:
                throw new AssertionError("unexpected enum value");
        }
    }

    public PortId getPort( EndpointKind kind )
    {
        switch (kind) {
            case SOURCE:
                return srcPortId;

            case DESTINATION:
                return destPortId;

            default:
                throw new AssertionError("unexpected enum value");
        }
    }

    public DirectedNodePort getEndpoint( EndpointKind kind )
    {
        return NodePort.of(getNode(kind), getPort(kind)).directed(kind.getTrafficDirection());
    }

    public boolean hasSrcNode( NodeId nodeId )
    {
        return nodeId.equals(srcNodeId);
    }

    public boolean hasSrcPort( PortId portId )
    {
        return portId.equals(srcPortId);
    }

    public boolean hasDestNode( NodeId nodeId )
    {
        return nodeId.equals(destNodeId);
    }

    public boolean hasDestPort( PortId portId )
    {
        return portId.equals(destPortId);
    }

    public boolean hasNode( NodeId nodeId )
    {
        return hasSrcNode(nodeId) || hasDestNode(nodeId);
    }

    public boolean hasPort( PortId portId )
    {
        return hasSrcPort(portId) || hasDestPort(portId);
    }

    public DatapathLink reversed()
    {
        return new DatapathLink(destNodeId, destPortId, srcNodeId, srcPortId);
    }

    public boolean coincidesWith( DatapathLink other )
    {
        return this.srcNodeId.equals(other.srcNodeId)
               || this.srcNodeId.equals(other.destNodeId)
               || this.destNodeId.equals(other.srcNodeId)
               || this.destNodeId.equals(other.destNodeId);
    }

    public boolean succeeds( DatapathLink other )
    {
        return this.srcNodeId.equals(other.destNodeId);
    }

    public boolean precedes( DatapathLink other )
    {
        return this.destNodeId.equals(other.srcNodeId);
    }

    public Link toMutableLink()
    {
        return new Link(
            srcNodeId.getDpid(),
            srcPortId.getOFPort(),
            destNodeId.getDpid(),
            destPortId.getOFPort(),
            U64.ZERO);
    }

    public UnidiNodePorts toUnidiNodePorts()
    {
        return UnidiNodePorts.of(
            NodePort.of(srcNodeId, srcPortId),
            NodePort.of(destNodeId, destPortId));
    }

    public BidiNodePorts toBidiNodePorts()
    {
        return BidiNodePorts.of(
            NodePort.of(srcNodeId, srcPortId),
            NodePort.of(destNodeId, destPortId));
    }

    @Override
    public boolean equals( Object other )
    {
        return (other instanceof DatapathLink) && this.equals((DatapathLink)other);
    }

    public boolean equals( DatapathLink other )
    {
        return other != null
               && this.destNodeId.equals(other.destNodeId)
               && this.destPortId.equals(other.destPortId)
               && this.srcNodeId.equals(other.srcNodeId)
               && this.srcPortId.equals(other.srcPortId);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(destNodeId, destPortId, srcNodeId, srcPortId);
    }

    @Override
    public int compareTo( DatapathLink other )
    {
        return Comparator
            .comparing(DatapathLink::getSrcNode)
            .thenComparing(DatapathLink::getSrcPort)
            .thenComparing(DatapathLink::getDestNode)
            .thenComparing(DatapathLink::getDestPort)
            .compare(this, other);
    }

    @Override
    public void putTo( PrimitiveSink sink )
    {
        srcNodeId.putTo(sink);
        srcPortId.putTo(sink);
        destNodeId.putTo(sink);
        destPortId.putTo(sink);
    }

    @Override
    public String toString()
    {
        return toString(true);
    }

    public String toString( boolean sortEndpoints )
    {
        NodePort srcEndpt = getSrcEndpoint().undirected();
        NodePort destEndpt = getDestEndpoint().undirected();
        if (sortEndpoints && Comparables.aGTb(srcEndpt, destEndpt))
            return String.format("%s < %s", destEndpt, srcEndpt);
        else
            return String.format("%s > %s", srcEndpt, destEndpt);
    }

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class IO
    {
        public static IOWriter<DatapathLink> writer()
        {
            return DatapathLinkWriter.INSTANCE;
        }

        private static enum DatapathLinkWriter implements IOWriter<DatapathLink>
        {
            INSTANCE;

            @Override
            public void write( DatapathLink link, WritableByteChannel ch ) throws IOChannelWriteException
            {
                NodeId.IO.writer().write(link.srcNodeId, ch);
                PortId.IO.writer().write(link.srcPortId, ch);
                NodeId.IO.writer().write(link.destNodeId, ch);
                PortId.IO.writer().write(link.destPortId, ch);
            }

            @Override
            public void write( DatapathLink link, OutputStream out ) throws IOWriteException
            {
                NodeId.IO.writer().write(link.srcNodeId, out);
                PortId.IO.writer().write(link.srcPortId, out);
                NodeId.IO.writer().write(link.destNodeId, out);
                PortId.IO.writer().write(link.destPortId, out);
            }

            @Override
            public void write( DatapathLink link, DataOutput out ) throws IOWriteException
            {
                NodeId.IO.writer().write(link.srcNodeId, out);
                PortId.IO.writer().write(link.srcPortId, out);
                NodeId.IO.writer().write(link.destNodeId, out);
                PortId.IO.writer().write(link.destPortId, out);
            }
        }

        public static IOReader<DatapathLink> reader()
        {
            return reader(NodeId.NIL_ID_ALIASER);
        }

        public static IOReader<DatapathLink> reader( Function<DatapathId, String> idAliaser )
        {
            Objects.requireNonNull(idAliaser);
            return new IOReader<DatapathLink>() {

                @Override
                public DatapathLink read( ReadableByteChannel ch ) throws IOChannelReadException
                {
                    NodeId srcNodeId = NodeId.IO.reader(idAliaser).read(ch);
                    PortId srcPortId = PortId.IO.reader().read(ch);
                    NodeId destNodeId = NodeId.IO.reader(idAliaser).read(ch);
                    PortId destPortId = PortId.IO.reader().read(ch);
                    return of(srcNodeId, srcPortId, destNodeId, destPortId);
                }

                @Override
                public DatapathLink read( InputStream in ) throws IOReadException
                {
                    NodeId srcNodeId = NodeId.IO.reader(idAliaser).read(in);
                    PortId srcPortId = PortId.IO.reader().read(in);
                    NodeId destNodeId = NodeId.IO.reader(idAliaser).read(in);
                    PortId destPortId = PortId.IO.reader().read(in);
                    return of(srcNodeId, srcPortId, destNodeId, destPortId);
                }

                @Override
                public DatapathLink read( DataInput in ) throws IOReadException
                {
                    NodeId srcNodeId = NodeId.IO.reader(idAliaser).read(in);
                    PortId srcPortId = PortId.IO.reader().read(in);
                    NodeId destNodeId = NodeId.IO.reader(idAliaser).read(in);
                    PortId destPortId = PortId.IO.reader().read(in);
                    return of(srcNodeId, srcPortId, destNodeId, destPortId);
                }
            };
        }

        private IO()
        {
            // not used
        }
    }
}
