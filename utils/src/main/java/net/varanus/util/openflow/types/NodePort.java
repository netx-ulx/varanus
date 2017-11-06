package net.varanus.util.openflow.types;


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
import net.varanus.util.openflow.NodePortUtils;


/**
 * 
 */
@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class NodePort
    implements Flowable<NodePort>, Directable<NodePort>, PrimitiveSinkable, Comparable<NodePort>
{
    public static NodePort of( NodeId nodeId, PortId portId )
    {
        return new NodePort(
            Objects.requireNonNull(nodeId),
            Objects.requireNonNull(portId));
    }

    public static NodePort parse( String s, Function<DatapathId, String> idAliaser )
    {
        return NodePortUtils.parseNodePort(s, idAliaser);
    }

    public static NodePort parse( String s )
    {
        return parse(s, NodeId.NIL_ID_ALIASER);
    }

    private final NodeId nodeId;
    private final PortId portId;

    private NodePort( NodeId nodeId, PortId portId )
    {
        this.nodeId = nodeId;
        this.portId = portId;
    }

    public NodeId getNodeId()
    {
        return nodeId;
    }

    public PortId getPortId()
    {
        return portId;
    }

    @Override
    public FlowedNodePort flowed( Flow flow )
    {
        return FlowedNodePort.of(this, flow);
    }

    @Override
    public DirectedNodePort directed( TrafficDirection direction )
    {
        return DirectedNodePort.of(this, direction);
    }

    @Override
    public boolean equals( Object other )
    {
        return (other instanceof NodePort)
               && this.equals((NodePort)other);
    }

    public boolean equals( NodePort other )
    {
        return (other != null)
               && this.nodeId.equals(other.nodeId)
               && this.portId.equals(other.portId);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(nodeId, portId);
    }

    @Override
    public int compareTo( NodePort other )
    {
        return Comparator
            .comparing(NodePort::getNodeId)
            .thenComparing(NodePort::getPortId)
            .compare(this, other);
    }

    @Override
    public void putTo( PrimitiveSink sink )
    {
        nodeId.putTo(sink);
        portId.putTo(sink);
    }

    @Override
    public String toString()
    {
        return String.format("%s[%s]", nodeId, portId);
    }

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class IO
    {
        public static IOWriter<NodePort> writer()
        {
            return NodePortWriter.INSTANCE;
        }

        private static enum NodePortWriter implements IOWriter<NodePort>
        {
            INSTANCE;

            @Override
            public void write( NodePort nodePort, WritableByteChannel ch ) throws IOChannelWriteException
            {
                NodeId.IO.writer().write(nodePort.nodeId, ch);
                PortId.IO.writer().write(nodePort.portId, ch);
            }

            @Override
            public void write( NodePort nodePort, OutputStream out ) throws IOWriteException
            {
                NodeId.IO.writer().write(nodePort.nodeId, out);
                PortId.IO.writer().write(nodePort.portId, out);
            }

            @Override
            public void write( NodePort nodePort, DataOutput out ) throws IOWriteException
            {
                NodeId.IO.writer().write(nodePort.nodeId, out);
                PortId.IO.writer().write(nodePort.portId, out);
            }
        }

        public static IOReader<NodePort> reader()
        {
            return reader(NodeId.NIL_ID_ALIASER);
        }

        public static IOReader<NodePort> reader( Function<DatapathId, String> idAliaser )
        {
            Objects.requireNonNull(idAliaser);
            return new IOReader<NodePort>() {

                @Override
                public NodePort read( ReadableByteChannel ch ) throws IOChannelReadException
                {
                    NodeId nodeId = NodeId.IO.reader(idAliaser).read(ch);
                    PortId portId = PortId.IO.reader().read(ch);
                    return of(nodeId, portId);
                }

                @Override
                public NodePort read( InputStream in ) throws IOReadException
                {
                    NodeId nodeId = NodeId.IO.reader(idAliaser).read(in);
                    PortId portId = PortId.IO.reader().read(in);
                    return of(nodeId, portId);
                }

                @Override
                public NodePort read( DataInput in ) throws IOReadException
                {
                    NodeId nodeId = NodeId.IO.reader(idAliaser).read(in);
                    PortId portId = PortId.IO.reader().read(in);
                    return of(nodeId, portId);
                }
            };
        }

        private IO()
        {
            // not used
        }
    }
}
