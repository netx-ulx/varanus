package net.varanus.configprotocol;


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
import net.varanus.util.functional.PossibleDouble;
import net.varanus.util.io.Serializers;
import net.varanus.util.io.exception.IOChannelReadException;
import net.varanus.util.io.exception.IOChannelWriteException;
import net.varanus.util.io.serializer.IOReader;
import net.varanus.util.io.serializer.IOWriter;
import net.varanus.util.lang.MoreObjects;
import net.varanus.util.openflow.types.UnidiNodePorts;
import net.varanus.util.unitvalue.si.InfoDouble;


/**
 * 
 */
@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class LinkBandwidthRequest extends Generic.LinkBandwidth
{
    private final UnidiNodePorts link;
    private final InfoDouble     bandwidth;

    public LinkBandwidthRequest( UnidiNodePorts link, InfoDouble bandwidth )
    {
        this.link = Objects.requireNonNull(link);
        this.bandwidth = Objects.requireNonNull(bandwidth);
    }

    public UnidiNodePorts getLink()
    {
        return link;
    }

    public InfoDouble getBandwidth()
    {
        return bandwidth;
    }

    @Override
    public String toString()
    {
        return String.format("( link %s; bandwidth %s )", link, bandwidth);
    }

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class IO
    {
        private static final long REQUEST_PREAMBLE = 0x81ed4f50d537fb48L;

        public static IOWriter<LinkBandwidthRequest> writer( Logger log )
        {
            Objects.requireNonNull(log);
            return new IOWriter<LinkBandwidthRequest>() {
                @Override
                public void write( LinkBandwidthRequest request, WritableByteChannel ch ) throws IOChannelWriteException
                {
                    final UnidiNodePorts link = request.link;
                    final InfoDouble bandwidth = request.bandwidth;

                    if (log.isTraceEnabled()) {
                        log.trace("Writing link-bandwidth request to server: request preamble 0x{}",
                            Long.toHexString(REQUEST_PREAMBLE));
                    }
                    Serializers.longWriter(ByteOrder.BIG_ENDIAN).writeLong(REQUEST_PREAMBLE, ch);

                    log.trace("Writing link-bandwidth request to server: link {}", link);
                    UnidiNodePorts.IO.writer().write(link, ch);

                    log.trace("Writing link-bandwidth request to server: bandwidth {}", bandwidth);
                    bandwidthWriter().write(bandwidth, ch);
                }
            };
        }

        public static IOReader<LinkBandwidthRequest> reader( Function<DatapathId, String> idAliaser, Logger log )
        {
            MoreObjects.requireNonNull(idAliaser, "idAliaser", log, "log");
            return new IOReader<LinkBandwidthRequest>() {
                @Override
                public LinkBandwidthRequest read( ReadableByteChannel ch ) throws IOChannelReadException
                {
                    final long preamble = Serializers.longReader(ByteOrder.BIG_ENDIAN).readLong(ch);
                    if (preamble != REQUEST_PREAMBLE) {
                        throw new IOChannelReadException(
                            String.format("expected request preamble of %x to server but found %x instead",
                                REQUEST_PREAMBLE, preamble));
                    }
                    if (log.isTraceEnabled()) {
                        log.trace("Read link-bandwidth request to server: request preamble 0x{}",
                            Long.toHexString(preamble));
                    }

                    final UnidiNodePorts link = UnidiNodePorts.IO.reader(idAliaser).read(ch);
                    log.trace("Read link-bandwidth request to server: link {}", link);

                    final InfoDouble bandwidth = bandwidthReader().read(ch);
                    log.trace("Read link-bandwidth request to server: bandwidth {}", bandwidth);

                    return new LinkBandwidthRequest(link, bandwidth);
                }
            };
        }

        private static IOWriter<InfoDouble> bandwidthWriter()
        {
            IOWriter<PossibleDouble> possibleWriter = PossibleDouble.IO.writer(ByteOrder.BIG_ENDIAN);
            return new IOWriter<InfoDouble>() {

                @Override
                public void write( InfoDouble bandwidth, WritableByteChannel ch ) throws IOChannelWriteException
                {
                    possibleWriter.write(bandwidth.asPossibleInBits(), ch);
                }
            };
        }

        private static IOReader<InfoDouble> bandwidthReader()
        {
            IOReader<PossibleDouble> possibleReader = PossibleDouble.IO.reader(ByteOrder.BIG_ENDIAN);
            return new IOReader<InfoDouble>() {

                @Override
                public InfoDouble read( ReadableByteChannel ch ) throws IOChannelReadException
                {
                    return InfoDouble.ofPossibleBits(possibleReader.read(ch));
                }
            };
        }

        private IO()
        {
            // not used
        }
    }
}
