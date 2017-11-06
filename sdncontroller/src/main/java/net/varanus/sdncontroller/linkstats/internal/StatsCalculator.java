package net.varanus.sdncontroller.linkstats.internal;


import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.OptionalLong;

import org.projectfloodlight.openflow.protocol.OFFlowRemoved;
import org.projectfloodlight.openflow.types.U64;
import org.slf4j.Logger;

import net.varanus.mirroringprotocol.util.PacketSummary;
import net.varanus.mirroringprotocol.util.TimedPacketSummary;
import net.varanus.sdncontroller.linkstats.FlowedLinkStats.SwitchCounterSubStats;
import net.varanus.sdncontroller.linkstats.FlowedLinkStats.TrajectorySubStats;
import net.varanus.sdncontroller.linkstats.GeneralLinkStats.LLDPProbingSubStats;
import net.varanus.sdncontroller.linkstats.GeneralLinkStats.SecureProbingSubStats;
import net.varanus.sdncontroller.linkstats.StatsBuilders.LatencyStatsBuilder;
import net.varanus.sdncontroller.linkstats.StatsBuilders.LossStatsBuilder;
import net.varanus.sdncontroller.linkstats.sample.LLDPProbingSample;
import net.varanus.sdncontroller.linkstats.sample.SecureProbingSample;
import net.varanus.sdncontroller.linkstats.sample.SwitchCounterSample;
import net.varanus.sdncontroller.linkstats.sample.TrajectorySample;
import net.varanus.sdncontroller.logging.Logging;
import net.varanus.sdncontroller.types.DatapathLink;
import net.varanus.sdncontroller.types.FlowedLink;
import net.varanus.sdncontroller.util.Fields;
import net.varanus.sdncontroller.util.Ratio;
import net.varanus.util.functional.Possible;
import net.varanus.util.openflow.types.NodeId;
import net.varanus.util.time.TimeDouble;
import net.varanus.util.time.TimeDoubleUnit;
import net.varanus.util.time.TimeLong;
import net.varanus.util.unitvalue.si.InfoDouble;
import net.varanus.util.unitvalue.si.MetricDouble;


/**
 * 
 */
final class StatsCalculator
{
    private static final Logger LOG = Logging.linkstats.LOG;

    static void calcStatistics( LLDPProbingSubStats.Builder builder, LLDPProbingSample sample )
    {
        if (!sample.hasResults()) {
            DatapathLink link = sample.getLink();
            LOG.trace("Resetting LLDP-probing sub-statistics for datapath-link {}", link);

            builder
                .clear()
                .incrementNumUpdates();
        }
        else {
            DatapathLink link = sample.getLink();
            LOG.trace("Calculating LLDP-probing sub-statistics for datapath-link {}", link);

            handleLatency(builder, sample.getLatency(), sample.getLatencyTimestamp());
            builder.incrementNumUpdates();
        }
    }

