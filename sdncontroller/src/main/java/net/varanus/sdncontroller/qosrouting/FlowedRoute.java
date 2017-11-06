package net.varanus.sdncontroller.qosrouting;


import static net.varanus.sdncontroller.util.stats.Stat.StringFormat.VALUE_TYPE;
import static net.varanus.sdncontroller.util.stats.StatType.SAFE;
import static net.varanus.sdncontroller.util.stats.StatType.UNSAFE;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Spliterator;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.hash.Funnel;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;

import net.varanus.sdncontroller.linkstats.FlowedLinkStats;
import net.varanus.sdncontroller.types.DatapathHop;
import net.varanus.sdncontroller.types.DatapathLink;
import net.varanus.sdncontroller.types.DatapathPath;
import net.varanus.sdncontroller.types.FlowedConnection;
import net.varanus.sdncontroller.types.FlowedLink;
import net.varanus.sdncontroller.types.FlowedPath;
import net.varanus.sdncontroller.util.Ratio;
import net.varanus.sdncontroller.util.RatioSummary;
import net.varanus.sdncontroller.util.TimeSummary;
import net.varanus.sdncontroller.util.stats.Stat;
import net.varanus.sdncontroller.util.stats.Stat.StringFormat;
import net.varanus.sdncontroller.util.stats.StatValuePrinters;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.collect.CollectionUtils;
import net.varanus.util.collect.builder.ImmutableListBuilder;
import net.varanus.util.functional.Possible;
import net.varanus.util.functional.StreamUtils;
import net.varanus.util.openflow.types.Flow;
import net.varanus.util.openflow.types.PortId;
import net.varanus.util.text.StringUtils;
import net.varanus.util.time.TimeDouble;
import net.varanus.util.unitvalue.si.InfoDouble;
import net.varanus.util.unitvalue.si.InfoLongUnit;


/**
 * A sequence of flowed-links with overall statistics combining each individual
 * flowed link statistics.
 */
