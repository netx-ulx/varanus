package net.varanus.sdncontroller.trafficgenerator;


import java.time.Duration;
import java.util.Collections;

import javax.annotation.ParametersAreNonnullByDefault;

import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.types.OFPort;

import net.floodlightcontroller.packet.IPacket;
import net.varanus.sdncontroller.types.FlowedLink;
import net.varanus.sdncontroller.util.IPacketUtils;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.lang.Unsigned;
import net.varanus.util.time.TimeDoubleUnit;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class TrafficProperties
{
    public static TrafficProperties create( FlowedLink flowedLink, int packetsPerSecond, int batchSize )
    {
        byte[] packetData = generatePacket(flowedLink.getFlow().getMatch());
        Duration xmitInterval = calcXmitInterval(packetsPerSecond);
        checkBatchSize(batchSize);

        return new TrafficProperties(packetData, flowedLink, xmitInterval, batchSize);
    }

    private final byte[]     packetData;
    private final FlowedLink flowedLink;
    private final Duration   xmitInterval;
    private final int        batchSize;

    private TrafficProperties( byte[] packetData,
                               FlowedLink flowedLink,
                               Duration xmitInterval,
                               int batchSize )
    {
        this.packetData = packetData;
        this.flowedLink = flowedLink;
        this.xmitInterval = xmitInterval;
        this.batchSize = batchSize;
    }

    public OFPacketOut buildPacketOut( OFFactory fact )
    {
        return fact.buildPacketOut()
            .setInPort(OFPort.CONTROLLER)
            .setData(packetData)
            .setActions(
                Collections.singletonList(
                    fact.actions().output(OFPort.TABLE, Unsigned.MAX_SHORT)))
            .build();
    }

    public FlowedLink getFlowedLink()
    {
        return flowedLink;
    }

    public Duration getTransmissionInterval()
    {
        return xmitInterval;
    }

    public int getBatchSize()
    {
        return batchSize;
    }

    private static byte[] generatePacket( Match match )
    {
        IPacket headerOnly = IPacketUtils.fromMatch(match);
        int headerSize = headerOnly.serialize().length;

        // TODO make this size configurable
        byte[] payload = new byte[1500 - headerSize];
        IPacket fullPacket = IPacketUtils.fromMatch(match, payload);

        return fullPacket.serialize();
    }

    private static Duration calcXmitInterval( int pps )
    {
        if (pps < 1)
            throw new IllegalArgumentException("packets-per-second must be positive");

        double nanosInOneSecond = TimeDoubleUnit.SECONDS.toNanos(1);
        long intervalNanos = (long)Math.ceil(nanosInOneSecond / pps);
        return Duration.ofNanos(intervalNanos);
    }

    private static void checkBatchSize( int batchSize )
    {
        if (batchSize < 1)
            throw new IllegalArgumentException("batch size must be positive");
    }
}
