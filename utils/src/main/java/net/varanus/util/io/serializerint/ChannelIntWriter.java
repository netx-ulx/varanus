package net.varanus.util.io.serializerint;


import java.nio.channels.WritableByteChannel;

import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.io.exception.IOChannelWriteException;


/**
 * 
 */
@ParametersAreNonnullByDefault
public interface ChannelIntWriter
{
    public void writeInt( int i, WritableByteChannel ch ) throws IOChannelWriteException;
}
