package net.varanus.util.concurrent;


import java.util.concurrent.atomic.AtomicInteger;

import net.varanus.util.math.ExtraMath;


/**
 * A class similar to {@link AtomicInteger} in which incrementing/decrementing
 * the integer value is done in a cyclic fashion.
 */
public class AtomicCyclicInteger extends Number
{
    private static final long serialVersionUID = 6542739059611115634L;

    private final int           maxVal;
    private final AtomicInteger ai;

    /**
     * Constructs a new {@code AtomicCyclicInteger} with the provided maximum
     * value.
     * 
     * @param maxVal
     *            The maximum value
     * @exception IllegalArgumentException
     *                If {@code (maxVal < 0)}
     */
    public AtomicCyclicInteger( int maxVal )
    {
        this.maxVal = validMaxValue(maxVal);
        this.ai = new AtomicInteger(0);
    }

    /**
     * Constructs a new {@code AtomicCyclicInteger} with the provided initial
     * and maximum values.
     * 
     * @param initialVal
     *            The initial value
     * @param maxVal
     *            The maximum value
     * @exception IllegalArgumentException
     *                If {@code (maxVal < 0)} or {@code (initialVal < 0)} or
     *                {@code (initialVal > maxVal)}
     */
    public AtomicCyclicInteger( int initialVal, int maxVal )
    {
        this.maxVal = validMaxValue(maxVal);
        this.ai = new AtomicInteger(validInitialValue(initialVal));
    }

    /**
     * Returns the maximum value.
     * 
     * @return the maximum value
     */
    public int maxValue()
    {
        return maxVal;
    }

    /**
     * Gets the current value.
     *
     * @return the current value
     */
    public final int get()
    {
        return ai.get();
    }

    /**
     * Returns the result of calling {@link #addAndGet(int) addAndGet(delta)}
     * but without changing the current value.
     * 
     * @param delta
     *            the value to add
     * @return the resulting value
     */
    public final int getWithDelta( int delta )
    {
        return addDelta(get(), delta);
    }

    /**
     * Sets to the given value.
     *
     * @param newValue
     *            the new value
     * @exception IllegalArgumentException
     *                If {@code (newValue < 0)} or
     *                {@code (newValue > maxValue())}
     */
    public final void set( int newValue )
    {
        ai.set(validNewValue(newValue));
    }

    /**
     * Eventually sets to the given value.
     *
     * @param newValue
     *            the new value
     * @exception IllegalArgumentException
     *                If {@code (newValue < 0)} or
     *                {@code (newValue > maxValue())}
     */
    public final void lazySet( int newValue )
    {
        ai.lazySet(validNewValue(newValue));
    }

    /**
     * Atomically sets to the given value and returns the old value.
     *
     * @param newValue
     *            the new value
     * @return the previous value
     * @exception IllegalArgumentException
     *                If {@code (newValue < 0)} or
     *                {@code (newValue > maxValue())}
     */
    public final int getAndSet( int newValue )
    {
        return ai.getAndSet(validNewValue(newValue));
    }

    /**
     * Atomically sets the value to the given updated value if the current value
     * {@code ==} the expected value.
     *
     * @param expect
     *            the expected value
     * @param update
     *            the new value
     * @return true if successful. False return indicates that the actual value
     *         was not equal to the expected value.
     * @exception IllegalArgumentException
     *                If {@code (update < 0)} or {@code (update > maxValue())}
     */
    public final boolean compareAndSet( int expect, int update )
    {
        return ai.compareAndSet(expect, validUpdate(update));
    }

    /**
     * Atomically sets the value to the given updated value if the current value
     * {@code ==} the expected value.
     * <p>
     * May fail spuriously and does not provide ordering guarantees, so is only
     * rarely an appropriate alternative to {@code compareAndSet}.
     *
     * @param expect
     *            the expected value
     * @param update
     *            the new value
     * @return true if successful.
     */
    public final boolean weakCompareAndSet( int expect, int update )
    {
        return ai.weakCompareAndSet(expect, validUpdate(update));
    }

