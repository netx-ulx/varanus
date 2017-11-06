package net.varanus.infoprotocol;


import static net.varanus.infoprotocol.ReplyType.PACKET_REPLY;
import static net.varanus.infoprotocol.ReplyType.ROUTE_REPLY;
import static net.varanus.infoprotocol.ReplyType.STATISTICS_REPLY;
import static net.varanus.infoprotocol.ReplyType.TOPOLOGY_REPLY;

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
public final class GenericReply extends Generic<ReplyType, TopologyReply, StatisticsReply, RouteReply, PacketReply>
{
    public static GenericReply fromTopology( TopologyReply reply )
    {
        return new GenericReply(Objects.requireNonNull(reply));
    }

    public static GenericReply fromStatistics( StatisticsReply reply )
    {
        return new GenericReply(Objects.requireNonNull(reply));
    }

    public static GenericReply fromRoute( RouteReply reply )
    {
        return new GenericReply(Objects.requireNonNull(reply));
    }

    public static GenericReply fromPacket( PacketReply reply )
    {
        return new GenericReply(Objects.requireNonNull(reply));
    }

    private GenericReply( TopologyReply reply )
    {
        super(TOPOLOGY_REPLY, reply);
    }

    private GenericReply( StatisticsReply reply )
    {
        super(STATISTICS_REPLY, reply);
    }

    private GenericReply( RouteReply reply )
    {
        super(ROUTE_REPLY, reply);
    }

    private GenericReply( PacketReply reply )
    {
        super(PACKET_REPLY, reply);
    }

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class IO
    {
        public static IOWriter<GenericReply> writer( Logger log )
        {
            return Generic.IO.writer(
                ReplyType.IO.writer(),
                TopologyReply.IO.writer(log),
                StatisticsReply.IO.writer(log),
                RouteReply.IO.writer(log),
                PacketReply.IO.writer(log));
        }

        public static IOReader<GenericReply> reader( Function<DatapathId, String> idAliaser, Logger log )
        {
            return Generic.IO.reader(
                ReplyType.IO.reader(),
                TopologyReply.IO.reader(idAliaser, log),
                GenericReply::fromTopology,
                StatisticsReply.IO.reader(idAliaser, log),
                GenericReply::fromStatistics,
                RouteReply.IO.reader(idAliaser, log),
                GenericReply::fromRoute,
                PacketReply.IO.reader(log),
                GenericReply::fromPacket);
        }
    }
}
