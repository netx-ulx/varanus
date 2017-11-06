package net.varanus.mirroringprotocol;


import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import javax.annotation.ParametersAreNonnullByDefault;

import org.projectfloodlight.openflow.types.DatapathId;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableSet;

import net.varanus.mirroringprotocol.util.CollectorId;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.io.Serializers;
import net.varanus.util.io.serializer.ChannelReader;
import net.varanus.util.io.serializer.ChannelWriter;
import net.varanus.util.lang.MoreObjects;
import net.varanus.util.openflow.types.NodeId;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class CollectorConnection
{
    private final CollectorId          collectorId;
    private final ImmutableSet<NodeId> suppSwitches;

    public CollectorConnection( CollectorId collectorId, ImmutableSet<NodeId> suppSwitches )
    {
        this.collectorId = Objects.requireNonNull(collectorId);
        this.suppSwitches = Objects.requireNonNull(suppSwitches);
    }

    public CollectorId getCollectorId()
    {
        return collectorId;
    }

    public ImmutableSet<NodeId> getSupportedSwitches()
    {
        return suppSwitches;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{ collector_id = ").append(collectorId);
        sb.append("; supp_switches = ").append(suppSwitches);
        sb.append(" }");
        return sb.toString();
    }

    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class IO
    {
        public static ChannelWriter<CollectorConnection> writer( Logger log )
        {
            Objects.requireNonNull(log);
            return ( conn, ch ) -> {
                final CollectorId collectorId = conn.getCollectorId();
                final Set<NodeId> suppSwitches = conn.getSupportedSwitches();

                log.debug("Writing from collector {}: the collector ID", collectorId);
                CollectorId.IO.writer().write(collectorId, ch);

                log.debug("Writing from collector {}: supported switches {}", collectorId, suppSwitches);
                Serializers.colWriter(NodeId.IO.writer()).write(suppSwitches, ch);
            };
        }

        public static ChannelReader<CollectorConnection> reader( Function<DatapathId, String> idAliaser, Logger log )
        {
            MoreObjects.requireNonNull(idAliaser, "idAliaser", log, "log");
            return ( ch ) -> {
                final CollectorId collectorId = CollectorId.IO.reader().read(ch);
                log.debug("Read from collector {}: the collector ID", collectorId);

                final ImmutableSet<NodeId> suppSwitches =
                    Serializers.immuSetReader(NodeId.IO.reader(idAliaser)).read(ch);
                log.debug("Read from collector {}: supported switches {}", collectorId, suppSwitches);

                return new CollectorConnection(collectorId, suppSwitches);
            };
        }

        private IO()
        {
            // not used
        }
    }
}
