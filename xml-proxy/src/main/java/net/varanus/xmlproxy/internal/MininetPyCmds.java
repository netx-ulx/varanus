package net.varanus.xmlproxy.internal;


import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.OFPort;

import com.google.common.collect.ImmutableMap;

import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.collect.CommonPair;
import net.varanus.util.collect.Pair;
import net.varanus.util.collect.builder.ImmutableMapBuilder;
import net.varanus.util.openflow.NodePortUtils;
import net.varanus.util.openflow.types.NodeId;
import net.varanus.util.openflow.types.NodePort;
import net.varanus.util.text.StringUtils;


/**
 * 
 */
final class MininetPyCmds
{
    static final Switches SWITCHES = new Switches();

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    static final class Switches
    {
        String command()
        {
            return "map("
                   + "lambda S : S.name + '|' + S.dpid_as_hex(add_prefix=True) + '|' + str(S.is_remote()), "
                   + "switches())";
        }

        ImmutableMap<DatapathId, MininetSwitch> parseResult( String res ) throws IllegalArgumentException
        {
            ImmutableMapBuilder<DatapathId, MininetSwitch> switchMap = ImmutableMapBuilder.create();
            for (MininetSwitch sw : StringUtils.parseList(res, this::parseSwitch)) {
                switchMap.put(sw.getDpid(), sw);
            }
            return switchMap.build();
        }

        private MininetSwitch parseSwitch( String sw ) throws IllegalArgumentException
        {
            return MininetSwitch.parse(StringUtils.parseStringLiteral(sw.trim()));
        }

        private Switches()
        {
            // private constructor
        }
    }

    static final Hosts HOSTS = new Hosts();

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    static final class Hosts
    {
        String command()
        {
            return "map("
                   + "lambda H : H.name + '|' + H.IP() + '|' + str(H.is_remote()) + '|' + str(H.is_visible()), "
                   + "hosts())";
        }

        ImmutableMap<String, MininetHost> parseResult( String res ) throws IllegalArgumentException
        {
            ImmutableMapBuilder<String, MininetHost> hostMap = ImmutableMapBuilder.create();
            for (MininetHost host : StringUtils.parseList(res, this::parseHost)) {
                hostMap.put(host.getAddress(), host);
            }
            return hostMap.build();
        }

        private MininetHost parseHost( String host ) throws IllegalArgumentException
        {
            return MininetHost.parse(StringUtils.parseStringLiteral(host.trim()));
        }

        private Hosts()
        {
            // private constructor
        }
    }

    static final SwitchNeighborHosts SWITCH_NEIGHBOR_HOSTS = new SwitchNeighborHosts();

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    static final class SwitchNeighborHosts
    {
        String command( MininetSwitch sw )
        {
            return String.format("map(lambda L : L.ndst.IP(), hsrclinks('%s'))",
                sw.getName());
        }

        List<String> parseResult( String res ) throws IllegalArgumentException
        {
            return StringUtils.parseList(res, this::parseHostAddress);
        }

        private String parseHostAddress( String hostAddr ) throws IllegalArgumentException
        {
            hostAddr = StringUtils.parseStringLiteral(hostAddr.trim());
            return IPv4Address.of(hostAddr).toString();
        }

        private SwitchNeighborHosts()
        {
            // private constructor
        }
    }

    static final HostNeighborSwitchPorts HOST_NEIGHBOR_SWITCH_PORTS = new HostNeighborSwitchPorts();

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    static final class HostNeighborSwitchPorts
    {
        String command( MininetHost host )
        {
            return String.format(
                "map(lambda L : '{0}[{1}]'.format(L.ndst.dpid_as_hex(add_prefix=True), L.idst.ofport), ssrclinks('%s'))",
                host.getName());
        }

        List<Pair<DatapathId, OFPort>> parseResult( String res ) throws IllegalArgumentException
        {
            return StringUtils.parseList(res, this::parseSwitchPort);
        }

        private Pair<DatapathId, OFPort> parseSwitchPort( String swPort ) throws IllegalArgumentException
        {
            swPort = StringUtils.parseStringLiteral(swPort.trim());
            NodePort nodePort = NodePortUtils.parseNodePort(swPort, NodeId.NIL_ID_ALIASER);
            return Pair.of(nodePort.getNodeId().getDpid(), nodePort.getPortId().getOFPort());
        }

        private HostNeighborSwitchPorts()
        {
            // private constructor
        }
    }

    static final GetLinksInfo GET_LINKS_INFO = new GetLinksInfo();

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    static final class GetLinksInfo
    {
        String command()
        {
            return "map("
                   + "lambda L :"
                   + " '{{{L.nsrc};{L.isrc};{L.ndst};{L.idst};{B};{N}}}'.format(L=L, B=L.qos[0] or '', N=L.qos[1] or '')"
                   + ",links())";
        }

        ImmutableMap<CommonPair<String>, MininetLinkInfo> parseResult( String res ) throws IllegalArgumentException
        {
            ImmutableMapBuilder<CommonPair<String>, MininetLinkInfo> linksInfoSMap = ImmutableMapBuilder.create();
            for (MininetLinkInfo info : StringUtils.parseList(res, this::parseLinkInfo)) {
                linksInfoSMap.put(CommonPair.of(info.getSrcName(), info.getDestName()), info);
            }
            return linksInfoSMap.build();
        }

        private MininetLinkInfo parseLinkInfo( String linkInfo ) throws IllegalArgumentException
        {
            return MininetLinkInfo.parse(StringUtils.parseStringLiteral(linkInfo.trim()));
        }

        private GetLinksInfo()
        {
            // private constructor
        }
    }

    static final SetLinkQoS SET_LINK_QOS = new SetLinkQoS();

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    static final class SetLinkQoS
    {
        String command( MininetQoSSetup setup )
        {
            return String.format("setqos('%s', '%s', '%s', '%s')",
                setup.getSrcName(), setup.getDestName(),
                setup.getBandwidthQoS().getCommandArg(), setup.getNetemQoS().getCommandArgs());
        }

        boolean parseResult( String res ) throws IllegalArgumentException
        {
            return StringUtils.convertToBoolean(res);
        }

        private SetLinkQoS()
        {
            // private constructor
        }
    }

    static final DelLinkQoS DEL_LINK_QOS = new DelLinkQoS();

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    static final class DelLinkQoS
    {
        String command( String src, String dest )
        {
            return String.format("delqos('%s', '%s')", src, dest);
        }

        boolean parseResult( String res ) throws IllegalArgumentException
        {
            return StringUtils.convertToBoolean(res);
        }

        private DelLinkQoS()
        {
            // private constructor
        }
    }

    private MininetPyCmds()
    {
        // not used
    }
}
