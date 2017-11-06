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
@XmlRootElement( name = "route_error" )
@XmlAccessorType( XmlAccessType.FIELD )
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class XMLRouteError
{
    @XmlElement( name = "from", required = true )
    private String from;

    @XmlElement( name = "to", required = true )
    private String to;

    @XmlElement( name = "match", required = true )
    private String match;

    @XmlElement( name = "message", required = true )
    private String message;

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

    public String getMessage()
    {
        return Objects.requireNonNull(message);
    }

    public void setMessage( String message )
    {
        this.message = Objects.requireNonNull(message);
    }
}
