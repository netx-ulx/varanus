package net.varanus.util.io.serializerlong;


import java.io.InputStream;

import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.io.exception.IOReadException;


/**
 * 
 */
@ParametersAreNonnullByDefault
public interface StreamLongReader
{
    public long readLong( InputStream in ) throws IOReadException;
}
