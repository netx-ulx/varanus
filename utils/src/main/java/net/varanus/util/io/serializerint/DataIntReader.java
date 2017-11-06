package net.varanus.util.io.serializerint;


import java.io.DataInput;

import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.io.exception.IOReadException;


/**
 * 
 */
@ParametersAreNonnullByDefault
public interface DataIntReader
{
    public int readInt( DataInput in ) throws IOReadException;
}
