package net.varanus.util.io.serializerdouble;


import java.nio.channels.ReadableByteChannel;

import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.io.exception.IOChannelReadException;


/**
 * 
 */
@ParametersAreNonnullByDefault
public interface ChannelDoubleReader
{
    public double readDouble( ReadableByteChannel ch ) throws IOChannelReadException;
}
