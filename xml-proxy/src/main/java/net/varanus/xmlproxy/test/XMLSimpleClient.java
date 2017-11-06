package net.varanus.xmlproxy.test;


import static net.varanus.xmlproxy.xml.XMLPacket.Type.COMMAND_FAILURE;
import static net.varanus.xmlproxy.xml.XMLPacket.Type.COMMAND_SUCCESS;
import static net.varanus.xmlproxy.xml.XMLPacket.Type.LINK_CONFIG_FAILURE;
import static net.varanus.xmlproxy.xml.XMLPacket.Type.LINK_CONFIG_SUCCESS;
import static net.varanus.xmlproxy.xml.XMLPacket.Type.LINK_STATE_FAILURE;
import static net.varanus.xmlproxy.xml.XMLPacket.Type.LINK_STATE_SUCCESS;
import static net.varanus.xmlproxy.xml.XMLPacket.Type.ROUTE_FAILURE;
import static net.varanus.xmlproxy.xml.XMLPacket.Type.ROUTE_SUCCESS;
import static net.varanus.xmlproxy.xml.XMLPacket.Type.STATISTICS_FAILURE;
import static net.varanus.xmlproxy.xml.XMLPacket.Type.STATISTICS_SUCCESS;
import static net.varanus.xmlproxy.xml.XMLPacket.Type.TOPOLOGY_FAILURE;
import static net.varanus.xmlproxy.xml.XMLPacket.Type.TOPOLOGY_SUCCESS;
import static net.varanus.xmlproxy.xml.XMLPacket.Type.TRAFFIC_INJECT_FAILURE;
import static net.varanus.xmlproxy.xml.XMLPacket.Type.TRAFFIC_INJECT_SUCCESS;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.collect.CollectionUtils;
import net.varanus.util.functional.Report;
import net.varanus.util.io.exception.IOChannelWriteException;
import net.varanus.util.time.TimeLong;
import net.varanus.xmlproxy.util.XMLPacketReceiver;
import net.varanus.xmlproxy.xml.Helper;
import net.varanus.xmlproxy.xml.XMLCommandError;
import net.varanus.xmlproxy.xml.XMLCommandReply;
import net.varanus.xmlproxy.xml.XMLCommandRequest;
import net.varanus.xmlproxy.xml.XMLLinkConfigError;
import net.varanus.xmlproxy.xml.XMLLinkConfigReply;
import net.varanus.xmlproxy.xml.XMLLinkConfigRequest;
import net.varanus.xmlproxy.xml.XMLLinkStateError;
import net.varanus.xmlproxy.xml.XMLLinkStateReply;
import net.varanus.xmlproxy.xml.XMLLinkStateRequest;
import net.varanus.xmlproxy.xml.XMLPacket;
import net.varanus.xmlproxy.xml.XMLRouteError;
import net.varanus.xmlproxy.xml.XMLRouteReply;
import net.varanus.xmlproxy.xml.XMLRouteRequest;
import net.varanus.xmlproxy.xml.XMLStatError;
import net.varanus.xmlproxy.xml.XMLStatReply;
import net.varanus.xmlproxy.xml.XMLStatRequest;
import net.varanus.xmlproxy.xml.XMLTopologyError;
import net.varanus.xmlproxy.xml.XMLTopologyReply;
import net.varanus.xmlproxy.xml.XMLTrafficInjectError;
import net.varanus.xmlproxy.xml.XMLTrafficInjectReply;
import net.varanus.xmlproxy.xml.XMLTrafficInjectRequest;
import net.varanus.xmlproxy.xml.types.CommandType;
import net.varanus.xmlproxy.xml.types.Link;
import net.varanus.xmlproxy.xml.types.LinkDirection;
import net.varanus.xmlproxy.xml.types.Node;


/**
 * 
 */
public final class XMLSimpleClient
{
    private static final Charset                 INPUT_LINE_CS              = StandardCharsets.UTF_8;
    private static final PrintStream             OUTPUT                     = System.out;
    private static final SocketAddress           DEFAULT_PROVIDER_ADDR      = new InetSocketAddress("localhost", 32771);
    private static final String                  STAT_MATCH                 = "eth_type = 0x0800";
    private static final String /* auto-set */   ROUTE_MATCH                = "";
    private static final long /* in ms */        DEFAULT_LINK_CFG_DELAY     = 50;
    private static final int /* [0, 100]% */     DEFAULT_LINK_CFG_LOSS      = 0;
    private static final double /* in Mbits/s */ DEFAULT_LINK_CFG_BANDWIDTH = 500;
    private static final double /* in Mbits/s */ DEFAULT_IPERF_BANDWIDTH    = 1.0;
    private static final double /* in Mbits/s */ DEFAULT_INJECT_BANDWIDTH   = 1.0;
    private static final String                  DEFAULT_INJECT_MATCH       = "eth_type = 0x0800";
    private static final TimeLong                COMMAND_WAIT_TIME          = TimeLong.of(5, TimeUnit.SECONDS);
    private static final TimeLong                INJECT_WAIT_TIME           = TimeLong.of(30, TimeUnit.SECONDS);

