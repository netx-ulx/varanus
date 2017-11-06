package net.varanus.sdncontroller.linkstats.internal;


import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.slf4j.Logger;

import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.varanus.sdncontroller.alias.IAliasService;
import net.varanus.sdncontroller.linkstats.FlowedLinkStats;
import net.varanus.sdncontroller.linkstats.GeneralLinkStats;
import net.varanus.sdncontroller.linkstats.ILinkStatsListener;
import net.varanus.sdncontroller.linkstats.ILinkStatsService;
import net.varanus.sdncontroller.linkstats.internal.Props.FileLogger;
import net.varanus.sdncontroller.logging.Logging;
import net.varanus.sdncontroller.types.DatapathLink;
import net.varanus.sdncontroller.types.FlowedLink;
import net.varanus.sdncontroller.util.module.IModuleManager;
import net.varanus.sdncontroller.util.module.ModuleUtils;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.collect.CollectionUtils;
import net.varanus.util.text.StringUtils;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
final class Debugger implements IModuleManager, ILinkStatsListener
{
    private static final Logger LOG = Logging.linkstats.LOG;

    private @Nullable Set<FlowedLink>   debuggedFLinks;
    private @Nullable Set<DatapathLink> debuggedDLinks;

    private @Nullable FileLogger    gStatsFileLogger;
    private @Nullable LogFileGStats logFileGStats;

    private @Nullable FileLogger    fStatsFileLogger;
    private @Nullable LogFileFStats logFileFStats;

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies()
    {
        return ModuleUtils.services(IAliasService.class);
    }

    @Override
    public void init( FloodlightModuleContext context, Class<? extends IFloodlightModule> moduleClass )
        throws FloodlightModuleException
    {
        IAliasService aliasService = context.getServiceImpl(IAliasService.class);

        Map<String, String> params = context.getConfigParams(moduleClass);
        this.debuggedFLinks = Props.getDebuggedFlowedLinks(params, aliasService);
        this.debuggedDLinks = CollectionUtils.toSet(debuggedFLinks, FlowedLink::unflowed);
        this.gStatsFileLogger = Props.getGeneralStatsFileLogger(params);
        this.logFileGStats = Props.getLogFileGeneralStats(params);
        this.fStatsFileLogger = Props.getFlowedStatsFileLogger(params);
        this.logFileFStats = Props.getLogFileFlowedStats(params);

        context.getServiceImpl(ILinkStatsService.class).addListener(this);
    }

    @Override
    public void startUp( FloodlightModuleContext context, Class<? extends IFloodlightModule> moduleClass )
    {
        if (debuggedFLinks.isEmpty()) {
            LOG.debug("No debugged flowed-links configured, nothing to debug");
        }
        else {
            LOG.debug("The following flowed-links will be debugged:{}{}",
                System.lineSeparator(),
                StringUtils.joinAllInLines(debuggedFLinks));
        }

        LOG.debug("Using log file {} for general link stats {}", gStatsFileLogger.getFilePath(), logFileGStats);
        LOG.debug("Using log file {} for flowed link stats {}", fStatsFileLogger.getFilePath(), logFileFStats);
    }

    @Override
    public void generalUpdated( GeneralLinkStats stats )
    {
        if (debuggedDLinks.contains(stats.getLink())) {
            LOG.debug("Datapath-link UPDATED:{}", prettyStats(stats));
            gStatsFileLogger.getPrinter()
                .println(String.format("%s\t%s", Instant.now(), logFileGStats.getStat(stats)));
        }
    }

    @Override
    public void flowedUpdated( FlowedLinkStats stats )
    {
        if (debuggedFLinks.contains(stats.getLink())) {
            LOG.debug("Flowed-link UPDATED:{}", prettyStats(stats));
            fStatsFileLogger.getPrinter()
                .println(String.format("%s\t%s", Instant.now(), logFileFStats.getStat(stats)));
        }
    }

    @Override
    public void generalCleared( GeneralLinkStats last )
    {
        if (debuggedDLinks.contains(last.getLink())) {
            LOG.debug("Datapath-link CLEARED:{}", prettyStats(last));
        }
    }

    @Override
    public void flowedCleared( FlowedLinkStats last )
    {
        if (debuggedFLinks.contains(last.getLink())) {
            LOG.debug("Flowed-link CLEARED:{}", prettyStats(last));
        }
    }

    private static String prettyStats( GeneralLinkStats stats )
    {
        StringBuilder sb = new StringBuilder();
        appendPrettyStats(sb, stats);
        return sb.toString();
    }

    private static String prettyStats( FlowedLinkStats stats )
    {
        StringBuilder sb = new StringBuilder();
        appendPrettyStats(sb, stats);
        appendPrettyStats(sb, stats.general());
        return sb.toString();
    }

    private static StringBuilder appendPrettyStats( StringBuilder sb, GeneralLinkStats stats )
    {
        newLine(sb).append(stats.toPrettyString());
        newLine(sb).append(stats.linkConfig().toPrettyString());
        newLine(sb).append(stats.lldpProbing().toPrettyString());
        newLine(sb).append(stats.secureProbing().toPrettyString());
        return sb;
    }

    private static StringBuilder appendPrettyStats( StringBuilder sb, FlowedLinkStats stats )
    {
        newLine(sb).append(stats.toPrettyString());
        newLine(sb).append(stats.switchCounter().toPrettyString());
        newLine(sb).append(stats.trajectory().toPrettyString());
        return sb;
    }

    private static StringBuilder newLine( StringBuilder sb )
    {
        return sb.append(System.lineSeparator());
    }
}
