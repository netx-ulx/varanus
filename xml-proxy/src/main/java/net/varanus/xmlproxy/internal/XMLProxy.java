package net.varanus.xmlproxy.internal;


import java.io.IOException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import net.varanus.configprotocol.LinkEnablingReply;
import net.varanus.infoprotocol.PacketReply;
import net.varanus.infoprotocol.RouteReply;
import net.varanus.infoprotocol.StatisticsReply;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.collect.CommonPair;
import net.varanus.util.collect.Pair;
import net.varanus.util.concurrent.ConcurrencyUtils;
import net.varanus.util.concurrent.ConcurrentService;
import net.varanus.util.concurrent.InterruptibleRunnable;
import net.varanus.util.functional.Report;
import net.varanus.util.io.ByteBuffers;
import net.varanus.util.io.NetworkChannelUtils;
import net.varanus.util.io.exception.IOChannelAcceptException;
import net.varanus.util.io.exception.IOChannelReadException;
import net.varanus.util.openflow.types.BidiNodePorts;
import net.varanus.util.openflow.types.Flow;
import net.varanus.util.openflow.types.FlowedUnidiNodePorts;
import net.varanus.util.text.StringUtils;
import net.varanus.util.time.TimeDouble;
import net.varanus.util.unitvalue.si.InfoDouble;
import net.varanus.util.unitvalue.si.InfoLong;
import net.varanus.xmlproxy.XMLProxyConfig;
import net.varanus.xmlproxy.internal.MininetLinkInfo.BandwidthQoS;
import net.varanus.xmlproxy.internal.MininetLinkInfo.NetemQoS;
import net.varanus.xmlproxy.internal.TopologyCacher.TopologyState;
import net.varanus.xmlproxy.util.XMLPacketSender;
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


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
public final class XMLProxy
{
    private static final Logger LOG = LoggerFactory.getLogger(XMLProxy.class);

    private final Proxy proxy;

    public XMLProxy( XMLProxyConfig config ) throws JAXBException
    {
        this.proxy = new Proxy(config);
    }

