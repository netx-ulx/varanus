package net.varanus.infoprotocol;


import static net.varanus.infoprotocol.ReplyType.PACKET_REPLY;
import static net.varanus.infoprotocol.ReplyType.ROUTE_REPLY;
import static net.varanus.infoprotocol.ReplyType.STATISTICS_REPLY;
import static net.varanus.infoprotocol.ReplyType.TOPOLOGY_REPLY;

import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.io.Serializers;
import net.varanus.util.io.serializer.IOReader;
import net.varanus.util.io.serializer.IOWriter;


/**
 * 
 */
@ReturnValuesAreNonnullByDefault
public enum RequestType implements Generic.GenericTyped
{
    TOPOLOGY_REQUEST
    {
        @Override
        public ReplyType reply()
        {
            return TOPOLOGY_REPLY;
        }

        @Override
        public Generic.GenericType type()
        {
            return Generic.GenericType.TOPOLOGY;
        }
    },

    STATISTICS_REQUEST
    {
        @Override
        public ReplyType reply()
        {
            return STATISTICS_REPLY;
        }

        @Override
        public Generic.GenericType type()
        {
            return Generic.GenericType.STATISTICS;
        }
    },

    ROUTE_REQUEST
    {
        @Override
        public ReplyType reply()
        {
            return ROUTE_REPLY;
        }

        @Override
        public Generic.GenericType type()
        {
            return Generic.GenericType.ROUTE;
        }
    },

    PACKET_REQUEST
    {
        @Override
        public ReplyType reply()
        {
            return PACKET_REPLY;
        }

        @Override
        public Generic.GenericType type()
        {
            return Generic.GenericType.PACKET;
        }
    };

    public abstract ReplyType reply();

    public static final class IO
    {
        public static IOWriter<RequestType> writer()
        {
            return Serializers.enumWriter();
        }

        public static IOReader<RequestType> reader()
        {
            return Serializers.enumReader(RequestType.class);
        }

        private IO()
        {
            // not used
        }
    }
}
