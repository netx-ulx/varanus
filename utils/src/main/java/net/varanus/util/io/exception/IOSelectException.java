package net.varanus.util.io.exception;


import java.io.IOException;


/**
 * 
 */
public class IOSelectException extends IOBaseException
{
    private static final long serialVersionUID = 1097313934855604935L;

    public IOSelectException( String message )
    {
        super(message, null);
    }

    public IOSelectException( IOException cause )
    {
        super(cause);
    }

    public IOSelectException( String message, IOException cause )
    {
        super(message, cause);
    }
}
