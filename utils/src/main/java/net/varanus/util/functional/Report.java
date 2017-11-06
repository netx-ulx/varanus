package net.varanus.util.functional;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.io.Serializers;
import net.varanus.util.io.exception.IOChannelReadException;
import net.varanus.util.io.exception.IOChannelWriteException;
import net.varanus.util.io.exception.IOReadException;
import net.varanus.util.io.exception.IOWriteException;
import net.varanus.util.io.serializer.IOReader;
import net.varanus.util.io.serializer.IOWriter;


/**
 * @param <T>
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class Report<T>
{
    public static <T> Report<T> of( T value )
    {
        return new Report<>(
            Optional.of(value),
            Optional.empty());
    }

    public static <T> Report<T> ofError( String error )
    {
        return new Report<>(
            Optional.empty(),
            Optional.of(error));
    }

    private final Optional<T>      value;
    private final Optional<String> error;

    private Report( Optional<T> value, Optional<String> error )
    {
        this.value = value;
        this.error = error;
    }

    public boolean hasValue()
    {
        return value.isPresent();
    }

    public T getValue() throws NoSuchElementException
    {
        return value.orElseThrow(() -> new NoSuchElementException("value is not present"));
    }

    public boolean hasError()
    {
        return error.isPresent();
    }

    public String getError() throws NoSuchElementException
    {
        return error.orElseThrow(() -> new NoSuchElementException("error is not present"));
    }

    @Override
    public boolean equals( Object other )
    {
        return other instanceof Report<?>
               && this.equals((Report<?>)other);
    }

    public boolean equals( Report<?> other )
    {
        return other != null
               && this.value.equals(other.value)
               && this.error.equals(other.error);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(value, error);
    }

    @Override
    public String toString()
    {
        return toString(T::toString);
    }

    public String toString( Function<T, String> valueConverter )
    {
        return value.map(valueConverter).orElseGet(this::getError);
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class IO
    {
        public static <T> IOWriter<Report<T>> writer( IOWriter<T> valueWriter )
        {
            Objects.requireNonNull(valueWriter);
            return new IOWriter<Report<T>>() {

                @Override
                public void write( Report<T> rep, WritableByteChannel ch ) throws IOChannelWriteException
                {
                    boolean hasValue = rep.hasValue();
                    Serializers.boolWriter().write(hasValue, ch);
                    if (hasValue)
                        valueWriter.write(rep.getValue(), ch);
                    else
                        ERROR_WRITER.write(rep.getError(), ch);
                }

                @Override
                public void write( Report<T> rep, OutputStream out ) throws IOWriteException
                {
                    boolean hasValue = rep.hasValue();
                    Serializers.boolWriter().write(hasValue, out);
                    if (hasValue)
                        valueWriter.write(rep.getValue(), out);
                    else
                        ERROR_WRITER.write(rep.getError(), out);
                }

                @Override
                public void write( Report<T> rep, DataOutput out ) throws IOWriteException
                {
                    boolean hasValue = rep.hasValue();
                    Serializers.boolWriter().write(hasValue, out);
                    if (hasValue)
                        valueWriter.write(rep.getValue(), out);
                    else
                        ERROR_WRITER.write(rep.getError(), out);
                }
            };
        }

        public static <T> IOReader<Report<T>> reader( IOReader<T> valueReader )
        {
            Objects.requireNonNull(valueReader);
            return new IOReader<Report<T>>() {

                @Override
                public Report<T> read( ReadableByteChannel ch ) throws IOChannelReadException
                {
                    boolean hasValue = Serializers.boolReader().read(ch);
                    if (hasValue)
                        return Report.of(valueReader.read(ch));
                    else
                        return Report.ofError(ERROR_READER.read(ch));
                }

                @Override
                public Report<T> read( InputStream in ) throws IOReadException
                {
                    boolean hasValue = Serializers.boolReader().read(in);
                    if (hasValue)
                        return Report.of(valueReader.read(in));
                    else
                        return Report.ofError(ERROR_READER.read(in));
                }

                @Override
                public Report<T> read( DataInput in ) throws IOReadException
                {
                    boolean hasValue = Serializers.boolReader().read(in);
                    if (hasValue)
                        return Report.of(valueReader.read(in));
                    else
                        return Report.ofError(ERROR_READER.read(in));
                }
            };
        }

        private static final IOWriter<String> ERROR_WRITER = Serializers.stringWriter(StandardCharsets.UTF_8);
        private static final IOReader<String> ERROR_READER = Serializers.stringReader(StandardCharsets.UTF_8);

        private IO()
        {
            // not used
        }
    }
}
