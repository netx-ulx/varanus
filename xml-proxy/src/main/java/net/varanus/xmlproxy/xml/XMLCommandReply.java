package net.varanus.xmlproxy.xml;


import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalLong;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.functional.OptionalUtils;
import net.varanus.xmlproxy.xml.types.CommandType;


/**
 * 
 */
@XmlRootElement( name = "command_reply" )
@XmlAccessorType( XmlAccessType.FIELD )
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class XMLCommandReply
{
    @XmlElement( name = "type", required = true )
    private CommandType type;

    @XmlElement( name = "from", required = true )
    private String from;

    @XmlElement( name = "to", required = true )
    private String to;

    @XmlElement( name = "enabled", required = true )
    private Boolean enabled;

    @XmlElement( name = "latency", required = false )
    private Double latency = null; /* in ms */

    @XmlElement( name = "throughput", required = false )
    private Long throughput = null; /* in bit/s */

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

    public boolean getEnabled()
    {
        return Objects.requireNonNull(enabled);
    }

    public void setEnabled( boolean enabled )
    {
        this.enabled = enabled;
    }

    public OptionalDouble getLatency()
    {
        return OptionalUtils.ofNullable(latency);
    }

    public void setLatency( double latency )
    {
        this.latency = latency;
    }

    public OptionalLong getThroughput()
    {
        return OptionalUtils.ofNullable(throughput);
    }

    public void setThroughput( long throughput )
    {
        this.throughput = throughput;
    }
}
