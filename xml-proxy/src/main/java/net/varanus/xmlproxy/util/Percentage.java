package net.varanus.xmlproxy.util;


import java.util.function.IntConsumer;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import com.google.common.base.Preconditions;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.functional.BasePossible;
import net.varanus.util.functional.PossibleInt;


@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class Percentage implements BasePossible<Percentage>
{
    private static final Percentage ABSENT = new Percentage(PossibleInt.absent());

    public static Percentage of( int percent ) throws IllegalArgumentException
    {
        Preconditions.checkArgument(0 <= percent && percent <= 100,
            "percentage value must be between 0 and 100");
        return new Percentage(PossibleInt.of(percent));
    }

    public static Percentage ofPossible( PossibleInt percent ) throws IllegalArgumentException
    {
        return percent.isPresent() ? of(percent.getAsInt()) : absent();
    }

    public static Percentage absent()
    {
        return ABSENT;
    }

    private final PossibleInt percent;

    private Percentage( PossibleInt percent )
    {
        this.percent = percent;
    }

    @Override
    public boolean isPresent()
    {
        return percent.isPresent();
    }

    @Override
    public Percentage ifAbsent( Runnable action )
    {
        percent.ifAbsent(action);
        return this;
    }

    @Override
    public Percentage ifPresent( Runnable action )
    {
        percent.ifPresent(action);
        return this;
    }

    public Percentage ifPresent( IntConsumer consumer )
    {
        percent.ifPresent(consumer);
        return this;
    }

    public int getAsInt()
    {
        return percent.getAsInt();
    }
}
