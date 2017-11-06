package net.varanus.util.io;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.function.UnaryOperator;

import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.functional.FunctionUtils;
import net.varanus.util.io.ByteBuffers.BufferType;
import net.varanus.util.io.exception.IOChannelReadException;
import net.varanus.util.io.exception.IOChannelWriteException;
import net.varanus.util.io.exception.IOReadException;
import net.varanus.util.io.exception.IOWriteException;
import net.varanus.util.io.serializer.ChannelReader;
import net.varanus.util.io.serializer.ChannelWriter;
import net.varanus.util.io.serializer.DataReader;
import net.varanus.util.io.serializer.DataWriter;
import net.varanus.util.io.serializer.IOReader;
import net.varanus.util.io.serializer.IOSerializer;
import net.varanus.util.io.serializer.IOWriter;
import net.varanus.util.io.serializer.StreamReader;
import net.varanus.util.io.serializer.StreamWriter;
import net.varanus.util.io.serializerdouble.IODoubleReader;
import net.varanus.util.io.serializerdouble.IODoubleSerializer;
import net.varanus.util.io.serializerdouble.IODoubleWriter;
import net.varanus.util.io.serializerint.IOIntReader;
import net.varanus.util.io.serializerint.IOIntSerializer;
import net.varanus.util.io.serializerint.IOIntWriter;
import net.varanus.util.io.serializerlong.IOLongReader;
import net.varanus.util.io.serializerlong.IOLongSerializer;
import net.varanus.util.io.serializerlong.IOLongWriter;
import net.varanus.util.lang.MoreObjects;


