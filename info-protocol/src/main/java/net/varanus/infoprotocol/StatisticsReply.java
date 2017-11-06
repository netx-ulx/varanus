package net.varanus.infoprotocol;


import java.nio.ByteOrder;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import org.projectfloodlight.openflow.types.DatapathId;
import org.slf4j.Logger;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.functional.PossibleDouble;
import net.varanus.util.functional.Report;
import net.varanus.util.io.Serializers;
import net.varanus.util.io.exception.IOChannelReadException;
import net.varanus.util.io.exception.IOChannelWriteException;
import net.varanus.util.io.serializer.IOReader;
import net.varanus.util.io.serializer.IOWriter;
import net.varanus.util.lang.MoreObjects;
import net.varanus.util.openflow.types.FlowedUnidiNodePorts;
import net.varanus.util.time.TimeDouble;
import net.varanus.util.unitvalue.si.InfoDouble;
import net.varanus.util.unitvalue.si.MetricDouble;


/**
 * 
 */
@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class StatisticsReply extends Generic.Stat
{
    public static StatisticsReply of( FlowedUnidiNodePorts link,
                                      TimeDouble latency,
                                      boolean isProbeLatency,
                                      PossibleDouble byteLoss,
                                      boolean isProbeLoss,
                                      InfoDouble throughput,
                                      InfoDouble txRate,
                                      InfoDouble recRate,
                                      InfoDouble umtchTxRate,
                                      InfoDouble umtchRecRate,
                                      MetricDouble srcPktDropRate,
                                      MetricDouble destPktDropRate,
                                      Instant timestamp )
    {
        return new StatisticsReply(Report.of(new Result(
            Objects.requireNonNull(link),
            Objects.requireNonNull(latency),
            isProbeLatency,
            Objects.requireNonNull(byteLoss),
            isProbeLoss,
            Objects.requireNonNull(throughput),
            Objects.requireNonNull(txRate),
            Objects.requireNonNull(recRate),
            Objects.requireNonNull(umtchTxRate),
            Objects.requireNonNull(umtchRecRate),
            Objects.requireNonNull(srcPktDropRate),
            Objects.requireNonNull(destPktDropRate),
            Objects.requireNonNull(timestamp))));
    }

    public static StatisticsReply empty( FlowedUnidiNodePorts link, Instant timestamp )
    {
        return new StatisticsReply(Report.of(new Result(
            Objects.requireNonNull(link),
            TimeDouble.absent(),
            false,
            PossibleDouble.absent(),
            false,
            InfoDouble.absent(),
            InfoDouble.absent(),
            InfoDouble.absent(),
            InfoDouble.absent(),
            InfoDouble.absent(),
            MetricDouble.absent(),
            MetricDouble.absent(),
            timestamp)));
    }

    public static StatisticsReply ofError( String errorMsg )
    {
        return new StatisticsReply(Report.ofError(errorMsg));
    }

    private final Report<Result> result;

    private StatisticsReply( Report<Result> result )
    {
        this.result = result;
    }

    public boolean hasResult()
    {
        return result.hasValue();
    }

    public FlowedUnidiNodePorts getLink() throws NoSuchElementException
    {
        return result.getValue().link;
    }

    public TimeDouble getLatency() throws NoSuchElementException
    {
        return result.getValue().latency;
    }

    public boolean isProbeLatency() throws NoSuchElementException
    {
        return result.getValue().isProbeLatency;
    }

    public PossibleDouble getByteLoss() throws NoSuchElementException
    {
        return result.getValue().byteLoss;
    }

    public boolean isProbeLoss() throws NoSuchElementException
    {
        return result.getValue().isProbeLoss;
    }

    public InfoDouble getThroughput() throws NoSuchElementException
    {
        return result.getValue().throughput;
    }

    public InfoDouble getTransmissionRate() throws NoSuchElementException
    {
        return result.getValue().txRate;
    }

    public InfoDouble getReceptionRate() throws NoSuchElementException
    {
        return result.getValue().recRate;
    }

    public InfoDouble getUnmatchedTransmissionRate() throws NoSuchElementException
    {
        return result.getValue().umtchTxRate;
    }

    public InfoDouble getUnmatchedReceptionRate() throws NoSuchElementException
    {
        return result.getValue().umtchRecRate;
    }

    public MetricDouble getSourcePacketDropRate() throws NoSuchElementException
    {
        return result.getValue().srcPktDropRate;
    }

    public MetricDouble getDestinationPacketDropRate() throws NoSuchElementException
    {
        return result.getValue().destPktDropRate;
    }

    public Instant getTimestamp() throws NoSuchElementException
    {
        return result.getValue().timestamp;
    }

    public boolean hasError()
    {
        return result.hasError();
    }

    public String getError() throws NoSuchElementException
    {
        return result.getError();
    }

    @Override
    public String toString()
    {
        return String.format("( %s )", result);
    }

    @Immutable
    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    private static final class Result
    {
        final FlowedUnidiNodePorts link;
        final TimeDouble           latency;
        final boolean              isProbeLatency;
        final PossibleDouble       byteLoss;
        final boolean              isProbeLoss;
        final InfoDouble           throughput;
        final InfoDouble           txRate;
        final InfoDouble           recRate;
        final InfoDouble           umtchTxRate;
        final InfoDouble           umtchRecRate;
        final MetricDouble         srcPktDropRate;
        final MetricDouble         destPktDropRate;
        final Instant              timestamp;

        Result( FlowedUnidiNodePorts link,
                TimeDouble latency,
                boolean isProbeLatency,
                PossibleDouble byteLoss,
                boolean isProbeLoss,
                InfoDouble throughput,
                InfoDouble txRate,
                InfoDouble recRate,
                InfoDouble umtchTxRate,
                InfoDouble umtchRecRate,
                MetricDouble srcPktDropRate,
                MetricDouble destPktDropRate,
                Instant timestamp )
        {
            this.link = link;
            this.latency = latency;
            this.isProbeLatency = isProbeLatency;
            this.byteLoss = byteLoss;
            this.isProbeLoss = isProbeLoss;
            this.throughput = throughput;
            this.txRate = txRate;
            this.recRate = recRate;
            this.umtchTxRate = umtchTxRate;
            this.umtchRecRate = umtchRecRate;
            this.srcPktDropRate = srcPktDropRate;
            this.destPktDropRate = destPktDropRate;
            this.timestamp = timestamp;
        }

        @Override
        public String toString()
        {
            return String.format("link %s, latency=%s[probe=%s], byte_loss=%s[probe=%s], "
                                 + "throughput=%s, tx_rate=%s, rx_rate=%s, unmatched_tx_rate=%s, unmatched_rx_rate=%s, "
                                 + "src_packet_drop_rate=%s, dest_packet_drop_rate=%s, "
                                 + "timestamp=%s",
                link, latency, isProbeLatency, byteLoss, isProbeLoss,
                throughput, txRate, recRate, umtchTxRate, umtchRecRate,
                srcPktDropRate, destPktDropRate,
                timestamp);
        }
    }

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class IO
    {
        private static final long REPLY_PREAMBLE = 0x980a801f22029f2aL;

        public static IOWriter<StatisticsReply> writer( Logger log )
        {
            Objects.requireNonNull(log);
            return new IOWriter<StatisticsReply>() {
                @Override
                public void write( StatisticsReply reply, WritableByteChannel ch ) throws IOChannelWriteException
                {
                    if (log.isTraceEnabled()) {
                        log.trace("Writing statistics reply from server: reply preamble 0x{}",
                            Long.toHexString(REPLY_PREAMBLE));
                    }
                    Serializers.longWriter(ByteOrder.BIG_ENDIAN).writeLong(REPLY_PREAMBLE, ch);

                    Report<Result> result = reply.result;
                    log.trace("Writing statistics reply from server: {}", result);
                    Report.IO.writer(resultWriter()).write(result, ch);
                }
            };
        }

        public static IOReader<StatisticsReply> reader( Function<DatapathId, String> idAliaser, Logger log )
        {
            MoreObjects.requireNonNull(idAliaser, "idAliaser", log, "log");
            return new IOReader<StatisticsReply>() {
                @Override
                public StatisticsReply read( ReadableByteChannel ch ) throws IOChannelReadException
                {
                    final long preamble = Serializers.longReader(ByteOrder.BIG_ENDIAN).readLong(ch);
                    if (preamble != REPLY_PREAMBLE) {
                        throw new IOChannelReadException(
                            String.format("expected reply preamble of %x from server but found %x instead",
                                REPLY_PREAMBLE, preamble));
                    }
                    if (log.isTraceEnabled()) {
                        log.trace("Read statistics reply from server: reply preamble 0x{}",
                            Long.toHexString(preamble));
                    }

                    Report<Result> result = Report.IO.reader(resultReader(idAliaser)).read(ch);
                    log.trace("Read statistics reply from server: {}", result);

                    return new StatisticsReply(result);
                }
            };
        }

        private static IOWriter<Result> resultWriter()
        {
            return new IOWriter<Result>() {

                @Override
                public void write( Result res, WritableByteChannel ch ) throws IOChannelWriteException
                {
                    FlowedUnidiNodePorts.IO.writer().write(res.link, ch);
                    PossibleDouble.IO.writer(ByteOrder.BIG_ENDIAN).write(res.latency.asPossibleInNanos(), ch);
                    Serializers.boolWriter().write(res.isProbeLatency, ch);
                    PossibleDouble.IO.writer(ByteOrder.BIG_ENDIAN).write(res.byteLoss, ch);
                    Serializers.boolWriter().write(res.isProbeLoss, ch);
                    PossibleDouble.IO.writer(ByteOrder.BIG_ENDIAN).write(res.throughput.asPossibleInBits(), ch);
                    PossibleDouble.IO.writer(ByteOrder.BIG_ENDIAN).write(res.txRate.asPossibleInBits(), ch);
                    PossibleDouble.IO.writer(ByteOrder.BIG_ENDIAN).write(res.recRate.asPossibleInBits(), ch);
                    PossibleDouble.IO.writer(ByteOrder.BIG_ENDIAN).write(res.umtchTxRate.asPossibleInBits(), ch);
                    PossibleDouble.IO.writer(ByteOrder.BIG_ENDIAN).write(res.umtchRecRate.asPossibleInBits(), ch);
                    PossibleDouble.IO.writer(ByteOrder.BIG_ENDIAN).write(res.srcPktDropRate.asPossibleInUnits(), ch);
                    PossibleDouble.IO.writer(ByteOrder.BIG_ENDIAN).write(res.destPktDropRate.asPossibleInUnits(), ch);
                    Serializers.instantWriter().write(res.timestamp, ch);
                }
            };
        }

        private static IOReader<Result> resultReader( Function<DatapathId, String> idAliaser )
        {
            return new IOReader<Result>() {

                @Override
                public Result read( ReadableByteChannel ch ) throws IOChannelReadException
                {
                    FlowedUnidiNodePorts link = FlowedUnidiNodePorts.IO.reader(idAliaser).read(ch);
                    TimeDouble latency =
                        TimeDouble.ofPossibleNanos(PossibleDouble.IO.reader(ByteOrder.BIG_ENDIAN).read(ch));
                    boolean isProbeLatency = Serializers.boolReader().read(ch);
                    PossibleDouble byteLoss = PossibleDouble.IO.reader(ByteOrder.BIG_ENDIAN).read(ch);
                    boolean isProbeLoss = Serializers.boolReader().read(ch);
                    InfoDouble throughput =
                        InfoDouble.ofPossibleBits(PossibleDouble.IO.reader(ByteOrder.BIG_ENDIAN).read(ch));
                    InfoDouble txRate =
                        InfoDouble.ofPossibleBits(PossibleDouble.IO.reader(ByteOrder.BIG_ENDIAN).read(ch));
                    InfoDouble recRate =
                        InfoDouble.ofPossibleBits(PossibleDouble.IO.reader(ByteOrder.BIG_ENDIAN).read(ch));
                    InfoDouble umtchTxRate =
                        InfoDouble.ofPossibleBits(PossibleDouble.IO.reader(ByteOrder.BIG_ENDIAN).read(ch));
                    InfoDouble umtchRecRate =
                        InfoDouble.ofPossibleBits(PossibleDouble.IO.reader(ByteOrder.BIG_ENDIAN).read(ch));
                    MetricDouble srcPktDropRate =
                        MetricDouble.ofPossibleUnits(PossibleDouble.IO.reader(ByteOrder.BIG_ENDIAN).read(ch));
                    MetricDouble destPktDropRate =
                        MetricDouble.ofPossibleUnits(PossibleDouble.IO.reader(ByteOrder.BIG_ENDIAN).read(ch));
                    Instant timestamp = Serializers.instantReader().read(ch);

                    return new Result(link, latency, isProbeLatency, byteLoss, isProbeLoss,
                        throughput, txRate, recRate, umtchTxRate, umtchRecRate,
                        srcPktDropRate, destPktDropRate,
                        timestamp);
                }
            };
        }

        private IO()
        {
            // not used
        }
    }
}
