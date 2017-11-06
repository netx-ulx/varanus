package net.varanus.util.io;


import java.nio.ByteBuffer;
import java.nio.channels.ByteChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.function.Consumer;

import javax.annotation.ParametersAreNonnullByDefault;

import org.slf4j.Logger;

import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.io.ExtraChannels.TraceableByteChannel;
import net.varanus.util.io.ExtraChannels.TraceableReadableByteChannel;
import net.varanus.util.io.ExtraChannels.TraceableWritableByteChannel;


/**
 *
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class DebugUtils
{
    public static TraceableWritableByteChannel debuggedChannel( WritableByteChannel ch, Logger log )
    {
        return ExtraChannels.getTraceable(ch, writeTracer(log));
    }

    public static TraceableReadableByteChannel debuggedChannel( ReadableByteChannel ch, Logger log )
    {
        return ExtraChannels.getTraceable(ch, readTracer(log));
    }

    public static TraceableByteChannel debuggedChannel( ByteChannel ch, Logger log )
    {
        return ExtraChannels.getTraceable(ch, writeTracer(log), readTracer(log));
    }

    private static Consumer<ByteBuffer> writeTracer( Logger log )
    {
        return ( buf ) -> {
            if (log.isTraceEnabled())
                log.trace("[WRITE]: {}", ByteBuffers.toHexString(buf));
        };
    }

    private static Consumer<ByteBuffer> readTracer( Logger log )
    {
        return ( buf ) -> {
            if (log.isTraceEnabled())
                log.trace("[READ]: {}", ByteBuffers.toHexString(buf));
        };
    }

    private DebugUtils()
    {
        // not used
    }
}
