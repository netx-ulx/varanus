package net.varanus.util.openflow.types;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Comparator;
import java.util.Objects;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import javax.annotation.ParametersAreNonnullByDefault;

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
import net.varanus.util.lang.MoreObjects;


/**
 * @param <UD>
 *            The directable type
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class AbstractDirected<UD extends Directable<UD> & PrimitiveSinkable & Comparable<UD>>
    implements Directed<UD>, PrimitiveSinkable, Comparable<Directed<UD>>
{

    protected final UD               undirected;
    protected final TrafficDirection direction;

    protected AbstractDirected( UD undirected, TrafficDirection direction )
    {
        this.undirected = Objects.requireNonNull(undirected);
        this.direction = Objects.requireNonNull(direction);
    }

    @Override
    public final TrafficDirection getDirection()
    {
        return direction;
    }

    @Override
    public final UD undirected()
    {
        return undirected;
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public boolean equals( Object other )
    {
        return (other instanceof AbstractDirected)
               && this.equals((AbstractDirected<?>)other);
    }

    protected final boolean equals( AbstractDirected<?> other )
    {
        return (other != null)
               && this.undirected.equals(other.undirected)
               && this.direction.equals(other.direction);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public int hashCode()
    {
        return Objects.hash(undirected, direction);
    }

    @Override
    public int compareTo( Directed<UD> other )
    {
        return Comparator
            .comparing(Directed<UD>::undirected)
            .thenComparing(Directed::getDirection)
            .compare(this, other);
    }

    @Override
    public void putTo( PrimitiveSink sink )
    {
        undirected.putTo(sink);
        direction.putTo(sink);
    }

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    protected static final class DirectedIO
    {
        //@formatter:off
        public static <UD extends Directable<UD> & PrimitiveSinkable & Comparable<UD>,
                       D extends Directed<UD> & PrimitiveSinkable & Comparable<Directed<UD>>>
        IOWriter<D> writer( IOWriter<UD> undirWriter )
        {//@formatter:on
            Objects.requireNonNull(undirWriter);
            return new IOWriter<D>() {

                @Override
                public void write( D directed, WritableByteChannel ch ) throws IOChannelWriteException
                {
                    undirWriter.write(directed.undirected(), ch);
                    TrafficDirection.IO.writer().write(directed.getDirection(), ch);
                }

                @Override
                public void write( D directed, OutputStream out ) throws IOWriteException
                {
                    undirWriter.write(directed.undirected(), out);
                    TrafficDirection.IO.writer().write(directed.getDirection(), out);
                }

                @Override
                public void write( D directed, DataOutput out ) throws IOWriteException
                {
                    undirWriter.write(directed.undirected(), out);
                    TrafficDirection.IO.writer().write(directed.getDirection(), out);
                }
            };
        }

        //@formatter:off
        public static <UD extends Directable<UD> & PrimitiveSinkable & Comparable<UD>,
                       D extends Directed<UD> & PrimitiveSinkable & Comparable<Directed<UD>>>
        IOReader<D> reader( IOReader<UD> undirReader, DirectedFactory<UD, D> dirFactory )
        {//@formatter:on
            MoreObjects.requireNonNull(undirReader, "undirReader", dirFactory, "dirFactory");
            return new IOReader<D>() {

                @Override
                public D read( ReadableByteChannel ch ) throws IOChannelReadException
                {
                    UD undirected = undirReader.read(ch);
                    TrafficDirection direction = TrafficDirection.IO.reader().read(ch);
                    return dirFactory.create(undirected, direction);
                }

                @Override
                public D read( InputStream in ) throws IOReadException
                {
                    UD undirected = undirReader.read(in);
                    TrafficDirection direction = TrafficDirection.IO.reader().read(in);
                    return dirFactory.create(undirected, direction);
                }

                @Override
                public D read( DataInput in ) throws IOReadException
                {
                    UD undirected = undirReader.read(in);
                    TrafficDirection direction = TrafficDirection.IO.reader().read(in);
                    return dirFactory.create(undirected, direction);
                }
            };
        }

        @FunctionalInterface
        public static interface DirectedFactory<UD, D>
        {
            public D create( UD undirected, TrafficDirection direction );
        }
    }
}
