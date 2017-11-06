package net.varanus.util.io.serializerlong;


import java.nio.channels.ReadableByteChannel;

import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.io.exception.IOChannelReadException;


/**
 * 
 */
@ParametersAreNonnullByDefault
public interface ChannelLongReader
{
    public long readLong( ReadableByteChannel ch ) throws IOChannelReadException;
}
