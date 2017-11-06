package net.varanus.xmlproxy.internal;


import static net.varanus.xmlproxy.internal.MininetPyCmds.DEL_LINK_QOS;
import static net.varanus.xmlproxy.internal.MininetPyCmds.GET_LINKS_INFO;
import static net.varanus.xmlproxy.internal.MininetPyCmds.HOSTS;
import static net.varanus.xmlproxy.internal.MininetPyCmds.HOST_NEIGHBOR_SWITCH_PORTS;
import static net.varanus.xmlproxy.internal.MininetPyCmds.SET_LINK_QOS;
import static net.varanus.xmlproxy.internal.MininetPyCmds.SWITCHES;
import static net.varanus.xmlproxy.internal.MininetPyCmds.SWITCH_NEIGHBOR_HOSTS;
import static net.varanus.xmlproxy.internal.MininetShCmds.IPERF;
import static net.varanus.xmlproxy.internal.MininetShCmds.PING;
import static net.varanus.xmlproxy.internal.MininetShCmds.TCP_REPLAY;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.collect.CommonPair;
import net.varanus.util.collect.Pair;
import net.varanus.util.collect.builder.ImmutableListBuilder;
import net.varanus.util.collect.builder.ImmutableMapBuilder;
import net.varanus.util.concurrent.InterruptibleRunnable;
import net.varanus.util.functional.Possible;
import net.varanus.util.io.ExtraChannels;
import net.varanus.util.io.NetworkChannelUtils;
import net.varanus.util.io.Serializers;
import net.varanus.util.io.exception.IOChannelConnectException;
import net.varanus.util.io.exception.IOChannelReadException;
import net.varanus.util.io.exception.IOChannelWriteException;
import net.varanus.util.io.serializer.IOReader;
import net.varanus.util.io.serializer.IOWriter;
import net.varanus.util.lang.MoreObjects;
import net.varanus.util.lang.Unsigned;
import net.varanus.util.text.StringUtils;
import net.varanus.util.time.TimeDouble;
import net.varanus.util.unitvalue.si.InfoDouble;
import net.varanus.util.unitvalue.si.InfoLong;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
final class MininetClient implements InterruptibleRunnable
{
    private static final Charset          MININET_COMM_CS = StandardCharsets.UTF_8;
    private static final IOWriter<String> CMD_WRITER      = Serializers.stringWriter(MININET_COMM_CS);
    private static final IOReader<String> RESULT_READER   = Serializers.stringReader(MININET_COMM_CS);
    private static final byte             PYTHON_CMD_TYPE = 0;
    private static final byte             SHELL_CMD_TYPE  = 1;

    private static final Logger LOG = LoggerFactory.getLogger(MininetClient.class);

    private final InetSocketAddress             remoteMininetAddress;
    private final MininetShCmdManager           shCmdManager;
    private final BlockingQueue<MininetRequest> reqQueue;

    MininetClient( InetSocketAddress remoteMininetAddress, InetSocketAddress commandOutputAddress )
    {
        this.remoteMininetAddress = remoteMininetAddress;
        this.shCmdManager = new MininetShCmdManager(commandOutputAddress);
        this.reqQueue = new LinkedBlockingQueue<>();
    }

    CompletableFuture<MininetTopo> requestTopology() throws InterruptedException
    {
        MininetRequest req = MininetRequest.forTopology();
        reqQueue.put(req);
        return req.getFutureForTopology();
    }

    CompletableFuture<MininetLinkInfoConfig> requestLinkInfoConfig() throws InterruptedException
    {
        return requestLinkInfoConfig(ImmutableList.of());
    }

    CompletableFuture<MininetLinkInfoConfig> requestLinkInfoConfig( ImmutableList<MininetQoSSetup> qosSetups )
        throws InterruptedException
    {
        MininetRequest req = MininetRequest.forQosConfig(qosSetups);
        reqQueue.put(req);
        return req.getFutureForQoSConfig();
    }