    private static final String TOPOLOGY_OPTION            = "topo";
    private static final String STATISTICS_OPTION_PREFIX   = "stat";
    private static final String ROUTE_OPTION_PREFIX        = "route";
    private static final String LINK_CFG_OPTION_PREFIX     = "qos";
    private static final String PING_OPTION_PREFIX         = "ping";
    private static final String IPERF_OPTION_PREFIX        = "iperf";
    private static final String ENABLE_LINK_OPTION_PREFIX  = "enable";
    private static final String DISABLE_LINK_OPTION_PREFIX = "disable";
    private static final String INJECT_OPTION_PREFIX       = "inject";
    private static final String HELP_OPTION                = "help";
    private static final String QUIT_OPTION                = "quit";
    private static final String EXIT_OPTION                = "exit";

    public static void main( String[] args )
    {
        try {
            SocketAddress providerAddr = getProviderAddress(args);
            try (SocketChannel ch = SocketChannel.open(providerAddr)) {
                BufferedReader lineReader = new BufferedReader(new InputStreamReader(System.in, INPUT_LINE_CS));
                XMLPacketReceiver pktReceiver = new XMLPacketReceiver(ch);
                JAXBContext jc = Helper.initContext();
                printSchema(jc);

                pktReceiver.start();
                try {
                    Topo topo = null;
                    while (true) {
                        println();
                        print("> ");
                        String input = lineReader.readLine();
                        if (input == null) {
                            println();
                            break;
                        }
                        else {
                            input = input.trim();
                            if (input.equals(TOPOLOGY_OPTION)) {
                                topo = handleTopologyOption(ch, jc, pktReceiver);
                            }
                            else if (input.startsWith(STATISTICS_OPTION_PREFIX)) {
                                if (checkTopo(topo)) {
                                    String[] opArgs = getPrefixOptionArgs(input, STATISTICS_OPTION_PREFIX);
                                    handleStatisticsOption(opArgs, topo, ch, jc, pktReceiver);
                                }
                            }
                            else if (input.startsWith(ROUTE_OPTION_PREFIX)) {
                                if (checkTopo(topo)) {
                                    String[] opArgs = getPrefixOptionArgs(input, ROUTE_OPTION_PREFIX);
                                    handleRouteOption(opArgs, topo, ch, jc, pktReceiver);
                                }
                            }
                            else if (input.startsWith(LINK_CFG_OPTION_PREFIX)) {
                                if (checkTopo(topo)) {
                                    String[] opArgs = getPrefixOptionArgs(input, LINK_CFG_OPTION_PREFIX);
                                    handleLinkConfigOption(opArgs, topo, ch, jc, pktReceiver);
                                }
                            }
                            else if (input.startsWith(PING_OPTION_PREFIX)) {
                                if (checkTopo(topo)) {
                                    String[] opArgs = getPrefixOptionArgs(input, PING_OPTION_PREFIX);
                                    handlePingOption(opArgs, topo, ch, jc, pktReceiver);
                                }
                            }
                            else if (input.startsWith(IPERF_OPTION_PREFIX)) {
                                if (checkTopo(topo)) {
                                    String[] opArgs = getPrefixOptionArgs(input, IPERF_OPTION_PREFIX);
                                    handleIperfOption(opArgs, topo, ch, jc, pktReceiver);
                                }
                            }
                            else if (input.startsWith(ENABLE_LINK_OPTION_PREFIX)) {
                                if (checkTopo(topo)) {
                                    String[] opArgs = getPrefixOptionArgs(input, ENABLE_LINK_OPTION_PREFIX);
                                    handleLinkStateOption(opArgs, true, topo, ch, jc, pktReceiver);
                                }
                            }
                            else if (input.startsWith(DISABLE_LINK_OPTION_PREFIX)) {
                                if (checkTopo(topo)) {
                                    String[] opArgs = getPrefixOptionArgs(input, DISABLE_LINK_OPTION_PREFIX);
                                    handleLinkStateOption(opArgs, false, topo, ch, jc, pktReceiver);
                                }
                            }
                            else if (input.startsWith(INJECT_OPTION_PREFIX)) {
                                if (checkTopo(topo)) {
                                    String[] opArgs = getPrefixOptionArgs(input, INJECT_OPTION_PREFIX, 3);
                                    handleTrafficInjectionOption(opArgs, topo, ch, jc, pktReceiver);
                                }
                            }
                            // ================ DEBUG ================
                            else if (input.equals("test-ping")) {
                                if (checkTopo(topo)) {
                                    String[] qosLinks = {"s_00:00:00:00:00:00:00:01-s_00:00:00:00:00:00:00:02"};
                                    handleLinkConfigOption(qosLinks, topo, ch, jc, pktReceiver);
                                    String[] pingHosts = {"h_192.168.10.101", "h_192.168.10.104"};
                                    handlePingOption(pingHosts, topo, ch, jc, pktReceiver);
                                }
                            }
                            else if (input.equals("test-iperf")) {
                                if (checkTopo(topo)) {
                                    String[] qosLinks = {"s_00:00:00:00:00:00:00:01-s_00:00:00:00:00:00:00:02"};
                                    handleLinkConfigOption(qosLinks, topo, ch, jc, pktReceiver);
                                    String[] iperfHosts = {"h_192.168.10.101", "h_192.168.10.104"};
                                    handleIperfOption(iperfHosts, topo, ch, jc, pktReceiver);
                                }
                            }
                            // ================ DEBUG ================
                            else if (input.equals(HELP_OPTION)) {
                                println("Available commands:");
                                println("> topo");
                                println("> stat <link> <link> ...");
                                println("> route <src_node> <dst_node>");
                                println("> qos <link> <link> ...");
                                println("> ping <src_node> <dst_node>");
                                println("> iperf <src_node> <dst_node>");
                                println("> enable <link> <link>...");
                                println("> disable <link> <link>...");
                                println("> inject <link> <direction> [<match args>...]");
                                println("> help");
                                println("> quit|exit");
                            }
                            else if (input.equals(QUIT_OPTION) || input.equals(EXIT_OPTION)) {
                                break;
                            }
                            else {
                                println("Unknown command. Run 'help' for a list of available commands");
                            }
                        }
                    }
                }
                finally {
                    pktReceiver.stop();
                }
            }
            catch (IOChannelWriteException e) {
                e.checkInterruptStatus();
                abort(String.format("IO-WRITE error: %s", e.getMessage()), e);
                return;
            }
            catch (IOException e) {
                abort(String.format("IO error: %s", e.getMessage()), e);
                return;
            }
            catch (JAXBException e) {
                abort(String.format("JAXB error: %s", e.getMessage()), e);
                return;
            }
        }
        catch (UnknownHostException e) {
            abort(String.format("Unknown host: %s", e.getMessage()));
            return;
        }
        catch (IllegalArgumentException e) {
            abort(String.format("Illegal argument: %s", e.getMessage()));
            return;
        }
        catch (InterruptedException e) {
            abort("Interrupted");
            return;
        }

        println("<done>");
    }

