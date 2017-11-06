package net.varanus.sdncontroller.linkstats.internal;


import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.annotation.ParametersAreNonnullByDefault;

import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.varanus.sdncontroller.alias.IAliasService;
import net.varanus.sdncontroller.types.FlowedLink;
import net.varanus.sdncontroller.util.MetricSummary;
import net.varanus.sdncontroller.util.RatioSummary;
import net.varanus.sdncontroller.util.TimeSummary;
import net.varanus.sdncontroller.util.module.ModuleUtils;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.io.PathUtils;
import net.varanus.util.json.JSONUtils;
import net.varanus.util.math.HysteresibleDouble;
import net.varanus.util.text.CustomProperty;
import net.varanus.util.text.IntProperty;
import net.varanus.util.text.StringUtils;


/**
 * 
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
final class Props
{
    static int getLLDPProbingLatencyWindowSize( Map<String, String> params ) throws FloodlightModuleException
    {
        return ModuleUtils.readIntProperty(params,
            IntProperty.ofPositive("LLDPProbingLatencyWindowSize",
                TimeSummary.Builder.DEFAULT_WINDOW_SIZE));
    }

    static int getSecureProbingLatencyWindowSize( Map<String, String> params ) throws FloodlightModuleException
    {
        return ModuleUtils.readIntProperty(params,
            IntProperty.ofPositive("secureProbingLatencyWindowSize",
                TimeSummary.Builder.DEFAULT_WINDOW_SIZE));
    }

    static int getSecureProbingLossWindowSize( Map<String, String> params ) throws FloodlightModuleException
    {
        return ModuleUtils.readIntProperty(params,
            IntProperty.ofPositive("secureProbingLossWindowSize",
                RatioSummary.Builder.DEFAULT_WINDOW_SIZE));
    }

    static int getTrajectoryLatencyWindowSize( Map<String, String> params ) throws FloodlightModuleException
    {
        return ModuleUtils.readIntProperty(params,
            IntProperty.ofPositive("trajectoryLatencyWindowSize",
                TimeSummary.Builder.DEFAULT_WINDOW_SIZE));
    }

    static int getTrajectoryLossWindowSize( Map<String, String> params ) throws FloodlightModuleException
    {
        return ModuleUtils.readIntProperty(params,
            IntProperty.ofPositive("trajectoryLossWindowSize",
                RatioSummary.Builder.DEFAULT_WINDOW_SIZE));
    }

    static int getPacketDropRateWindowSize( Map<String, String> params ) throws FloodlightModuleException
    {
        return ModuleUtils.readIntProperty(params,
            IntProperty.ofPositive("packetDropRateWindowSize",
                MetricSummary.Builder.DEFAULT_WINDOW_SIZE));
    }

    static double getHysteresisThresholdFactor( Map<String, String> params ) throws FloodlightModuleException
    {
        return ModuleUtils.readCustomProperty(params,
            CustomProperty.of("hysteresisPercentage",
                HysteresibleDouble.DEFAULT_THRESHOLD_FACTOR,
                ( s ) -> StringUtils.parseUnsignedInt(s) / 100d));
    }

    static FileLogger getGeneralStatsFileLogger( Map<String, String> params ) throws FloodlightModuleException
    {
        return FileLogger.init(params.get("generalStatsLogFilePath"));
    }

    static LogFileGStats getLogFileGeneralStats( Map<String, String> params ) throws FloodlightModuleException
    {
        return ModuleUtils.readCustomProperty(params,
            CustomProperty.of("logFileGeneralStats",
                LogFileGStats::parse));
    }

    static FileLogger getFlowedStatsFileLogger( Map<String, String> params ) throws FloodlightModuleException
    {
        return FileLogger.init(params.get("flowedStatsLogFilePath"));
    }

    static LogFileFStats getLogFileFlowedStats( Map<String, String> params ) throws FloodlightModuleException
    {
        return ModuleUtils.readCustomProperty(params,
            CustomProperty.of("logFileFlowedStats",
                LogFileFStats::parse));
    }

    static Set<FlowedLink> getDebuggedFlowedLinks( Map<String, String> params, IAliasService aliasService )
        throws FloodlightModuleException
    {
        return ModuleUtils.readCustomProperty(params,
            CustomProperty.of("debuggedFlowedLinks",
                Collections.emptySet(),
                s -> JSONUtils.parseSet(s, ss -> FlowedLink.parse(ss, aliasService::getSwitchAlias))));
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    static final class FileLogger
    {
        private static FileLogger init( String filePathStr ) throws FloodlightModuleException
        {
            try {
                Path filePath = PathUtils.toUnixPath(Objects.toString(filePathStr, "/dev/null"));
                boolean autoFlush = true;
                PrintWriter printer = new PrintWriter(Files.newBufferedWriter(filePath), autoFlush);
                return new FileLogger(filePath, printer);
            }
            catch (InvalidPathException e) {
                throw new FloodlightModuleException(
                    String.format("Invalid path: %s", e.getMessage()));
            }
            catch (IOException e) {
                throw new FloodlightModuleException("IO error while opening log file", e);
            }
        }

        private final Path        filePath;
        private final PrintWriter printer;

        private FileLogger( Path filePath, PrintWriter printer )
        {
            this.filePath = filePath;
            this.printer = printer;
        }

        Path getFilePath()
        {
            return filePath;
        }

        PrintWriter getPrinter()
        {
            return printer;
        }
    }

    private Props()
    {
        // not used
    }
}
