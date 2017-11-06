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

import net.varanus.configprotocol.ClientCommunicator;
import net.varanus.configprotocol.GenericReply;
import net.varanus.configprotocol.GenericRequest;
import net.varanus.configprotocol.LinkBandwidthReply;
import net.varanus.configprotocol.LinkBandwidthRequest;
import net.varanus.configprotocol.LinkEnablingReply;
import net.varanus.configprotocol.LinkEnablingRequest;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.concurrent.InterruptibleRunnable;
import net.varanus.util.io.NetworkChannelUtils;
import net.varanus.util.io.exception.IOChannelConnectException;
import net.varanus.util.io.exception.IOChannelReadException;
import net.varanus.util.io.exception.IOChannelWriteException;
import net.varanus.util.openflow.types.BidiNodePorts;
import net.varanus.util.openflow.types.NodeId;
import net.varanus.util.openflow.types.UnidiNodePorts;
import net.varanus.util.unitvalue.si.InfoDouble;


@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
final class ConfigClient implements InterruptibleRunnable
{
    private static final Logger LOG = LoggerFactory.getLogger(ConfigClient.class);

    private final InetSocketAddress            configServerAddress;
    private final BlockingQueue<ConfigRequest> reqQueue;

    ConfigClient( InetSocketAddress configServerAddress )
    {
        this.configServerAddress = configServerAddress;
        this.reqQueue = new LinkedBlockingQueue<>();
    }

    CompletableFuture<LinkEnablingReply> requestLinkEnabling( BidiNodePorts bidiLink, boolean enable )
        throws InterruptedException
    {
        CompletableFuture<LinkEnablingReply> future = new CompletableFuture<>();
        ConfigRequest req = ConfigRequest.forLinkEnabling(new LinkEnablingRequest(bidiLink, enable), future);
        reqQueue.put(req);
        return future;
    }

    CompletableFuture<LinkBandwidthReply> requestLinkBandwidth( UnidiNodePorts link, InfoDouble bandwidth )
        throws InterruptedException
    {
        CompletableFuture<LinkBandwidthReply> future = new CompletableFuture<>();
        ConfigRequest req = ConfigRequest.forLinkBandwidth(new LinkBandwidthRequest(link, bandwidth), future);
        reqQueue.put(req);
        return future;
    }

    @Override
    public void runInterruptibly() throws InterruptedException
    {
        LOG.debug("-- Starting config-client --");
        for (;;) {
            LOG.debug("Connecting to config-server...");
            try (SocketChannel ch = NetworkChannelUtils.stubbornConnect(configServerAddress)) {
                LOG.info("== Connection established with config-server at remote address {} ==", configServerAddress);
                ClientCommunicator comm = ClientCommunicator.create(ch, NodeId.NIL_ID_ALIASER, LOG);
                runClientLoop(comm);
            }
            catch (IOChannelWriteException e) {
                e.checkInterruptStatus();
                LOG.warn("! IO-WRITE exception in connection to config-server: {}", e.getMessage());
                TimeUnit.SECONDS.sleep(1); // wait before retrying
                continue;
            }
            catch (IOChannelReadException e) {
                e.checkInterruptStatus();
                LOG.warn("! IO-READ exception in connection to config-server: {}", e.getMessage());
                TimeUnit.SECONDS.sleep(1); // wait before retrying
                continue;
            }
            catch (IOChannelConnectException e) {
                e.checkInterruptStatus();
                LOG.error(String.format("!!! IO-CONNECT error in connection to config-server: %s", e.getMessage()), e);
                break;
            }
            catch (IOException e) {
                LOG.error(String.format("!!! IO error while closing connection to config-server: %s",
                    e.getMessage()), e);
                break;
            }
        }
    }

    private void runClientLoop( ClientCommunicator comm )
        throws InterruptedException, IOChannelWriteException, IOChannelReadException
    {
        while (true) {
            LOG.debug("-- Waiting for config requests from proxy --");
            ConfigRequest cfgReq = reqQueue.take(); // blocks
            GenericRequest request = cfgReq.getRequest();

            try {
                LOG.debug("--- Sending config request: {}", request);
                comm.sendRequest(request);

                LOG.debug("--- Waiting for config reply...");
                GenericReply reply = comm.receiveReply();

                LOG.debug("--- Received config reply: {}", reply);
                cfgReq.setReply(reply);
            }
            catch (IOChannelWriteException e) {
                cfgReq.setError(String.format("IO-WRITE error in connection with config-server: %s", e.getMessage()));
                throw e;
            }
            catch (IOChannelReadException e) {
                cfgReq.setError(String.format("IO-READ error in connection with config-server: %s", e.getMessage()));
                throw e;
            }
        }
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    private static final class ConfigRequest
    {
        static ConfigRequest forLinkEnabling( LinkEnablingRequest req, CompletableFuture<LinkEnablingReply> future )
        {
            return new ConfigRequest(GenericRequest.fromLinkEnabling(req), future);
        }

        static ConfigRequest forLinkBandwidth( LinkBandwidthRequest req, CompletableFuture<LinkBandwidthReply> future )
        {
            return new ConfigRequest(GenericRequest.fromLinkBandwidth(req), future);
        }

        private final GenericRequest       request;
        private final CompletableFuture<?> future;

        private ConfigRequest( GenericRequest request, CompletableFuture<?> future )
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
                    case LINK_ENABLING_REPLY:
                        if (reply.forLinkEnabling().getBidiLink().equals(request.forLinkEnabling().getBidiLink()))
                            linkEnablingFuture().complete(reply.forLinkEnabling());
                        else
                            setError("protocol error: links do not match in link-enabling request/reply");
                    break;

                    case LINK_BANDWIDTH_REPLY:
                        if (reply.forLinkBandwidth().getLink().equals(request.forLinkBandwidth().getLink()))
                            linkBandwidthFuture().complete(reply.forLinkBandwidth());
                        else
                            setError("protocol error: links do not match in link-bandwidth request/reply");
                    break;

                    default:
                        throw new AssertionError("unexpected enum value");
                }
            }
            else {
                setError(String.format("protocol error: received config reply with incompatible type %s",
                    reply.getType()));
            }
        }

        void setError( String errorMsg )
        {
            future.completeExceptionally(new RuntimeException(errorMsg));
        }

        @SuppressWarnings( "unchecked" )
        private CompletableFuture<LinkEnablingReply> linkEnablingFuture()
        {
            return (CompletableFuture<LinkEnablingReply>)future;
        }

        @SuppressWarnings( "unchecked" )
        private CompletableFuture<LinkBandwidthReply> linkBandwidthFuture()
        {
            return (CompletableFuture<LinkBandwidthReply>)future;
        }
    }
}
