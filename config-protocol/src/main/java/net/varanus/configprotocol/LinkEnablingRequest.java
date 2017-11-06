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
public final class LinkEnablingRequest extends Generic.LinkEnabling
{
    private final BidiNodePorts bidiLink;
    private final boolean       enable;

    public LinkEnablingRequest( BidiNodePorts bidiLink, boolean enable )
    {
        this.bidiLink = Objects.requireNonNull(bidiLink);
        this.enable = enable;
    }

    public BidiNodePorts getBidiLink()
    {
        return bidiLink;
    }

    public boolean getEnable()
    {
        return enable;
    }

    @Override
    public String toString()
    {
        return String.format("( %s link %s )", enableString(enable), bidiLink);
    }

    private static String enableString( boolean enable )
    {
        return enable ? "enable" : "disable";
    }

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class IO
    {
        private static final long REQUEST_PREAMBLE = 0xdc4dc2d0132bbe88L;

        public static IOWriter<LinkEnablingRequest> writer( Logger log )
        {
            Objects.requireNonNull(log);
            return new IOWriter<LinkEnablingRequest>() {
                @Override
                public void write( LinkEnablingRequest request, WritableByteChannel ch ) throws IOChannelWriteException
                {
                    final BidiNodePorts bidiLink = request.bidiLink;
                    final boolean enable = request.enable;

                    if (log.isTraceEnabled()) {
                        log.trace("Writing link-enabling request to server: request preamble 0x{}",
                            Long.toHexString(REQUEST_PREAMBLE));
                    }
                    Serializers.longWriter(ByteOrder.BIG_ENDIAN).writeLong(REQUEST_PREAMBLE, ch);

                    log.trace("Writing link-enabling request to server: {} {}", enableString(enable), bidiLink);
                    BidiNodePorts.IO.writer().write(bidiLink, ch);
                    Serializers.boolWriter().write(enable, ch);
                }
            };
        }

        public static IOReader<LinkEnablingRequest> reader( Function<DatapathId, String> idAliaser, Logger log )
        {
            MoreObjects.requireNonNull(idAliaser, "idAliaser", log, "log");
            return new IOReader<LinkEnablingRequest>() {
                @Override
                public LinkEnablingRequest read( ReadableByteChannel ch ) throws IOChannelReadException
                {
                    final long preamble = Serializers.longReader(ByteOrder.BIG_ENDIAN).readLong(ch);
                    if (preamble != REQUEST_PREAMBLE) {
                        throw new IOChannelReadException(
                            String.format("expected request preamble of %x to server but found %x instead",
                                REQUEST_PREAMBLE, preamble));
                    }
                    if (log.isTraceEnabled()) {
                        log.trace("Read link-enabling request to server: request preamble 0x{}",
                            Long.toHexString(preamble));
                    }

                    final BidiNodePorts bidiLink = BidiNodePorts.IO.reader(idAliaser).read(ch);
                    final boolean enable = Serializers.boolReader().read(ch);
                    log.trace("Read link-enabling request to server: {} {}", enableString(enable), bidiLink);

                    return new LinkEnablingRequest(bidiLink, enable);
                }
            };
        }

        private IO()
        {
            // not used
        }
    }
}
