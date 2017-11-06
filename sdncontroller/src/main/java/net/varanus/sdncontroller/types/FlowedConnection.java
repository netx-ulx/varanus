package net.varanus.sdncontroller.types;


import java.util.Objects;
import java.util.function.Function;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import org.projectfloodlight.openflow.types.DatapathId;

import net.varanus.sdncontroller.util.LinkUtils;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.io.serializer.IOReader;
import net.varanus.util.io.serializer.IOWriter;
import net.varanus.util.openflow.types.AbstractFlowed;
import net.varanus.util.openflow.types.Flow;
import net.varanus.util.openflow.types.FlowDirectedNodePort;
import net.varanus.util.openflow.types.FlowedBidiNodePorts;
import net.varanus.util.openflow.types.FlowedUnidiNodePorts;
import net.varanus.util.openflow.types.NodeId;
import net.varanus.util.openflow.types.NodePort;
import net.varanus.util.openflow.types.PortId;
import net.varanus.util.openflow.types.UnidiNodePorts;


/**
 * 
 */
@Immutable
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class FlowedConnection extends AbstractFlowed<DatapathConnection>
{
    public static FlowedConnection of( DatapathConnection connection, Flow flow )
    {
        return new FlowedConnection(Objects.requireNonNull(connection), Objects.requireNonNull(flow));
    }

    public static FlowedConnection of( NodePort entryPoint, NodePort exitPoint, Flow flow )
    {
        return of(DatapathConnection.of(entryPoint, exitPoint), flow);
    }

    public static FlowedConnection of( NodeId entryNodeId,
                                       PortId entryPortId,
                                       NodeId exitNodeId,
                                       PortId exitPortId,
                                       Flow flow )
    {
        return of(DatapathConnection.of(entryNodeId, entryPortId, exitNodeId, exitPortId), flow);
    }

    public static FlowedConnection of( UnidiNodePorts unidiNodePorts, Flow flow )
    {
        return of(unidiNodePorts.getSource(), unidiNodePorts.getTarget(), flow);
    }

    public static FlowedConnection of( FlowedUnidiNodePorts unidiNodePorts )
    {
        return of(unidiNodePorts.getSource(), unidiNodePorts.getTarget(), unidiNodePorts.getFlow());
    }

    public static FlowedConnection parse( String s, Function<DatapathId, String> idAliaser )
    {
        return LinkUtils.parseFlowedConnection(s, idAliaser);
    }

    public static FlowedConnection parse( String s )
    {
        return parse(s, NodeId.NIL_ID_ALIASER);
    }

    private FlowedConnection( DatapathConnection unflowed, Flow flow )
    {
        super(unflowed, flow);
    }

    public NodeId getEntryNodeId()
    {
        return unflowed.getEntryNodeId();
    }

    public PortId getEntryPortId()
    {
        return unflowed.getEntryPortId();
    }

    public FlowDirectedNodePort getEntryPoint()
    {
        return unflowed.getEntryPoint().flowed(flow);
    }

    public NodeId getExitNodeId()
    {
        return unflowed.getExitNodeId();
    }

    public PortId getExitPortId()
    {
        return unflowed.getExitPortId();
    }

    public FlowDirectedNodePort getExitPoint()
    {
        return unflowed.getExitPoint().flowed(flow);
    }

    public FlowedConnection reversed()
    {
        return new FlowedConnection(unflowed.reversed(), flow);
    }

    public FlowedUnidiNodePorts toUnidiNodePorts()
    {
        return FlowedUnidiNodePorts.of(unflowed.toUnidiNodePorts(), flow);
    }

    public FlowedBidiNodePorts toBidiNodePorts()
    {
        return FlowedBidiNodePorts.of(unflowed.toBidiNodePorts(), flow);
    }

    @Override
    public boolean equals( Object other )
    {
        return (other instanceof FlowedConnection)
               && super.equals(other);
    }

    public boolean equals( FlowedConnection other )
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
        public static IOWriter<FlowedConnection> writer()
        {
            return FlowedIO.writer(DatapathConnection.IO.writer());
        }

        public static IOReader<FlowedConnection> reader()
        {
            return reader(NodeId.NIL_ID_ALIASER);
        }

        public static IOReader<FlowedConnection> reader( Function<DatapathId, String> idAliaser )
        {
            return FlowedIO.reader(DatapathConnection.IO.reader(idAliaser), FlowedConnection::of);
        }

        private IO()
        {
            // not used
        }
    }
}
