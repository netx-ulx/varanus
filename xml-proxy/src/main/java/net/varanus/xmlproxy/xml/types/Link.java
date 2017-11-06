package net.varanus.xmlproxy.xml.types;


import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 */
@XmlType( name = "link" )
@XmlAccessorType( XmlAccessType.FIELD )
public final class Link
{
    @XmlAttribute( name = "id", required = true )
    private String id;

    @XmlElement( name = "from", required = true )
    private String from;

    @XmlElement( name = "to", required = true )
    private String to;

    @XmlElement( name = "enabled", required = true )
    private Boolean enabled;

    @XmlElement( name = "config_forward", required = true )
    private LinkConfig configForward;

    @XmlElement( name = "config_reverse", required = true )
    private LinkConfig configReverse;

    public String getId()
    {
        return Objects.requireNonNull(id);
    }

    public void setId( String id )
    {
        this.id = Objects.requireNonNull(id);
    }

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

    public boolean getEnabled()
    {
        return Objects.requireNonNull(enabled);
    }

    public void setEnabled( boolean enabled )
    {
        this.enabled = enabled;
    }

    public LinkConfig getConfigForward()
    {
        return Objects.requireNonNull(configForward);
    }

    public void setConfigForward( LinkConfig configForward )
    {
        this.configForward = Objects.requireNonNull(configForward);
    }

    public LinkConfig getConfigReverse()
    {
        return Objects.requireNonNull(configReverse);
    }

    public void setConfigReverse( LinkConfig configReverse )
    {
        this.configReverse = Objects.requireNonNull(configReverse);
    }
}
