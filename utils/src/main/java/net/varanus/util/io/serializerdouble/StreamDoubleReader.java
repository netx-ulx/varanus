package net.varanus.util.io.serializerdouble;


import java.io.InputStream;

import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.io.exception.IOReadException;


/**
 * 
 */
@ParametersAreNonnullByDefault
public interface StreamDoubleReader
{
    public double readDouble( InputStream in ) throws IOReadException;
}
