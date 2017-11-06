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
 * @param <UF>
 *            The flowable type
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class AbstractFlowed<UF extends Flowable<UF> & PrimitiveSinkable & Comparable<UF>>
    implements Flowed<UF>, PrimitiveSinkable, Comparable<Flowed<UF>>
{
    protected final UF   unflowed;
    protected final Flow flow;

    protected AbstractFlowed( UF unflowed, Flow flow )
    {
        this.unflowed = Objects.requireNonNull(unflowed);
        this.flow = Objects.requireNonNull(flow);
    }

    @Override
    public final Flow getFlow()
    {
        return flow;
    }

    @Override
    public final UF unflowed()
    {
        return unflowed;
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public boolean equals( Object other )
    {
        return (other instanceof AbstractFlowed)
               && this.equals((AbstractFlowed<?>)other);
    }

    protected final boolean equals( AbstractFlowed<?> other )
    {
        return (other != null)
               && this.unflowed.equals(other.unflowed)
               && this.flow.equals(other.flow);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public int hashCode()
    {
        return Objects.hash(unflowed, flow);
    }

    @Override
    public int compareTo( Flowed<UF> other )
    {
        return Comparator
            .comparing(Flowed<UF>::unflowed)
            .thenComparing(Flowed::getFlow)
            .compare(this, other);
    }

    @Override
    public void putTo( PrimitiveSink sink )
    {
        unflowed.putTo(sink);
        flow.putTo(sink);
    }

    @Override
    public String toString()
    {
        return String.format("%s | %s", unflowed, flow);
    }

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    protected static final class FlowedIO
    {
        //@formatter:off
        public static <UF extends Flowable<UF> & PrimitiveSinkable & Comparable<UF>,
                       F extends Flowed<UF> & PrimitiveSinkable & Comparable<Flowed<UF>>>
        IOWriter<F> writer( IOWriter<UF> unflowWriter )
        {//@formatter:on
            Objects.requireNonNull(unflowWriter);
            return new IOWriter<F>() {

                @Override
                public void write( F flowed, WritableByteChannel ch ) throws IOChannelWriteException
                {
                    unflowWriter.write(flowed.unflowed(), ch);
                    Flow.IO.writer().write(flowed.getFlow(), ch);
                }

                @Override
                public void write( F flowed, OutputStream out ) throws IOWriteException
                {
                    unflowWriter.write(flowed.unflowed(), out);
                    Flow.IO.writer().write(flowed.getFlow(), out);
                }

                @Override
                public void write( F flowed, DataOutput out ) throws IOWriteException
                {
                    unflowWriter.write(flowed.unflowed(), out);
                    Flow.IO.writer().write(flowed.getFlow(), out);
                }
            };
        }

        //@formatter:off
        public static <UF extends Flowable<UF> & PrimitiveSinkable & Comparable<UF>,
                       F extends Flowed<UF> & PrimitiveSinkable & Comparable<Flowed<UF>>>
        IOReader<F> reader( IOReader<UF> unflowReader, FlowedFactory<UF, F> flowedFactory )
        {//@formatter:on
            MoreObjects.requireNonNull(unflowReader, "unflowReader", flowedFactory, "flowedFactory");
            return new IOReader<F>() {

                @Override
                public F read( ReadableByteChannel ch ) throws IOChannelReadException
                {
                    UF unflowed = unflowReader.read(ch);
                    Flow flow = Flow.IO.reader().read(ch);
                    return flowedFactory.create(unflowed, flow);
                }

                @Override
                public F read( InputStream in ) throws IOReadException
                {
                    UF unflowed = unflowReader.read(in);
                    Flow flow = Flow.IO.reader().read(in);
                    return flowedFactory.create(unflowed, flow);
                }

                @Override
                public F read( DataInput in ) throws IOReadException
                {
                    UF unflowed = unflowReader.read(in);
                    Flow flow = Flow.IO.reader().read(in);
                    return flowedFactory.create(unflowed, flow);
                }
            };
        }

        @FunctionalInterface
        public static interface FlowedFactory<UF, F>
        {
            public F create( UF unflowed, Flow flow );
        }
    }
}
