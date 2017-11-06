package net.varanus.infoprotocol;


import java.nio.channels.ByteChannel;
import java.util.function.Function;

import javax.annotation.ParametersAreNonnullByDefault;

import org.projectfloodlight.openflow.types.DatapathId;
import org.slf4j.Logger;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.io.DebugUtils;
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
public final class ServerCommunicator
{
    public static ServerCommunicator create( ByteChannel channel, Function<DatapathId, String> idAliaser, Logger log )
    {
        return new ServerCommunicator(channel, idAliaser, log);
    }

    private final ByteChannel                   channel;
    private final ChannelReader<GenericRequest> requestReader;
    private final ChannelWriter<GenericReply>   replyWriter;

    private ServerCommunicator( ByteChannel channel, Function<DatapathId, String> idAliaser, Logger log )
    {
        this.channel = DebugUtils.debuggedChannel(channel, log);
        this.requestReader = GenericRequest.IO.reader(idAliaser, log);
        this.replyWriter = GenericReply.IO.writer(log);
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