    private static @CheckForNull Topo handleTopologyOption( SocketChannel ch,
                                                            JAXBContext jc,
                                                            XMLPacketReceiver pktReceiver )
        throws JAXBException, IOChannelWriteException, InterruptedException
    {
        XMLTopologyReply topoReply = requestTopology(ch, jc, pktReceiver);
        if (topoReply != null) {
            printTopologyReply(topoReply, jc);
            return Topo.of(topoReply);
        }
        else {
            return null;
        }
    }

    private static boolean checkTopo( @Nullable Topo topo )
    {
        if (topo != null) {
            return true;
        }
        else {
            println("OOPS: no topology is available yet, please call 'topo' first");
            return false;
        }
    }

    private static String[] getPrefixOptionArgs( String input, String optionPrefix )
    {
        return getPrefixOptionArgs(input, optionPrefix, 0);
    }

    private static String[] getPrefixOptionArgs( String input, String optionPrefix, int limit )
    {
        String args = input.replaceFirst(optionPrefix, "").trim();
        if (args.isEmpty())
            return new String[0];
        else
            return args.split("\\s+", limit);
    }

    private static void handleStatisticsOption( String[] args,
                                                Topo topo,
                                                SocketChannel ch,
                                                JAXBContext jc,
                                                XMLPacketReceiver pktReceiver )
        throws JAXBException, IOChannelWriteException, InterruptedException
    {
        if (args.length == 0) {
            println("OOPS: must pass at least one link as argument");
        }
        else if (validateLinks(args, topo)) {
            for (String linkId : args) {
                Link link = topo.getLink(linkId);
                for (LinkDirection dir : LinkDirection.values()) {
                    XMLStatRequest req = newStatisticsRequest(link, dir);
                    printStatisticsRequest(req, jc);
                    XMLStatReply reply = requestStatistics(req, ch, jc, pktReceiver);
                    if (reply != null) {
                        printStatisticsReply(reply, jc);
                    }
                }
            }
        }
    }

    private static void handleRouteOption( String[] args,
                                           Topo topo,
                                           SocketChannel ch,
                                           JAXBContext jc,
                                           XMLPacketReceiver pktReceiver )
        throws JAXBException, IOChannelWriteException, InterruptedException
    {
        if (args.length != 2) {
            println("OOPS: must pass exactly two nodes as arguments");
        }
        else if (validateNodes(args, topo)) {
            Node srcNode = topo.getNode(args[0]);
            Node destNode = topo.getNode(args[1]);
            XMLRouteRequest req = newRouteRequest(srcNode, destNode);
            printRouteRequest(req, jc);
            XMLRouteReply reply = requestRoute(req, ch, jc, pktReceiver);
            if (reply != null) {
                printRouteReply(reply, jc);
            }
        }
    }

    private static void handleLinkConfigOption( String[] args,
                                                Topo topo,
                                                SocketChannel ch,
                                                JAXBContext jc,
                                                XMLPacketReceiver pktReceiver )
        throws JAXBException, IOChannelWriteException, InterruptedException
    {
        if (args.length == 0) {
            println("OOPS: must pass at least one link as argument");
        }
        else if (validateLinks(args, topo)) {
            for (String linkId : args) {
                Link link = topo.getLink(linkId);
                for (LinkDirection dir : LinkDirection.values()) {
                    XMLLinkConfigRequest req = newLinkConfigRequest(link, dir);
                    printLinkConfigRequest(req, jc);
                    XMLLinkConfigReply reply = requestLinkConfig(req, ch, jc, pktReceiver);
                    if (reply != null) {
                        printLinkConfigReply(reply, jc);
                    }
                }
            }
        }
    }

    private static void handlePingOption( String[] args,
                                          Topo topo,
                                          SocketChannel ch,
                                          JAXBContext jc,
                                          XMLPacketReceiver pktReceiver )
        throws JAXBException, IOChannelWriteException, InterruptedException
    {
        if (args.length != 2) {
            println("OOPS: must pass exactly two nodes as arguments");
        }
        else if (validateNodes(args, topo)) {
            Node srcNode = topo.getNode(args[0]);
            Node destNode = topo.getNode(args[1]);

            XMLCommandRequest startReq = newPingStartRequest(srcNode, destNode);
            printCommandRequest(startReq, jc);
            if (requestCommandStart(startReq, ch, jc, pktReceiver)) {
                XMLCommandRequest stopReq = newPingStopRequest(srcNode, destNode);
                printCommandRequest(stopReq, jc);
                requestCommandStop(stopReq, ch, jc, pktReceiver);
            }
        }
    }

