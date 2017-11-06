package net.varanus.util.lang;


import java.util.Objects;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;


/**
 * 
 */
@ParametersAreNonnullByDefault
public final class MoreObjects
{
    public static <T> T getRuntimeNull()
    {
        if (MoreObjects.class.equals(MoreObjects.class))
            return null;
        else
            throw new AssertionError("should never happen");
    }

    public static void requireNonNull( Object obj1,
                                       @Nullable String msg1,
                                       Object obj2,
                                       @Nullable String msg2 )
    {
        Objects.requireNonNull(obj1, msg1);
        Objects.requireNonNull(obj2, msg2);
    }

    public static void requireNonNull( Object obj1,
                                       @Nullable String msg1,
                                       Object obj2,
                                       @Nullable String msg2,
                                       Object obj3,
                                       @Nullable String msg3 )
    {
        Objects.requireNonNull(obj1, msg1);
        Objects.requireNonNull(obj2, msg2);
        Objects.requireNonNull(obj3, msg3);
    }

    public static void requireNonNull( Object obj1,
                                       @Nullable String msg1,
                                       Object obj2,
                                       @Nullable String msg2,
                                       Object obj3,
                                       @Nullable String msg3,
                                       Object obj4,
                                       @Nullable String msg4 )
    {
        Objects.requireNonNull(obj1, msg1);
        Objects.requireNonNull(obj2, msg2);
        Objects.requireNonNull(obj3, msg3);
        Objects.requireNonNull(obj4, msg4);
    }

    public static void requireNonNull( Object obj1,
                                       @Nullable String msg1,
                                       Object obj2,
                                       @Nullable String msg2,
                                       Object obj3,
                                       @Nullable String msg3,
                                       Object obj4,
                                       @Nullable String msg4,
                                       Object obj5,
                                       @Nullable String msg5 )
    {
        Objects.requireNonNull(obj1, msg1);
        Objects.requireNonNull(obj2, msg2);
        Objects.requireNonNull(obj3, msg3);
        Objects.requireNonNull(obj4, msg4);
        Objects.requireNonNull(obj5, msg5);
    }

    public static void requireNonNull( Object obj1,
                                       @Nullable String msg1,
                                       Object obj2,
                                       @Nullable String msg2,
                                       Object obj3,
                                       @Nullable String msg3,
                                       Object obj4,
                                       @Nullable String msg4,
                                       Object obj5,
                                       @Nullable String msg5,
                                       Object obj6,
                                       @Nullable String msg6 )
    {
        Objects.requireNonNull(obj1, msg1);
        Objects.requireNonNull(obj2, msg2);
        Objects.requireNonNull(obj3, msg3);
        Objects.requireNonNull(obj4, msg4);
        Objects.requireNonNull(obj5, msg5);
        Objects.requireNonNull(obj6, msg6);
    }

    public static void requireNonNull( Object obj1,
                                       @Nullable String msg1,
                                       Object obj2,
                                       @Nullable String msg2,
                                       Object obj3,
                                       @Nullable String msg3,
                                       Object obj4,
                                       @Nullable String msg4,
                                       Object obj5,
                                       @Nullable String msg5,
                                       Object obj6,
                                       @Nullable String msg6,
                                       Object obj7,
                                       @Nullable String msg7 )
    {
        Objects.requireNonNull(obj1, msg1);
        Objects.requireNonNull(obj2, msg2);
        Objects.requireNonNull(obj3, msg3);
        Objects.requireNonNull(obj4, msg4);
        Objects.requireNonNull(obj5, msg5);
        Objects.requireNonNull(obj6, msg6);
        Objects.requireNonNull(obj7, msg7);
    }

    public static void requireNonNull( Object obj1,
                                       @Nullable String msg1,
                                       Object obj2,
                                       @Nullable String msg2,
                                       Object obj3,
                                       @Nullable String msg3,
                                       Object obj4,
                                       @Nullable String msg4,
                                       Object obj5,
                                       @Nullable String msg5,
                                       Object obj6,
                                       @Nullable String msg6,
                                       Object obj7,
                                       @Nullable String msg7,
                                       Object obj8,
                                       @Nullable String msg8 )
    {
        Objects.requireNonNull(obj1, msg1);
        Objects.requireNonNull(obj2, msg2);
        Objects.requireNonNull(obj3, msg3);
        Objects.requireNonNull(obj4, msg4);
        Objects.requireNonNull(obj5, msg5);
        Objects.requireNonNull(obj6, msg6);
        Objects.requireNonNull(obj7, msg7);
        Objects.requireNonNull(obj8, msg8);
    }

    public static void requireNonNull( Object obj1,
                                       @Nullable String msg1,
                                       Object obj2,
                                       @Nullable String msg2,
                                       Object obj3,
                                       @Nullable String msg3,
                                       Object obj4,
                                       @Nullable String msg4,
                                       Object obj5,
                                       @Nullable String msg5,
                                       Object obj6,
                                       @Nullable String msg6,
                                       Object obj7,
                                       @Nullable String msg7,
                                       Object obj8,
                                       @Nullable String msg8,
                                       Object obj9,
                                       @Nullable String msg9 )
    {
        Objects.requireNonNull(obj1, msg1);
        Objects.requireNonNull(obj2, msg2);
        Objects.requireNonNull(obj3, msg3);
        Objects.requireNonNull(obj4, msg4);
        Objects.requireNonNull(obj5, msg5);
        Objects.requireNonNull(obj6, msg6);
        Objects.requireNonNull(obj7, msg7);
        Objects.requireNonNull(obj8, msg8);
        Objects.requireNonNull(obj9, msg9);
    }

    public static void requireNonNull( Object obj1,
                                       @Nullable String msg1,
                                       Object obj2,
                                       @Nullable String msg2,
                                       Object obj3,
                                       @Nullable String msg3,
                                       Object obj4,
                                       @Nullable String msg4,
                                       Object obj5,
                                       @Nullable String msg5,
                                       Object obj6,
                                       @Nullable String msg6,
                                       Object obj7,
                                       @Nullable String msg7,
                                       Object obj8,
                                       @Nullable String msg8,
                                       Object obj9,
                                       @Nullable String msg9,
                                       Object obj10,
                                       @Nullable String msg10 )
    {
        Objects.requireNonNull(obj1, msg1);
        Objects.requireNonNull(obj2, msg2);
        Objects.requireNonNull(obj3, msg3);
        Objects.requireNonNull(obj4, msg4);
        Objects.requireNonNull(obj5, msg5);
        Objects.requireNonNull(obj6, msg6);
        Objects.requireNonNull(obj7, msg7);
        Objects.requireNonNull(obj8, msg8);
        Objects.requireNonNull(obj9, msg9);
        Objects.requireNonNull(obj10, msg10);
    }

    private MoreObjects()
    {
        // not used
    }
}
