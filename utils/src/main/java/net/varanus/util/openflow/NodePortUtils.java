package net.varanus.util.openflow;


import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import javax.annotation.ParametersAreNonnullByDefault;

import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.util.HexString;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.collect.CollectionUtils;
import net.varanus.util.json.JSONUtils;
import net.varanus.util.openflow.types.DirectedPortId;
import net.varanus.util.openflow.types.NodeId;
import net.varanus.util.openflow.types.NodePort;
import net.varanus.util.openflow.types.PortId;
import net.varanus.util.openflow.types.TrafficDirection;
import net.varanus.util.text.StringUtils;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class NodePortUtils
{
    private static final String  DIR_PORT_FMT_DESCRIPT;
    private static final Pattern DIR_PORT_FMT_PATTERN;
    private static final int     DIR_PORT_SPLIT_LIMIT;
    private static final Pattern DIR_PORT_SPLIT_PATTERN;

    public static final String   NODE_PORT_FMT_DESCRIPT;
    public static final Pattern  NODE_PORT_FMT_PATTERN;
    private static final int     NODE_PORT_SPLIT_LIMIT;
    private static final Pattern NODE_PORT_SPLIT_PATTERN;

    static {//@formatter:off
        // ================================================================================================
        // (Ingress|Egress)_PORT
        // ================================================================================================
        DIR_PORT_FMT_DESCRIPT  = "(Ingress|Egress)_PORT";
        DIR_PORT_FMT_PATTERN   = Pattern.compile(
                                 "(Ingress|Egress)_" + // either the word "Ingress" or "Egress", followed by a '_'
                                 "[0-9]+",             // a non-negative integer
                                 Pattern.CASE_INSENSITIVE);

        DIR_PORT_SPLIT_LIMIT   = 2;
        DIR_PORT_SPLIT_PATTERN = Pattern.compile(
                                 "_");                 // the character '_'


        // ================================================================================================
        // DPID[PORT]
        // ================================================================================================
        NODE_PORT_FMT_DESCRIPT  = "DPID[OF_PORT]";
        NODE_PORT_FMT_PATTERN   = Pattern.compile(
                                  "[^\\[\\]]+" + // one or more characters not '[' or ']'
                                  "\\["        + // the character '['
                                  "[^\\[\\]]+" + // one or more characters not '[' or ']'
                                  "\\]");        // the character ']'

        NODE_PORT_SPLIT_LIMIT   = 3;
        NODE_PORT_SPLIT_PATTERN = Pattern.compile(
                                  "[\\[\\]]");   // the characters '[' and ']'
    }//@formatter:on

    public static DatapathId parseDatapathId( String s ) throws IllegalArgumentException
    {
        // be nice to the user and accept leading or trailing whitespace
        s = s.trim();

        try {
            if (s.indexOf(':') != -1) { // if containsChar(':')
                // may throw NumberFormatException
                return DatapathId.of(HexString.toLong(s));
            }
            else {
                return DatapathId.of(StringUtils.parseUnsignedLong(s));
            }
        }
        catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                String.format("error while parsing datapath_id: %s", e.getMessage()),
                e);
        }
    }

    public static OFPort parseOFPort( String s ) throws IllegalArgumentException
    {
        // be nice to the user and accept leading or trailing whitespace
        s = s.trim();

        try {
            return OFValueTypeUtils.parseValue(s, OFPort.class);
        }
        catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                String.format("error while parsing OF port: %s", e.getMessage()),
                e);
        }
    }

    public static DirectedPortId parseDirectedPortId( String s ) throws IllegalArgumentException
    {
        // be nice to the user and accept leading or trailing whitespace
        s = s.trim();

        if (DIR_PORT_FMT_PATTERN.matcher(s).matches()) {
            String[] split = DIR_PORT_SPLIT_PATTERN.split(s, DIR_PORT_SPLIT_LIMIT);
            try {
                TrafficDirection direction = parseDirection(split[0]);
                PortId portId = PortId.parse(split[1]);
                return DirectedPortId.of(portId, direction);
            }
            catch (IllegalArgumentException e) {
                // should never happen
                throw new IllegalArgumentException(
                    String.format("error while parsing directed port id: %s", e.getMessage()),
                    e);
            }
        }
        else {
            throw new IllegalArgumentException(
                String.format(
                    "malformed directed port id string (must have the form '%s')",
                    DIR_PORT_FMT_DESCRIPT));
        }
    }

    private static TrafficDirection parseDirection( String s ) throws IllegalArgumentException
    {
        switch (s.toLowerCase()) {
            case "ingress":
                return TrafficDirection.INGRESS;

            case "egress":
                return TrafficDirection.EGRESS;

            default:
                throw new IllegalArgumentException(
                    String.format("invalid traffic direction: %s", s));
        }
    }

    public static NodePort parseNodePort( String s, Function<DatapathId, String> idAliaser )
        throws IllegalArgumentException
    {
        // be nice to the user and accept leading or trailing whitespace
        s = s.trim();

        if (NODE_PORT_FMT_PATTERN.matcher(s).matches()) {
            String[] split = NODE_PORT_SPLIT_PATTERN.split(s, NODE_PORT_SPLIT_LIMIT);
            try {
                NodeId nodeId = NodeId.parse(split[0], idAliaser);
                PortId portId = PortId.parse(split[1]);

                return NodePort.of(nodeId, portId);
            }
            catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                    String.format("error while parsing node-port: %s", e.getMessage()),
                    e);
            }
        }
        else {
            throw new IllegalArgumentException(
                String.format(
                    "malformed node-port string (must have the form '%s')",
                    NODE_PORT_FMT_DESCRIPT));
        }
    }

    public static Map<DatapathId, String> parseAliasMap( String s ) throws IllegalArgumentException
    {
        // be nice to the user and accept leading or trailing whitespace
        s = s.trim();

        return JSONUtils.parseMap(s, NodePortUtils::parseDatapathId, Function.identity());
    }

    public static <M extends Map<DatapathId, String>> M parseAliasMap( String s, Supplier<M> mapFactory )
        throws IllegalArgumentException
    {
        return CollectionUtils.toMap(parseAliasMap(s), Function.identity(), mapFactory);
    }

    public static boolean isSpecialPort( OFPort port )
    {
        return (port.equals(OFPort.ALL)
                || port.equals(OFPort.ANY)
                || port.equals(OFPort.CONTROLLER)
                || port.equals(OFPort.IN_PORT)
                || port.equals(OFPort.FLOOD)
                || port.equals(OFPort.LOCAL)
                || port.equals(OFPort.NORMAL)
                || port.equals(OFPort.TABLE));
    }

    private NodePortUtils()
    {
        // not used
    }
}
