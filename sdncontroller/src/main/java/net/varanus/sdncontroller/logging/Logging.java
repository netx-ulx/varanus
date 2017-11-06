package net.varanus.sdncontroller.logging;


import javax.annotation.ParametersAreNonnullByDefault;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.varanus.sdncontroller.activeforwarding.ActiveForwardingModule;
import net.varanus.sdncontroller.alias.AliasModule;
import net.varanus.sdncontroller.configserver.ConfigServerModule;
import net.varanus.sdncontroller.flowdiscovery.FlowDiscoveryModule;
import net.varanus.sdncontroller.infoserver.InfoServerModule;
import net.varanus.sdncontroller.linkstats.LinkStatsModule;
import net.varanus.sdncontroller.monitoring.MonitoringModule;
import net.varanus.sdncontroller.monitoring.submodules.collectorhandler.CollectorHandlerModule;
import net.varanus.sdncontroller.monitoring.submodules.probing.LinkProbingModule;
import net.varanus.sdncontroller.monitoring.submodules.sampling.LinkSamplingModule;
import net.varanus.sdncontroller.monitoring.submodules.switches.SwitchMonitoringModule;
import net.varanus.sdncontroller.qosrouting.QoSRoutingModule;
import net.varanus.sdncontroller.staticforwarding.StaticForwardingModule;
import net.varanus.sdncontroller.topologygraph.TopologyGraphModule;
import net.varanus.sdncontroller.trafficgenerator.TrafficGeneratorModule;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;


/**
 * This class centralizes the logging service for the varanus modules.
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class Logging
{
    public static final class activeforwarding
    {
        public static final Logger LOG = newPackageLevelLogger(ActiveForwardingModule.class);
    }

    public static final class alias
    {
        public static final Logger LOG = newPackageLevelLogger(AliasModule.class);
    }

    public static final class configserver
    {
        public static final Logger LOG = newPackageLevelLogger(ConfigServerModule.class);
    }

    public static final class flowdiscovery
    {
        public static final Logger LOG = newPackageLevelLogger(FlowDiscoveryModule.class);
    }

    public static final class infoserver
    {
        public static final Logger LOG = newPackageLevelLogger(InfoServerModule.class);
    }

    public static final class linkstats
    {
        public static final Logger LOG = newPackageLevelLogger(LinkStatsModule.class);
    }

    public static final class monitoring
    {
        public static final Logger LOG = newPackageLevelLogger(MonitoringModule.class);

        public static final class collectorhandler
        {
            public static final Logger LOG = newPackageLevelLogger(CollectorHandlerModule.class);
        }

        public static final class probing
        {
            public static final Logger LOG = newPackageLevelLogger(LinkProbingModule.class);
        }

        public static final class sampling
        {
            public static final Logger LOG = newPackageLevelLogger(LinkSamplingModule.class);
        }

        public static final class switches
        {
            public static final Logger LOG = newPackageLevelLogger(SwitchMonitoringModule.class);
        }
    }

    public static final class qosrouting
    {
        public static final Logger LOG = newPackageLevelLogger(QoSRoutingModule.class);
    }

    public static final class staticforwarding
    {
        public static final Logger LOG = newPackageLevelLogger(StaticForwardingModule.class);
    }

    public static final class topologygraph
    {
        public static final Logger LOG = newPackageLevelLogger(TopologyGraphModule.class);
    }

    public static final class trafficgenerator
    {
        public static final Logger LOG = newPackageLevelLogger(TrafficGeneratorModule.class);
    }

    private static Logger newPackageLevelLogger( Class<?> clazz )
    {
        return LoggerFactory.getLogger(clazz.getPackage().getName());
    }

    /*
     * private static final LogLevel DEFAULT_LOGGER_RUNNABLE_LEVEL =
     * LogLevel.TRACE;
     * public static Runnable newLoggerRunnable(Runnable r, Logger logger) {
     * return new LoggerRunnable(r, logger, DEFAULT_LOGGER_RUNNABLE_LEVEL);
     * }
     * public static Runnable newLoggerRunnable(Runnable r, String name, Logger
     * logger) {
     * return new LoggerRunnable(r, name, logger,
     * DEFAULT_LOGGER_RUNNABLE_LEVEL);
     * }
     * public static Runnable newLoggerRunnable(Runnable r, Logger logger,
     * LogLevel logLevel) {
     * return new LoggerRunnable(r, logger, logLevel);
     * }
     * public static Runnable newLoggerRunnable(Runnable r, String name, Logger
     * logger, LogLevel logLevel) {
     * return new LoggerRunnable(r, name, logger, logLevel);
     * }
     */

    private Logging()
    {
        // not used
    }
}
