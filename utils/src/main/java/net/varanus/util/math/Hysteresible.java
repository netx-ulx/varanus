package net.varanus.util.math;


import java.util.Objects;

import javax.annotation.Nonnegative;
import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;


/**
 * @param <T>
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public abstract class Hysteresible<T>
{
    private final HysteresibleDouble hysteresible;
    private T                        value;

    protected Hysteresible( @Nonnegative double thresFactor )
    {
        this.hysteresible = HysteresibleDouble.empty(thresFactor);
        this.value = Objects.requireNonNull(_deconvert(hysteresible.value()));
    }

    protected Hysteresible( @Nonnegative double thresFactor, T initialValue )
    {
        this.hysteresible = HysteresibleDouble.of(thresFactor, _convert(initialValue));
        this.value = Objects.requireNonNull(_deconvert(hysteresible.value()));
    }

    public final T value()
    {
        return value;
    }

    public final boolean update( T newValue )
    {
        if (hysteresible.update(_convert(Objects.requireNonNull(newValue)))) {
            this.value = Objects.requireNonNull(_deconvert(hysteresible.value()));
            return true;
        }
        else {
            return false;
        }
    }

    public final void reset()
    {
        hysteresible.reset();
        this.value = Objects.requireNonNull(_deconvert(hysteresible.value()));
    }

    protected abstract double _convert( T newValue );

    protected abstract T _deconvert( double newDouble );
}
