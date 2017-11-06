package net.varanus.mirroringprotocol;


import java.nio.channels.ByteChannel;
import java.util.function.Function;

import javax.annotation.ParametersAreNonnullByDefault;

import org.projectfloodlight.openflow.types.DatapathId;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableSet;

import net.varanus.mirroringprotocol.SamplingReply.IO.CompressionStrategy;
import net.varanus.mirroringprotocol.util.CollectorId;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.io.DebugUtils;
import net.varanus.util.io.ExtraChannels.TraceableByteChannel;
import net.varanus.util.io.exception.IOChannelReadException;
import net.varanus.util.io.exception.IOChannelWriteException;
import net.varanus.util.io.serializer.ChannelReader;
import net.varanus.util.io.serializer.ChannelWriter;
import net.varanus.util.openflow.types.NodeId;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class CollectorCommunicator
{
    public static CollectorCommunicator create( CollectorId collectorId,
                                                ImmutableSet<NodeId> suppSwitches,
                                                ByteChannel channel,
                                                Function<DatapathId, String> idAliaser,
                                                CompressionStrategy compStrat,
                                                Logger log )
        throws IOChannelWriteException
    {
        return create(collectorId, suppSwitches, channel, idAliaser, compStrat, log, log, log);
    }

    public static CollectorCommunicator create( CollectorId collectorId,
                                                ImmutableSet<NodeId> suppSwitches,
                                                ByteChannel channel,
                                                Function<DatapathId, String> idAliaser,
                                                CompressionStrategy compStrat,
                                                Logger chLog,
                                                Logger sampLog,
                                                Logger probLog )
        throws IOChannelWriteException
    {
        CollectorConnection conn = new CollectorConnection(collectorId, suppSwitches);
        CollectorConnection.IO.writer(chLog).write(conn, channel);
        return new CollectorCommunicator(conn, channel, idAliaser, compStrat, chLog, sampLog, probLog);
    }

    private final CollectorConnection           conn;
    private final TraceableByteChannel          channel;
    private final ChannelReader<GenericRequest> requestReader;
    private final ChannelWriter<GenericReply>   replyWriter;

    private CollectorCommunicator( CollectorConnection conn,
                                   ByteChannel channel,
                                   Function<DatapathId, String> idAliaser,
                                   CompressionStrategy compStrat,
                                   Logger chLog,
                                   Logger sampLog,
                                   Logger probLog )
    {
        this.conn = conn;
        this.channel = DebugUtils.debuggedChannel(channel, chLog);
        this.requestReader = GenericRequest.IO.reader(conn.getCollectorId(), idAliaser, sampLog, probLog);
        this.replyWriter = GenericReply.IO.writer(conn.getCollectorId(), compStrat, sampLog, probLog);
    }

    public CollectorConnection getConnection()
    {
        return conn;
    }

    public GenericRequest receiveRequest() throws IOChannelReadException
    {
        return requestReader.read(channel);
    }

    public void sendReply( GenericReply reply ) throws IOChannelWriteException
    {
        replyWriter.write(reply, channel);
    }
}
