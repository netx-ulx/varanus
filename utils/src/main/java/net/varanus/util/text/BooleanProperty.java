package net.varanus.util.text;


import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;


/**
 * 
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class BooleanProperty extends BaseProperty<Boolean>
{
    public static BooleanProperty of( String propKey )
    {
        return new BooleanProperty(propKey);
    }

    public static BooleanProperty of( String propKey, boolean defaultValue )
    {
        return new BooleanProperty(propKey, defaultValue);
    }

    private BooleanProperty( String propKey )
    {
        super(propKey);
    }

    private BooleanProperty( String propKey, boolean defaultValue )
    {
        super(propKey, defaultValue);
    }

    public boolean readBoolean( Map<String, String> props ) throws IllegalArgumentException
    {
        return readBoolean(asPropMapper(props));
    }

    public boolean readBoolean( Properties props ) throws IllegalArgumentException
    {
        return readBoolean(asPropMapper(props));
    }

    public boolean readBoolean( Function<String, String> propMapper ) throws IllegalArgumentException
    {
        return readProperty(propMapper, StringUtils::convertToBoolean);
    }
}
