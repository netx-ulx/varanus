package net.varanus.util.io.serializerlong;


import java.io.DataInput;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;

import net.varanus.util.io.exception.IOChannelReadException;
import net.varanus.util.io.exception.IOReadException;


/**
 * 
 */
public interface IOLongReader extends ChannelLongReader, StreamLongReader, DataLongReader
{
    @Override
    public default long readLong( ReadableByteChannel ch ) throws IOChannelReadException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public default long readLong( InputStream in ) throws IOReadException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public default long readLong( DataInput in ) throws IOReadException
    {
        throw new UnsupportedOperationException("not implemented");
    }
}
