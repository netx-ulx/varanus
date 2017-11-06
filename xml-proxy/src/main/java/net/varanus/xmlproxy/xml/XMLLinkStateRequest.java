package net.varanus.xmlproxy.xml;


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
@XmlRootElement( name = "link_state_request" )
@XmlAccessorType( XmlAccessType.FIELD )
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class XMLLinkStateRequest
{
    @XmlElement( name = "link", required = true )
    private String link;

    @XmlElement( name = "enabled", required = true )
    private Boolean enabled;

    public String getLink()
    {
        return Objects.requireNonNull(link);
    }

    public void setLink( String link )
    {
        this.link = Objects.requireNonNull(link);
    }

    public boolean getEnabled()
    {
        return Objects.requireNonNull(enabled);
    }

    public void setEnabled( boolean enabled )
    {
        this.enabled = enabled;
    }
}
