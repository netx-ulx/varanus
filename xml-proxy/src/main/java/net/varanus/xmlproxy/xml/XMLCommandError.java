package net.varanus.xmlproxy.xml;


import java.util.Objects;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.xmlproxy.xml.types.CommandType;


/**
 * 
 */
@XmlRootElement( name = "command_error" )
@XmlAccessorType( XmlAccessType.FIELD )
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class XMLCommandError
{
    @XmlElement( name = "type", required = true )
    private CommandType type;

    @XmlElement( name = "from", required = true )
    private String from;

    @XmlElement( name = "to", required = true )
    private String to;

    @XmlElement( name = "message", required = true )
    private String message;

    public CommandType getType()
    {
        return Objects.requireNonNull(type);
    }

    public void setType( CommandType type )
    {
        this.type = Objects.requireNonNull(type);
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

    public String getMessage()
    {
        return Objects.requireNonNull(message);
    }

    public void setMessage( String message )
    {
        this.message = Objects.requireNonNull(message);
    }
}
