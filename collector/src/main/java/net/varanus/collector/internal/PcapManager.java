package net.varanus.collector.internal;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.annotation.CheckForNull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.jnetpcap.ByteBufferHandler;
import org.jnetpcap.Pcap;
import org.jnetpcap.PcapHeader;
import org.jnetpcap.PcapIf;
import org.jnetpcap.PcapTask;
import org.jnetpcap.PcapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.varanus.mirroringprotocol.GenericRequest;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.concurrent.InterruptibleRunnable;
import net.varanus.util.openflow.types.BitMatch;
import net.varanus.util.openflow.types.NodeId;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
final class PcapManager
{
    private static final int PCAP_READ_TIMEOUT_MILLIS = 10;
    private static final int PCAP_SNAP_LENGTH         = Pcap.DEFAULT_SNAPLEN;
    private static final int PCAP_SET_PROMISCUOUS     = Pcap.MODE_PROMISCUOUS;

    private static final Logger LOG = LoggerFactory.getLogger(PcapManager.class);

    private final Map<NodeId, String>                switchToIfaceMap;
    private final Map<String, PcapTask<PcapContext>> ifaceToPcapTaskMap;
    private final boolean                            ignoreBSNPackets;

    PcapManager( Map<NodeId, String> switchToIfaceMap, boolean ignoreBSNPackets )
    {
        this.switchToIfaceMap = Objects.requireNonNull(switchToIfaceMap);
        this.ifaceToPcapTaskMap = new LinkedHashMap<>();
        this.ignoreBSNPackets = ignoreBSNPackets;
    }

    void start() throws IOException
    {
        // XXX !!! HACK !!! XXX
        LOG.warn("==============================================================");
        LOG.warn(">>>>>>>>>>>>>> PROBE FILTER HACK IS ENABLED <<<<<<<<<<<<<<<<<<");
        LOG.warn("==============================================================");
        // XXX !!! HACK !!! XXX

        LOG.debug("-- Starting pcap tasks --");

        // only do things if there is at least one switch_to_interface entry
        if (!switchToIfaceMap.isEmpty()) {
            List<PcapIf> availableDevs = new ArrayList<>();
            StringBuilder errBuf = new StringBuilder();

            // find all available devices that allow sniffing with pcap
            if (Pcap.findAllDevs(availableDevs, errBuf) == Pcap.ERROR) {
                throw new IOException(
                    String.format("error while finding available devices to sniff with pcap: %s",
                        errBuf));
            }

            // verify if requested interfaces are all available for sniffing
            // with pcap
            for (String iface : switchToIfaceMap.values()) {
                boolean found = false;
                for (PcapIf pcapIf : availableDevs) {
                    if (iface.equals(pcapIf.getName())) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    throw new IOException(
                        String.format("requested network interface %s cannot be sniffed with pcap (maybe run as root?)",
                            iface));
                }
            }

            // open all pcaps associated to each requested interface and start
            // their sniffing tasks (on error shutdown all previously started
            // tasks)
            Map<String, PcapTask<PcapContext>> tasksByIface = new LinkedHashMap<>();
            try {
                for (String iface : switchToIfaceMap.values()) {
                    // only run one task per interface
                    if (!tasksByIface.containsKey(iface)) {
                        LOG.trace("Opening pcap on interface {}", iface);

                        errBuf.setLength(0); // reset the builder
                        Pcap pcap = Pcap.openLive(
                            iface,
                            PCAP_SNAP_LENGTH,
                            PCAP_SET_PROMISCUOUS,
                            PCAP_READ_TIMEOUT_MILLIS,
                            errBuf);

                        if (pcap == null) {
                            throw new IOException(
                                String.format("error while opening pcap on interface %s: %s",
                                    iface,
                                    errBuf));
                        }
                        else {
                            if (errBuf.length() > 0) {
                                // pcap was opened successfully, but it
                                // generated a warning
                                LOG.warn("!! Warning while opening pcap on interface {}: {}",
                                    iface,
                                    errBuf);
                            }

                            PacketHandler handler = new PacketHandler(ignoreBSNPackets);
                            PcapContext ctxt = new PcapContext(iface);
                            PcapTask<PcapContext> task = startNewPcapTask(pcap, handler, ctxt);
                            tasksByIface.put(iface, task);
                        }
                    }
                }
            }
            catch (Throwable t) {
                // an error occured, shutdown all previous pcap tasks and then
                // re-throw the exception
                for (PcapTask<PcapContext> task : tasksByIface.values()) {
                    try {
                        shutdownPcapTask(task);
                    }
                    catch (Throwable suppr) {
                        t.addSuppressed(suppr);
                    }
                }
                throw t;
            }

            this.ifaceToPcapTaskMap.putAll(tasksByIface);
        }

        if (ifaceToPcapTaskMap.isEmpty()) {
            LOG.debug("-- No interfaces configured for capturing packets --");
        }
        else {
            for (String iface : ifaceToPcapTaskMap.keySet()) {
                LOG.debug("-- Capturing packets on interface {} --", iface);
                LOG.debug("-- {} BSN packets --", (ignoreBSNPackets ? "Ignoring" : "Allowing"));
            }
        }
    }

