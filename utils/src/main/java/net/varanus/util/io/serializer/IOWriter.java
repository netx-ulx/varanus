package net.varanus.util.io.serializer;


import java.io.DataOutput;
import java.io.OutputStream;
import java.nio.channels.WritableByteChannel;

import net.varanus.util.io.exception.IOChannelWriteException;
import net.varanus.util.io.exception.IOWriteException;


/**
 * @param <T>
 */
public interface IOWriter<T> extends ChannelWriter<T>, StreamWriter<T>, DataWriter<T>
{
    @Override
    public default void write( T obj, WritableByteChannel ch ) throws IOChannelWriteException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public default void write( T obj, OutputStream out ) throws IOWriteException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public default void write( T obj, DataOutput out ) throws IOWriteException
    {
        throw new UnsupportedOperationException("not implemented");
    }
}
