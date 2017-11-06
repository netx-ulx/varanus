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
@XmlRootElement( name = "topology_error" )
@XmlAccessorType( XmlAccessType.FIELD )
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class XMLTopologyError
{
    @XmlElement( name = "message", required = true )
    private String message;

    public String getMessage()
    {
        return Objects.requireNonNull(message);
    }

    public void setMessage( String message )
    {
        this.message = Objects.requireNonNull(message);
    }
}
