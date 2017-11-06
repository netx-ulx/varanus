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
public final class GenericRequest
{
    public static GenericRequest fromSampling( SamplingRequest req )
    {
        return new GenericRequest(
            SAMPLING,
            Optional.of(req),
            Optional.empty());
    }

    public static GenericRequest fromProbing( ProbingRequest req )
    {
        return new GenericRequest(
            PROBING,
            Optional.empty(),
            Optional.of(req));
    }

    private final CollectionType            type;
    private final Optional<SamplingRequest> sampRequest;
    private final Optional<ProbingRequest>  probRequest;

    private GenericRequest( CollectionType type,
                            Optional<SamplingRequest> sampRequest,
                            Optional<ProbingRequest> probRequest )
    {
        this.type = type;
        this.sampRequest = sampRequest;
        this.probRequest = probRequest;
    }

    public CollectionType getType()
    {
        return type;
    }

    public SamplingRequest forSampling() throws NoSuchElementException
    {
        return sampRequest.orElseThrow(() -> new NoSuchElementException("called incorrect request method"));
    }

    public ProbingRequest forProbing() throws NoSuchElementException
    {
        return probRequest.orElseThrow(() -> new NoSuchElementException("called incorrect request method"));
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
        public static IOWriter<GenericRequest> writer( CollectorId collectorId, Logger log )
        {
            return writer(collectorId, log, log);
        }

        public static IOWriter<GenericRequest> writer( CollectorId collectorId, Logger sampLog, Logger probLog )
        {
            IOWriter<SamplingRequest> sampWriter = SamplingRequest.IO.writer(collectorId, sampLog);
            IOWriter<ProbingRequest> probWriter = ProbingRequest.IO.writer(collectorId, probLog);
            return new IOWriter<GenericRequest>() {
                @Override
                public void write( GenericRequest req, WritableByteChannel ch ) throws IOChannelWriteException
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

        public static IOReader<GenericRequest> reader( CollectorId collectorId,
                                                       Function<DatapathId, String> idAliaser,
                                                       Logger log )
        {
            return reader(collectorId, idAliaser, log, log);
        }

        public static IOReader<GenericRequest> reader( CollectorId collectorId,
                                                       Function<DatapathId, String> idAliaser,
                                                       Logger sampLog,
                                                       Logger probLog )
        {
            IOReader<SamplingRequest> sampReader = SamplingRequest.IO.reader(collectorId, idAliaser, sampLog);
            IOReader<ProbingRequest> probReader = ProbingRequest.IO.reader(collectorId, idAliaser, probLog);
            return new IOReader<GenericRequest>() {
                @Override
                public GenericRequest read( ReadableByteChannel ch ) throws IOChannelReadException
                {
                    CollectionType type = CollectionType.IO.reader().read(ch);
                    switch (type) {
                        case SAMPLING:
                            return GenericRequest.fromSampling(sampReader.read(ch));

                        case PROBING:
                            return GenericRequest.fromProbing(probReader.read(ch));

                        default:
                            throw new AssertionError("unexpected enum value");
                    }
                }
            };
        }
    }
}
