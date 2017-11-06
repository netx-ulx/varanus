package net.varanus.util.io.serializerlong;


import java.io.OutputStream;

import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.io.exception.IOWriteException;


/**
 * 
 */
@ParametersAreNonnullByDefault
public interface StreamLongWriter
{
    public void writeLong( long eL, OutputStream out ) throws IOWriteException;
}
