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
 * Like a {@link NodePort} but with an associated flow.
 */
@Immutable
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class FlowedNodePort extends AbstractFlowed<NodePort> implements Directable<FlowedNodePort>
{
    public static FlowedNodePort of( NodePort nodePort, Flow flow )
    {
        return new FlowedNodePort(
            Objects.requireNonNull(nodePort),
            Objects.requireNonNull(flow));
    }

    public static FlowedNodePort of( NodeId nodeId, PortId portId, Flow flow )
    {
        return of(NodePort.of(nodeId, portId), flow);
    }

    private FlowedNodePort( NodePort unflowed, Flow flow )
    {
        super(unflowed, flow);
    }

    public NodeId getNodeId()
    {
        return unflowed.getNodeId();
    }

    public PortId getPortId()
    {
        return unflowed.getPortId();
    }

    @Override
    public FlowDirectedNodePort directed( TrafficDirection direction )
    {
        return FlowDirectedNodePort.of(this, direction);
    }

    @Override
    public boolean equals( Object other )
    {
        return (other instanceof FlowedNodePort)
               && super.equals(other);
    }

    public boolean equals( FlowedNodePort other )
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
        return super.toString();
    }

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class IO
    {
        public static IOWriter<FlowedNodePort> writer()
        {
            return FlowedIO.writer(NodePort.IO.writer());
        }

        public static IOReader<FlowedNodePort> reader()
        {
            return reader(NodeId.NIL_ID_ALIASER);
        }

        public static IOReader<FlowedNodePort> reader( Function<DatapathId, String> idAliaser )
        {
            return FlowedIO.reader(NodePort.IO.reader(idAliaser), FlowedNodePort::of);
        }

        private IO()
        {
            // not used
        }
    }
}
