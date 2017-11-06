package net.varanus.collector.internal;


import static net.varanus.mirroringprotocol.MirroringConfig.HASH_FUNCTION;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.CheckForNull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.varanus.collector.CollectorConfig;
import net.varanus.mirroringprotocol.CollectionType;
import net.varanus.mirroringprotocol.CollectorCommunicator;
import net.varanus.mirroringprotocol.GenericReply;
import net.varanus.mirroringprotocol.GenericRequest;
import net.varanus.mirroringprotocol.MirroringConfig;
import net.varanus.mirroringprotocol.ProbingReply;
import net.varanus.mirroringprotocol.ProbingRequest;
import net.varanus.mirroringprotocol.SamplingReply;
import net.varanus.mirroringprotocol.SamplingRequest;
import net.varanus.mirroringprotocol.util.CollectorId;
import net.varanus.mirroringprotocol.util.PacketSummary;
import net.varanus.mirroringprotocol.util.TimedPacketSummary;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.collect.builder.ImmutableListBuilder;
import net.varanus.util.concurrent.ConcurrencyUtils;
import net.varanus.util.concurrent.ConcurrentService;
import net.varanus.util.concurrent.InterruptibleRunnable;
import net.varanus.util.functional.Possible;
import net.varanus.util.io.NetworkChannelUtils;
import net.varanus.util.io.exception.IOChannelConnectException;
import net.varanus.util.io.exception.IOChannelReadException;
import net.varanus.util.io.exception.IOChannelWriteException;
import net.varanus.util.openflow.types.BitMatch;
import net.varanus.util.openflow.types.DirectedNodePort;
import net.varanus.util.openflow.types.Flow;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
public final class Collector
{
    private static final Logger MAIN_LOG = LoggerFactory.getLogger(Collector.class);
    private static final Logger SAMP_LOG = LoggerFactory.getLogger(MAIN_LOG.getName() + "-sampling");
    private static final Logger PROB_LOG = LoggerFactory.getLogger(MAIN_LOG.getName() + "-probing");

    private static Logger logFor( CollectionType type )
    {
        switch (type) {
            case SAMPLING:
                return SAMP_LOG;

            case PROBING:
                return PROB_LOG;

            default:
                throw new AssertionError("unexpected enum value");
        }
    }

    private final CollectorId     collectorId;
    private final CollectorClient client;

    public Collector( CollectorConfig config )
    {
        this.collectorId = config.getCollectorID();
        this.client = new CollectorClient(config);
    }