    private static void handleIperfOption( String[] args,
                                           Topo topo,
                                           SocketChannel ch,
                                           JAXBContext jc,
                                           XMLPacketReceiver pktReceiver )
        throws JAXBException, IOChannelWriteException, InterruptedException
    {
        if (args.length != 2) {
            println("OOPS: must pass exactly two nodes as arguments");
        }
        else if (validateNodes(args, topo)) {
            Node srcNode = topo.getNode(args[0]);
            Node destNode = topo.getNode(args[1]);

            XMLCommandRequest startReq = newIperfStartRequest(srcNode, destNode);
            printCommandRequest(startReq, jc);
            if (requestCommandStart(startReq, ch, jc, pktReceiver)) {
                XMLCommandRequest stopReq = newIperfStopRequest(srcNode, destNode);
                printCommandRequest(stopReq, jc);
                requestCommandStop(stopReq, ch, jc, pktReceiver);
            }
        }
    }

    private static void handleLinkStateOption( String[] args,
                                               boolean enable,
                                               Topo topo,
                                               SocketChannel ch,
                                               JAXBContext jc,
                                               XMLPacketReceiver pktReceiver )
        throws JAXBException, IOChannelWriteException, InterruptedException
    {
        if (args.length == 0) {
            println("OOPS: must pass at least one link as argument");
        }
        else if (validateLinks(args, topo)) {
            for (String linkId : args) {
                Link link = topo.getLink(linkId);
                XMLLinkStateRequest req = newLinkStateRequest(link, enable);
                printLinkStateRequest(req, jc);
                XMLLinkStateReply reply = requestLinkState(req, ch, jc, pktReceiver);
                if (reply != null) {
                    printLinkStateReply(reply, jc);
                }
            }
        }
    }

    private static void handleTrafficInjectionOption( String[] args,
                                                      Topo topo,
                                                      SocketChannel ch,
                                                      JAXBContext jc,
                                                      XMLPacketReceiver pktReceiver )
        throws JAXBException, IOChannelWriteException, InterruptedException
    {
        if (args.length < 2) {
            println("OOPS: must pass at least one link and one direction as arguments");
        }
        else if (validateLink(args[0], topo) && validateLinkDirection(args[1])) {
            Link link = topo.getLink(args[0]);
            LinkDirection direction = topo.getLinkDirection(args[1]);
            String match = (args.length > 2) ? args[2] : DEFAULT_INJECT_MATCH;

            XMLTrafficInjectRequest startReq = newTrafficInjectStartRequest(link, direction, match);
            printTrafficInjectRequest(startReq, jc);
            if (requestTrafficInjectStartOrStop(startReq, ch, jc, pktReceiver)) {
                printf("Waiting %s before stopping the injection...%n", INJECT_WAIT_TIME);
                INJECT_WAIT_TIME.sleep();
                XMLTrafficInjectRequest stopReq = newTrafficInjectStopRequest(link, direction);
                printTrafficInjectRequest(stopReq, jc);
                requestTrafficInjectStartOrStop(stopReq, ch, jc, pktReceiver);
            }
        }
    }

    private static boolean validateNodes( String[] nodes, Topo topo )
    {
        for (String nodeId : nodes) {
            if (!topo.hasNode(nodeId)) {
                printf("OOPS: node '%s' does not exist%n", nodeId);
                return false;
            }
        }

        return true;
    }

    private static boolean validateLinks( String[] links, Topo topo )
    {
        for (String linkId : links) {
            if (!validateLink(linkId, topo)) {
                return false;
            }
        }

        return true;
    }

    private static boolean validateLink( String linkId, Topo topo )
    {
        if (!topo.hasLink(linkId)) {
            printf("OOPS: link '%s' does not exist%n", linkId);
            return false;
        }
        else {
            return true;
        }
    }

    private static boolean validateLinkDirection( String linkDir )
    {
        linkDir = linkDir.toUpperCase();
        if (!(linkDir.equals(LinkDirection.FORWARD.name()) || linkDir.equals(LinkDirection.REVERSE.name()))) {
            printf("OOPS: link direction '%s' is not correct (must be forward|reverse)%n", linkDir);
            return false;
        }
        else {
            return true;
        }
    }

    private static @CheckForNull XMLTopologyReply requestTopology( SocketChannel ch,
                                                                   JAXBContext jc,
                                                                   XMLPacketReceiver pktReceiver )
        throws JAXBException, IOChannelWriteException, InterruptedException
    {
        XMLPacket reqPkt = XMLPacket.newTopologyRequest();
        println("- Sending topology request packet...");
        XMLPacket.IO.writer().write(reqPkt, ch);

        println("- Awaiting reply...");
        Report<XMLPacket> replyReport = getReport(pktReceiver.receive(TOPOLOGY_SUCCESS, TOPOLOGY_FAILURE));
        if (replyReport.hasValue()) {
            XMLPacket replyPkt = replyReport.getValue();
            switch (replyPkt.getType()) {
                case TOPOLOGY_SUCCESS:
                    return Helper.fromBytes(replyPkt.getData(), XMLTopologyReply.class, jc);

                case TOPOLOGY_FAILURE:
                    XMLTopologyError error = Helper.fromBytes(replyPkt.getData(), XMLTopologyError.class, jc);
                    printTopologyError(error, jc);
                    return null;

                default:
                    printf("WARN: Unexpected received packet type %s%n", replyPkt.getType());
                    return null;
            }
        }
        else {
            printf("ERROR: %s%n", replyReport.getError());
            return null;
        }
    }

