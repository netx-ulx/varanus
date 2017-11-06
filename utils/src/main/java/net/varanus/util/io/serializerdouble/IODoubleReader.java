package net.varanus.util.io.serializerdouble;


import java.io.DataInput;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;

import net.varanus.util.io.exception.IOChannelReadException;
import net.varanus.util.io.exception.IOReadException;


/**
 * 
 */
public interface IODoubleReader extends ChannelDoubleReader, StreamDoubleReader, DataDoubleReader
{
    @Override
    public default double readDouble( ReadableByteChannel ch ) throws IOChannelReadException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public default double readDouble( InputStream in ) throws IOReadException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public default double readDouble( DataInput in ) throws IOReadException
    {
        throw new UnsupportedOperationException("not implemented");
    }
}
