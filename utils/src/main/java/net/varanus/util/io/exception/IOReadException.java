package net.varanus.util.io.exception;


import java.io.IOException;


/**
 * 
 */
public class IOReadException extends IOBaseException
{
    private static final long serialVersionUID = 1872943497841558825L;

    public IOReadException( String message )
    {
        super(message, null);
    }

    public IOReadException( IOException cause )
    {
        super(cause);
    }

    public IOReadException( String message, IOException cause )
    {
        super(message, cause);
    }
}