    boolean startPing( MininetHost srcHost, MininetHost destHost, CommandResultHandler<TimeDouble> resultHandler )
        throws InterruptedException
    {
        if (shCmdManager.registerPingCmd(srcHost, destHost, resultHandler)) {
            String cmd = PING.startCommand(srcHost, destHost, shCmdManager.getServerAddress());
            try {
                requestShellCommand(cmd).get();
            }
            catch (InterruptedException e) {
                shCmdManager.unregisterPingCmd(srcHost, destHost);
                throw e;
            }
            catch (ExecutionException e) {
                shCmdManager.unregisterPingCmd(srcHost, destHost);
                // ignore exception
            }
            return true;
        }
        else {
            return false;
        }
    }

    boolean stopPing( MininetHost srcHost, MininetHost destHost ) throws InterruptedException
    {
        if (shCmdManager.unregisterPingCmd(srcHost, destHost)) {
            String cmd = PING.stopCommand(srcHost, destHost);
            try {
                requestShellCommand(cmd).get();
            }
            catch (ExecutionException e) {
                // ignore
            }
            return true;
        }
        else {
            return false;
        }
    }

    boolean startIperf( MininetHost srcHost,
                        MininetHost destHost,
                        InfoDouble bandwidth,
                        CommandResultHandler<InfoLong> resultHandler )
        throws InterruptedException
    {
        if (shCmdManager.registerIperfCmd(srcHost, destHost, resultHandler)) {
            String srvCmd = IPERF.serverStartCommand(destHost, shCmdManager.getServerAddress());
            String cliCmd = IPERF.clientStartNoOutputCommand(srcHost, destHost, bandwidth);
            try {
                CompletableFuture<Void> srvFuture = requestShellCommand(srvCmd);
                CompletableFuture<Void> cliFuture = requestShellCommand(cliCmd);
                CompletableFuture.allOf(srvFuture, cliFuture).get();
            }
            catch (InterruptedException e) {
                shCmdManager.unregisterIperfCmd(srcHost, destHost);
                throw e;
            }
            catch (ExecutionException e) {
                shCmdManager.unregisterIperfCmd(srcHost, destHost);
                // ignore exception
            }
            return true;
        }
        else {
            return false;
        }
    }

    boolean stopIperf( MininetHost srcHost, MininetHost destHost ) throws InterruptedException
    {
        if (shCmdManager.unregisterIperfCmd(srcHost, destHost)) {
            String cliCmd = IPERF.clientStopCommand(srcHost, destHost);
            String srvCmd = IPERF.serverStopCommand(destHost);
            try {
                CompletableFuture<Void> cliFuture = requestShellCommand(cliCmd);
                CompletableFuture<Void> srvFuture = requestShellCommand(srvCmd);
                CompletableFuture.allOf(cliFuture, srvFuture).get();
            }
            catch (ExecutionException e) {
                // ignore
            }
            return true;
        }
        else {
            return false;
        }
    }

    boolean startTcpReplay( MininetLinkInfo linkInfo, byte[] packet, InfoDouble rate ) throws InterruptedException
    {
        if (shCmdManager.registerTcpReplayCmd(linkInfo)) {
            String cmd = TCP_REPLAY.startCommandNoOutput(linkInfo, packet, rate);
            try {
                requestShellCommand(cmd).get();
            }
            catch (InterruptedException e) {
                shCmdManager.unregisterTcpReplayCmd(linkInfo);
                throw e;
            }
            catch (ExecutionException e) {
                shCmdManager.unregisterTcpReplayCmd(linkInfo);
                // ignore exception
            }
            return true;
        }
        else {
            return false;
        }
    }

    boolean stopTcpReplay( MininetLinkInfo linkInfo ) throws InterruptedException
    {
        if (shCmdManager.unregisterTcpReplayCmd(linkInfo)) {
            String cmd = TCP_REPLAY.stopCommand(linkInfo);
            try {
                requestShellCommand(cmd).get();
            }
            catch (ExecutionException e) {
                // ignore
            }
            return true;
        }
        else {
            return false;
        }
    }

    private CompletableFuture<Void> requestShellCommand( String shellCmd ) throws InterruptedException
    {
        MininetRequest req = MininetRequest.forShellCommand(shellCmd);
        reqQueue.put(req);
        return req.getFutureForShellCommand();
    }

