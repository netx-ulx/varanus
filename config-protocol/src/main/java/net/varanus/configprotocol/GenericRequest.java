package net.varanus.configprotocol;


import static net.varanus.configprotocol.RequestType.LINK_BANDWIDTH_REQUEST;
import static net.varanus.configprotocol.RequestType.LINK_ENABLING_REQUEST;

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
public final class GenericRequest extends Generic<RequestType, LinkEnablingRequest, LinkBandwidthRequest>
{
    public static GenericRequest fromLinkEnabling( LinkEnablingRequest req )
    {
        return new GenericRequest(Objects.requireNonNull(req));
    }

    public static GenericRequest fromLinkBandwidth( LinkBandwidthRequest req )
    {
        return new GenericRequest(Objects.requireNonNull(req));
    }

    private GenericRequest( LinkEnablingRequest req )
    {
        super(LINK_ENABLING_REQUEST, req);
    }

    private GenericRequest( LinkBandwidthRequest req )
    {
        super(LINK_BANDWIDTH_REQUEST, req);
    }

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class IO
    {
        public static IOWriter<GenericRequest> writer( Logger log )
        {
            return Generic.IO.writer(
                RequestType.IO.writer(),
                LinkEnablingRequest.IO.writer(log),
                LinkBandwidthRequest.IO.writer(log));
        }

        public static IOReader<GenericRequest> reader( Function<DatapathId, String> idAliaser, Logger log )
        {
            return Generic.IO.reader(
                RequestType.IO.reader(),
                LinkEnablingRequest.IO.reader(idAliaser, log),
                GenericRequest::fromLinkEnabling,
                LinkBandwidthRequest.IO.reader(idAliaser, log),
                GenericRequest::fromLinkBandwidth);
        }
    }
}
