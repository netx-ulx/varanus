package net.varanus.util.io.serializer;


import java.io.DataOutput;

import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.io.exception.IOWriteException;


/**
 * @param <T>
 */
@ParametersAreNonnullByDefault
public interface DataWriter<T>
{
    public void write( T obj, DataOutput out ) throws IOWriteException;
}