    private static XMLStatRequest newStatisticsRequest( Link link, LinkDirection dir )
    {
        XMLStatRequest req = new XMLStatRequest();
        req.setLink(link.getId());
        req.setDirection(dir);
        req.setMatch(STAT_MATCH);
        return req;
    }

    private static @CheckForNull XMLStatReply requestStatistics( XMLStatRequest req,
                                                                 SocketChannel ch,
                                                                 JAXBContext jc,
                                                                 XMLPacketReceiver pktReceiver )
        throws JAXBException, IOChannelWriteException, InterruptedException
    {
        XMLPacket reqPkt = XMLPacket.newStatisticsRequest(Helper.toBytes(req, jc));
        println("- Sending statistics request packet...");
        XMLPacket.IO.writer().write(reqPkt, ch);

        println("- Awaiting reply...");
        Report<XMLPacket> replyReport = getReport(pktReceiver.receive(STATISTICS_SUCCESS, STATISTICS_FAILURE));
        if (replyReport.hasValue()) {
            XMLPacket replyPkt = replyReport.getValue();
            switch (replyPkt.getType()) {
                case STATISTICS_SUCCESS:
                    return Helper.fromBytes(replyPkt.getData(), XMLStatReply.class, jc);

                case STATISTICS_FAILURE:
                    XMLStatError error = Helper.fromBytes(replyPkt.getData(), XMLStatError.class, jc);
                    printStatisticsError(error, jc);
                    return null;

                default:
                    printf("WARN: Unexpected received packet type %s%n", replyPkt.getType());
                    return null;
            }
        }
        else {
            printf("ERROR: %s%n", replyReport.getError());
            return null;
        }
    }

    private static XMLRouteRequest newRouteRequest( Node srcHost, Node destHost )
    {
        XMLRouteRequest req = new XMLRouteRequest();
        req.setFrom(srcHost.getId());
        req.setTo(destHost.getId());
        req.setMatch(ROUTE_MATCH);
        return req;
    }

    private static @CheckForNull XMLRouteReply requestRoute( XMLRouteRequest req,
                                                             SocketChannel ch,
                                                             JAXBContext jc,
                                                             XMLPacketReceiver pktReceiver )
        throws JAXBException, IOChannelWriteException, InterruptedException
    {
        XMLPacket reqPkt = XMLPacket.newRouteRequest(Helper.toBytes(req, jc));
        println("- Sending route request packet...");
        XMLPacket.IO.writer().write(reqPkt, ch);

        println("- Awaiting reply...");
        Report<XMLPacket> replyReport = getReport(pktReceiver.receive(ROUTE_SUCCESS, ROUTE_FAILURE));
        if (replyReport.hasValue()) {
            XMLPacket replyPkt = replyReport.getValue();
            switch (replyPkt.getType()) {
                case ROUTE_SUCCESS:
                    return Helper.fromBytes(replyPkt.getData(), XMLRouteReply.class, jc);

                case ROUTE_FAILURE:
                    XMLRouteError error = Helper.fromBytes(replyPkt.getData(), XMLRouteError.class, jc);
                    printRouteError(error, jc);
                    return null;

                default:
                    printf("WARN: Unexpected received packet type %s%n", replyPkt.getType());
                    return null;
            }
        }
        else {
            printf("ERROR: %s%n", replyReport.getError());
            return null;
        }
    }

    private static XMLLinkConfigRequest newLinkConfigRequest( Link link, LinkDirection dir )
    {
        XMLLinkConfigRequest req = new XMLLinkConfigRequest();
        req.setLink(link.getId());
        req.setDirection(dir);
        req.setDelay(DEFAULT_LINK_CFG_DELAY);
        req.setLossRate(DEFAULT_LINK_CFG_LOSS);
        req.setBandwidth(DEFAULT_LINK_CFG_BANDWIDTH);
        return req;
    }

    private static @CheckForNull XMLLinkConfigReply requestLinkConfig( XMLLinkConfigRequest req,
                                                                       SocketChannel ch,
                                                                       JAXBContext jc,
                                                                       XMLPacketReceiver pktReceiver )
        throws JAXBException, IOChannelWriteException, InterruptedException
    {
        XMLPacket reqPkt = XMLPacket.newLinkConfigRequest(Helper.toBytes(req, jc));
        println("- Sending link-config request packet...");
        XMLPacket.IO.writer().write(reqPkt, ch);

        println("- Awaiting reply...");
        Report<XMLPacket> replyReport = getReport(pktReceiver.receive(LINK_CONFIG_SUCCESS, LINK_CONFIG_FAILURE));
        if (replyReport.hasValue()) {
            XMLPacket replyPkt = replyReport.getValue();
            switch (replyPkt.getType()) {
                case LINK_CONFIG_SUCCESS:
                    return Helper.fromBytes(replyPkt.getData(), XMLLinkConfigReply.class, jc);

                case LINK_CONFIG_FAILURE:
                    XMLLinkConfigError error = Helper.fromBytes(replyPkt.getData(), XMLLinkConfigError.class, jc);
                    printLinkConfigError(error, jc);
                    return null;

                default:
                    printf("WARN: Unexpected received packet type %s%n", replyPkt.getType());
                    return null;
            }
        }
        else {
            printf("ERROR: %s%n", replyReport.getError());
            return null;
        }
    }

