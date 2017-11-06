package net.varanus.util.io.serializerlong;


import java.io.DataOutput;
import java.io.OutputStream;
import java.nio.channels.WritableByteChannel;

import net.varanus.util.io.exception.IOChannelWriteException;
import net.varanus.util.io.exception.IOWriteException;


/**
 * 
 */
public interface IOLongWriter extends ChannelLongWriter, StreamLongWriter, DataLongWriter
{
    @Override
    public default void writeLong( long eL, WritableByteChannel ch ) throws IOChannelWriteException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public default void writeLong( long eL, OutputStream out ) throws IOWriteException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public default void writeLong( long eL, DataOutput out ) throws IOWriteException
    {
        throw new UnsupportedOperationException("not implemented");
    }
}