    static void calcStatistics( SecureProbingSubStats.Builder builder, SecureProbingSample sample )
    {
        if (!sample.hasResults()) {
            DatapathLink link = sample.getLink();
            LOG.trace("Resetting secure-probing sub-statistics for datapath-link {}", link);

            builder
                .clear()
                .incrementNumUpdates();
        }
        else {
            DatapathLink link = sample.getLink();
            LOG.trace("Calculating secure-probing sub-statistics for datapath-link {}", link);

            // BitMatch bitMatch = sample.getBitMatch();
            Possible<TimedPacketSummary> srcSumm = sample.getSourceSummary();
            Possible<TimedPacketSummary> destSumm = sample.getDestinationSummary();
            // Duration collDur = sample.getCollectDuration();
            Instant collFinTime = sample.getCollectFinishingTime();

            TimeDouble latency = TimeDouble.absent();
            long xmittedBytes = 0;  // total bytes tx from source
            long correctBytes = 0;  // total bytes of packets tx and rx
            long xmittedPkts = 0;  // total packets tx from source
            long correctPkts = 0;  // total packets tx and rx

            if (srcSumm.isPresent()) {
                PacketSummary srcPkt = srcSumm.get().value();
                xmittedBytes = srcPkt.length();
                xmittedPkts = 1;
            }
            if (srcSumm.isPresent() && destSumm.isPresent()) {
                PacketSummary srcPkt = srcSumm.get().value();
                PacketSummary destPkt = destSumm.get().value();

                if (srcPkt.equals(destPkt)) {
                    Instant srcTime = srcSumm.get().timestamp();
                    Instant destTime = destSumm.get().timestamp();
                    Duration dur = Duration.between(srcTime, destTime);

                    // a negative delay turns into a packet loss
                    if (dur.isNegative()) {
                        LOG.warn("Found negative delay of {} in secure-probing sample for link {}", dur, link);
                    }
                    else {
                        latency = TimeDouble.fromDuration(dur);
                        correctBytes = destPkt.length();
                        correctPkts = 1;
                    }
                }
            }

            Ratio byteLoss = calcLoss(xmittedBytes, correctBytes);
            Ratio pktLoss = calcLoss(xmittedPkts, correctPkts);

            handleLatency(builder, latency, collFinTime);
            handleByteLoss(builder, byteLoss, collFinTime);
            handlePacketLoss(builder, pktLoss, collFinTime);
            builder.incrementNumUpdates();
        }
    }

    static void calcStatistics( SwitchCounterSubStats.Builder builder, SwitchCounterSample sample )
    {
        if (!sample.hasResults()) {
            FlowedLink flowedLink = sample.getLink();
            LOG.trace("Resetting switch-counter sub-statistics for flowed-link {}", flowedLink);

            builder
                .clear()
                .incrementNumUpdates();
        }
        else {
            FlowedLink flowedLink = sample.getLink();
            LOG.trace("Calculating switch-counter sub-statistics for flowed-link {}", flowedLink);

            OFFlowRemoved srcResult = sample.getSourceResult().value();
            Instant srcTimestamp = sample.getSourceResult().timestamp();
            OFFlowRemoved destResult = sample.getDestinationResult().value();
            Instant destTimestamp = sample.getDestinationResult().timestamp();

            Duration sampDur = sample.getSamplingDuration();

            OptionalLong xmittedBytes = getByteCount(srcResult, flowedLink.getSrcNode());
            InfoDouble txDataRate = calcDataRate(xmittedBytes, sampDur);
            OptionalLong receivedBytes = getByteCount(destResult, flowedLink.getDestNode());
            InfoDouble recDataRate = calcDataRate(receivedBytes, sampDur);

            OptionalLong xmittedPackets = getPacketCount(srcResult, flowedLink.getSrcNode());
            MetricDouble txPacketRate = calcPacketRate(xmittedPackets, sampDur);
            OptionalLong receivedPackets = getPacketCount(destResult, flowedLink.getDestNode());
            MetricDouble recPacketRate = calcPacketRate(receivedPackets, sampDur);

            builder
                .setDataTransmissionRate(txDataRate, srcTimestamp)
                .setDataReceptionRate(recDataRate, destTimestamp)
                .setPacketTransmissionRate(txPacketRate, srcTimestamp)
                .setPacketReceptionRate(recPacketRate, destTimestamp)
                .setLastRoundDuration(TimeLong.fromDuration(sampDur))
                .incrementNumUpdates();
        }
    }

