package net.varanus.sdncontroller.monitoring.submodules.collectorhandler.internal;


import static net.varanus.mirroringprotocol.CollectionType.PROBING;
import static net.varanus.mirroringprotocol.CollectionType.SAMPLING;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.ByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import org.slf4j.Logger;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterators;
import com.google.common.io.Closeables;

import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.varanus.mirroringprotocol.CollectionType;
import net.varanus.mirroringprotocol.ControllerCommunicator;
import net.varanus.mirroringprotocol.GenericReply;
import net.varanus.mirroringprotocol.GenericRequest;
import net.varanus.mirroringprotocol.MirroringConfig;
import net.varanus.mirroringprotocol.ProbingReply;
import net.varanus.mirroringprotocol.ProbingRequest;
import net.varanus.mirroringprotocol.SamplingReply;
import net.varanus.mirroringprotocol.SamplingRequest;
import net.varanus.mirroringprotocol.util.CollectorId;
import net.varanus.mirroringprotocol.util.TimedPacketSummary;
import net.varanus.sdncontroller.alias.IAliasService;
import net.varanus.sdncontroller.linkstats.sample.SecureProbingSample;
import net.varanus.sdncontroller.linkstats.sample.TrajectorySample;
import net.varanus.sdncontroller.logging.Logging;
import net.varanus.sdncontroller.monitoring.internal.IMonitoringModuleContext;
import net.varanus.sdncontroller.monitoring.submodules.collectorhandler.ICollectorHandlerService;
import net.varanus.sdncontroller.monitoring.util.ISubmoduleManager;
import net.varanus.sdncontroller.topologygraph.ITopologyGraphService;
import net.varanus.sdncontroller.types.DatapathLink;
import net.varanus.sdncontroller.types.FlowedLink;
import net.varanus.sdncontroller.util.module.ModuleUtils;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.concurrent.ConcurrentService;
import net.varanus.util.concurrent.InterruptibleRunnable;
import net.varanus.util.functional.Possible;
import net.varanus.util.io.NetworkChannelUtils;
import net.varanus.util.io.SelectorProxy;
import net.varanus.util.io.exception.IOChannelAcceptException;
import net.varanus.util.io.exception.IOChannelReadException;
import net.varanus.util.io.exception.IOChannelWriteException;
import net.varanus.util.io.exception.IOSelectException;
import net.varanus.util.openflow.types.BitMatch;
import net.varanus.util.openflow.types.DirectedNodePort;
import net.varanus.util.openflow.types.Flow;
import net.varanus.util.openflow.types.FlowDirectedNodePort;
import net.varanus.util.openflow.types.NodeId;
import net.varanus.util.openflow.types.PortId;
import net.varanus.util.time.TimeUtils;
import net.varanus.util.time.Timed;


// TODO try to create more generalized code for both sampling and probing to
// reduce code size

/**
 *
 */
@FieldsAreNonnullByDefault
public class CollectorHandlerManager implements ISubmoduleManager, ICollectorHandlerService
{
    private static final Logger LOG = Logging.monitoring.collectorhandler.LOG;

    private final CollectorHandlerServer server;

    private @Nullable PortId samplingPort;