    @Override
    public void runInterruptibly() throws InterruptedException
    {
        LOG.debug("-- Starting mininet-client --");
        shCmdManager.start();
        try {
            for (;;) {
                LOG.debug("Connecting to mininet-server...");
                try (SocketChannel ch = NetworkChannelUtils.stubbornConnect(remoteMininetAddress)) {
                    LOG.info("== Connection established with mininet-server at remote address {} ==",
                        remoteMininetAddress);
                    runClientLoop(ch);
                }
                catch (IOChannelWriteException e) {
                    e.checkInterruptStatus();
                    LOG.warn("! IO-WRITE exception in connection to mininet-server: {}", e.getMessage());
                    TimeUnit.SECONDS.sleep(1); // wait before retrying
                    continue;
                }
                catch (IOChannelReadException e) {
                    e.checkInterruptStatus();
                    LOG.warn("! IO-READ exception in connection to mininet-server: {}", e.getMessage());
                    TimeUnit.SECONDS.sleep(1); // wait before retrying
                    continue;
                }
                catch (IOChannelConnectException e) {
                    e.checkInterruptStatus();
                    LOG.error(String.format("!!! IO-CONNECT error in connection to mininet-server: %s", e.getMessage()),
                        e);
                    break;
                }
                catch (IOException e) {
                    LOG.error(
                        String.format("!!! IO error while closing connection to mininet-server: %s", e.getMessage()),
                        e);
                    break;
                }
            }
        }
        finally {
            shCmdManager.stop();
        }
    }

    private void runClientLoop( SocketChannel ch )
        throws InterruptedException,
        IOChannelWriteException,
        IOChannelReadException
    {
        while (true) {
            LOG.debug("-- Waiting for mininet requests from proxy --");
            MininetRequest req = reqQueue.take(); // blocks

            try {
                switch (req.getType()) {
                    case TOPOLOGY:
                        MininetTopo topo = requestTopology(ch);
                        req.completeTopology(topo);
                    break;

                    case LINF_INFO_CONFIG:
                        MininetLinkInfoConfig cfg = requestLinkInfoConfig(ch, req.getQoSSetups());
                        req.completeQosConfig(cfg);
                    break;

                    case SHELL_COMMAND:
                        requestShellCommand(ch, req.getShellCommand());
                        req.completeShellCommand();
                    break;

                    default:
                        throw new AssertionError("unexpected enum value");
                }
            }
            catch (IOChannelWriteException e) {
                req.abort(String.format("IO-WRITE error in connection with mininet-server: %s", e.getMessage()));
                throw e;
            }
            catch (IOChannelReadException e) {
                req.abort(String.format("IO-READ error in connection with mininet-server: %s", e.getMessage()));
                throw e;
            }
        }
    }

    private static MininetTopo requestTopology( SocketChannel ch )
        throws IOChannelWriteException,
        IOChannelReadException
    {
        try {
            LOG.debug("--- Sending mininet topology request commands ---");

            String swRes = sendPythonCommand(ch, SWITCHES.command());
            ImmutableMap<DatapathId, MininetSwitch> switchMap = SWITCHES.parseResult(swRes);

            String hostRes = sendPythonCommand(ch, HOSTS.command());
            ImmutableMap<String, MininetHost> hostMap = HOSTS.parseResult(hostRes);

            ImmutableMapBuilder<DatapathId, ImmutableList<MininetHost>> switchNeighMap = ImmutableMapBuilder.create();
            for (MininetSwitch sw : switchMap.values()) {
                String swNeighRes = sendPythonCommand(ch, SWITCH_NEIGHBOR_HOSTS.command(sw));
                List<String> hostNames = SWITCH_NEIGHBOR_HOSTS.parseResult(swNeighRes);
                ImmutableList<MininetHost> neighHosts = ImmutableListBuilder.<MininetHost>create()
                    .addEach(hostNames.stream()
                        .map(hostMap::get)
                        .filter(Objects::nonNull))
                    .build();
                switchNeighMap.put(sw.getDpid(), neighHosts);
            }

            ImmutableMapBuilder<String, ImmutableList<Pair<MininetSwitch, OFPort>>> hostNeighMap =
                ImmutableMapBuilder.create();
            for (MininetHost host : hostMap.values()) {
                String hostNeighRes = sendPythonCommand(ch, HOST_NEIGHBOR_SWITCH_PORTS.command(host));
                List<Pair<DatapathId, OFPort>> switchPorts = HOST_NEIGHBOR_SWITCH_PORTS.parseResult(hostNeighRes);
                ImmutableList<Pair<MininetSwitch, OFPort>> neighSwitchPorts =
                    ImmutableListBuilder.<Pair<MininetSwitch, OFPort>>create()
                        .addEach(switchPorts.stream()
                            .map(pair -> pair.map(switchMap::get, Function.identity()))
                            .filter(pair -> pair.getFirst() != null))
                        .build();
                hostNeighMap.put(host.getAddress(), neighSwitchPorts);
            }

            return new MininetTopo(switchMap, switchNeighMap.build(), hostMap, hostNeighMap.build());
        }
        catch (IllegalArgumentException e) {
            throw new IOChannelReadException(
                String.format("received a malformed result: %s", e.getMessage()));
        }
    }

