package net.varanus.xmlproxy.xml.types;


import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 */
@XmlType( name = "link_config" )
@XmlAccessorType( XmlAccessType.FIELD )
public final class LinkConfig
{
    @XmlElement( name = "delay", required = true )
    private Long delay; /* in ms */

    @XmlElement( name = "loss_rate", required = true )
    private Integer lossRate; /* 0 to 100% */

    @XmlElement( name = "bandwidth", required = true )
    private Double bandwidth; /* in Mbit/s */

    public long getDelay()
    {
        return Objects.requireNonNull(delay);
    }

    public void setDelay( long delay )
    {
        this.delay = delay;
    }

    public int getLossRate()
    {
        return Objects.requireNonNull(lossRate);
    }

    public void setLossRate( int lossRate )
    {
        this.lossRate = lossRate;
    }

    public double getBandwidth()
    {
        return Objects.requireNonNull(bandwidth);
    }

    public void setBandwidth( double bandwidth )
    {
        this.bandwidth = bandwidth;
    }
}
