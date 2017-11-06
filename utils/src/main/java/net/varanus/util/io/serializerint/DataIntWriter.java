package net.varanus.util.io.serializerint;


import java.io.DataOutput;

import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.io.exception.IOWriteException;


/**
 * 
 */
@ParametersAreNonnullByDefault
public interface DataIntWriter
{
    public void writeInt( int i, DataOutput out ) throws IOWriteException;
}
