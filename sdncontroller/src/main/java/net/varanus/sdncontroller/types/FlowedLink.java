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
 * Like a {@link DatapathLink} object and with an associated flow identified
 * by a {@link Flow} object.
 */
@Immutable
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class FlowedLink extends AbstractFlowed<DatapathLink>
{
    public static FlowedLink of( DatapathLink link, Flow flow )
    {
        return new FlowedLink(Objects.requireNonNull(link), Objects.requireNonNull(flow));
    }

    public static FlowedLink of( NodeId srcNodeId, PortId srcPortId, NodeId destNodeId, PortId destPortId, Flow flow )
    {
        return of(DatapathLink.of(srcNodeId, srcPortId, destNodeId, destPortId), flow);
    }

    public static FlowedLink of( NodePort srcNodePort, NodePort destNodePort, Flow flow )
    {
        return of(DatapathLink.of(srcNodePort, destNodePort), flow);
    }

    public static FlowedLink of( UnidiNodePorts unidiNodePorts, Flow flow )
    {
        return of(unidiNodePorts.getSource(), unidiNodePorts.getTarget(), flow);
    }

    public static FlowedLink of( FlowedUnidiNodePorts unidiNodePorts )
    {
        return of(unidiNodePorts.getSource(), unidiNodePorts.getTarget(), unidiNodePorts.getFlow());
    }

    public static FlowedLink betweenHops( FlowedHop fromHop, FlowedHop toHop )
    {
        if (!fromHop.getFlow().equals(toHop.getFlow()))
            throw new IllegalArgumentException("both flowed-hops must contain the same flow");

        return of(DatapathLink.betweenHops(fromHop.unflowed(), toHop.unflowed()), fromHop.getFlow());
    }

    public static FlowedLink parse( String s, Function<DatapathId, String> idAliaser )
    {
        return LinkUtils.parseFlowedLink(s, idAliaser);
    }

    public static FlowedLink parse( String s )
    {
        return parse(s, NodeId.NIL_ID_ALIASER);
    }

    private FlowedLink( DatapathLink unflowed, Flow flow )
    {
        super(unflowed, flow);
    }

    public NodeId getSrcNode()
    {
        return unflowed.getSrcNode();
    }

    public PortId getSrcPort()
    {
        return unflowed.getSrcPort();
    }

    public FlowDirectedNodePort getSrcEndpoint()
    {
        return unflowed.getSrcEndpoint().flowed(flow);
    }

    public NodeId getDestNode()
    {
        return unflowed.getDestNode();
    }

    public PortId getDestPort()
    {
        return unflowed.getDestPort();
    }

    public FlowDirectedNodePort getDestEndpoint()
    {
        return unflowed.getDestEndpoint().flowed(flow);
    }

    public NodeId getNode( EndpointKind kind )
    {
        return unflowed.getNode(kind);
    }

    public PortId getPort( EndpointKind kind )
    {
        return unflowed.getPort(kind);
    }

    public FlowDirectedNodePort getEndpoint( EndpointKind kind )
    {
        return unflowed.getEndpoint(kind).flowed(flow);
    }

    public boolean hasSrcNode( NodeId nodeId )
    {
        return unflowed.hasSrcNode(nodeId);
    }

    public boolean hasSrcPort( PortId portId )
    {
        return unflowed.hasSrcPort(portId);
    }

    public boolean hasDestNode( NodeId nodeId )
    {
        return unflowed.hasDestNode(nodeId);
    }

    public boolean hasDestPort( PortId portId )
    {
        return unflowed.hasDestPort(portId);
    }

    public boolean hasNode( NodeId nodeId )
    {
        return unflowed.hasNode(nodeId);
    }

    public boolean hasPort( PortId portId )
    {
        return unflowed.hasPort(portId);
    }

    public FlowedLink reversed()
    {
        return new FlowedLink(unflowed.reversed(), flow);
    }

    public boolean coincidesWith( FlowedLink other )
    {
        return this.flow.equals(other.flow)
               && this.unflowed.coincidesWith(other.unflowed);
    }

    public boolean succeeds( FlowedLink other )
    {
        return this.flow.equals(other.flow)
               && this.unflowed.succeeds(other.unflowed);
    }

    public boolean precedes( FlowedLink other )
    {
        return this.flow.equals(other.flow)
               && this.unflowed.precedes(other.unflowed);
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
        return (other instanceof FlowedLink)
               && super.equals(other);
    }

    public boolean equals( FlowedLink other )
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
        public static IOWriter<FlowedLink> writer()
        {
            return FlowedIO.writer(DatapathLink.IO.writer());
        }

        public static IOReader<FlowedLink> reader()
        {
            return reader(NodeId.NIL_ID_ALIASER);
        }

        public static IOReader<FlowedLink> reader( Function<DatapathId, String> idAliaser )
        {
            return FlowedIO.reader(DatapathLink.IO.reader(idAliaser), FlowedLink::of);
        }

        private IO()
        {
            // not used
        }
    }
}
