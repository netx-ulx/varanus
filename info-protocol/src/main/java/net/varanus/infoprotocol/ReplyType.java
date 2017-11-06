package net.varanus.infoprotocol;


import static net.varanus.infoprotocol.RequestType.PACKET_REQUEST;
import static net.varanus.infoprotocol.RequestType.ROUTE_REQUEST;
import static net.varanus.infoprotocol.RequestType.STATISTICS_REQUEST;
import static net.varanus.infoprotocol.RequestType.TOPOLOGY_REQUEST;

import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.io.Serializers;
import net.varanus.util.io.serializer.IOReader;
import net.varanus.util.io.serializer.IOWriter;


/**
 * 
 */
@ReturnValuesAreNonnullByDefault
public enum ReplyType implements Generic.GenericTyped
{
    TOPOLOGY_REPLY
    {
        @Override
        public RequestType request()
        {
            return TOPOLOGY_REQUEST;
        }

        @Override
        public Generic.GenericType type()
        {
            return Generic.GenericType.TOPOLOGY;
        }
    },

    STATISTICS_REPLY
    {
        @Override
        public RequestType request()
        {
            return STATISTICS_REQUEST;
        }

        @Override
        public Generic.GenericType type()
        {
            return Generic.GenericType.STATISTICS;
        }
    },

    ROUTE_REPLY
    {
        @Override
        public RequestType request()
        {
            return ROUTE_REQUEST;
        }

        @Override
        public Generic.GenericType type()
        {
            return Generic.GenericType.ROUTE;
        }
    },

    PACKET_REPLY
    {
        @Override
        public RequestType request()
        {
            return PACKET_REQUEST;
        }

        @Override
        public Generic.GenericType type()
        {
            return Generic.GenericType.PACKET;
        }
    };

    public abstract RequestType request();

    public static final class IO
    {
        public static IOWriter<ReplyType> writer()
        {
            return Serializers.enumWriter();
        }

        public static IOReader<ReplyType> reader()
        {
            return Serializers.enumReader(ReplyType.class);
        }

        private IO()
        {
            // not used
        }
    }
}
