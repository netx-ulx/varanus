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
import java.util.OptionalInt;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.functional.functions.IntBiFunction;
import net.varanus.util.functional.functions.IntToDoubleBiFunction;
import net.varanus.util.functional.functions.IntToLongBiFunction;
import net.varanus.util.io.Serializers;
import net.varanus.util.io.exception.IOChannelReadException;
import net.varanus.util.io.exception.IOChannelWriteException;
import net.varanus.util.io.exception.IOReadException;
import net.varanus.util.io.exception.IOWriteException;
import net.varanus.util.io.serializer.IOReader;
import net.varanus.util.io.serializer.IOWriter;
import net.varanus.util.io.serializerint.IOIntReader;
import net.varanus.util.io.serializerint.IOIntWriter;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class PossibleInt implements BasePossible<PossibleInt>, Comparable<PossibleInt>
{
    private static final PossibleInt ABSENT = new PossibleInt(OptionalInt.empty());

    public static PossibleInt absent()
    {
        return ABSENT;
    }

    public static PossibleInt of( int value )
    {
        return ofOptional(OptionalInt.of(value));
    }

    public static PossibleInt ofNullable( @Nullable Integer value )
    {
        return ofOptional(OptionalUtils.ofNullable(value));
    }

    public static PossibleInt ofOptional( OptionalInt optional )
    {
        return optional.isPresent() ? new PossibleInt(optional) : absent();
    }

    private final OptionalInt optional;

    private PossibleInt( OptionalInt optional )
    {
        this.optional = optional;
    }

    @Override
    public boolean isPresent()
    {
        return optional.isPresent();
    }

    @Override
    public PossibleInt ifAbsent( Runnable action )
    {
        if (!this.isPresent())
            action.run();
        return this;
    }

    @Override
    public PossibleInt ifPresent( Runnable action )
    {
        if (this.isPresent())
            action.run();
        return this;
    }

    public PossibleInt ifPresent( IntConsumer consumer )
    {
        optional.ifPresent(consumer);
        return this;
    }

    public int getAsInt() throws NoSuchElementException
    {
        return optional.getAsInt();
    }

    public PossibleInt or( PossibleInt other )
    {
        if (this.isPresent())
            return this;
        else
            return Objects.requireNonNull(other);
    }

    public PossibleInt or( Supplier<? extends PossibleInt> other )
    {
        if (this.isPresent())
            return this;
        else
            return Objects.requireNonNull(other.get());
    }

    public int orElse( int other )
    {
        return optional.orElse(other);
    }

    public int orElseGet( IntSupplier other )
    {
        return optional.orElseGet(other);
    }

    public <X extends Throwable> int orElseThrow( Supplier<? extends X> exceptionSupplier ) throws X
    {
        return optional.orElseThrow(exceptionSupplier);
    }

    public boolean contains( int value )
    {
        return OptionalUtils.contains(optional, value);
    }

    public boolean test( IntPredicate predicate )
    {
        return OptionalUtils.test(optional, predicate);
    }

    public PossibleInt filter( IntPredicate predicate )
    {
        Objects.requireNonNull(predicate);
        if (!this.isPresent())
            return this;
        else
            return predicate.test(this.getAsInt()) ? this : absent();
    }

    public PossibleInt map( IntUnaryOperator mapper )
    {
        OptionalInt mapped = OptionalUtils.identityPreservingMap(optional, mapper);
        return (optional == mapped) ? this : ofOptional(mapped);
    }

    public <T> Possible<T> mapToObj( IntFunction<T> mapper )
    {
        Objects.requireNonNull(mapper);
        if (!this.isPresent())
            return Possible.absent();
        else
            return Possible.ofNullable(mapper.apply(this.getAsInt()));
    }

    public PossibleLong mapToLong( IntToLongFunction mapper )
    {
        Objects.requireNonNull(mapper);
        if (!this.isPresent())
            return PossibleLong.absent();
        else
            return PossibleLong.of(mapper.applyAsLong(this.getAsInt()));
    }

    public PossibleDouble mapToDouble( IntToDoubleFunction mapper )
    {
        Objects.requireNonNull(mapper);
        if (!this.isPresent())
            return PossibleDouble.absent();
        else
            return PossibleDouble.of(mapper.applyAsDouble(this.getAsInt()));
    }

    public PossibleInt flatMap( IntFunction<? extends PossibleInt> mapper )
    {
        Objects.requireNonNull(mapper);
        if (!this.isPresent())
            return absent();
        else
            return Objects.requireNonNull(mapper.apply(this.getAsInt()));
    }

    public <T> Possible<T> flatMapToObj( IntFunction<? extends Possible<T>> mapper )
    {
        Objects.requireNonNull(mapper);
        if (!this.isPresent())
            return Possible.absent();
        else
            return Objects.requireNonNull(mapper.apply(this.getAsInt()));
    }

    public PossibleLong flatMapToLong( IntFunction<? extends PossibleLong> mapper )
    {
        Objects.requireNonNull(mapper);
        if (!this.isPresent())
            return PossibleLong.absent();
        else
            return Objects.requireNonNull(mapper.apply(this.getAsInt()));
    }

    public PossibleDouble flatMapToDouble( IntFunction<? extends PossibleDouble> mapper )
    {
        Objects.requireNonNull(mapper);
        if (!this.isPresent())
            return PossibleDouble.absent();
        else
            return Objects.requireNonNull(mapper.apply(this.getAsInt()));
    }

    public PossibleInt combine( PossibleInt other, IntBinaryOperator combiner )
    {
        Objects.requireNonNull(combiner);
        if (this.isPresent() && other.isPresent()) {
            int combined = combiner.applyAsInt(this.getAsInt(), other.getAsInt());
            if (this.contains(combined))
                return this;
            else
                return of(combined);
        }
        else {
            return PossibleInt.absent();
        }
    }

    public <T> Possible<T> combineToObj( PossibleInt other, IntBiFunction<? extends T> combiner )
    {
        Objects.requireNonNull(combiner);
        if (this.isPresent() && other.isPresent())
            return Possible.of(combiner.apply(this.getAsInt(), other.getAsInt()));
        else
            return Possible.absent();
    }

    public PossibleLong combineToLong( PossibleInt other, IntToLongBiFunction combiner )
    {
        Objects.requireNonNull(combiner);
        if (this.isPresent() && other.isPresent())
            return PossibleLong.of(combiner.applyAsLong(this.getAsInt(), other.getAsInt()));
        else
            return PossibleLong.absent();
    }

    public PossibleDouble combineToDouble( PossibleInt other, IntToDoubleBiFunction combiner )
    {
        Objects.requireNonNull(combiner);
        if (this.isPresent() && other.isPresent())
            return PossibleDouble.of(combiner.applyAsDouble(this.getAsInt(), other.getAsInt()));
        else
            return PossibleDouble.absent();
    }

    public OptionalInt asOptional()
    {
        return optional;
    }

    public IntStream asStream()
    {
        return this.isPresent() ? IntStream.of(this.getAsInt()) : IntStream.empty();
    }

    @Override
    public int compareTo( PossibleInt other )
    {
        return OptionalUtils.compare(this.optional, other.optional);
    }

    @Override
    public boolean equals( Object other )
    {
        return (other instanceof PossibleInt)
               && this.equals((PossibleInt)other);
    }

    public boolean equals( PossibleInt other )
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
        return toString(Integer::toString, "absent");
    }

    public String toString( IntFunction<String> converter )
    {
        return toString(converter, "absent");
    }

    public String toString( String absentString )
    {
        return toString(Integer::toString, absentString);
    }

    public String toString( IntFunction<String> converter, String absentString )
    {
        Objects.requireNonNull(converter, absentString);
        if (this.isPresent())
            return Objects.requireNonNull(converter.apply(this.getAsInt()));
        else
            return absentString;
    }

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class IO
    {
        public static IOWriter<PossibleInt> writer( ByteOrder order )
        {
            if (order.equals(ByteOrder.BIG_ENDIAN))
                return BIG_ENDIAN_WRITER;
            else if (order.equals(ByteOrder.LITTLE_ENDIAN))
                return LITTLE_ENDIAN_WRITER;
            else
                throw new AssertionError("unexpected byte order");
        }

        public static IOWriter<PossibleInt> writer( IOIntWriter valWriter )
        {
            Objects.requireNonNull(valWriter);
            return new IOWriter<PossibleInt>() {
                @Override
                public void write( PossibleInt val, WritableByteChannel ch ) throws IOChannelWriteException
                {
                    boolean isPresent = val.isPresent();
                    Serializers.boolWriter().write(isPresent, ch);
                    if (isPresent)
                        valWriter.writeInt(val.getAsInt(), ch);
                }

                @Override
                public void write( PossibleInt val, OutputStream out ) throws IOWriteException
                {
                    boolean isPresent = val.isPresent();
                    Serializers.boolWriter().write(isPresent, out);
                    if (isPresent)
                        valWriter.writeInt(val.getAsInt(), out);
                }

                @Override
                public void write( PossibleInt val, DataOutput out ) throws IOWriteException
                {
                    boolean isPresent = val.isPresent();
                    Serializers.boolWriter().write(isPresent, out);
                    if (isPresent)
                        valWriter.writeInt(val.getAsInt(), out);
                }
            };
        }

        public static IOReader<PossibleInt> reader( ByteOrder order )
        {
            if (order.equals(ByteOrder.BIG_ENDIAN))
                return BIG_ENDIAN_READER;
            else if (order.equals(ByteOrder.LITTLE_ENDIAN))
                return LITTLE_ENDIAN_READER;
            else
                throw new AssertionError("unexpected byte order");
        }

        public static IOReader<PossibleInt> reader( IOIntReader valReader )
        {
            Objects.requireNonNull(valReader);
            return new IOReader<PossibleInt>() {
                @Override
                public PossibleInt read( ReadableByteChannel ch ) throws IOChannelReadException
                {
                    boolean isPresent = Serializers.boolReader().read(ch);
                    if (isPresent)
                        return PossibleInt.of(valReader.readInt(ch));
                    else
                        return PossibleInt.absent();
                }

                @Override
                public PossibleInt read( InputStream in ) throws IOReadException
                {
                    boolean isPresent = Serializers.boolReader().read(in);
                    if (isPresent)
                        return PossibleInt.of(valReader.readInt(in));
                    else
                        return PossibleInt.absent();
                }

                @Override
                public PossibleInt read( DataInput in ) throws IOReadException
                {
                    boolean isPresent = Serializers.boolReader().read(in);
                    if (isPresent)
                        return PossibleInt.of(valReader.readInt(in));
                    else
                        return PossibleInt.absent();
                }
            };
        }

        private static final IOWriter<PossibleInt> BIG_ENDIAN_WRITER    =
            PossibleInt.IO.writer(Serializers.intWriter(ByteOrder.BIG_ENDIAN));
        private static final IOWriter<PossibleInt> LITTLE_ENDIAN_WRITER =
            PossibleInt.IO.writer(Serializers.intWriter(ByteOrder.LITTLE_ENDIAN));
        private static final IOReader<PossibleInt> BIG_ENDIAN_READER    =
            PossibleInt.IO.reader(Serializers.intReader(ByteOrder.BIG_ENDIAN));
        private static final IOReader<PossibleInt> LITTLE_ENDIAN_READER =
            PossibleInt.IO.reader(Serializers.intReader(ByteOrder.LITTLE_ENDIAN));

        private IO()
        {
            // not used
        }
    }
}
