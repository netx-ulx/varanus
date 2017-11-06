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


/**
 * 
 */
@XmlRootElement( name = "route_reply" )
@XmlAccessorType( XmlAccessType.FIELD )
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class XMLRouteReply
{
    @XmlElement( name = "from", required = true )
    private String from;

    @XmlElement( name = "to", required = true )
    private String to;

    @XmlElement( name = "match", required = true )
    private String match;

    @XmlElement( name = "link", required = false )
    private List<String> links = new ArrayList<>();

    public String getFrom()
    {
        return Objects.requireNonNull(from);
    }

    public void setFrom( String from )
    {
        this.from = Objects.requireNonNull(from);
    }

    public String getTo()
    {
        return Objects.requireNonNull(to);
    }

    public void setTo( String to )
    {
        this.to = Objects.requireNonNull(to);
    }

    public String getMatch()
    {
        return Objects.requireNonNull(match);
    }

    public void setMatch( String match )
    {
        this.match = Objects.requireNonNull(match);
    }

    public List<String> getLinks()
    {
        return links;
    }

    public void setLinks( List<String> links )
    {
        this.links = Objects.requireNonNull(links);
    }
}
