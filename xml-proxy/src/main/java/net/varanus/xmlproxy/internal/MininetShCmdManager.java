package net.varanus.xmlproxy.internal;


import static net.varanus.xmlproxy.internal.MininetShCmds.IPERF;
import static net.varanus.xmlproxy.internal.MininetShCmds.PING;
import static net.varanus.xmlproxy.internal.MininetShCmds.TCP_REPLAY;

import java.io.EOFException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import javax.annotation.CheckForNull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterators;
import com.google.common.io.Closeables;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.concurrent.ConcurrencyUtils;
import net.varanus.util.concurrent.ConcurrentService;
import net.varanus.util.functional.Report;
import net.varanus.util.io.NetworkChannelUtils;
import net.varanus.util.io.SelectorProxy;
import net.varanus.util.io.Serializers;
import net.varanus.util.io.exception.IOChannelAcceptException;
import net.varanus.util.io.exception.IOChannelReadException;
import net.varanus.util.io.exception.IOSelectException;
import net.varanus.util.io.serializer.IOReader;
import net.varanus.util.time.TimeDouble;
import net.varanus.util.unitvalue.si.InfoLong;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
final class MininetShCmdManager extends ConcurrentService
{
    private static final Charset          MININET_COMM_CS = StandardCharsets.UTF_8;
    private static final IOReader<String> STRING_READER   = Serializers.stringReader(MININET_COMM_CS);

    private static final Logger LOG = LoggerFactory.getLogger(MininetShCmdManager.class);

    private final InetSocketAddress          localAddress;
    private final Map<String, ResultHandler> activeCommands;
    private final Set<String>                activeNoOutputCommands;
    private final Object                     lock;

    MininetShCmdManager( InetSocketAddress localAddress )
    {
        super(ConcurrencyUtils.defaultDaemonThreadFactory(),
            ( msg, ex ) -> LOG.error(msg, ex));

        this.localAddress = localAddress;
        this.activeCommands = new HashMap<>();
        this.activeNoOutputCommands = new HashSet<>();
        this.lock = new Object();
    }

    InetSocketAddress getServerAddress()
    {
        return localAddress;
    }

    boolean registerPingCmd( MininetHost srcHost, MininetHost destHost, CommandResultHandler<TimeDouble> resultHandler )
    {
        synchronized (lock) {
            String key = PING.key(srcHost, destHost);
            if (isActive(key)) {
                return false;
            }
            else {
                activate(key, ResultHandler.forPing(resultHandler));
                LOG.debug("Registered ping command with key {}", key);
                return true;
            }
        }
    }

    boolean unregisterPingCmd( MininetHost srcHost, MininetHost destHost )
    {
        synchronized (lock) {
            String key = PING.key(srcHost, destHost);
            if (!isActive(key)) {
                return false;
            }
            else {
                inactivate(key);
                LOG.debug("Unregistered ping command with key {}", key);
                return true;
            }
        }
    }

    boolean registerIperfCmd( MininetHost srcHost, MininetHost destHost, CommandResultHandler<InfoLong> resultHandler )
    {
        synchronized (lock) {
            String cliKey = IPERF.clientKey(srcHost, destHost);
            String srvKey = IPERF.serverKey(destHost);
            if (isActiveNoOutput(cliKey) || isActive(srvKey)) {
                return false;
            }
            else {
                activateNoOutput(cliKey);
                activate(srvKey, ResultHandler.forIperf(resultHandler));
                LOG.debug("Registered iperf command with client key {} and server key {}", cliKey, srvKey);
                return true;
            }
        }
    }

    boolean unregisterIperfCmd( MininetHost srcHost, MininetHost destHost )
    {
        synchronized (lock) {
            String cliKey = IPERF.clientKey(srcHost, destHost);
            String srvKey = IPERF.serverKey(destHost);
            if (!(isActiveNoOutput(cliKey) && isActive(srvKey))) {
                return false;
            }
            else {
                inactivateNoOutput(cliKey);
                inactivate(srvKey);
                LOG.debug("Unregistered iperf command with client key {} and server key {}", cliKey, srvKey);
                return true;
            }
        }
    }