    static void calcStatistics( TrajectorySubStats.Builder builder, TrajectorySample sample )
    {
        if (!sample.hasResults()) {
            FlowedLink flowedLink = sample.getLink();
            LOG.trace("Resetting trajectory sub-statistics for flowed-link {}", flowedLink);

            builder
                .clear()
                .incrementNumUpdates();
        }
        else {
            FlowedLink flowedLink = sample.getLink();
            LOG.trace("Calculating trajectory sub-statistics for flowed-link {}", flowedLink);

            List<TimedPacketSummary> srcSumms = sample.getSourceSummaries();
            long umtchTxBytes = sample.getSourceUnmatchedBytes();
            long umtchTxPkts = sample.getSourceUnmatchedPackets();

            List<TimedPacketSummary> destSumms = sample.getDestinationSummaries();
            long umtchRecBytes = sample.getDestinationUnmatchedBytes();
            long umtchRecPkts = sample.getDestinationUnmatchedPackets();

            Duration collDur = sample.getCollectDuration();
            Instant collFinTime = sample.getCollectFinishingTime();

            long xmittedBytes = 0;  // total bytes tx from source
            long receivedBytes = 0; // total bytes rx from dest
            long correctBytes = 0;  // total bytes of packets tx and rx

            long xmittedPkts = 0;  // total packets tx from source
            long receivedPkts = 0; // total packets rx from dest
            long correctPkts = 0;  // total packets tx and rx

            // map from packet summary to two lists of switch times (src and
            // dest)
            Map<PacketSummary, TrajPacketTimes> map = new LinkedHashMap<>(srcSumms.size());

            // first add all src times
            for (TimedPacketSummary summ : srcSumms) {
                PacketSummary pkt = summ.value();
                TrajPacketTimes times = map.get(pkt);
                if (times == null) {
                    times = new TrajPacketTimes();
                    map.put(pkt, times);
                }

                xmittedBytes += pkt.length();
                xmittedPkts++;
                times.addSourceTime(summ.timestamp());
            }

            // then add the dest times that have correspondence with src ones
            for (TimedPacketSummary summ : destSumms) {
                PacketSummary pkt = summ.value();
                TrajPacketTimes times = map.get(pkt);
                if (times != null) {
                    times.addDestTime(summ.timestamp());
                }
                // TODO else what?

                receivedBytes += pkt.length();
                receivedPkts++;
            }

            // delays of packets that were tx from source and rx from dest
            List<TimeDouble> delays = new ArrayList<>();

            // for goodput calculation
            // List<Instant> correctSrcTimes = new ArrayList<>();
            List<Instant> correctDestTimes = new ArrayList<>();

            for (Entry<PacketSummary, TrajPacketTimes> entry : map.entrySet()) {
                int packetLen = entry.getKey().length();
                List<Instant> srcTimes = entry.getValue().getSourceTimes();
                List<Instant> destTimes = entry.getValue().getDestTimes();

                // this assumes that times in the same position of both lists
                // are from the same packet, which may not be true...
                int numCorrect = Math.min(srcTimes.size(), destTimes.size());
                for (int i = 0; i < numCorrect; i++) {
                    Instant srcTime = srcTimes.get(i);
                    Instant destTime = destTimes.get(i);
                    Duration dur = Duration.between(srcTime, destTime);

                    // a negative delay turns into a packet loss
                    if (dur.isNegative()) {
                        LOG.warn("Found negative delay of {} in trajectory sample for flowed-link {}",
                            dur, flowedLink);
                    }
                    else {
                        correctBytes += packetLen;
                        correctPkts++;
                        delays.add(TimeDouble.fromDuration(dur));
                        // correctSrcTimes.add(srcTime);
                        correctDestTimes.add(destTime);
                    }
                }
            }

            // we try to reduce the window of time when traffic was active in
            // order to get more precise data rate values
            Duration transDur = getTightDurationFromSummaries(srcSumms);
            Duration recepDur = getTightDurationFromSummaries(destSumms);
            if (transDur.isZero() || transDur.isNegative()) transDur = collDur;
            if (recepDur.isZero() || recepDur.isNegative()) recepDur = collDur;

            Duration correctDur = getTightDurationFromTimes(correctDestTimes);
            if (correctDur.isZero() || correctDur.isNegative()) correctDur = collDur;

            InfoDouble dataTxRate = calcDataRate(xmittedBytes, transDur);
            InfoDouble dataRecRate = calcDataRate(receivedBytes, recepDur);
            InfoDouble dataGoodput = calcDataRate(correctBytes, correctDur);

            MetricDouble pktTxRate = calcPacketRate(xmittedPkts, transDur);
            MetricDouble pktRecRate = calcPacketRate(receivedPkts, recepDur);
            MetricDouble packetGoodput = calcPacketRate(correctPkts, correctDur);

            InfoDouble umtchDataTxRate = calcDataRate(umtchTxBytes, transDur);
            InfoDouble umtchDataRecRate = calcDataRate(umtchRecBytes, recepDur);

            MetricDouble umtchPktTxRate = calcPacketRate(umtchTxPkts, transDur);
            MetricDouble umtchPktRecRate = calcPacketRate(umtchRecPkts, recepDur);

            Ratio byteLoss = calcLoss(xmittedBytes, correctBytes);
            Ratio pktLoss = calcLoss(xmittedPkts, correctPkts);

            handleLatencies(builder, delays, collFinTime);
            handleByteLoss(builder, byteLoss, collFinTime);
            handlePacketLoss(builder, pktLoss, collFinTime);

            builder
                .setDataThroughput(dataGoodput, collFinTime)
                .setPacketThroughput(packetGoodput, collFinTime)
                .setDataTransmissionRate(dataTxRate, collFinTime)
                .setDataReceptionRate(dataRecRate, collFinTime)
                .setPacketTransmissionRate(pktTxRate, collFinTime)
                .setPacketReceptionRate(pktRecRate, collFinTime)
                .setUnmatchedDataTransmissionRate(umtchDataTxRate, collFinTime)
                .setUnmatchedDataReceptionRate(umtchDataRecRate, collFinTime)
                .setUnmatchedPacketTransmissionRate(umtchPktTxRate, collFinTime)
                .setUnmatchedPacketReceptionRate(umtchPktRecRate, collFinTime)
                .setLastRoundDuration(TimeLong.fromDuration(collDur))
                .setLastRoundTxDuration(TimeLong.fromDuration(transDur))
                .setLastRoundRxDuration(TimeLong.fromDuration(recepDur))
                .incrementNumUpdates();
        }
    }

