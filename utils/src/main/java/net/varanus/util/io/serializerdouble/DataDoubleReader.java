package net.varanus.util.io.serializerdouble;


import java.io.DataInput;

import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.io.exception.IOReadException;


/**
 * 
 */
@ParametersAreNonnullByDefault
public interface DataDoubleReader
{
    public double readDouble( DataInput in ) throws IOReadException;
}
