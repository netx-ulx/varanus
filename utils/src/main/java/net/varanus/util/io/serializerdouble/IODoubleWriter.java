package net.varanus.util.io.serializerdouble;


import java.io.DataOutput;
import java.io.OutputStream;
import java.nio.channels.WritableByteChannel;

import net.varanus.util.io.exception.IOChannelWriteException;
import net.varanus.util.io.exception.IOWriteException;


/**
 * 
 */
public interface IODoubleWriter extends ChannelDoubleWriter, StreamDoubleWriter, DataDoubleWriter
{
    @Override
    public default void writeDouble( double d, WritableByteChannel ch ) throws IOChannelWriteException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public default void writeDouble( double d, OutputStream out ) throws IOWriteException
    {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public default void writeDouble( double d, DataOutput out ) throws IOWriteException
    {
        throw new UnsupportedOperationException("not implemented");
    }
}
