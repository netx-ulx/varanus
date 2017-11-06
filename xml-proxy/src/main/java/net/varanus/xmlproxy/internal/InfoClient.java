package net.varanus.xmlproxy.internal;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.annotation.ParametersAreNonnullByDefault;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.varanus.infoprotocol.ClientCommunicator;
import net.varanus.infoprotocol.GenericReply;
import net.varanus.infoprotocol.GenericRequest;
import net.varanus.infoprotocol.PacketReply;
import net.varanus.infoprotocol.PacketRequest;
import net.varanus.infoprotocol.RouteReply;
import net.varanus.infoprotocol.RouteRequest;
import net.varanus.infoprotocol.StatisticsReply;
import net.varanus.infoprotocol.StatisticsRequest;
import net.varanus.infoprotocol.TopologyReply;
import net.varanus.infoprotocol.TopologyRequest;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.concurrent.InterruptibleRunnable;
import net.varanus.util.io.NetworkChannelUtils;
import net.varanus.util.io.exception.IOChannelConnectException;
import net.varanus.util.io.exception.IOChannelReadException;
import net.varanus.util.io.exception.IOChannelWriteException;
import net.varanus.util.openflow.types.Flow;
import net.varanus.util.openflow.types.FlowedUnidiNodePorts;
import net.varanus.util.openflow.types.NodeId;


@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
final class InfoClient implements InterruptibleRunnable
{
    private static final Logger LOG = LoggerFactory.getLogger(InfoClient.class);

    private final InetSocketAddress          infoServerAddress;
    private final boolean                    statErrorOnUnknownLink;
    private final BlockingQueue<InfoRequest> reqQueue;

    InfoClient( InetSocketAddress infoServerAddress, boolean statErrorOnUnknownLink )
    {
        this.infoServerAddress = infoServerAddress;
        this.statErrorOnUnknownLink = statErrorOnUnknownLink;
        this.reqQueue = new LinkedBlockingQueue<>();
    }

    CompletableFuture<TopologyReply> requestTopology() throws InterruptedException
    {
        CompletableFuture<TopologyReply> future = new CompletableFuture<>();
        InfoRequest req = new InfoRequest(GenericRequest.fromTopology(new TopologyRequest()), future);
        reqQueue.put(req);
        return future;
    }

    CompletableFuture<StatisticsReply> requestStatistics( FlowedUnidiNodePorts link ) throws InterruptedException
    {
        CompletableFuture<StatisticsReply> future = new CompletableFuture<>();
        InfoRequest req = new InfoRequest(GenericRequest.fromStatistics(
            new StatisticsRequest(link, statErrorOnUnknownLink)), future);
        reqQueue.put(req);
        return future;
    }

    CompletableFuture<RouteReply> requestRoute( FlowedUnidiNodePorts connection ) throws InterruptedException
    {
        CompletableFuture<RouteReply> future = new CompletableFuture<>();
        InfoRequest req = new InfoRequest(GenericRequest.fromRoute(new RouteRequest(connection)), future);
        reqQueue.put(req);
        return future;
    }

    CompletableFuture<PacketReply> requestPacket( Flow flow ) throws InterruptedException
    {
        CompletableFuture<PacketReply> future = new CompletableFuture<>();
        InfoRequest req = new InfoRequest(GenericRequest.fromPacket(PacketRequest.of(flow)), future);
        reqQueue.put(req);
        return future;

    }

    CompletableFuture<PacketReply> requestPacket( Flow flow, byte[] payload ) throws InterruptedException
    {
        CompletableFuture<PacketReply> future = new CompletableFuture<>();
        InfoRequest req = new InfoRequest(GenericRequest.fromPacket(PacketRequest.of(flow, payload)), future);
        reqQueue.put(req);
        return future;
    }

