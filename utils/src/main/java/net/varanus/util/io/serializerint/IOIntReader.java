package net.varanus.util.io.serializerint;


import java.io.DataInput;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;

import net.varanus.util.io.exception.IOChannelReadException;
import net.varanus.util.io.exception.IOReadException;


/**
 * 
 */
public interface IOIntReader extends ChannelIntReader, StreamIntReader, DataIntReader
{
    @Override
    public default int readInt( ReadableByteChannel ch ) throws IOChannelReadException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public default int readInt( InputStream in ) throws IOReadException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public default int readInt( DataInput in ) throws IOReadException
    {
        throw new UnsupportedOperationException("not implemented");
    }
}
