package net.varanus.util.openflow.types;


import java.util.Objects;
import java.util.function.Function;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import org.projectfloodlight.openflow.types.DatapathId;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.io.serializer.IOReader;
import net.varanus.util.io.serializer.IOWriter;


/**
 * 
 */
@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class FlowedBidiNodePorts extends AbstractFlowed<BidiNodePorts>
{
    public static FlowedBidiNodePorts of( BidiNodePorts bidiNodePorts, Flow flow )
    {
        return new FlowedBidiNodePorts(
            Objects.requireNonNull(bidiNodePorts),
            Objects.requireNonNull(flow));
    }

    public static FlowedBidiNodePorts of( NodePort nodePort1, NodePort nodePort2, Flow flow )
    {
        return of(BidiNodePorts.of(nodePort1, nodePort2), flow);
    }

    private FlowedBidiNodePorts( BidiNodePorts unflowed, Flow flow )
    {
        super(unflowed, flow);
    }

    public NodePort getMin()
    {
        return unflowed.getMin();
    }

    public NodePort getMax()
    {
        return unflowed.getMax();
    }

    @Override
    public boolean equals( Object other )
    {
        return (other instanceof FlowedBidiNodePorts)
               && super.equals(other);
    }

    public boolean equals( FlowedBidiNodePorts other )
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
        public static IOWriter<FlowedBidiNodePorts> writer()
        {
            return FlowedIO.writer(BidiNodePorts.IO.writer());
        }

        public static IOReader<FlowedBidiNodePorts> reader()
        {
            return reader(NodeId.NIL_ID_ALIASER);
        }

        public static IOReader<FlowedBidiNodePorts> reader( Function<DatapathId, String> idAliaser )
        {
            return FlowedIO.reader(BidiNodePorts.IO.reader(idAliaser), FlowedBidiNodePorts::of);
        }

        private IO()
        {
            // not used
        }
    }
}
