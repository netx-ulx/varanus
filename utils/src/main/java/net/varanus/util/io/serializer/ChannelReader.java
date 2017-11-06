package net.varanus.util.io.serializer;


import java.nio.channels.ReadableByteChannel;

import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.io.exception.IOChannelReadException;


/**
 * @param <T>
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public interface ChannelReader<T>
{
    public T read( ReadableByteChannel ch ) throws IOChannelReadException;
}
