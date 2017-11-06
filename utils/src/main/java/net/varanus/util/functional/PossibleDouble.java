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
import java.util.OptionalDouble;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.functional.functions.DoubleBiFunction;
import net.varanus.util.functional.functions.DoubleToIntBiFunction;
import net.varanus.util.functional.functions.DoubleToLongBiFunction;
import net.varanus.util.io.Serializers;
import net.varanus.util.io.exception.IOChannelReadException;
import net.varanus.util.io.exception.IOChannelWriteException;
import net.varanus.util.io.exception.IOReadException;
import net.varanus.util.io.exception.IOWriteException;
import net.varanus.util.io.serializer.IOReader;
import net.varanus.util.io.serializer.IOWriter;
import net.varanus.util.io.serializerdouble.IODoubleReader;
import net.varanus.util.io.serializerdouble.IODoubleWriter;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class PossibleDouble implements BasePossible<PossibleDouble>, Comparable<PossibleDouble>
{
    private static final PossibleDouble ABSENT = new PossibleDouble(OptionalDouble.empty());

    public static PossibleDouble absent()
    {
        return ABSENT;
    }

    public static PossibleDouble of( double value )
    {
        return ofOptional(OptionalDouble.of(value));
    }

    public static PossibleDouble ofNullable( @Nullable Double value )
    {
        return ofOptional(OptionalUtils.ofNullable(value));
    }

    public static PossibleDouble ofOptional( OptionalDouble optional )
    {
        return optional.isPresent() ? new PossibleDouble(optional) : absent();
    }

    private final OptionalDouble optional;

    private PossibleDouble( OptionalDouble optional )
    {
        this.optional = optional;
    }

    @Override
    public boolean isPresent()
    {
        return optional.isPresent();
    }

    @Override
    public PossibleDouble ifAbsent( Runnable action )
    {
        if (!this.isPresent())
            action.run();
        return this;
    }

    @Override
    public PossibleDouble ifPresent( Runnable action )
    {
        if (this.isPresent())
            action.run();
        return this;
    }

    public PossibleDouble ifPresent( DoubleConsumer consumer )
    {
        optional.ifPresent(consumer);
        return this;
    }

    public double getAsDouble() throws NoSuchElementException
    {
        return optional.getAsDouble();
    }

    public PossibleDouble or( PossibleDouble other )
    {
        if (this.isPresent())
            return this;
        else
            return Objects.requireNonNull(other);
    }

    public PossibleDouble or( Supplier<? extends PossibleDouble> other )
    {
        if (this.isPresent())
            return this;
        else
            return Objects.requireNonNull(other.get());
    }

    public double orElse( double other )
    {
        return optional.orElse(other);
    }

    public double orElseGet( DoubleSupplier other )
    {
        return optional.orElseGet(other);
    }

    public <X extends Throwable> double orElseThrow( Supplier<? extends X> exceptionSupplier ) throws X
    {
        return optional.orElseThrow(exceptionSupplier);
    }

    public boolean contains( double value )
    {
        return OptionalUtils.contains(optional, value);
    }

    public boolean test( DoublePredicate predicate )
    {
        return OptionalUtils.test(optional, predicate);
    }

    public PossibleDouble filter( DoublePredicate predicate )
    {
        Objects.requireNonNull(predicate);
        if (!this.isPresent())
            return this;
        else
            return predicate.test(this.getAsDouble()) ? this : absent();
    }

    public PossibleDouble map( DoubleUnaryOperator mapper )
    {
        OptionalDouble mapped = OptionalUtils.identityPreservingMap(optional, mapper);
        return (optional == mapped) ? this : ofOptional(mapped);
    }

    public <T> Possible<T> mapToObj( DoubleFunction<T> mapper )
    {
        Objects.requireNonNull(mapper);
        if (!this.isPresent())
            return Possible.absent();
        else
            return Possible.ofNullable(mapper.apply(this.getAsDouble()));
    }

    public PossibleInt mapToInt( DoubleToIntFunction mapper )
    {
        Objects.requireNonNull(mapper);
        if (!this.isPresent())
            return PossibleInt.absent();
        else
            return PossibleInt.of(mapper.applyAsInt(this.getAsDouble()));
    }

    public PossibleLong mapToLong( DoubleToLongFunction mapper )
    {
        Objects.requireNonNull(mapper);
        if (!this.isPresent())
            return PossibleLong.absent();
        else
            return PossibleLong.of(mapper.applyAsLong(this.getAsDouble()));
    }

    public PossibleDouble flatMap( DoubleFunction<? extends PossibleDouble> mapper )
    {
        Objects.requireNonNull(mapper);
        if (!this.isPresent())
            return absent();
        else
            return Objects.requireNonNull(mapper.apply(this.getAsDouble()));
    }

    public <T> Possible<T> flatMapToObj( DoubleFunction<? extends Possible<T>> mapper )
    {
        Objects.requireNonNull(mapper);
        if (!this.isPresent())
            return Possible.absent();
        else
            return Objects.requireNonNull(mapper.apply(this.getAsDouble()));
    }

    public PossibleInt flatMapToInt( DoubleFunction<? extends PossibleInt> mapper )
    {
        Objects.requireNonNull(mapper);
        if (!this.isPresent())
            return PossibleInt.absent();
        else
            return Objects.requireNonNull(mapper.apply(this.getAsDouble()));
    }

    public PossibleLong flatMapToDouble( DoubleFunction<? extends PossibleLong> mapper )
    {
        Objects.requireNonNull(mapper);
        if (!this.isPresent())
            return PossibleLong.absent();
        else
            return Objects.requireNonNull(mapper.apply(this.getAsDouble()));
    }

    public PossibleDouble combine( PossibleDouble other, DoubleBinaryOperator combiner )
    {
        Objects.requireNonNull(combiner);
        if (this.isPresent() && other.isPresent()) {
            double combined = combiner.applyAsDouble(this.getAsDouble(), other.getAsDouble());
            if (this.contains(combined))
                return this;
            else
                return of(combined);
        }
        else {
            return PossibleDouble.absent();
        }
    }

    public <T> Possible<T> combineToObj( PossibleDouble other, DoubleBiFunction<? extends T> combiner )
    {
        Objects.requireNonNull(combiner);
        if (this.isPresent() && other.isPresent())
            return Possible.of(combiner.apply(this.getAsDouble(), other.getAsDouble()));
        else
            return Possible.absent();
    }

    public PossibleInt combineToInt( PossibleDouble other, DoubleToIntBiFunction combiner )
    {
        Objects.requireNonNull(combiner);
        if (this.isPresent() && other.isPresent())
            return PossibleInt.of(combiner.applyAsInt(this.getAsDouble(), other.getAsDouble()));
        else
            return PossibleInt.absent();
    }

    public PossibleLong combineToLong( PossibleDouble other, DoubleToLongBiFunction combiner )
    {
        Objects.requireNonNull(combiner);
        if (this.isPresent() && other.isPresent())
            return PossibleLong.of(combiner.applyAsLong(this.getAsDouble(), other.getAsDouble()));
        else
            return PossibleLong.absent();
    }

    public OptionalDouble asOptional()
    {
        return optional;
    }

    public DoubleStream asStream()
    {
        return this.isPresent() ? DoubleStream.of(this.getAsDouble()) : DoubleStream.empty();
    }

    @Override
    public int compareTo( PossibleDouble other )
    {
        return OptionalUtils.compare(this.optional, other.optional);
    }

    @Override
    public boolean equals( Object other )
    {
        return (other instanceof PossibleDouble)
               && this.equals((PossibleDouble)other);
    }

    public boolean equals( PossibleDouble other )
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
        return toString(Double::toString, "absent");
    }

    public String toString( DoubleFunction<String> converter )
    {
        return toString(converter, "absent");
    }

    public String toString( String absentString )
    {
        return toString(Double::toString, absentString);
    }

    public String toString( DoubleFunction<String> converter, String absentString )
    {
        Objects.requireNonNull(converter, absentString);
        if (this.isPresent())
            return Objects.requireNonNull(converter.apply(this.getAsDouble()));
        else
            return absentString;
    }

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class IO
    {
        public static IOWriter<PossibleDouble> writer( ByteOrder order )
        {
            if (order.equals(ByteOrder.BIG_ENDIAN))
                return BIG_ENDIAN_WRITER;
            else if (order.equals(ByteOrder.LITTLE_ENDIAN))
                return LITTLE_ENDIAN_WRITER;
            else
                throw new AssertionError("unexpected byte order");
        }

        public static IOWriter<PossibleDouble> writer( IODoubleWriter valWriter )
        {
            Objects.requireNonNull(valWriter);
            return new IOWriter<PossibleDouble>() {
                @Override
                public void write( PossibleDouble val, WritableByteChannel ch ) throws IOChannelWriteException
                {
                    boolean isPresent = val.isPresent();
                    Serializers.boolWriter().write(isPresent, ch);
                    if (isPresent)
                        valWriter.writeDouble(val.getAsDouble(), ch);
                }

                @Override
                public void write( PossibleDouble val, OutputStream out ) throws IOWriteException
                {
                    boolean isPresent = val.isPresent();
                    Serializers.boolWriter().write(isPresent, out);
                    if (isPresent)
                        valWriter.writeDouble(val.getAsDouble(), out);
                }

                @Override
                public void write( PossibleDouble val, DataOutput out ) throws IOWriteException
                {
                    boolean isPresent = val.isPresent();
                    Serializers.boolWriter().write(isPresent, out);
                    if (isPresent)
                        valWriter.writeDouble(val.getAsDouble(), out);
                }
            };
        }

        public static IOReader<PossibleDouble> reader( ByteOrder order )
        {
            if (order.equals(ByteOrder.BIG_ENDIAN))
                return BIG_ENDIAN_READER;
            else if (order.equals(ByteOrder.LITTLE_ENDIAN))
                return LITTLE_ENDIAN_READER;
            else
                throw new AssertionError("unexpected byte order");
        }

        public static IOReader<PossibleDouble> reader( IODoubleReader valReader )
        {
            Objects.requireNonNull(valReader);
            return new IOReader<PossibleDouble>() {
                @Override
                public PossibleDouble read( ReadableByteChannel ch ) throws IOChannelReadException
                {
                    boolean isPresent = Serializers.boolReader().read(ch);
                    if (isPresent)
                        return PossibleDouble.of(valReader.readDouble(ch));
                    else
                        return PossibleDouble.absent();
                }

                @Override
                public PossibleDouble read( InputStream in ) throws IOReadException
                {
                    boolean isPresent = Serializers.boolReader().read(in);
                    if (isPresent)
                        return PossibleDouble.of(valReader.readDouble(in));
                    else
                        return PossibleDouble.absent();
                }

                @Override
                public PossibleDouble read( DataInput in ) throws IOReadException
                {
                    boolean isPresent = Serializers.boolReader().read(in);
                    if (isPresent)
                        return PossibleDouble.of(valReader.readDouble(in));
                    else
                        return PossibleDouble.absent();
                }
            };
        }

        private static final IOWriter<PossibleDouble> BIG_ENDIAN_WRITER    =
            PossibleDouble.IO.writer(Serializers.doubleWriter(ByteOrder.BIG_ENDIAN));
        private static final IOWriter<PossibleDouble> LITTLE_ENDIAN_WRITER =
            PossibleDouble.IO.writer(Serializers.doubleWriter(ByteOrder.LITTLE_ENDIAN));
        private static final IOReader<PossibleDouble> BIG_ENDIAN_READER    =
            PossibleDouble.IO.reader(Serializers.doubleReader(ByteOrder.BIG_ENDIAN));
        private static final IOReader<PossibleDouble> LITTLE_ENDIAN_READER =
            PossibleDouble.IO.reader(Serializers.doubleReader(ByteOrder.LITTLE_ENDIAN));

        private IO()
        {
            // not used
        }
    }
}
