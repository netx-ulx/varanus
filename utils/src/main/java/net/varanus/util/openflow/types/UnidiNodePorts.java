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


/**
 * 
 */
@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class UnidiNodePorts implements Flowable<UnidiNodePorts>, PrimitiveSinkable, Comparable<UnidiNodePorts>
{
    public static UnidiNodePorts of( NodePort source, NodePort target )
    {
        return new UnidiNodePorts(
            Objects.requireNonNull(source),
            Objects.requireNonNull(target));
    }

    private final NodePort source;
    private final NodePort target;

    private UnidiNodePorts( NodePort source, NodePort target )
    {
        this.source = source;
        this.target = target;
    }

    public NodePort getSource()
    {
        return source;
    }

    public NodePort getTarget()
    {
        return target;
    }

    @Override
    public FlowedUnidiNodePorts flowed( Flow flow )
    {
        return FlowedUnidiNodePorts.of(this, flow);
    }

    @Override
    public boolean equals( Object other )
    {
        return (other instanceof UnidiNodePorts)
               && this.equals((UnidiNodePorts)other);
    }

    public boolean equals( UnidiNodePorts other )
    {
        return (other != null)
               && this.source.equals(other.source)
               && this.target.equals(other.target);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(source, target);
    }

    @Override
    public int compareTo( UnidiNodePorts other )
    {
        return Comparator
            .comparing(UnidiNodePorts::getSource)
            .thenComparing(UnidiNodePorts::getTarget)
            .compare(this, other);
    }

    @Override
    public void putTo( PrimitiveSink sink )
    {
        source.putTo(sink);
        target.putTo(sink);
    }

    @Override
    public String toString()
    {
        return String.format("%s > %s", source, target);
    }

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class IO
    {
        public static IOWriter<UnidiNodePorts> writer()
        {
            return UnidiLinkWriter.INSTANCE;
        }

        private static enum UnidiLinkWriter implements IOWriter<UnidiNodePorts>
        {
            INSTANCE;

            @Override
            public void write( UnidiNodePorts link, WritableByteChannel ch ) throws IOChannelWriteException
            {
                NodePort.IO.writer().write(link.source, ch);
                NodePort.IO.writer().write(link.target, ch);
            }

            @Override
            public void write( UnidiNodePorts link, OutputStream out ) throws IOWriteException
            {
                NodePort.IO.writer().write(link.source, out);
                NodePort.IO.writer().write(link.target, out);
            }

            @Override
            public void write( UnidiNodePorts link, DataOutput out ) throws IOWriteException
            {
                NodePort.IO.writer().write(link.source, out);
                NodePort.IO.writer().write(link.target, out);
            }
        }

        public static IOReader<UnidiNodePorts> reader()
        {
            return reader(NodeId.NIL_ID_ALIASER);
        }

        public static IOReader<UnidiNodePorts> reader( Function<DatapathId, String> idAliaser )
        {
            Objects.requireNonNull(idAliaser);
            return new IOReader<UnidiNodePorts>() {

                @Override
                public UnidiNodePorts read( ReadableByteChannel ch ) throws IOChannelReadException
                {
                    NodePort source = NodePort.IO.reader(idAliaser).read(ch);
                    NodePort target = NodePort.IO.reader(idAliaser).read(ch);
                    return new UnidiNodePorts(source, target);
                }

                @Override
                public UnidiNodePorts read( InputStream in ) throws IOReadException
                {
                    NodePort source = NodePort.IO.reader(idAliaser).read(in);
                    NodePort target = NodePort.IO.reader(idAliaser).read(in);
                    return new UnidiNodePorts(source, target);
                }

                @Override
                public UnidiNodePorts read( DataInput in ) throws IOReadException
                {
                    NodePort source = NodePort.IO.reader(idAliaser).read(in);
                    NodePort target = NodePort.IO.reader(idAliaser).read(in);
                    return new UnidiNodePorts(source, target);
                }
            };
        }

        private IO()
        {
            // not used
        }
    }
}