/**
 * 
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class Serializers
{
    public static IOIntWriter intWriter( ByteOrder order )
    {
        return PrimitiveSerializer.of(order);
    }

    public static IOIntReader intReader( ByteOrder order )
    {
        return PrimitiveSerializer.of(order);
    }

    public static IOLongWriter longWriter( ByteOrder order )
    {
        return PrimitiveSerializer.of(order);
    }

    public static IOLongReader longReader( ByteOrder order )
    {
        return PrimitiveSerializer.of(order);
    }

    public static IODoubleWriter doubleWriter( ByteOrder order )
    {
        return PrimitiveSerializer.of(order);
    }

    public static IODoubleReader doubleReader( ByteOrder order )
    {
        return PrimitiveSerializer.of(order);
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    private static enum PrimitiveSerializer implements IOIntSerializer, IOLongSerializer, IODoubleSerializer
    {
        BIG_ENDIAN(ByteOrder.BIG_ENDIAN),
        LITTLE_ENDIAN(ByteOrder.LITTLE_ENDIAN);

        static PrimitiveSerializer of( ByteOrder order )
        {
            if (order.equals(ByteOrder.BIG_ENDIAN)) {
                return BIG_ENDIAN;
            }
            else if (order.equals(ByteOrder.LITTLE_ENDIAN)) {
                return LITTLE_ENDIAN;
            }
            else {
                // should never happen
                throw new AssertionError("unexpected byte order");
            }
        }

        private final transient ByteOrder order;

        private PrimitiveSerializer( ByteOrder order )
        {
            this.order = order;
        }

        @Override
        public void writeInt( int i, WritableByteChannel ch ) throws IOChannelWriteException
        {
            ExtraChannels.writeInt(ch, i, order);
        }

        @Override
        public void writeInt( int i, OutputStream out ) throws IOWriteException
        {
            IOStreams.writeInt(out, i, order);
        }

        @Override
        public void writeInt( int i, DataOutput out ) throws IOWriteException
        {
            DataIO.writeInt(out, i, order);
        }

        @Override
        public int readInt( ReadableByteChannel ch ) throws IOChannelReadException
        {
            return ExtraChannels.readInt(ch, order);
        }

        @Override
        public int readInt( InputStream in ) throws IOReadException
        {
            return IOStreams.readInt(in, order);
        }

        @Override
        public int readInt( DataInput in ) throws IOReadException
        {
            return DataIO.readInt(in, order);
        }

        @Override
        public void writeLong( long eL, WritableByteChannel ch ) throws IOChannelWriteException
        {
            ExtraChannels.writeLong(ch, eL, order);
        }

        @Override
        public void writeLong( long eL, OutputStream out ) throws IOWriteException
        {
            IOStreams.writeLong(out, eL, order);
        }

        @Override
        public void writeLong( long eL, DataOutput out ) throws IOWriteException
        {
            DataIO.writeLong(out, eL, order);
        }

        @Override
        public long readLong( ReadableByteChannel ch ) throws IOChannelReadException
        {
            return ExtraChannels.readLong(ch, order);
        }

        @Override
        public long readLong( InputStream in ) throws IOReadException
        {
            return IOStreams.readLong(in, order);
        }

        @Override
        public long readLong( DataInput in ) throws IOReadException
        {
            return DataIO.readLong(in, order);
        }

        @Override
        public void writeDouble( double d, WritableByteChannel ch ) throws IOChannelWriteException
        {
            ExtraChannels.writeDouble(ch, d, order);
        }

        @Override
        public void writeDouble( double d, OutputStream out ) throws IOWriteException
        {
            IOStreams.writeDouble(out, d, order);
        }

        @Override
        public void writeDouble( double d, DataOutput out ) throws IOWriteException
        {
            DataIO.writeDouble(out, d, order);
        }

        @Override
        public double readDouble( ReadableByteChannel ch ) throws IOChannelReadException
        {
            return ExtraChannels.readDouble(ch, order);
        }

        @Override
        public double readDouble( InputStream in ) throws IOReadException
        {
            return IOStreams.readDouble(in, order);
        }

        @Override
        public double readDouble( DataInput in ) throws IOReadException
        {
            return DataIO.readDouble(in, order);
        }
    }

    public static IOWriter<Boolean> boolWriter()
    {
        return BoolSerializer.INSTANCE;
    }

    public static IOReader<Boolean> boolReader()
    {
        return BoolSerializer.INSTANCE;
    }

    private static enum BoolSerializer implements IOSerializer<Boolean>
    {
        INSTANCE;

        @Override
        public void write( Boolean bool, WritableByteChannel ch ) throws IOChannelWriteException
        {
            ExtraChannels.writeByte(ch, toByte(bool));
        }

        @Override
        public void write( Boolean bool, OutputStream out ) throws IOWriteException
        {
            IOStreams.writeByte(out, toByte(bool));
        }

        @Override
        public void write( Boolean bool, DataOutput out ) throws IOWriteException
        {
            DataIO.writeByte(out, toByte(bool));
        }

        @Override
        public Boolean read( ReadableByteChannel ch ) throws IOChannelReadException
        {
            return toBool(ExtraChannels.readByte(ch), IOChannelReadException::new);
        }

        @Override
        public Boolean read( InputStream in ) throws IOReadException
        {
            return toBool(IOStreams.readByte(in), IOReadException::new);
        }

        @Override
        public Boolean read( DataInput in ) throws IOReadException
        {
            return toBool(DataIO.readByte(in), IOReadException::new);
        }

        private static final byte FALSE_BYTE = (byte)0b01010101;
        private static final byte TRUE_BYTE  = (byte)(~FALSE_BYTE);

        private static byte toByte( boolean bool )
        {
            return bool ? TRUE_BYTE : FALSE_BYTE;
        }

        private static <X extends IOReadException> boolean toBool( byte b, Function<String, X> exFactory ) throws X
        {
            if (b == TRUE_BYTE)
                return true;
            else if (b == FALSE_BYTE)
                return false;
            else
                throw exFactory.apply(String.format("invalid boolean binary value: %s",
                    Integer.toBinaryString(b)));
        }
    }

    public static IOWriter<String> stringWriter( Charset charset )
    {
        Objects.requireNonNull(charset);
        return new IOWriter<String>() {

            @Override
            public void write( String str, WritableByteChannel ch ) throws IOChannelWriteException
            {
                ByteBuffer buf = charset.encode(str);
                bufferWriter().write(buf, ch);
            }

            @Override
            public void write( String str, OutputStream out ) throws IOWriteException
            {
                ByteBuffer buf = charset.encode(str);
                bufferWriter().write(buf, out);
            }

            @Override
            public void write( String str, DataOutput out ) throws IOWriteException
            {
                ByteBuffer buf = charset.encode(str);
                bufferWriter().write(buf, out);
            }
        };
    }

    public static IOReader<String> stringReader( Charset charset )
    {
        Objects.requireNonNull(charset);
        IOReader<ByteBuffer> bufReader = allocatedBufferReader(BufferType.ARRAY_BACKED);
        return new IOReader<String>() {

            @Override
            public String read( ReadableByteChannel ch ) throws IOChannelReadException
            {
                ByteBuffer buf = bufReader.read(ch);
                return charset.decode(buf).toString();
            }

            @Override
            public String read( InputStream in ) throws IOReadException
            {
                ByteBuffer buf = bufReader.read(in);
                return charset.decode(buf).toString();
            }

            @Override
            public String read( DataInput in ) throws IOReadException
            {
                ByteBuffer buf = bufReader.read(in);
                return charset.decode(buf).toString();
            }
        };
    }

    @SuppressWarnings( "unchecked" )
    public static <T extends Enum<T>> IOWriter<T> enumWriter()
    {
        return (IOWriter<T>)EnumWriter.INSTANCE;
    }

    private static enum EnumWriter implements IOWriter<Enum<?>>
    {
        INSTANCE;

        @Override
        public void write( Enum<?> obj, WritableByteChannel ch ) throws IOChannelWriteException
        {
            intWriter(ByteOrder.BIG_ENDIAN).writeInt(obj.ordinal(), ch);
        }

        @Override
        public void write( Enum<?> obj, OutputStream out ) throws IOWriteException
        {
            intWriter(ByteOrder.BIG_ENDIAN).writeInt(obj.ordinal(), out);
        }

        @Override
        public void write( Enum<?> obj, DataOutput out ) throws IOWriteException
        {
            intWriter(ByteOrder.BIG_ENDIAN).writeInt(obj.ordinal(), out);
        }
    }

    public static <T extends Enum<T>> IOReader<T> enumReader( Class<T> enumClass )
    {
        final T[] enumValues = enumClass.getEnumConstants();
        Preconditions.checkNotNull(enumValues, "invalid enum class");
        return new IOReader<T>() {

            @Override
            public T read( ReadableByteChannel ch ) throws IOChannelReadException
            {
                return fromOrdinal(intReader(ByteOrder.BIG_ENDIAN).readInt(ch), IOChannelReadException::new);
            }

            @Override
            public T read( InputStream in ) throws IOReadException
            {
                return fromOrdinal(intReader(ByteOrder.BIG_ENDIAN).readInt(in), IOReadException::new);
            }

            @Override
            public T read( DataInput in ) throws IOReadException
            {
                return fromOrdinal(intReader(ByteOrder.BIG_ENDIAN).readInt(in), IOReadException::new);
            }

            private <X extends IOReadException> T fromOrdinal( int ordinal, Function<String, X> exFactory ) throws X
            {
                if (ordinal < 0 || ordinal >= enumValues.length) {
                    throw exFactory.apply(String.format("read %s enum ordinal is out of bounds: %d",
                        enumClass.getSimpleName(), ordinal));
                }

                return enumValues[ordinal];
            }
        };
    }

    public static IOWriter<Instant> instantWriter()
    {
        return InstantSerializer.INSTANCE;
    }

    public static IOReader<Instant> instantReader()
    {
        return InstantSerializer.INSTANCE;
    }

    private static enum InstantSerializer implements IOSerializer<Instant>
    {
        INSTANCE;

        @Override
        public void write( Instant time, WritableByteChannel ch ) throws IOChannelWriteException
        {
            Serializers.longWriter(ByteOrder.BIG_ENDIAN).writeLong(time.getEpochSecond(), ch);
            Serializers.intWriter(ByteOrder.BIG_ENDIAN).writeInt(time.getNano(), ch);
        }

        @Override
        public void write( Instant time, OutputStream out ) throws IOWriteException
        {
            Serializers.longWriter(ByteOrder.BIG_ENDIAN).writeLong(time.getEpochSecond(), out);
            Serializers.intWriter(ByteOrder.BIG_ENDIAN).writeInt(time.getNano(), out);
        }

        @Override
        public void write( Instant time, DataOutput out ) throws IOWriteException
        {
            Serializers.longWriter(ByteOrder.BIG_ENDIAN).writeLong(time.getEpochSecond(), out);
            Serializers.intWriter(ByteOrder.BIG_ENDIAN).writeInt(time.getNano(), out);
        }

        @Override
        public Instant read( ReadableByteChannel ch ) throws IOChannelReadException
        {
            long secs = Serializers.longReader(ByteOrder.BIG_ENDIAN).readLong(ch);
            int nanos = Serializers.intReader(ByteOrder.BIG_ENDIAN).readInt(ch);
            return Instant.ofEpochSecond(secs, nanos);
        }

        @Override
        public Instant read( InputStream in ) throws IOReadException
        {
            long secs = Serializers.longReader(ByteOrder.BIG_ENDIAN).readLong(in);
            int nanos = Serializers.intReader(ByteOrder.BIG_ENDIAN).readInt(in);
            return Instant.ofEpochSecond(secs, nanos);
        }

        @Override
        public Instant read( DataInput in ) throws IOReadException
        {
            long secs = Serializers.longReader(ByteOrder.BIG_ENDIAN).readLong(in);
            int nanos = Serializers.intReader(ByteOrder.BIG_ENDIAN).readInt(in);
            return Instant.ofEpochSecond(secs, nanos);
        }
    }

    public static IOWriter<Duration> durationWriter()
    {
        return DurationSerializer.INSTANCE;
    }

    public static IOReader<Duration> durationReader()
    {
        return DurationSerializer.INSTANCE;
    }

    private static enum DurationSerializer implements IOSerializer<Duration>
    {
        INSTANCE;

        @Override
        public void write( Duration dur, WritableByteChannel ch ) throws IOChannelWriteException
        {
            Serializers.longWriter(ByteOrder.BIG_ENDIAN).writeLong(dur.toNanos(), ch);
        }

        @Override
        public void write( Duration dur, OutputStream out ) throws IOWriteException
        {
            Serializers.longWriter(ByteOrder.BIG_ENDIAN).writeLong(dur.toNanos(), out);
        }

        @Override
        public void write( Duration dur, DataOutput out ) throws IOWriteException
        {
            Serializers.longWriter(ByteOrder.BIG_ENDIAN).writeLong(dur.toNanos(), out);
        }

        @Override
        public Duration read( ReadableByteChannel ch ) throws IOChannelReadException
        {
            return ofNanos(Serializers.longReader(ByteOrder.BIG_ENDIAN).readLong(ch), IOChannelReadException::new);
        }

        @Override
        public Duration read( InputStream in ) throws IOReadException
        {
            return ofNanos(Serializers.longReader(ByteOrder.BIG_ENDIAN).readLong(in), IOReadException::new);
        }

        @Override
        public Duration read( DataInput in ) throws IOReadException
        {
            return ofNanos(Serializers.longReader(ByteOrder.BIG_ENDIAN).readLong(in), IOReadException::new);
        }

        private static <X extends IOReadException> Duration ofNanos( long nanos, Function<String, X> exFactory )
            throws X
        {
            if (nanos < 0) {
                throw exFactory.apply(String.format("read invalid negative nanosecond duration: %d", nanos));
            }
            else {
                return Duration.ofNanos(nanos);
            }
        }
    }

    public static IOWriter<ByteBuffer> bufferWriter()
    {
        return BufferWriter.INSTANCE;
    }

    private static enum BufferWriter implements IOWriter<ByteBuffer>
    {
        INSTANCE;

        @Override
        public void write( ByteBuffer buf, WritableByteChannel ch ) throws IOChannelWriteException
        {
            intWriter(ByteOrder.BIG_ENDIAN).writeInt(buf.remaining(), ch);
            ExtraChannels.writeBytes(ch, buf);
        }

        @Override
        public void write( ByteBuffer buf, OutputStream out ) throws IOWriteException
        {
            intWriter(ByteOrder.BIG_ENDIAN).writeInt(buf.remaining(), out);
            IOStreams.writeBytes(out, buf);
        }

        @Override
        public void write( ByteBuffer buf, DataOutput out ) throws IOWriteException
        {
            intWriter(ByteOrder.BIG_ENDIAN).writeInt(buf.remaining(), out);
            DataIO.writeBytes(out, buf);
        }
    }

    public static IOReader<ByteBuffer> bufferReader( IntFunction<? extends ByteBuffer> bufFactory )
    {
        Objects.requireNonNull(bufFactory);
        return new IOReader<ByteBuffer>() {

            @Override
            public ByteBuffer read( ReadableByteChannel ch ) throws IOChannelReadException
            {
                final int len = validLength(intReader(ByteOrder.BIG_ENDIAN).readInt(ch), IOChannelReadException::new);
                ByteBuffer buf = bufFactory.apply(len);
                ExtraChannels.readBytes(ch, buf, len, BufferOperation.FLIP_RELATIVELY);
                return buf;
            }

            @Override
            public ByteBuffer read( InputStream in ) throws IOReadException
            {
                final int len = validLength(intReader(ByteOrder.BIG_ENDIAN).readInt(in), IOReadException::new);
                ByteBuffer buf = bufFactory.apply(len);
                IOStreams.readBytes(in, buf, len, BufferOperation.FLIP_RELATIVELY);
                return buf;
            }

            @Override
            public ByteBuffer read( DataInput in ) throws IOReadException
            {
                final int len = validLength(intReader(ByteOrder.BIG_ENDIAN).readInt(in), IOReadException::new);
                ByteBuffer buf = bufFactory.apply(len);
                DataIO.readBytes(in, buf, len, BufferOperation.FLIP_RELATIVELY);
                return buf;
            }

            private <X extends IOReadException> int validLength( int len, Function<String, X> exFactory ) throws X
            {
                if (len < 0)
                    throw exFactory.apply("read invalid negative length");
                else
                    return len;
            }
        };
    }

    public static IOReader<ByteBuffer> allocatedBufferReader( BufferType type )
    {
        return bufferReader(ByteBuffers.allocator(type));
    }

    public static IOReader<ByteBuffer> cachedBufferReader( BufferType type )
    {
        return bufferReader(ByteBuffers.cachedSupplier(type));
    }

    public static IOWriter<ByteBuffer> rawBytesWriter()
    {
        return RawBytesWriter.INSTANCE;
    }

    private static enum RawBytesWriter implements IOWriter<ByteBuffer>
    {
        INSTANCE;

        @Override
        public void write( ByteBuffer raw, WritableByteChannel ch ) throws IOChannelWriteException
        {
            ExtraChannels.writeBytes(ch, raw);
        }

        @Override
        public void write( ByteBuffer raw, OutputStream out ) throws IOWriteException
        {
            IOStreams.writeBytes(out, raw);
        }

        @Override
        public void write( ByteBuffer raw, DataOutput out ) throws IOWriteException
        {
            DataIO.writeBytes(out, raw);
        }
    }

    public static IOReader<ByteBuffer> rawBytesReader( Supplier<ByteBuffer> bufFactory )
    {
        Objects.requireNonNull(bufFactory);
        return new IOReader<ByteBuffer>() {

            @Override
            public ByteBuffer read( ReadableByteChannel ch ) throws IOChannelReadException
            {
                ByteBuffer raw = bufFactory.get();
                ExtraChannels.readBytes(ch, raw, BufferOperation.FLIP_RELATIVELY);
                return raw;
            }

            @Override
            public ByteBuffer read( InputStream in ) throws IOReadException
            {
                ByteBuffer raw = bufFactory.get();
                IOStreams.readBytes(in, raw, BufferOperation.FLIP_RELATIVELY);
                return raw;
            }

            @Override
            public ByteBuffer read( DataInput in ) throws IOReadException
            {
                ByteBuffer raw = bufFactory.get();
                DataIO.readBytes(in, raw, BufferOperation.FLIP_RELATIVELY);
                return raw;
            }
        };
    }

    public static IOReader<ByteBuffer> allocatedRawBytesReader( int capacity, BufferType type )
    {
        Objects.requireNonNull(type);
        if (capacity < 0)
            throw new IllegalArgumentException("capacity must not be negative");

        return rawBytesReader(() -> ByteBuffers.allocate(capacity, type));
    }

    public static IOReader<ByteBuffer> cachedRawBytesReader( int capacity, BufferType type )
    {
        Objects.requireNonNull(type);
        if (capacity < 0)
            throw new IllegalArgumentException("capacity must not be negative");
        if (capacity > ByteBuffers.MAX_CACHED_BUFFER_CAPACITY)
            throw new IllegalArgumentException("capacity exceeds maximum cached buffer size");

        return rawBytesReader(() -> ByteBuffers.getCached(capacity, type));
    }

    public static <T> IOWriter<T[]> arrayWriter( IOWriter<? super T> elWriter )
    {
        Objects.requireNonNull(elWriter);
        return new IOWriter<T[]>() {

            @Override
            public void write( T[] arr, WritableByteChannel ch ) throws IOChannelWriteException
            {
                writeMultiple(Arrays.asList(arr), List::size, ch, elWriter);
            }

            @Override
            public void write( T[] arr, OutputStream out ) throws IOWriteException
            {
                writeMultiple(Arrays.asList(arr), List::size, out, elWriter);
            }

            @Override
            public void write( T[] arr, DataOutput out ) throws IOWriteException
            {
                writeMultiple(Arrays.asList(arr), List::size, out, elWriter);
            }
        };
    }

    public static <T> IOReader<T[]> arrayReader( IOReader<? extends T> elReader, Class<T> objType )
    {
        MoreObjects.requireNonNull(elReader, "elReader", objType, "objType");
        return new IOReader<T[]>() {

            @Override
            public T[] read( ReadableByteChannel ch ) throws IOChannelReadException
            {
                return readMultiple(ch, elReader,
                    arraySupplier(objType), arrayAccumulator(), UnaryOperator.identity());
            }

            @Override
            public T[] read( InputStream in ) throws IOReadException
            {
                return readMultiple(in, elReader,
                    arraySupplier(objType), arrayAccumulator(), UnaryOperator.identity());
            }

            @Override
            public T[] read( DataInput in ) throws IOReadException
            {
                return readMultiple(in, elReader,
                    arraySupplier(objType), arrayAccumulator(), UnaryOperator.identity());
            }
        };
    }

    @SuppressWarnings( "unchecked" )
    private static <T> IntFunction<T[]> arraySupplier( final Class<T> objType )
    {
        return ( size ) -> (T[])Array.newInstance(objType, size);
    }

    private static <T> BiConsumer<T[], T> arrayAccumulator()
    {
        final AtomicInteger ai = new AtomicInteger(0);
        return ( arr, el ) -> arr[ai.getAndIncrement()] = el;
    }

    public static <T, I extends Iterable<? extends T>> IOWriter<I> multiWriter( IOWriter<? super T> elWriter,
                                                                                ToIntFunction<? super I> sizer )
    {
        MoreObjects.requireNonNull(elWriter, "elWriter", sizer, "sizer");
        return new IOWriter<I>() {

            @Override
            public void write( I iter, WritableByteChannel ch ) throws IOChannelWriteException
            {
                writeMultiple(iter, sizer, ch, elWriter);
            }

            @Override
            public void write( I iter, OutputStream out ) throws IOWriteException
            {
                writeMultiple(iter, sizer, out, elWriter);
            }

            @Override
            public void write( I iter, DataOutput out ) throws IOWriteException
            {
                writeMultiple(iter, sizer, out, elWriter);
            }
        };
    }

    public static <T, A, R> IOReader<R> multiReader( IOReader<T> elReader,
                                                     IntFunction<A> supplier,
                                                     BiConsumer<? super A, ? super T> accumulator,
                                                     Function<? super A, R> finisher )
    {
        MoreObjects.requireNonNull(
            elReader, "elReader",
            supplier, "supplier",
            accumulator, "accumulator",
            finisher, "finisher");
        return new IOReader<R>() {

            @Override
            public R read( ReadableByteChannel ch ) throws IOChannelReadException
            {
                return readMultiple(ch, elReader, supplier, accumulator, finisher);
            }

            @Override
            public R read( InputStream in ) throws IOReadException
            {
                return readMultiple(in, elReader, supplier, accumulator, finisher);
            }

            @Override
            public R read( DataInput in ) throws IOReadException
            {
                return readMultiple(in, elReader, supplier, accumulator, finisher);
            }
        };
    }

    public static <T, A, R> IOReader<R> multiReader( IOReader<T> elReader,
                                                     Supplier<A> supplier,
                                                     BiConsumer<? super A, ? super T> accumulator,
                                                     Function<? super A, R> finisher )
    {
        return multiReader(elReader, FunctionUtils.asIntFunction(supplier), accumulator, finisher);
    }

    public static <T, R> IOReader<R> multiReader( IOReader<T> elReader,
                                                  IntFunction<R> supplier,
                                                  BiConsumer<? super R, ? super T> accumulator )
    {
        return multiReader(elReader, supplier, accumulator, UnaryOperator.identity());
    }

    public static <T, R> IOReader<R> multiReader( IOReader<T> elReader,
                                                  Supplier<R> supplier,
                                                  BiConsumer<? super R, ? super T> accumulator )
    {
        return multiReader(elReader, FunctionUtils.asIntFunction(supplier), accumulator, UnaryOperator.identity());
    }

    public static <T, C extends Collection<T>> IOWriter<C> colWriter( IOWriter<? super T> elWriter )
    {
        return multiWriter(elWriter, Collection<T>::size);
    }

    public static <K, V> IOWriter<Map<K, V>> mapWriter( IOWriter<? super K> keyWriter, IOWriter<? super V> valueWriter )
    {
        IOWriter<Collection<Map.Entry<K, V>>> entriesWriter = colWriter(mapEntryWriter(keyWriter, valueWriter));
        return new IOWriter<Map<K, V>>() {

            @Override
            public void write( Map<K, V> map, WritableByteChannel ch ) throws IOChannelWriteException
            {
                entriesWriter.write(map.entrySet(), ch);
            }

            @Override
            public void write( Map<K, V> map, OutputStream out ) throws IOWriteException
            {
                entriesWriter.write(map.entrySet(), out);
            }

            @Override
            public void write( Map<K, V> map, DataOutput out ) throws IOWriteException
            {
                entriesWriter.write(map.entrySet(), out);
            }
        };
    }

    public static <K, V> IOWriter<Map.Entry<K, V>> mapEntryWriter( IOWriter<? super K> keyWriter,
                                                                   IOWriter<? super V> valueWriter )
    {
        MoreObjects.requireNonNull(keyWriter, "keyWriter", valueWriter, "valueWriter");
        return new IOWriter<Map.Entry<K, V>>() {

            @Override
            public void write( Map.Entry<K, V> entry, WritableByteChannel ch ) throws IOChannelWriteException
            {
                keyWriter.write(entry.getKey(), ch);
                valueWriter.write(entry.getValue(), ch);
            }

            @Override
            public void write( Map.Entry<K, V> entry, OutputStream out ) throws IOWriteException
            {
                keyWriter.write(entry.getKey(), out);
                valueWriter.write(entry.getValue(), out);
            }

            @Override
            public void write( Map.Entry<K, V> entry, DataOutput out ) throws IOWriteException
            {
                keyWriter.write(entry.getKey(), out);
                valueWriter.write(entry.getValue(), out);
            }
        };
    }

    public static <T, C extends Collection<T>> IOReader<C> colReader( IOReader<T> elReader, IntFunction<C> colFactory )
    {
        return multiReader(elReader, colFactory, Collection<T>::add);
    }

    public static <T, C extends Collection<T>> IOReader<C> colReader( IOReader<T> elReader, Supplier<C> colFactory )
    {
        return multiReader(elReader, colFactory, Collection<T>::add);
    }

    public static <T> IOReader<List<T>> listReader( IOReader<T> elReader )
    {
        return colReader(elReader, size -> new ArrayList<>(size));
    }

    public static <T> IOReader<Set<T>> setReader( IOReader<T> elReader )
    {
        return colReader(elReader, size -> Sets.newLinkedHashSetWithExpectedSize(size));
    }

    public static <K, V, M extends Map<K, V>> IOReader<M> mapReader( IOReader<K> keyReader,
                                                                     IOReader<V> valueReader,
                                                                     Supplier<M> mapFactory )
    {
        Objects.requireNonNull(mapFactory);
        IOReader<Map.Entry<K, V>> entryReader = mapEntryReader(keyReader, valueReader, SimpleEntry<K, V>::new);
        IOReader<List<Map.Entry<K, V>>> entriesReader = listReader(entryReader);
        return new IOReader<M>() {

            @Override
            public M read( ReadableByteChannel ch ) throws IOChannelReadException
            {
                M map = mapFactory.get();
                List<Map.Entry<K, V>> entries = entriesReader.read(ch);
                for (Map.Entry<K, V> e : entries) {
                    map.put(e.getKey(), e.getValue());
                }
                return map;
            }

            @Override
            public M read( InputStream in ) throws IOReadException
            {
                M map = mapFactory.get();
                List<Map.Entry<K, V>> entries = entriesReader.read(in);
                for (Map.Entry<K, V> e : entries) {
                    map.put(e.getKey(), e.getValue());
                }
                return map;
            }

            @Override
            public M read( DataInput in ) throws IOReadException
            {
                M map = mapFactory.get();
                List<Map.Entry<K, V>> entries = entriesReader.read(in);
                for (Map.Entry<K, V> e : entries) {
                    map.put(e.getKey(), e.getValue());
                }
                return map;
            }
        };
    }

    public static <K, V, E extends Map.Entry<K, V>> IOReader<E> mapEntryReader( IOReader<K> keyReader,
                                                                                IOReader<V> valueReader,
                                                                                BiFunction<K, V, E> entryFactory )
    {
        MoreObjects.requireNonNull(keyReader, "keyReader", valueReader, "valueReader", entryFactory, "entryFactory");
        return new IOReader<E>() {

            @Override
            public E read( ReadableByteChannel ch ) throws IOChannelReadException
            {
                K key = keyReader.read(ch);
                V value = valueReader.read(ch);
                return entryFactory.apply(key, value);
            }

            @Override
            public E read( InputStream in ) throws IOReadException
            {
                K key = keyReader.read(in);
                V value = valueReader.read(in);
                return entryFactory.apply(key, value);
            }

            @Override
            public E read( DataInput in ) throws IOReadException
            {
                K key = keyReader.read(in);
                V value = valueReader.read(in);
                return entryFactory.apply(key, value);
            }
        };
    }

    public static <T> IOReader<ImmutableList<T>> immuListReader( IOReader<T> elReader )
    {
        Supplier<ImmutableList.Builder<T>> supplier = ImmutableList::builder;
        BiConsumer<ImmutableList.Builder<T>, T> accumulator = ImmutableList.Builder::add;
        Function<ImmutableList.Builder<T>, ImmutableList<T>> finisher = ImmutableList.Builder::build;
        return multiReader(elReader, supplier, accumulator, finisher);
    }

    public static <T> IOReader<ImmutableSet<T>> immuSetReader( IOReader<T> elReader )
    {
        Supplier<ImmutableSet.Builder<T>> supplier = ImmutableSet::builder;
        BiConsumer<ImmutableSet.Builder<T>, T> accumulator = ImmutableSet.Builder::add;
        Function<ImmutableSet.Builder<T>, ImmutableSet<T>> finisher = ImmutableSet.Builder::build;
        return multiReader(elReader, supplier, accumulator, finisher);
    }

    public static <K, V> IOReader<ImmutableMap<K, V>> immuMapReader( IOReader<K> keyReader, IOReader<V> valueReader )
    {
        IOReader<List<Map.Entry<K, V>>> entriesReader = listReader(immuEntryReader(keyReader, valueReader));
        return new IOReader<ImmutableMap<K, V>>() {

            @Override
            public ImmutableMap<K, V> read( ReadableByteChannel ch ) throws IOChannelReadException
            {
                List<Map.Entry<K, V>> entries = entriesReader.read(ch);
                ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();
                for (Map.Entry<K, V> e : entries) {
                    builder.put(e);
                }
                return builder.build();
            }

            @Override
            public ImmutableMap<K, V> read( InputStream in ) throws IOReadException
            {
                List<Map.Entry<K, V>> entries = entriesReader.read(in);
                ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();
                for (Map.Entry<K, V> e : entries) {
                    builder.put(e);
                }
                return builder.build();
            }

            @Override
            public ImmutableMap<K, V> read( DataInput in ) throws IOReadException
            {
                List<Map.Entry<K, V>> entries = entriesReader.read(in);
                ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();
                for (Map.Entry<K, V> e : entries) {
                    builder.put(e);
                }
                return builder.build();
            }
        };
    }

    public static <K, V> IOReader<Map.Entry<K, V>> immuEntryReader( IOReader<K> keyReader, IOReader<V> valueReader )
    {
        return mapEntryReader(keyReader, valueReader, Maps::immutableEntry);
    }

    private static <T, I extends Iterable<? extends T>> void writeMultiple( I iter,
                                                                            ToIntFunction<? super I> sizer,
                                                                            WritableByteChannel ch,
                                                                            ChannelWriter<? super T> elWriter )
        throws IOChannelWriteException
    {
        int size = sizer.applyAsInt(iter);
        checkSizeForWriting(size);

        intWriter(ByteOrder.BIG_ENDIAN).writeInt(size, ch);
        for (T el : iter) {
            elWriter.write(el, ch);
        }
    }

    private static <T, I extends Iterable<? extends T>> void writeMultiple( I iter,
                                                                            ToIntFunction<? super I> sizer,
                                                                            OutputStream out,
                                                                            StreamWriter<? super T> elWriter )
        throws IOWriteException
    {
        int size = sizer.applyAsInt(iter);
        checkSizeForWriting(size);

        intWriter(ByteOrder.BIG_ENDIAN).writeInt(size, out);
        for (T el : iter) {
            elWriter.write(el, out);
        }
    }

    private static <T, I extends Iterable<? extends T>> void writeMultiple( I iter,
                                                                            ToIntFunction<? super I> sizer,
                                                                            DataOutput out,
                                                                            DataWriter<? super T> elWriter )
        throws IOWriteException
    {
        int size = sizer.applyAsInt(iter);
        checkSizeForWriting(size);

        intWriter(ByteOrder.BIG_ENDIAN).writeInt(size, out);
        for (T el : iter) {
            elWriter.write(el, out);
        }
    }

    private static <T, A, R> R readMultiple( ReadableByteChannel ch,
                                             ChannelReader<T> elReader,
                                             IntFunction<A> supplier,
                                             BiConsumer<? super A, ? super T> accumulator,
                                             Function<? super A, R> finisher )
        throws IOChannelReadException
    {
        int size = intReader(ByteOrder.BIG_ENDIAN).readInt(ch);
        validateSizeForReading(size, IOChannelReadException::new);

        A holder = supplier.apply(size);
        for (int i = 0; i < size; i++) {
            T el = elReader.read(ch);
            accumulator.accept(holder, el);
        }

        return finisher.apply(holder);
    }

    private static <T, A, R> R readMultiple( InputStream in,
                                             StreamReader<? extends T> elReader,
                                             IntFunction<A> supplier,
                                             BiConsumer<? super A, ? super T> accumulator,
                                             Function<? super A, R> finisher )
        throws IOReadException
    {
        int size = intReader(ByteOrder.BIG_ENDIAN).readInt(in);
        validateSizeForReading(size, IOReadException::new);

        A holder = supplier.apply(size);
        for (int i = 0; i < size; i++) {
            T el = elReader.read(in);
            accumulator.accept(holder, el);
        }

        return finisher.apply(holder);
    }

    private static <T, A, R> R readMultiple( DataInput in,
                                             DataReader<? extends T> elReader,
                                             IntFunction<A> supplier,
                                             BiConsumer<? super A, ? super T> accumulator,
                                             Function<? super A, R> finisher )
        throws IOReadException
    {
        int size = intReader(ByteOrder.BIG_ENDIAN).readInt(in);
        validateSizeForReading(size, IOReadException::new);

        A holder = supplier.apply(size);
        for (int i = 0; i < size; i++) {
            T el = elReader.read(in);
            accumulator.accept(holder, el);
        }

        return finisher.apply(holder);
    }

    private static void checkSizeForWriting( int size )
    {
        Preconditions.checkArgument(size >= 0, "expected non-negative size for writing multiple elements");
    }

    private static <X extends IOReadException> void validateSizeForReading( int size, Function<String, X> exFactory )
        throws X
    {
        if (size < 0)
            throw exFactory.apply("received negative size for reading multiple elements");
    }

    private Serializers()
    {
        // not used
    }
}
