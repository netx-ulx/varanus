package net.varanus.util.openflow.types;


import java.util.Objects;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import org.projectfloodlight.openflow.types.OFPort;

import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.io.serializer.IOReader;
import net.varanus.util.io.serializer.IOWriter;
import net.varanus.util.openflow.NodePortUtils;


/**
 * 
 */
@Immutable
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class DirectedPortId extends AbstractDirected<PortId>
{
    public static DirectedPortId of( PortId portId, TrafficDirection direction )
    {
        return new DirectedPortId(
            Objects.requireNonNull(portId),
            Objects.requireNonNull(direction));
    }

    public static DirectedPortId of( OFPort ofPort, TrafficDirection direction )
    {
        return of(PortId.of(ofPort), direction);
    }

    public static DirectedPortId ofInt( int portNumber, TrafficDirection direction )
    {
        return of(PortId.ofInt(portNumber), direction);
    }

    public static DirectedPortId ofShort( short portNumber, TrafficDirection direction )
    {
        return of(PortId.ofShort(portNumber), direction);
    }

    public static DirectedPortId parse( String s )
    {
        return NodePortUtils.parseDirectedPortId(s);
    }

    private DirectedPortId( PortId undirected, TrafficDirection direction )
    {
        super(undirected, direction);
    }

    public OFPort getOFPort()
    {
        return undirected.getOFPort();
    }

    public int getPortNumber()
    {
        return undirected.getPortNumber();
    }

    public short getShortPortNumber()
    {
        return undirected.getShortPortNumber();
    }

    @Override
    public boolean equals( Object other )
    {
        return (other instanceof DirectedPortId)
               && super.equals(other);
    }

    public boolean equals( DirectedPortId other )
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
        return String.format("%s_%s", direction, undirected);
    }

    @ReturnValuesAreNonnullByDefault
    public static final class IO
    {
        public static IOWriter<DirectedPortId> writer()
        {
            return DirectedIO.writer(PortId.IO.writer());
        }

        public static IOReader<DirectedPortId> reader()
        {
            return DirectedIO.reader(PortId.IO.reader(), DirectedPortId::of);
        }

        private IO()
        {
            // not used
        }
    }
}
