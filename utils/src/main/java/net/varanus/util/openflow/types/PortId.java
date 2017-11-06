package net.varanus.util.openflow.types;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Objects;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.PrimitiveSinkable;

import com.google.common.hash.PrimitiveSink;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.io.Serializers;
import net.varanus.util.io.exception.IOChannelReadException;
import net.varanus.util.io.exception.IOChannelWriteException;
import net.varanus.util.io.exception.IOReadException;
import net.varanus.util.io.exception.IOWriteException;
import net.varanus.util.io.serializer.IOReader;
import net.varanus.util.io.serializer.IOSerializer;
import net.varanus.util.io.serializer.IOWriter;
import net.varanus.util.openflow.NodePortUtils;


/**
 * 
 */
@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class PortId implements Directable<PortId>, PrimitiveSinkable, Comparable<PortId>
{
    public static PortId of( OFPort ofPort ) throws IllegalArgumentException
    {
        Objects.requireNonNull(ofPort);
        if (NodePortUtils.isSpecialPort(ofPort))
            throw new IllegalArgumentException("special port not supported");

        return new PortId(ofPort);
    }

    public static PortId ofInt( int portNumber )
    {
        return of(OFPort.ofInt(portNumber));
    }

    public static PortId ofShort( short portNumber )
    {
        return of(OFPort.ofShort(portNumber));
    }

    public static PortId parse( String s )
    {
        return of(NodePortUtils.parseOFPort(s));
    }

    private final OFPort ofPort;

    private PortId( OFPort ofPort )
    {
        this.ofPort = ofPort;
    }

    @Override
    public DirectedPortId directed( TrafficDirection direction )
    {
        return DirectedPortId.of(this, direction);
    }

    public OFPort getOFPort()
    {
        return ofPort;
    }

    public int getPortNumber()
    {
        return ofPort.getPortNumber();
    }

    public short getShortPortNumber()
    {
        return ofPort.getShortPortNumber();
    }

    @Override
    public boolean equals( Object other )
    {
        return (other instanceof PortId)
               && this.equals((PortId)other);
    }

    public boolean equals( PortId other )
    {
        return (other != null)
               && this.ofPort.equals(other.ofPort);
    }

    @Override
    public int hashCode()
    {
        return ofPort.hashCode();
    }

    @Override
    public int compareTo( PortId other )
    {
        return this.ofPort.compareTo(other.ofPort);
    }

    @Override
    public void putTo( PrimitiveSink sink )
    {
        ofPort.putTo(sink);
    }

    @Override
    public String toString()
    {
        return ofPort.toString();
    }

    @ReturnValuesAreNonnullByDefault
    public static final class IO
    {
        public static IOWriter<PortId> writer()
        {
            return PortIdSerializer.INSTANCE;
        }

        public static IOReader<PortId> reader()
        {
            return PortIdSerializer.INSTANCE;
        }

        private static enum PortIdSerializer implements IOSerializer<PortId>
        {
            INSTANCE;

            @Override
            public void write( PortId portId, WritableByteChannel ch ) throws IOChannelWriteException
            {
                Serializers.intWriter(ByteOrder.BIG_ENDIAN).writeInt(portId.getPortNumber(), ch);
            }

            @Override
            public void write( PortId portId, OutputStream out ) throws IOWriteException
            {
                Serializers.intWriter(ByteOrder.BIG_ENDIAN).writeInt(portId.getPortNumber(), out);
            }

            @Override
            public void write( PortId portId, DataOutput out ) throws IOWriteException
            {
                Serializers.intWriter(ByteOrder.BIG_ENDIAN).writeInt(portId.getPortNumber(), out);
            }

            @Override
            public PortId read( ReadableByteChannel ch ) throws IOChannelReadException
            {
                return PortId.ofInt(Serializers.intReader(ByteOrder.BIG_ENDIAN).readInt(ch));
            }

            @Override
            public PortId read( InputStream in ) throws IOReadException
            {
                return PortId.ofInt(Serializers.intReader(ByteOrder.BIG_ENDIAN).readInt(in));
            }

            @Override
            public PortId read( DataInput in ) throws IOReadException
            {
                return PortId.ofInt(Serializers.intReader(ByteOrder.BIG_ENDIAN).readInt(in));
            }
        }

        private IO()
        {
            // not used
        }
    }
}
