package net.varanus.sdncontroller.types;


import java.util.Objects;
import java.util.function.Function;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import org.projectfloodlight.openflow.types.DatapathId;

import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.io.serializer.IOReader;
import net.varanus.util.io.serializer.IOWriter;
import net.varanus.util.openflow.types.AbstractFlowed;
import net.varanus.util.openflow.types.Flow;
import net.varanus.util.openflow.types.FlowDirectedNodePort;
import net.varanus.util.openflow.types.NodeId;
import net.varanus.util.openflow.types.NodePort;
import net.varanus.util.openflow.types.PortId;


@Immutable
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class FlowedHop extends AbstractFlowed<DatapathHop>
{
    public static FlowedHop of( DatapathHop hop, Flow flow )
    {
        return new FlowedHop(
            Objects.requireNonNull(hop),
            Objects.requireNonNull(flow));
    }

    public static FlowedHop of( NodeId nodeId, PortId inPortId, PortId outPortId, Flow flow )
    {
        return of(
            DatapathHop.of(nodeId, inPortId, outPortId),
            flow);
    }

    public static FlowedHop of( NodePort inNodePort, NodePort outNodePort, Flow flow )
    {
        return of(
            DatapathHop.of(inNodePort, outNodePort),
            flow);
    }

    private FlowedHop( DatapathHop unflowed, Flow flow )
    {
        super(unflowed, flow);
    }

    public NodeId getNodeId()
    {
        return unflowed.getNodeId();
    }

    public PortId getInPortId()
    {
        return unflowed.getInPortId();
    }

    public FlowDirectedNodePort getInEndpoint()
    {
        return unflowed.getInEndpoint().flowed(flow);
    }

    public PortId getOutPortId()
    {
        return unflowed.getOutPortId();
    }

    public FlowDirectedNodePort getOutEndpoint()
    {
        return unflowed.getOutEndpoint().flowed(flow);
    }

    public FlowedHop reversed()
    {
        return new FlowedHop(unflowed.reversed(), flow);
    }

    @Override
    public boolean equals( Object other )
    {
        return (other instanceof FlowedHop)
               && super.equals(other);
    }

    public boolean equals( FlowedHop other )
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
        public static IOWriter<FlowedHop> writer()
        {
            return FlowedIO.writer(DatapathHop.IO.writer());
        }

        public static IOReader<FlowedHop> reader()
        {
            return reader(NodeId.NIL_ID_ALIASER);
        }

        public static IOReader<FlowedHop> reader( Function<DatapathId, String> idAliaser )
        {
            return FlowedIO.reader(DatapathHop.IO.reader(idAliaser), FlowedHop::of);
        }

        private IO()
        {
            // not used
        }
    }
}
