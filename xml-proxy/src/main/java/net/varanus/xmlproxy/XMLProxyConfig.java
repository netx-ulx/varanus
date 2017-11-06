package net.varanus.xmlproxy;


import java.io.IOException;
import java.io.Reader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.slf4j.Logger;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.text.BooleanProperty;
import net.varanus.util.text.CustomProperty;
import net.varanus.util.time.TimeLong;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class XMLProxyConfig
{
    public static XMLProxyConfig getConfig( String[] args ) throws IllegalArgumentException, IOException
    {
        Path configFile = retrieveConfigFilePath(args);
        Properties props = readProperties(configFile);

        return new XMLProxyConfig(
            INFO_SERVER_ADDR_PROP.readProperty(props),
            CFG_SERVER_ADDR_PROP.readProperty(props),
            REM_MININET_ADDR_PROP.readProperty(props),
            CMD_OUTPUT_ADDR_PROP.readProperty(props),
            LOCAL_ADDR_PROP.readProperty(props),
            TOPO_UPDATE_PERIOD_PROP.readProperty(props),
            AUTO_SET_ROUTE_MATCH_PROP.readBoolean(props),
            STAT_ERROR_ON_UNKNOWN_LINK_PROP.readBoolean(props));
    }

    private final InetSocketAddress infoServerAddress;
    private final InetSocketAddress cfgServerAddress;
    private final InetSocketAddress remoteMininetAddress;
    private final InetSocketAddress commandOutputAddress;
    private final InetSocketAddress localAddress;
    private final TimeLong          topoUpdatePeriod;
    private final boolean           autoSetRouteMatch;
    private final boolean           statErrorOnUnknownLink;

    private XMLProxyConfig( InetSocketAddress infoServerAddress,
                            InetSocketAddress cfgServerAddress,
                            InetSocketAddress remoteMininetAddress,
                            InetSocketAddress commandOutputAddress,
                            InetSocketAddress localAddress,
                            TimeLong topoUpdatePeriod,
                            boolean autoSetRouteMatch,
                            boolean statErrorOnUnknownLink )
    {
        this.infoServerAddress = infoServerAddress;
        this.cfgServerAddress = cfgServerAddress;
        this.remoteMininetAddress = remoteMininetAddress;
        this.commandOutputAddress = commandOutputAddress;
        this.localAddress = localAddress;
        this.topoUpdatePeriod = topoUpdatePeriod;
        this.autoSetRouteMatch = autoSetRouteMatch;
        this.statErrorOnUnknownLink = statErrorOnUnknownLink;
    }

    public InetSocketAddress getInfoServerAddress()
    {
        return infoServerAddress;
    }

    public InetSocketAddress getConfigServerAddress()
    {
        return cfgServerAddress;
    }

    public InetSocketAddress getRemoteMininetAddress()
    {
        return remoteMininetAddress;
    }

    public InetSocketAddress getCommandOutputAddress()
    {
        return commandOutputAddress;
    }

    public InetSocketAddress getLocalAddress()
    {
        return localAddress;
    }

    public TimeLong getTopologyUpdatePeriod()
    {
        return topoUpdatePeriod;
    }

    public boolean isAutoSetRouteMatch()
    {
        return autoSetRouteMatch;
    }

    public boolean getStatErrorOnUnknownLink()
    {
        return statErrorOnUnknownLink;
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
        builder.append(n);
        builder.append("Info-server address       : ").append(infoServerAddress).append(n);
        builder.append("Config-server address     : ").append(cfgServerAddress).append(n);
        builder.append("Remote-mininet address    : ").append(remoteMininetAddress).append(n);
        builder.append("Command-output address    : ").append(commandOutputAddress).append(n);
        builder.append("Local proxy address       : ").append(localAddress).append(n);
        builder.append("Topology update period    : ").append(topoUpdatePeriod).append(n);
        builder.append("Auto-set route match      : ").append(autoSetRouteMatch).append(n);
        builder.append("Stat error on unknown link: ").append(statErrorOnUnknownLink).append(n);

        log.info("{}{}", prefix, builder.toString());
    }

    private static final String DEFAULT_CONFIG_FILE = "config/varanus-xmlproxy.properties";
    private static final String CONFIG_FILE_OPTION  = "-cf";

    private static final CustomProperty<InetSocketAddress> INFO_SERVER_ADDR_PROP = newAddrProp("infoServerAddress");
    private static final CustomProperty<InetSocketAddress> CFG_SERVER_ADDR_PROP  = newAddrProp("configServerAddress");
    private static final CustomProperty<InetSocketAddress> REM_MININET_ADDR_PROP = newAddrProp("remoteMininetAddress");
    private static final CustomProperty<InetSocketAddress> CMD_OUTPUT_ADDR_PROP  = newAddrProp("commandOutputAddress");
    private static final CustomProperty<InetSocketAddress> LOCAL_ADDR_PROP       = newAddrProp("localAddress");

    private static final CustomProperty<TimeLong> TOPO_UPDATE_PERIOD_PROP = CustomProperty.of(
        "topologyUpdatePeriodMillis", s -> TimeLong.parse(s, TimeUnit.MILLISECONDS));

    private static final BooleanProperty AUTO_SET_ROUTE_MATCH_PROP       = BooleanProperty.of("autoSetRouteMatch");
    private static final BooleanProperty STAT_ERROR_ON_UNKNOWN_LINK_PROP = BooleanProperty.of("statErrorOnUnknownLink");

    private static CustomProperty<InetSocketAddress> newAddrProp( String propKey )
    {
        final Pattern addrRegex = Pattern.compile("^[^\\s:]+:[\\d]+$");
        final Pattern addrSplitRegex = Pattern.compile(":");
        final int addrSplitLimit = 2;

        return CustomProperty.of(propKey, ( addr ) -> {
            // be nice to the user and accept leading or trailing whitespace
            addr = addr.trim();

            // check format
            if (addrRegex.matcher(addr).matches()) {
                try {
                    String[] split = addrSplitRegex.split(addr, addrSplitLimit);

                    InetAddress ipAddr = InetAddress.getByName(split[0]);
                    int port = Integer.parseInt(split[1]);

                    // throws IllegalArgumentException if port is illegal
                    return new InetSocketAddress(ipAddr, port);
                }
                catch (UnknownHostException e) {
                    throw new IllegalArgumentException(e.getMessage());
                }
            }
            else {
                throw new IllegalArgumentException(
                    "malformed address (must have the form '<hostname|IP_address>:<port>')");
            }
        });
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
        try (Reader reader = Files.newBufferedReader(configFile)) {
            Properties props = new Properties();
            props.load(reader);
            return props;
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
}
