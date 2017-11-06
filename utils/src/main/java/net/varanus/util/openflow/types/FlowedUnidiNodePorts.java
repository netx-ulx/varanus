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
public final class FlowedUnidiNodePorts extends AbstractFlowed<UnidiNodePorts>
{
    public static FlowedUnidiNodePorts of( UnidiNodePorts unidiNodePorts, Flow flow )
    {
        return new FlowedUnidiNodePorts(
            Objects.requireNonNull(unidiNodePorts),
            Objects.requireNonNull(flow));
    }

    public static FlowedUnidiNodePorts of( NodePort source, NodePort target, Flow flow )
    {
        return of(UnidiNodePorts.of(source, target), flow);
    }

    private FlowedUnidiNodePorts( UnidiNodePorts unflowed, Flow flow )
    {
        super(unflowed, flow);
    }

    public NodePort getSource()
    {
        return unflowed.getSource();
    }

    public NodePort getTarget()
    {
        return unflowed.getTarget();
    }

    @Override
    public boolean equals( Object other )
    {
        return (other instanceof FlowedUnidiNodePorts)
               && super.equals(other);
    }

    public boolean equals( FlowedUnidiNodePorts other )
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
        public static IOWriter<FlowedUnidiNodePorts> writer()
        {
            return FlowedIO.writer(UnidiNodePorts.IO.writer());
        }

        public static IOReader<FlowedUnidiNodePorts> reader()
        {
            return reader(NodeId.NIL_ID_ALIASER);
        }

        public static IOReader<FlowedUnidiNodePorts> reader( Function<DatapathId, String> idAliaser )
        {
            return FlowedIO.reader(UnidiNodePorts.IO.reader(idAliaser), FlowedUnidiNodePorts::of);
        }

        private IO()
        {
            // not used
        }
    }
}
