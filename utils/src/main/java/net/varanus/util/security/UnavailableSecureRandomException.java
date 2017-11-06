package net.varanus.util.security;

/**
 * 
 */
public final class UnavailableSecureRandomException extends Exception
{
    private static final long serialVersionUID = -7639438770515328083L;

    public UnavailableSecureRandomException()
    {
        super("no SecureRandom is available");
    }
}
