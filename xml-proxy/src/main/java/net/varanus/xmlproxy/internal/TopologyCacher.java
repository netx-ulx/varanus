package net.varanus.xmlproxy.internal;


import java.util.Set;
import java.util.StringJoiner;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import org.jgrapht.UndirectedGraph;
import org.jgrapht.graph.UnmodifiableUndirectedGraph;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import net.varanus.configprotocol.LinkBandwidthReply;
import net.varanus.infoprotocol.TopologyReply;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.collect.CollectionUtils;
import net.varanus.util.collect.Pair;
import net.varanus.util.concurrent.InterruptibleRunnable;
import net.varanus.util.functional.Possible;
import net.varanus.util.functional.Report;
import net.varanus.util.functional.StreamUtils;
import net.varanus.util.graph.GraphUtils;
import net.varanus.util.openflow.types.BidiNodePorts;
import net.varanus.util.openflow.types.NodeId;
import net.varanus.util.openflow.types.NodePort;
import net.varanus.util.openflow.types.UnidiNodePorts;
import net.varanus.util.text.StringUtils;
import net.varanus.util.time.TimeLong;
import net.varanus.util.unitvalue.si.InfoDouble;
import net.varanus.xmlproxy.internal.MininetClient.MininetLinkInfoConfig;
import net.varanus.xmlproxy.internal.MininetClient.MininetTopo;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
final class TopologyCacher implements InterruptibleRunnable
{
    private static final Logger LOG = LoggerFactory.getLogger(TopologyCacher.class);

    private final InfoClient               infoClient;
    private final ConfigClient             cfgClient;
    private final MininetClient            mininetClient;
    private final TimeLong                 topoUpdatePeriod;
    private volatile Report<TopologyState> state;
    private final Object                   stateLock;

    TopologyCacher( InfoClient infoClient,
                    ConfigClient cfgClient,
                    MininetClient mininetClient,
                    TimeLong topoUpdatePeriod )
    {
        this.infoClient = infoClient;
        this.cfgClient = cfgClient;
        this.mininetClient = mininetClient;
        this.topoUpdatePeriod = topoUpdatePeriod;
        this.state = Report.of(TopologyState.empty());
        this.stateLock = new Object();
    }

    Report<TopologyState> getTopologyState()
    {
        return state;
    }

    Report<TopologyState> updateTopologyState( ImmutableList<MininetQoSSetup> qosSetups ) throws InterruptedException
    {
        LOG.debug("Updating topology state on request...");

        synchronized (stateLock) {
            Report<MininetLinkInfoConfig> mnLICfgReport = requestMininetLinkInfoConfig(qosSetups);
            if (mnLICfgReport.hasError()) {
                String errorMsg = mnLICfgReport.getError();
                LOG.warn("! Error while requesting mininet link-info config: {}", errorMsg);
                this.state = Report.ofError(errorMsg);
            }
            else {
                MininetLinkInfoConfig mnLICfg = mnLICfgReport.getValue();
                TopologyState newState = this.state.hasValue() ? this.state.getValue().updated(mnLICfg)
                                                               : TopologyState.emptyUpdated(mnLICfg);

                String errorMsg = null;
                for (UnidiNodePorts link : newState.getUnidiLinks()) {
                    Report<InfoDouble> bwReport = requestBandwidthConfig(link, newState);
                    if (bwReport.hasError()) {
                        errorMsg = bwReport.getError();
                        LOG.warn("! Error while requesting bandwidth configuration for link {}: {}",
                            link, errorMsg);
                        break;
                    }
                }

                if (errorMsg != null)
                    this.state = Report.ofError(errorMsg);
                else
                    this.state = Report.of(newState);
                LOG.trace("Current topology state:{}{}", System.lineSeparator(), this.state);
            }
            return getTopologyState();
        }
    }

    @Override
    public void runInterruptibly() throws InterruptedException
    {
        while (true) {
            update();
            topoUpdatePeriod.sleep();
        }
    }

