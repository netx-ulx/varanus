package net.varanus.util.collect.builder;


import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;


/**
 * @param <E>
 * @param <C>
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public abstract class CollectionBuilder<E, C extends Collection<E>> implements BaseBuilder<C>
{
    private boolean nullsAllowed;

    public CollectionBuilder()
    {
        this.nullsAllowed = false;
    }

    public final CollectionBuilder<E, C> areNullsAllowed( boolean allowed )
    {
        this.nullsAllowed = allowed;
        return this;
    }

    private E checkNull( E element )
    {
        return nullsAllowed ? element : Objects.requireNonNull(element);
    }

    public final CollectionBuilder<E, C> add( E element )
    {
        _add(checkNull(element));
        return this;
    }

    @SafeVarargs
    public final CollectionBuilder<E, C> add( E... elements )
    {
        for (E e : elements) {
            add(e);
        }
        return this;
    }

    public final CollectionBuilder<E, C> addIf( boolean condition, E element )
    {
        if (condition)
            add(element);
        return this;
    }

    @SafeVarargs
    public final CollectionBuilder<E, C> addIf( boolean condition, E... elements )
    {
        if (condition)
            add(elements);
        return this;
    }

    public final CollectionBuilder<E, C> addIfPresent( Optional<? extends E> element )
    {
        element.ifPresent(this::add);
        return this;
    }

    public final CollectionBuilder<E, C> addAll( Iterable<? extends E> elements )
    {
        for (E e : elements)
            add(e);
        return this;
    }

    public final CollectionBuilder<E, C> addAllIf( boolean condition, Iterable<? extends E> elements )
    {
        if (condition)
            addAll(elements);
        return this;
    }

    public final CollectionBuilder<E, C> addEach( Stream<? extends E> elements )
    {
        elements.forEach(this::add);
        return this;
    }

    public final CollectionBuilder<E, C> addEachIf( Predicate<? super E> constraint, Iterable<? extends E> elements )
    {
        for (E e : elements) {
            addIf(constraint.test(e), e);
        }
        return this;
    }

    public final CollectionBuilder<E, C> clear()
    {
        _clear();
        return this;
    }

    @Override
    public abstract C build();

    protected abstract void _add( E element );

    protected abstract void _clear();
}
