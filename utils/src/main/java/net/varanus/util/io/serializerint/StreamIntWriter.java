package net.varanus.util.io.serializerint;


import java.io.OutputStream;

import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.io.exception.IOWriteException;


/**
 * 
 */
@ParametersAreNonnullByDefault
public interface StreamIntWriter
{
    public void writeInt( int i, OutputStream out ) throws IOWriteException;
}
