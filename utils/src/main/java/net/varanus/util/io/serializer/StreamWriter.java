package net.varanus.util.io.serializer;


import java.io.OutputStream;

import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.io.exception.IOWriteException;


/**
 * @param <T>
 */
@ParametersAreNonnullByDefault
public interface StreamWriter<T>
{
    public void write( T obj, OutputStream out ) throws IOWriteException;
}