    private static XMLCommandRequest newPingStartRequest( Node src, Node dest )
    {
        XMLCommandRequest req = new XMLCommandRequest();
        req.setType(CommandType.PING);
        req.setFrom(src.getId());
        req.setTo(dest.getId());
        req.setEnabled(true);
        return req;
    }

    private static XMLCommandRequest newPingStopRequest( Node src, Node dest )
    {
        XMLCommandRequest req = new XMLCommandRequest();
        req.setType(CommandType.PING);
        req.setFrom(src.getId());
        req.setTo(dest.getId());
        req.setEnabled(false);
        return req;
    }

    private static XMLCommandRequest newIperfStartRequest( Node src, Node dest )
    {
        XMLCommandRequest req = new XMLCommandRequest();
        req.setType(CommandType.IPERF);
        req.setFrom(src.getId());
        req.setTo(dest.getId());
        req.setEnabled(true);
        req.setBandwidth(DEFAULT_IPERF_BANDWIDTH);
        return req;
    }

    private static XMLCommandRequest newIperfStopRequest( Node src, Node dest )
    {
        XMLCommandRequest req = new XMLCommandRequest();
        req.setType(CommandType.IPERF);
        req.setFrom(src.getId());
        req.setTo(dest.getId());
        req.setEnabled(false);
        return req;
    }

    private static boolean requestCommandStart( XMLCommandRequest req,
                                                SocketChannel ch,
                                                JAXBContext jc,
                                                XMLPacketReceiver pktReceiver )
        throws JAXBException, IOChannelWriteException, InterruptedException
    {
        XMLPacket reqPkt = XMLPacket.newCommandRequest(Helper.toBytes(req, jc));
        printf("- Sending %s-start request packet...%n", req.getType());
        XMLPacket.IO.writer().write(reqPkt, ch);

        printf("- Awaiting %s replies...%n", req.getType());
        long startTime = System.nanoTime();
        while ((System.nanoTime() - startTime) < COMMAND_WAIT_TIME.inNanos()) {
            Report<XMLPacket> replyReport = getReport(pktReceiver.receive(COMMAND_SUCCESS, COMMAND_FAILURE));
            if (replyReport.hasValue()) {
                XMLPacket replyPkt = replyReport.getValue();
                switch (replyPkt.getType()) {
                    case COMMAND_SUCCESS:
                        XMLCommandReply reply = Helper.fromBytes(replyPkt.getData(), XMLCommandReply.class, jc);
                        printCommandReply(reply, jc);
                    break;

                    case COMMAND_FAILURE:
                        XMLCommandError error = Helper.fromBytes(replyPkt.getData(), XMLCommandError.class, jc);
                        printCommandError(error, jc);
                        return false;

                    default:
                        printf("WARN: Unexpected received packet type %s%n", replyPkt.getType());
                        return false;
                }
            }
            else {
                printf("ERROR: %s%n", replyReport.getError());
                return false;
            }
        }

        return true;
    }

    private static void requestCommandStop( XMLCommandRequest req,
                                            SocketChannel ch,
                                            JAXBContext jc,
                                            XMLPacketReceiver pktReceiver )
        throws JAXBException, IOChannelWriteException, InterruptedException
    {
        XMLPacket reqPkt = XMLPacket.newCommandRequest(Helper.toBytes(req, jc));
        printf("- Sending %s-stop request packet...%n", req.getType());
        XMLPacket.IO.writer().write(reqPkt, ch);

        printf("- Awaiting %s reply...%n", req.getType());
        boolean done = false;
        while (!done) {
            Report<XMLPacket> replyReport = getReport(pktReceiver.receive(COMMAND_SUCCESS, COMMAND_FAILURE));
            if (replyReport.hasValue()) {
                XMLPacket replyPkt = replyReport.getValue();
                switch (replyPkt.getType()) {
                    case COMMAND_SUCCESS:
                        XMLCommandReply reply = Helper.fromBytes(replyPkt.getData(), XMLCommandReply.class, jc);
                        if (!reply.getEnabled()) {
                            printf("- Received %s-stop acknowledgment; done%n", req.getType());
                            printCommandReply(reply, jc);
                            done = true;
                        }
                        else {
                            printf("- Received a leftover %s reply; not done yet%n", req.getType());
                        }
                    break;

                    case COMMAND_FAILURE:
                        XMLCommandError error = Helper.fromBytes(replyPkt.getData(), XMLCommandError.class, jc);
                        printf("- Received %s error! aborting%n", req.getType());
                        printCommandError(error, jc);
                        done = true;
                    break;

                    default:
                        printf("WARN: Unexpected received packet type %s%n", replyPkt.getType());
                    break;
                }
            }
            else {
                printf("ERROR: %s%n", replyReport.getError());
                done = true;
            }
        }
    }

    private static XMLLinkStateRequest newLinkStateRequest( Link link, boolean enable )
    {
        XMLLinkStateRequest req = new XMLLinkStateRequest();
        req.setLink(link.getId());
        req.setEnabled(enable);
        return req;
    }

