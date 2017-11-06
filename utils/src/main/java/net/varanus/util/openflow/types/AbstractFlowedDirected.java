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
 * @param <UFD>
 *            The flowable directable type
 * @param <UF>
 *            The flowable type
 * @param <UD>
 *            The directable type
 */
//@formatter:off
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class AbstractFlowedDirected<UFD extends Flowable<UFD> & Directable<UFD> & PrimitiveSinkable & Comparable<UFD>,
                                             UF extends Flowable<UF> & Directed<UFD>,
                                             UD extends Directable<UD> & Flowed<UFD>>
    implements FlowedDirected<UFD, UF, UD>, PrimitiveSinkable, Comparable<FlowedDirected<UFD, UF, UD>>
{//@formatter:on
    protected final UFD              unflowDirected;
    protected final Flow             flow;
    protected final TrafficDirection direction;

    protected AbstractFlowedDirected( UFD unflowDirected, Flow flow, TrafficDirection direction )
    {
        this.unflowDirected = Objects.requireNonNull(unflowDirected);
        this.flow = Objects.requireNonNull(flow);
        this.direction = Objects.requireNonNull(direction);
    }

    @Override
    public final Flow getFlow()
    {
        return flow;
    }

    @Override
    public final TrafficDirection getDirection()
    {
        return direction;
    }

    @Override
    public final UFD unflowDirected()
    {
        return unflowDirected;
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public final UF unflowed()
    {
        return (UF)unflowDirected.directed(direction);
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public final UD undirected()
    {
        return (UD)unflowDirected.flowed(flow);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public boolean equals( Object other )
    {
        return (other instanceof AbstractFlowedDirected)
               && this.equals((AbstractFlowedDirected<?, ?, ?>)other);
    }

    protected boolean equals( AbstractFlowedDirected<?, ?, ?> other )
    {
        return (other != null)
               && this.unflowDirected.equals(other.unflowDirected)
               && this.flow.equals(other.flow)
               && this.direction.equals(other.direction);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public int hashCode()
    {
        return Objects.hash(unflowDirected, flow, direction);
    }

    @Override
    public int compareTo( FlowedDirected<UFD, UF, UD> other )
    {
        return Comparator
            .comparing(FlowedDirected<UFD, ?, ?>::unflowDirected)
            .thenComparing(FlowedDirected::getFlow)
            .thenComparing(FlowedDirected::getDirection)
            .compare(this, other);
    }

    @Override
    public void putTo( PrimitiveSink sink )
    {
        unflowDirected.putTo(sink);
        flow.putTo(sink);
        direction.putTo(sink);
    }

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    protected static final class FlowedDirectedIO
    {
        //@formatter:off
        public static <UFD extends Flowable<UFD> & Directable<UFD> & PrimitiveSinkable & Comparable<UFD>,
                       UF extends Flowable<UF> & Directed<UFD>,
                       UD extends Directable<UD> & Flowed<UFD>,
                       FD extends FlowedDirected<UFD, UF, UD> & PrimitiveSinkable & Comparable<FlowedDirected<UFD, UF, UD>>>
        IOWriter<FD> writer( IOWriter<UFD> unflowDirWriter )
        {//@formatter:on
            Objects.requireNonNull(unflowDirWriter);
            return new IOWriter<FD>() {

                @Override
                public void write( FD flowDirected, WritableByteChannel ch ) throws IOChannelWriteException
                {
                    unflowDirWriter.write(flowDirected.unflowDirected(), ch);
                    Flow.IO.writer().write(flowDirected.getFlow(), ch);
                    TrafficDirection.IO.writer().write(flowDirected.getDirection(), ch);
                }

                @Override
                public void write( FD flowDirected, OutputStream out ) throws IOWriteException
                {
                    unflowDirWriter.write(flowDirected.unflowDirected(), out);
                    Flow.IO.writer().write(flowDirected.getFlow(), out);
                    TrafficDirection.IO.writer().write(flowDirected.getDirection(), out);
                }

                @Override
                public void write( FD flowDirected, DataOutput out ) throws IOWriteException
                {
                    unflowDirWriter.write(flowDirected.unflowDirected(), out);
                    Flow.IO.writer().write(flowDirected.getFlow(), out);
                    TrafficDirection.IO.writer().write(flowDirected.getDirection(), out);
                }
            };
        }

        //@formatter:off
        public static <UFD extends Flowable<UFD> & Directable<UFD> & PrimitiveSinkable & Comparable<UFD>,
                       UF extends Flowable<UF> & Directed<UFD>,
                       UD extends Directable<UD> & Flowed<UFD>,
                       FD extends FlowedDirected<UFD, UF, UD> & PrimitiveSinkable & Comparable<FlowedDirected<UFD, UF, UD>>>
        IOReader<FD> reader( IOReader<UFD> unflowDirReader, FlowedDirectedFactory<UFD, FD> flowDirFactory )
        {//@formatter:on
            MoreObjects.requireNonNull(unflowDirReader, "unflowDirReader", flowDirFactory, "flowDirFactory");
            return new IOReader<FD>() {

                @Override
                public FD read( ReadableByteChannel ch ) throws IOChannelReadException
                {
                    UFD unflowDirected = unflowDirReader.read(ch);
                    Flow flow = Flow.IO.reader().read(ch);
                    TrafficDirection direction = TrafficDirection.IO.reader().read(ch);
                    return flowDirFactory.create(unflowDirected, flow, direction);
                }

                @Override
                public FD read( InputStream in ) throws IOReadException
                {
                    UFD unflowDirected = unflowDirReader.read(in);
                    Flow flow = Flow.IO.reader().read(in);
                    TrafficDirection direction = TrafficDirection.IO.reader().read(in);
                    return flowDirFactory.create(unflowDirected, flow, direction);
                }

                @Override
                public FD read( DataInput in ) throws IOReadException
                {
                    UFD unflowDirected = unflowDirReader.read(in);
                    Flow flow = Flow.IO.reader().read(in);
                    TrafficDirection direction = TrafficDirection.IO.reader().read(in);
                    return flowDirFactory.create(unflowDirected, flow, direction);
                }
            };
        }

        @FunctionalInterface
        public static interface FlowedDirectedFactory<UFD, FD>
        {
            public FD create( UFD unflowDirected, Flow flow, TrafficDirection direction );
        }
    }
}
