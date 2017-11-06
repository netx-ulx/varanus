package net.varanus.xmlproxy.xml.types;


import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;


/**
 * 
 */
@XmlType( name = "latency_stat" )
@XmlAccessorType( XmlAccessType.FIELD )
public final class LatencyStat
{
    @XmlAttribute( name = "probe", required = true )
    private Boolean probe;

    @XmlValue
    private Double value; /* in ms */

    public boolean getProbe()
    {
        return Objects.requireNonNull(probe);
    }

    public void setProbe( boolean probe )
    {
        this.probe = probe;
    }

    public double getValue()
    {
        return Objects.requireNonNull(value);
    }

    public void setValue( double value )
    {
        this.value = value;
    }
}
