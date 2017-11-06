package net.varanus.util.io.serializer;


import java.io.DataInput;

import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.io.exception.IOReadException;


/**
 * @param <T>
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public interface DataReader<T>
{
    public T read( DataInput in ) throws IOReadException;
}
