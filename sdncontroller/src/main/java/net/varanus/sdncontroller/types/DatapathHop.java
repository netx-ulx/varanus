package net.varanus.sdncontroller.types;


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

import com.google.common.hash.PrimitiveSink;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.io.exception.IOChannelReadException;
import net.varanus.util.io.exception.IOChannelWriteException;
import net.varanus.util.io.exception.IOReadException;
import net.varanus.util.io.exception.IOWriteException;
import net.varanus.util.io.serializer.IOReader;
import net.varanus.util.io.serializer.IOWriter;
import net.varanus.util.openflow.types.DirectedNodePort;
import net.varanus.util.openflow.types.Flow;
import net.varanus.util.openflow.types.Flowable;
import net.varanus.util.openflow.types.NodeId;
import net.varanus.util.openflow.types.NodePort;
import net.varanus.util.openflow.types.PortId;
import net.varanus.util.openflow.types.TrafficDirection;


/**
 * 
 */
@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class DatapathHop implements Flowable<DatapathHop>, PrimitiveSinkable, Comparable<DatapathHop>
{
    public static DatapathHop of( NodeId nodeId, PortId inPortId, PortId outPortId )
    {
        return new DatapathHop(
            Objects.requireNonNull(nodeId),
            Objects.requireNonNull(inPortId),
            Objects.requireNonNull(outPortId));
    }

    public static DatapathHop of( NodePort inNodePort, NodePort outNodePort )
    {
        if (!inNodePort.getNodeId().equals(outNodePort.getNodeId()))
            throw new IllegalArgumentException("node-ports must have the same node");

        NodeId nodeId = inNodePort.getNodeId();
        return of(nodeId, inNodePort.getPortId(), outNodePort.getPortId());
    }

    public static DatapathHop beforeLink( PortId inPortId, DatapathLink link )
    {
        NodeId nodeId = link.getSrcNode();
        PortId outPortId = link.getSrcPort();
        return of(nodeId, inPortId, outPortId);
    }

    public static DatapathHop betweenLinks( DatapathLink fromLink, DatapathLink toLink )
    {
        if (!fromLink.getDestNode().equals(toLink.getSrcNode()))
            throw new IllegalArgumentException("links are not adjacent");

        NodeId nodeId = fromLink.getDestNode();
        PortId inPortId = fromLink.getDestPort();
        PortId outPortId = toLink.getSrcPort();
        return of(nodeId, inPortId, outPortId);
    }

    public static DatapathHop afterLink( DatapathLink link, PortId outPortId )
    {
        NodeId nodeId = link.getDestNode();
        PortId inPortId = link.getDestPort();
        return of(nodeId, inPortId, outPortId);
    }

    private final NodeId nodeId;
    private final PortId inPortId;
    private final PortId outPortId;

    private DatapathHop( NodeId nodeId, PortId inPortId, PortId outPortId )
    {
        this.nodeId = nodeId;
        this.inPortId = inPortId;
        this.outPortId = outPortId;
    }

    @Override
    public FlowedHop flowed( Flow flow )
    {
        return FlowedHop.of(this, flow);
    }

    public NodeId getNodeId()
    {
        return nodeId;
    }

    public PortId getInPortId()
    {
        return inPortId;
    }

    public DirectedNodePort getInEndpoint()
    {
        return DirectedNodePort.of(nodeId, inPortId, TrafficDirection.INGRESS);
    }

    public PortId getOutPortId()
    {
        return outPortId;
    }

    public DirectedNodePort getOutEndpoint()
    {
        return DirectedNodePort.of(nodeId, outPortId, TrafficDirection.EGRESS);
    }

    public DatapathHop reversed()
    {
        return new DatapathHop(nodeId, outPortId, inPortId);
    }

    @Override
    public boolean equals( Object other )
    {
        return (other instanceof DatapathHop) && this.equals((DatapathHop)other);
    }

    public boolean equals( DatapathHop other )
    {
        return (other != null)
               && this.nodeId.equals(other.nodeId)
               && this.inPortId.equals(other.inPortId)
               && this.outPortId.equals(other.outPortId);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(nodeId, inPortId, outPortId);
    }

    @Override
    public int compareTo( DatapathHop other )
    {
        return Comparator
            .comparing(DatapathHop::getNodeId)
            .thenComparing(DatapathHop::getInPortId)
            .thenComparing(DatapathHop::getOutPortId)
            .compare(this, other);
    }

    @Override
    public void putTo( PrimitiveSink sink )
    {
        nodeId.putTo(sink);
        inPortId.putTo(sink);
        outPortId.putTo(sink);
    }

    @Override
    public String toString()
    {
        return String.format("[%s]%s[%s]", inPortId, nodeId, outPortId);
    }

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class IO
    {
        public static IOWriter<DatapathHop> writer()
        {
            return DatapathHopWriter.INSTANCE;
        }

        private static enum DatapathHopWriter implements IOWriter<DatapathHop>
        {
            INSTANCE;

            @Override
            public void write( DatapathHop hop, WritableByteChannel ch ) throws IOChannelWriteException
            {
                NodeId.IO.writer().write(hop.nodeId, ch);
                PortId.IO.writer().write(hop.inPortId, ch);
                PortId.IO.writer().write(hop.outPortId, ch);
            }

            @Override
            public void write( DatapathHop hop, OutputStream out ) throws IOWriteException
            {
                NodeId.IO.writer().write(hop.nodeId, out);
                PortId.IO.writer().write(hop.inPortId, out);
                PortId.IO.writer().write(hop.outPortId, out);
            }

            @Override
            public void write( DatapathHop hop, DataOutput out ) throws IOWriteException
            {
                NodeId.IO.writer().write(hop.nodeId, out);
                PortId.IO.writer().write(hop.inPortId, out);
                PortId.IO.writer().write(hop.outPortId, out);
            }
        }

        public static IOReader<DatapathHop> reader()
        {
            return reader(NodeId.NIL_ID_ALIASER);
        }

        public static IOReader<DatapathHop> reader( Function<DatapathId, String> idAliaser )
        {
            Objects.requireNonNull(idAliaser);
            return new IOReader<DatapathHop>() {

                @Override
                public DatapathHop read( ReadableByteChannel ch ) throws IOChannelReadException
                {
                    NodeId nodeId = NodeId.IO.reader(idAliaser).read(ch);
                    PortId inPortId = PortId.IO.reader().read(ch);
                    PortId outPortId = PortId.IO.reader().read(ch);
                    return of(nodeId, inPortId, outPortId);
                }

                @Override
                public DatapathHop read( InputStream in ) throws IOReadException
                {
                    NodeId nodeId = NodeId.IO.reader(idAliaser).read(in);
                    PortId inPortId = PortId.IO.reader().read(in);
                    PortId outPortId = PortId.IO.reader().read(in);
                    return of(nodeId, inPortId, outPortId);
                }

                @Override
                public DatapathHop read( DataInput in ) throws IOReadException
                {
                    NodeId nodeId = NodeId.IO.reader(idAliaser).read(in);
                    PortId inPortId = PortId.IO.reader().read(in);
                    PortId outPortId = PortId.IO.reader().read(in);
                    return of(nodeId, inPortId, outPortId);
                }
            };
        }

        private IO()
        {
            // not used
        }
    }
}
