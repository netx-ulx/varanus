package net.varanus.util.io.exception;


import java.io.IOException;


/**
 * 
 */
public class IOWriteException extends IOBaseException
{
    private static final long serialVersionUID = -962326315507368952L;

    public IOWriteException( String message )
    {
        super(message);
    }

    public IOWriteException( IOException cause )
    {
        super(cause);
    }

    public IOWriteException( String message, IOException cause )
    {
        super(message, cause);
    }
}
