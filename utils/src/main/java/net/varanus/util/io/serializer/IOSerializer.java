package net.varanus.util.io.serializer;

/**
 * @param <T>
 */
public interface IOSerializer<T> extends IOWriter<T>, IOReader<T>,
    ChannelSerializer<T>, StreamSerializer<T>, DataSerializer<T>
{
    // merger interface
}
