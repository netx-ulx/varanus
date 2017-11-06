package net.varanus.util.io.serializerint;


import java.io.DataOutput;
import java.io.OutputStream;
import java.nio.channels.WritableByteChannel;

import net.varanus.util.io.exception.IOChannelWriteException;
import net.varanus.util.io.exception.IOWriteException;


/**
 * 
 */
public interface IOIntWriter extends ChannelIntWriter, StreamIntWriter, DataIntWriter
{
    @Override
    public default void writeInt( int i, WritableByteChannel ch ) throws IOChannelWriteException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public default void writeInt( int i, OutputStream out ) throws IOWriteException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public default void writeInt( int i, DataOutput out ) throws IOWriteException
    {
        throw new UnsupportedOperationException("not implemented");
    }
}
