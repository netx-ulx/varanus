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
public final class StatisticsRequest extends Generic.Stat
{
    private final FlowedUnidiNodePorts link;
    private final boolean              errorOnUnknownLink;

    public StatisticsRequest( FlowedUnidiNodePorts link, boolean errorOnUnknownLink )
    {
        this.link = Objects.requireNonNull(link);
        this.errorOnUnknownLink = errorOnUnknownLink;
    }

    public FlowedUnidiNodePorts getLink()
    {
        return link;
    }

    public boolean doErrorOnUnknownLink()
    {
        return errorOnUnknownLink;
    }

    @Override
    public String toString()
    {
        return String.format("( link %s, error_on_unknown_link=%s )", link, errorOnUnknownLink);
    }

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class IO
    {
        private static final long REQUEST_PREAMBLE = 0x5ed44a96a3476b73L;

        public static IOWriter<StatisticsRequest> writer( Logger log )
        {
            Objects.requireNonNull(log);
            return new IOWriter<StatisticsRequest>() {
                @Override
                public void write( StatisticsRequest request, WritableByteChannel ch ) throws IOChannelWriteException
                {
                    FlowedUnidiNodePorts link = request.getLink();
                    boolean errorOnUnknownLink = request.doErrorOnUnknownLink();

                    if (log.isTraceEnabled()) {
                        log.trace("Writing statistics request to server: request preamble 0x{}",
                            Long.toHexString(REQUEST_PREAMBLE));
                    }
                    Serializers.longWriter(ByteOrder.BIG_ENDIAN).writeLong(REQUEST_PREAMBLE, ch);

                    log.trace("Writing statistics request to server: link {}", link);
                    FlowedUnidiNodePorts.IO.writer().write(link, ch);
                    log.trace("Writing statistics request to server: error on unknown link? {}", errorOnUnknownLink);
                    Serializers.boolWriter().write(errorOnUnknownLink, ch);
                }
            };
        }

        public static IOReader<StatisticsRequest> reader( Function<DatapathId, String> idAliaser, Logger log )
        {
            MoreObjects.requireNonNull(idAliaser, "idAliaser", log, "log");
            return new IOReader<StatisticsRequest>() {
                @Override
                public StatisticsRequest read( ReadableByteChannel ch ) throws IOChannelReadException
                {
                    final long preamble = Serializers.longReader(ByteOrder.BIG_ENDIAN).readLong(ch);
                    if (preamble != REQUEST_PREAMBLE) {
                        throw new IOChannelReadException(
                            String.format("expected request preamble of %x to server but found %x instead",
                                REQUEST_PREAMBLE, preamble));
                    }
                    if (log.isTraceEnabled()) {
                        log.trace("Read statistics request to server: request preamble 0x{}",
                            Long.toHexString(preamble));
                    }

                    FlowedUnidiNodePorts link = FlowedUnidiNodePorts.IO.reader(idAliaser).read(ch);
                    log.trace("Read statistics request to server: link {}", link);
                    boolean errorOnUnknownLink = Serializers.boolReader().read(ch);
                    log.trace("Read statistics request to server: error on unknown link? {}", errorOnUnknownLink);

                    return new StatisticsRequest(link, errorOnUnknownLink);
                }
            };
        }

        private IO()
        {
            // not used
        }
    }
}