    boolean registerTcpReplayCmd( MininetLinkInfo linkInfo )
    {
        synchronized (lock) {
            String key = TCP_REPLAY.key(linkInfo);
            if (isActiveNoOutput(key)) {
                return false;
            }
            else {
                activateNoOutput(key);
                LOG.debug("Registered tcpreplay command with key {}", key);
                return true;
            }
        }
    }

    boolean unregisterTcpReplayCmd( MininetLinkInfo linkInfo )
    {
        synchronized (lock) {
            String key = TCP_REPLAY.key(linkInfo);
            if (!isActiveNoOutput(key)) {
                return false;
            }
            else {
                inactivateNoOutput(key);
                LOG.debug("Unregistered tcpreplay command with key {}", key);
                return true;
            }
        }
    }

    // NOTE: call only when holding lock
    private boolean isActive( String key )
    {
        return activeCommands.containsKey(key);
    }

    // NOTE: call only when holding lock
    private boolean isActiveNoOutput( String noOutputKey )
    {
        return activeNoOutputCommands.contains(noOutputKey);
    }

    // NOTE: call only when holding lock
    private void activate( String key, ResultHandler handler )
    {
        activeCommands.put(key, handler);
    }

    // NOTE: call only when holding lock
    private void activateNoOutput( String noOutputKey )
    {
        activeNoOutputCommands.add(noOutputKey);
    }

    // NOTE: call only when holding lock
    private void inactivate( String key )
    {
        activeCommands.remove(key);
    }

    // NOTE: call only when holding lock
    private void inactivateNoOutput( String noOutpuKey )
    {
        activeNoOutputCommands.remove(noOutpuKey);
    }

