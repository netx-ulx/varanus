package net.varanus.xmlproxy.internal;


import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.annotation.ParametersAreNonnullByDefault;

import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;

import com.google.common.collect.ImmutableList;

import net.varanus.infoprotocol.RouteReply;
import net.varanus.infoprotocol.StatisticsReply;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.collect.CommonPair;
import net.varanus.util.collect.Pair;
import net.varanus.util.functional.PossibleDouble;
import net.varanus.util.functional.PossibleInt;
import net.varanus.util.functional.PossibleLong;
import net.varanus.util.openflow.types.BidiNodePorts;
import net.varanus.util.openflow.types.Flow;
import net.varanus.util.openflow.types.FlowedUnidiNodePorts;
import net.varanus.util.openflow.types.NodeId;
import net.varanus.util.openflow.types.NodePort;
import net.varanus.util.openflow.types.PortId;
import net.varanus.util.openflow.types.UnidiNodePorts;
import net.varanus.util.time.TimeDouble;
import net.varanus.util.time.TimeDoubleUnit;
import net.varanus.util.time.TimeLong;
import net.varanus.util.unitvalue.si.InfoDouble;
import net.varanus.util.unitvalue.si.InfoDoubleUnit;
import net.varanus.util.unitvalue.si.InfoLong;
import net.varanus.util.unitvalue.si.InfoLongUnit;
import net.varanus.util.unitvalue.si.MetricLongPrefix;
import net.varanus.xmlproxy.internal.MininetLinkInfo.BandwidthQoS;
import net.varanus.xmlproxy.internal.MininetLinkInfo.NetemQoS;
import net.varanus.xmlproxy.internal.TopologyCacher.TopologyState;
import net.varanus.xmlproxy.util.Percentage;
import net.varanus.xmlproxy.xml.Helper;
import net.varanus.xmlproxy.xml.Helper.LinkType;
import net.varanus.xmlproxy.xml.XMLCommandError;
import net.varanus.xmlproxy.xml.XMLCommandReply;
import net.varanus.xmlproxy.xml.XMLCommandRequest;
import net.varanus.xmlproxy.xml.XMLLinkConfigError;
import net.varanus.xmlproxy.xml.XMLLinkConfigReply;
import net.varanus.xmlproxy.xml.XMLLinkConfigRequest;
import net.varanus.xmlproxy.xml.XMLLinkStateError;
import net.varanus.xmlproxy.xml.XMLLinkStateReply;
import net.varanus.xmlproxy.xml.XMLLinkStateRequest;
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
import net.varanus.xmlproxy.xml.types.LatencyStat;
import net.varanus.xmlproxy.xml.types.Link;
import net.varanus.xmlproxy.xml.types.LinkConfig;
import net.varanus.xmlproxy.xml.types.LossRateStat;
import net.varanus.xmlproxy.xml.types.Node;


