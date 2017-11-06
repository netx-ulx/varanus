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
@XmlRootElement( name = "link_state_error" )
@XmlAccessorType( XmlAccessType.FIELD )
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class XMLLinkStateError
{
    @XmlElement( name = "link", required = true )
    private String link;

    @XmlElement( name = "message", required = true )
    private String message;

    public String getLink()
    {
        return Objects.requireNonNull(link);
    }

    public void setLink( String link )
    {
        this.link = Objects.requireNonNull(link);
    }

    public String getMessage()
    {
        return Objects.requireNonNull(message);
    }

    public void setMessage( String message )
    {
        this.message = Objects.requireNonNull(message);
    }
}