    private static @CheckForNull XMLLinkStateReply requestLinkState( XMLLinkStateRequest req,
                                                                     SocketChannel ch,
                                                                     JAXBContext jc,
                                                                     XMLPacketReceiver pktReceiver )
        throws JAXBException, IOChannelWriteException, InterruptedException
    {
        XMLPacket reqPkt = XMLPacket.newLinkStateRequest(Helper.toBytes(req, jc));
        println("- Sending link-state request packet...");
        XMLPacket.IO.writer().write(reqPkt, ch);

        println("- Awaiting reply...");
        Report<XMLPacket> replyReport =
            getReport(pktReceiver.receive(LINK_STATE_SUCCESS, LINK_STATE_FAILURE));
        if (replyReport.hasValue()) {
            XMLPacket replyPkt = replyReport.getValue();
            switch (replyPkt.getType()) {
                case LINK_STATE_SUCCESS:
                    return Helper.fromBytes(replyPkt.getData(), XMLLinkStateReply.class, jc);

                case LINK_STATE_FAILURE:
                    XMLLinkStateError error = Helper.fromBytes(replyPkt.getData(), XMLLinkStateError.class, jc);
                    printLinkStateError(error, jc);
                    return null;

                default:
                    printf("WARN: Unexpected received packet type %s%n", replyPkt.getType());
                    return null;
            }
        }
        else {
            printf("ERROR: %s%n", replyReport.getError());
            return null;
        }
    }

    private static XMLTrafficInjectRequest newTrafficInjectStartRequest( Link link,
                                                                         LinkDirection direction,
                                                                         String match )
    {
        XMLTrafficInjectRequest req = new XMLTrafficInjectRequest();
        req.setLink(link.getId());
        req.setDirection(direction);
        req.setEnabled(true);
        req.setMatch(match);
        req.setBandwidth(DEFAULT_INJECT_BANDWIDTH);
        return req;
    }

    private static XMLTrafficInjectRequest newTrafficInjectStopRequest( Link link, LinkDirection direction )
    {
        XMLTrafficInjectRequest req = new XMLTrafficInjectRequest();
        req.setLink(link.getId());
        req.setDirection(direction);
        req.setEnabled(false);
        return req;
    }

    private static boolean requestTrafficInjectStartOrStop( XMLTrafficInjectRequest req,
                                                            SocketChannel ch,
                                                            JAXBContext jc,
                                                            XMLPacketReceiver pktReceiver )
        throws JAXBException, IOChannelWriteException, InterruptedException
    {
        XMLPacket reqPkt = XMLPacket.newTrafficInjectRequest(Helper.toBytes(req, jc));
        println("- Sending traffic-inject request packet...");
        XMLPacket.IO.writer().write(reqPkt, ch);

        println("- Awaiting reply...");
        Report<XMLPacket> replyReport = getReport(pktReceiver.receive(TRAFFIC_INJECT_SUCCESS, TRAFFIC_INJECT_FAILURE));
        if (replyReport.hasValue()) {
            XMLPacket replyPkt = replyReport.getValue();
            switch (replyPkt.getType()) {
                case TRAFFIC_INJECT_SUCCESS:
                    XMLTrafficInjectReply reply = Helper.fromBytes(replyPkt.getData(), XMLTrafficInjectReply.class, jc);
                    printTrafficInjectReply(reply, jc);
                    return true;

                case TRAFFIC_INJECT_FAILURE:
                    XMLTrafficInjectError error = Helper.fromBytes(replyPkt.getData(), XMLTrafficInjectError.class, jc);
                    printTrafficInjectError(error, jc);
                    return false;

                default:
                    printf("WARN: Unexpected received packet type %s%n", replyPkt.getType());
                    return false;
            }
        }
        else {
            printf("ERROR: %s%n", replyReport.getError());
            return false;
        }
    }

    private static Report<XMLPacket> getReport( CompletableFuture<Report<XMLPacket>> future )
        throws InterruptedException
    {
        try {
            return future.get();
        }
        catch (ExecutionException e) {
            // should never happen
            throw new RuntimeException(e.getCause());
        }
    }

    private static void printSchema( JAXBContext jc )
    {
        println();
        println("XML Schema:");
        println(Helper.getSchema(jc));
        println();
    }

    private static void printStatisticsRequest( XMLStatRequest req, JAXBContext jc ) throws JAXBException
    {
        println();
        println("Statistics request:");
        Helper.print(req, OUTPUT, jc);
        println();
    }

    private static void printRouteRequest( XMLRouteRequest req, JAXBContext jc ) throws JAXBException
    {
        println();
        println("Route request:");
        Helper.print(req, OUTPUT, jc);
        println();
    }

    private static void printLinkConfigRequest( XMLLinkConfigRequest req, JAXBContext jc ) throws JAXBException
    {
        println();
        println("Link-config request:");
        Helper.print(req, OUTPUT, jc);
        println();
    }

    private static void printCommandRequest( XMLCommandRequest req, JAXBContext jc ) throws JAXBException
    {
        println();
        println("Command request:");
        Helper.print(req, OUTPUT, jc);
        println();
    }

    private static void printLinkStateRequest( XMLLinkStateRequest req, JAXBContext jc ) throws JAXBException
    {
        println();
        println("Link-state request:");
        Helper.print(req, OUTPUT, jc);
        println();
    }

    private static void printTrafficInjectRequest( XMLTrafficInjectRequest req, JAXBContext jc ) throws JAXBException
    {
        println();
        println("Traffic-inject request:");
        Helper.print(req, OUTPUT, jc);
        println();
    }

    private static void printTopologyReply( XMLTopologyReply topo, JAXBContext jc ) throws JAXBException
    {
        println();
        println("Received topology:");
        Helper.print(topo, OUTPUT, jc);
        println();
    }

