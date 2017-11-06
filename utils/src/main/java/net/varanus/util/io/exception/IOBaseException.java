package net.varanus.util.io.exception;


import java.io.IOException;

import javax.annotation.OverridingMethodsMustInvokeSuper;


/**
 * 
 */
class IOBaseException extends Exception
{
    private static final long serialVersionUID = 2286078184897334383L;

    IOBaseException( String message )
    {
        super(message, null);
    }

    IOBaseException( IOException cause )
    {
        super(cause);
    }

    IOBaseException( String message, IOException cause )
    {
        super(message, cause);
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public synchronized IOException getCause()
    {
        return (IOException)super.getCause();
    }
}
