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
public class ClientCommunicator
{
    public static ClientCommunicator create( ByteChannel channel, Function<DatapathId, String> idAliaser, Logger log )
    {
        return new ClientCommunicator(channel, idAliaser, log);
    }

    private final ByteChannel                   channel;
    private final ChannelWriter<GenericRequest> requestWriter;
    private final ChannelReader<GenericReply>   replyReader;

    private ClientCommunicator( ByteChannel channel, Function<DatapathId, String> idAliaser, Logger log )
    {
        this.channel = DebugUtils.debuggedChannel(channel, log);
        this.requestWriter = GenericRequest.IO.writer(log);
        this.replyReader = GenericReply.IO.reader(idAliaser, log);
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
