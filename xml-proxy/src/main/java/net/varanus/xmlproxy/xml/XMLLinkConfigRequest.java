package net.varanus.xmlproxy.xml;


import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

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
@XmlRootElement( name = "link_config_request" )
@XmlAccessorType( XmlAccessType.FIELD )
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class XMLLinkConfigRequest
{
    @XmlElement( name = "link", required = true )
    private String link;

    @XmlElement( name = "direction", required = true )
    private LinkDirection direction;

    @XmlElement( name = "delay", required = false )
    private Long delay = null; /* in ms */

    @XmlElement( name = "loss_rate", required = false )
    private Integer lossRate = null; /* 0 to 100% */

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

    public OptionalLong getDelay()
    {
        return OptionalUtils.ofNullable(delay);
    }

    public void setDelay( long delay )
    {
        this.delay = delay;
    }

    public OptionalInt getLossRate()
    {
        return OptionalUtils.ofNullable(lossRate);
    }

    public void setLossRate( int lossRate )
    {
        this.lossRate = lossRate;
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
