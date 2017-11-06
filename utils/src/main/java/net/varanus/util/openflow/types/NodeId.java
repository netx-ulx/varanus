package net.varanus.util.openflow.types;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import javax.annotation.Nullable;
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
import net.varanus.util.openflow.OFSerializers;


/**
 * 
 */
@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class NodeId implements PrimitiveSinkable, Comparable<NodeId>
{
    public static final Function<DatapathId, String> NIL_ID_ALIASER = dpid -> null;

    public static NodeId of( DatapathId dpid, @Nullable String alias )
    {
        return new NodeId(
            Objects.requireNonNull(dpid),
            Optional.ofNullable(alias));
    }

    public static NodeId of( DatapathId dpid, Function<DatapathId, String> idAliaser )
    {
        return of(dpid, idAliaser.apply(Objects.requireNonNull(dpid)));
    }

    public static NodeId of( DatapathId dpid )
    {
        return of(dpid, NIL_ID_ALIASER);
    }

    public static NodeId ofLong( long raw, Function<DatapathId, String> idAliaser )
    {
        return of(DatapathId.of(raw), idAliaser);
    }

    public static NodeId ofLong( long raw )
    {
        return ofLong(raw, NIL_ID_ALIASER);
    }

    public static NodeId parse( String s, Function<DatapathId, String> idAliaser ) throws IllegalArgumentException
    {
        DatapathId dpid = NodePortUtils.parseDatapathId(s);
        return NodeId.of(dpid, idAliaser);
    }

    public static NodeId parse( String s ) throws IllegalArgumentException
    {
        return parse(s, NIL_ID_ALIASER);
    }

    private final DatapathId       dpid;
    private final Optional<String> alias;

    private NodeId( DatapathId dpid, Optional<String> alias )
    {
        this.dpid = dpid;
        this.alias = alias;
    }

    public DatapathId getDpid()
    {
        return dpid;
    }

    public long getLong()
    {
        return dpid.getLong();
    }

    public Optional<String> getAlias()
    {
        return alias;
    }

    @Override
    public boolean equals( Object other )
    {
        return (other instanceof NodeId)
               && this.equals((NodeId)other);
    }

    public boolean equals( NodeId other )
    {
        return (other != null)
               && this.dpid.equals(other.dpid);
    }

    @Override
    public int hashCode()
    {
        return dpid.hashCode();
    }

    @Override
    public int compareTo( NodeId other )
    {
        return this.dpid.compareTo(other.dpid);
    }

    @Override
    public void putTo( PrimitiveSink sink )
    {
        dpid.putTo(sink);
    }

    @Override
    public String toString()
    {
        return alias.orElseGet(dpid::toString);
    }

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class IO
    {
        public static IOWriter<NodeId> writer()
        {
            return NodeIdWriter.INSTANCE;
        }

        private static enum NodeIdWriter implements IOWriter<NodeId>
        {
            INSTANCE;

            @Override
            public void write( NodeId nodeId, WritableByteChannel ch ) throws IOChannelWriteException
            {
                OFSerializers.dpathIdSerializer().write(nodeId.getDpid(), ch);
            }

            @Override
            public void write( NodeId nodeId, OutputStream out ) throws IOWriteException
            {
                OFSerializers.dpathIdSerializer().write(nodeId.getDpid(), out);
            }

            @Override
            public void write( NodeId nodeId, DataOutput out ) throws IOWriteException
            {
                OFSerializers.dpathIdSerializer().write(nodeId.getDpid(), out);
            }
        }

        public static IOReader<NodeId> reader()
        {
            return reader(NIL_ID_ALIASER);
        }

        public static IOReader<NodeId> reader( Function<DatapathId, String> idAliaser )
        {
            Objects.requireNonNull(idAliaser);
            return new IOReader<NodeId>() {

                @Override
                public NodeId read( ReadableByteChannel ch ) throws IOChannelReadException
                {
                    DatapathId dpid = OFSerializers.dpathIdSerializer().read(ch);
                    return of(dpid, idAliaser);
                }

                @Override
                public NodeId read( InputStream in ) throws IOReadException
                {
                    DatapathId dpid = OFSerializers.dpathIdSerializer().read(in);
                    return of(dpid, idAliaser);
                }

                @Override
                public NodeId read( DataInput in ) throws IOReadException
                {
                    DatapathId dpid = OFSerializers.dpathIdSerializer().read(in);
                    return of(dpid, idAliaser);
                }
            };
        }

        private IO()
        {
            // not used
        }
    }
}
