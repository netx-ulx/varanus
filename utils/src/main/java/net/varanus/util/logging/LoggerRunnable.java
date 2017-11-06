package net.varanus.util.logging;


import java.util.Objects;

import org.slf4j.Logger;

import net.varanus.util.lang.ClassUtils;


/**
 * A {@code Runnable} with task-start/end and exception logging.
 */
public class LoggerRunnable implements Runnable
{
    private final Runnable       runnable;
    private final String         name;
    private final AbstractLogger logger;

    public LoggerRunnable( Runnable runnable, Logger logger, LogLevel logLevel )
    {
        this(runnable, abb(runnable.getClass()), logger, logLevel);
    }

    public LoggerRunnable( Runnable runnable, String name, Logger logger, LogLevel logLevel )
    {
        this.runnable = Objects.requireNonNull(runnable);
        this.name = Objects.requireNonNull(name);
        this.logger = AbstractLogger.of(logger, logLevel);
    }

    @Override
    public void run()
    {
        try {
            logger.log("{} has begun execution", name);
            runnable.run();
            logger.log("{} has finished execution", name);
        }
        catch (Throwable t) {
            logger.log(String.format("%s has thrown an exception", name), t);
            throw t;
        }
    }

    private static String abb( Class<?> klass )
    {
        return ClassUtils.getAbbreviatedName(klass);
    }
}
