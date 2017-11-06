package net.varanus.mirroringprotocol;


import static net.varanus.mirroringprotocol.CollectionType.PROBING;
import static net.varanus.mirroringprotocol.CollectionType.SAMPLING;

import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import org.projectfloodlight.openflow.types.DatapathId;
import org.slf4j.Logger;

import net.varanus.mirroringprotocol.SamplingReply.IO.CompressionStrategy;
import net.varanus.mirroringprotocol.util.CollectorId;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.io.exception.IOChannelReadException;
import net.varanus.util.io.exception.IOChannelWriteException;
import net.varanus.util.io.serializer.IOReader;
import net.varanus.util.io.serializer.IOWriter;


/**
 * 
 */
@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class GenericReply
{
    public static GenericReply fromSampling( SamplingReply reply )
    {
        return new GenericReply(
            SAMPLING,
            Optional.of(reply),
            Optional.empty());
    }

    public static GenericReply fromProbing( ProbingReply reply )
    {
        return new GenericReply(
            PROBING,
            Optional.empty(),
            Optional.of(reply));
    }

    private final CollectionType          type;
    private final Optional<SamplingReply> sampReply;
    private final Optional<ProbingReply>  probReply;

    private GenericReply( CollectionType type,
                          Optional<SamplingReply> sampReply,
                          Optional<ProbingReply> probReply )
    {
        this.type = type;
        this.sampReply = sampReply;
        this.probReply = probReply;
    }

    public CollectionType getType()
    {
        return type;
    }

    public SamplingReply forSampling() throws NoSuchElementException
    {
        return sampReply.orElseThrow(() -> new NoSuchElementException("called incorrect reply method"));
    }

    public ProbingReply forProbing() throws NoSuchElementException
    {
        return probReply.orElseThrow(() -> new NoSuchElementException("called incorrect reply method"));
    }

    @Override
    public String toString()
    {
        switch (type) {
            case SAMPLING:
                return "sampling" + forSampling().toString();

            case PROBING:
                return "probing" + forProbing().toString();

            default:
                throw new AssertionError("unexpected enum value");
        }
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class IO
    {
        public static IOWriter<GenericReply> writer( CollectorId collectorId,
                                                     CompressionStrategy compStrat,
                                                     Logger log )
        {
            return writer(collectorId, compStrat, log, log);
        }

        public static IOWriter<GenericReply> writer( CollectorId collectorId,
                                                     CompressionStrategy compStrat,
                                                     Logger sampLog,
                                                     Logger probLog )
        {
            IOWriter<SamplingReply> sampWriter = SamplingReply.IO.writer(collectorId, compStrat, sampLog);
            IOWriter<ProbingReply> probWriter = ProbingReply.IO.writer(collectorId, probLog);
            return new IOWriter<GenericReply>() {
                @Override
                public void write( GenericReply req, WritableByteChannel ch ) throws IOChannelWriteException
                {
                    CollectionType.IO.writer().write(req.type, ch);
                    switch (req.type) {
                        case SAMPLING:
                            sampWriter.write(req.forSampling(), ch);
                        break;

                        case PROBING:
                            probWriter.write(req.forProbing(), ch);
                        break;

                        default:
                            throw new AssertionError("unexpected enum value");
                    }
                }
            };
        }

        public static IOReader<GenericReply> reader( CollectorId collectorId,
                                                     Function<DatapathId, String> idAliaser,
                                                     CompressionStrategy compStrat,
                                                     Logger log )
        {
            return reader(collectorId, idAliaser, compStrat, log, log);
        }

        public static IOReader<GenericReply> reader( CollectorId collectorId,
                                                     Function<DatapathId, String> idAliaser,
                                                     CompressionStrategy compStrat,
                                                     Logger sampLog,
                                                     Logger probLog )
        {
            IOReader<SamplingReply> sampReader = SamplingReply.IO.reader(collectorId, idAliaser, compStrat, sampLog);
            IOReader<ProbingReply> probReader = ProbingReply.IO.reader(collectorId, idAliaser, probLog);
            return new IOReader<GenericReply>() {
                @Override
                public GenericReply read( ReadableByteChannel ch ) throws IOChannelReadException
                {
                    CollectionType type = CollectionType.IO.reader().read(ch);
                    switch (type) {
                        case SAMPLING:
                            return GenericReply.fromSampling(sampReader.read(ch));

                        case PROBING:
                            return GenericReply.fromProbing(probReader.read(ch));

                        default:
                            throw new AssertionError("unexpected enum value");
                    }
                }
            };
        }
    }
}
