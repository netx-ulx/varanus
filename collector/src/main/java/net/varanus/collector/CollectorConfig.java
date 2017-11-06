package net.varanus.collector;


import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.projectfloodlight.openflow.types.DatapathId;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableMap;

import net.varanus.mirroringprotocol.util.CollectorId;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.io.IOFunctionUtils;
import net.varanus.util.json.JSONUtils;
import net.varanus.util.openflow.NodePortUtils;
import net.varanus.util.openflow.types.NodeId;
import net.varanus.util.text.BooleanProperty;
import net.varanus.util.text.StringUtils;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class CollectorConfig
{
    public static CollectorConfig getConfig( String[] args ) throws IllegalArgumentException, IOException
    {
        CollectorId collectorID = retrieveCollectorId(args);
        Path configFile = retrieveConfigFilePath(args);
        Properties props = readProperties(configFile);
        Map<DatapathId, String> dpidAliases = readDpidAliases(props);

        return new CollectorConfig(
            collectorID,
            dpidAliases,
            readSwitchIfaceMapping(props, collectorID, dpidAliases),
            readControllerAddress(props),
            readIgnoreBSNPackets(props));
    }

    private final CollectorId                      collectorID;
    private final ImmutableMap<DatapathId, String> dpidAliases;
    private final ImmutableMap<NodeId, String>     switchIfaceMapping;
    private final SocketAddress                    controllerAddress;
    private final boolean                          ignoreBSNPackets;

    private CollectorConfig( CollectorId collectorID,
                             Map<DatapathId, String> dpidAliases,
                             Map<NodeId, String> switchIfaceMapping,
                             SocketAddress controllerAddress,
                             boolean ignoreBSNPackets )
    {
        this.collectorID = collectorID;
        this.dpidAliases = ImmutableMap.copyOf(dpidAliases);
        this.switchIfaceMapping = ImmutableMap.copyOf(switchIfaceMapping);
        this.controllerAddress = controllerAddress;
        this.ignoreBSNPackets = ignoreBSNPackets;
    }

    public CollectorId getCollectorID()
    {
        return collectorID;
    }

    public ImmutableMap<DatapathId, String> getDpidAliases()
    {
        return dpidAliases;
    }

    public ImmutableMap<NodeId, String> getSwitchIfaceMapping()
    {
        return switchIfaceMapping;
    }

    public SocketAddress getControllerAddress()
    {
        return controllerAddress;
    }

    public boolean getIgnoreBSNPackets()
    {
        return ignoreBSNPackets;
    }

    public void log( Logger log )
    {
        log(log, null);
    }

    public void log( Logger log, @Nullable String prefix )
    {
        prefix = (prefix != null) ? prefix : "";

        StringBuilder builder = new StringBuilder();
        String n = System.lineSeparator();
        String i = n + "                       ";
        builder.append(n);
        builder.append("Collector ID         : ").append(collectorID).append(n);
        builder.append("Controller address   : ").append(controllerAddress).append(n);
        builder.append("Switch-iface mappings: ").append(StringUtils.joinAll(i, switchIfaceMapping)).append(n);

        log.info("{}{}", prefix, builder.toString());
    }

    private static final String COLLECTOR_ID_OPTION = "-id";

    private static final String DEFAULT_CONFIG_FILE = "config/varanus-collector.properties";
    private static final String CONFIG_FILE_OPTION  = "-cf";

    private static final String          DPID_ALIASES_PROPKEY         = "switchAliases";
    private static final String          SWITCH_IFACE_MAPPING_PROPKEY = "switchIfaceMapping";
    private static final String          CONTROLLER_ADDRESS_PROPKEY   = "controllerAddress";
    private static final BooleanProperty IGNORE_BSN_PACKETS_PROP      = BooleanProperty.of("ignoreBSNPackets");

    private static final String  CONTROLLER_ADDRESS_DESCRIPT    = "<hostname|IP_address>:<port>";
    private static final Pattern CONTROLLER_ADDRESS_FMT         = Pattern.compile("^[^\\s:]+:[\\d]+$");
    private static final Pattern CONTROLLER_ADDRESS_SPLIT_FMT   = Pattern.compile(":");
    private static final int     CONTROLLER_ADDRESS_SPLIT_LIMIT = 2;

    private static CollectorId retrieveCollectorId( String[] args ) throws IllegalArgumentException
    {
        final String option = COLLECTOR_ID_OPTION;

        String idStr = Optional.ofNullable(
            readOption(args, option))
            .orElseThrow(
                () -> new IllegalArgumentException(
                    String.format(
                        "must pass command line option '%s' with collector id",
                        option)));

        try {
            return CollectorId.of(idStr);
        }
        catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                invalidOptionValueErrorMsg(idStr, option, e.getMessage()));
        }
    }

    private static Path retrieveConfigFilePath( String[] args ) throws IllegalArgumentException
    {
        final String option = CONFIG_FILE_OPTION;
        final String defaultValue = DEFAULT_CONFIG_FILE;

        String configFile = Optional.ofNullable(
            readOption(args, option))
            .orElse(defaultValue);

        try {
            return Paths.get(configFile);
        }
        catch (InvalidPathException e) {
            throw new IllegalArgumentException(
                invalidOptionValueErrorMsg(configFile, option, e.getMessage()));
        }
    }

    private static Properties readProperties( Path configFile ) throws IOException
    {
        if (Files.isDirectory(configFile)) {
            try (Stream<Path> files = Files.list(configFile)) {
                Properties props = new Properties();
                files.sorted()
                    .filter(Files::isReadable)
                    .filter(Files::isRegularFile)
                    .filter(file -> file.toString().endsWith(".properties"))
                    .forEach(IOFunctionUtils.uncheckedConsumer(( file ) -> {
                        try (Reader reader = Files.newBufferedReader(file)) {
                            props.load(reader);
                        }
                    }));
                return props;
            }
            catch (UncheckedIOException e) {
                throw e.getCause();
            }
        }
        else {
            try (Reader reader = Files.newBufferedReader(configFile)) {
                Properties props = new Properties();
                props.load(reader);
                return props;
            }
        }
    }

    private static Map<DatapathId, String> readDpidAliases( Properties props )
    {
        final String prop = DPID_ALIASES_PROPKEY;

        // be nice to the user and accept leading or trailing whitespace
        Optional<String> rawConfig = Optional.ofNullable(props.getProperty(prop)).map(String::trim);
        if (rawConfig.isPresent()) {
            try {
                return NodePortUtils.parseAliasMap(rawConfig.get());
            }
            catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                    invalidPropertyValueErrorMsg(rawConfig.get(), prop, e.getMessage()));
            }
        }
        else {
            return Collections.emptyMap();
        }
    }

    private static Map<NodeId, String> readSwitchIfaceMapping( Properties props,
                                                               CollectorId collectorID,
                                                               Map<DatapathId, String> dpidAliases )
    {
        final String prop = SWITCH_IFACE_MAPPING_PROPKEY;

        // be nice to the user and accept leading or trailing whitespace
        Optional<String> rawConfig = Optional.ofNullable(props.getProperty(prop)).map(String::trim);
        if (rawConfig.isPresent()) {
            try {
                Map<CollectorId, Map<NodeId, String>> mapping = JSONUtils.parseMapOfMaps(
                    rawConfig.get(),
                    CollectorId::of,
                    s -> NodeId.parse(s, dpidAliases::get),
                    Function.identity());

                return mapping.getOrDefault(collectorID, Collections.emptyMap());
            }
            catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                    invalidPropertyValueErrorMsg(rawConfig.get(), prop, e.getMessage()));
            }
        }
        else {
            throw new IllegalArgumentException(
                propertyNotFoundErrorMsg(prop));
        }
    }

    private static SocketAddress readControllerAddress( Properties props ) throws IllegalArgumentException
    {
        final String prop = CONTROLLER_ADDRESS_PROPKEY;
        final String descript = CONTROLLER_ADDRESS_DESCRIPT;
        final Pattern regex = CONTROLLER_ADDRESS_FMT;
        final Pattern splitRegex = CONTROLLER_ADDRESS_SPLIT_FMT;
        final int splitLimit = CONTROLLER_ADDRESS_SPLIT_LIMIT;

        Optional<String> rawAddr = Optional.ofNullable(props.getProperty(prop));
        if (rawAddr.isPresent()) {
            // be nice to the user and accept leading or trailing whitespace
            rawAddr = rawAddr.map(raw -> raw.trim());

            // check format
            if (rawAddr.filter(raw -> regex.matcher(raw).matches()).isPresent()) {
                try {
                    String[] split = splitRegex.split(
                        rawAddr.get(),
                        splitLimit);

                    InetAddress ipAddr = InetAddress.getByName(split[0]);
                    int port = Integer.parseInt(split[1]);

                    // throws IllegalArgumentException if port is illegal
                    return new InetSocketAddress(ipAddr, port);
                }
                catch (UnknownHostException | IllegalArgumentException e) {
                    throw new IllegalArgumentException(
                        invalidPropertyValueErrorMsg(rawAddr.get(), prop, e.getMessage()));
                }
            }
            else {
                throw new IllegalArgumentException(
                    malformedPropertyValueErrorMsg(rawAddr.get(), prop, descript));
            }
        }
        else {
            throw new IllegalArgumentException(
                propertyNotFoundErrorMsg(prop));
        }
    }

    private static boolean readIgnoreBSNPackets( Properties props )
    {
        final BooleanProperty prop = IGNORE_BSN_PACKETS_PROP;
        try {
            return prop.readBoolean(props);
        }
        catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                String.format(
                    "invalid property %s: %s",
                    prop.getPropKey(),
                    e.getMessage()));
        }
    }

    private static String readOption( String[] args, String option )
    {
        String arg = null;

        int i = 0;
        while (i < args.length) {
            int current = i;

            if (args[current].equalsIgnoreCase(option)) {
                int next = current + 1;
                if (next < args.length) {
                    arg = args[next];
                    current = next;
                }
                else {
                    throw new IllegalArgumentException(
                        String.format("missing value for option '%s'", option));
                }
            }

            i = current + 1;
        }

        return arg;
    }

    private static String invalidOptionValueErrorMsg( Object value, String option, String reason )
    {
        return String.format(
            "invalid value \"%s\" for command line option '%s': %s",
            value,
            option,
            reason);
    }

    private static String propertyNotFoundErrorMsg( String propKey )
    {
        return String.format(
            "config property '%s' not found",
            propKey);
    }

    private static String malformedPropertyValueErrorMsg( Object value, String propKey, String propDesc )
    {
        return String.format(
            "malformed value \"%s\" for config property '%s' (must have the form '%s')",
            value,
            propKey,
            propDesc);
    }

    private static String invalidPropertyValueErrorMsg( Object value, String propKey, String reason )
    {
        return String.format(
            "invalid value \"%s\" for config property '%s': %s",
            value,
            propKey,
            reason);
    }
}
