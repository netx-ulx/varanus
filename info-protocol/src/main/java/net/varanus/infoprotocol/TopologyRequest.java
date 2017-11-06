package net.varanus.infoprotocol;


import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Objects;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import org.slf4j.Logger;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.io.Serializers;
import net.varanus.util.io.exception.IOChannelReadException;
import net.varanus.util.io.exception.IOChannelWriteException;
import net.varanus.util.io.serializer.IOReader;
import net.varanus.util.io.serializer.IOWriter;


/**
 * 
 */
@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class TopologyRequest extends Generic.Topo
{
    public TopologyRequest()
    {
        // nothing to initialize
    }

    @Override
    public String toString()
    {
        return "()";
    }

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class IO
    {
        private static final long REQUEST_PREAMBLE = 0xa09a3b412c5e0bf3L;

        public static IOWriter<TopologyRequest> writer( Logger log )
        {
            Objects.requireNonNull(log);
            return new IOWriter<TopologyRequest>() {
                @Override
                public void write( TopologyRequest request, WritableByteChannel ch ) throws IOChannelWriteException
                {
                    if (log.isTraceEnabled()) {
                        log.trace("Writing topology request to server: request preamble 0x{}",
                            Long.toHexString(REQUEST_PREAMBLE));
                    }
                    Serializers.longWriter(ByteOrder.BIG_ENDIAN).writeLong(REQUEST_PREAMBLE, ch);
                }
            };
        }

        public static IOReader<TopologyRequest> reader( Logger log )
        {
            Objects.requireNonNull(log);
            return new IOReader<TopologyRequest>() {
                @Override
                public TopologyRequest read( ReadableByteChannel ch ) throws IOChannelReadException
                {
                    final long preamble = Serializers.longReader(ByteOrder.BIG_ENDIAN).readLong(ch);
                    if (preamble != REQUEST_PREAMBLE) {
                        throw new IOChannelReadException(
                            String.format("expected request preamble of %x to server but found %x instead",
                                REQUEST_PREAMBLE, preamble));
                    }
                    if (log.isTraceEnabled()) {
                        log.trace("Read topology request to server: request preamble 0x{}",
                            Long.toHexString(preamble));
                    }

                    return new TopologyRequest();
                }
            };
        }

        private IO()
        {
            // not used
        }
    }
}
