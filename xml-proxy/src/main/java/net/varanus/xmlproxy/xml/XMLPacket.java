package net.varanus.xmlproxy.xml;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.io.Serializers;
import net.varanus.util.io.ByteBuffers.BufferType;
import net.varanus.util.io.exception.IOChannelReadException;
import net.varanus.util.io.exception.IOChannelWriteException;
import net.varanus.util.io.serializer.IOReader;
import net.varanus.util.io.serializer.IOSerializer;
import net.varanus.util.io.serializer.IOWriter;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class XMLPacket
{
    public static XMLPacket newTopologyRequest()
    {
        return new XMLPacket(Type.REQUEST_TOPOLOGY, EMPTY_DATA);
    }

    public static XMLPacket newTopologyReply( byte[] data )
    {
        return new XMLPacket(Type.TOPOLOGY_SUCCESS, Objects.requireNonNull(data));
    }

    public static XMLPacket newTopologyFailure( byte[] data )
    {
        return new XMLPacket(Type.TOPOLOGY_FAILURE, Objects.requireNonNull(data));
    }

    public static XMLPacket newStatisticsRequest( byte[] data )
    {
        return new XMLPacket(Type.REQUEST_STATISTICS, Objects.requireNonNull(data));
    }

    public static XMLPacket newStatisticsReply( byte[] data )
    {
        return new XMLPacket(Type.STATISTICS_SUCCESS, Objects.requireNonNull(data));
    }

    public static XMLPacket newStatisticsFailure( byte[] data )
    {
        return new XMLPacket(Type.STATISTICS_FAILURE, Objects.requireNonNull(data));
    }

    public static XMLPacket newRouteRequest( byte[] data )
    {
        return new XMLPacket(Type.REQUEST_ROUTE, Objects.requireNonNull(data));
    }

    public static XMLPacket newRouteReply( byte[] data )
    {
        return new XMLPacket(Type.ROUTE_SUCCESS, Objects.requireNonNull(data));
    }

    public static XMLPacket newRouteFailure( byte[] data )
    {
        return new XMLPacket(Type.ROUTE_FAILURE, Objects.requireNonNull(data));
    }

    public static XMLPacket newLinkConfigRequest( byte[] data )
    {
        return new XMLPacket(Type.REQUEST_LINK_CONFIG, Objects.requireNonNull(data));
    }

    public static XMLPacket newLinkConfigReply( byte[] data )
    {
        return new XMLPacket(Type.LINK_CONFIG_SUCCESS, Objects.requireNonNull(data));
    }

    public static XMLPacket newLinkConfigFailure( byte[] data )
    {
        return new XMLPacket(Type.LINK_CONFIG_FAILURE, Objects.requireNonNull(data));
    }

    public static XMLPacket newCommandRequest( byte[] data )
    {
        return new XMLPacket(Type.REQUEST_COMMAND, Objects.requireNonNull(data));
    }

    public static XMLPacket newCommandReply( byte[] data )
    {
        return new XMLPacket(Type.COMMAND_SUCCESS, Objects.requireNonNull(data));
    }

    public static XMLPacket newCommandFailure( byte[] data )
    {
        return new XMLPacket(Type.COMMAND_FAILURE, Objects.requireNonNull(data));
    }

    public static XMLPacket newLinkStateRequest( byte[] data )
    {
        return new XMLPacket(Type.REQUEST_LINK_STATE, Objects.requireNonNull(data));
    }

    public static XMLPacket newLinkStateReply( byte[] data )
    {
        return new XMLPacket(Type.LINK_STATE_SUCCESS, Objects.requireNonNull(data));
    }

    public static XMLPacket newLinkStateFailure( byte[] data )
    {
        return new XMLPacket(Type.LINK_STATE_FAILURE, Objects.requireNonNull(data));
    }

    public static XMLPacket newTrafficInjectRequest( byte[] data )
    {
        return new XMLPacket(Type.REQUEST_TRAFFIC_INJECT, Objects.requireNonNull(data));
    }

    public static XMLPacket newTrafficInjectReply( byte[] data )
    {
        return new XMLPacket(Type.TRAFFIC_INJECT_SUCCESS, Objects.requireNonNull(data));
    }

    public static XMLPacket newTrafficInjectFailure( byte[] data )
    {
        return new XMLPacket(Type.TRAFFIC_INJECT_FAILURE, Objects.requireNonNull(data));
    }

    private static final byte[] EMPTY_DATA     = new byte[0];
    private static final byte[] MAGIC_PREAMBLE = "SGP".getBytes(StandardCharsets.UTF_8);

    private final Type   type;
    private final byte[] data;

    private XMLPacket( Type type, byte[] data )
    {
        this.type = type;
        this.data = data;
    }

    public Type getType()
    {
        return type;
    }

    public byte[] getData()
    {
        return data;
    }

    @ReturnValuesAreNonnullByDefault
    public static enum Type
    {
        NONE(0),
        REQUEST_TOPOLOGY(1),
        TOPOLOGY_SUCCESS(2),
        TOPOLOGY_FAILURE(3),
        REQUEST_STATISTICS(4),
        STATISTICS_SUCCESS(5),
        STATISTICS_FAILURE(6),
        REQUEST_ROUTE(7),
        ROUTE_SUCCESS(8),
        ROUTE_FAILURE(9),
        REQUEST_LINK_CONFIG(10),
        LINK_CONFIG_SUCCESS(11),
        LINK_CONFIG_FAILURE(12),
        REQUEST_COMMAND(13),
        COMMAND_SUCCESS(14),
        COMMAND_FAILURE(15),
        REQUEST_LINK_STATE(16),
        LINK_STATE_SUCCESS(17),
        LINK_STATE_FAILURE(18),
        REQUEST_TRAFFIC_INJECT(19),
        TRAFFIC_INJECT_SUCCESS(20),
        TRAFFIC_INJECT_FAILURE(21);

        private final int value;

        private Type( int value )
        {
            this.value = value;
        }

        private static Type fromValue( int value )
        {
            for (Type t : Type.values()) {
                if (t.value == value)
                    return t;
            }
            return NONE;
        }

        @ReturnValuesAreNonnullByDefault
        public static final class IO
        {
            public static IOWriter<Type> writer()
            {
                return TypeSerializer.INSTANCE;
            }

            public static IOReader<Type> reader()
            {
                return TypeSerializer.INSTANCE;
            }

            private static enum TypeSerializer implements IOSerializer<Type>
            {
                INSTANCE;

                @Override
                public void write( Type type, WritableByteChannel ch ) throws IOChannelWriteException
                {
                    int value = type.value;
                    Serializers.intWriter(ByteOrder.BIG_ENDIAN).writeInt(value, ch);
                }

                @Override
                public Type read( ReadableByteChannel ch ) throws IOChannelReadException
                {
                    int value = Serializers.intReader(ByteOrder.BIG_ENDIAN).readInt(ch);
                    return fromValue(value);
                }
            }

            private IO()
            {
                // not used
            }
        }
    }

    @ReturnValuesAreNonnullByDefault
    public static final class IO
    {
        public static IOWriter<XMLPacket> writer()
        {
            return XMLPacketSerializer.INSTANCE;
        }

        public static IOReader<XMLPacket> reader()
        {
            return XMLPacketSerializer.INSTANCE;
        }

        @FieldsAreNonnullByDefault
        @ParametersAreNonnullByDefault
        private static enum XMLPacketSerializer implements IOSerializer<XMLPacket>
        {
            INSTANCE;

            @Override
            public void write( XMLPacket pkt, WritableByteChannel ch ) throws IOChannelWriteException
            {
                Serializers.rawBytesWriter().write(ByteBuffer.wrap(MAGIC_PREAMBLE), ch);
                Type.IO.writer().write(pkt.type, ch);
                Serializers.bufferWriter().write(ByteBuffer.wrap(pkt.data), ch);
            }

            @Override
            public XMLPacket read( ReadableByteChannel ch ) throws IOChannelReadException
            {
                validatePreamble(PREAMBLE_READER.read(ch));
                Type type = Type.IO.reader().read(ch);
                ByteBuffer data = Serializers.allocatedBufferReader(BufferType.ARRAY_BACKED).read(ch);

                return new XMLPacket(type, data.array());
            }

            private static final IOReader<ByteBuffer> PREAMBLE_READER =
                Serializers.allocatedRawBytesReader(MAGIC_PREAMBLE.length, BufferType.ARRAY_BACKED);

            private static void validatePreamble( ByteBuffer preamble ) throws IOChannelReadException
            {
                if (!preamble.equals(ByteBuffer.wrap(MAGIC_PREAMBLE)))
                    throw new IOChannelReadException("Bad magic preamble");
            }
        }

        private IO()
        {
            // not used
        }
    }
}
