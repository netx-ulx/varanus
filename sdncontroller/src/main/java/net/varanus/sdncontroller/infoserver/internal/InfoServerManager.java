package net.varanus.sdncontroller.infoserver.internal;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.time.Instant;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.slf4j.Logger;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;

import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.packet.IPacket;
import net.varanus.infoprotocol.GenericReply;
import net.varanus.infoprotocol.GenericRequest;
import net.varanus.infoprotocol.PacketReply;
import net.varanus.infoprotocol.PacketRequest;
import net.varanus.infoprotocol.RouteReply;
import net.varanus.infoprotocol.RouteRequest;
import net.varanus.infoprotocol.ServerCommunicator;
import net.varanus.infoprotocol.StatisticsReply;
import net.varanus.infoprotocol.StatisticsRequest;
import net.varanus.infoprotocol.TopologyReply;
import net.varanus.sdncontroller.activeforwarding.IActiveForwardingService;
import net.varanus.sdncontroller.alias.IAliasService;
import net.varanus.sdncontroller.linkstats.FlowedLinkStats;
import net.varanus.sdncontroller.linkstats.GeneralLinkStats;
import net.varanus.sdncontroller.linkstats.ILinkStatsService;
import net.varanus.sdncontroller.logging.Logging;
import net.varanus.sdncontroller.monitoring.IMonitoringService;
import net.varanus.sdncontroller.qosrouting.FlowedRoute;
import net.varanus.sdncontroller.topologygraph.ITopologyGraphService;
import net.varanus.sdncontroller.topologygraph.ITopologyGraphService.TopologySnapshot;
import net.varanus.sdncontroller.types.DatapathLink;
import net.varanus.sdncontroller.types.FlowedConnection;
import net.varanus.sdncontroller.types.FlowedLink;
import net.varanus.sdncontroller.util.IPacketUtils;
import net.varanus.sdncontroller.util.MetricSummary;
import net.varanus.sdncontroller.util.RatioSummary;
import net.varanus.sdncontroller.util.TimeSummary;
import net.varanus.sdncontroller.util.module.IModuleManager;
import net.varanus.sdncontroller.util.module.ModuleUtils;
import net.varanus.sdncontroller.util.stats.Stat;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.collect.builder.ImmutableListBuilder;
import net.varanus.util.concurrent.ConcurrentService;
import net.varanus.util.concurrent.InterruptibleRunnable;
import net.varanus.util.functional.PossibleDouble;
import net.varanus.util.io.ByteBuffers;
import net.varanus.util.io.CloseableList;
import net.varanus.util.io.NetworkChannelUtils;
import net.varanus.util.io.SelectorProxy;
import net.varanus.util.io.exception.IOChannelAcceptException;
import net.varanus.util.io.exception.IOChannelReadException;
import net.varanus.util.io.exception.IOChannelWriteException;
import net.varanus.util.io.exception.IOSelectException;
import net.varanus.util.lang.Comparables;
import net.varanus.util.openflow.types.BidiNodePorts;
import net.varanus.util.openflow.types.Flow;
import net.varanus.util.openflow.types.NodeId;
import net.varanus.util.openflow.types.UnidiNodePorts;
import net.varanus.util.time.TimeDouble;
import net.varanus.util.unitvalue.si.InfoDouble;
import net.varanus.util.unitvalue.si.MetricDouble;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class InfoServerManager implements IModuleManager
{
    private static final Logger LOG = Logging.infoserver.LOG;

    private final InfoServer server;

    public InfoServerManager()
    {
        this.server = new InfoServer();
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies()
    {
        return ModuleUtils.services(
            IAliasService.class,
            ITopologyGraphService.class,
            ILinkStatsService.class,
            IMonitoringService.class,
            IActiveForwardingService.class);
    }

    @Override
    public void init( FloodlightModuleContext context, Class<? extends IFloodlightModule> moduleClass )
        throws FloodlightModuleException
    {
        server.init(context, moduleClass);
    }

    @Override
    public void startUp( FloodlightModuleContext context, Class<? extends IFloodlightModule> moduleClass )
        throws FloodlightModuleException
    {
        server.start();
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    private static final class InfoServer extends ConcurrentService
    {
        private final ExecutorService infoConnsExec;

        private @Nullable SocketAddress            localAddress;
        private @Nullable IAliasService            aliasService;
        private @Nullable ITopologyGraphService    topoGraphService;
        private @Nullable ILinkStatsService        linkStatsService;
        private @Nullable IMonitoringService       monitService;
        private @Nullable IActiveForwardingService activeFwdService;

        InfoServer()
        {
            super(( msg, ex ) -> LOG.error(msg, ex));

            this.infoConnsExec = Executors.newCachedThreadPool();
        }

        void init( FloodlightModuleContext context, Class<? extends IFloodlightModule> moduleClass )
            throws FloodlightModuleException
        {
            Map<String, String> params = context.getConfigParams(moduleClass);
            this.localAddress = new InetSocketAddress(Props.getLocalPort(params));

            this.aliasService = ModuleUtils.getServiceImpl(context, IAliasService.class);
            this.topoGraphService = ModuleUtils.getServiceImpl(context, ITopologyGraphService.class);
            this.linkStatsService = ModuleUtils.getServiceImpl(context, ILinkStatsService.class);
            this.monitService = ModuleUtils.getServiceImpl(context, IMonitoringService.class);
            this.activeFwdService = ModuleUtils.getServiceImpl(context, IActiveForwardingService.class);
        }

        @Override
        protected void startUp()
        {
            LOG.info("Using local address {} for the info server", localAddress);
            LOG.debug("Starting info server");
        }

        @Override
        protected void shutDown()
        {
            LOG.debug("Shutting down info server");
            infoConnsExec.shutdownNow();
        }

        @Override
        public void runInterruptibly() throws InterruptedException
        {
            try (SelectorProxy sel = SelectorProxy.open()) {
                ServerSocketChannel srvCh = ServerSocketChannel.open();
                sel.registerChannel(srvCh, SelectionKey.OP_ACCEPT, null);
                srvCh.bind(localAddress);

                LOG.info("Listening to info connections...");
                runServerLoop(sel);
            }
            catch (IOSelectException e) {
                LOG.error(String.format("!!! IO-SELECT error in info server: %s", e.getMessage()), e);
            }
            catch (IOChannelAcceptException e) {
                e.checkInterruptStatus();
                LOG.error(String.format("!!! IO-ACCEPT error in info server: %s", e.getMessage()), e);
            }
            catch (IOException e) {
                LOG.error(String.format("!!! IO error in info server: %s", e.getMessage()), e);
            }
        }

        // NOTE: this method never returns normally, it always returns from a
        // thrown exception
        private void runServerLoop( SelectorProxy sel )
            throws InterruptedException,
            IOSelectException,
            IOChannelAcceptException
        {
            while (true) {
                final int numSel = sel.selectInterruptibly();
                if (numSel > 0) {
                    Set<SelectionKey> selected = sel.selectedKeys();
                    Iterator<SelectionKey> iter = Iterators.consumingIterator(selected.iterator());
                    while (iter.hasNext()) {
                        SelectionKey key = iter.next(); // key is also removed
                        if (key.isAcceptable()) {
                            ServerSocketChannel srvCh = (ServerSocketChannel)key.channel();
                            SocketChannel ch = NetworkChannelUtils.accept(srvCh);
                            if (ch != null) {
                                infoConnsExec.execute(new InfoWorker(ch));
                            }
                        }
                    }
                }
            }
        }

        @FieldsAreNonnullByDefault
        @ParametersAreNonnullByDefault
        @ReturnValuesAreNonnullByDefault
        private final class InfoWorker implements InterruptibleRunnable
        {
            private final SocketChannel ch;

            InfoWorker( SocketChannel ch )
            {
                this.ch = ch;
            }

            @Override
            public void runInterruptibly() throws InterruptedException
            {
                try (CloseableList cl = new CloseableList()) {
                    cl.add(ch);
                    LOG.info("Accepted new info connection from {}", NetworkChannelUtils.getRemoteAddress(ch));

                    ServerCommunicator comm = ServerCommunicator.create(ch, aliasService::getSwitchAlias, LOG);
                    while (true) {
                        LOG.debug("Waiting for info requests...");
                        GenericRequest genReq = comm.receiveRequest();
                        switch (genReq.getType()) {
                            case TOPOLOGY_REQUEST: {
                                // TopologyRequest req = genReq.forTopology();
                                LOG.debug("Received topology request");

                                TopologyReply reply = onTopologyRequest();

                                LOG.debug("Sending topology reply...");
                                comm.sendReply(GenericReply.fromTopology(reply));
                                LOG.debug("Topology reply sent");
                            }
                            break;

                            case STATISTICS_REQUEST: {
                                StatisticsRequest req = genReq.forStatistics();
                                LOG.debug("Received statistics request for link {}", req.getLink());

                                StatisticsReply reply = onStatisticsRequest(req);

                                LOG.debug("Sending statistics reply...");
                                comm.sendReply(GenericReply.fromStatistics(reply));
                                LOG.debug("Statistics reply sent");
                            }
                            break;

                            case ROUTE_REQUEST: {
                                RouteRequest req = genReq.forRoute();
                                LOG.debug("Received route request for connection {}", req.getConnection());

                                RouteReply reply = onRouteRequest(req);

                                LOG.debug("Sending route reply...");
                                comm.sendReply(GenericReply.fromRoute(reply));
                                LOG.debug("Route reply sent");
                            }
                            break;

                            case PACKET_REQUEST: {
                                PacketRequest req = genReq.forPacket();
                                LOG.debug("Received packet request for flow {}", req.getFlow());

                                PacketReply reply = onPacketRequest(req);

                                LOG.debug("Sending packet reply...");
                                comm.sendReply(GenericReply.fromPacket(reply));
                                LOG.debug("packet reply sent");
                            }
                            break;

                            default:
                                LOG.warn("! Received unsupported info operation {}", genReq.getType());
                            break;
                        }
                    }
                }
                catch (IOChannelReadException e) {
                    e.checkInterruptStatus();
                    LOG.warn("! IO-READ exception in info connection: {}", e.getMessage());
                }
                catch (IOChannelWriteException e) {
                    e.checkInterruptStatus();
                    LOG.warn("! IO-WRITE exception in info connection: {}", e.getMessage());
                }
                catch (IOException e) {
                    LOG.error(String.format("!!! IO error while closing info connection: %s", e.getMessage()), e);
                }
            }

            private TopologyReply onTopologyRequest()
            {
                ImmutableListBuilder<NodeId> switches = ImmutableListBuilder.create();
                ImmutableListBuilder<BidiNodePorts> links = ImmutableListBuilder.create();
                // filter duplicates
                Set<BidiNodePorts> knownLinks = new HashSet<>();
                TopologySnapshot snap = topoGraphService.getSnapshot();
                for (NodeId nodeId : snap.getGraph().vertexSet()) {
                    switches.add(nodeId);
                }
                for (DatapathLink link : snap.getGraph().edgeSet()) {
                    BidiNodePorts bidiLink = link.toBidiNodePorts();
                    if (knownLinks.add(bidiLink))
                        links.add(bidiLink);
                }

                return TopologyReply.of(switches.build(), links.build(), snap.getDisabledBidiLinks());
            }

            private StatisticsReply onStatisticsRequest( StatisticsRequest req )
            {
                FlowedLink link = FlowedLink.of(req.getLink());

                try {
                    monitService.validateMonitorableFlow(link.getFlow());
                }
                catch (IllegalArgumentException e) {
                    return StatisticsReply.ofError(e.getMessage());
                }

                if (monitService.startMonitoring(link.getFlow()))
                    LOG.debug("Started monitoring flow {}", link.getFlow());

                Optional<FlowedLinkStats> fStats = linkStatsService.getFlowedStats(link);
                if (fStats.isPresent()) {
                    return newStatsReply(req, fStats.get());
                }
                else {
                    Optional<GeneralLinkStats> gStats = linkStatsService.getGeneralStats(link.unflowed());
                    if (gStats.isPresent()) {
                        return newStatsReply(req, gStats.get());
                    }
                    else if (!req.doErrorOnUnknownLink()) {
                        return StatisticsReply.empty(req.getLink(), Instant.now());
                    }
                    else {
                        return StatisticsReply.ofError("statistics unavailable for requested link");
                    }
                }
            }

            private RouteReply onRouteRequest( RouteRequest req )
            {
                FlowedConnection conn = FlowedConnection.of(req.getConnection());

                Optional<FlowedRoute> route = activeFwdService.getActiveRoute(conn);
                if (route.isPresent()) {
                    ImmutableList<UnidiNodePorts> links = ImmutableListBuilder.<UnidiNodePorts>create()
                        .addEach(
                            route.get().getPath().unflowed().getLinks().stream().map(DatapathLink::toUnidiNodePorts))
                        .build();
                    return RouteReply.of(req.getConnection(), links);
                }
                else {
                    return RouteReply.ofError("route unavailable for requested connection");
                }
            }

            private PacketReply onPacketRequest( PacketRequest req )
            {
                Flow flow = req.getFlow();
                byte[] payload = ByteBuffers.getArrayCopy(req.getPayload());

                IPacket packet = IPacketUtils.fromMatch(flow.getMatch(), payload);
                return PacketReply.of(packet.serialize());
            }

            private StatisticsReply newStatsReply( StatisticsRequest req, GeneralLinkStats stats )
            {
                Stat<TimeSummary> latStat = stats.getLatency();
                TimeSummary latSumm = latStat.value();
                TimeDouble latency = latSumm.isPresent() ? latSumm.getMean() : TimeDouble.absent();
                boolean isProbeLatency = latency.isPresent();

                Stat<RatioSummary> lossStat = stats.getByteLoss();
                RatioSummary lossSumm = lossStat.value();
                PossibleDouble byteLoss = lossSumm.isPresent() ? PossibleDouble.of(lossSumm.getMean().doubleValue())
                                                               : PossibleDouble.absent();
                boolean isProbeLoss = byteLoss.isPresent();

                InfoDouble throughput = InfoDouble.absent();
                InfoDouble txRate = InfoDouble.absent();
                InfoDouble recRate = InfoDouble.absent();
                InfoDouble unmatchedTxRate = InfoDouble.absent();
                InfoDouble unmatchedRecRate = InfoDouble.absent();

                Stat<MetricSummary> srcPktDropRateStat = stats.getSourcePacketDropRate();
                Stat<MetricSummary> destPktDropRateStat = stats.getDestinationPacketDropRate();
                MetricDouble srcPktDropRate = srcPktDropRateStat.value().getMean();
                MetricDouble destPktDropRate = destPktDropRateStat.value().getMean();

                Instant timestamp = Comparables.max(latStat.timestamp(), lossStat.timestamp(),
                    srcPktDropRateStat.timestamp(), destPktDropRateStat.timestamp());

                return StatisticsReply.of(
                    req.getLink(),
                    latency, isProbeLatency,
                    byteLoss, isProbeLoss,
                    throughput,
                    txRate,
                    recRate,
                    unmatchedTxRate,
                    unmatchedRecRate,
                    srcPktDropRate,
                    destPktDropRate,
                    timestamp);
            }

            private StatisticsReply newStatsReply( StatisticsRequest req, FlowedLinkStats stats )
            {
                Stat<TimeSummary> latStat = stats.getLatency();
                TimeSummary latSumm = latStat.value();
                TimeDouble latency = latSumm.isPresent() ? latSumm.getMean() : TimeDouble.absent();
                boolean isProbeLatency = latSumm.isPresent() && !stats.trajectory().getLatency().value().isPresent();

                Stat<RatioSummary> lossStat = stats.getByteLoss();
                RatioSummary lossSumm = lossStat.value();
                PossibleDouble byteLoss = lossSumm.isPresent() ? PossibleDouble.of(lossSumm.getMean().doubleValue())
                                                               : PossibleDouble.absent();
                boolean isProbeLoss = lossSumm.isPresent() && !stats.trajectory().getByteLoss().value().isPresent();

                Stat<InfoDouble> thrptStat = stats.getThroughput();
                Stat<InfoDouble> txRateStat = stats.trajectory().getDataTransmissionRate();
                Stat<InfoDouble> recRateStat = stats.trajectory().getDataReceptionRate();
                Stat<InfoDouble> umtchTxRateStat = stats.trajectory().getUnmatchedDataTransmissionRate();
                Stat<InfoDouble> umtchRecRateStat = stats.trajectory().getUnmatchedDataReceptionRate();

                InfoDouble throughput = thrptStat.value();
                InfoDouble txRate = txRateStat.value();
                InfoDouble recRate = recRateStat.value();
                InfoDouble umtchTxRate = umtchTxRateStat.value();
                InfoDouble umtchRecRate = umtchRecRateStat.value();

                Stat<MetricSummary> srcPktDropRateStat = stats.general().getSourcePacketDropRate();
                Stat<MetricSummary> destPktDropRateStat = stats.general().getDestinationPacketDropRate();
                MetricDouble srcPktDropRate = srcPktDropRateStat.value().getMean();
                MetricDouble destPktDropRate = destPktDropRateStat.value().getMean();

                Instant timestamp = Comparables.max(
                    latStat.timestamp(), lossStat.timestamp(),
                    thrptStat.timestamp(),
                    txRateStat.timestamp(), recRateStat.timestamp(),
                    umtchTxRateStat.timestamp(), umtchRecRateStat.timestamp(),
                    srcPktDropRateStat.timestamp(), destPktDropRateStat.timestamp());

                return StatisticsReply.of(
                    req.getLink(),
                    latency, isProbeLatency,
                    byteLoss, isProbeLoss,
                    throughput,
                    txRate,
                    recRate,
                    umtchTxRate,
                    umtchRecRate,
                    srcPktDropRate,
                    destPktDropRate,
                    timestamp);
            }
        }
    }
}
