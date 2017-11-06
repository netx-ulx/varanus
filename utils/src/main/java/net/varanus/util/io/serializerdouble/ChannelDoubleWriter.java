package net.varanus.util.io.serializerdouble;


import java.nio.channels.WritableByteChannel;

import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.io.exception.IOChannelWriteException;


/**
 * 
 */
@ParametersAreNonnullByDefault
public interface ChannelDoubleWriter
{
    public void writeDouble( double d, WritableByteChannel ch ) throws IOChannelWriteException;
}
