package net.varanus.util.openflow.types;


import java.util.Objects;
import java.util.function.Function;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import org.projectfloodlight.openflow.types.DatapathId;

import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.io.serializer.IOReader;
import net.varanus.util.io.serializer.IOWriter;


/**
 * Like a {@link NodePort} but with an associated direction.
 */
@Immutable
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class DirectedNodePort extends AbstractDirected<NodePort> implements Flowable<DirectedNodePort>
{
    public static DirectedNodePort of( NodePort nodePort, TrafficDirection direction )
    {
        return new DirectedNodePort(
            Objects.requireNonNull(nodePort),
            Objects.requireNonNull(direction));
    }

    public static DirectedNodePort of( NodeId nodeId, DirectedPortId dirPortId )
    {
        return of(NodePort.of(nodeId, dirPortId.undirected()), dirPortId.getDirection());
    }

    public static DirectedNodePort of( NodeId nodeId, PortId portId, TrafficDirection direction )
    {
        return of(NodePort.of(nodeId, portId), direction);
    }

    private DirectedNodePort( NodePort undirected, TrafficDirection direction )
    {
        super(undirected, direction);
    }

    public NodeId getNodeId()
    {
        return undirected.getNodeId();
    }

    public PortId getPortId()
    {
        return undirected.getPortId();
    }

    public DirectedPortId getDirectedPortId()
    {
        return DirectedPortId.of(getPortId(), direction);
    }

    @Override
    public FlowDirectedNodePort flowed( Flow flow )
    {
        return FlowDirectedNodePort.of(this, flow);
    }

    @Override
    public boolean equals( Object other )
    {
        return (other instanceof DirectedNodePort)
               && super.equals(other);
    }

    public boolean equals( DirectedNodePort other )
    {
        return super.equals(other);
    }

    @Override
    public int hashCode()
    {
        return super.hashCode();
    }

    @Override
    public String toString()
    {
        return String.format("%s[%s]", getNodeId(), getDirectedPortId());
    }

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class IO
    {
        public static IOWriter<DirectedNodePort> writer()
        {
            return DirectedIO.writer(NodePort.IO.writer());
        }

        public static IOReader<DirectedNodePort> reader()
        {
            return reader(NodeId.NIL_ID_ALIASER);
        }

        public static IOReader<DirectedNodePort> reader( Function<DatapathId, String> idAliaser )
        {
            return DirectedIO.reader(NodePort.IO.reader(idAliaser), DirectedNodePort::of);
        }

        private IO()
        {
            // not used
        }
    }
}
