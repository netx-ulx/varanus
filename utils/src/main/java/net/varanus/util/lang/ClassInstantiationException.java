package net.varanus.util.lang;


import java.util.Objects;


/**
 * 
 */
public class ClassInstantiationException extends Exception
{
    private static final long serialVersionUID = -6169586513981381566L;

    public ClassInstantiationException( Throwable cause )
    {
        super(Objects.requireNonNull(cause));
    }
}