    @Override
    public void runInterruptibly() throws InterruptedException
    {
        LOG.debug("-- Starting info-client --");
        for (;;) {
            LOG.debug("Connecting to info-server...");
            try (SocketChannel ch = NetworkChannelUtils.stubbornConnect(infoServerAddress)) {
                LOG.info("== Connection established with info-server at remote address {} ==", infoServerAddress);
                ClientCommunicator comm = ClientCommunicator.create(ch, NodeId.NIL_ID_ALIASER, LOG);
                runClientLoop(comm);
            }
            catch (IOChannelWriteException e) {
                e.checkInterruptStatus();
                LOG.warn("! IO-WRITE exception in connection to info-server: {}", e.getMessage());
                TimeUnit.SECONDS.sleep(1); // wait before retrying
                continue;
            }
            catch (IOChannelReadException e) {
                e.checkInterruptStatus();
                LOG.warn("! IO-READ exception in connection to info-server: {}", e.getMessage());
                TimeUnit.SECONDS.sleep(1); // wait before retrying
                continue;
            }
            catch (IOChannelConnectException e) {
                e.checkInterruptStatus();
                LOG.error(String.format("!!! IO-CONNECT error in connection to info-server: %s", e.getMessage()), e);
                break;
            }
            catch (IOException e) {
                LOG.error(String.format("!!! IO error while closing connection to info-server: %s", e.getMessage()),
                    e);
                break;
            }
        }
    }

    private void runClientLoop( ClientCommunicator comm )
        throws InterruptedException, IOChannelWriteException, IOChannelReadException
    {
        while (true) {
            LOG.debug("-- Waiting for info requests from proxy --");
            InfoRequest infoReq = reqQueue.take(); // blocks
            GenericRequest request = infoReq.getRequest();

            try {
                LOG.debug("--- Sending info request: {}", request);
                comm.sendRequest(request);

                LOG.debug("--- Waiting for info reply...");
                GenericReply reply = comm.receiveReply();

                LOG.debug("--- Received info reply: {}", reply);
                infoReq.setReply(reply);
            }
            catch (IOChannelWriteException e) {
                infoReq.setError(String.format("IO-WRITE error in connection with info-server: %s", e.getMessage()));
                throw e;
            }
            catch (IOChannelReadException e) {
                infoReq.setError(String.format("IO-READ error in connection with info-server: %s", e.getMessage()));
                throw e;
            }
        }
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    private static final class InfoRequest
    {
        private final GenericRequest       request;
        private final CompletableFuture<?> future;

        InfoRequest( GenericRequest request, CompletableFuture<?> future )
        {
            this.request = request;
            this.future = future;
        }

        GenericRequest getRequest()
        {
            return request;
        }

        void setReply( GenericReply reply )
        {
            if (reply.getType().request().equals(request.getType())) {
                switch (reply.getType()) {
                    case TOPOLOGY_REPLY:
                        topoFuture().complete(reply.forTopology());
                    break;

                    case STATISTICS_REPLY:
                        statsFuture().complete(reply.forStatistics());
                    break;

                    case ROUTE_REPLY:
                        routeFuture().complete(reply.forRoute());
                    break;

                    case PACKET_REPLY:
                        packetFuture().complete(reply.forPacket());
                    break;

                    default:
                        throw new AssertionError("unexpected enum value");
                }
            }
            else {
                setError(String.format("protocol error: received info reply with incompatible type %s",
                    reply.getType()));
            }
        }

        void setError( String errorMsg )
        {
            future.completeExceptionally(new RuntimeException(errorMsg));
        }

        @SuppressWarnings( "unchecked" )
        private CompletableFuture<TopologyReply> topoFuture()
        {
            return (CompletableFuture<TopologyReply>)future;
        }

        @SuppressWarnings( "unchecked" )
        private CompletableFuture<StatisticsReply> statsFuture()
        {
            return (CompletableFuture<StatisticsReply>)future;
        }

        @SuppressWarnings( "unchecked" )
        private CompletableFuture<RouteReply> routeFuture()
        {
            return (CompletableFuture<RouteReply>)future;
        }

        @SuppressWarnings( "unchecked" )
        private CompletableFuture<PacketReply> packetFuture()
        {
            return (CompletableFuture<PacketReply>)future;
        }
    }
}
