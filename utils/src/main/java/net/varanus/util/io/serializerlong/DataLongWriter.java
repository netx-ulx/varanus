package net.varanus.util.io.serializerlong;


import java.io.DataOutput;

import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.io.exception.IOWriteException;


/**
 * 
 */
@ParametersAreNonnullByDefault
public interface DataLongWriter
{
    public void writeLong( long eL, DataOutput out ) throws IOWriteException;
}
