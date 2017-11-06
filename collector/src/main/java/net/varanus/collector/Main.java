package net.varanus.collector;


import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.varanus.collector.internal.Collector;


/**
 * 
 */
public final class Main
{
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main( String[] args )
    {
        try {
            LOG.info("== Loading configuration for varanus-collector ==");
            CollectorConfig config = CollectorConfig.getConfig(args);

            LOG.info("== Initializing varanus-collector ==");
            Collector collector = new Collector(config);

            LOG.info("== Starting varanus-collector ==");
            collector.start();
            collector.waitForShutdown();

            LOG.info("== Exiting varanus-collector ==");
        }
        catch (IllegalArgumentException e) {
            LOG.error("!!! {}", e.getMessage(), e);
            System.exit(1);
            return; // for compiler only
        }
        catch (IOException e) {
            LOG.error("!!! IO error: {}", e.getMessage(), e);
            System.exit(1);
            return; // for compiler only
        }
    }

    private Main()
    {
        // not used
    }
}
