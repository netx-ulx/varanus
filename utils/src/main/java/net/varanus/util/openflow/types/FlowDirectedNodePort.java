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
 * Like a {@link NodePort} but with an associated flow and direction.
 */
@Immutable
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class FlowDirectedNodePort extends AbstractFlowedDirected<NodePort, DirectedNodePort, FlowedNodePort>
{
    public static FlowDirectedNodePort of( NodePort nodePort, Flow flow, TrafficDirection direction )
    {
        return new FlowDirectedNodePort(
            Objects.requireNonNull(nodePort),
            Objects.requireNonNull(flow),
            Objects.requireNonNull(direction));
    }

    public static FlowDirectedNodePort of( DirectedNodePort dirNodePort, Flow flow )
    {
        return of(dirNodePort.undirected(), flow, dirNodePort.getDirection());
    }

    public static FlowDirectedNodePort of( FlowedNodePort floNodePort, TrafficDirection direction )
    {
        return of(floNodePort.unflowed(), floNodePort.getFlow(), direction);
    }

    public static FlowDirectedNodePort of( NodeId nodeId, DirectedPortId dirPortId, Flow flow )
    {
        return of(NodePort.of(nodeId, dirPortId.undirected()), flow, dirPortId.getDirection());
    }

    public static FlowDirectedNodePort of( NodeId nodeId,
                                           PortId portId,
                                           Flow flow,
                                           TrafficDirection direction )
    {
        return of(NodePort.of(nodeId, portId), flow, direction);
    }

    private FlowDirectedNodePort( NodePort unflowedirected, Flow flow, TrafficDirection direction )
    {
        super(unflowedirected, flow, direction);
    }

    public NodeId getNodeId()
    {
        return unflowDirected.getNodeId();
    }

    public PortId getPortId()
    {
        return unflowDirected.getPortId();
    }

    public DirectedPortId getDirectedPortId()
    {
        return unflowed().getDirectedPortId();
    }

    @Override
    public boolean equals( Object other )
    {
        return (other instanceof FlowDirectedNodePort)
               && super.equals(other);
    }

    public boolean equals( FlowDirectedNodePort other )
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
        return String.format("%s | %s", unflowed(), flow);
    }

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class IO
    {
        public static IOWriter<FlowDirectedNodePort> writer()
        {
            return FlowedDirectedIO.writer(NodePort.IO.writer());
        }

        public static IOReader<FlowDirectedNodePort> reader()
        {
            return reader(NodeId.NIL_ID_ALIASER);
        }

        public static IOReader<FlowDirectedNodePort> reader( Function<DatapathId, String> idAliaser )
        {
            return FlowedDirectedIO.reader(NodePort.IO.reader(idAliaser), FlowDirectedNodePort::of);
        }

        private IO()
        {
            // not used
        }
    }
}
