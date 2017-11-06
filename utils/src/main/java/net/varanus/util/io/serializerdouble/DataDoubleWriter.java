package net.varanus.util.io.serializerdouble;


import java.io.DataOutput;

import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.io.exception.IOWriteException;


/**
 * 
 */
@ParametersAreNonnullByDefault
public interface DataDoubleWriter
{
    public void writeDouble( double d, DataOutput out ) throws IOWriteException;
}