    private void update() throws InterruptedException
    {
        LOG.debug("Updating topology state on schedule...");

        synchronized (stateLock) {
            Report<TopologyReply> infoTopoReport = requestInfoTopology();
            if (infoTopoReport.hasError()) {
                String errorMsg = infoTopoReport.getError();
                LOG.warn("! Error while requesting info topology: {}", errorMsg);
                this.state = Report.ofError(errorMsg);
            }
            else {
                TopologyReply reply = infoTopoReport.getValue();
                if (reply.hasError()) {
                    String errorMsg = reply.getError();
                    LOG.warn("! Received info topology error: {}", errorMsg);
                    this.state = Report.ofError(errorMsg);
                }
                else {
                    Report<MininetTopo> mnTopoReport = requestMininetTopology();
                    if (mnTopoReport.hasError()) {
                        String errorMsg = mnTopoReport.getError();
                        LOG.warn("! Error while requesting mininet topology: {}", errorMsg);
                        this.state = Report.ofError(errorMsg);
                    }
                    else {
                        Report<MininetLinkInfoConfig> mnLICfgReport = requestMininetLinkInfoConfig();
                        if (mnLICfgReport.hasError()) {
                            String errorMsg = mnLICfgReport.getError();
                            LOG.warn("! Error while requesting mininet link-info config: {}", errorMsg);
                            this.state = Report.ofError(errorMsg);
                        }
                        else {
                            MininetTopo mnTopo = mnTopoReport.getValue();
                            MininetLinkInfoConfig mnLICfg = mnLICfgReport.getValue();
                            TopologyState newState =
                                new TopologyState(reply.getGraph(), reply.getDisabledLinks(), mnTopo, mnLICfg);

                            String errorMsg = null;
                            for (UnidiNodePorts link : newState.getUnidiLinks()) {
                                Report<InfoDouble> bwReport = requestBandwidthConfig(link, newState);
                                if (bwReport.hasError()) {
                                    errorMsg = bwReport.getError();
                                    LOG.warn("! Error while requesting bandwidth configuration for link {}: {}",
                                        link, errorMsg);
                                    break;
                                }
                            }

                            if (errorMsg != null)
                                this.state = Report.ofError(errorMsg);
                            else
                                this.state = Report.of(newState);
                            LOG.trace("Current topology state:{}{}", System.lineSeparator(), this.state);
                        }
                    }
                }
            }
        }
    }

    private Report<TopologyReply> requestInfoTopology() throws InterruptedException
    {
        try {
            LOG.trace("Requesting topology from info-server...");
            return Report.of(infoClient.requestTopology().get());
        }
        catch (ExecutionException e) {
            return Report.ofError(StringUtils.getExceptionCauseString(e));
        }
    }

    private Report<MininetTopo> requestMininetTopology() throws InterruptedException
    {
        try {
            LOG.trace("Requesting topology from mininet...");
            return Report.of(mininetClient.requestTopology().get());
        }
        catch (ExecutionException e) {
            return Report.ofError(StringUtils.getExceptionCauseString(e));
        }
    }

    private Report<MininetLinkInfoConfig> requestMininetLinkInfoConfig() throws InterruptedException
    {
        try {
            LOG.trace("Requesting link-info configuration from mininet...");
            return Report.of(mininetClient.requestLinkInfoConfig().get());
        }
        catch (ExecutionException e) {
            return Report.ofError(StringUtils.getExceptionCauseString(e));
        }
    }

    private Report<MininetLinkInfoConfig> requestMininetLinkInfoConfig( ImmutableList<MininetQoSSetup> qosSetups )
        throws InterruptedException
    {
        try {
            LOG.trace("Requesting link-info configuration from mininet and setting up QoS...");
            return Report.of(mininetClient.requestLinkInfoConfig(qosSetups).get());
        }
        catch (ExecutionException e) {
            return Report.ofError(StringUtils.getExceptionCauseString(e));
        }
    }

