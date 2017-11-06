package net.varanus.util.logging;


import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.Marker;


/**
 * 
 */
public abstract class AbstractLogger
{
    public static final AbstractLogger of( Logger logger, LogLevel logLevel )
    {
        switch (logLevel) {
            case INFO:
                return new InfoLogger(logger);

            case WARN:
                return new WarnLogger(logger);

            case ERROR:
                return new ErrorLogger(logger);

            case DEBUG:
                return new DebugLogger(logger);

            case TRACE:
                return new TraceLogger(logger);

            case OFF:
                return new OffLogger(logger);

            default:
                throw new AssertionError("unknown enum type");
        }
    }

    protected final Logger logger;

    AbstractLogger( Logger logger )
    {
        this.logger = Objects.requireNonNull(logger);
    }

    public abstract void log( String arg0 );

    public abstract void log( Marker arg0, String arg1 );

    public abstract void log( String arg0, Object arg1 );

    public abstract void log( String arg0, Object[] arg1 );

    public abstract void log( String arg0, Throwable arg1 );

    public abstract void log( Marker arg0, String arg1, Object arg2 );

    public abstract void log( Marker arg0, String arg1, Object[] arg2 );

    public abstract void log( Marker arg0, String arg1, Throwable arg2 );

    public abstract void log( String arg0, Object arg1, Object arg2 );

    public abstract void log( Marker arg0, String arg1, Object arg2, Object arg3 );

    public final String getName()
    {
        return logger.getName();
    }

    @Override
    public final boolean equals( Object other )
    {
        return (other instanceof AbstractLogger) && this.equals((AbstractLogger)other);
    }

    public final boolean equals( AbstractLogger other )
    {
        return other != null
               && this.logger.equals(other.logger);
    }

    @Override
    public final int hashCode()
    {
        return logger.hashCode();
    }

    @Override
    public final String toString()
    {
        return logger.toString();
    }

    private static final class InfoLogger extends AbstractLogger
    {
        InfoLogger( Logger logger )
        {
            super(logger);
        }

        @Override
        public void log( String arg0 )
        {
            logger.info(arg0);
        }

        @Override
        public void log( Marker arg0, String arg1 )
        {
            logger.info(arg0, arg1);
        }

        @Override
        public void log( String arg0, Object arg1 )
        {
            logger.info(arg0, arg1);
        }

        @Override
        public void log( String arg0, Object[] arg1 )
        {
            logger.info(arg0, arg1);
        }

        @Override
        public void log( String arg0, Throwable arg1 )
        {
            logger.info(arg0, arg1);
        }

        @Override
        public void log( Marker arg0, String arg1, Object arg2 )
        {
            logger.info(arg0, arg1, arg2);
        }

        @Override
        public void log( Marker arg0, String arg1, Object[] arg2 )
        {
            logger.info(arg0, arg1, arg2);
        }

        @Override
        public void log( Marker arg0, String arg1, Throwable arg2 )
        {
            logger.info(arg0, arg1, arg2);
        }

        @Override
        public void log( String arg0, Object arg1, Object arg2 )
        {
            logger.info(arg0, arg1, arg2);
        }

        @Override
        public void log( Marker arg0, String arg1, Object arg2, Object arg3 )
        {
            logger.info(arg0, arg1, arg2, arg3);
        }
    }

    private static final class WarnLogger extends AbstractLogger
    {
        WarnLogger( Logger logger )
        {
            super(logger);
        }

        @Override
        public void log( String arg0 )
        {
            logger.warn(arg0);
        }

        @Override
        public void log( Marker arg0, String arg1 )
        {
            logger.warn(arg0, arg1);
        }

        @Override
        public void log( String arg0, Object arg1 )
        {
            logger.warn(arg0, arg1);
        }

        @Override
        public void log( String arg0, Object[] arg1 )
        {
            logger.warn(arg0, arg1);
        }

        @Override
        public void log( String arg0, Throwable arg1 )
        {
            logger.warn(arg0, arg1);
        }

        @Override
        public void log( Marker arg0, String arg1, Object arg2 )
        {
            logger.warn(arg0, arg1, arg2);
        }

        @Override
        public void log( Marker arg0, String arg1, Object[] arg2 )
        {
            logger.warn(arg0, arg1, arg2);
        }

        @Override
        public void log( Marker arg0, String arg1, Throwable arg2 )
        {
            logger.warn(arg0, arg1, arg2);
        }

        @Override
        public void log( String arg0, Object arg1, Object arg2 )
        {
            logger.warn(arg0, arg1, arg2);
        }

        @Override
        public void log( Marker arg0, String arg1, Object arg2, Object arg3 )
        {
            logger.warn(arg0, arg1, arg2, arg3);
        }
    }

    private static final class ErrorLogger extends AbstractLogger
    {
        ErrorLogger( Logger logger )
        {
            super(logger);
        }

        @Override
        public void log( String arg0 )
        {
            logger.error(arg0);
        }

        @Override
        public void log( Marker arg0, String arg1 )
        {
            logger.error(arg0, arg1);
        }

