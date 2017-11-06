package net.varanus.xmlproxy.xml;


import java.util.Objects;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.xmlproxy.xml.types.LinkDirection;


/**
 * 
 */
@XmlRootElement( name = "stat_request" )
@XmlAccessorType( XmlAccessType.FIELD )
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class XMLStatRequest
{
    @XmlElement( name = "link", required = true )
    private String link;

    @XmlElement( name = "direction", required = true )
    private LinkDirection direction;

    @XmlElement( name = "match", required = true )
    private String match;

    public String getLink()
    {
        return Objects.requireNonNull(link);
    }

    public void setLink( String link )
    {
        this.link = Objects.requireNonNull(link);
    }

    public LinkDirection getDirection()
    {
        return Objects.requireNonNull(direction);
    }

    public void setDirection( LinkDirection direction )
    {
        this.direction = Objects.requireNonNull(direction);
    }

    public String getMatch()
    {
        return Objects.requireNonNull(match);
    }

    public void setMatch( String match )
    {
        this.match = Objects.requireNonNull(match);
    }
}
