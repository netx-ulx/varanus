package net.varanus.util.functional;


import java.io.DataInput;
import java.io.DataOutput;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Collections;
import java.util.Comparator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongBiFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Stream;

import javax.annotation.Nullable;
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
 *            The type of the present value
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class Possible<T> implements BasePossible<Possible<T>>
{
    private static final Possible<?> ABSENT = new Possible<>(Optional.empty());

    public static <T> Possible<T> absent()
    {
        return safeCast(ABSENT);
    }

    public static <T> Possible<T> of( T value )
    {
        return ofOptional(Optional.of(value));
    }

    public static <T> Possible<T> ofNullable( @Nullable T value )
    {
        return ofOptional(Optional.ofNullable(value));
    }

    public static <T> Possible<T> ofOptional( Optional<T> optional )
    {
        return optional.isPresent() ? new Possible<>(optional) : absent();
    }

    public static <T> Comparator<Possible<T>> comparingBy( Comparator<? super T> comparator )
    {
        Objects.requireNonNull(comparator);
        return ( p1, p2 ) -> OptionalUtils.compare(p1.optional, p2.optional, comparator);
    }

    public static <T extends Comparable<? super T>> Comparator<Possible<T>> comparing()
    {
        return ( p1, p2 ) -> OptionalUtils.compare(p1.optional, p2.optional);
    }

    private final Optional<T> optional;

    private Possible( Optional<T> optional )
    {
        this.optional = optional;
    }

    @Override
    public boolean isPresent()
    {
        return optional.isPresent();
    }

    @Override
    public Possible<T> ifAbsent( Runnable action )
    {
        if (!this.isPresent())
            action.run();
        return this;
    }

    @Override
    public Possible<T> ifPresent( Runnable action )
    {
        if (this.isPresent())
            action.run();
        return this;
    }

    public Possible<T> ifPresent( Consumer<? super T> consumer )
    {
        optional.ifPresent(consumer);
        return this;
    }

    public T get() throws NoSuchElementException
    {
        return optional.get();
    }

    public Possible<T> or( Possible<T> other )
    {
        if (this.isPresent())
            return this;
        else
            return Objects.requireNonNull(other);
    }

    public Possible<T> or( Supplier<? extends Possible<T>> other )
    {
        if (this.isPresent())
            return this;
        else
            return Objects.requireNonNull(other.get());
    }

    public @Nullable T orElse( @Nullable T other )
    {
        return optional.orElse(other);
    }

    public @Nullable T orElseGet( Supplier<? extends T> other )
    {
        return optional.orElseGet(other);
    }

    public <X extends Throwable> T orElseThrow( Supplier<? extends X> exceptionSupplier ) throws X
    {
        return optional.orElseThrow(exceptionSupplier);
    }

    public boolean contains( @Nullable Object value )
    {
        return OptionalUtils.contains(optional, value);
    }

    public <U> boolean contains( @Nullable U value, BiPredicate<T, U> predicate )
    {
        return OptionalUtils.contains(optional, value, predicate);
    }

    public boolean test( Predicate<? super T> predicate )
    {
        return OptionalUtils.test(optional, predicate);
    }

    public boolean testNullable( Predicate<? super T> predicate )
    {
        return OptionalUtils.testNullable(optional, predicate);
    }

    public Possible<T> filter( Predicate<? super T> predicate )
    {
        Objects.requireNonNull(predicate);
        if (!this.isPresent())
            return this;
        else
            return predicate.test(this.get()) ? this : absent();
    }

    public <U> Possible<U> map( Function<? super T, ? extends U> mapper )
    {
        Optional<U> mapped = OptionalUtils.identityPreservingMap(optional, mapper);
        return (optional == mapped) ? safeCast(this) : ofOptional(mapped);
    }

    public PossibleInt mapToInt( ToIntFunction<? super T> mapper )
    {
        Objects.requireNonNull(mapper);
        if (!this.isPresent())
            return PossibleInt.absent();
        else
            return PossibleInt.of(mapper.applyAsInt(this.get()));
    }

    public PossibleLong mapToLong( ToLongFunction<? super T> mapper )
    {
        Objects.requireNonNull(mapper);
        if (!this.isPresent())
            return PossibleLong.absent();
        else
            return PossibleLong.of(mapper.applyAsLong(this.get()));
    }

    public PossibleDouble mapToDouble( ToDoubleFunction<? super T> mapper )
    {
        Objects.requireNonNull(mapper);
        if (!this.isPresent())
            return PossibleDouble.absent();
        else
            return PossibleDouble.of(mapper.applyAsDouble(this.get()));
    }

    public <U> Possible<U> flatMap( Function<? super T, ? extends Possible<U>> mapper )
    {
        Objects.requireNonNull(mapper);
        if (!this.isPresent())
            return absent();
        else
            return Objects.requireNonNull(mapper.apply(this.get()));
    }

    public PossibleInt flatMapToInt( Function<? super T, ? extends PossibleInt> mapper )
    {
        Objects.requireNonNull(mapper);
        if (!this.isPresent())
            return PossibleInt.absent();
        else
            return Objects.requireNonNull(mapper.apply(this.get()));
    }

    public PossibleLong flatMapToLong( Function<? super T, ? extends PossibleLong> mapper )
    {
        Objects.requireNonNull(mapper);
        if (!this.isPresent())
            return PossibleLong.absent();
        else
            return Objects.requireNonNull(mapper.apply(this.get()));
    }

    public PossibleDouble flatMapToDouble( Function<? super T, ? extends PossibleDouble> mapper )
    {
        Objects.requireNonNull(mapper);
        if (!this.isPresent())
            return PossibleDouble.absent();
        else
            return Objects.requireNonNull(mapper.apply(this.get()));
    }

    public <U, R> Possible<R> combine( Possible<U> other, BiFunction<? super T, ? super U, ? extends R> combiner )
    {
        Objects.requireNonNull(combiner);
        if (this.isPresent() && other.isPresent()) {
            R combined = combiner.apply(this.get(), other.get());
            if (this.contains(combined, ( a, b ) -> a == b))
                return safeCast(this);
            else
                return ofNullable(combined);
        }
        else {
            return Possible.absent();
        }
    }

    public <T2> PossibleInt combineToInt( Possible<T2> other, ToIntBiFunction<? super T, ? super T2> combiner )
    {
        Objects.requireNonNull(combiner);
        if (this.isPresent() && other.isPresent())
            return PossibleInt.of(combiner.applyAsInt(this.get(), other.get()));
        else
            return PossibleInt.absent();
    }

    public <T2> PossibleLong combineToLong( Possible<T2> other, ToLongBiFunction<? super T, ? super T2> combiner )
    {
        Objects.requireNonNull(combiner);
        if (this.isPresent() && other.isPresent())
            return PossibleLong.of(combiner.applyAsLong(this.get(), other.get()));
        else
            return PossibleLong.absent();
    }

    public <T2> PossibleDouble combineToDouble( Possible<T2> other, ToDoubleBiFunction<? super T, ? super T2> combiner )
    {
        Objects.requireNonNull(combiner);
        if (this.isPresent() && other.isPresent())
            return PossibleDouble.of(combiner.applyAsDouble(this.get(), other.get()));
        else
            return PossibleDouble.absent();
    }

    public Optional<T> asOptional()
    {
        return optional;
    }

    public Stream<T> asStream()
    {
        return this.isPresent() ? Stream.of(this.get()) : Stream.empty();
    }

    public Set<T> asSet()
    {
        return this.isPresent() ? Collections.singleton(this.get()) : Collections.emptySet();
    }

    @Override
    public boolean equals( Object other )
    {
        return (other instanceof Possible<?>)
               && this.equals((Possible<?>)other);
    }

    public boolean equals( Possible<?> other )
    {
        return (other != null)
               && this.optional.equals(other.optional);
    }

    public <U> boolean equals( Possible<U> other, BiPredicate<T, U> predicate )
    {
        return (other != null)
               && OptionalUtils.areEqual(this.optional, other.optional, predicate);
    }

    @Override
    public int hashCode()
    {
        return optional.hashCode();
    }

    @Override
    public String toString()
    {
        return toString(T::toString, "absent");
    }

    public String toString( Function<? super T, String> converter )
    {
        return toString(converter, "absent");
    }

    public String toString( String absentString )
    {
        return toString(T::toString, absentString);
    }

    public String toString( Function<? super T, String> converter, String absentString )
    {
        Objects.requireNonNull(converter, absentString);
        if (this.isPresent())
            return Objects.requireNonNull(converter.apply(this.get()));
        else
            return absentString;
    }

    @SuppressWarnings( "unchecked" )
    private static <U> Possible<U> safeCast( Possible<?> possible )
    {
        return (Possible<U>)possible;
    }

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class IO
    {
        public static <T> IOWriter<Possible<T>> writer( IOWriter<T> valWriter )
        {
            Objects.requireNonNull(valWriter);
            return new IOWriter<Possible<T>>() {
                @Override
                public void write( Possible<T> val, WritableByteChannel ch ) throws IOChannelWriteException
                {
                    boolean isPresent = val.isPresent();
                    Serializers.boolWriter().write(isPresent, ch);
                    if (isPresent)
                        valWriter.write(val.get(), ch);
                }

                @Override
                public void write( Possible<T> val, OutputStream out ) throws IOWriteException
                {
                    boolean isPresent = val.isPresent();
                    Serializers.boolWriter().write(isPresent, out);
                    if (isPresent)
                        valWriter.write(val.get(), out);
                }

                @Override
                public void write( Possible<T> val, DataOutput out ) throws IOWriteException
                {
                    boolean isPresent = val.isPresent();
                    Serializers.boolWriter().write(isPresent, out);
                    if (isPresent)
                        valWriter.write(val.get(), out);
                }
            };
        }

        public static <T> IOReader<Possible<T>> reader( IOReader<T> valReader )
        {
            Objects.requireNonNull(valReader);
            return new IOReader<Possible<T>>() {
                @Override
                public Possible<T> read( ReadableByteChannel ch ) throws IOChannelReadException
                {
                    boolean isPresent = Serializers.boolReader().read(ch);
                    if (isPresent)
                        return Possible.of(valReader.read(ch));
                    else
                        return Possible.absent();
                }

                @Override
                public Possible<T> read( InputStream in ) throws IOReadException
                {
                    boolean isPresent = Serializers.boolReader().read(in);
                    if (isPresent)
                        return Possible.of(valReader.read(in));
                    else
                        return Possible.absent();
                }

                @Override
                public Possible<T> read( DataInput in ) throws IOReadException
                {
                    boolean isPresent = Serializers.boolReader().read(in);
                    if (isPresent)
                        return Possible.of(valReader.read(in));
                    else
                        return Possible.absent();
                }
            };
        }

        private IO()
        {
            // not used
        }
    }
}