    public void start() throws IOException
    {
        LOG.debug("-- Starting XML proxy --");
        try {
            proxy.start().get();
        }
        catch (InterruptedException e) {/* ignore */ }
        catch (ExecutionException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof IOException) {
                throw (IOException)cause;
            }
            else {
                throw new RuntimeException(cause);
            }
        }
    }

    public void waitForShutdown()
    {
        ConcurrencyUtils.runUntilInterrupted(proxy::waitForShutdown);
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    private static final class Proxy extends ConcurrentService
    {
        private final XMLProxyConfig config;
        private final JAXBContext    jc;
        private final InfoClient     infoClient;
        private final ConfigClient   cfgClient;
        private final MininetClient  mininetClient;
        private final TopologyCacher topoCacher;
        private final boolean        autoSetRouteMatch;

        private final ExecutorService executor;

        Proxy( XMLProxyConfig cfg ) throws JAXBException
        {
            super(( msg, ex ) -> LOG.error(msg, ex));

            this.config = cfg;
            this.jc = Helper.initContext();
            this.infoClient = new InfoClient(cfg.getInfoServerAddress(), cfg.getStatErrorOnUnknownLink());
            this.cfgClient = new ConfigClient(cfg.getConfigServerAddress());
            this.mininetClient = new MininetClient(cfg.getRemoteMininetAddress(), cfg.getCommandOutputAddress());
            this.topoCacher = new TopologyCacher(infoClient, cfgClient, mininetClient, cfg.getTopologyUpdatePeriod());
            this.autoSetRouteMatch = cfg.isAutoSetRouteMatch();

            this.executor = Executors.newCachedThreadPool();
        }

        @Override
        protected void startUp() throws Exception
        {
            config.log(LOG, "== Configuration ==");

            LOG.debug("Starting info-client");
            executor.execute(infoClient);

            LOG.debug("Starting config-client");
            executor.execute(cfgClient);

            LOG.debug("Starting mininet-client");
            executor.execute(mininetClient);

            LOG.debug("Starting topology cacher");
            executor.execute(topoCacher);
        }

        @Override
        protected void shutDown() throws Exception
        {
            // interrupt all proxy worker threads
            LOG.debug("Shutting down proxy workers");
            executor.shutdownNow();
        }

        @Override
        public void runInterruptibly() throws InterruptedException
        {
            LOG.debug("Starting proxy server");
            try (ServerSocketChannel servCh = ServerSocketChannel.open()) {
                servCh.bind(config.getLocalAddress());

                LOG.debug("Listening to proxy client connections...");
                runServerLoop(servCh);
            }
            catch (IOChannelAcceptException e) {
                e.checkInterruptStatus();
                LOG.error(String.format("!!! IO-ACCEPT error in proxy server: %s", e.getMessage()), e);
            }
            catch (IOException e) {
                LOG.error(String.format("!!! IO error in proxy server: %s", e.getMessage()), e);
            }
        }

        private void runServerLoop( ServerSocketChannel srvCh ) throws IOChannelAcceptException
        {
            while (true) {
                SocketChannel ch = NetworkChannelUtils.accept(srvCh);
                LOG.info("== Connection established with proxy client at remote address {} ==",
                    NetworkChannelUtils.getRemoteAddress(ch));
                executor.execute(new ServerWorker(ch));
            }
        }

        @FieldsAreNonnullByDefault
        @ParametersAreNonnullByDefault
        @ReturnValuesAreNonnullByDefault
        private final class ServerWorker implements InterruptibleRunnable
        {
            private final SocketChannel   ch;
            private final XMLPacketSender pktSender;

            ServerWorker( SocketChannel ch )
            {
                this.ch = ch;
                this.pktSender = new XMLPacketSender(ch, LOG);
            }

            @Override
            public void runInterruptibly() throws InterruptedException
            {
                try (SocketChannel _autoClose = ch) {
                    _autoClose.isOpen(); // ignored
                    pktSender.start();
                    while (true) {
                        XMLPacket pkt = XMLPacket.IO.reader().read(ch);
                        LOG.debug("Received XML request of type {}", pkt.getType());

                        switch (pkt.getType()) {
                            case REQUEST_TOPOLOGY: {
                                handleTopologyRequest();
                            }
                            break;

                            case REQUEST_STATISTICS: {
                                XMLStatRequest request = Helper.fromBytes(pkt.getData(), XMLStatRequest.class, jc);
                                handleStatisticsRequest(request);
                            }
                            break;

                            case REQUEST_ROUTE: {
                                XMLRouteRequest request = Helper.fromBytes(pkt.getData(), XMLRouteRequest.class, jc);
                                handleRouteRequest(request);
                            }
                            break;

                            case REQUEST_LINK_CONFIG: {
                                XMLLinkConfigRequest request =
                                    Helper.fromBytes(pkt.getData(), XMLLinkConfigRequest.class, jc);
                                handleLinkConfigRequest(request);
                            }
                            break;

                            case REQUEST_COMMAND: {
                                XMLCommandRequest request =
                                    Helper.fromBytes(pkt.getData(), XMLCommandRequest.class, jc);
                                handleCommandRequest(request);
                            }
                            break;

                            case REQUEST_LINK_STATE: {
                                XMLLinkStateRequest request =
                                    Helper.fromBytes(pkt.getData(), XMLLinkStateRequest.class, jc);
                                handleLinkStateRequest(request);
                            }
                            break;

                            case REQUEST_TRAFFIC_INJECT: {
                                XMLTrafficInjectRequest request =
                                    Helper.fromBytes(pkt.getData(), XMLTrafficInjectRequest.class, jc);
                                handleTrafficInjectRequest(request);
                            }
                            break;

                            default:
                                LOG.warn("! Received unsupported XML operation {}", pkt.getType());
                            break;
                        }
                    }
                }
                catch (IOChannelReadException e) {
                    e.checkInterruptStatus();
                    LOG.warn("! IO-READ exception in proxy client connection: {}", e.getMessage());
                }
                catch (JAXBException e) {
                    LOG.error(String.format("!!! JAXB error in proxy client connection: %s", e.getMessage()),
                        e);
                }
                catch (IOException e) {
                    LOG.error(String.format("!!! IO error while closing proxy client connection: %s", e.getMessage()),
                        e);
                }
                finally {
                    pktSender.stop();
                }
            }

            private void handleTopologyRequest() throws JAXBException, InterruptedException
            {
                Report<TopologyState> topoReport = topoCacher.getTopologyState();
                if (topoReport.hasValue())
                    sendTopologyReply(XML.buildXMLTopologyReply(topoReport.getValue()));
                else
                    sendTopologyError(XML.buildXMLTopologyError(topoReport.getError()));
            }

            private void handleStatisticsRequest( XMLStatRequest request ) throws InterruptedException, JAXBException
            {
                Report<TopologyState> topoReport = topoCacher.getTopologyState();
                if (topoReport.hasValue()) {
                    try {
                        FlowedUnidiNodePorts link = XML.getStatRequestLink(request, topoReport.getValue());
                        StatisticsReply reply = infoClient.requestStatistics(link).get();
                        if (reply.hasResult())
                            sendStatisticsReply(XML.buildXMLStatReply(request, reply));
                        else
                            sendStatisticsError(XML.buildXMLStatError(request, reply.getError()));
                    }
                    catch (IllegalArgumentException e) {
                        sendStatisticsError(XML.buildXMLStatError(request, e.getMessage()));
                    }
                    catch (ExecutionException e) {
                        sendStatisticsError(XML.buildXMLStatError(request,
                            StringUtils.getExceptionCauseString(e)));
                    }
                }
                else {
                    sendStatisticsError(XML.buildXMLStatError(request, topoReport.getError()));
                }
            }

            private void handleRouteRequest( XMLRouteRequest request ) throws InterruptedException, JAXBException
            {
                Report<TopologyState> topoReport = topoCacher.getTopologyState();
                if (topoReport.hasValue()) {
                    try {
                        FlowedUnidiNodePorts conn =
                            XML.getRouteRequestConnection(request, topoReport.getValue(), autoSetRouteMatch);
                        RouteReply reply = infoClient.requestRoute(conn).get();
                        if (reply.hasResult())
                            sendRouteReply(XML.buildXMLRouteReply(request, reply));
                        else
                            sendRouteError(XML.buildXMLRouteError(request, reply.getError()));
                    }
                    catch (IllegalArgumentException e) {
                        sendRouteError(XML.buildXMLRouteError(request, e.getMessage()));
                    }
                    catch (ExecutionException e) {
                        sendRouteError(XML.buildXMLRouteError(request,
                            StringUtils.getExceptionCauseString(e)));
                    }
                }
                else {
                    sendRouteError(XML.buildXMLRouteError(request, topoReport.getError()));
                }
            }

            private void handleLinkConfigRequest( XMLLinkConfigRequest request )
                throws InterruptedException,
                JAXBException
            {
                Report<TopologyState> topoReport = topoCacher.getTopologyState();
                if (topoReport.hasValue()) {
                    try {
                        MininetQoSSetup qosSetup = XML.getQoSSetup(request, topoReport.getValue());
                        topoReport = topoCacher.updateTopologyState(ImmutableList.of(qosSetup));
                        if (topoReport.hasValue()) {
                            BandwidthQoS bandQoS = getConfiguredBandwidthQoS(qosSetup, topoReport.getValue());
                            NetemQoS netemQoS = getConfiguredNetemQoS(qosSetup, topoReport.getValue());
                            sendLinkConfigReply(XML.buildXMLLinkConfigReply(request, bandQoS, netemQoS));
                        }
                        else {
                            sendLinkConfigError(XML.buildXMLLinkConfigError(request, topoReport.getError()));
                        }
                    }
                    catch (IllegalArgumentException e) {
                        sendLinkConfigError(XML.buildXMLLinkConfigError(request, e.getMessage()));
                    }
                }
                else {
                    sendLinkConfigError(XML.buildXMLLinkConfigError(request, topoReport.getError()));
                }
            }

            private void handleCommandRequest( XMLCommandRequest request ) throws InterruptedException, JAXBException
            {
                Report<TopologyState> topoReport = topoCacher.getTopologyState();
                if (topoReport.hasValue()) {
                    try {
                        CommonPair<MininetHost> hosts = XML.getCommandHosts(request, topoReport.getValue());
                        MininetHost srcHost = hosts.getFirst();
                        MininetHost destHost = hosts.getSecond();

                        if (request.getEnabled())
                            handleCommandStart(request, srcHost, destHost);
                        else
                            handleCommandStop(request, srcHost, destHost);
                    }
                    catch (IllegalArgumentException e) {
                        sendCommandError(XML.buildXMLCommandError(request, e.getMessage()));
                    }
                }
                else {
                    sendCommandError(XML.buildXMLCommandError(request, topoReport.getError()));
                }
            }

            private void handleCommandStart( XMLCommandRequest request, MininetHost srcHost, MininetHost destHost )
                throws InterruptedException,
                JAXBException,
                IllegalArgumentException
            {
                switch (request.getType()) {
                    case PING: {
                        CommandResultHandler<TimeDouble> handler =
                            ( latency ) -> handlePingResult(request, latency);
                        if (!mininetClient.startPing(srcHost, destHost, handler))
                            sendCommandError(XML.buildXMLCommandError(request,
                                String.format("cannot start an already active ping command on %s -> %s",
                                    srcHost.getName(), destHost.getName())));
                    }
                    break;

                    case IPERF: {
                        InfoDouble bandwidth = XML.getIperfCommandBandwidth(request);
                        CommandResultHandler<InfoLong> handler =
                            ( throughput ) -> handleIperfResult(request, throughput);
                        if (!mininetClient.startIperf(srcHost, destHost, bandwidth, handler))
                            sendCommandError(XML.buildXMLCommandError(request,
                                String.format("cannot start an already active iperf command on %s -> %s",
                                    srcHost.getName(), destHost.getName())));
                    }
                    break;

                    default:
                        throw new AssertionError("unexpected enum value");
                }
            }

            private void handleCommandStop( XMLCommandRequest request, MininetHost srcHost, MininetHost destHost )
                throws InterruptedException,
                JAXBException
            {
                switch (request.getType()) {
                    case PING:
                        if (mininetClient.stopPing(srcHost, destHost)) {
                            sendCommandReply(XML.buildXMLPingCommandReply(request, false, TimeDouble.absent()));
                        }
                        else {
                            sendCommandError(XML.buildXMLCommandError(request,
                                String.format("cannot stop an inactive ping command on %s -> %s",
                                    srcHost.getName(), destHost.getName())));
                        }
                    break;

                    case IPERF:
                        if (mininetClient.stopIperf(srcHost, destHost)) {
                            sendCommandReply(XML.buildXMLIperfCommandReply(request, false, InfoLong.absent()));
                        }
                        else {
                            sendCommandError(XML.buildXMLCommandError(request,
                                String.format("cannot stop an inactive iperf command on %s -> %s",
                                    srcHost.getName(), destHost.getName())));
                        }
                    break;

                    default:
                        throw new AssertionError("unexpected enum value");
                }
            }

            private void handlePingResult( XMLCommandRequest request, Report<TimeDouble> latency )
                throws InterruptedException
            {
                try {
                    if (latency.hasValue()) {
                        sendCommandReply(XML.buildXMLPingCommandReply(request, true, latency.getValue()));
                    }
                    else {
                        sendCommandError(XML.buildXMLCommandError(request, latency.getError()));
                    }
                }
                catch (JAXBException e) {
                    LOG.error(String.format("!!! JAXB error in ping result handler: %s", e.getMessage()), e);
                }
            }

            private void handleIperfResult( XMLCommandRequest request, Report<InfoLong> throughput )
                throws InterruptedException
            {
                try {
                    if (throughput.hasValue()) {
                        sendCommandReply(XML.buildXMLIperfCommandReply(request, true, throughput.getValue()));
                    }
                    else {
                        sendCommandError(XML.buildXMLCommandError(request, throughput.getError()));
                    }
                }
                catch (JAXBException e) {
                    LOG.error(String.format("!!! JAXB error in iperf result handler: %s", e.getMessage()), e);
                }
            }

            private void handleLinkStateRequest( XMLLinkStateRequest request )
                throws InterruptedException,
                JAXBException
            {
                Report<TopologyState> topoReport = topoCacher.getTopologyState();
                if (topoReport.hasValue()) {
                    try {
                        Pair<BidiNodePorts, Boolean> p = XML.getLinkAndStateOp(request, topoReport.getValue());
                        BidiNodePorts link = p.getFirst();
                        boolean enable = p.getSecond();

                        try {
                            LinkEnablingReply reply = cfgClient.requestLinkEnabling(link, enable).get();
                            if (reply.hasError()) {
                                sendLinkStateError(XML.buildXMLLinkStateError(request, reply.getError()));
                            }
                            else {
                                // implicit in config-protocol
                                boolean enabled = enable;
                                sendLinkStateReply(XML.buildXMLLinkStateReply(request, enabled));
                            }
                        }
                        catch (ExecutionException e) {
                            sendLinkStateError(
                                XML.buildXMLLinkStateError(request, StringUtils.getExceptionCauseString(e)));
                        }
                    }
                    catch (IllegalArgumentException e) {
                        sendLinkStateError(XML.buildXMLLinkStateError(request, e.getMessage()));
                    }
                }
                else {
                    sendLinkStateError(XML.buildXMLLinkStateError(request, topoReport.getError()));
                }
            }

            private void handleTrafficInjectRequest( XMLTrafficInjectRequest request )
                throws InterruptedException,
                JAXBException
            {
                Report<TopologyState> topoReport = topoCacher.getTopologyState();
                if (topoReport.hasValue()) {
                    try {
                        Pair<MininetLinkInfo, Boolean> p = XML.getLinkInfoAndInjectOp(request, topoReport.getValue());
                        MininetLinkInfo linkInfo = p.getFirst();
                        boolean enable = p.getSecond();

                        if (enable)
                            enableTrafficInjection(request, linkInfo);
                        else
                            disableTrafficInjection(request, linkInfo);
                    }
                    catch (IllegalArgumentException e) {
                        sendTrafficInjectError(XML.buildXMLTrafficInjectError(request, e.getMessage()));
                    }
                }
                else {
                    sendTrafficInjectError(XML.buildXMLTrafficInjectError(request, topoReport.getError()));
                }
            }

            private BandwidthQoS getConfiguredBandwidthQoS( MininetQoSSetup qosSetup, TopologyState topoState )
            {
                return topoState.getMininetLinkInfo(qosSetup.getSrcName(), qosSetup.getDestName())
                    .map(MininetLinkInfo::getBandwidthQoS)
                    .orElseThrow(() -> new IllegalStateException("illegal topology state"));
            }

            private NetemQoS getConfiguredNetemQoS( MininetQoSSetup qosSetup, TopologyState topoState )
            {
                return topoState.getMininetLinkInfo(qosSetup.getSrcName(), qosSetup.getDestName())
                    .map(MininetLinkInfo::getNetemQoS)
                    .orElseThrow(() -> new IllegalStateException("illegal topology state"));
            }

            private void enableTrafficInjection( XMLTrafficInjectRequest request, MininetLinkInfo linkInfo )
                throws InterruptedException,
                JAXBException
            {
                Pair<Flow, InfoDouble> p2 = XML.getInjectFlowAndBandwidth(request);
                Flow flow = p2.getFirst();
                InfoDouble bandwidth = p2.getSecond();
                try {
                    PacketReply pktReply = infoClient.requestPacket(flow).get();
                    if (pktReply.hasError()) {
                        sendTrafficInjectError(XML.buildXMLTrafficInjectError(
                            request, pktReply.getError()));
                    }
                    else {
                        byte[] packet = ByteBuffers.getArrayCopy(pktReply.getPacket());
                        if (mininetClient.startTcpReplay(linkInfo, packet, bandwidth)) {
                            sendTrafficInjectReply(XML.buildXMLTrafficInjectReply(
                                request, true, Optional.of(flow), bandwidth));
                        }
                        else {
                            sendTrafficInjectError(XML.buildXMLTrafficInjectError(request,
                                String.format("cannot start an already active traffic injection on %s -> %s",
                                    linkInfo.getSrcName(), linkInfo.getDestName())));
                        }
                    }

                }
                catch (ExecutionException e) {
                    sendTrafficInjectError(XML.buildXMLTrafficInjectError(
                        request, StringUtils.getExceptionCauseString(e)));
                }
            }

            private void disableTrafficInjection( XMLTrafficInjectRequest request, MininetLinkInfo linkInfo )
                throws InterruptedException,
                JAXBException
            {
                if (mininetClient.stopTcpReplay(linkInfo)) {
                    sendTrafficInjectReply(XML.buildXMLTrafficInjectReply(
                        request, false, Optional.empty(), InfoDouble.absent()));
                }
                else {
                    sendTrafficInjectError(XML.buildXMLTrafficInjectError(request,
                        String.format("cannot stop an inactive traffic injection on %s -> %s",
                            linkInfo.getSrcName(), linkInfo.getDestName())));
                }
            }

            private void sendTopologyReply( XMLTopologyReply reply ) throws JAXBException, InterruptedException
            {
                byte[] data = Helper.toBytes(reply, jc);
                if (LOG.isDebugEnabled())
                    LOG.debug("Sending XML topology reply to proxy client:{}{}",
                        System.lineSeparator(), Helper.toString(reply, jc));
                pktSender.send(XMLPacket.newTopologyReply(data));
            }

            private void sendTopologyError( XMLTopologyError error ) throws JAXBException, InterruptedException
            {
                byte[] data = Helper.toBytes(error, jc);
                if (LOG.isWarnEnabled())
                    LOG.warn("! Sending XML topology error to proxy client:{}{}",
                        System.lineSeparator(), Helper.toString(error, jc));
                pktSender.send(XMLPacket.newTopologyFailure(data));
            }

            private void sendStatisticsReply( XMLStatReply reply ) throws JAXBException, InterruptedException
            {
                byte[] data = Helper.toBytes(reply, jc);
                if (LOG.isDebugEnabled())
                    LOG.debug("Sending XML statistics reply to proxy client:{}{}",
                        System.lineSeparator(), Helper.toString(reply, jc));
                pktSender.send(XMLPacket.newStatisticsReply(data));
            }

            private void sendStatisticsError( XMLStatError error ) throws JAXBException, InterruptedException
            {
                byte[] data = Helper.toBytes(error, jc);
                if (LOG.isWarnEnabled())
                    LOG.warn("! Sending XML statistics error to proxy client:{}{}",
                        System.lineSeparator(), Helper.toString(error, jc));
                pktSender.send(XMLPacket.newStatisticsFailure(data));
            }

            private void sendRouteReply( XMLRouteReply reply ) throws JAXBException, InterruptedException
            {
                byte[] data = Helper.toBytes(reply, jc);
                if (LOG.isDebugEnabled())
                    LOG.debug("Sending XML route reply to proxy client:{}{}",
                        System.lineSeparator(), Helper.toString(reply, jc));
                pktSender.send(XMLPacket.newRouteReply(data));
            }

            private void sendRouteError( XMLRouteError error ) throws JAXBException, InterruptedException
            {
                byte[] data = Helper.toBytes(error, jc);
                if (LOG.isWarnEnabled())
                    LOG.warn("! Sending XML route error to proxy client:{}{}",
                        System.lineSeparator(), Helper.toString(error, jc));
                pktSender.send(XMLPacket.newRouteFailure(data));
            }

            private void sendLinkConfigReply( XMLLinkConfigReply reply ) throws JAXBException, InterruptedException
            {
                byte[] data = Helper.toBytes(reply, jc);
                if (LOG.isDebugEnabled())
                    LOG.debug("Sending XML link-config reply to proxy client:{}{}",
                        System.lineSeparator(), Helper.toString(reply, jc));
                pktSender.send(XMLPacket.newLinkConfigReply(data));
            }

            private void sendLinkConfigError( XMLLinkConfigError error ) throws JAXBException, InterruptedException
            {
                byte[] data = Helper.toBytes(error, jc);
                if (LOG.isWarnEnabled())
                    LOG.warn("! Sending XML link-config error to proxy client:{}{}",
                        System.lineSeparator(), Helper.toString(error, jc));
                pktSender.send(XMLPacket.newLinkConfigFailure(data));
            }

            private void sendCommandReply( XMLCommandReply reply ) throws JAXBException, InterruptedException
            {
                byte[] data = Helper.toBytes(reply, jc);
                if (LOG.isDebugEnabled())
                    LOG.debug("Sending XML command reply to proxy client:{}{}",
                        System.lineSeparator(), Helper.toString(reply, jc));
                pktSender.send(XMLPacket.newCommandReply(data));
            }

            private void sendCommandError( XMLCommandError error ) throws JAXBException, InterruptedException
            {
                byte[] data = Helper.toBytes(error, jc);
                if (LOG.isWarnEnabled())
                    LOG.warn("! Sending XML command error to proxy client:{}{}",
                        System.lineSeparator(), Helper.toString(error, jc));
                pktSender.send(XMLPacket.newCommandFailure(data));
            }

            private void sendLinkStateReply( XMLLinkStateReply reply ) throws JAXBException, InterruptedException
            {
                byte[] data = Helper.toBytes(reply, jc);
                if (LOG.isDebugEnabled())
                    LOG.debug("Sending XML link-state reply to proxy client:{}{}",
                        System.lineSeparator(), Helper.toString(reply, jc));
                pktSender.send(XMLPacket.newLinkStateReply(data));
            }

            private void sendLinkStateError( XMLLinkStateError error ) throws JAXBException, InterruptedException
            {
                byte[] data = Helper.toBytes(error, jc);
                if (LOG.isWarnEnabled())
                    LOG.warn("! Sending XML link-state error to proxy client:{}{}",
                        System.lineSeparator(), Helper.toString(error, jc));
                pktSender.send(XMLPacket.newLinkStateFailure(data));
            }

            private void sendTrafficInjectReply( XMLTrafficInjectReply reply )
                throws JAXBException,
                InterruptedException
            {
                byte[] data = Helper.toBytes(reply, jc);
                if (LOG.isDebugEnabled())
                    LOG.debug("Sending XML traffic-inject reply to proxy client:{}{}",
                        System.lineSeparator(), Helper.toString(reply, jc));
                pktSender.send(XMLPacket.newTrafficInjectReply(data));
            }

            private void sendTrafficInjectError( XMLTrafficInjectError error )
                throws JAXBException,
                InterruptedException
            {
                byte[] data = Helper.toBytes(error, jc);
                if (LOG.isWarnEnabled())
                    LOG.warn("! Sending XML traffic-inject error to proxy client:{}{}",
                        System.lineSeparator(), Helper.toString(error, jc));
                pktSender.send(XMLPacket.newTrafficInjectFailure(data));
            }
        }
    }
}