    public void start() throws IOException
    {
        MAIN_LOG.debug("-- Starting collector {} --", collectorId);
        try {
            client.start().get();
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
        ConcurrencyUtils.runUntilInterrupted(client::waitForShutdown);
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    private final class CollectorClient extends ConcurrentService
    {
        private final CollectorConfig config;

        private final ExecutorService collectWorkerExecutor;
        private final ExecutorService replySenderExecutor;

        private final PcapManager pcapManager;

        CollectorClient( CollectorConfig config )
        {
            super(( msg, ex ) -> MAIN_LOG.error(msg, ex));

            this.config = config;

            this.collectWorkerExecutor = Executors.newCachedThreadPool();
            this.replySenderExecutor = Executors.newSingleThreadExecutor();

            this.pcapManager = new PcapManager(config.getSwitchIfaceMapping(), config.getIgnoreBSNPackets());
        }

        @Override
        protected void startUp() throws IOException
        {
            config.log(MAIN_LOG, "== Configuration ==");

            MAIN_LOG.debug("Starting pcap manager");
            pcapManager.start();

            MAIN_LOG.debug("Starting connection client");
        }

        @Override
        protected void shutDown()
        {
            MAIN_LOG.debug("Shutting down connection client");

            // interrupt reply sender's and collect workers' threads
            MAIN_LOG.debug("Shutting down reply sender");
            replySenderExecutor.shutdownNow();

            MAIN_LOG.debug("Shutting down collect workers");
            collectWorkerExecutor.shutdownNow();

            MAIN_LOG.debug("Shutting down pcap manager");
            pcapManager.shutdown();
        }

        @Override
        public void runInterruptibly() throws InterruptedException
        {
            final SocketAddress controllerAddr = config.getControllerAddress();
            for (;;) {
                MAIN_LOG.debug("Connecting to controller...");
                try (SocketChannel contrChannel = NetworkChannelUtils.stubbornConnect(controllerAddr)) {
                    MAIN_LOG.info("== Connection established with controller at remote address {} ==", controllerAddr);

                    MAIN_LOG.debug("-- Sending connection parameters to controller --");
                    CollectorCommunicator comm = CollectorCommunicator.create(
                        collectorId,
                        config.getSwitchIfaceMapping().keySet(),
                        contrChannel,
                        config.getDpidAliases()::get,
                        MirroringConfig.COMPRESSION_STRAT,
                        MAIN_LOG,
                        SAMP_LOG,
                        PROB_LOG);

                    runClientLoop(comm, collectWorkerExecutor, replySenderExecutor);
                }
                catch (IOChannelWriteException e) {
                    e.checkInterruptStatus();
                    MAIN_LOG.warn("! IO-WRITE exception in connection with controller: {}", e.getMessage());
                    TimeUnit.SECONDS.sleep(1); // wait before retrying
                    continue;
                }
                catch (IOChannelReadException e) {
                    e.checkInterruptStatus();
                    MAIN_LOG.warn("! IO-READ exception in connection with controller: {}", e.getMessage());
                    TimeUnit.SECONDS.sleep(1); // wait before retrying
                    continue;
                }
                catch (IOChannelConnectException e) {
                    e.checkInterruptStatus();
                    MAIN_LOG.error(
                        String.format("!!! IO-CONNECT error in connection with controller: %s", e.getMessage()),
                        e);
                    break;
                }
                catch (IOException e) {
                    MAIN_LOG.error(
                        String.format("!!! IO error while closing connection to controller: %s", e.getMessage()),
                        e);
                    break;
                }
            }
        }

        private void runClientLoop( CollectorCommunicator comm,
                                    ExecutorService workerExecutor,
                                    ExecutorService replySenderExecutor )
            throws IOChannelReadException
        {
            while (true) {
                MAIN_LOG.debug("-- Waiting for collection requests from controller --");
                GenericRequest req = comm.receiveRequest(); // blocks

                logFor(req.getType()).debug("Starting a new collection worker for request {}", req);
                workerExecutor.execute(new CollectionWorker(req, pcapManager, comm, replySenderExecutor));
            }
        }
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    private static final class CollectionWorker implements InterruptibleRunnable
    {
        private final GenericRequest        request;
        private final PcapManager           pcapManager;
        private final CollectorCommunicator comm;
        private final ExecutorService       replySenderExecutor;

        CollectionWorker( GenericRequest request,
                          PcapManager pcapManager,
                          CollectorCommunicator comm,
                          ExecutorService replySenderExecutor )
        {
            this.request = request;
            this.pcapManager = pcapManager;
            this.comm = comm;
            this.replySenderExecutor = replySenderExecutor;
        }

        @Override
        public void runInterruptibly() throws InterruptedException
        {
            GenericReply reply = runCollection();
            if (reply != null)
                replySenderExecutor.execute(new ReplySenderTask(comm, reply));
        }

        private @CheckForNull GenericReply runCollection() throws InterruptedException
        {
            CollectionHandle handle = pcapManager.acquireCollectionHandle(request);
            if (handle != null) {
                try {
                    return collect(request, handle);
                }
                finally {
                    pcapManager.releaseCollectionHandle(request);
                }
            }
            else {
                MAIN_LOG.warn("!! No available collection handle for request {}", request);
                return null;
            }
        }

        private static GenericReply collect( GenericRequest req, CollectionHandle handle )
            throws InterruptedException
        {
            switch (req.getType()) {
                case SAMPLING:
                    return collectForSampling(req.forSampling(), handle);

                case PROBING:
                    return collectForProbing(req.forProbing(), handle);

                default:
                    throw new AssertionError("unexpected enum value");
            }
        }

        private static GenericReply collectForSampling( SamplingRequest req, CollectionHandle handle )
            throws InterruptedException
        {
            DirectedNodePort switchPort = req.getSwitchPort();
            Flow flow = req.getFlow();
            Duration collDuration = req.getCollectDuration();

            ImmutableListBuilder<TimedPacketSummary> timedSumms = ImmutableListBuilder.create();
            long remainingNanos;
            final long finishNanos = System.nanoTime() + collDuration.toNanos();
            do {
                remainingNanos = finishNanos - System.nanoTime();
                CapturedPacket cap = handle.pollMatched(remainingNanos, TimeUnit.NANOSECONDS);
                if (cap != null) {
                    byte[] pkt = cap.getPacket();
                    Instant timestamp = cap.getCaptureTime();
                    PacketSummary pktSumm = PacketSummary.hashPacket(HASH_FUNCTION.newHasher(), pkt);
                    timedSumms.add(new TimedPacketSummary(pktSumm, timestamp));
                }
            }
            while (remainingNanos > 0);

            final long unmatchedBytes = handle.getUnmatchedBytes();
            final long unmatchedPkts = handle.getUnmatchedPackets();

            return GenericReply.fromSampling(
                new SamplingReply(switchPort, flow, timedSumms.build(), unmatchedBytes, unmatchedPkts));
        }

        private static GenericReply collectForProbing( ProbingRequest req, CollectionHandle handle )
            throws InterruptedException
        {
            DirectedNodePort switchPort = req.getSwitchPort();
            BitMatch bitMatch = req.getBitMatch();
            Duration collDuration = req.getCollectDuration();

            Possible<TimedPacketSummary> timedSumm = Possible.absent();
            long remainingNanos;
            final long finishNanos = System.nanoTime() + collDuration.toNanos();
            do {
                remainingNanos = finishNanos - System.nanoTime();
                CapturedPacket cap = handle.pollMatched(remainingNanos, TimeUnit.NANOSECONDS);
                // collect only the first one that was captured
                if (!(cap == null || timedSumm.isPresent())) {
                    byte[] pkt = cap.getPacket();
                    Instant timestamp = cap.getCaptureTime();
                    PacketSummary pktSumm = PacketSummary.hashPacket(HASH_FUNCTION.newHasher(), pkt);
                    timedSumm = Possible.of(new TimedPacketSummary(pktSumm, timestamp));
                }
            }
            while (remainingNanos > 0);

            return GenericReply.fromProbing(new ProbingReply(switchPort, bitMatch, timedSumm));
        }
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    private static final class ReplySenderTask implements InterruptibleRunnable
    {
        private final CollectorCommunicator comm;
        private final GenericReply          reply;

        ReplySenderTask( CollectorCommunicator comm, GenericReply reply )
        {
            this.comm = comm;
            this.reply = reply;
        }

        @Override
        public void runInterruptibly() throws InterruptedException
        {
            try {
                logFor(reply.getType()).debug("Sending to controller the reply {}", reply);
                comm.sendReply(reply);
            }
            catch (IOChannelWriteException e) {
                e.checkInterruptStatus();
                MAIN_LOG.warn("! IO-WRITE exception in reply sender: {}", e.getMessage());
            }
        }
    }
}
