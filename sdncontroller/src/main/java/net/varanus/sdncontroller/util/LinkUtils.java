package net.varanus.sdncontroller.util;


import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Pattern;

import javax.annotation.ParametersAreNonnullByDefault;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.U64;

import net.floodlightcontroller.linkdiscovery.ILinkDiscovery.LDUpdate;
import net.floodlightcontroller.linkdiscovery.internal.LinkInfo;
import net.floodlightcontroller.routing.Link;
import net.varanus.sdncontroller.types.DatapathConnection;
import net.varanus.sdncontroller.types.DatapathLink;
import net.varanus.sdncontroller.types.EndpointKind;
import net.varanus.sdncontroller.types.FlowedConnection;
import net.varanus.sdncontroller.types.FlowedLink;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.openflow.MatchUtils;
import net.varanus.util.openflow.NodePortUtils;
import net.varanus.util.openflow.types.Directed;
import net.varanus.util.openflow.types.Flow;
import net.varanus.util.openflow.types.NodeId;
import net.varanus.util.openflow.types.NodePort;
import net.varanus.util.openflow.types.PortId;
import net.varanus.util.openflow.types.TrafficDirection;
import net.varanus.util.time.TimeDouble;
import net.varanus.util.time.TimeLong;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class LinkUtils
{
    private static final String  LINK_FMT_DESCRIPT;
    private static final Pattern LINK_FMT_PATTERN;
    private static final int     LINK_SPLIT_LIMIT;
    private static final Pattern LINK_SPLIT_PATTERN;

    private static final String  CONN_FMT_DESCRIPT;
    private static final Pattern CONN_FMT_PATTERN;
    private static final int     CONN_SPLIT_LIMIT;
    private static final Pattern CONN_SPLIT_PATTERN;

    private static final String  FLOWED_LINK_FMT_DESCRIPT;
    private static final Pattern FLOWED_LINK_FMT_PATTERN;
    private static final int     FLOWED_LINK_SPLIT_LIMIT;
    private static final Pattern FLOWED_LINK_SPLIT_PATTERN;

    private static final String  FLOWED_CONN_FMT_DESCRIPT;
    private static final Pattern FLOWED_CONN_FMT_PATTERN;
    private static final int     FLOWED_CONN_SPLIT_LIMIT;
    private static final Pattern FLOWED_CONN_SPLIT_PATTERN;

    static {//@formatter:off
        // ================================================================================================
        // DPID[PORT] > DPID[PORT]
        // ================================================================================================
        LINK_FMT_DESCRIPT    = NodePortUtils.NODE_PORT_FMT_DESCRIPT + " > " + NodePortUtils.NODE_PORT_FMT_DESCRIPT;
        LINK_FMT_PATTERN     = Pattern.compile(
                             NodePortUtils.NODE_PORT_FMT_PATTERN +

                             "[\\s]*" + // zero or more whitespace characters
                             ">"      + // the character '>'
                             "[\\s]*" + // zero or more whitespace characters

                             NodePortUtils.NODE_PORT_FMT_PATTERN);

        LINK_SPLIT_LIMIT   = 2;
        LINK_SPLIT_PATTERN = Pattern.compile(
                             ">");          // the character '>'


        // ================================================================================================
        // DPID[PORT] >> DPID[PORT]
        // ================================================================================================
        CONN_FMT_DESCRIPT  = NodePortUtils.NODE_PORT_FMT_DESCRIPT + " >> " + NodePortUtils.NODE_PORT_FMT_DESCRIPT;
        CONN_FMT_PATTERN   = Pattern.compile(
                             NodePortUtils.NODE_PORT_FMT_PATTERN +

                             "[\\s]*" + // zero or more whitespace characters
                             ">>"     + // the characters ">>"
                             "[\\s]*" + // zero or more whitespace characters

                             NodePortUtils.NODE_PORT_FMT_PATTERN);

        CONN_SPLIT_LIMIT   = 2;
        CONN_SPLIT_PATTERN = Pattern.compile(
                             ">>");         // the characters ">>"


        // ================================================================================================
        // DPID[PORT] > DPID[PORT] | v##[key1=value1,key2=value2,...]
        // ================================================================================================
        FLOWED_LINK_FMT_DESCRIPT  = LINK_FMT_DESCRIPT + " | " + MatchUtils.VER_MATCH_FMT_DESCRIPT;
        FLOWED_LINK_FMT_PATTERN   = Pattern.compile(
                                  LINK_FMT_PATTERN +

                                  "[\\s]*" + // zero or more whitespace characters
                                  "\\|"    + // the character '|'
                                  "[\\s]*" + // zero or more whitespace characters

                                  MatchUtils.VER_MATCH_FMT_PATTERN);

        FLOWED_LINK_SPLIT_LIMIT   = 2;
        FLOWED_LINK_SPLIT_PATTERN = Pattern.compile(
                                  "\\|");                        // the character '|'


        // ================================================================================================
        // DPID[PORT] >> DPID[PORT] | v##[key1=value1,key2=value2,...]
        // ================================================================================================
        FLOWED_CONN_FMT_DESCRIPT  = CONN_FMT_DESCRIPT + " | " + MatchUtils.VER_MATCH_FMT_DESCRIPT;
        FLOWED_CONN_FMT_PATTERN   = Pattern.compile(
                                    CONN_FMT_PATTERN +

                                    "[\\s]*" + // zero or more whitespace characters
                                    "\\|"    + // the character '|'
                                    "[\\s]*" + // zero or more whitespace characters

                                    MatchUtils.VER_MATCH_FMT_PATTERN);

        FLOWED_CONN_SPLIT_LIMIT   = 2;
        FLOWED_CONN_SPLIT_PATTERN = Pattern.compile(
                                    "\\|");                        // the character '|'
        // ================================================================================================
    }//@formatter:on

    public static DatapathLink parseLink( String s, Function<DatapathId, String> idAliaser )
        throws IllegalArgumentException
    {
        // be nice to the user and accept leading or trailing whitespace
        s = s.trim();

        if (LINK_FMT_PATTERN.matcher(s).matches()) {
            String[] split = LINK_SPLIT_PATTERN.split(s, LINK_SPLIT_LIMIT);
            try {
                NodePort srcNodePort = NodePort.parse(split[0], idAliaser);
                NodePort destNodePort = NodePort.parse(split[1], idAliaser);

                return DatapathLink.of(srcNodePort, destNodePort);
            }
            catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                    String.format("error while parsing link: %s", e.getMessage()),
                    e);
            }
        }
        else {
            throw new IllegalArgumentException(
                String.format(
                    "malformed link string (must have the form '%s')",
                    LINK_FMT_DESCRIPT));
        }
    }

    public static DatapathConnection parseConnection( String s, Function<DatapathId, String> idAliaser )
        throws IllegalArgumentException
    {
        // be nice to the user and accept leading or trailing whitespace
        s = s.trim();

        if (CONN_FMT_PATTERN.matcher(s).matches()) {
            String[] split = CONN_SPLIT_PATTERN.split(s, CONN_SPLIT_LIMIT);
            try {
                NodePort entryPoint = NodePort.parse(split[0], idAliaser);
                NodePort exitPoint = NodePort.parse(split[1], idAliaser);

                return DatapathConnection.of(entryPoint, exitPoint);
            }
            catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                    String.format("error while parsing connection: %s", e.getMessage()),
                    e);
            }
        }
        else {
            throw new IllegalArgumentException(
                String.format(
                    "malformed connection string (must have the form '%s')",
                    CONN_FMT_DESCRIPT));
        }
    }

    public static FlowedLink parseFlowedLink( String s, Function<DatapathId, String> idAliaser )
        throws IllegalArgumentException
    {
        // be nice to the user and accept leading or trailing whitespace
        s = s.trim();

        if (FLOWED_LINK_FMT_PATTERN.matcher(s).matches()) {
            String[] split = FLOWED_LINK_SPLIT_PATTERN.split(s, FLOWED_LINK_SPLIT_LIMIT);
            try {
                DatapathLink link = parseLink(split[0], idAliaser);
                Flow flow = Flow.parse(split[1]);

                return FlowedLink.of(link, flow);
            }
            catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                    String.format("error while parsing flowed-link: %s", e.getMessage()),
                    e);
            }
        }
        else {
            throw new IllegalArgumentException(
                String.format(
                    "malformed flowed-link string (must have the form '%s')",
                    FLOWED_LINK_FMT_DESCRIPT));
        }
    }

    public static FlowedConnection parseFlowedConnection( String s, Function<DatapathId, String> idAliaser )
        throws IllegalArgumentException
    {
        // be nice to the user and accept leading or trailing whitespace
        s = s.trim();

        if (FLOWED_CONN_FMT_PATTERN.matcher(s).matches()) {
            String[] split = FLOWED_CONN_SPLIT_PATTERN.split(s, FLOWED_CONN_SPLIT_LIMIT);
            try {
                DatapathConnection conn = parseConnection(split[0], idAliaser);
                Flow flow = Flow.parse(split[1]);

                return FlowedConnection.of(conn, flow);
            }
            catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                    String.format("error while parsing flowed-connection: %s", e.getMessage()),
                    e);
            }
        }
        else {
            throw new IllegalArgumentException(
                String.format(
                    "malformed flowed-connection string (must have the form '%s')",
                    FLOWED_CONN_FMT_DESCRIPT));
        }
    }

    public static DatapathLink getLinkFromUpdate( LDUpdate update )
    {
        return getLinkFromUpdate(update, NodeId.NIL_ID_ALIASER);
    }

    public static DatapathLink getLinkFromUpdate( LDUpdate update, Function<DatapathId, String> idAliaser )
    {
        return DatapathLink.of(
            NodeId.of(update.getSrc(), idAliaser),
            PortId.of(update.getSrcPort()),
            NodeId.of(update.getDst(), idAliaser),
            PortId.of(update.getDstPort()));
    }

    public static Link getMutableLinkFromUpdate( LDUpdate update )
    {
        return new Link(
            update.getSrc(),
            update.getSrcPort(),
            update.getDst(),
            update.getDstPort(),
            update.getLatency());
    }

    public static TimeDouble getLLDPLatency( LinkInfo linkInfo )
    {
        U64 lat = linkInfo.getCurrentLatency();
        return (lat != null) ? TimeLong.of(lat.getValue(), TimeUnit.MILLISECONDS).asDouble()
                             : TimeDouble.absent();
    }

    public static boolean hasSrcDirection( Directed<?> directed )
    {
        return directed.getDirection().equals(EndpointKind.SOURCE.getTrafficDirection());
    }

    public static boolean hasDestDirection( Directed<?> directed )
    {
        return directed.getDirection().equals(EndpointKind.DESTINATION.getTrafficDirection());
    }

    public static TrafficDirection getSrcDirection()
    {
        return EndpointKind.SOURCE.getTrafficDirection();
    }

    public static TrafficDirection getDestDirection()
    {
        return EndpointKind.DESTINATION.getTrafficDirection();
    }

    private LinkUtils()
    {
        // not used
    }
}
