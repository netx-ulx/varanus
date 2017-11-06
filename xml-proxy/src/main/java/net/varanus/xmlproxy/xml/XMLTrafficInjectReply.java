package net.varanus.xmlproxy.xml;


import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.functional.OptionalUtils;
import net.varanus.xmlproxy.xml.types.LinkDirection;


/**
 * 
 */
@XmlRootElement( name = "traffic_inject_reply" )
@XmlAccessorType( XmlAccessType.FIELD )
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class XMLTrafficInjectReply
{
    @XmlElement( name = "link", required = true )
    private String link;

    @XmlElement( name = "direction", required = true )
    private LinkDirection direction;

    @XmlElement( name = "enabled", required = true )
    private Boolean enabled;

    @XmlElement( name = "match", required = false )
    private String match = null;

    @XmlElement( name = "bandwidth", required = false )
    private Double bandwidth = null; /* in Mbit/s */

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

    public boolean getEnabled()
    {
        return Objects.requireNonNull(enabled);
    }

    public void setEnabled( boolean enabled )
    {
        this.enabled = enabled;
    }

    public Optional<String> getMatch()
    {
        return Optional.ofNullable(match);
    }

    public void setMatch( String match )
    {
        this.match = Objects.requireNonNull(match);
    }

    public OptionalDouble getBandwidth()
    {
        return OptionalUtils.ofNullable(bandwidth);
    }

    public void setBandwidth( double bandwidth )
    {
        this.bandwidth = bandwidth;
    }
}