    @CheckForNull
    CollectionHandle acquireCollectionHandle( GenericRequest request )
    {
        String iface = switchToIfaceMap.get(getSwitchId(request));
        if (iface != null) {
            PcapTask<PcapContext> task = ifaceToPcapTaskMap.get(iface);
            if (task != null) {
                return task.getUser().registerHandle(getBitMatch(request));
            }
        }
        return null;
    }

    void releaseCollectionHandle( GenericRequest request )
    {
        String iface = switchToIfaceMap.get(getSwitchId(request));
        if (iface != null) {
            PcapTask<PcapContext> task = ifaceToPcapTaskMap.get(iface);
            if (task != null) {
                task.getUser().unregisterHandle(getBitMatch(request));
            }
        }
    }

    private static NodeId getSwitchId( GenericRequest req )
    {
        switch (req.getType()) {
            case SAMPLING:
                return req.forSampling().getSwitchPort().getNodeId();

            case PROBING:
                return req.forProbing().getSwitchPort().getNodeId();

            default:
                throw new AssertionError("unexpected enum value");
        }
    }

    private static BitMatch getBitMatch( GenericRequest req )
    {
        switch (req.getType()) {
            case SAMPLING:
                return req.forSampling().getFlow().getBitMatch();

            case PROBING:
                return req.forProbing().getBitMatch();

            default:
                throw new AssertionError("unexpected enum value");
        }
    }

    void shutdown()
    {
        LOG.debug("-- Shutting down {} pcap tasks --", ifaceToPcapTaskMap.size());
        for (PcapTask<PcapContext> task : ifaceToPcapTaskMap.values()) {
            shutdownPcapTask(task);
        }
    }

    private static PcapTask<PcapContext> startNewPcapTask( Pcap pcap, PacketHandler handler, PcapContext ctxt )
        throws IOException
    {
        PcapTask<PcapContext> task = PcapUtils.loopInBackground(
            pcap,
            Pcap.LOOP_INFINITE,
            handler,
            ctxt);

        try {
            task.start();
            return task;
        }
        catch (InterruptedException e) {
            // before throwing the exception, we must shutdown the new task
            shutdownPcapTask(task);
            throw new IOException(
                String.format("interrupted while starting a pcap task for sniffing %s",
                    ctxt.getIface()));
        }
    }

    // stops a sniffing task and closes the pcap
    private static void shutdownPcapTask( PcapTask<PcapContext> task )
    {
        try {
            Thread stopper = new Thread((InterruptibleRunnable)() -> task.stop());
            stopper.start();
            TimeUnit.SECONDS.timedJoin(stopper, 1);
        }
        catch (InterruptedException e) {
            LOG.error("!!! Interrupted while stopping a pcap task for sniffing {}",
                task.getUser().getIface());
        }

        if (task.getResult() == Pcap.ERROR) {
            LOG.error("!!! pcap task for sniffing {} returned an error on shutdown: {}",
                task.getUser().getIface(),
                task.getPcap().getErr());
        }

        LOG.trace("Shutting down pcap task for interface {}", task.getUser().getIface());
        task.getPcap().close();
        LOG.trace("Pcap task for interface {} has shut down", task.getUser().getIface());
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    private static final class PcapContext
    {
        private final String            iface;
        private final CollectionManager collMngr;

        PcapContext( String iface )
        {
            this.iface = Objects.requireNonNull(iface);
            this.collMngr = new CollectionManager();
        }

        String getIface()
        {
            return iface;
        }

        CollectionHandle registerHandle( BitMatch match )
        {
            return collMngr.registerHandle(match, LOG);
        }

        void unregisterHandle( BitMatch match )
        {
            collMngr.unregisterHandle(match);
        }

        // to be called only by a packet capturer thread
        void collectPacket( PcapHeader header, ByteBuffer packet )
        {
            collMngr.collectPacket(new CapturedPacket(header, packet));
        }
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    private static final class PacketHandler implements ByteBufferHandler<PcapContext>
    {
        private final boolean ignoreBSNPackets;

        PacketHandler( boolean ignoreBSNPackets )
        {
            this.ignoreBSNPackets = ignoreBSNPackets;
        }

        @Override
        public void nextPacket( PcapHeader header, ByteBuffer buffer, PcapContext ctxt )
        {
            if (ignoreBSNPackets && isBSNPacket(buffer)) {
                LOG.trace("Ignored received BSN packet");
            }
            else {
                ctxt.collectPacket(header, buffer);
            }
        }
    }

    private static final int SIZE_OF_MAC_ADDRESS = 6;
    private static final int ETH_TYPE_OFFSET     = 2 * SIZE_OF_MAC_ADDRESS;
    private static final int TYPE_BSN            = 0x8942;

    private static boolean isBSNPacket( ByteBuffer packet )
    {
        int ethTypeVal = packet.getShort(ETH_TYPE_OFFSET) & 0xFFFF;
        return (ethTypeVal == TYPE_BSN);
    }
}
