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
import net.varanus.util.functional.Report;
import net.varanus.util.io.Serializers;
import net.varanus.util.io.exception.IOChannelReadException;
import net.varanus.util.io.exception.IOChannelWriteException;
import net.varanus.util.io.serializer.IOReader;
import net.varanus.util.io.serializer.IOWriter;
import net.varanus.util.lang.MoreObjects;
import net.varanus.util.openflow.types.BidiNodePorts;


/**
 * 
 */
@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class LinkEnablingReply extends Generic.LinkEnabling
{
    public static LinkEnablingReply of( BidiNodePorts bidiLink, boolean enabled )
    {
        return new LinkEnablingReply(Objects.requireNonNull(bidiLink), Report.of(enabled));
    }

    public static LinkEnablingReply ofError( BidiNodePorts bidiLink, String errorMsg )
    {
        return new LinkEnablingReply(Objects.requireNonNull(bidiLink), Report.ofError(errorMsg));
    }

    private final BidiNodePorts   bidiLink;
    private final Report<Boolean> result;

    private LinkEnablingReply( BidiNodePorts bidiLink, Report<Boolean> result )
    {
        this.bidiLink = bidiLink;
        this.result = result;
    }

    public BidiNodePorts getBidiLink()
    {
        return bidiLink;
    }

    public boolean hasResult()
    {
        return result.hasValue();
    }

    public boolean getEnabled() throws NoSuchElementException
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
        return String.format("( link %s; %s )", bidiLink, result.toString(LinkEnablingReply::enabledString));
    }

    private static String enabledString( boolean enable )
    {
        return enable ? "enabled" : "disabled";
    }

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class IO
    {
        private static final long REPLY_PREAMBLE = 0x3a257e2b6c2f81c5L;

        public static IOWriter<LinkEnablingReply> writer( Logger log )
        {
            Objects.requireNonNull(log);
            return new IOWriter<LinkEnablingReply>() {
                @Override
                public void write( LinkEnablingReply reply, WritableByteChannel ch ) throws IOChannelWriteException
                {
                    if (log.isTraceEnabled()) {
                        log.trace("Writing link-enabling reply from server: reply preamble 0x{}",
                            Long.toHexString(REPLY_PREAMBLE));
                    }
                    Serializers.longWriter(ByteOrder.BIG_ENDIAN).writeLong(REPLY_PREAMBLE, ch);

                    final BidiNodePorts bidiLink = reply.bidiLink;
                    log.trace("Writing link-enabling reply from server: link {}", bidiLink);
                    BidiNodePorts.IO.writer().write(bidiLink, ch);

                    final Report<Boolean> result = reply.result;
                    log.trace("Writing link-enabling reply from server: {}",
                        result.toString(LinkEnablingReply::enabledString));
                    Report.IO.writer(Serializers.boolWriter()).write(result, ch);
                }
            };
        }

        public static IOReader<LinkEnablingReply> reader( Function<DatapathId, String> idAliaser, Logger log )
        {
            MoreObjects.requireNonNull(idAliaser, "idAliaser", log, "log");
            return new IOReader<LinkEnablingReply>() {
                @Override
                public LinkEnablingReply read( ReadableByteChannel ch ) throws IOChannelReadException
                {
                    final long preamble = Serializers.longReader(ByteOrder.BIG_ENDIAN).readLong(ch);
                    if (preamble != REPLY_PREAMBLE) {
                        throw new IOChannelReadException(
                            String.format("expected reply preamble of %x from server but found %x instead",
                                REPLY_PREAMBLE, preamble));
                    }
                    if (log.isTraceEnabled()) {
                        log.trace("Read link-enabling reply from server: reply preamble 0x{}",
                            Long.toHexString(preamble));
                    }

                    BidiNodePorts bidiLink = BidiNodePorts.IO.reader(idAliaser).read(ch);
                    log.trace("Read link-enabling reply from server: link {}", bidiLink);

                    Report<Boolean> result = Report.IO.reader(Serializers.boolReader()).read(ch);
                    log.trace("Read link-enabling reply from server: {}",
                        result.toString(LinkEnablingReply::enabledString));

                    return new LinkEnablingReply(bidiLink, result);
                }
            };
        }

        private IO()
        {
            // not used
        }
    }
}
