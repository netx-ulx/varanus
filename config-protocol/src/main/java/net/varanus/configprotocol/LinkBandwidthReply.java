package net.varanus.configprotocol;


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

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.functional.PossibleDouble;
import net.varanus.util.functional.Report;
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
public final class LinkBandwidthReply extends Generic.LinkBandwidth
{
    public static LinkBandwidthReply of( UnidiNodePorts link, InfoDouble bandwidth )
    {
        return new LinkBandwidthReply(Objects.requireNonNull(link), Report.of(bandwidth));
    }

    public static LinkBandwidthReply ofError( UnidiNodePorts link, String errorMsg )
    {
        return new LinkBandwidthReply(Objects.requireNonNull(link), Report.ofError(errorMsg));
    }

    private final UnidiNodePorts     link;
    private final Report<InfoDouble> result;

    private LinkBandwidthReply( UnidiNodePorts link, Report<InfoDouble> result )
    {
        this.link = link;
        this.result = result;
    }

    public UnidiNodePorts getLink()
    {
        return link;
    }

    public boolean hasResult()
    {
        return result.hasValue();
    }

    public InfoDouble getBandwidth() throws NoSuchElementException
    {
        return result.getValue();
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
        return String.format("( link %s; %s )", link, result.toString(LinkBandwidthReply::bandwidthString));
    }

    private static String bandwidthString( InfoDouble bandwidth )
    {
        return String.format("bandwidth %s", bandwidth);
    }

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class IO
    {
        private static final long REPLY_PREAMBLE = 0xe379fd3cba52bd47L;

        public static IOWriter<LinkBandwidthReply> writer( Logger log )
        {
            Objects.requireNonNull(log);
            return new IOWriter<LinkBandwidthReply>() {
                @Override
                public void write( LinkBandwidthReply reply, WritableByteChannel ch ) throws IOChannelWriteException
                {

                    if (log.isTraceEnabled()) {
                        log.trace("Writing link-bandwidth reply from server: reply preamble 0x{}",
                            Long.toHexString(REPLY_PREAMBLE));
                    }
                    Serializers.longWriter(ByteOrder.BIG_ENDIAN).writeLong(REPLY_PREAMBLE, ch);

                    final UnidiNodePorts link = reply.link;
                    log.trace("Writing link-bandwidth reply from server: link {}", link);
                    UnidiNodePorts.IO.writer().write(link, ch);

                    final Report<InfoDouble> result = reply.result;
                    log.trace("Writing link-bandwidth reply from server: {}",
                        result.toString(LinkBandwidthReply::bandwidthString));
                    Report.IO.writer(bandwidthWriter()).write(result, ch);
                }
            };
        }

        public static IOReader<LinkBandwidthReply> reader( Function<DatapathId, String> idAliaser, Logger log )
        {
            MoreObjects.requireNonNull(idAliaser, "idAliaser", log, "log");
            return new IOReader<LinkBandwidthReply>() {
                @Override
                public LinkBandwidthReply read( ReadableByteChannel ch ) throws IOChannelReadException
                {
                    final long preamble = Serializers.longReader(ByteOrder.BIG_ENDIAN).readLong(ch);
                    if (preamble != REPLY_PREAMBLE) {
                        throw new IOChannelReadException(
                            String.format("expected reply preamble of %x from server but found %x instead",
                                REPLY_PREAMBLE, preamble));
                    }
                    if (log.isTraceEnabled()) {
                        log.trace("Read link-bandwidth reply from server: reply preamble 0x{}",
                            Long.toHexString(preamble));
                    }

                    UnidiNodePorts link = UnidiNodePorts.IO.reader(idAliaser).read(ch);
                    log.trace("Read link-bandwidth reply from server: link {}", link);

                    Report<InfoDouble> result = Report.IO.reader(bandwidthReader()).read(ch);
                    log.trace("Read link-bandwidth reply from server: {}",
                        result.toString(LinkBandwidthReply::bandwidthString));

                    return new LinkBandwidthReply(link, result);
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
