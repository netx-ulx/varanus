package net.varanus.infoprotocol;


import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Objects;
import java.util.function.Function;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import org.projectfloodlight.openflow.types.DatapathId;
import org.slf4j.Logger;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.io.Serializers;
import net.varanus.util.io.exception.IOChannelReadException;
import net.varanus.util.io.exception.IOChannelWriteException;
import net.varanus.util.io.serializer.IOReader;
import net.varanus.util.io.serializer.IOWriter;
import net.varanus.util.lang.MoreObjects;
import net.varanus.util.openflow.types.FlowedUnidiNodePorts;


/**
 * 
 */
@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class RouteRequest extends Generic.Route
{
    private final FlowedUnidiNodePorts connection;

    public RouteRequest( FlowedUnidiNodePorts connection )
    {
        this.connection = Objects.requireNonNull(connection);
    }

    public FlowedUnidiNodePorts getConnection()
    {
        return connection;
    }

    @Override
    public String toString()
    {
        return String.format("( connection %s )", connection);
    }

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class IO
    {
        private static final long REQUEST_PREAMBLE = 0xdca0aa31898ef9a4L;

        public static IOWriter<RouteRequest> writer( Logger log )
        {
            Objects.requireNonNull(log);
            return new IOWriter<RouteRequest>() {
                @Override
                public void write( RouteRequest request, WritableByteChannel ch ) throws IOChannelWriteException
                {
                    final FlowedUnidiNodePorts connection = request.getConnection();

                    if (log.isTraceEnabled()) {
                        log.trace("Writing route request to server: request preamble 0x{}",
                            Long.toHexString(REQUEST_PREAMBLE));
                    }
                    Serializers.longWriter(ByteOrder.BIG_ENDIAN).writeLong(REQUEST_PREAMBLE, ch);

                    log.trace("Writing route request to server: connection {}", connection);
                    FlowedUnidiNodePorts.IO.writer().write(connection, ch);
                }
            };
        }

        public static IOReader<RouteRequest> reader( Function<DatapathId, String> idAliaser, Logger log )
        {
            MoreObjects.requireNonNull(idAliaser, "idAliaser", log, "log");
            return new IOReader<RouteRequest>() {
                @Override
                public RouteRequest read( ReadableByteChannel ch ) throws IOChannelReadException
                {
                    final long preamble = Serializers.longReader(ByteOrder.BIG_ENDIAN).readLong(ch);
                    if (preamble != REQUEST_PREAMBLE) {
                        throw new IOChannelReadException(
                            String.format("expected request preamble of %x to server but found %x instead",
                                REQUEST_PREAMBLE, preamble));
                    }
                    if (log.isTraceEnabled()) {
                        log.trace("Read route request to server: request preamble 0x{}",
                            Long.toHexString(preamble));
                    }

                    final FlowedUnidiNodePorts connection = FlowedUnidiNodePorts.IO.reader(idAliaser).read(ch);
                    log.trace("Read route request to server: connection {}", connection);

                    return new RouteRequest(connection);
                }
            };
        }

        private IO()
        {
            // not used
        }
    }
}