    private static OptionalLong getByteCount( OFFlowRemoved flowRem, NodeId nodeId )
    {
        final U64 count = flowRem.getByteCount();
        if (count.equals(Fields.MAX_U64)) {
            LOG.trace("!!! flow byte count is not supported in switch {} !!!", nodeId);
            return OptionalLong.empty();
        }
        else {
            return OptionalLong.of(Fields.getSaturatedLong(count));
        }
    }

    private static OptionalLong getPacketCount( OFFlowRemoved flowRem, NodeId nodeId )
    {
        final U64 count = flowRem.getPacketCount();
        if (count.equals(Fields.MAX_U64)) {
            LOG.trace("!!! flow packet count is not supported in switch {} !!!", nodeId);
            return OptionalLong.empty();
        }
        else {
            return OptionalLong.of(Fields.getSaturatedLong(count));
        }
    }

    private static InfoDouble calcDataRate( OptionalLong opBytes, Duration dur )
    {
        return opBytes.isPresent() ? calcDataRate(opBytes.getAsLong(), dur)
                                   : InfoDouble.absent();
    }

    private static MetricDouble calcPacketRate( OptionalLong opPackets, Duration dur )
    {
        return opPackets.isPresent() ? calcPacketRate(opPackets.getAsLong(), dur)
                                     : MetricDouble.absent();
    }

    private static InfoDouble calcDataRate( long bytes, Duration dur )
    {
        if (dur.isNegative() || dur.isZero()) {
            return InfoDouble.absent();
        }
        else {
            final double secs = TimeDoubleUnit.NANOSECONDS.toSeconds(dur.toNanos());
            return InfoDouble.ofBytes(bytes / secs);
        }
    }

    private static MetricDouble calcPacketRate( long packets, Duration dur )
    {
        if (dur.isNegative() || dur.isZero()) {
            return MetricDouble.absent();
        }
        else {
            final double secs = TimeDoubleUnit.NANOSECONDS.toSeconds(dur.toNanos());
            return MetricDouble.ofUnits(packets / secs);
        }
    }

    private static Ratio calcLoss( long xmitted, long correct )
    {
        long lost = xmitted - correct;
        return Ratio.of(lost, xmitted);
    }

