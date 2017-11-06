package net.varanus.util.io.serializerlong;


import java.io.DataInput;

import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.io.exception.IOReadException;


/**
 * 
 */
@ParametersAreNonnullByDefault
public interface DataLongReader
{
    public long readLong( DataInput in ) throws IOReadException;
}
