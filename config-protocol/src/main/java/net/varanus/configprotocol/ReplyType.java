package net.varanus.configprotocol;


import static net.varanus.configprotocol.RequestType.LINK_BANDWIDTH_REQUEST;
import static net.varanus.configprotocol.RequestType.LINK_ENABLING_REQUEST;

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
    LINK_ENABLING_REPLY
    {
        @Override
        public RequestType request()
        {
            return LINK_ENABLING_REQUEST;
        }

        @Override
        public Generic.GenericType type()
        {
            return Generic.GenericType.LINK_ENABLING;
        }
    },

    LINK_BANDWIDTH_REPLY
    {
        @Override
        public RequestType request()
        {
            return LINK_BANDWIDTH_REQUEST;
        }

        @Override
        public Generic.GenericType type()
        {
            return Generic.GenericType.LINK_BANDWIDTH;
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