    private static void printTopologyError( XMLTopologyError error, JAXBContext jc ) throws JAXBException
    {
        println();
        println("! Received topology error:");
        Helper.print(error, OUTPUT, jc);
        println();
    }

    private static void printStatisticsReply( XMLStatReply reply, JAXBContext jc ) throws JAXBException
    {
        println();
        println("Received statistics:");
        Helper.print(reply, OUTPUT, jc);
        println();
    }

    private static void printStatisticsError( XMLStatError error, JAXBContext jc ) throws JAXBException
    {
        println();
        println("! Received statistics error:");
        Helper.print(error, OUTPUT, jc);
        println();
    }

    private static void printRouteReply( XMLRouteReply reply, JAXBContext jc ) throws JAXBException
    {
        println();
        println("Received route:");
        Helper.print(reply, OUTPUT, jc);
        println();
    }

    private static void printRouteError( XMLRouteError error, JAXBContext jc ) throws JAXBException
    {
        println();
        println("! Received route error:");
        Helper.print(error, OUTPUT, jc);
        println();
    }

    private static void printLinkConfigReply( XMLLinkConfigReply reply, JAXBContext jc ) throws JAXBException
    {
        println();
        println("Received link-config:");
        Helper.print(reply, OUTPUT, jc);
        println();
    }

    private static void printLinkConfigError( XMLLinkConfigError error, JAXBContext jc ) throws JAXBException
    {
        println();
        println("! Received link-config error:");
        Helper.print(error, OUTPUT, jc);
        println();
    }

    private static void printCommandReply( XMLCommandReply reply, JAXBContext jc ) throws JAXBException
    {
        println();
        println("Received command result:");
        Helper.print(reply, OUTPUT, jc);
        println();
    }

    private static void printCommandError( XMLCommandError error, JAXBContext jc ) throws JAXBException
    {
        println();
        println("! Received command error:");
        Helper.print(error, OUTPUT, jc);
        println();
    }

    private static void printLinkStateReply( XMLLinkStateReply reply, JAXBContext jc ) throws JAXBException
    {
        println();
        println("Received link-state reply:");
        Helper.print(reply, OUTPUT, jc);
        println();
    }

    private static void printLinkStateError( XMLLinkStateError error, JAXBContext jc ) throws JAXBException
    {
        println();
        println("! Received link-state error:");
        Helper.print(error, OUTPUT, jc);
        println();
    }

    private static void printTrafficInjectReply( XMLTrafficInjectReply reply, JAXBContext jc ) throws JAXBException
    {
        println();
        println("Received traffic-inject reply:");
        Helper.print(reply, OUTPUT, jc);
        println();
    }

    private static void printTrafficInjectError( XMLTrafficInjectError error, JAXBContext jc ) throws JAXBException
    {
        println();
        println("! Received traffic-inject error:");
        Helper.print(error, OUTPUT, jc);
        println();
    }

    private static SocketAddress getProviderAddress( String[] args )
        throws UnknownHostException, IllegalArgumentException
    {
        if (args.length > 0) {
            if (args.length > 1)
                println("WARN: Ignoring extra arguments beyond the first");
            return Utils.parseSocketAddress(args[0]);
        }
        else {
            printf(
                "INFO: No arguments provided, using default provider address %s%n",
                DEFAULT_PROVIDER_ADDR);
            return DEFAULT_PROVIDER_ADDR;
        }
    }

    private static void abort( @Nullable String msg )
    {
        abort(msg, null);
    }

    private static void abort( @Nullable String msg, @Nullable Throwable t )
    {
        println(Objects.toString(msg, "ERROR: aborted"));
        if (t != null)
            printStackTrace(t);
        System.exit(1);
    }

    private static void print( String s )
    {
        OUTPUT.print(s);
    }

    private static void println()
    {
        OUTPUT.println();
    }

    private static void println( String s )
    {
        OUTPUT.println(s);
    }

    private static void printf( String format, Object... args )
    {
        OUTPUT.printf(format, args);
    }

    private static void printStackTrace( Throwable t )
    {
        t.printStackTrace(OUTPUT);
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    private static final class Topo
    {
        static Topo of( XMLTopologyReply topoReply )
        {
            return new Topo(
                CollectionUtils.toMap(topoReply.getNodes(), Node::getId, Function.identity()),
                CollectionUtils.toMap(topoReply.getLinks(), Link::getId, Function.identity()));
        }

        private final Map<String, Node> nodes;
        private final Map<String, Link> links;

        private Topo( Map<String, Node> nodes, Map<String, Link> links )
        {
            this.nodes = nodes;
            this.links = links;
        }

        boolean hasNode( String nodeId )
        {
            return nodes.containsKey(nodeId);
        }

        boolean hasLink( String linkId )
        {
            return links.containsKey(linkId);
        }

        Node getNode( String nodeId )
        {
            Node node = nodes.get(nodeId);
            if (node != null)
                return node;
            else
                throw new IllegalArgumentException(String.format("node '%s' is not in topology", nodeId));
        }

        Link getLink( String linkId )
        {
            Link link = links.get(linkId);
            if (link != null)
                return link;
            else
                throw new IllegalArgumentException(String.format("link '%s' is not in topology", linkId));
        }

        LinkDirection getLinkDirection( String linkDir )
        {
            return LinkDirection.valueOf(linkDir.toUpperCase());
        }
    }

    private XMLSimpleClient()
    {
        // not used
    }
}