    private static MininetLinkInfoConfig requestLinkInfoConfig( SocketChannel ch,
                                                                ImmutableList<MininetQoSSetup> qosSetups )
        throws IOChannelWriteException,
        IOChannelReadException
    {
        try {
            LOG.debug("--- Sending mininet link-info request and QoS-setup commands ---");

            for (MininetQoSSetup setup : qosSetups) {
                if (setup.hasQoSToSetup()) {
                    String res = sendPythonCommand(ch, SET_LINK_QOS.command(setup));
                    boolean success = SET_LINK_QOS.parseResult(res);
                    if (!success)
                        LOG.debug("QoS-setup failed for link between {} and {}",
                            setup.getSrcName(), setup.getDestName());
                }
                else {
                    String res = sendPythonCommand(ch, DEL_LINK_QOS.command(setup.getSrcName(), setup.getDestName()));
                    boolean success = DEL_LINK_QOS.parseResult(res);
                    if (!success)
                        LOG.debug("QoS-setup (deletion) failed for link between {} and {}",
                            setup.getSrcName(), setup.getDestName());
                }
            }

            String linksInfoRes = sendPythonCommand(ch, GET_LINKS_INFO.command());
            ImmutableMap<CommonPair<String>, MininetLinkInfo> linksInfoMap = GET_LINKS_INFO.parseResult(linksInfoRes);
            return new MininetLinkInfoConfig(linksInfoMap);
        }
        catch (IllegalArgumentException e) {
            throw new IOChannelReadException(
                String.format("received a malformed result: %s", e.getMessage()));
        }

    }

    private static void requestShellCommand( SocketChannel ch, String shellCmd )
        throws IOChannelWriteException,
        IOChannelReadException
    {
        LOG.debug("--- Sending mininet shell command ---");
        sendShellCommand(ch, shellCmd);
    }

    private static String sendPythonCommand( SocketChannel ch, String cmd )
        throws IOChannelWriteException,
        IOChannelReadException
    {
        LOG.trace("Sending mininet python command: {}", cmd);
        ExtraChannels.writeByte(ch, PYTHON_CMD_TYPE);
        CMD_WRITER.write(cmd, ch);

        byte cmdType = ExtraChannels.readByte(ch);
        if (cmdType != PYTHON_CMD_TYPE)
            throw new IOChannelReadException(String.format("received an unexpected command type in python reply: %x",
                Unsigned.byteValue(cmdType)));

        int resCode = Unsigned.byteValue(ExtraChannels.readByte(ch));
        switch (resCode) {
            case 1:
                String res = RESULT_READER.read(ch);
                LOG.trace("Received mininet python command result: {}", res);
                return res;

            case 0:
                throw new IOChannelReadException("received a null python command result");

            case 2:
                String exception = RESULT_READER.read(ch);
                throw new IOChannelReadException(
                    String.format("received a python command exception: %s", exception));

            default:
                throw new IOChannelReadException(
                    String.format("received invalid python result code %d", resCode));
        }
    }

