package net.varanus.configprotocol;


import static net.varanus.configprotocol.ReplyType.LINK_BANDWIDTH_REPLY;
import static net.varanus.configprotocol.ReplyType.LINK_ENABLING_REPLY;

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
    LINK_ENABLING_REQUEST
    {
        @Override
        public ReplyType reply()
        {
            return LINK_ENABLING_REPLY;
        }

        @Override
        public Generic.GenericType type()
        {
            return Generic.GenericType.LINK_ENABLING;
        }
    },

    LINK_BANDWIDTH_REQUEST
    {
        @Override
        public ReplyType reply()
        {
            return LINK_BANDWIDTH_REPLY;
        }

        @Override
        public Generic.GenericType type()
        {
            return Generic.GenericType.LINK_BANDWIDTH;
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
