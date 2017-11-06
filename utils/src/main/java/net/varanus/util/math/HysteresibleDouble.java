package net.varanus.util.math;


import javax.annotation.Nonnegative;

import com.google.common.base.Preconditions;

import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;


/**
 * 
 */
@ReturnValuesAreNonnullByDefault
public final class HysteresibleDouble
{
    public static final double DEFAULT_THRESHOLD_FACTOR = 0.05;

    public static HysteresibleDouble of( @Nonnegative double thresFactor, double initialValue )
    {
        Preconditions.checkArgument(Double.isFinite(thresFactor), "threshold factor must be finite");
        Preconditions.checkArgument(thresFactor >= 0, "threshold factor must be non-negative");
        return new HysteresibleDouble(thresFactor, initialValue);
    }

    public static HysteresibleDouble empty( @Nonnegative double thresFactor )
    {
        return of(thresFactor, Double.NaN);
    }

    private final @Nonnegative /* finite */ double thresFactor;
    private double                                 value;

    private HysteresibleDouble( @Nonnegative double thresFactor, double value )
    {
        this.thresFactor = thresFactor;
        this.value = value;
    }

    public double value()
    {
        return this.value;
    }

    public boolean update( double newValue )
    {
        double oldValue = value();
        if (Double.isFinite(oldValue) && Double.isFinite(newValue)) {
            // common case where both values are finite
            double lowerThres = oldValue - (oldValue * thresFactor);
            double upperThres = oldValue + (oldValue * thresFactor);
            if (newValue < lowerThres || upperThres < newValue) {
                // update if new is outside threshold region around old
                _set(newValue);
                return true;
            }
            else {
                // do not update if new is inside threshold region around old
                return false;
            }
        }
        else {
            // at least one of the values is an infinity or NaN
            return updateNonFinite(oldValue, newValue);
        }
    }

    private boolean updateNonFinite( double oldValue, double newValue )
    {
        if (Double.isNaN(oldValue) || Double.isNaN(newValue)) {
            // at least one of the values is NaN
            if (!(Double.isNaN(oldValue) && Double.isNaN(newValue))) {
                // (NaN -> not NaN) always updates
                // (not NaN -> NaN) always updates
                _set(newValue);
                return true;
            }
            else {
                // (NaN -> NaN) never updates
                return false;
            }
        }
        else {
            // at least one of the values is an infinity
            if (oldValue != newValue) {
                // (finite -> infinity) always updates
                // (infinity -> finite) always updates
                // (-Inf -> +Inf) always updates
                // (+Inf -> -Inf) always updates
                _set(newValue);
                return true;
            }
            else {
                // (-Inf -> -Inf) never updates
                // (+Inf -> +Inf) never updates
                return false;
            }
        }
    }

    public void reset()
    {
        _set(Double.NaN);
    }

    private void _set( double newValue )
    {
        this.value = newValue;
    }
}
