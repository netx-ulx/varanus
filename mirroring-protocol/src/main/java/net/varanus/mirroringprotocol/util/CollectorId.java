package net.varanus.mirroringprotocol.util;


import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.io.Serializers;
import net.varanus.util.io.exception.IOChannelReadException;
import net.varanus.util.io.exception.IOChannelWriteException;
import net.varanus.util.io.serializer.IOReader;
import net.varanus.util.io.serializer.IOSerializer;
import net.varanus.util.io.serializer.IOWriter;
import net.varanus.util.text.StringUtils;


/**
 * 
 */
@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class CollectorId
{
    public static CollectorId of( long id )
    {
        return new CollectorId(id);
    }

    public static CollectorId of( String str )
    {
        return of(StringUtils.parseUnsignedLong(str));
    }

    private final long id;

    private CollectorId( long id )
    {
        this.id = id;
    }

    public long getLong()
    {
        return id;
    }

    @Override
    public boolean equals( Object other )
    {
        return (other instanceof CollectorId)
               && this.equals((CollectorId)other);
    }

    public boolean equals( CollectorId other )
    {
        return (other != null)
               && this.id == other.id;
    }

    @Override
    public int hashCode()
    {
        return Long.hashCode(id);
    }

    @Override
    public String toString()
    {
        return String.valueOf(id);
    }

    @ReturnValuesAreNonnullByDefault
    public static final class IO
    {
        public static IOWriter<CollectorId> writer()
        {
            return Serial.INSTANCE;
        }

        public static IOReader<CollectorId> reader()
        {
            return Serial.INSTANCE;
        }

        private static enum Serial implements IOSerializer<CollectorId>
        {
            INSTANCE;

            @Override
            public void write( CollectorId id, WritableByteChannel ch ) throws IOChannelWriteException
            {
                Serializers.longWriter(ByteOrder.BIG_ENDIAN).writeLong(id.id, ch);
            }

            @Override
            public CollectorId read( ReadableByteChannel ch ) throws IOChannelReadException
            {
                return of(Serializers.longReader(ByteOrder.BIG_ENDIAN).readLong(ch));
            }
        }

        private IO()
        {
            // not used
        }
    }
}
