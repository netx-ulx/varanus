package net.varanus.xmlproxy;


import java.io.IOException;

import javax.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.varanus.xmlproxy.internal.XMLProxy;


/**
 * 
 */
public final class Main
{
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main( String[] args )
    {
        try {
            LOG.info("== Loading configuration for varanus-xml-proxy ==");
            XMLProxyConfig config = XMLProxyConfig.getConfig(args);

            LOG.info("== Initializing varanus-xml-proxy ==");
            XMLProxy proxy = new XMLProxy(config);

            LOG.info("== Starting varanus-xml-proxy ==");
            proxy.start();
            proxy.waitForShutdown();

            LOG.info("== Exiting varanus-xml-proxy ==");
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
        catch (JAXBException e) {
            LOG.error("!!! JAXB error: {}", e.getMessage(), e);
            System.exit(1);
            return; // for compiler only
        }
    }

    private Main()
    {
        // not used
    }
}