    private static Duration getTightDurationFromSummaries( List<TimedPacketSummary> pktSumms )
    {
        if (pktSumms.size() < 2) {
            return Duration.ZERO;
        }
        else {
            Instant first = pktSumms.get(0).timestamp();
            Instant last = pktSumms.get(pktSumms.size() - 1).timestamp();
            return Duration.between(first, last);
        }
    }

    private static Duration getTightDurationFromTimes( List<Instant> times )
    {
        if (times.size() < 2) {
            return Duration.ZERO;
        }
        else {
            Instant first = times.get(0);
            Instant last = times.get(times.size() - 1);
            return Duration.between(first, last);
        }
    }

    private static void handleLatency( LatencyStatsBuilder<?, ?> builder, TimeDouble latency, Instant timestamp )
    {
        if (latency.isPresent())
            builder.collectLatency(latency, timestamp);
        else
            builder.resetLatency(timestamp);
    }

    // NOTE: requires latencies that are all present
    private static void handleLatencies( LatencyStatsBuilder<?, ?> builder,
                                         List<TimeDouble> latencies,
                                         Instant timestamp )
    {
        if (!latencies.isEmpty())
            builder.collectLatencies(latencies, timestamp);
        else
            builder.resetLatency(timestamp);
    }

    private static void handleByteLoss( LossStatsBuilder<?, ?> builder, Ratio byteLoss, Instant timestamp )
    {
        if (!byteLoss.isNaN())
            builder.collectByteLoss(byteLoss, timestamp);
        else
            builder.resetByteLoss(timestamp);
    }

    private static void handlePacketLoss( LossStatsBuilder<?, ?> builder, Ratio pktLoss, Instant timestamp )
    {
        if (!pktLoss.isNaN())
            builder.collectPacketLoss(pktLoss, timestamp);
        else
            builder.resetPacketLoss(timestamp);
    }

    // private static Duration getSpannedDuration( List<Instant> srcTimed,
    // List<Instant> destTimed )
    // {
    // if (srcTimed.isEmpty() || destTimed.isEmpty()) {
    // return Duration.ZERO;
    // }
    // else {
    // Instant first = srcTimed.get(0);
    // Instant last = destTimed.get(destTimed.size() - 1);
    // return Duration.between(first, last);
    // }
    // }

    private static final class TrajPacketTimes
    {
        private final List<Instant> srcTimes;
        private final List<Instant> destTimes;

        private TrajPacketTimes()
        {
            this.srcTimes = new ArrayList<>();
            this.destTimes = new ArrayList<>();
        }

        void addSourceTime( Instant time )
        {
            srcTimes.add(time);
        }

        void addDestTime( Instant time )
        {
            destTimes.add(time);
        }

        List<Instant> getSourceTimes()
        {
            return srcTimes;
        }

        List<Instant> getDestTimes()
        {
            return destTimes;
        }
    }

    // private static TimeLong getDuration( OFFlowRemoved flowRem,
    // DatapathId
    // swID, Logger log )
    // {
    // long secs = roundedLong(flowRem.getDurationSec());
    // long nanos = flowRem.getDurationNsec();
    // if (U64.ofRaw(nanos).equals(Fields.MAX_U64)) {
    // log.trace("!! only seconds-precision flow duration is supported in switch
    // {} !!", swID);
    // return TimeLong.of(secs, TimeUnit.SECONDS);
    // }
    // else {
    // Preconditions.checkArgument(
    // (0 <= nanos && nanos < TimeUnit.SECONDS.toNanos(1)),
    // "unexpected nanosecond flow duration value of %s from switch %s",
    // nanos,
    // swID);
    //
    // long totalNanos = roundedLong(TimeUnit.SECONDS.toNanos(secs) + nanos);
    // return TimeLong.ofNanos(totalNanos);
    // }
    // }
    //
    // private static long roundedLong( long value )
    // {
    // return Fields.getRoundedLong(U64.ofRaw(value));
    // }

    private StatsCalculator()
    {
        // not used
    }
}