    private Report<InfoDouble> requestBandwidthConfig( UnidiNodePorts link, TopologyState topoState )
        throws InterruptedException
    {
        try {
            InfoDouble bw = topoState.getMininetLinkInfo(link)
                .ifAbsent(() -> LOG.warn("Link {} is unmanaged by Mininet!", link))
                .map(linkInfo -> linkInfo.getBandwidthQoS().getBandwidth())
                .orElse(InfoDouble.absent());

            LOG.trace("Requesting configuration of a bandwidth of {} for link {}", bw, link);
            LinkBandwidthReply reply = cfgClient.requestLinkBandwidth(link, bw).get();
            if (reply.hasResult())
                return Report.of(reply.getBandwidth());
            else
                return Report.ofError(reply.getError());
        }
        catch (ExecutionException e) {
            return Report.ofError(StringUtils.getExceptionCauseString(e));
        }
    }

    @Immutable
    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    static final class TopologyState
    {
        private static TopologyState empty()
        {
            return new TopologyState(
                new UnmodifiableUndirectedGraph<>(GraphUtils.emptyUndirectedGraph()),
                ImmutableSet.of(),
                MininetTopo.empty(),
                MininetLinkInfoConfig.empty());
        }

        private static TopologyState emptyUpdated( MininetLinkInfoConfig mnLinkInfoCfg )
        {
            return new TopologyState(
                new UnmodifiableUndirectedGraph<>(GraphUtils.emptyUndirectedGraph()),
                ImmutableSet.of(),
                MininetTopo.empty(),
                mnLinkInfoCfg);
        }

        private final UndirectedGraph<NodeId, BidiNodePorts> topoGraph;
        private final ImmutableSet<BidiNodePorts>            disabledLinks;
        private final MininetTopo                            mnTopo;
        private final MininetLinkInfoConfig                  mnLinkInfoCfg;

        private TopologyState( UndirectedGraph<NodeId, BidiNodePorts> topoGraph,
                               ImmutableSet<BidiNodePorts> disabledLinks,
                               MininetTopo mnTopo,
                               MininetLinkInfoConfig mnLinkInfoConfig )
        {
            this.topoGraph = topoGraph;
            this.disabledLinks = disabledLinks;
            this.mnTopo = mnTopo;
            this.mnLinkInfoCfg = mnLinkInfoConfig;
        }

        private TopologyState updated( MininetLinkInfoConfig mnLinkInfoConfig )
        {
            return new TopologyState(this.topoGraph, this.disabledLinks, this.mnTopo, mnLinkInfoConfig);
        }

        Possible<BidiNodePorts> getBidiLink( NodeId swId1, NodeId swId2 )
        {
            return Possible.ofNullable(topoGraph.getEdge(swId1, swId2));
        }

        Possible<BidiNodePorts> getDisabledBidiLink( NodeId swId1, NodeId swId2 )
        {
            return Possible.ofOptional(CollectionUtils.findFirst(disabledLinks, ( link ) -> {
                NodePort firstNP = link.getMin();
                NodePort secondNP = link.getMax();
                return (firstNP.getNodeId().equals(swId1) && secondNP.getNodeId().equals(swId2))
                       || (secondNP.getNodeId().equals(swId1) && firstNP.getNodeId().equals(swId2));
            }));
        }

        Possible<UnidiNodePorts> getUnidiLink( NodeId sourceSwId, NodeId targetSwId )
        {
            return getBidiLink(sourceSwId, targetSwId).map(bidi -> {
                NodePort minNP = bidi.getMin();
                NodePort maxNP = bidi.getMax();
                if (sourceSwId.equals(minNP.getNodeId()) && targetSwId.equals(maxNP.getNodeId()))
                    return UnidiNodePorts.of(minNP, maxNP);
                else if (sourceSwId.equals(maxNP.getNodeId()) && targetSwId.equals(minNP.getNodeId()))
                    return UnidiNodePorts.of(maxNP, minNP);
                else
                    throw new IllegalStateException("illegal graph state");
            });
        }

        Set<NodeId> getSwitchIds()
        {
            return topoGraph.vertexSet();
        }

        Set<BidiNodePorts> getBidiLinks()
        {
            return topoGraph.edgeSet();
        }

        Set<BidiNodePorts> getDisabledBidiLinks()
        {
            return disabledLinks;
        }

        Set<UnidiNodePorts> getUnidiLinks()
        {
            return CollectionUtils.toFlatSet(getBidiLinks(),
                ( bidi ) -> Stream.of(
                    UnidiNodePorts.of(bidi.getMin(), bidi.getMax()),
                    UnidiNodePorts.of(bidi.getMax(), bidi.getMin())));
        }

        Possible<MininetSwitch> getMininetSwitch( DatapathId dpid )
        {
            return mnTopo.getSwitch(dpid);
        }

        ImmutableList<MininetHost> getMininetSwitchNeighborHosts( DatapathId dpid )
        {
            return mnTopo.getSwitchNeighborHosts(dpid);
        }

        Possible<MininetHost> getMininetHost( String addr )
        {
            return mnTopo.getHost(addr);
        }

        ImmutableList<Pair<MininetSwitch, OFPort>> getMininetHostNeighborSwitchPorts( String addr )
        {
            return mnTopo.getHostNeighborSwitchPorts(addr);
        }

        ImmutableCollection<MininetSwitch> getMininetSwitches()
        {
            return mnTopo.getSwitches();
        }

        ImmutableCollection<MininetHost> getMininetHosts()
        {
            return mnTopo.getHosts();
        }

        Possible<MininetLinkInfo> getMininetLinkInfo( UnidiNodePorts link )
        {
            return getMininetLinkInfo(
                link.getSource().getNodeId().getDpid(),
                link.getTarget().getNodeId().getDpid());
        }

        Possible<MininetLinkInfo> getMininetLinkInfo( DatapathId srcSwId, DatapathId destSwId )
        {
            return getMininetLinkInfo(
                getMininetSwitch(srcSwId).map(MininetSwitch::getName),
                getMininetSwitch(destSwId).map(MininetSwitch::getName));
        }

        Possible<MininetLinkInfo> getMininetLinkInfo( DatapathId srcSwId, String destHostAddr )
        {
            return getMininetLinkInfo(
                getMininetSwitch(srcSwId).map(MininetSwitch::getName),
                getMininetHost(destHostAddr).map(MininetHost::getName));
        }

        Possible<MininetLinkInfo> getMininetLinkInfo( String srcHostAddr, DatapathId destSwId )
        {
            return getMininetLinkInfo(
                getMininetHost(srcHostAddr).map(MininetHost::getName),
                getMininetSwitch(destSwId).map(MininetSwitch::getName));
        }

        private Possible<MininetLinkInfo> getMininetLinkInfo( Possible<String> srcName, Possible<String> destName )
        {
            if (srcName.isPresent() && destName.isPresent())
                return getMininetLinkInfo(srcName.get(), destName.get());
            else
                return Possible.absent();
        }

        Possible<MininetLinkInfo> getMininetLinkInfo( String srcName, String destName )
        {
            return mnLinkInfoCfg.getLinkInfo(srcName, destName);
        }

        ImmutableCollection<MininetLinkInfo> getMininetLinkInfos()
        {
            return mnLinkInfoCfg.getLinkInfos();
        }

        @Override
        public String toString()
        {
            StringJoiner sj = StringUtils.linesJoiner();

            sj.add(fmt("Topology nodes  : %s", StreamUtils.toString(this.topoGraph.vertexSet().stream().sorted())));
            sj.add(fmt("Topology links  : %s", StreamUtils.toString(this.topoGraph.edgeSet().stream().sorted())));
            sj.add(fmt("Disabled links  : %s", StreamUtils.toString(this.disabledLinks.stream().sorted())));
            sj.add(/**/"Mininet topology:").add(this.mnTopo.toString());
            sj.add(/**/"Mininet links   :").add(this.mnLinkInfoCfg.toString());

            return sj.toString();
        }

        private static String fmt( String format, Object... args )
        {
            return String.format(format, args);
        }
    }
}
