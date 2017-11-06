package net.varanus.util.io.serializerint;


import java.nio.channels.ReadableByteChannel;

import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.io.exception.IOChannelReadException;


/**
 * 
 */
@ParametersAreNonnullByDefault
public interface ChannelIntReader
{
    public int readInt( ReadableByteChannel ch ) throws IOChannelReadException;
}