    @Override
    public void runInterruptibly() throws InterruptedException
    {
        try (SelectorProxy sel = SelectorProxy.open()) {
            ServerSocketChannel srvCh = ServerSocketChannel.open();
            sel.registerChannel(srvCh, SelectionKey.OP_ACCEPT);
            srvCh.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            srvCh.bind(localAddress);

            LOG.info("Listening to command output connections...");
            runServerLoop(sel);
        }
        catch (IOSelectException e) {
            LOG.error(String.format("!!! IO-SELECT error in mininet command output server: %s", e.getMessage()),
                e);
        }
        catch (IOChannelAcceptException e) {
            e.checkInterruptStatus();
            LOG.error(String.format("!!! IO-ACCEPT error in mininet command output server: %s", e.getMessage()),
                e);
        }
        catch (IOException e) {
            LOG.error("!!! IO error in mininet command output server: {}", e.getMessage());
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
                        handleReceivedData(ch);
                    }
                }
            }
        }
    }

    private static void handleNewConnection( SocketChannel ch, SelectorProxy sel )
    {
        try {
            // closes channel on error
            sel.registerChannel(ch, SelectionKey.OP_READ);
            SocketAddress remoteAddr = NetworkChannelUtils.getRemoteAddress(ch);
            LOG.debug("Mininet command output client connected from {}", remoteAddr);
        }
        catch (IOException e) {
            LOG.error("!!! IO error in channel registration stage of communication with mininet command client: {}",
                e.getMessage());
        }
    }

    private void handleReceivedData( SocketChannel ch ) throws InterruptedException
    {
        boolean error = true;
        try {
            receiveCommandOutput(ch);
            error = false;
        }
        catch (IOChannelReadException e) {
            e.checkInterruptStatus();
            if (e.getCause() instanceof EOFException)
                LOG.debug("Connection with mininet command output client was remotely closed");
            else
                LOG.warn("! IO-READ exception in connection with mininet command output client: {}",
                    e.getMessage());
        }
        finally {
            if (error) {
                Closeables.closeQuietly(ch);
            }
        }
    }

    private void receiveCommandOutput( SocketChannel ch ) throws IOChannelReadException, InterruptedException
    {
        String key = STRING_READER.read(ch);
        String result = STRING_READER.read(ch);
        ResultHandler handler = getActiveHandler(key);
        if (handler != null) {
            LOG.trace("Received mininet command output of type {} for key {}: {}",
                new Object[] {handler.getCmdType(), key, result});
            handler.acceptResult(result);
        }
        else {
            SocketAddress remoteAddr = NetworkChannelUtils.getRemoteAddress(ch);
            throw new IOChannelReadException(
                String.format("received output with inactive command key %s from %s", key, remoteAddr));
        }
    }

    private @CheckForNull ResultHandler getActiveHandler( String key )
    {
        synchronized (lock) {
            return activeCommands.get(key);
        }
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    private static final class ResultHandler
    {
        static ResultHandler forPing( CommandResultHandler<TimeDouble> handler )
        {
            return new ResultHandler(
                CmdType.PING,
                Optional.of(handler),
                Optional.empty());
        }

        static ResultHandler forIperf( CommandResultHandler<InfoLong> handler )
        {
            return new ResultHandler(
                CmdType.IPERF,
                Optional.empty(),
                Optional.of(handler));
        }

        private final CmdType                                    cmdType;
        private final Optional<CommandResultHandler<TimeDouble>> pingHandler;
        private final Optional<CommandResultHandler<InfoLong>>   iperfHandler;

        private ResultHandler( CmdType cmdType,
                               Optional<CommandResultHandler<TimeDouble>> pingHandler,
                               Optional<CommandResultHandler<InfoLong>> iperfHandler )
        {
            this.cmdType = cmdType;
            this.pingHandler = pingHandler;
            this.iperfHandler = iperfHandler;
        }

        CmdType getCmdType()
        {
            return cmdType;
        }

        void acceptResult( String result ) throws InterruptedException
        {
            switch (cmdType) {
                case PING: {
                    CommandResultHandler<TimeDouble> handler = pingHandler();
                    try {
                        Optional<TimeDouble> opRtt = PING.parseResult(result);
                        if (opRtt.isPresent()) {
                            TimeDouble rtt = opRtt.get();
                            LOG.trace("Received RTT of {} in ping result", rtt);
                            handler.accept(Report.of(rtt));
                        }
                        else {
                            // ignore
                            LOG.trace("Received no RTT in ping result");
                        }
                    }
                    catch (IllegalArgumentException e) {
                        handler.accept(Report.ofError(String.format("Received invalid mininet command output: %s",
                            e.getMessage())));
                    }
                }
                break;

                case IPERF: {
                    CommandResultHandler<InfoLong> handler = iperfHandler();
                    try {
                        Optional<InfoLong> opThrpt = IPERF.parseServerResult(result);
                        if (opThrpt.isPresent()) {
                            InfoLong thrpt = opThrpt.get();
                            LOG.trace("Received throughput of {} in iperf result", thrpt);
                            handler.accept(Report.of(thrpt));
                        }
                        else {
                            // ignore
                            LOG.trace("Received no throughput in iperf result");
                        }
                    }
                    catch (IllegalArgumentException e) {
                        handler.accept(Report.ofError(String.format("Received invalid mininet command output: %s",
                            e.getMessage())));
                    }
                }
                break;

                default:
                    throw new AssertionError("unexpected enum value");
            }
        }

        private CommandResultHandler<TimeDouble> pingHandler()
        {
            return pingHandler.orElseThrow(() -> new NoSuchElementException("not available for this handler"));
        }

        private CommandResultHandler<InfoLong> iperfHandler()
        {
            return iperfHandler.orElseThrow(() -> new NoSuchElementException("not available for this handler"));
        }

        static enum CmdType
        {
            PING, IPERF;
        }
    }
}