    private static void sendShellCommand( SocketChannel ch, String cmd )
        throws IOChannelWriteException,
        IOChannelReadException
    {
        LOG.trace("Sending mininet shell command: {}", cmd);
        ExtraChannels.writeByte(ch, SHELL_CMD_TYPE);
        CMD_WRITER.write(cmd, ch);

        byte cmdType = ExtraChannels.readByte(ch);
        if (cmdType != SHELL_CMD_TYPE)
            throw new IOChannelReadException(String.format("received an unexpected command type in shell reply: %x",
                Unsigned.byteValue(cmdType)));

        int resCode = Unsigned.byteValue(ExtraChannels.readByte(ch));
        switch (resCode) {
            case 1:
                LOG.trace("Received mininet shell command OK result");
            break;

            case 2:
                String error = RESULT_READER.read(ch);
                throw new IOChannelReadException(
                    String.format("received a shell command error: %s", error));

            default:
                throw new IOChannelReadException(
                    String.format("received invalid shell command result code %d", resCode));
        }
    }

    @Immutable
    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    static final class MininetTopo
    {
        private static final MininetTopo EMPTY_TOPO =
            new MininetTopo(ImmutableMap.of(), ImmutableMap.of(), ImmutableMap.of(), ImmutableMap.of());

        static MininetTopo empty()
        {
            return EMPTY_TOPO;
        }

        private final ImmutableMap<DatapathId, MininetSwitch>                          switchMap;
        private final ImmutableMap<DatapathId, ImmutableList<MininetHost>>             switchNeighborsMap;
        private final ImmutableMap<String, MininetHost>                                hostMap;
        private final ImmutableMap<String, ImmutableList<Pair<MininetSwitch, OFPort>>> hostNeighborsMap;

        MininetTopo( ImmutableMap<DatapathId, MininetSwitch> switchMap,
                     ImmutableMap<DatapathId, ImmutableList<MininetHost>> switchNeighborsMap,
                     ImmutableMap<String, MininetHost> hostMap,
                     ImmutableMap<String, ImmutableList<Pair<MininetSwitch, OFPort>>> hostNeighborsMap )
        {
            this.switchMap = switchMap;
            this.switchNeighborsMap = switchNeighborsMap;
            this.hostMap = hostMap;
            this.hostNeighborsMap = hostNeighborsMap;
        }

        Possible<MininetSwitch> getSwitch( DatapathId dpid )
        {
            return Possible.ofNullable(switchMap.get(dpid));
        }

        ImmutableList<MininetHost> getSwitchNeighborHosts( DatapathId dpid )
        {
            return switchNeighborsMap.getOrDefault(dpid, ImmutableList.of());
        }

        Possible<MininetHost> getHost( String addr )
        {
            return Possible.ofNullable(hostMap.get(addr));
        }

        ImmutableList<Pair<MininetSwitch, OFPort>> getHostNeighborSwitchPorts( String addr )
        {
            return hostNeighborsMap.getOrDefault(addr, ImmutableList.of());
        }

        ImmutableCollection<MininetSwitch> getSwitches()
        {
            return switchMap.values();
        }

        ImmutableCollection<MininetHost> getHosts()
        {
            return hostMap.values();
        }

        @Override
        public String toString()
        {
            StringJoiner sj = StringUtils.linesJoiner();

            if (switchMap.isEmpty()) {
                sj.add("- (No switches)");
            }
            else {
                switchMap.forEach(( dpid, sw ) -> {
                    sj.add(fmt("- Switch %s", sw));
                    for (MininetHost host : getSwitchNeighborHosts(dpid)) {
                        sj.add(fmt("-- has neighbor host %s", host));
                    }
                });
            }

            if (hostMap.isEmpty()) {
                sj.add("- (No hosts)");
            }
            else {
                hostMap.forEach(( addr, host ) -> {
                    sj.add(fmt("- Host %s", host));
                    for (Pair<MininetSwitch, OFPort> pair : getHostNeighborSwitchPorts(addr)) {
                        sj.add(fmt("-- has neighbor switch-port %s[%s]", pair.getFirst().getName(), pair.getSecond()));
                    }
                });
            }

            return sj.toString();
        }
    }

