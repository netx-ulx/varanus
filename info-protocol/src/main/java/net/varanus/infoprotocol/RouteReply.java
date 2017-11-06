package net.varanus.infoprotocol;


import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import org.projectfloodlight.openflow.types.DatapathId;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableList;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.functional.Report;
import net.varanus.util.io.Serializers;
import net.varanus.util.io.exception.IOChannelReadException;
import net.varanus.util.io.exception.IOChannelWriteException;
import net.varanus.util.io.serializer.IOReader;
import net.varanus.util.io.serializer.IOWriter;
import net.varanus.util.lang.MoreObjects;
import net.varanus.util.openflow.types.FlowedUnidiNodePorts;
import net.varanus.util.openflow.types.UnidiNodePorts;


/**
 * 
 */
@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class RouteReply extends Generic.Route
{
    public static RouteReply of( FlowedUnidiNodePorts connection, ImmutableList<UnidiNodePorts> links )
    {
        return new RouteReply(Report.of(new Result(
            Objects.requireNonNull(connection),
            Objects.requireNonNull(links))));
    }

    public static RouteReply ofError( String errorMsg )
    {
        return new RouteReply(Report.ofError(errorMsg));
    }

    private final Report<Result> result;

    private RouteReply( Report<Result> result )
    {
        this.result = result;
    }

    public boolean hasResult()
    {
        return result.hasValue();
    }

    public FlowedUnidiNodePorts getConnection() throws NoSuchElementException
    {
        return result.getValue().connection;
    }

    public ImmutableList<UnidiNodePorts> getLinks() throws NoSuchElementException
    {
        return result.getValue().links;
    }

    public boolean hasError()
    {
        return result.hasError();
    }

    public String getError() throws NoSuchElementException
    {
        return result.getError();
    }

    @Override
    public String toString()
    {
        return String.format("( %s )", result);
    }

    @Immutable
    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    private static final class Result
    {
        final FlowedUnidiNodePorts          connection;
        final ImmutableList<UnidiNodePorts> links;

        Result( FlowedUnidiNodePorts connection, ImmutableList<UnidiNodePorts> links )
        {
            this.connection = connection;
            this.links = links;
        }

        @Override
        public String toString()
        {
            return String.format("connection %s, links=%s", connection, links);
        }
    }

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class IO
    {
        private static final long REPLY_PREAMBLE = 0xfa9703b0bd2b44b3L;

        public static IOWriter<RouteReply> writer( Logger log )
        {
            Objects.requireNonNull(log);
            return new IOWriter<RouteReply>() {
                @Override
                public void write( RouteReply reply, WritableByteChannel ch ) throws IOChannelWriteException
                {
                    if (log.isTraceEnabled()) {
                        log.trace("Writing route reply from server: reply preamble 0x{}",
                            Long.toHexString(REPLY_PREAMBLE));
                    }
                    Serializers.longWriter(ByteOrder.BIG_ENDIAN).writeLong(REPLY_PREAMBLE, ch);

                    Report<Result> result = reply.result;
                    log.trace("Writing route reply from server: {}", result);
                    Report.IO.writer(resultWriter()).write(result, ch);
                }
            };
        }

        public static IOReader<RouteReply> reader( Function<DatapathId, String> idAliaser, Logger log )
        {
            MoreObjects.requireNonNull(idAliaser, "idAliaser", log, "log");
            return new IOReader<RouteReply>() {
                @Override
                public RouteReply read( ReadableByteChannel ch ) throws IOChannelReadException
                {
                    final long preamble = Serializers.longReader(ByteOrder.BIG_ENDIAN).readLong(ch);
                    if (preamble != REPLY_PREAMBLE) {
                        throw new IOChannelReadException(
                            String.format("expected reply preamble of %x from server but found %x instead",
                                REPLY_PREAMBLE, preamble));
                    }
                    if (log.isTraceEnabled()) {
                        log.trace("Read route reply from server: reply preamble 0x{}",
                            Long.toHexString(preamble));
                    }

                    Report<Result> result = Report.IO.reader(resultReader(idAliaser)).read(ch);
                    log.trace("Read route reply from server: {}", result);

                    return new RouteReply(result);
                }
            };
        }

        private static IOWriter<Result> resultWriter()
        {
            return new IOWriter<Result>() {

                @Override
                public void write( Result res, WritableByteChannel ch ) throws IOChannelWriteException
                {
                    FlowedUnidiNodePorts.IO.writer().write(res.connection, ch);
                    Serializers.colWriter(UnidiNodePorts.IO.writer()).write(res.links, ch);
                }
            };
        }

        private static IOReader<Result> resultReader( Function<DatapathId, String> idAliaser )
        {
            return new IOReader<Result>() {

                @Override
                public Result read( ReadableByteChannel ch ) throws IOChannelReadException
                {
                    final FlowedUnidiNodePorts connection = FlowedUnidiNodePorts.IO.reader(idAliaser).read(ch);
                    final ImmutableList<UnidiNodePorts> links =
                        Serializers.immuListReader(UnidiNodePorts.IO.reader(idAliaser)).read(ch);

                    return new Result(connection, links);
                }
            };
        }

        private IO()
        {
            // not used
        }
    }
}
