package net.varanus.util.functional;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.function.LongSupplier;
import java.util.function.LongToDoubleFunction;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;
import java.util.function.Supplier;
import java.util.stream.LongStream;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.functional.functions.LongBiFunction;
import net.varanus.util.functional.functions.LongToDoubleBiFunction;
import net.varanus.util.functional.functions.LongToIntBiFunction;
import net.varanus.util.io.Serializers;
import net.varanus.util.io.exception.IOChannelReadException;
import net.varanus.util.io.exception.IOChannelWriteException;
import net.varanus.util.io.exception.IOReadException;
import net.varanus.util.io.exception.IOWriteException;
import net.varanus.util.io.serializer.IOReader;
import net.varanus.util.io.serializer.IOWriter;
import net.varanus.util.io.serializerlong.IOLongReader;
import net.varanus.util.io.serializerlong.IOLongWriter;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class PossibleLong implements BasePossible<PossibleLong>, Comparable<PossibleLong>
{
    private static final PossibleLong ABSENT = new PossibleLong(OptionalLong.empty());

    public static PossibleLong absent()
    {
        return ABSENT;
    }

    public static PossibleLong of( long value )
    {
        return ofOptional(OptionalLong.of(value));
    }

    public static PossibleLong ofNullable( @Nullable Long value )
    {
        return ofOptional(OptionalUtils.ofNullable(value));
    }

    public static PossibleLong ofOptional( OptionalLong optional )
    {
        return optional.isPresent() ? new PossibleLong(optional) : absent();
    }

    private final OptionalLong optional;

    private PossibleLong( OptionalLong optional )
    {
        this.optional = optional;
    }

    @Override
    public boolean isPresent()
    {
        return optional.isPresent();
    }

    @Override
    public PossibleLong ifAbsent( Runnable action )
    {
        if (!this.isPresent())
            action.run();
        return this;
    }

    @Override
    public PossibleLong ifPresent( Runnable action )
    {
        if (this.isPresent())
            action.run();
        return this;
    }

    public PossibleLong ifPresent( LongConsumer consumer )
    {
        optional.ifPresent(consumer);
        return this;
    }

    public long getAsLong() throws NoSuchElementException
    {
        return optional.getAsLong();
    }

    public PossibleLong or( PossibleLong other )
    {
        if (this.isPresent())
            return this;
        else
            return Objects.requireNonNull(other);
    }

    public PossibleLong or( Supplier<? extends PossibleLong> other )
    {
        if (this.isPresent())
            return this;
        else
            return Objects.requireNonNull(other.get());
    }

    public long orElse( long other )
    {
        return optional.orElse(other);
    }

    public long orElseGet( LongSupplier other )
    {
        return optional.orElseGet(other);
    }

    public <X extends Throwable> long orElseThrow( Supplier<? extends X> exceptionSupplier ) throws X
    {
        return optional.orElseThrow(exceptionSupplier);
    }

    public boolean contains( long value )
    {
        return OptionalUtils.contains(optional, value);
    }

    public boolean test( LongPredicate predicate )
    {
        return OptionalUtils.test(optional, predicate);
    }

    public PossibleLong filter( LongPredicate predicate )
    {
        Objects.requireNonNull(predicate);
        if (!this.isPresent())
            return this;
        else
            return predicate.test(this.getAsLong()) ? this : absent();
    }

    public PossibleLong map( LongUnaryOperator mapper )
    {
        OptionalLong mapped = OptionalUtils.identityPreservingMap(optional, mapper);
        return (optional == mapped) ? this : ofOptional(mapped);
    }

    public <T> Possible<T> mapToObj( LongFunction<T> mapper )
    {
        Objects.requireNonNull(mapper);
        if (!this.isPresent())
            return Possible.absent();
        else
            return Possible.ofNullable(mapper.apply(this.getAsLong()));
    }

    public PossibleInt mapToInt( LongToIntFunction mapper )
    {
        Objects.requireNonNull(mapper);
        if (!this.isPresent())
            return PossibleInt.absent();
        else
            return PossibleInt.of(mapper.applyAsInt(this.getAsLong()));
    }

    public PossibleDouble mapToDouble( LongToDoubleFunction mapper )
    {
        Objects.requireNonNull(mapper);
        if (!this.isPresent())
            return PossibleDouble.absent();
        else
            return PossibleDouble.of(mapper.applyAsDouble(this.getAsLong()));
    }

    public PossibleLong flatMap( LongFunction<? extends PossibleLong> mapper )
    {
        Objects.requireNonNull(mapper);
        if (!this.isPresent())
            return absent();
        else
            return Objects.requireNonNull(mapper.apply(this.getAsLong()));
    }

    public <T> Possible<T> flatMapToObj( LongFunction<? extends Possible<T>> mapper )
    {
        Objects.requireNonNull(mapper);
        if (!this.isPresent())
            return Possible.absent();
        else
            return Objects.requireNonNull(mapper.apply(this.getAsLong()));
    }

    public PossibleInt flatMapToInt( LongFunction<? extends PossibleInt> mapper )
    {
        Objects.requireNonNull(mapper);
        if (!this.isPresent())
            return PossibleInt.absent();
        else
            return Objects.requireNonNull(mapper.apply(this.getAsLong()));
    }

    public PossibleDouble flatMapToDouble( LongFunction<? extends PossibleDouble> mapper )
    {
        Objects.requireNonNull(mapper);
        if (!this.isPresent())
            return PossibleDouble.absent();
        else
            return Objects.requireNonNull(mapper.apply(this.getAsLong()));
    }

    public PossibleLong combine( PossibleLong other, LongBinaryOperator combiner )
    {
        Objects.requireNonNull(combiner);
        if (this.isPresent() && other.isPresent()) {
            long combined = combiner.applyAsLong(this.getAsLong(), other.getAsLong());
            if (this.contains(combined))
                return this;
            else
                return of(combined);
        }
        else {
            return PossibleLong.absent();
        }
    }

    public <T> Possible<T> combineToObj( PossibleLong other, LongBiFunction<? extends T> combiner )
    {
        Objects.requireNonNull(combiner);
        if (this.isPresent() && other.isPresent())
            return Possible.of(combiner.apply(this.getAsLong(), other.getAsLong()));
        else
            return Possible.absent();
    }

    public PossibleInt combineToInt( PossibleLong other, LongToIntBiFunction combiner )
    {
        Objects.requireNonNull(combiner);
        if (this.isPresent() && other.isPresent())
            return PossibleInt.of(combiner.applyAsInt(this.getAsLong(), other.getAsLong()));
        else
            return PossibleInt.absent();
    }

    public PossibleDouble combineToDouble( PossibleLong other, LongToDoubleBiFunction combiner )
    {
        Objects.requireNonNull(combiner);
        if (this.isPresent() && other.isPresent())
            return PossibleDouble.of(combiner.applyAsDouble(this.getAsLong(), other.getAsLong()));
        else
            return PossibleDouble.absent();
    }

    public OptionalLong asOptional()
    {
        return optional;
    }

    public LongStream asStream()
    {
        return this.isPresent() ? LongStream.of(this.getAsLong()) : LongStream.empty();
    }

    @Override
    public int compareTo( PossibleLong other )
    {
        return OptionalUtils.compare(this.optional, other.optional);
    }

    @Override
    public boolean equals( Object other )
    {
        return (other instanceof PossibleLong)
               && this.equals((PossibleLong)other);
    }

    public boolean equals( PossibleLong other )
    {
        return (other != null)
               && this.optional.equals(other.optional);
    }

    @Override
    public int hashCode()
    {
        return optional.hashCode();
    }

    @Override
    public String toString()
    {
        return toString(Long::toString, "absent");
    }

    public String toString( LongFunction<String> converter )
    {
        return toString(converter, "absent");
    }

    public String toString( String absentString )
    {
        return toString(Long::toString, absentString);
    }

    public String toString( LongFunction<String> converter, String absentString )
    {
        Objects.requireNonNull(converter, absentString);
        if (this.isPresent())
            return Objects.requireNonNull(converter.apply(this.getAsLong()));
        else
            return absentString;
    }

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class IO
    {
        public static IOWriter<PossibleLong> writer( ByteOrder order )
        {
            if (order.equals(ByteOrder.BIG_ENDIAN))
                return BIG_ENDIAN_WRITER;
            else if (order.equals(ByteOrder.LITTLE_ENDIAN))
                return LITTLE_ENDIAN_WRITER;
            else
                throw new AssertionError("unexpected byte order");
        }

        public static IOWriter<PossibleLong> writer( IOLongWriter valWriter )
        {
            Objects.requireNonNull(valWriter);
            return new IOWriter<PossibleLong>() {
                @Override
                public void write( PossibleLong val, WritableByteChannel ch ) throws IOChannelWriteException
                {
                    boolean isPresent = val.isPresent();
                    Serializers.boolWriter().write(isPresent, ch);
                    if (isPresent)
                        valWriter.writeLong(val.getAsLong(), ch);
                }

                @Override
                public void write( PossibleLong val, OutputStream out ) throws IOWriteException
                {
                    boolean isPresent = val.isPresent();
                    Serializers.boolWriter().write(isPresent, out);
                    if (isPresent)
                        valWriter.writeLong(val.getAsLong(), out);
                }

                @Override
                public void write( PossibleLong val, DataOutput out ) throws IOWriteException
                {
                    boolean isPresent = val.isPresent();
                    Serializers.boolWriter().write(isPresent, out);
                    if (isPresent)
                        valWriter.writeLong(val.getAsLong(), out);
                }
            };
        }

        public static IOReader<PossibleLong> reader( ByteOrder order )
        {
            if (order.equals(ByteOrder.BIG_ENDIAN))
                return BIG_ENDIAN_READER;
            else if (order.equals(ByteOrder.LITTLE_ENDIAN))
                return LITTLE_ENDIAN_READER;
            else
                throw new AssertionError("unexpected byte order");
        }

        public static IOReader<PossibleLong> reader( IOLongReader valReader )
        {
            Objects.requireNonNull(valReader);
            return new IOReader<PossibleLong>() {
                @Override
                public PossibleLong read( ReadableByteChannel ch ) throws IOChannelReadException
                {
                    boolean isPresent = Serializers.boolReader().read(ch);
                    if (isPresent)
                        return PossibleLong.of(valReader.readLong(ch));
                    else
                        return PossibleLong.absent();
                }

                @Override
                public PossibleLong read( InputStream in ) throws IOReadException
                {
                    boolean isPresent = Serializers.boolReader().read(in);
                    if (isPresent)
                        return PossibleLong.of(valReader.readLong(in));
                    else
                        return PossibleLong.absent();
                }

                @Override
                public PossibleLong read( DataInput in ) throws IOReadException
                {
                    boolean isPresent = Serializers.boolReader().read(in);
                    if (isPresent)
                        return PossibleLong.of(valReader.readLong(in));
                    else
                        return PossibleLong.absent();
                }
            };
        }

        private static final IOWriter<PossibleLong> BIG_ENDIAN_WRITER    =
            PossibleLong.IO.writer(Serializers.longWriter(ByteOrder.BIG_ENDIAN));
        private static final IOWriter<PossibleLong> LITTLE_ENDIAN_WRITER =
            PossibleLong.IO.writer(Serializers.longWriter(ByteOrder.LITTLE_ENDIAN));
        private static final IOReader<PossibleLong> BIG_ENDIAN_READER    =
            PossibleLong.IO.reader(Serializers.longReader(ByteOrder.BIG_ENDIAN));
        private static final IOReader<PossibleLong> LITTLE_ENDIAN_READER =
            PossibleLong.IO.reader(Serializers.longReader(ByteOrder.LITTLE_ENDIAN));

        private IO()
        {
            // not used
        }
    }
}
