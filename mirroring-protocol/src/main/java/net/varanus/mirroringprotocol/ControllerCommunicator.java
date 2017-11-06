package net.varanus.mirroringprotocol;


import java.nio.channels.ByteChannel;
import java.util.function.Function;

import javax.annotation.ParametersAreNonnullByDefault;

import org.projectfloodlight.openflow.types.DatapathId;
import org.slf4j.Logger;

import net.varanus.mirroringprotocol.SamplingReply.IO.CompressionStrategy;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.io.DebugUtils;
import net.varanus.util.io.ExtraChannels.TraceableByteChannel;
import net.varanus.util.io.exception.IOChannelReadException;
import net.varanus.util.io.exception.IOChannelWriteException;
import net.varanus.util.io.serializer.ChannelReader;
import net.varanus.util.io.serializer.ChannelWriter;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public class ControllerCommunicator
{
    public static ControllerCommunicator create( ByteChannel channel,
                                                 Function<DatapathId, String> idAliaser,
                                                 CompressionStrategy compStrat,
                                                 Logger log )
        throws IOChannelReadException
    {
        return create(channel, idAliaser, compStrat, log, log, log);
    }

    public static ControllerCommunicator create( ByteChannel channel,
                                                 Function<DatapathId, String> idAliaser,
                                                 CompressionStrategy compStrat,
                                                 Logger chLog,
                                                 Logger sampLog,
                                                 Logger probLog )
        throws IOChannelReadException
    {
        CollectorConnection conn = CollectorConnection.IO.reader(idAliaser, chLog).read(channel);
        return new ControllerCommunicator(conn, channel, idAliaser, compStrat, chLog, sampLog, probLog);
    }

    private final CollectorConnection           conn;
    private final TraceableByteChannel          channel;
    private final ChannelWriter<GenericRequest> requestWriter;
    private final ChannelReader<GenericReply>   replyReader;

    private ControllerCommunicator( CollectorConnection conn,
                                    ByteChannel channel,
                                    Function<DatapathId, String> idAliaser,
                                    CompressionStrategy compStrat,
                                    Logger chLog,
                                    Logger sampLog,
                                    Logger probLog )
    {
        this.conn = conn;
        this.channel = DebugUtils.debuggedChannel(channel, chLog);
        this.requestWriter = GenericRequest.IO.writer(conn.getCollectorId(), sampLog, probLog);
        this.replyReader = GenericReply.IO.reader(conn.getCollectorId(), idAliaser, compStrat, sampLog, probLog);
    }

    public CollectorConnection getConnection()
    {
        return conn;
    }

    public void sendRequest( GenericRequest request ) throws IOChannelWriteException
    {
        requestWriter.write(request, channel);
    }

    public GenericReply receiveReply() throws IOChannelReadException
    {
        return replyReader.read(channel);
    }
}
