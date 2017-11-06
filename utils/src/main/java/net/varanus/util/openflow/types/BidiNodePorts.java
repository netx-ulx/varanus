package net.varanus.util.openflow.types;


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

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.io.exception.IOChannelReadException;
import net.varanus.util.io.exception.IOChannelWriteException;
import net.varanus.util.io.exception.IOReadException;
import net.varanus.util.io.exception.IOWriteException;
import net.varanus.util.io.serializer.IOReader;
import net.varanus.util.io.serializer.IOWriter;
import net.varanus.util.lang.Comparables;


/**
 * 
 */
@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class BidiNodePorts implements Flowable<BidiNodePorts>, PrimitiveSinkable, Comparable<BidiNodePorts>
{
    public static BidiNodePorts of( NodePort nodePort1, NodePort nodePort2 )
    {
        return new BidiNodePorts(
            Comparables.min(nodePort1, nodePort2),
            Comparables.max(nodePort1, nodePort2));
    }

    private final NodePort min;
    private final NodePort max;

    private BidiNodePorts( NodePort min, NodePort max )
    {
        this.min = min;
        this.max = max;
    }

    public NodePort getMin()
    {
        return min;
    }

    public NodePort getMax()
    {
        return max;
    }

    @Override
    public FlowedBidiNodePorts flowed( Flow flow )
    {
        return FlowedBidiNodePorts.of(this, flow);
    }

    @Override
    public boolean equals( Object other )
    {
        return (other instanceof BidiNodePorts)
               && this.equals((BidiNodePorts)other);
    }

    public boolean equals( BidiNodePorts other )
    {
        return (other != null)
               && this.min.equals(other.min)
               && this.max.equals(other.max);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(min, max);
    }

    @Override
    public int compareTo( BidiNodePorts other )
    {
        return Comparator
            .comparing(BidiNodePorts::getMin)
            .thenComparing(BidiNodePorts::getMax)
            .compare(this, other);
    }

    @Override
    public void putTo( PrimitiveSink sink )
    {
        min.putTo(sink);
        max.putTo(sink);
    }

    @Override
    public String toString()
    {
        return String.format("%s - %s", min, max);
    }

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class IO
    {
        public static IOWriter<BidiNodePorts> writer()
        {
            return BidiLinkWriter.INSTANCE;
        }

        private static enum BidiLinkWriter implements IOWriter<BidiNodePorts>
        {
            INSTANCE;

            @Override
            public void write( BidiNodePorts link, WritableByteChannel ch ) throws IOChannelWriteException
            {
                NodePort.IO.writer().write(link.min, ch);
                NodePort.IO.writer().write(link.max, ch);
            }

            @Override
            public void write( BidiNodePorts link, OutputStream out ) throws IOWriteException
            {
                NodePort.IO.writer().write(link.min, out);
                NodePort.IO.writer().write(link.max, out);
            }

            @Override
            public void write( BidiNodePorts link, DataOutput out ) throws IOWriteException
            {
                NodePort.IO.writer().write(link.min, out);
                NodePort.IO.writer().write(link.max, out);
            }
        }

        public static IOReader<BidiNodePorts> reader()
        {
            return reader(NodeId.NIL_ID_ALIASER);
        }

        public static IOReader<BidiNodePorts> reader( Function<DatapathId, String> idAliaser )
        {
            Objects.requireNonNull(idAliaser);
            return new IOReader<BidiNodePorts>() {

                @Override
                public BidiNodePorts read( ReadableByteChannel ch ) throws IOChannelReadException
                {
                    NodePort min = NodePort.IO.reader(idAliaser).read(ch);
                    NodePort max = NodePort.IO.reader(idAliaser).read(ch);
                    return new BidiNodePorts(min, max);
                }

                @Override
                public BidiNodePorts read( InputStream in ) throws IOReadException
                {
                    NodePort min = NodePort.IO.reader(idAliaser).read(in);
                    NodePort max = NodePort.IO.reader(idAliaser).read(in);
                    return new BidiNodePorts(min, max);
                }

                @Override
                public BidiNodePorts read( DataInput in ) throws IOReadException
                {
                    NodePort min = NodePort.IO.reader(idAliaser).read(in);
                    NodePort max = NodePort.IO.reader(idAliaser).read(in);
                    return new BidiNodePorts(min, max);
                }
            };
        }

        private IO()
        {
            // not used
        }
    }
}
