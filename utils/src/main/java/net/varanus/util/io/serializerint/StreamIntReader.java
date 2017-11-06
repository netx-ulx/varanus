package net.varanus.util.io.serializerint;


import java.io.InputStream;

import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.io.exception.IOReadException;


/**
 * 
 */
@ParametersAreNonnullByDefault
public interface StreamIntReader
{
    public int readInt( InputStream in ) throws IOReadException;
}
