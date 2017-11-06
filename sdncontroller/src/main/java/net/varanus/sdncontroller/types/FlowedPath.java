package net.varanus.sdncontroller.types;


import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import org.projectfloodlight.openflow.types.DatapathId;

import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.io.serializer.IOReader;
import net.varanus.util.io.serializer.IOWriter;
import net.varanus.util.openflow.types.AbstractFlowed;
import net.varanus.util.openflow.types.Flow;
import net.varanus.util.openflow.types.NodeId;


/**
 * 
 */
@Immutable
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class FlowedPath extends AbstractFlowed<DatapathPath>
{
    public static FlowedPath of( DatapathPath path, Flow flow )
    {
        return new FlowedPath(Objects.requireNonNull(path), Objects.requireNonNull(flow));
    }

    private FlowedPath( DatapathPath unflowed, Flow flow )
    {
        super(unflowed, flow);
    }

    public FlowedConnection getConnection()
    {
        return unflowed.getConnection().flowed(flow);
    }

    public boolean hasLinks()
    {
        return unflowed.hasLinks();
    }

    public int numberOfLinks()
    {
        return unflowed.numberOfLinks();
    }

    public Stream<FlowedLink> getLinks()
    {
        return unflowed.getLinks().stream()
            .map(link -> link.flowed(flow));
    }

    public int numberOfHops()
    {
        return unflowed.numberOfHops();
    }

    public Stream<FlowedHop> getHops()
    {
        return unflowed.getHops().stream()
            .map(hop -> hop.flowed(flow));
    }

    public FlowedPath reversed()
    {
        return new FlowedPath(unflowed.reversed(), flow);
    }

    @Override
    public boolean equals( Object other )
    {
        return (other instanceof FlowedPath)
               && super.equals(other);
    }

    public boolean equals( FlowedPath other )
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
        public static IOWriter<FlowedPath> writer()
        {
            return FlowedIO.writer(DatapathPath.IO.writer());
        }

        public static IOReader<FlowedPath> reader()
        {
            return reader(NodeId.NIL_ID_ALIASER);
        }

        public static IOReader<FlowedPath> reader( Function<DatapathId, String> idAliaser )
        {
            return FlowedIO.reader(DatapathPath.IO.reader(idAliaser), FlowedPath::of);
        }

        private IO()
        {
            // not used
        }
    }
}
