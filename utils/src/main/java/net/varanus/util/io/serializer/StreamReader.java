package net.varanus.util.io.serializer;


import java.io.InputStream;

import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.io.exception.IOReadException;


/**
 * @param <T>
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public interface StreamReader<T>
{
    public T read( InputStream in ) throws IOReadException;
}
