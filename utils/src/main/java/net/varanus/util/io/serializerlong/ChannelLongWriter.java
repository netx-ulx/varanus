package net.varanus.util.io.serializerlong;


import java.nio.channels.WritableByteChannel;

import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.io.exception.IOChannelWriteException;


/**
 * 
 */
@ParametersAreNonnullByDefault
public interface ChannelLongWriter
{
    public void writeLong( long eL, WritableByteChannel ch ) throws IOChannelWriteException;
}
