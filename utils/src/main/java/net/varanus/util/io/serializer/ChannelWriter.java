package net.varanus.util.io.serializer;


import java.nio.channels.WritableByteChannel;

import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.io.exception.IOChannelWriteException;


/**
 * @param <T>
 */
@ParametersAreNonnullByDefault
public interface ChannelWriter<T>
{
    public void write( T obj, WritableByteChannel ch ) throws IOChannelWriteException;
}
