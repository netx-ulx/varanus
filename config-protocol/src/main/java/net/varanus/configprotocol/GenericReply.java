package net.varanus.configprotocol;


import static net.varanus.configprotocol.ReplyType.LINK_BANDWIDTH_REPLY;
import static net.varanus.configprotocol.ReplyType.LINK_ENABLING_REPLY;

import java.util.Objects;
import java.util.function.Function;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import org.projectfloodlight.openflow.types.DatapathId;
import org.slf4j.Logger;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.io.serializer.IOReader;
import net.varanus.util.io.serializer.IOWriter;


/**
 * 
 */
@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class GenericReply extends Generic<ReplyType, LinkEnablingReply, LinkBandwidthReply>
{
    public static GenericReply fromLinkEnabling( LinkEnablingReply reply )
    {
        return new GenericReply(Objects.requireNonNull(reply));
    }

    public static GenericReply fromLinkBandwidth( LinkBandwidthReply reply )
    {
        return new GenericReply(Objects.requireNonNull(reply));
    }

    private GenericReply( LinkEnablingReply reply )
    {
        super(LINK_ENABLING_REPLY, reply);
    }

    private GenericReply( LinkBandwidthReply reply )
    {
        super(LINK_BANDWIDTH_REPLY, reply);
    }

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class IO
    {
        public static IOWriter<GenericReply> writer( Logger log )
        {
            return Generic.IO.writer(
                ReplyType.IO.writer(),
                LinkEnablingReply.IO.writer(log),
                LinkBandwidthReply.IO.writer(log));
        }

        public static IOReader<GenericReply> reader( Function<DatapathId, String> idAliaser, Logger log )
        {
            return Generic.IO.reader(
                ReplyType.IO.reader(),
                LinkEnablingReply.IO.reader(idAliaser, log),
                GenericReply::fromLinkEnabling,
                LinkBandwidthReply.IO.reader(idAliaser, log),
                GenericReply::fromLinkBandwidth);
        }
    }
}