    public CollectorHandlerManager()
    {
        this.server = new CollectorHandlerServer();
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies()
    {
        return ModuleUtils.services(IAliasService.class, ITopologyGraphService.class);
    }

    @Override
    public void init( IMonitoringModuleContext context ) throws FloodlightModuleException
    {
        Map<String, String> params = context.getConfigParams();
        this.samplingPort = Props.getSamplingPort(params);

        server.init(context);
    }

    @Override
    public void startUp( IMonitoringModuleContext context ) throws FloodlightModuleException
    {
        LOG.info("Using sampling OF port {}", samplingPort);
        context.getServiceImpl(ITopologyGraphService.class).suppressLinkDiscovery(samplingPort);

        server.start();
    }

    @Override
    public boolean hasCollector( NodeId nodeId )
    {
        return server.hasWorkerFor(Objects.requireNonNull(nodeId));
    }

    @Override
    public CompletableFuture<TrajectorySample> sendSamplingRequest( FlowedLink flowedLink, Duration collDuration )
    {
        return server.requestSamplingCollection(
            Objects.requireNonNull(flowedLink),
            Objects.requireNonNull(collDuration));
    }

    @Override
    public CompletableFuture<SecureProbingSample> sendProbingRequest( BitMatch bitMatch,
                                                                      DatapathLink link,
                                                                      Duration collDuration )
    {
        return server.requestProbingCollection(
            Objects.requireNonNull(bitMatch),
            Objects.requireNonNull(link),
            Objects.requireNonNull(collDuration));
    }

    @Override
    public PortId getSamplingPort()
    {
        PortId port = this.samplingPort;
        Preconditions.checkState(port != null, "cannot retrieve sampling OF port, awaiting module initialization");
        return port;
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    private static final class CollectorHandlerServer extends ConcurrentService
    {
        private final Set<CollectorId>                      connectedCollectors;
        private final Map<NodeId, CollectWorker>            collectWorkers;
        private final Map<FlowedLink, SamplingFuture>       activeSampCollects;
        private final Map<DatapathLink, ProbingFuture>      activeProbCollects;
        private final Map<FlowDirectedNodePort, FlowedLink> sampEndpointMap;
        private final Map<DirectedNodePort, DatapathLink>   probEndpointMap;
        private final Object                                commLock;

        // NOTE: we assume that a (flowed-)link end-point is only part of one
        // unique (flowed-)link

        private final ExecutorService requestSenderExec;

        private @Nullable SocketAddress localAddress;
        private @Nullable IAliasService aliasService;

        CollectorHandlerServer()
        {
            super(( msg, ex ) -> LOG.error(msg, ex));

            this.connectedCollectors = new HashSet<>();
            this.collectWorkers = new HashMap<>();
            this.activeSampCollects = new HashMap<>();
            this.activeProbCollects = new HashMap<>();
            this.sampEndpointMap = new HashMap<>();
            this.probEndpointMap = new HashMap<>();
            this.commLock = new Object();

            this.requestSenderExec = Executors.newCachedThreadPool();
        }

        void init( IMonitoringModuleContext context ) throws FloodlightModuleException
        {
            Map<String, String> params = context.getConfigParams();
            this.localAddress = new InetSocketAddress(Props.getLocalPort(params));

            this.aliasService = context.getServiceImpl(IAliasService.class);
        }

        @Override
        protected void startUp()
        {
            LOG.info("Using local address {} for the connection server", localAddress);
            LOG.debug("Starting collector-handler server");
        }

        @Override
        protected void shutDown()
        {
            LOG.debug("Shutting down collector-handler server");
            requestSenderExec.shutdownNow();
        }

        CompletableFuture<TrajectorySample> requestSamplingCollection( FlowedLink flowedLink, Duration collDuration )
        {
            final SamplingFuture future = new SamplingFuture(flowedLink, collDuration);
            synchronized (commLock) {
                CollectWorker srcWorker = collectWorkers.get(flowedLink.getSrcNode());
                CollectWorker destWorker = collectWorkers.get(flowedLink.getDestNode());

                if (srcWorker == null) {
                    abortDueToNoNode(future, flowedLink.getSrcNode(), "flow sampling", "source");
                }
                else if (destWorker == null) {
                    abortDueToNoNode(future, flowedLink.getDestNode(), "flow sampling", "destination");
                }
                else {
                    if (null == activeSampCollects.putIfAbsent(flowedLink, future)) {
                        FlowDirectedNodePort srcEndpoint = flowedLink.getSrcEndpoint();
                        sampEndpointMap.put(srcEndpoint, flowedLink);
                        srcWorker.newSamplingRequest(srcEndpoint);

                        FlowDirectedNodePort destEndpoint = flowedLink.getDestEndpoint();
                        sampEndpointMap.put(destEndpoint, flowedLink);
                        destWorker.newSamplingRequest(destEndpoint);
                    }
                    else {
                        abortDueToDuplicate(future, flowedLink, "flow sampling", "flowed-link");
                    }
                }
            }

            return future;
        }

        CompletableFuture<SecureProbingSample> requestProbingCollection( BitMatch bitMatch,
                                                                         DatapathLink link,
                                                                         Duration collDuration )
        {
            final ProbingFuture future = new ProbingFuture(bitMatch, link, collDuration);
            synchronized (commLock) {
                CollectWorker srcWorker = collectWorkers.get(link.getSrcNode());
                CollectWorker destWorker = collectWorkers.get(link.getDestNode());

                if (srcWorker == null) {
                    abortDueToNoNode(future, link.getSrcNode(), "probing", "source");
                }
                else if (destWorker == null) {
                    abortDueToNoNode(future, link.getDestNode(), "probing", "destination");
                }
                else {
                    if (null == activeProbCollects.putIfAbsent(link, future)) {
                        DirectedNodePort srcEndpoint = link.getSrcEndpoint();
                        probEndpointMap.put(srcEndpoint, link);
                        srcWorker.newProbingRequest(srcEndpoint);

                        DirectedNodePort destEndpoint = link.getDestEndpoint();
                        probEndpointMap.put(destEndpoint, link);
                        destWorker.newProbingRequest(destEndpoint);
                    }
                    else {
                        abortDueToDuplicate(future, link, "probing", "link");
                    }
                }
            }

            return future;
        }

        private static void abortDueToNoNode( CollectionFuture<?> future,
                                              NodeId node,
                                              String collType,
                                              String nodeType )
        {
            LOG.debug("Request for a {} collection failed: no collector is available for {} node {}",
                new Object[] {collType, nodeType, node});

            future.abort(String.format("no collector is available for %s node", nodeType));
        }

        private static void abortDueToDuplicate( CollectionFuture<?> future,
                                                 Object link,
                                                 String collType,
                                                 String linkType )
        {
            LOG.debug("Request for a {} collection failed: {} is already being collected - {}",
                new Object[] {collType, linkType, link});

            future.abort(String.format("%s is already being collected", linkType));
        }

        boolean hasWorkerFor( NodeId nodeId )
        {
            synchronized (commLock) {
                return collectWorkers.containsKey(nodeId);
            }
        }

        @Override
        public void runInterruptibly() throws InterruptedException
        {
            try (SelectorProxy sel = SelectorProxy.open()) {
                ServerSocketChannel srvCh = ServerSocketChannel.open();
                sel.registerChannel(srvCh, SelectionKey.OP_ACCEPT);
                srvCh.setOption(StandardSocketOptions.SO_REUSEADDR, true);
                srvCh.bind(localAddress);

                LOG.info("Listening to collector connections...");
                runServerLoop(sel);
            }
            catch (IOSelectException e) {
                LOG.error(String.format("!!! IO-SELECT error in collector-handler server: %s", e.getMessage()),
                    e);
            }
            catch (IOChannelAcceptException e) {
                e.checkInterruptStatus();
                LOG.error(String.format("!!! IO-ACCEPT error in collector-handler server: %s", e.getMessage()),
                    e);
            }
            catch (IOException e) {
                LOG.error(String.format("!!! IO error in collector-handler server: %s", e.getMessage()),
                    e);
            }
        }

        // NOTE: this method never returns normally, it always returns from a
        // thrown exception
        private void runServerLoop( SelectorProxy sel )
            throws InterruptedException, IOSelectException, IOChannelAcceptException
        {
            while (true) {
                final int numSel = sel.selectInterruptibly();
                if (numSel > 0) {
                    Set<SelectionKey> selected = sel.selectedKeys();
                    Iterator<SelectionKey> iter = Iterators.consumingIterator(selected.iterator());
                    while (iter.hasNext()) {
                        // key is also removed
                        SelectionKey key = iter.next();
                        if (key.isAcceptable()) {
                            ServerSocketChannel srvCh = (ServerSocketChannel)key.channel();
                            SocketChannel ch = NetworkChannelUtils.accept(srvCh);
                            if (ch != null) {
                                handleNewConnection(ch, sel);
                            }
                        }
                        else if (key.isReadable()) {
                            SocketChannel ch = (SocketChannel)key.channel();
                            CollectWorker worker = (CollectWorker)key.attachment();
                            handleReceivedData(ch, worker);
                        }
                    }
                }
            }
        }

        private void handleNewConnection( SocketChannel ch, SelectorProxy sel ) throws InterruptedException
        {
            CollectWorker worker = handleIdentification(ch);
            if (worker != null) {
                try {
                    // closes channel on error
                    sel.registerChannel(ch, SelectionKey.OP_READ, worker);
                    LOG.info("== New connection for collector {} (supports switches {}) ==",
                        worker.getCollectorId(),
                        worker.getSupportedSwitches());
                }
                catch (IOException e) {
                    removeWorker(worker);
                    LOG.error(
                        String.format("!!! IO error in channel registration stage of communication with collector %s",
                            worker.getCollectorId()),
                        e);
                }
            }
        }

        private @CheckForNull CollectWorker handleIdentification( SocketChannel ch ) throws InterruptedException
        {
            boolean error = true;
            try {
                CollectWorker worker = new CollectWorker(ch);
                if (addWorker(worker)) {
                    error = false;
                    return worker;
                }
            }
            catch (IOChannelReadException e) {
                e.checkInterruptStatus();
                LOG.warn("! IO-READ exception in connection with collector (before identification): {}",
                    e.getMessage());
            }
            finally {
                if (error) {
                    Closeables.closeQuietly(ch);
                }
            }

            return null;
        }

        private void handleReceivedData( SocketChannel ch, CollectWorker worker ) throws InterruptedException
        {
            boolean error = true;
            try {
                worker.receiveReply();
                error = false;
            }
            catch (IOChannelReadException e) {
                e.checkInterruptStatus();
                LOG.warn("! IO-READ exception in connection with collector {}: {}",
                    worker.getCollectorId(), e.getMessage());
            }
            finally {
                if (error) {
                    Closeables.closeQuietly(ch);
                    removeWorker(worker);
                }
            }
        }

        private boolean addWorker( CollectWorker worker )
        {
            synchronized (commLock) {
                if (connectedCollectors.contains(worker.getCollectorId())) {
                    LOG.error("Connection failed with collector {}: duplicate collector ID", worker.getCollectorId());
                    return false;
                }
                else {
                    // 1st pass over the switches to check for duplicates
                    for (NodeId nodeId : worker.getSupportedSwitches()) {
                        if (collectWorkers.containsKey(nodeId)) {
                            LOG.error("Connection failed with collector {}: duplicate supported switch {}",
                                worker.getCollectorId(),
                                nodeId);
                            return false;
                        }
                    }

                    // 2nd pass over the switches to register the worker
                    for (NodeId nodeId : worker.getSupportedSwitches()) {
                        collectWorkers.put(nodeId, worker);
                    }

                    connectedCollectors.add(worker.getCollectorId());
                    worker.start(requestSenderExec);
                    return true;
                }
            }
        }

        private void removeWorker( CollectWorker worker )
        {
            synchronized (commLock) {
                worker.shutdown();
                for (NodeId nodeId : worker.getSupportedSwitches()) {
                    abortActiveCollections(nodeId, "disconnected collector");
                    collectWorkers.remove(nodeId);
                }
                connectedCollectors.remove(worker.getCollectorId());
                LOG.warn("== disconnected collector {} ==", worker.getCollectorId());
            }
        }

        // NOTE: call only when commLock is held
        private void abortActiveCollections( NodeId nodeId, String reason )
        {
            Iterator<SamplingFuture> sampIt = activeSampCollects.values().iterator();
            while (sampIt.hasNext()) {
                SamplingFuture future = sampIt.next();
                FlowedLink link = future.getFlowedLink();
                if (link.hasNode(nodeId)) {
                    sampIt.remove();
                    sampEndpointMap.remove(link.getSrcEndpoint());
                    sampEndpointMap.remove(link.getDestEndpoint());
                    future.abort(reason);
                }
            }

            Iterator<ProbingFuture> probIt = activeProbCollects.values().iterator();
            while (probIt.hasNext()) {
                ProbingFuture future = probIt.next();
                DatapathLink link = future.getLink();
                if (link.hasNode(nodeId)) {
                    probIt.remove();
                    probEndpointMap.remove(link.getSrcEndpoint());
                    probEndpointMap.remove(link.getDestEndpoint());
                    future.abort(reason);
                }
            }
        }

        @FieldsAreNonnullByDefault
        @ParametersAreNonnullByDefault
        @ReturnValuesAreNonnullByDefault
        private final class CollectWorker implements InterruptibleRunnable
        {
            private final BlockingQueue<CollectionRequest> requestsQueue;
            private final ControllerCommunicator           comm;

            private @Nullable volatile Future<?> workerHandler;

            CollectWorker( ByteChannel ch ) throws IOChannelReadException
            {
                this.requestsQueue = new LinkedBlockingQueue<>();
                this.comm = ControllerCommunicator.create(
                    ch,
                    aliasService::getSwitchAlias,
                    MirroringConfig.COMPRESSION_STRAT,
                    LOG);
            }

            CollectorId getCollectorId()
            {
                return comm.getConnection().getCollectorId();
            }

            ImmutableSet<NodeId> getSupportedSwitches()
            {
                return comm.getConnection().getSupportedSwitches();
            }

            void start( ExecutorService requestSenderExec )
            {
                LOG.debug("Starting worker for collector {}", getCollectorId());
                this.workerHandler = requestSenderExec.submit(this);
            }

            void shutdown()
            {
                LOG.debug("Shutting down worker for collector {}", getCollectorId());
                this.workerHandler.cancel(true);
            }

            void newSamplingRequest( FlowDirectedNodePort flowedEndpoint )
            {
                LOG.trace("New collection sampling request for flowed-link end-point {}", flowedEndpoint);
                requestsQueue.add(CollectionRequest.forSampling(flowedEndpoint));
            }

            void newProbingRequest( DirectedNodePort endpoint )
            {
                LOG.trace("New collection probing request for link end-point {}", endpoint);
                requestsQueue.add(CollectionRequest.forProbing(endpoint));
            }

            @Override
            public void runInterruptibly() throws InterruptedException
            {
                try {
                    while (true) {
                        LOG.debug("Waiting for collection requests to send");
                        CollectionRequest collReq = requestsQueue.take();

                        switch (collReq.getType()) {
                            case SAMPLING: {
                                FlowDirectedNodePort flowedEndpoint = collReq.getEndpointForSampling();
                                SamplingFuture future = retrieveSamplingFuture(flowedEndpoint);
                                if (future != null) {
                                    GenericRequest sampReq = buildSamplingRequest(flowedEndpoint, future);
                                    sendRequest(sampReq);
                                }
                            }
                            break;

                            case PROBING: {
                                DirectedNodePort endpoint = collReq.getEndpointForProbing();
                                ProbingFuture future = retrieveProbingFuture(endpoint);
                                if (future != null) {
                                    GenericRequest probReq = buildProbingRequest(endpoint, future);
                                    sendRequest(probReq);
                                }
                            }
                            break;

                            default:
                                throw new AssertionError("unexpected enum value");
                        }
                    }
                }
                catch (IOChannelWriteException e) {
                    e.checkInterruptStatus();
                    LOG.warn("! IO-WRITE error in collect worker: {}", e.getMessage());
                }
                finally {
                    LOG.debug("Worker for collector {} has shut down", getCollectorId());
                }
            }

            private void sendRequest( GenericRequest req ) throws IOChannelWriteException
            {
                switch (req.getType()) {
                    case SAMPLING: {
                        SamplingRequest sampReq = req.forSampling();
                        LOG.debug(
                            "Sending a new sampling request to collector {} with a duration of {} for switch-port {} and flow {}",
                            new Object[] {getCollectorId(),
                                          TimeUtils.toSmartDurationString(sampReq.getCollectDuration()),
                                          sampReq.getSwitchPort(),
                                          sampReq.getFlow()});
                    }
                    break;

                    case PROBING: {
                        ProbingRequest probReq = req.forProbing();
                        LOG.debug(
                            "Sending a new probing request to collector {} with a duration of {} for switch-port {} and bit match {}",
                            new Object[] {getCollectorId(),
                                          TimeUtils.toSmartDurationString(probReq.getCollectDuration()),
                                          probReq.getSwitchPort(),
                                          probReq.getBitMatch()});
                    }
                    break;

                    default:
                        throw new AssertionError("unexpected enum value");
                }

                comm.sendRequest(req);
            }

            void receiveReply() throws IOChannelReadException
            {
                GenericReply reply = comm.receiveReply();
                Instant replyTime = Instant.now();
                switch (reply.getType()) {
                    case SAMPLING:
                        receiveSamplingReply(reply.forSampling(), replyTime);
                    break;

                    case PROBING:
                        receiveProbingReply(reply.forProbing(), replyTime);
                    break;

                    default:
                        throw new AssertionError("unexpected enum value");
                }
            }

            private void receiveSamplingReply( SamplingReply reply, Instant replyTime )
            {
                DirectedNodePort switchPort = reply.getSwitchPort();
                Flow flow = reply.getFlow();
                FlowDirectedNodePort flowedEndpoint = FlowDirectedNodePort.of(switchPort, flow);
                LOG.trace("Received collector sampling reply with flowed-link end-point {}", flowedEndpoint);

                SamplingFuture future = retrieveSamplingFuture(flowedEndpoint);
                if (future != null) {
                    LOG.debug("Delivering collector sampling reply for flowed-link end-point {}", flowedEndpoint);
                    boolean replySet = deliverSamplingResult(Timed.of(reply, replyTime), flowedEndpoint, future);
                    if (!replySet) {
                        LOG.warn("Received duplicate collector sampling reply for flowed-link end-point {}",
                            flowedEndpoint);
                    }
                }
                else {
                    LOG.trace(
                        "Ignored received collector sampling reply (no active collection) for flowed-link end-point {}",
                        flowedEndpoint);
                }
            }

            private void receiveProbingReply( ProbingReply reply, Instant replyTime )
            {
                DirectedNodePort endpoint = reply.getSwitchPort();
                LOG.trace("Received collector probing reply with link end-point {}", endpoint);

                ProbingFuture future = retrieveProbingFuture(endpoint);
                if (future != null) {
                    LOG.debug("Delivering collector probing reply for link end-point {}", endpoint);
                    boolean replySet = deliverProbingResult(Timed.of(reply, replyTime), endpoint, future);
                    if (!replySet) {
                        LOG.warn("Received duplicate collector probing reply for link end-point {}", endpoint);
                    }
                }
                else {
                    LOG.trace("Ignored received collector probing reply (no active collection) for link end-point {}",
                        endpoint);
                }
            }

            private GenericRequest buildSamplingRequest( FlowDirectedNodePort flowedEndpoint, SamplingFuture future )
            {
                DirectedNodePort switchPort = flowedEndpoint.unflowed();
                Flow flow = flowedEndpoint.getFlow();
                Duration collDuration = future.getCollectDuration();
                return GenericRequest.fromSampling(new SamplingRequest(switchPort, flow, collDuration));
            }

            private GenericRequest buildProbingRequest( DirectedNodePort endpoint, ProbingFuture future )
            {
                BitMatch bitMatch = future.getBitMatch();
                Duration collDuration = future.getCollectDuration();
                return GenericRequest.fromProbing(new ProbingRequest(endpoint, bitMatch, collDuration));
            }

            private @CheckForNull SamplingFuture retrieveSamplingFuture( FlowDirectedNodePort flowedEndpoint )
            {
                synchronized (commLock) {
                    FlowedLink flowedLink = sampEndpointMap.get(flowedEndpoint);
                    if (flowedLink == null) {
                        LOG.trace("flowed-link retrieved by flowed-link end-point is unknown: {}", flowedEndpoint);
                        return null;
                    }
                    else {
                        SamplingFuture future = activeSampCollects.get(flowedLink);
                        if (future == null) {
                            LOG.trace("deliverable future retrieved by flowed-link end-point is unknown: {}",
                                flowedEndpoint);
                        }
                        return future;
                    }
                }
            }

            private @CheckForNull ProbingFuture retrieveProbingFuture( DirectedNodePort endpoint )
            {
                synchronized (commLock) {
                    DatapathLink link = probEndpointMap.get(endpoint);
                    if (link == null) {
                        LOG.trace("link retrieved by link end-point is unknown: {}", endpoint);
                        return null;
                    }
                    else {
                        ProbingFuture future = activeProbCollects.get(link);
                        if (future == null) {
                            LOG.trace("deliverable future retrieved by link end-point is unknown: {}", endpoint);
                        }
                        return future;
                    }
                }
            }

            private boolean deliverSamplingResult( Timed<SamplingReply> reply,
                                                   FlowDirectedNodePort flowedEndpoint,
                                                   SamplingFuture future )
            {
                synchronized (commLock) {
                    if (future.setResult(reply, flowedEndpoint)) {
                        handleSamplingResultAfterSet(future);
                        return true;
                    }
                    else {
                        return false;
                    }
                }
            }

            private boolean deliverProbingResult( Timed<ProbingReply> reply,
                                                  DirectedNodePort endpoint,
                                                  ProbingFuture future )
            {
                synchronized (commLock) {
                    if (future.setResult(reply, endpoint)) {
                        handleProbingResultAfterSet(future);
                        return true;
                    }
                    else {
                        return false;
                    }
                }
            }

            // NOTE: call only when commLock is held
            private void handleSamplingResultAfterSet( SamplingFuture future )
            {
                if (future.isDone()) {
                    FlowedLink flowedLink = future.getFlowedLink();
                    activeSampCollects.remove(flowedLink);
                    sampEndpointMap.remove(flowedLink.getSrcEndpoint());
                    sampEndpointMap.remove(flowedLink.getDestEndpoint());
                }
            }

            // NOTE: call only when commLock is held
            private void handleProbingResultAfterSet( ProbingFuture future )
            {
                if (future.isDone()) {
                    DatapathLink link = future.getLink();
                    activeProbCollects.remove(link);
                    probEndpointMap.remove(link.getSrcEndpoint());
                    probEndpointMap.remove(link.getDestEndpoint());
                }
            }
        }

        @Immutable
        @FieldsAreNonnullByDefault
        @ParametersAreNonnullByDefault
        @ReturnValuesAreNonnullByDefault
        private static final class CollectionRequest
        {
            static CollectionRequest forSampling( FlowDirectedNodePort flowedEndpoint )
            {
                return new CollectionRequest(
                    SAMPLING,
                    Optional.of(flowedEndpoint),
                    Optional.empty());
            }

            static CollectionRequest forProbing( DirectedNodePort endpoint )
            {
                return new CollectionRequest(
                    PROBING,
                    Optional.empty(),
                    Optional.of(endpoint));
            }

            private final CollectionType                 type;
            private final Optional<FlowDirectedNodePort> sampEndpoint;
            private final Optional<DirectedNodePort>     probEndpoint;

            private CollectionRequest( CollectionType type,
                                       Optional<FlowDirectedNodePort> sampEndpoint,
                                       Optional<DirectedNodePort> probEndpoint )
            {
                this.type = type;
                this.sampEndpoint = sampEndpoint;
                this.probEndpoint = probEndpoint;
            }

            CollectionType getType()
            {
                return type;
            }

            FlowDirectedNodePort getEndpointForSampling()
            {
                return sampEndpoint.orElseThrow(() -> new UnsupportedOperationException("wrong endpoint type"));
            }

            DirectedNodePort getEndpointForProbing()
            {
                return probEndpoint.orElseThrow(() -> new UnsupportedOperationException("wrong endpoint type"));
            }
        }

        @ParametersAreNonnullByDefault
        private static abstract class CollectionFuture<SAMPLE> extends CompletableFuture<SAMPLE>
        {
            final void abort( String reason )
            {
                completeExceptionally(
                    new IllegalStateException(
                        String.format("collection aborted: %s", reason)));
            }
        }

        @FieldsAreNonnullByDefault
        @ParametersAreNonnullByDefault
        @ReturnValuesAreNonnullByDefault
        private static final class SamplingFuture extends CollectionFuture<TrajectorySample>
        {
            private final FlowedLink flowedLink;
            private final Duration   collDuration;

            private Optional<Timed<List<TimedPacketSummary>>> srcSumms;
            private Optional<Timed<Long>>                     srcUmtchBytes;
            private Optional<Timed<Long>>                     srcUmtchPkts;
            private Optional<Timed<List<TimedPacketSummary>>> destSumms;
            private Optional<Timed<Long>>                     destUmtchBytes;
            private Optional<Timed<Long>>                     destUmtchPkts;

            private final Object writeLock;

            SamplingFuture( FlowedLink flowedLink, Duration collDuration )
            {
                this.flowedLink = flowedLink;
                this.collDuration = collDuration;

                this.srcSumms = Optional.empty();
                this.srcUmtchBytes = Optional.empty();
                this.srcUmtchPkts = Optional.empty();
                this.destSumms = Optional.empty();
                this.destUmtchBytes = Optional.empty();
                this.destUmtchPkts = Optional.empty();

                this.writeLock = new Object();
            }

            FlowedLink getFlowedLink()
            {
                return flowedLink;
            }

            Duration getCollectDuration()
            {
                return collDuration;
            }

            boolean setResult( Timed<SamplingReply> result, FlowDirectedNodePort flowedEndpoint )
            {
                if (flowedEndpoint.equals(flowedLink.getSrcEndpoint())) {
                    return setSrcResult(result);
                }
                else if (flowedEndpoint.equals(flowedLink.getDestEndpoint())) {
                    return setDestResult(result);
                }
                else {
                    throw new IllegalArgumentException("invalid flowed-node-port");
                }
            }

            private boolean setSrcResult( Timed<SamplingReply> srcResult )
            {
                synchronized (writeLock) {
                    if (this.srcSumms.isPresent()) {
                        return false;
                    }
                    else {
                        this.srcSumms = Optional.of(srcResult.mapSameTime(SamplingReply::getTimedPacketSummaries));
                        this.srcUmtchBytes = Optional.of(srcResult.mapSameTime(SamplingReply::getUnmatchedBytes));
                        this.srcUmtchPkts = Optional.of(srcResult.mapSameTime(SamplingReply::getUnmatchedPackets));
                        tryDeliverCollected();
                        return true;
                    }
                }
            }

            private boolean setDestResult( Timed<SamplingReply> destResult )
            {
                synchronized (writeLock) {
                    if (this.destSumms.isPresent()) {
                        return false;
                    }
                    else {
                        this.destSumms = Optional.of(destResult.mapSameTime(SamplingReply::getTimedPacketSummaries));
                        this.destUmtchBytes = Optional.of(destResult.mapSameTime(SamplingReply::getUnmatchedBytes));
                        this.destUmtchPkts = Optional.of(destResult.mapSameTime(SamplingReply::getUnmatchedPackets));
                        tryDeliverCollected();
                        return true;
                    }
                }
            }

            // NOTE: call only when writeLock is held
            private void tryDeliverCollected()
            {
                if (canDeliverCollected()) {
                    complete(TrajectorySample.of(
                        this.flowedLink,
                        this.collDuration,
                        this.srcSumms.get(),
                        this.srcUmtchBytes.get(),
                        this.srcUmtchPkts.get(),
                        this.destSumms.get(),
                        this.destUmtchBytes.get(),
                        this.destUmtchPkts.get()));
                }
            }

            // NOTE: call only when writeLock is held
            private boolean canDeliverCollected()
            {
                return this.srcSumms.isPresent() && this.destSumms.isPresent();
            }
        }

        @FieldsAreNonnullByDefault
        @ParametersAreNonnullByDefault
        @ReturnValuesAreNonnullByDefault
        private static final class ProbingFuture extends CollectionFuture<SecureProbingSample>
        {
            private final BitMatch     bitMatch;
            private final DatapathLink link;
            private final Duration     collDuration;

            private Optional<Timed<Possible<TimedPacketSummary>>> srcSumm;
            private Optional<Timed<Possible<TimedPacketSummary>>> destSumm;
            private final Object                                  writeLock;

            ProbingFuture( BitMatch bitMatch, DatapathLink link, Duration collDuration )
            {
                this.bitMatch = bitMatch;
                this.link = link;
                this.collDuration = collDuration;

                this.srcSumm = Optional.empty();
                this.destSumm = Optional.empty();
                this.writeLock = new Object();
            }

            BitMatch getBitMatch()
            {
                return bitMatch;
            }

            DatapathLink getLink()
            {
                return link;
            }

            Duration getCollectDuration()
            {
                return collDuration;
            }

            boolean setResult( Timed<ProbingReply> result, DirectedNodePort endpoint )
            {
                if (endpoint.equals(link.getSrcEndpoint())) {
                    return setSrcResult(result);
                }
                else if (endpoint.equals(link.getDestEndpoint())) {
                    return setDestResult(result);
                }
                else {
                    throw new IllegalArgumentException("invalid node-port");
                }
            }

            private boolean setSrcResult( Timed<ProbingReply> srcResult )
            {
                synchronized (writeLock) {
                    if (this.srcSumm.isPresent()) {
                        return false;
                    }
                    else {
                        this.srcSumm = Optional.of(srcResult.mapSameTime(ProbingReply::getTimedPacketSummary));
                        tryDeliverCollected();
                        return true;
                    }
                }
            }

            private boolean setDestResult( Timed<ProbingReply> destResult )
            {
                synchronized (writeLock) {
                    if (this.destSumm.isPresent()) {
                        return false;
                    }
                    else {
                        this.destSumm = Optional.of(destResult.mapSameTime(ProbingReply::getTimedPacketSummary));
                        tryDeliverCollected();
                        return true;
                    }
                }
            }

            // NOTE: call only when writeLock is held
            private void tryDeliverCollected()
            {
                if (canDeliverCollected()) {
                    complete(SecureProbingSample.of(
                        this.bitMatch,
                        this.link,
                        this.collDuration,
                        this.srcSumm.get(),
                        this.destSumm.get()));
                }
            }

            // NOTE: call only when writeLock is held
            private boolean canDeliverCollected()
            {
                return this.srcSumm.isPresent() && this.destSumm.isPresent();
            }
        }
    }
}