        @Override
        public void log( String arg0, Object arg1 )
        {
            logger.error(arg0, arg1);
        }

        @Override
        public void log( String arg0, Object[] arg1 )
        {
            logger.error(arg0, arg1);
        }

        @Override
        public void log( String arg0, Throwable arg1 )
        {
            logger.error(arg0, arg1);
        }

        @Override
        public void log( Marker arg0, String arg1, Object arg2 )
        {
            logger.error(arg0, arg1, arg2);
        }

        @Override
        public void log( Marker arg0, String arg1, Object[] arg2 )
        {
            logger.error(arg0, arg1, arg2);
        }

        @Override
        public void log( Marker arg0, String arg1, Throwable arg2 )
        {
            logger.error(arg0, arg1, arg2);
        }

        @Override
        public void log( String arg0, Object arg1, Object arg2 )
        {
            logger.error(arg0, arg1, arg2);
        }

        @Override
        public void log( Marker arg0, String arg1, Object arg2, Object arg3 )
        {
            logger.error(arg0, arg1, arg2, arg3);
        }
    }

    private static final class DebugLogger extends AbstractLogger
    {
        DebugLogger( Logger logger )
        {
            super(logger);
        }

        @Override
        public void log( String arg0 )
        {
            logger.debug(arg0);
        }

        @Override
        public void log( Marker arg0, String arg1 )
        {
            logger.debug(arg0, arg1);
        }

        @Override
        public void log( String arg0, Object arg1 )
        {
            logger.debug(arg0, arg1);
        }

        @Override
        public void log( String arg0, Object[] arg1 )
        {
            logger.debug(arg0, arg1);
        }

        @Override
        public void log( String arg0, Throwable arg1 )
        {
            logger.debug(arg0, arg1);
        }

        @Override
        public void log( Marker arg0, String arg1, Object arg2 )
        {
            logger.debug(arg0, arg1, arg2);
        }

        @Override
        public void log( Marker arg0, String arg1, Object[] arg2 )
        {
            logger.debug(arg0, arg1, arg2);
        }

        @Override
        public void log( Marker arg0, String arg1, Throwable arg2 )
        {
            logger.debug(arg0, arg1, arg2);
        }

        @Override
        public void log( String arg0, Object arg1, Object arg2 )
        {
            logger.debug(arg0, arg1, arg2);
        }

        @Override
        public void log( Marker arg0, String arg1, Object arg2, Object arg3 )
        {
            logger.debug(arg0, arg1, arg2, arg3);
        }
    }

    private static final class TraceLogger extends AbstractLogger
    {
        TraceLogger( Logger logger )
        {
            super(logger);
        }

        @Override
        public void log( String arg0 )
        {
            logger.trace(arg0);
        }

        @Override
        public void log( Marker arg0, String arg1 )
        {
            logger.trace(arg0, arg1);
        }

        @Override
        public void log( String arg0, Object arg1 )
        {
            logger.trace(arg0, arg1);
        }

        @Override
        public void log( String arg0, Object[] arg1 )
        {
            logger.trace(arg0, arg1);
        }

        @Override
        public void log( String arg0, Throwable arg1 )
        {
            logger.trace(arg0, arg1);
        }

        @Override
        public void log( Marker arg0, String arg1, Object arg2 )
        {
            logger.trace(arg0, arg1, arg2);
        }

        @Override
        public void log( Marker arg0, String arg1, Object[] arg2 )
        {
            logger.trace(arg0, arg1, arg2);
        }

        @Override
        public void log( Marker arg0, String arg1, Throwable arg2 )
        {
            logger.trace(arg0, arg1, arg2);
        }

        @Override
        public void log( String arg0, Object arg1, Object arg2 )
        {
            logger.trace(arg0, arg1, arg2);
        }

        @Override
        public void log( Marker arg0, String arg1, Object arg2, Object arg3 )
        {
            logger.trace(arg0, arg1, arg2, arg3);
        }
    }

    private static final class OffLogger extends AbstractLogger
    {
        OffLogger( Logger logger )
        {
            super(logger);
        }

        @Override
        public void log( String arg0 )
        {
            // do nothing
        }

        @Override
        public void log( Marker arg0, String arg1 )
        {
            // do nothing
        }

        @Override
        public void log( String arg0, Object arg1 )
        {
            // do nothing
        }

        @Override
        public void log( String arg0, Object[] arg1 )
        {
            // do nothing
        }

        @Override
        public void log( String arg0, Throwable arg1 )
        {
            // do nothing
        }

        @Override
        public void log( Marker arg0, String arg1, Object arg2 )
        {
            // do nothing
        }

        @Override
        public void log( Marker arg0, String arg1, Object[] arg2 )
        {
            // do nothing
        }

        @Override
        public void log( Marker arg0, String arg1, Throwable arg2 )
        {
            // do nothing
        }

        @Override
        public void log( String arg0, Object arg1, Object arg2 )
        {
            // do nothing
        }

        @Override
        public void log( Marker arg0, String arg1, Object arg2, Object arg3 )
        {
            // do nothing
        }
    }
}
