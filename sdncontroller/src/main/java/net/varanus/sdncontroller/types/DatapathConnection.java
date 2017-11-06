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
import net.varanus.util.openflow.types.TrafficDirection;
import net.varanus.util.openflow.types.UnidiNodePorts;


/**
 * 
 */
@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class DatapathConnection
    implements Flowable<DatapathConnection>, PrimitiveSinkable, Comparable<DatapathConnection>
{
    public static DatapathConnection of( NodePort entryPoint, NodePort exitPoint )
    {
        return new DatapathConnection(
            Objects.requireNonNull(entryPoint),
            Objects.requireNonNull(exitPoint));
    }

    public static DatapathConnection of( NodeId entryNodeId, PortId entryPortId, NodeId exitNodeId, PortId exitPortId )
    {
        return of(
            NodePort.of(entryNodeId, entryPortId),
            NodePort.of(exitNodeId, exitPortId));
    }

    public static DatapathConnection of( UnidiNodePorts unidiNodePorts )
    {
        return of(
            unidiNodePorts.getSource(),
            unidiNodePorts.getTarget());
    }

    public static DatapathConnection parse( String s, Function<DatapathId, String> idAliaser )
    {
        return LinkUtils.parseConnection(s, idAliaser);
    }

    public static DatapathConnection parse( String s )
    {
        return parse(s, NodeId.NIL_ID_ALIASER);
    }

    private final NodePort entryPoint;
    private final NodePort exitPoint;

    private DatapathConnection( NodePort entryPoint, NodePort exitPoint )
    {
        this.entryPoint = entryPoint;
        this.exitPoint = exitPoint;
    }

    @Override
    public FlowedConnection flowed( Flow flow )
    {
        return FlowedConnection.of(this, flow);
    }

    public NodeId getEntryNodeId()
    {
        return entryPoint.getNodeId();
    }

    public PortId getEntryPortId()
    {
        return entryPoint.getPortId();
    }

    public DirectedNodePort getEntryPoint()
    {
        return DirectedNodePort.of(entryPoint, TrafficDirection.INGRESS);
    }

    public NodeId getExitNodeId()
    {
        return exitPoint.getNodeId();
    }

    public PortId getExitPortId()
    {
        return exitPoint.getPortId();
    }

    public DirectedNodePort getExitPoint()
    {
        return DirectedNodePort.of(exitPoint, TrafficDirection.EGRESS);
    }

    public DatapathConnection reversed()
    {
        return new DatapathConnection(exitPoint, entryPoint);
    }

    public UnidiNodePorts toUnidiNodePorts()
    {
        return UnidiNodePorts.of(entryPoint, exitPoint);
    }

    public BidiNodePorts toBidiNodePorts()
    {
        return BidiNodePorts.of(entryPoint, exitPoint);
    }

    @Override
    public boolean equals( Object other )
    {
        return (other instanceof DatapathConnection) && this.equals((DatapathConnection)other);
    }

    public boolean equals( DatapathConnection other )
    {
        return (other != null)
               && this.entryPoint.equals(other.entryPoint)
               && this.exitPoint.equals(other.exitPoint);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(entryPoint, exitPoint);
    }

    @Override
    public int compareTo( DatapathConnection other )
    {
        return Comparator
            .comparing(DatapathConnection::getEntryPoint)
            .thenComparing(DatapathConnection::getExitPoint)
            .compare(this, other);
    }

    @Override
    public void putTo( PrimitiveSink sink )
    {
        entryPoint.putTo(sink);
        exitPoint.putTo(sink);
    }

    @Override
    public String toString()
    {
        return toString(true);
    }

    public String toString( boolean sortEndpoints )
    {
        if (sortEndpoints && Comparables.aGTb(entryPoint, exitPoint))
            return String.format("%s << %s", exitPoint, entryPoint);
        else
            return String.format("%s >> %s", entryPoint, exitPoint);
    }

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class IO
    {
        public static IOWriter<DatapathConnection> writer()
        {
            return DatapathConnectionWriter.INSTANCE;
        }

        private static enum DatapathConnectionWriter implements IOWriter<DatapathConnection>
        {
            INSTANCE;

            @Override
            public void write( DatapathConnection conn, WritableByteChannel ch ) throws IOChannelWriteException
            {
                NodePort.IO.writer().write(conn.entryPoint, ch);
                NodePort.IO.writer().write(conn.exitPoint, ch);
            }

            @Override
            public void write( DatapathConnection conn, OutputStream out ) throws IOWriteException
            {
                NodePort.IO.writer().write(conn.entryPoint, out);
                NodePort.IO.writer().write(conn.exitPoint, out);
            }

            @Override
            public void write( DatapathConnection conn, DataOutput out ) throws IOWriteException
            {
                NodePort.IO.writer().write(conn.entryPoint, out);
                NodePort.IO.writer().write(conn.exitPoint, out);
            }
        }

        public static IOReader<DatapathConnection> reader()
        {
            return reader(NodeId.NIL_ID_ALIASER);
        }

        public static IOReader<DatapathConnection> reader( Function<DatapathId, String> idAliaser )
        {
            Objects.requireNonNull(idAliaser);
            return new IOReader<DatapathConnection>() {

                @Override
                public DatapathConnection read( ReadableByteChannel ch ) throws IOChannelReadException
                {
                    NodePort entryPoint = NodePort.IO.reader(idAliaser).read(ch);
                    NodePort exitPoint = NodePort.IO.reader(idAliaser).read(ch);
                    return of(entryPoint, exitPoint);
                }

                @Override
                public DatapathConnection read( InputStream in ) throws IOReadException
                {
                    NodePort entryPoint = NodePort.IO.reader(idAliaser).read(in);
                    NodePort exitPoint = NodePort.IO.reader(idAliaser).read(in);
                    return of(entryPoint, exitPoint);
                }

                @Override
                public DatapathConnection read( DataInput in ) throws IOReadException
                {
                    NodePort entryPoint = NodePort.IO.reader(idAliaser).read(in);
                    NodePort exitPoint = NodePort.IO.reader(idAliaser).read(in);
                    return of(entryPoint, exitPoint);
                }
            };
        }

        private IO()
        {
            // not used
        }
    }
}
