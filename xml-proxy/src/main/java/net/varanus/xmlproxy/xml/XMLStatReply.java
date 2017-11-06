package net.varanus.xmlproxy.xml;


import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.functional.OptionalUtils;
import net.varanus.xmlproxy.xml.types.LatencyStat;
import net.varanus.xmlproxy.xml.types.LinkDirection;
import net.varanus.xmlproxy.xml.types.LossRateStat;


/**
 * 
 */
@XmlRootElement( name = "stat_reply" )
@XmlAccessorType( XmlAccessType.FIELD )
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class XMLStatReply
{
    @XmlElement( name = "link", required = true )
    private String link;

    @XmlElement( name = "direction", required = true )
    private LinkDirection direction;

    @XmlElement( name = "match", required = true )
    private String match;

    @XmlElement( name = "latency", required = false )
    private LatencyStat latency = null;

    @XmlElement( name = "loss_rate", required = false )
    private LossRateStat lossRate = null;

    @XmlElement( name = "throughput", required = false )
    private Long throughput = null; /* in bits/s */

    @XmlElement( name = "total_throughput", required = false )
    private Long totalThroughput = null; /* in bits/s */

    @XmlElement( name = "dropped_packets", required = false )
    private Long droppedPackets = null; /* in pps */

    @XmlElement( name = "timestamp", required = true )
    private Long timestamp; /* unix time in ms */

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

    public String getMatch()
    {
        return Objects.requireNonNull(match);
    }

    public void setMatch( String match )
    {
        this.match = Objects.requireNonNull(match);
    }

    public Optional<LatencyStat> getLatency()
    {
        return Optional.ofNullable(latency);
    }

    public void setLatency( LatencyStat latency )
    {
        this.latency = Objects.requireNonNull(latency);
    }

    public Optional<LossRateStat> getLossRate()
    {
        return Optional.ofNullable(lossRate);
    }

    public void setLossRate( LossRateStat lossRate )
    {
        this.lossRate = Objects.requireNonNull(lossRate);
    }

    public OptionalLong getThroughput()
    {
        return OptionalUtils.ofNullable(throughput);
    }

    public void setThroughput( long throughput )
    {
        this.throughput = throughput;
    }

    public OptionalLong getTotalThroughput()
    {
        return OptionalUtils.ofNullable(totalThroughput);
    }

    public void setTotalThroughput( long totalThroughput )
    {
        this.totalThroughput = totalThroughput;
    }

    public OptionalLong getDroppedPackets()
    {
        return OptionalUtils.ofNullable(droppedPackets);
    }

    public void setDroppedPackets( long droppedPackets )
    {
        this.droppedPackets = droppedPackets;
    }

    public long getTimestamp()
    {
        return Objects.requireNonNull(timestamp);
    }

    public void setTimestamp( long timestamp )
    {
        this.timestamp = timestamp;
    }
}
