package net.varanus.mirroringprotocol.util;


import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.time.Instant;
import java.util.Objects;

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
import net.varanus.util.time.Timed;


/**
 * 
 */
@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class TimedPacketSummary implements Timed<PacketSummary>
{
    private final PacketSummary summary;
    private final Instant       timestamp;

    public TimedPacketSummary( PacketSummary summary, Instant timestamp )
    {
        this.summary = Objects.requireNonNull(summary);
        this.timestamp = Objects.requireNonNull(timestamp);
    }

    @Override
    public PacketSummary value()
    {
        return summary;
    }

    @Override
    public Instant timestamp()
    {
        return timestamp;
    }

    @Override
    public Timed<PacketSummary> withTime( Instant timestamp )
    {
        return new TimedPacketSummary(summary, timestamp);
    }

    @Override
    public boolean equals( Object other )
    {
        return (other instanceof TimedPacketSummary) && this.equals((TimedPacketSummary)other);
    }

    public boolean equals( TimedPacketSummary other )
    {
        return (other != null)
               && this.summary.equals(other.summary)
               && this.timestamp.equals(other.timestamp);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(summary, timestamp);
    }

    @Override
    public String toString()
    {
        return String.format("%s [%s]", summary, timestamp);
    }

    @ReturnValuesAreNonnullByDefault
    public static final class IO
    {
        public static IOWriter<TimedPacketSummary> writer()
        {
            return Serial.INSTANCE;
        }

        public static IOReader<TimedPacketSummary> reader()
        {
            return Serial.INSTANCE;
        }

        private static enum Serial implements IOSerializer<TimedPacketSummary>
        {
            INSTANCE;

            @Override
            public void write( TimedPacketSummary timed, WritableByteChannel ch ) throws IOChannelWriteException
            {
                PacketSummary.IO.writer().write(timed.summary, ch);
                Serializers.instantWriter().write(timed.timestamp, ch);
            }

            @Override
            public TimedPacketSummary read( ReadableByteChannel ch ) throws IOChannelReadException
            {
                PacketSummary summary = PacketSummary.IO.reader().read(ch);
                Instant timestamp = Serializers.instantReader().read(ch);
                return new TimedPacketSummary(summary, timestamp);
            }
        }

        private IO()
        {
            // not used
        }
    }
}
