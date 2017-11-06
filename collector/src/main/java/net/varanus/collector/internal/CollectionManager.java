package net.varanus.collector.internal;


import java.util.HashMap;
import java.util.Map;

import javax.annotation.ParametersAreNonnullByDefault;

import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.TransportPort;
import org.slf4j.Logger;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.openflow.types.BitMatch;
import net.varanus.util.openflow.types.Flow;
import net.varanus.util.openflow.types.MatchEntry;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
final class CollectionManager
{
    private final Map<BitMatch, CollectionHandle> handleMap;
    private final Object                          mapLock;

    CollectionManager()
    {
        this.handleMap = new HashMap<>();
        this.mapLock = new Object();
    }

    void collectPacket( CapturedPacket packet )
    {
        synchronized (mapLock) {
            handleMap.forEach(( match, handle ) -> {
                if (match.matchesPacket(packet.getPacket()))
                    handle.collectMatched(packet);
                else if (!isFiltered(packet))// XXX !!! HACK!!! XXX
                    handle.logUnmatched(packet);
            });
        }
    }

    // XXX !!! HACK!!! XXX
    private static final Flow SEC_PROBE_FLOW  = Flow.of(MatchEntry.ofExact(MatchField.ETH_TYPE, EthType.of(0x9000)));
    private static final Flow LLDP_PROBE_FLOW = Flow.of(MatchEntry.ofExact(MatchField.ETH_TYPE, EthType.LLDP));
    private static final Flow BSN_PROBE_FLOW  = Flow.of(MatchEntry.ofExact(MatchField.ETH_TYPE, EthType.of(0x8942)));
    private static final Flow IPV6_FLOW       = Flow.of(MatchEntry.ofExact(MatchField.ETH_TYPE, EthType.IPv6));
    private static final Flow DHCP_PROBE_FLOW = Flow.of(
        MatchEntry.ofExact(MatchField.ETH_TYPE, EthType.IPv4),
        MatchEntry.ofExact(MatchField.IP_PROTO, IpProtocol.UDP),
        MatchEntry.ofExact(MatchField.UDP_SRC, TransportPort.of(68)),
        MatchEntry.ofExact(MatchField.UDP_DST, TransportPort.of(67)));

    private static boolean isFiltered( CapturedPacket packet )
    {
        return SEC_PROBE_FLOW.getBitMatch().matchesPacket(packet.getPacket())
               || LLDP_PROBE_FLOW.getBitMatch().matchesPacket(packet.getPacket())
               || BSN_PROBE_FLOW.getBitMatch().matchesPacket(packet.getPacket())
               || IPV6_FLOW.getBitMatch().matchesPacket(packet.getPacket())
               || DHCP_PROBE_FLOW.getBitMatch().matchesPacket(packet.getPacket());
    }
    // XXX !!! HACK!!! XXX

    CollectionHandle registerHandle( BitMatch newMatch, Logger log )
    {
        synchronized (mapLock) {
            checkNewMatch(newMatch, log);
            return handleMap.computeIfAbsent(newMatch, CollectionHandle::new);
        }
    }

    void unregisterHandle( BitMatch match )
    {
        synchronized (mapLock) {
            handleMap.remove(match);
        }
    }

    private void checkNewMatch( BitMatch newMatch, Logger log )
    {
        for (BitMatch match : handleMap.keySet()) {
            if (newMatch.equals(match))
                log.warn("!! Duplicate bit match when requesting new queue: {}", newMatch);
            else if (newMatch.matchesAllOf(match))
                log.warn(
                    "!! New bit match matches all of existing match when requesting new queue: {} matches all of {}",
                    newMatch,
                    match);
            else if (match.matchesAllOf(newMatch))
                log.warn(
                    "!! Existing bit match matches all of new match when requesting new queue: {} matches all of {}",
                    match,
                    newMatch);
        }
    }
}