    @Immutable
    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    static final class MininetLinkInfoConfig
    {
        private static final MininetLinkInfoConfig EMPTY_CONFIG = new MininetLinkInfoConfig(ImmutableMap.of());

        static MininetLinkInfoConfig empty()
        {
            return EMPTY_CONFIG;
        }

        private final ImmutableMap<CommonPair<String>, MininetLinkInfo> linksInfoMap;

        MininetLinkInfoConfig( ImmutableMap<CommonPair<String>, MininetLinkInfo> linksInfoMap )
        {
            this.linksInfoMap = linksInfoMap;
        }

        Possible<MininetLinkInfo> getLinkInfo( String srcName, String destName )
        {
            return Possible.ofNullable(linksInfoMap.get(CommonPair.of(srcName, destName)));
        }

        ImmutableCollection<MininetLinkInfo> getLinkInfos()
        {
            return linksInfoMap.values();
        }

        @Override
        public String toString()
        {
            if (linksInfoMap.isEmpty()) {
                return "- (No links)";
            }
            else {
                StringJoiner sj = StringUtils.linesJoiner();

                for (MininetLinkInfo linkInfo : getLinkInfos()) {
                    sj.add(fmt("- Link %s", linkInfo));
                }

                return sj.toString();
            }
        }
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    private static final class MininetRequest
    {
        static MininetRequest forTopology()
        {
            return new MininetRequest(RequestType.TOPOLOGY, new CompletableFuture<MininetTopo>(),
                Optional.empty(),
                Optional.empty());
        }

        static MininetRequest forQosConfig( ImmutableList<MininetQoSSetup> qosSetups )
        {
            return new MininetRequest(RequestType.LINF_INFO_CONFIG, new CompletableFuture<MininetLinkInfoConfig>(),
                Optional.of(qosSetups),
                Optional.empty());
        }

        static MininetRequest forShellCommand( String shellCommand )
        {
            return new MininetRequest(RequestType.SHELL_COMMAND, new CompletableFuture<Void>(),
                Optional.empty(),
                Optional.of(shellCommand));
        }

        private final RequestType                              type;
        private final CompletableFuture<?>                     future;
        private final Optional<ImmutableList<MininetQoSSetup>> qosSetups;
        private final Optional<String>                         shellCommand;

        private MininetRequest( RequestType type,
                                CompletableFuture<?> future,
                                Optional<ImmutableList<MininetQoSSetup>> qosSetups,
                                Optional<String> shellCommand )
        {
            this.type = type;
            this.future = future;
            this.qosSetups = qosSetups;
            this.shellCommand = shellCommand;
        }

        RequestType getType()
        {
            return type;
        }

        @SuppressWarnings( "unchecked" )
        CompletableFuture<MininetTopo> getFutureForTopology()
        {
            Preconditions.checkState(type.equals(RequestType.TOPOLOGY), "expected topology request");
            return (CompletableFuture<MininetTopo>)future;
        }

        @SuppressWarnings( "unchecked" )
        CompletableFuture<MininetLinkInfoConfig> getFutureForQoSConfig()
        {
            Preconditions.checkState(type.equals(RequestType.LINF_INFO_CONFIG), "expected QoS-config request");
            return (CompletableFuture<MininetLinkInfoConfig>)future;
        }

        @SuppressWarnings( "unchecked" )
        CompletableFuture<Void> getFutureForShellCommand()
        {
            Preconditions.checkState(type.equals(RequestType.SHELL_COMMAND), "expected shell command request");
            return (CompletableFuture<Void>)future;
        }

        ImmutableList<MininetQoSSetup> getQoSSetups()
        {
            return qosSetups.orElseThrow(() -> new NoSuchElementException("not available for this request"));
        }

        String getShellCommand()
        {
            return shellCommand.orElseThrow(() -> new NoSuchElementException("not available for this request"));
        }

        void completeTopology( MininetTopo topo )
        {
            getFutureForTopology().complete(topo);
        }

        void completeQosConfig( MininetLinkInfoConfig cfg )
        {
            getFutureForQoSConfig().complete(cfg);
        }

        // to bypass FindBugs null check
        private static final Void DUMMY = MoreObjects.getRuntimeNull();

        void completeShellCommand()
        {
            getFutureForShellCommand().complete(DUMMY);
        }

        void abort( String errorMsg )
        {
            future.completeExceptionally(new RuntimeException(errorMsg));
        }
    }

    private static enum RequestType
    {
        TOPOLOGY, LINF_INFO_CONFIG, SHELL_COMMAND;
    }

    private static String fmt( String format, Object... args )
    {
        return String.format(format, args);
    }
}
