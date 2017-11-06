package net.varanus.sdncontroller.configserver.internal;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.slf4j.Logger;

import com.google.common.collect.Iterators;

import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.varanus.configprotocol.GenericReply;
import net.varanus.configprotocol.GenericRequest;
import net.varanus.configprotocol.LinkBandwidthReply;
import net.varanus.configprotocol.LinkBandwidthRequest;
import net.varanus.configprotocol.LinkEnablingReply;
import net.varanus.configprotocol.LinkEnablingRequest;
import net.varanus.configprotocol.ServerCommunicator;
import net.varanus.sdncontroller.alias.IAliasService;
import net.varanus.sdncontroller.linkstats.ILinkStatsService;
import net.varanus.sdncontroller.logging.Logging;
import net.varanus.sdncontroller.topologygraph.ITopologyGraphService;
import net.varanus.sdncontroller.types.DatapathLink;
import net.varanus.sdncontroller.util.module.IModuleManager;
import net.varanus.sdncontroller.util.module.ModuleUtils;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.concurrent.ConcurrentService;
import net.varanus.util.concurrent.InterruptibleRunnable;
import net.varanus.util.io.CloseableList;
import net.varanus.util.io.NetworkChannelUtils;
import net.varanus.util.io.SelectorProxy;
import net.varanus.util.io.exception.IOChannelAcceptException;
import net.varanus.util.io.exception.IOChannelReadException;
import net.varanus.util.io.exception.IOChannelWriteException;
import net.varanus.util.io.exception.IOSelectException;
import net.varanus.util.openflow.types.BidiNodePorts;
import net.varanus.util.openflow.types.UnidiNodePorts;
import net.varanus.util.time.Timed;
import net.varanus.util.unitvalue.si.InfoDouble;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class ConfigServerManager implements IModuleManager
{
    private static final Logger LOG = Logging.configserver.LOG;

    private final ConfigServer server;

    public ConfigServerManager()
    {
        this.server = new ConfigServer();
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies()
    {
        return ModuleUtils.services(IAliasService.class, ITopologyGraphService.class, ILinkStatsService.class);
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
    private static final class ConfigServer extends ConcurrentService
    {
        private final ExecutorService cfgConnsExec;

        private @Nullable SocketAddress localAddress;

        private @Nullable IAliasService         aliasService;
        private @Nullable ITopologyGraphService topoGraphService;
        private @Nullable ILinkStatsService     linkStatsService;

        ConfigServer()
        {
            super(( msg, ex ) -> LOG.error(msg, ex));

            this.cfgConnsExec = Executors.newCachedThreadPool();
        }

        void init( FloodlightModuleContext context, Class<? extends IFloodlightModule> moduleClass )
            throws FloodlightModuleException
        {
            Map<String, String> params = context.getConfigParams(moduleClass);
            this.localAddress = new InetSocketAddress(Props.getLocalPort(params));

            this.aliasService = ModuleUtils.getServiceImpl(context, IAliasService.class);
            this.topoGraphService = ModuleUtils.getServiceImpl(context, ITopologyGraphService.class);
            this.linkStatsService = ModuleUtils.getServiceImpl(context, ILinkStatsService.class);
        }

        @Override
        protected void startUp()
        {
            LOG.info("Using local address {} for the config server", localAddress);
            LOG.debug("Starting config server");
        }

        @Override
        protected void shutDown()
        {
            LOG.debug("Shutting down config server");
            cfgConnsExec.shutdownNow();
        }

        @Override
        public void runInterruptibly() throws InterruptedException
        {
            try (SelectorProxy sel = SelectorProxy.open()) {
                ServerSocketChannel srvCh = ServerSocketChannel.open();
                sel.registerChannel(srvCh, SelectionKey.OP_ACCEPT, null);
                srvCh.bind(localAddress);

                LOG.info("Listening to config connections...");
                runServerLoop(sel);
            }
            catch (IOSelectException e) {
                LOG.error(String.format("!!! IO-SELECT error in config server: %s", e.getMessage()), e);
            }
            catch (IOChannelAcceptException e) {
                e.checkInterruptStatus();
                LOG.error(String.format("!!! IO-ACCEPT error in config server: %s", e.getMessage()), e);
            }
            catch (IOException e) {
                LOG.error(String.format("!!! IO error in config server: %s", e.getMessage()), e);
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
                        SelectionKey key = iter.next(); // key is also removed
                        if (key.isAcceptable()) {
                            ServerSocketChannel srvCh = (ServerSocketChannel)key.channel();
                            SocketChannel ch = NetworkChannelUtils.accept(srvCh);
                            if (ch != null) {
                                cfgConnsExec.execute(new ConfigWorker(ch));
                            }
                        }
                    }
                }
            }
        }

        @FieldsAreNonnullByDefault
        @ParametersAreNonnullByDefault
        @ReturnValuesAreNonnullByDefault
        private final class ConfigWorker implements InterruptibleRunnable
        {
            private final SocketChannel ch;

            ConfigWorker( SocketChannel ch )
            {
                this.ch = ch;
            }

            @Override
            public void runInterruptibly() throws InterruptedException
            {
                try (CloseableList cl = new CloseableList()) {
                    cl.add(ch);
                    LOG.info("Accepted new config connection from {}", NetworkChannelUtils.getRemoteAddress(ch));

                    ServerCommunicator comm = ServerCommunicator.create(ch, aliasService::getSwitchAlias, LOG);
                    while (true) {
                        LOG.debug("Waiting for config requests...");
                        GenericRequest genReq = comm.receiveRequest();
                        switch (genReq.getType()) {
                            case LINK_ENABLING_REQUEST: {
                                LinkEnablingRequest req = genReq.forLinkEnabling();
                                LOG.debug("Received link-enabling request: {}", req);

                                LinkEnablingReply reply = onLinkEnablingRequest(req);

                                LOG.debug("Sending link-enabling reply...");
                                comm.sendReply(GenericReply.fromLinkEnabling(reply));
                                LOG.debug("Link-enabling reply sent");
                            }
                            break;

                            case LINK_BANDWIDTH_REQUEST: {
                                LinkBandwidthRequest req = genReq.forLinkBandwidth();
                                LOG.debug("Received link-bandwidth request: {}", req);

                                LinkBandwidthReply reply = onLinkBandwidthRequest(req);

                                LOG.debug("Sending link-bandwidth reply...");
                                comm.sendReply(GenericReply.fromLinkBandwidth(reply));
                                LOG.debug("Link-bandwidth reply sent");
                            }
                            break;

                            default:
                                LOG.warn("! Received unsupported config operation {}", genReq.getType());
                            break;
                        }
                    }
                }
                catch (IOChannelReadException e) {
                    e.checkInterruptStatus();
                    LOG.warn("! IO-READ exception in config connection: {}", e.getMessage());
                }
                catch (IOChannelWriteException e) {
                    e.checkInterruptStatus();
                    LOG.warn("! IO-WRITE exception in config connection: {}", e.getMessage());
                }
                catch (IOException e) {
                    LOG.error(String.format("!!! IO error while closing config connection: %s", e.getMessage()), e);
                }
            }

            private LinkEnablingReply onLinkEnablingRequest( LinkEnablingRequest req )
            {
                BidiNodePorts bidiLink = req.getBidiLink();
                if (req.getEnable()) {
                    try {
                        if (topoGraphService.enableBidiLink(bidiLink))
                            LOG.debug("Enabled bidirectional link {}", bidiLink);
                        return LinkEnablingReply.of(bidiLink, true);
                    }
                    catch (IllegalStateException e) {
                        LOG.warn("! Could not enable bidirectional link {}: {}", bidiLink, e.getMessage());
                        return LinkEnablingReply.ofError(bidiLink, e.getMessage());
                    }
                }
                else {
                    if (topoGraphService.disableBidiLink(bidiLink))
                        LOG.debug("Disabled bidirectional link {}", bidiLink);
                    return LinkEnablingReply.of(bidiLink, false);
                }
            }

            private LinkBandwidthReply onLinkBandwidthRequest( LinkBandwidthRequest req )
            {
                UnidiNodePorts link = req.getLink();
                InfoDouble bandwidth = req.getBandwidth();
                try {
                    if (linkStatsService.updateVirtualCapacity(DatapathLink.of(link), Timed.now(bandwidth)))
                        LOG.debug("Updated virtual bandwidth to {} on link {}", bandwidth, link);
                    return LinkBandwidthReply.of(link, bandwidth);
                }
                catch (IllegalArgumentException e) {
                    LOG.warn("! Could not update virtual bandwidth  to {} on link {}: {}",
                        new Object[] {bandwidth, link, e.getMessage()});
                    return LinkBandwidthReply.ofError(link, e.getMessage());
                }
            }
        }
    }
}
