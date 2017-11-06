package net.varanus.util.lang;

/**
 * 
 */
public final class Unsigned
{
    public static final int  MAX_BYTE   = (1 << Byte.SIZE) - 1;
    public static final int  MAX_SHORT  = (1 << Short.SIZE) - 1;
    public static final int  MAX_MEDIUM = (1 << (3 * Byte.SIZE)) - 1;
    public static final long MAX_INT    = (1L << Integer.SIZE) - 1;

    public static int byteValue( byte value )
    {
        return value & MAX_BYTE;
    }

    public static int shortValue( short value )
    {
        return value & MAX_SHORT;
    }

    public static int mediumValue( int value )
    {
        return value & MAX_MEDIUM;
    }

    public static long intValue( long value )
    {
        return value & MAX_INT;
    }

    private Unsigned()
    {
        // not used
    }
}
