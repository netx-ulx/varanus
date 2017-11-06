package net.varanus.infoprotocol;


import static net.varanus.infoprotocol.RequestType.PACKET_REQUEST;
import static net.varanus.infoprotocol.RequestType.ROUTE_REQUEST;
import static net.varanus.infoprotocol.RequestType.STATISTICS_REQUEST;
import static net.varanus.infoprotocol.RequestType.TOPOLOGY_REQUEST;

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
public final class GenericRequest
    extends Generic<RequestType, TopologyRequest, StatisticsRequest, RouteRequest, PacketRequest>
{
    public static GenericRequest fromTopology( TopologyRequest req )
    {
        return new GenericRequest(Objects.requireNonNull(req));
    }

    public static GenericRequest fromStatistics( StatisticsRequest req )
    {
        return new GenericRequest(Objects.requireNonNull(req));
    }

    public static GenericRequest fromRoute( RouteRequest req )
    {
        return new GenericRequest(Objects.requireNonNull(req));
    }

    public static GenericRequest fromPacket( PacketRequest req )
    {
        return new GenericRequest(Objects.requireNonNull(req));
    }

    private GenericRequest( TopologyRequest req )
    {
        super(TOPOLOGY_REQUEST, req);
    }

    private GenericRequest( StatisticsRequest req )
    {
        super(STATISTICS_REQUEST, req);
    }

    private GenericRequest( RouteRequest req )
    {
        super(ROUTE_REQUEST, req);
    }

    private GenericRequest( PacketRequest req )
    {
        super(PACKET_REQUEST, req);
    }

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class IO
    {
        public static IOWriter<GenericRequest> writer( Logger log )
        {
            return Generic.IO.writer(
                RequestType.IO.writer(),
                TopologyRequest.IO.writer(log),
                StatisticsRequest.IO.writer(log),
                RouteRequest.IO.writer(log),
                PacketRequest.IO.writer(log));
        }

        public static IOReader<GenericRequest> reader( Function<DatapathId, String> idAliaser, Logger log )
        {
            return Generic.IO.reader(
                RequestType.IO.reader(),
                TopologyRequest.IO.reader(log),
                GenericRequest::fromTopology,
                StatisticsRequest.IO.reader(idAliaser, log),
                GenericRequest::fromStatistics,
                RouteRequest.IO.reader(idAliaser, log),
                GenericRequest::fromRoute,
                PacketRequest.IO.reader(log),
                GenericRequest::fromPacket);
        }
    }
}
