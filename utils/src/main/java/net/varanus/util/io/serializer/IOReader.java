package net.varanus.util.io.serializer;


import java.io.DataInput;
import java.io.InputStream;
import java.nio.channels.ReadableByteChannel;

import net.varanus.util.io.exception.IOChannelReadException;
import net.varanus.util.io.exception.IOReadException;


/**
 * @param <T>
 */
public interface IOReader<T> extends ChannelReader<T>, StreamReader<T>, DataReader<T>
{
    @Override
    public default T read( ReadableByteChannel ch ) throws IOChannelReadException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public default T read( InputStream in ) throws IOReadException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public default T read( DataInput in ) throws IOReadException
    {
        throw new UnsupportedOperationException("not implemented");
    }
}