    /**
     * Atomically increments by one the current value, except if the current
     * value is equal to the {@linkplain #maxValue() maximum value}, then it is
     * set to 0 instead.
     *
     * @return the previous value
     */
    public final int getAndIncrement()
    {
        for (;;) {
            int current = ai.get();
            int next = inc(current);
            if (ai.compareAndSet(current, next)) {
                return current;
            }
        }
    }

    /**
     * Atomically decrements by one the current value, except if the current
     * value is equal to zero, then it is set to the {@linkplain #maxValue()
     * maximum value} instead.
     *
     * @return the previous value
     */
    public final int getAndDecrement()
    {
        for (;;) {
            int current = ai.get();
            int next = dec(current);
            if (ai.compareAndSet(current, next)) {
                return current;
            }
        }
    }

    /**
     * Atomically adds the given value to the current value, using modular
     * arithmetic to keep the result between zero and the
     * {@linkplain #maxValue() maximum value}.
     *
     * @param delta
     *            the value to add
     * @return the previous value
     */
    public final int getAndAdd( int delta )
    {
        for (;;) {
            int current = get();
            int next = addDelta(current, delta);
            if (ai.compareAndSet(current, next)) {
                return current;
            }
        }
    }

    /**
     * Atomically increments by one the current value, except if the current
     * value is equal to the {@linkplain #maxValue() maximum value}, then it is
     * set to 0 instead.
     *
     * @return the updated value
     */
    public final int incrementAndGet()
    {
        for (;;) {
            int current = ai.get();
            int next = inc(current);
            if (ai.compareAndSet(current, next)) {
                return next;
            }
        }
    }

    /**
     * Atomically decrements by one the current value, except if the current
     * value is equal to zero, then it is set to the {@linkplain #maxValue()
     * maximum value} instead.
     *
     * @return the updated value
     */
    public final int decrementAndGet()
    {
        for (;;) {
            int current = ai.get();
            int next = dec(current);
            if (ai.compareAndSet(current, next)) {
                return next;
            }
        }
    }

    /**
     * Atomically adds the given value to the current value, using modular
     * arithmetic to keep the result between zero and the
     * {@linkplain #maxValue() maximum value}.
     *
     * @param delta
     *            the value to add
     * @return the updated value
     */
    public final int addAndGet( int delta )
    {
        for (;;) {
            int current = get();
            int next = addDelta(current, delta);
            if (ai.compareAndSet(current, next)) {
                return next;
            }
        }
    }

    @Override
    public String toString()
    {
        return ai.toString();
    }

    @Override
    public int intValue()
    {
        return ai.intValue();
    }

    @Override
    public long longValue()
    {
        return ai.longValue();
    }

    @Override
    public float floatValue()
    {
        return ai.floatValue();
    }

    @Override
    public double doubleValue()
    {
        return ai.doubleValue();
    }

    // ================ AUXILIARY FUNCTIONS ================ //

    // requires 0 <= value <= maxVal
    private int inc( int value )
    {
        return value < maxVal ? (value + 1) : 0;
    }

    // requires 0 <= value <= maxVal
    private int dec( int value )
    {
        return value > 0 ? (value - 1) : maxVal;
    }

    // requires 0 <= value <= maxVal
    private int addDelta( int value, int delta )
    {
        return ExtraMath.positiveMod(value + delta, maxVal + 1);
    }

    private int validInitialValue( int initialVal )
    {
        return validValue(initialVal, "initial value");
    }

    private int validNewValue( int newVal )
    {
        return validValue(newVal, "new value");
    }

    private int validUpdate( int update )
    {
        return validValue(update, "update");
    }

    private int validValue( int val, String valType )
    {
        if (val < 0) {
            throw new IllegalArgumentException(
                String.format("%s must not be negative", valType));
        }
        else if (val > maxVal) {
            throw new IllegalArgumentException(
                String.format("%s must not exceed the maximum value", valType));
        }
        else {
            return val;
        }
    }

    private static int validMaxValue( int maxVal )
    {
        if (maxVal < 0) {
            throw new IllegalArgumentException("maximum value must not be negative");
        }
        else {
            return maxVal;
        }
    }
}
