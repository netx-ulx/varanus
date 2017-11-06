package net.varanus.xmlproxy.xml;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.Objects;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.collect.CommonPair;
import net.varanus.util.collect.Pair;
import net.varanus.util.lang.Comparables;
import net.varanus.util.lang.MoreObjects;
import net.varanus.xmlproxy.xml.types.LatencyStat;
import net.varanus.xmlproxy.xml.types.Link;
import net.varanus.xmlproxy.xml.types.LinkConfig;
import net.varanus.xmlproxy.xml.types.LinkDirection;
import net.varanus.xmlproxy.xml.types.LossRateStat;
import net.varanus.xmlproxy.xml.types.Node;
import net.varanus.xmlproxy.xml.types.NodeType;


/**
 * 
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class Helper
{
    public static JAXBContext initContext() throws JAXBException
    {
        return JAXBContext.newInstance(
            Node.class, NodeType.class,
            Link.class, LinkDirection.class, LinkConfig.class,
            LatencyStat.class, LossRateStat.class,
            XMLTopologyReply.class, XMLTopologyError.class,
            XMLStatRequest.class, XMLStatReply.class, XMLStatError.class,
            XMLRouteRequest.class, XMLRouteReply.class, XMLRouteError.class,
            XMLLinkConfigRequest.class, XMLLinkConfigReply.class, XMLLinkConfigError.class,
            XMLCommandRequest.class, XMLCommandReply.class, XMLCommandError.class,
            XMLLinkStateRequest.class, XMLLinkStateReply.class, XMLLinkStateError.class,
            XMLTrafficInjectRequest.class, XMLTrafficInjectReply.class, XMLTrafficInjectError.class);
    }

    public static String buildSwitchId( String dpid )
    {
        return "s_" + Objects.requireNonNull(dpid);
    }

    public static Node buildSwitch( String dpid, String name, boolean virtual )
    {
        MoreObjects.requireNonNull(dpid, "dpid", name, "name");

        Node sw = new Node();
        sw.setId(buildSwitchId(dpid));
        sw.setType(NodeType.SWITCH);
        sw.setName(name);
        sw.setVirtual(virtual);
        sw.setVisible(true); // switches are always visible
        sw.setTitle(dpid);
        return sw;
    }

    public static String buildHostId( String address )
    {
        return "h_" + Objects.requireNonNull(address);
    }

    public static Node buildHost( String address, String name, boolean virtual, boolean visible )
    {
        MoreObjects.requireNonNull(address, "address", name, "name");

        Node host = new Node();
        host.setId(buildHostId(address));
        host.setType(NodeType.HOST);
        host.setName(name);
        host.setVirtual(virtual);
        host.setVisible(visible);
        host.setTitle(address);
        return host;
    }

    public static String buildLinkId( String nodeId1, String nodeId2 )
    {
        String minId = Comparables.min(nodeId1, nodeId2);
        String maxId = Comparables.max(nodeId1, nodeId2);
        return minId + "-" + maxId;
    }

    public static Link buildLink( String nodeId1,
                                  String nodeId2,
                                  boolean enabled,
                                  LinkConfig config1,
                                  LinkConfig config2 )
    {
        Link link = new Link();
        link.setId(buildLinkId(nodeId1, nodeId2));
        link.setFrom(Comparables.min(nodeId1, nodeId2));
        link.setTo(Comparables.max(nodeId1, nodeId2));
        link.setEnabled(enabled);
        if (Comparables.aLEb(nodeId1, nodeId2)) {
            link.setConfigForward(config1);
            link.setConfigReverse(config2);
        }
        else {
            link.setConfigForward(config2);
            link.setConfigReverse(config1);
        }
        return link;
    }

    public static Pair<CommonPair<String>, LinkType> getLinkMembers( String linkId ) throws IllegalArgumentException
    {
        return getLinkMembers(splitLinkId(linkId));
    }

    public static Pair<CommonPair<String>, LinkType> getLinkMembers( String linkId, LinkDirection direction )
        throws IllegalArgumentException
    {
        return getLinkMembers(splitLinkId(linkId, direction));
    }

    private static String[] splitLinkId( String linkId ) throws IllegalArgumentException
    {
        String[] split = linkId.split("-");
        if (split.length == 2)
            return split;
        else
            throw new IllegalArgumentException(String.format("illegal link id: %s", linkId));
    }

    private static String[] splitLinkId( String linkId, LinkDirection direction ) throws IllegalArgumentException
    {
        String[] split = splitLinkId(linkId);
        switch (direction) {
            case FORWARD:
                return split;

            case REVERSE:
                String tmp = split[0];
                split[0] = split[1];
                split[1] = tmp;
                return split;

            default:
                throw new AssertionError("unexpected enum value");
        }
    }

    private static Pair<CommonPair<String>, LinkType> getLinkMembers( String[] split )
    {
        String nodeId1 = split[0];
        String nodeId2 = split[1];

        if (isSwitchSwitchLink(nodeId1, nodeId2))
            return Pair.of(CommonPair.of(nodeId1, nodeId2), LinkType.SWITCH_SWITCH);
        else if (isSwitchHostLink(nodeId1, nodeId2))
            return Pair.of(CommonPair.of(nodeId1, nodeId2), LinkType.SWITCH_HOST);
        else if (isHostSwitchLink(nodeId1, nodeId2))
            return Pair.of(CommonPair.of(nodeId1, nodeId2), LinkType.HOST_SWITCH);
        else
            throw new IllegalArgumentException(
                "invalid link type (can only be \"switch-switch\", \"switch-host\" or \"host-switch\")");
    }

    private static boolean isSwitchSwitchLink( String nodeId1, String nodeId2 )
    {
        return nodeId1.startsWith("s_") && nodeId2.startsWith("s_");
    }

    private static boolean isSwitchHostLink( String nodeId1, String nodeId2 )
    {
        return nodeId1.startsWith("s_") && nodeId2.startsWith("h_");
    }

    private static boolean isHostSwitchLink( String nodeId1, String nodeId2 )
    {
        return nodeId1.startsWith("h_") && nodeId2.startsWith("s_");
    }

    public static String getSwitchDpid( String swId ) throws IllegalArgumentException
    {
        if (swId.startsWith("s_") && swId.length() > 2)
            return swId.substring(2);
        else
            throw new IllegalArgumentException(String.format("illegal switch id: %s", swId));
    }

    public static String getHostAddress( String hostId ) throws IllegalArgumentException
    {
        if (hostId.startsWith("h_") && hostId.length() > 2)
            return hostId.substring(2);
        else
            throw new IllegalArgumentException(String.format("illegal host id: %s", hostId));
    }

    public static String adaptMatch( String match )
    {
        return (match.startsWith("[") && match.endsWith("]")) ? match : String.format("[%s]", match);
    }

    public static byte[] toBytes( Object elem, JAXBContext jc ) throws JAXBException
    {
        MoreObjects.requireNonNull(elem, "elem", jc, "jc");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Marshaller jcm = jc.createMarshaller();
        jcm.marshal(elem, baos);
        return baos.toByteArray();
    }

    public static <T> T fromBytes( byte[] bytes, Class<T> elemClass, JAXBContext jc ) throws JAXBException
    {
        MoreObjects.requireNonNull(bytes, "bytes", elemClass, "elemClass", jc, "jc");

        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        Unmarshaller jcu = jc.createUnmarshaller();
        return jcu.unmarshal(new StreamSource(bais), elemClass).getValue();
    }

    public static String toString( Object elem, JAXBContext jc ) throws JAXBException
    {
        return toString(elem, jc, true);
    }

    public static String toString( Object elem, JAXBContext jc, boolean formatted ) throws JAXBException
    {
        MoreObjects.requireNonNull(elem, "elem", jc, "jc");

        StringWriter sw = new StringWriter();
        Marshaller jcm = jc.createMarshaller();
        jcm.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, formatted);
        jcm.marshal(elem, sw);
        return sw.toString();
    }

    public static void print( Object elem, OutputStream out, JAXBContext jc ) throws JAXBException
    {
        print(elem, out, jc, true);
    }

    public static void print( Object elem, OutputStream out, JAXBContext jc, boolean formatted ) throws JAXBException
    {
        MoreObjects.requireNonNull(elem, "elem", out, "out", jc, "jc");

        Marshaller jcm = jc.createMarshaller();
        jcm.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, formatted);
        jcm.marshal(elem, out);
    }

    public static String getSchema( JAXBContext jc )
    {
        try {
            StringWriter sw = new StringWriter();
            jc.generateSchema(new SchemaOutputResolver() {
                @Override
                public Result createOutput( String _namespaceUri, String _suggestedFileName )
                {
                    StreamResult res = new StreamResult(sw);
                    res.setSystemId("");
                    return res;
                }
            });

            return sw.toString();
        }
        catch (IOException e) {
            // should never happen
            throw new UncheckedIOException(e);
        }
    }

    public static enum LinkType
    {
        SWITCH_SWITCH,
        SWITCH_HOST,
        HOST_SWITCH;
    }

    private Helper()
    {
        // not used
    }
}
