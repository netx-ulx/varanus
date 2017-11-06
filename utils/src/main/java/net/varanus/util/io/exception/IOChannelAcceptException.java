package net.varanus.util.io.exception;


import java.io.IOException;
import java.nio.channels.ClosedByInterruptException;


/**
 * 
 */
public class IOChannelAcceptException extends IOBaseException
{
    private static final long serialVersionUID = 864443143057487982L;

    public IOChannelAcceptException( String message )
    {
        super(message, null);
    }

    public IOChannelAcceptException( IOException cause )
    {
        super(cause);
    }

    public IOChannelAcceptException( String message, IOException cause )
    {
        super(message, cause);
    }

    /**
     * If the cause of this exception is an instance of
     * {@code ClosedByInterruptException} and the current thread was previously
     * interrupted, then this method clears the interrupt status of the current
     * thread and throws an {@code InterruptedException}.
     * 
     * @throws InterruptedException
     *             If the cause of this exception is an instance of
     *             {@code ClosedByInterruptException} and the current thread was
     *             previously interrupted
     */
    public final void checkInterruptStatus() throws InterruptedException
    {
        if (this.getCause() instanceof ClosedByInterruptException && Thread.interrupted())
            throw new InterruptedException();
    }
}