@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class FlowedRoute implements Iterable<FlowedLinkStats>
{
    /**
     * Returns a new builder for flowed-routes.
     * 
     * @param entryPortId
     *            The entry port of the route path
     * @param exitPortId
     *            The exit port of the route path
     * @param flow
     *            The flow associated to the route
     * @return a new builder for {@code FlowedRoute} instances
     */
    public static Builder newBuilder( PortId entryPortId, PortId exitPortId, Flow flow )
    {
        return new Builder(
            Objects.requireNonNull(entryPortId),
            Objects.requireNonNull(exitPortId),
            Objects.requireNonNull(flow));
    }

    /**
     * Returns a flowed-route with absent statistics and a weight of zero.
     * 
     * @param path
     *            The flowed-path associated to the route
     * @return a new {@code FlowedRoute} instance with absent statistics and a
     *         weight of zero
     */
    public static FlowedRoute noStats( FlowedPath path )
    {
        return new FlowedRoute(Objects.requireNonNull(path));
    }

    private final FlowedPath                     path;
    private final ImmutableList<FlowedLinkStats> linkStats;
    private final double                         weight;

    private final Stat<TimeDouble>      latency;
    private final Stat<Possible<Ratio>> byteLoss;
    private final Stat<InfoDouble>      throughput;
    private final Stat<InfoDouble>      dataCapacity;

    private FlowedRoute( FlowedPath path )
    {
        this(path, StreamUtils.toImmutableList(path.getLinks().map(FlowedLinkStats::absent)), 0);
    }

    private FlowedRoute( FlowedPath path, ImmutableList<FlowedLinkStats> linkStats, double weight )
    {
        Preconditions.checkArgument(path.numberOfLinks() == linkStats.size(),
            "number of flowed-link statistics must match number of links in flowed-path");

        this.path = path;
        this.linkStats = linkStats;
        this.weight = weight;

        if (linkStats.isEmpty()) {
            Instant timestamp = Instant.now();
            this.latency = Stat.of(TimeDouble.absent(), timestamp, UNSAFE);
            this.byteLoss = Stat.of(Possible.absent(), timestamp, UNSAFE);
            this.throughput = Stat.of(InfoDouble.absent(), timestamp, UNSAFE, StatValuePrinters::dataPerSecond);
            this.dataCapacity = Stat.of(InfoDouble.absent(), timestamp, UNSAFE, StatValuePrinters::dataPerSecond);
        }
        else {
            Stat.Builder<TimeDouble> totalLat = Stat.newBuilder(TimeDouble.ZERO, Instant.MIN, SAFE);

            Stat.Builder<Possible<Ratio>> totalSucc =
                Stat.newBuilder(Possible.of(Ratio.ONE), Instant.MIN, SAFE, StatValuePrinters::ratio);

            Stat.Builder<InfoDouble> maxThr =
                Stat.newBuilder(InfoDouble.ZERO, Instant.MIN, SAFE, StatValuePrinters::dataPerSecond);

            Stat.Builder<InfoDouble> minCap =
                Stat.newBuilder(InfoDouble.ofBits(Double.POSITIVE_INFINITY), Instant.MIN, SAFE,
                    StatValuePrinters::dataPerSecond);

            for (FlowedLinkStats stats : linkStats) {
                totalLat = totalLat.combineWith(meanLatStat(stats.getLatency()), TimeDouble::plus);
                totalSucc = totalSucc.combineWith(oneMinus(meanLossStat(stats.getByteLoss())), FlowedRoute::times);
                maxThr = maxThr.combineWith(stats.getThroughput(), InfoDouble::maxPresent);
                minCap = minCap.combineWith(stats.getDataCapacity(), InfoDouble::minPresent);
            }

            this.latency = totalLat.build();
            this.byteLoss = totalSucc.apply(FlowedRoute::oneMinus).build();
            this.throughput = maxThr.build();
            this.dataCapacity = minCap.build();
        }
    }

    /**
     * Returns the flowed-path associated to this route.
     * 
     * @return a {@code FlowedPath} object
     */
    public FlowedPath getPath()
    {
        return path;
    }

    /**
     * Returns a hash of the flowed-path associated to this route.
     * 
     * @return an {@code long} value
     */
    public long getPathId()
    {
        Hasher hasher = PATH_ID_HASH_FUNCTION.newHasher();
        path.getLinks().forEachOrdered(link -> hasher.putObject(link, FLOWED_LINK_FUNNEL));
        return hasher.hash().asLong();
    }

    private static final HashFunction       PATH_ID_HASH_FUNCTION = Hashing.sha1();
    private static final Funnel<FlowedLink> FLOWED_LINK_FUNNEL    = ( link, sink ) -> {
                                                                      sink.putLong(link.getSrcNode().getLong());
                                                                      sink.putInt(link.getSrcPort().getPortNumber());
                                                                      sink.putLong(link.getDestNode().getLong());
                                                                      sink.putInt(link.getDestPort().getPortNumber());
                                                                  };

    /**
     * Returns the flowed-connection associated to this route.
     * 
     * @return a {@code FlowedConnection} object
     */
    public FlowedConnection getConnection()
    {
        return path.getConnection();
    }

    /**
     * Returns the flow associated to this route.
     * 
     * @return a {@code Flow} object
     */
    public Flow getFlow()
    {
        return path.getFlow();
    }

    /**
     * Returns the number of flowed-links in this route.
     * 
     * @return an {@code int} value
     */
    public int getLength()
    {
        return linkStats.size();
    }

    /**
     * Returns the weight of this route.
     * 
     * @return a {@code double} value
     */
    public double getWeight()
    {
        return weight;
    }

    /**
     * Returns the statistics for the flowed-link at the specified index in this
     * route.
     * 
     * @param index
     *            The zero-based position of the requested flowed-link
     * @return a {@code FlowedLinkStats} object
     * @exception IndexOutOfBoundsException
     *                If the index is out of range
     *                ({@code index < 0 || index >= getLength()})
     */
    public FlowedLinkStats getLinkStats( int index )
    {
        return linkStats.get(index);
    }

    /**
     * Returns the sum of the latencies of all flowed-links or an absent value
     * if not all flowed-links have known latencies.
     * 
     * @return a {@code Stat<TimeDouble>} value
     */
    public Stat<TimeDouble> getLatency()
    {
        return latency;
    }

    /**
     * Returns the combined byte loss of all flowed-links or an absent value if
     * not all flowed-links have known byte losses.
     * <p>
     * More specifically, the returned value (if present) will be equal to
     * one minus {@code SR}, where {@code SR} is the sum of the "success ratios"
     * of all flowed-links, and each success ratio is defined as one minus the
     * byte loss of the flowed-link.
     * 
     * @return a {@code Stat<Possible<Ratio>>} value
     */
    public Stat<Possible<Ratio>> getByteLoss()
    {
        return byteLoss;
    }

    /**
     * Returns the maximum throughput of all flowed-links or an absent value if
     * not all flowed-links have known throughputs.
     * 
     * @return a {@code Stat<InfoDouble>} value
     */
    public Stat<InfoDouble> getThroughput()
    {
        return throughput;
    }

    /**
     * Returns the minimum data capacity of all flowed-links or an absent value
     * if not all flowed-links have known capacities.
     * 
     * @return a {@code Stat<InfoDouble>} value
     */
    public Stat<InfoDouble> getDataCapacity()
    {
        return dataCapacity;
    }

    /**
     * Returns the difference between the data capacity and the throughput of
     * the route.
     * 
     * @return a {@code Stat<InfoDouble>} value
     */
    public Stat<InfoDouble> getAvailableBandwidth()
    {
        BinaryOperator<InfoDouble> combiner = ( capa, used ) -> capa.posDiff(used);

        return Stat.newBuilder(dataCapacity)
            .combineWith(throughput, combiner)
            .build();
    }

    /**
     * Returns the proportion of the throughput relative to the data capacity of
     * the route.
     * 
     * @return a {@code Stat<Possible<Ratio>>} value
     */
    public Stat<Possible<Ratio>> getDataUtilization()
    {
        BiFunction<InfoDouble, InfoDouble, Possible<Ratio>> mixer =
            ( capa, used ) -> used.asLong().coMap(capa.asLong(), InfoLongUnit.BITS,
                ( usedBits, capaBits ) -> Ratio.of(usedBits, capaBits));

        return Stat.newBuilder(dataCapacity)
            .mixWith(throughput, mixer, StatValuePrinters::ratio)
            .build();
    }

    @Override
    public ListIterator<FlowedLinkStats> iterator()
    {
        return linkStats.listIterator();
    }

    @Override
    public Spliterator<FlowedLinkStats> spliterator()
    {
        return linkStats.spliterator();
    }

    @Override
    public void forEach( Consumer<? super FlowedLinkStats> action )
    {
        linkStats.forEach(action);
    }

    public Stream<FlowedLinkStats> streamStats()
    {
        return linkStats.stream();
    }

    /**
     * Indicates whether this route has the same flowed-path as the provided
     * route.
     * 
     * @param other
     *            A {@code FlowedRoute} instance
     * @return {@code true} iff this route has the same flowed-path as the
     *         provided route
     */
    public boolean hasSamePath( FlowedRoute other )
    {
        return this.path.equals(other.path);
    }

    /**
     * Indicates whether this route has the same core statistics as the provided
     * route.
     * <p>
     * The core statistics are:
     * <ul>
     * <li>{@linkplain #getLatency() latency}</li>
     * <li>{@linkplain #getByteLoss() byte loss}</li>
     * <li>{@linkplain #getThroughput() throughput}</li>
     * <li>{@linkplain #getDataCapacity() data capacity}</li>
     * </ul>
     * 
     * @param other
     *            A {@code FlowedRoute} instance
     * @return {@code true} iff this route has the same core statistics as the
     *         provided route
     */
    public boolean hasSameCoreStats( FlowedRoute other )
    {
        return this.latency.equals(other.latency)
               && this.byteLoss.equals(other.byteLoss)
               && this.throughput.equals(other.throughput)
               && this.dataCapacity.equals(other.dataCapacity);
    }

    /**
     * Indicates whether this route has the same
     * {@linkplain FlowedLinkStats#hasSameCoreStats(FlowedLinkStats) flowed-link
     * core statistics} as the provided route.
     * 
     * @param other
     *            A {@code FlowedRoute} instance
     * @return {@code true} iff this route has the same flowed-link core
     *         statistics as the provided route
     */
    public boolean hasSameLinkCoreStats( FlowedRoute other )
    {
        return CollectionUtils.equalsBy(this.linkStats, other.linkStats,
            FlowedLinkStats::hasSameCoreStats);
    }

    @Override
    public boolean equals( Object other )
    {
        return (other instanceof FlowedRoute)
               && this.equals((FlowedRoute)other);
    }

    public boolean equals( FlowedRoute other )
    {
        return (other != null)
               && this.linkStats.equals(other.linkStats);
    }

    @Override
    public int hashCode()
    {
        return linkStats.hashCode();
    }

    @Override
    public String toString()
    {
        return StringUtils.joinAllPS(", ", "FlowedRoute{", "}",
            "path_id=" + Long.toHexString(getPathId()),
            "flow=" + getFlow(),
            "connection=" + getConnection().unflowed(),
            "weight=" + weight,
            "latency=" + latency.toString(StringFormat.VALUE_TYPE),
            "byte_loss=" + byteLoss.toString(StringFormat.VALUE_TYPE),
            "throughput=" + throughput.toString(StringFormat.VALUE_TYPE),
            "data_capacity=" + dataCapacity.toString(StringFormat.VALUE_TYPE),
            "available_bandwidth=" + getAvailableBandwidth().toString(StringFormat.VALUE_TYPE),
            "data_utilization=" + getDataUtilization().toString(StringFormat.VALUE_TYPE),
            "link_stats=" + linkStats);
    }

    public String toPrettyString()
    {
        return StringUtils.joinAllInLines(
            "===================== Flowed-route =============================================",
            "Path ID             : " + Long.toHexString(getPathId()),
            "Flow                : " + getFlow(),
            "Connection          : " + getConnection().unflowed(),
            "Weight              : " + String.format("%.3f", weight),
            "--------------------------------------------------------------------------------",
            "Latency             : " + latency.toString(StringFormat.VALUE_TYPE),
            "Byte loss           : " + byteLoss.toString(StringFormat.VALUE_TYPE),
            "Throughput          : " + throughput.toString(StringFormat.VALUE_TYPE),
            "Data capacity       : " + dataCapacity.toString(StringFormat.VALUE_TYPE),
            "Available bandwidth : " + getAvailableBandwidth().toString(StringFormat.VALUE_TYPE),
            "Data utilization    : " + getDataUtilization().toString(StringFormat.VALUE_TYPE),
            "--------------------------------------------------------------------------------",
            pathToPrettyString(),
            "================================================================================");
    }

    private String pathToPrettyString()
    {
        if (this.getLength() == 0) {
            // numberOfHops == 1; numberOfLinks == 0
            return path.unflowed().getHops().get(0).toString();
        }
        else {
            // numberOfHops > 1; numberOfLinks == numberOfHops - 1
            List<List<String>> columns = genPathColumns();
            List<Integer> maxLens = getColumnMaxLengths(columns);

            // to write multiple columns to a multi-line string, we must
            // "traverse" the columns by rows (i.e. transposing the columns)
            StringBuilder sb = new StringBuilder();
            StreamUtils.zipIterables(columns).forEachOrdered(
                ( row ) -> {
                    int i = 0;
                    for (String cell : row) {
                        sb.append(StringUtils.leftJust(cell, maxLens.get(i)));
                        i++;
                    }
                    sb.append(System.lineSeparator());
                });

            return sb.toString();
        }
    }

    private List<List<String>> genPathColumns()
    {
        List<DatapathHop> hops = path.unflowed().getHops();

        List<List<String>> columns = new ArrayList<>(hops.size() + linkStats.size());
        StreamUtils.zipRun(hops.stream(), linkStats.stream(),
            // traverse all hops except the last and all the link stats
            ( hop, stats ) -> {
                columns.add(genHopLines(hop));
                columns.add(getLinkStatsLines(stats));
            });

        // append the last hop
        columns.add(genHopLines(hops.get(hops.size() - 1)));
        return columns;
    }

    private static List<Integer> getColumnMaxLengths( List<List<String>> columns )
    {
        return columns.stream()
            .mapToInt(column -> column.stream()
                .mapToInt(String::length)
                .max().orElseThrow(AssertionError::new))
            .boxed()
            .collect(Collectors.toList());
    }

    private static List<String> genHopLines( DatapathHop hop )
    {
        return Arrays.asList(
            "",
            "",
            "",
            hop.toString() + " ");
    }

    private static List<String> getLinkStatsLines( FlowedLinkStats stats )
    {
        return Arrays.asList(
            getLatencyLine(stats),
            getLossRateLine(stats),
            getThroughputLine(stats),
            "---------------> ");
    }

    private static String getLatencyLine( FlowedLinkStats stats )
    {
        // TODO maybe reduce value precision here
        Stat<TimeDouble> meanLat = meanLatStat(stats.getLatency());
        return meanLat.toString(VALUE_TYPE);
    }

    private static String getLossRateLine( FlowedLinkStats stats )
    {
        Stat<Possible<Ratio>> meanLoss = meanLossStat(stats.getByteLoss());
        return meanLoss.with(pr -> pr.toString(r -> r.toPercentageString(2))).toString(VALUE_TYPE);
    }

    private static String getThroughputLine( FlowedLinkStats stats )
    {
        Stat<InfoDouble> thrpt = stats.getThroughput();
        return thrpt.with(info -> info.toBitStringAnd(3, "/s")).toString(VALUE_TYPE);
    }

    /**
     * A builder for flowed-routes.
     */
    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class Builder
    {
        private final Flow                                  flow;
        private final DatapathPath.LinkBuilder              pathBuilder;
        private final ImmutableListBuilder<FlowedLinkStats> statsBuilder;
        private double                                      weight;

        private Builder( PortId entryPortId, PortId exitPortId, Flow flow )
        {
            this.flow = flow;
            this.pathBuilder = DatapathPath.newBuilderFromLinks(entryPortId, exitPortId);
            this.statsBuilder = ImmutableListBuilder.create();
            this.weight = 0;
        }

        public Builder addLinkWithStats( FlowedLinkStats stats, double weight )
        {
            Preconditions.checkArgument(stats.getLink().getFlow().equals(flow),
                "stats must be associated with a flowed-link with flow %s", flow);
            pathBuilder.addLink(stats.getLink().unflowed());
            statsBuilder.add(stats);
            this.weight += weight;
            return this;
        }

        public Builder addLinkNoStats( DatapathLink link )
        {
            pathBuilder.addLink(link);
            statsBuilder.add(FlowedLinkStats.absent(link.flowed(flow)));
            return this;
        }

        public Builder addLinkNoStats( FlowedLink link )
        {
            Preconditions.checkArgument(link.getFlow().equals(flow),
                "flowed-link must have the flow %s", flow);
            return addLinkNoStats(link.unflowed());
        }

        public Builder clear()
        {
            this.pathBuilder.clear();
            this.statsBuilder.clear();
            return this;
        }

        public FlowedRoute build()
        {
            return new FlowedRoute(
                FlowedPath.of(pathBuilder.build(), flow),
                statsBuilder.build(),
                weight);
        }
    }

    private static Stat<TimeDouble> meanLatStat( Stat<TimeSummary> stat )
    {
        return stat.mapSameTime(summ -> summ.isPresent() ? summ.getMean() : TimeDouble.absent());
    }

    private static Stat<Possible<Ratio>> meanLossStat( Stat<RatioSummary> stat )
    {
        return stat.mapSameTime(summ -> summ.isPresent() ? Possible.of(summ.getMean()) : Possible.absent());
    }

    private static Stat<Possible<Ratio>> oneMinus( Stat<Possible<Ratio>> stat )
    {
        return stat.transformSameTime(FlowedRoute::oneMinus);
    }

    private static Possible<Ratio> oneMinus( Possible<Ratio> ratio )
    {
        return ratio.map(r -> Ratio.ONE.minus(r));
    }

    private static Possible<Ratio> times( Possible<Ratio> ratio1, Possible<Ratio> ratio2 )
    {
        return ratio1.combine(ratio2, ( r1, r2 ) -> r1.times(r2));
    }
}