/**
 * 
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
final class XML
{
    static XMLTopologyReply buildXMLTopologyReply( TopologyState topoState )
    {
        List<Node> nodes = new ArrayList<>();
        List<Link> links = new ArrayList<>();
        for (NodeId swId : topoState.getSwitchIds()) {
            topoState.getMininetSwitch(swId.getDpid())
                .ifPresent(( mnSw ) -> {
                    Node sw = Helper.buildSwitch(mnSw.getDpid().toString(), mnSw.getName(), !mnSw.isRemote());
                    nodes.add(sw);
                    for (MininetHost mnHost : topoState.getMininetSwitchNeighborHosts(mnSw.getDpid())) {
                        Node host = Helper.buildHost(mnHost.getAddress(), mnHost.getName(), !mnHost.isRemote(),
                            mnHost.isVisible());
                        nodes.add(host);
                        Link link = buildHostLink(mnSw.getDpid(), mnHost.getAddress(), topoState);
                        links.add(link);
                    }
                })
                .ifAbsent(() -> {
                    Node sw = Helper.buildSwitch(swId.getDpid().toString(), "unmanaged", false);
                    nodes.add(sw);
                });
        }
        for (BidiNodePorts bidiLink : topoState.getBidiLinks()) {
            DatapathId dpid1 = bidiLink.getMin().getNodeId().getDpid();
            DatapathId dpid2 = bidiLink.getMax().getNodeId().getDpid();
            Link link = buildSwitchLink(dpid1, dpid2, topoState, true);
            links.add(link);
        }

        for (BidiNodePorts bidiLink : topoState.getDisabledBidiLinks()) {
            DatapathId dpid1 = bidiLink.getMin().getNodeId().getDpid();
            DatapathId dpid2 = bidiLink.getMax().getNodeId().getDpid();
            Link link = buildSwitchLink(dpid1, dpid2, topoState, false);
            links.add(link);
        }

        XMLTopologyReply xmlReply = new XMLTopologyReply();
        xmlReply.setNodes(nodes);
        xmlReply.setLinks(links);
        return xmlReply;
    }

    private static Link buildHostLink( DatapathId swDpid, String hostAddr, TopologyState topoState )
    {
        String swId = Helper.buildSwitchId(swDpid.toString());
        String hostId = Helper.buildHostId(hostAddr);

        LinkConfig swCfg = buildLinkConfig(topoState.getMininetLinkInfo(swDpid, hostAddr)
            .orElseThrow(() -> new IllegalStateException("illegal topology state")));

        LinkConfig hostCfg = buildLinkConfig(topoState.getMininetLinkInfo(hostAddr, swDpid)
            .orElseThrow(() -> new IllegalStateException("illegal topology state")));

        boolean enabled = true; // we never disable host links
        return Helper.buildLink(swId, hostId, enabled, swCfg, hostCfg);
    }

    private static Link buildSwitchLink( DatapathId dpid1, DatapathId dpid2, TopologyState topoState, boolean enabled )
    {
        String swId1 = Helper.buildSwitchId(dpid1.toString());
        String swId2 = Helper.buildSwitchId(dpid2.toString());

        LinkConfig cfg1 = buildLinkConfig(topoState.getMininetLinkInfo(dpid1, dpid2)
            .orElseThrow(() -> new IllegalStateException("illegal topology state")));

        LinkConfig cfg2 = buildLinkConfig(topoState.getMininetLinkInfo(dpid2, dpid1)
            .orElseThrow(() -> new IllegalStateException("illegal topology state")));

        return Helper.buildLink(swId1, swId2, enabled, cfg1, cfg2);
    }

    private static LinkConfig buildLinkConfig( MininetLinkInfo linkInfo )
    {
        LinkConfig cfg = new LinkConfig();
        linkInfo.getBandwidthQoS().getBandwidth().ifPresent(InfoDoubleUnit.MEGABITS, ( mbits ) -> {
            cfg.setBandwidth(mbits);
        });
        linkInfo.getNetemQoS().getDelay().ifPresent(TimeUnit.MILLISECONDS, ( millis ) -> {
            cfg.setDelay(millis);
        });
        linkInfo.getNetemQoS().getLossRate().ifPresent(( percent ) -> {
            cfg.setLossRate(percent);
        });
        return cfg;
    }

    static FlowedUnidiNodePorts getStatRequestLink( XMLStatRequest request, TopologyState topoState )
        throws IllegalArgumentException
    {
        Pair<CommonPair<String>, LinkType> p = Helper.getLinkMembers(request.getLink(), request.getDirection());
        CommonPair<String> members = p.getFirst();
        if (!p.getSecond().equals(LinkType.SWITCH_SWITCH))
            throw new IllegalArgumentException("expected \"switch-switch\" link");

        String srcDpid = Helper.getSwitchDpid(members.getFirst());
        String destDpid = Helper.getSwitchDpid(members.getSecond());
        String match = Helper.adaptMatch(request.getMatch());

        NodeId sourceSwId = NodeId.parse(srcDpid);
        NodeId targetSwId = NodeId.parse(destDpid);
        Flow flow = Flow.parse(match, OFVersion.OF_14);

        return topoState.getUnidiLink(sourceSwId, targetSwId)
            .map(link -> link.flowed(flow))
            .orElseThrow(() -> new IllegalArgumentException("unknown link"));
    }

    static XMLTopologyError buildXMLTopologyError( String errorMsg )
    {
        XMLTopologyError xmlError = new XMLTopologyError();
        xmlError.setMessage(errorMsg);
        return xmlError;
    }

    static XMLStatReply buildXMLStatReply( XMLStatRequest request, StatisticsReply statReply )
    {
        XMLStatReply xmlReply = new XMLStatReply();
        xmlReply.setLink(request.getLink());
        xmlReply.setDirection(request.getDirection());
        xmlReply.setMatch(request.getMatch());
        setXMLLatencyStat(xmlReply, statReply);
        setXMLLossRateStat(xmlReply, statReply);
        setXMLThroughput(xmlReply, statReply);
        setXMLTotalThroughput(xmlReply, statReply);
        setXMLDroppedPackets(xmlReply, statReply);
        xmlReply.setTimestamp(statReply.getTimestamp().toEpochMilli());
        return xmlReply;
    }

    private static void setXMLLatencyStat( XMLStatReply xmlReply, StatisticsReply statReply )
    {
        statReply.getLatency().ifPresent(TimeDoubleUnit.MILLISECONDS, ( millis ) -> {
            LatencyStat xmlLatency = new LatencyStat();
            xmlLatency.setProbe(statReply.isProbeLatency());
            xmlLatency.setValue(millis);
            xmlReply.setLatency(xmlLatency);
        });
    }

    private static void setXMLLossRateStat( XMLStatReply xmlReply, StatisticsReply statReply )
    {
        statReply.getByteLoss().ifPresent(( ratio ) -> {
            LossRateStat xmlLossRate = new LossRateStat();
            xmlLossRate.setProbe(statReply.isProbeLoss());
            xmlLossRate.setValue(ratio);
            xmlReply.setLossRate(xmlLossRate);
        });
    }

    private static void setXMLThroughput( XMLStatReply xmlReply, StatisticsReply statReply )
    {
        statReply.getThroughput().asLong().ifPresent(InfoLongUnit.BITS, ( bits ) -> {
            xmlReply.setThroughput(bits);
        });
    }

    private static void setXMLTotalThroughput( XMLStatReply xmlReply, StatisticsReply statReply )
    {
        InfoLong matched = statReply.getReceptionRate().asLong();
        InfoLong unmatched = statReply.getUnmatchedReceptionRate().asLong();

        if (matched.isPresent() && unmatched.isPresent()) {
            xmlReply.setTotalThroughput(matched.plus(unmatched).in(InfoLongUnit.BITS));
        }
        else if (matched.isPresent()) { // && !unmatched.isPresent()
            xmlReply.setTotalThroughput(matched.in(InfoLongUnit.BITS));
        }
        else if (unmatched.isPresent()) { // && !thrpt.isPresent()
            xmlReply.setTotalThroughput(unmatched.in(InfoLongUnit.BITS));
        }
    }

    private static void setXMLDroppedPackets( XMLStatReply xmlReply, StatisticsReply statReply )
    {
        statReply.getSourcePacketDropRate().asLong().ifPresent(MetricLongPrefix.UNIT, ( pps ) -> {
            xmlReply.setDroppedPackets(pps);
        });
    }

    static FlowedUnidiNodePorts getRouteRequestConnection( XMLRouteRequest request,
                                                           TopologyState topoState,
                                                           boolean autoSetMatch )
        throws IllegalArgumentException
    {
        String sourceHostAddr = Helper.getHostAddress(request.getFrom());
        String targetHostAddr = Helper.getHostAddress(request.getTo());
        String match = autoSetMatch ? buildRouteMatch(sourceHostAddr, targetHostAddr)
                                    : Helper.adaptMatch(request.getMatch());

        NodePort sourceSwPort = findNeighborSwitchPort(sourceHostAddr, topoState);
        NodePort targetSwPort = findNeighborSwitchPort(targetHostAddr, topoState);
        Flow flow = Flow.parse(match, OFVersion.OF_14);

        return FlowedUnidiNodePorts.of(sourceSwPort, targetSwPort, flow);
    }

    private static String buildRouteMatch( String sourceHostAddr, String targetHostAddr )
    {
        return String.format("[ eth_type=0x0800, ipv4_src=%s, ipv4_dst=%s ]",
            sourceHostAddr, targetHostAddr);
    }

    private static NodePort findNeighborSwitchPort( String hostAddr, TopologyState topoState )
        throws IllegalArgumentException
    {
        List<Pair<MininetSwitch, OFPort>> neighSwPorts = topoState.getMininetHostNeighborSwitchPorts(hostAddr);
        int numNeighbors = neighSwPorts.size();
        if (numNeighbors == 0) {
            throw new IllegalArgumentException(
                String.format("host with address %s has no neighbor switches", hostAddr));
        }
        else if (numNeighbors > 1) {
            throw new IllegalArgumentException(
                String.format("host with address %s has more than one neighbor switch", hostAddr));
        }
        else {
            Pair<MininetSwitch, OFPort> p = neighSwPorts.get(0);
            return NodePort.of(NodeId.of(p.getFirst().getDpid()), PortId.of(p.getSecond()));
        }
    }

    static XMLStatError buildXMLStatError( XMLStatRequest request, String errorMsg )
    {
        XMLStatError xmlError = new XMLStatError();
        xmlError.setLink(request.getLink());
        xmlError.setDirection(request.getDirection());
        xmlError.setMatch(request.getMatch());
        xmlError.setMessage(errorMsg);
        return xmlError;
    }

    static XMLRouteReply buildXMLRouteReply( XMLRouteRequest request, RouteReply routeReply )
    {
        XMLRouteReply xmlReply = new XMLRouteReply();
        xmlReply.setFrom(request.getFrom());
        xmlReply.setTo(request.getTo());
        xmlReply.setMatch(request.getMatch());
        xmlReply.setLinks(buildXMLRouteLinks(request, routeReply.getLinks()));
        return xmlReply;
    }

    static XMLRouteError buildXMLRouteError( XMLRouteRequest request, String errorMsg )
    {
        XMLRouteError xmlError = new XMLRouteError();
        xmlError.setFrom(request.getFrom());
        xmlError.setTo(request.getTo());
        xmlError.setMatch(request.getMatch());
        xmlError.setMessage(errorMsg);
        return xmlError;
    }

    private static List<String> buildXMLRouteLinks( XMLRouteRequest request, ImmutableList<UnidiNodePorts> links )
    {
        List<String> xmlLinks = new ArrayList<>();
        if (!links.isEmpty()) {
            { // add the first host link
                String id1 = request.getFrom();
                String id2 = Helper.buildSwitchId(links.get(0).getSource().getNodeId().getDpid().toString());
                xmlLinks.add(Helper.buildLinkId(id1, id2));
            }
            // add the switch links
            for (UnidiNodePorts link : links) {
                String id1 = Helper.buildSwitchId(link.getSource().getNodeId().getDpid().toString());
                String id2 = Helper.buildSwitchId(link.getTarget().getNodeId().getDpid().toString());
                xmlLinks.add(Helper.buildLinkId(id1, id2));
            }
            { // add the last host link
                int last = links.size() - 1;
                String id1 = Helper.buildSwitchId(links.get(last).getTarget().getNodeId().getDpid().toString());
                String id2 = request.getTo();
                xmlLinks.add(Helper.buildLinkId(id1, id2));
            }
        }
        return xmlLinks;
    }

    static MininetQoSSetup getQoSSetup( XMLLinkConfigRequest request, TopologyState topoState )
        throws IllegalArgumentException
    {
        Pair<CommonPair<String>, LinkType> p = Helper.getLinkMembers(request.getLink(), request.getDirection());
        MininetLinkInfo linkInfo = getLinkInfo(p, topoState);

        String srcName = linkInfo.getSrcName();
        String destName = linkInfo.getDestName();
        InfoDouble bandwidth = getQoSBandwidth(request);
        TimeLong delay = getQoSDelay(request);
        Percentage lossRate = getQoSLossRate(request);

        BandwidthQoS bandQoS = BandwidthQoS.of(bandwidth);
        NetemQoS netemQoS = new NetemQoS(delay, lossRate);
        MininetQoSSetup setup = new MininetQoSSetup(srcName, destName, bandQoS, netemQoS);

        return setup;
    }

    private static TimeLong getQoSDelay( XMLLinkConfigRequest request ) throws IllegalArgumentException
    {
        try {
            PossibleLong millis = PossibleLong.ofOptional(request.getDelay());
            return TimeLong.ofPossible(millis, TimeUnit.MILLISECONDS);
        }
        catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("invalid delay: %s", e.getMessage()));
        }
    }

    private static Percentage getQoSLossRate( XMLLinkConfigRequest request ) throws IllegalArgumentException
    {
        try {
            PossibleInt percent = PossibleInt.ofOptional(request.getLossRate());
            return Percentage.ofPossible(percent);
        }
        catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("invalid loss rate: %s", e.getMessage()));
        }
    }

    private static InfoDouble getQoSBandwidth( XMLLinkConfigRequest request ) throws IllegalArgumentException
    {
        try {
            PossibleDouble mbits = PossibleDouble.ofOptional(request.getBandwidth());
            return InfoDouble.ofPossible(mbits, InfoDoubleUnit.MEGABITS);
        }
        catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("invalid bandwidth: %s", e.getMessage()));
        }
    }

    static XMLLinkConfigReply buildXMLLinkConfigReply( XMLLinkConfigRequest request,
                                                       BandwidthQoS bandQoS,
                                                       NetemQoS netemQoS )
    {
        XMLLinkConfigReply xmlReply = new XMLLinkConfigReply();
        xmlReply.setLink(request.getLink());
        xmlReply.setDirection(request.getDirection());
        bandQoS.getBandwidth().ifPresent(InfoDoubleUnit.MEGABITS, ( mbits ) -> {
            xmlReply.setBandwidth(mbits);
        });
        netemQoS.getDelay().ifPresent(TimeUnit.MILLISECONDS, ( millis ) -> {
            xmlReply.setDelay(millis);
        });
        netemQoS.getLossRate().ifPresent(( percent ) -> {
            xmlReply.setLossRate(percent);
        });
        return xmlReply;
    }

    static XMLLinkConfigError buildXMLLinkConfigError( XMLLinkConfigRequest request, String errorMsg )
    {
        XMLLinkConfigError xmlError = new XMLLinkConfigError();
        xmlError.setLink(request.getLink());
        xmlError.setDirection(request.getDirection());
        xmlError.setMessage(errorMsg);
        return xmlError;
    }

    static CommonPair<MininetHost> getCommandHosts( XMLCommandRequest request, TopologyState topoState )
        throws IllegalArgumentException
    {
        String srcAddr = Helper.getHostAddress(request.getFrom());
        String destAddr = Helper.getHostAddress(request.getTo());
        return CommonPair.of(
            getValidHost(srcAddr, topoState),
            getValidHost(destAddr, topoState));
    }

    private static MininetHost getValidHost( String addr, TopologyState topoState ) throws IllegalArgumentException
    {
        return topoState.getMininetHost(addr)
            .orElseThrow(
                () -> new IllegalArgumentException(String.format("unknown host with address %s", addr)));
    }

    static InfoDouble getIperfCommandBandwidth( XMLCommandRequest request ) throws IllegalArgumentException
    {
        double mbits = request.getBandwidth()
            .orElseThrow(() -> new IllegalArgumentException("bandwidth must be provided"));

        return InfoDouble.of(mbits, InfoDoubleUnit.MEGABITS);
    }

    static XMLCommandReply buildXMLPingCommandReply( XMLCommandRequest request, boolean enabled, TimeDouble latency )
    {
        XMLCommandReply xmlReply = new XMLCommandReply();
        xmlReply.setFrom(request.getFrom());
        xmlReply.setTo(request.getTo());
        xmlReply.setType(request.getType());
        xmlReply.setEnabled(enabled);
        latency.ifPresent(TimeDoubleUnit.MILLISECONDS, ( millis ) -> {
            xmlReply.setLatency(millis);
        });
        return xmlReply;
    }

    static XMLCommandReply buildXMLIperfCommandReply( XMLCommandRequest request, boolean enabled, InfoLong throughput )
    {
        XMLCommandReply xmlReply = new XMLCommandReply();
        xmlReply.setFrom(request.getFrom());
        xmlReply.setTo(request.getTo());
        xmlReply.setType(request.getType());
        xmlReply.setEnabled(enabled);
        throughput.ifPresent(InfoLongUnit.BITS, ( bits ) -> {
            xmlReply.setThroughput(bits);
        });
        return xmlReply;
    }

    static XMLCommandError buildXMLCommandError( XMLCommandRequest request, String errorMsg )
    {
        XMLCommandError xmlError = new XMLCommandError();
        xmlError.setFrom(request.getFrom());
        xmlError.setTo(request.getTo());
        xmlError.setType(request.getType());
        xmlError.setMessage(errorMsg);
        return xmlError;
    }

    static Pair<BidiNodePorts, Boolean> getLinkAndStateOp( XMLLinkStateRequest request, TopologyState topoState )
        throws IllegalArgumentException
    {
        Pair<CommonPair<String>, LinkType> p = Helper.getLinkMembers(request.getLink());
        CommonPair<String> members = p.getFirst();
        if (!p.getSecond().equals(LinkType.SWITCH_SWITCH))
            throw new IllegalArgumentException("expected switch-link");

        String dpid1 = Helper.getSwitchDpid(members.getFirst());
        String dpid2 = Helper.getSwitchDpid(members.getSecond());

        NodeId swId1 = NodeId.parse(dpid1);
        NodeId swId2 = NodeId.parse(dpid2);
        boolean enable = request.getEnabled();

        return topoState.getBidiLink(swId1, swId2)
            .or(() -> topoState.getDisabledBidiLink(swId1, swId2))
            .map(link -> Pair.of(link, enable))
            .orElseThrow(() -> new IllegalArgumentException("unknown switch-link"));
    }

    static XMLLinkStateReply buildXMLLinkStateReply( XMLLinkStateRequest request, boolean enabled )
    {
        XMLLinkStateReply reply = new XMLLinkStateReply();
        reply.setLink(request.getLink());
        reply.setEnabled(enabled);
        return reply;
    }

    static XMLLinkStateError buildXMLLinkStateError( XMLLinkStateRequest request, String errorMsg )
    {
        XMLLinkStateError xmlError = new XMLLinkStateError();
        xmlError.setLink(request.getLink());
        xmlError.setMessage(errorMsg);
        return xmlError;
    }

    static Pair<MininetLinkInfo, Boolean> getLinkInfoAndInjectOp( XMLTrafficInjectRequest request,
                                                                  TopologyState topoState )
        throws IllegalArgumentException
    {
        Pair<CommonPair<String>, LinkType> p = Helper.getLinkMembers(request.getLink(), request.getDirection());
        MininetLinkInfo info = getLinkInfo(p, topoState);
        return Pair.of(info, request.getEnabled());
    }

    static Pair<Flow, InfoDouble> getInjectFlowAndBandwidth( XMLTrafficInjectRequest request )
        throws IllegalArgumentException
    {
        Flow flow = getInjectFlow(request);
        InfoDouble bandwidth = getInjectBandwidth(request);
        return Pair.of(flow, bandwidth);
    }

    private static Flow getInjectFlow( XMLTrafficInjectRequest request ) throws IllegalArgumentException
    {
        String match = request.getMatch()
            .orElseThrow(() -> new IllegalArgumentException("match must be provided"));

        try {
            return Flow.parse(Helper.adaptMatch(match), OFVersion.OF_14);
        }
        catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("invalid match: %s", e.getMessage()));
        }
    }

    private static InfoDouble getInjectBandwidth( XMLTrafficInjectRequest request ) throws IllegalArgumentException
    {
        double mbits = request.getBandwidth()
            .orElseThrow(() -> new IllegalArgumentException("bandwidth must be provided"));

        try {
            InfoDouble bandwidth = InfoDouble.of(mbits, InfoDoubleUnit.MEGABITS);
            if (bandwidth.inBits() == 0)
                throw new IllegalArgumentException("bandwidth must not be zero bits/s");
            else
                return bandwidth;
        }
        catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format("invalid bandwidth: %s", e.getMessage()));
        }
    }

    static XMLTrafficInjectReply buildXMLTrafficInjectReply( XMLTrafficInjectRequest request,
                                                             boolean enabled,
                                                             Optional<Flow> flow,
                                                             InfoDouble bandwidth )
    {
        XMLTrafficInjectReply xmlReply = new XMLTrafficInjectReply();
        xmlReply.setLink(request.getLink());
        xmlReply.setDirection(request.getDirection());
        xmlReply.setEnabled(enabled);
        if (enabled) {
            flow.ifPresent(( f ) -> {
                xmlReply.setMatch(f.toMatchString());
            });
            bandwidth.ifPresent(InfoDoubleUnit.MEGABITS, ( mbits ) -> {
                xmlReply.setBandwidth(mbits);
            });
        }
        return xmlReply;
    }

    static XMLTrafficInjectError buildXMLTrafficInjectError( XMLTrafficInjectRequest request, String errorMsg )
    {
        XMLTrafficInjectError xmlError = new XMLTrafficInjectError();
        xmlError.setLink(request.getLink());
        xmlError.setDirection(request.getDirection());
        xmlError.setMessage(errorMsg);
        return xmlError;
    }

    private static MininetLinkInfo getLinkInfo( Pair<CommonPair<String>, LinkType> p, TopologyState topoState )
        throws IllegalArgumentException
    {
        CommonPair<String> members = p.getFirst();
        LinkType linkType = p.getSecond();
        switch (linkType) {
            case SWITCH_SWITCH: {
                String srcDpid = Helper.getSwitchDpid(members.getFirst());
                String destDpid = Helper.getSwitchDpid(members.getSecond());
                return topoState.getMininetLinkInfo(
                    NodeId.parse(srcDpid).getDpid(),
                    NodeId.parse(destDpid).getDpid())
                    .orElseThrow(() -> new IllegalArgumentException("unknown mininet switch-link"));
            }
            case SWITCH_HOST: {
                String srcDpid = Helper.getSwitchDpid(members.getFirst());
                String destAddr = Helper.getHostAddress(members.getSecond());
                return topoState.getMininetLinkInfo(
                    NodeId.parse(srcDpid).getDpid(),
                    destAddr)
                    .orElseThrow(() -> new IllegalArgumentException("unknown mininet switch-host-link"));
            }
            case HOST_SWITCH: {
                String srcAddr = Helper.getHostAddress(members.getFirst());
                String destDpid = Helper.getSwitchDpid(members.getSecond());
                return topoState.getMininetLinkInfo(
                    srcAddr,
                    NodeId.parse(destDpid).getDpid())
                    .orElseThrow(() -> new IllegalArgumentException("unknown mininet host-switch-link"));
            }
            default:
                throw new AssertionError("unexpected enum value");
        }
    }

    private XML()
    {
        // not used
    }
}
