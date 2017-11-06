package net.varanus.util.io.serializerdouble;


import java.io.OutputStream;

import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.io.exception.IOWriteException;


/**
 * 
 */
@ParametersAreNonnullByDefault
public interface StreamDoubleWriter
{
    public void writeDouble( double d, OutputStream out ) throws IOWriteException;
}
