package net.varanus.xmlproxy.xml;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.xmlproxy.xml.types.Link;
import net.varanus.xmlproxy.xml.types.Node;


/**
 * 
 */
@XmlRootElement( name = "topology_reply" )
@XmlAccessorType( XmlAccessType.FIELD )
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class XMLTopologyReply
{
    @XmlElement( name = "node", required = false )
    private List<Node> nodes = new ArrayList<>();

    @XmlElement( name = "link", required = false )
    private List<Link> links = new ArrayList<>();

    public List<Node> getNodes()
    {
        return nodes;
    }

    public void setNodes( List<Node> nodes )
    {
        this.nodes = Objects.requireNonNull(nodes);
    }

    public List<Link> getLinks()
    {
        return links;
    }

    public void setLinks( List<Link> links )
    {
        this.links = Objects.requireNonNull(links);
    }
}
