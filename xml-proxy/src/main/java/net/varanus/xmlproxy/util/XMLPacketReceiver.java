package net.varanus.xmlproxy.util;


import java.nio.channels.ReadableByteChannel;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import javax.annotation.CheckForNull;
import javax.annotation.ParametersAreNonnullByDefault;

import org.slf4j.Logger;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.collect.CollectionUtils;
import net.varanus.util.concurrent.ConcurrencyUtils;
import net.varanus.util.concurrent.ConcurrentService;
import net.varanus.util.functional.Report;
import net.varanus.util.io.exception.IOChannelReadException;
import net.varanus.util.io.serializer.IOReader;
import net.varanus.util.time.TimeLong;
import net.varanus.xmlproxy.xml.XMLPacket;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class XMLPacketReceiver extends ConcurrentService
{
    private final ReadableByteChannel                           ch;
    private final Map<XMLPacket.Type, BlockingQueue<XMLPacket>> pktQueues;
    private final ExecutorService                               pktDeliverExec;

    public XMLPacketReceiver( ReadableByteChannel ch )
    {
        this(ch, ( msg, ex ) -> {
            if (msg != null)
                System.err.print(msg);
            if (ex != null)
                ex.printStackTrace(System.err);
        });
    }

    public XMLPacketReceiver( ReadableByteChannel ch, Logger log )
    {
        this(ch, ( msg, ex ) -> log.error(msg, ex));
    }

    private XMLPacketReceiver( ReadableByteChannel ch, BiConsumer<String, Throwable> exceptionLogger )
    {
        super(ConcurrencyUtils.defaultDaemonThreadFactory(), exceptionLogger);

        this.ch = Objects.requireNonNull(ch);
        this.pktQueues = initPktQueues();
        this.pktDeliverExec = Executors.newCachedThreadPool(ConcurrencyUtils.defaultDaemonThreadFactory());
    }

    private static Map<XMLPacket.Type, BlockingQueue<XMLPacket>> initPktQueues()
    {
        Map<XMLPacket.Type, BlockingQueue<XMLPacket>> map = new EnumMap<>(XMLPacket.Type.class);
        for (XMLPacket.Type t : XMLPacket.Type.values()) {
            map.put(t, new LinkedBlockingQueue<>());
        }
        return map;
    }

    public CompletableFuture<Report<XMLPacket>> receive( XMLPacket.Type type, XMLPacket.Type... moreTypes )
    {
        return receive(EnumSet.of(type, moreTypes));
    }

    public CompletableFuture<Report<XMLPacket>> receive( Set<XMLPacket.Type> pktTypes )
    {
        CompletableFuture<Report<XMLPacket>> future = new CompletableFuture<>();
        Runnable deliverer = pktTypes.size() == 1 ? deliverSingleType(future, pktTypes.iterator().next())
                                                  : deliverMultiType(future, pktTypes);
        try {
            pktDeliverExec.execute(deliverer);
        }
        catch (RejectedExecutionException e) {
            future.complete(Report.ofError("XMLPacketReceiver shut down"));
        }

        return future;
    }

    private Runnable deliverSingleType( CompletableFuture<Report<XMLPacket>> future, XMLPacket.Type pktType )
    {
        BlockingQueue<XMLPacket> queue = pktQueues.get(pktType);
        return () -> {
            try {
                future.complete(Report.of(queue.take()));
            }
            catch (InterruptedException e) {
                future.complete(Report.ofError("XMLPacketReceiver shut down"));
            }
        };
    }

    private Runnable deliverMultiType( CompletableFuture<Report<XMLPacket>> future, Set<XMLPacket.Type> pktTypes )
    {
        List<BlockingQueue<XMLPacket>> queues = CollectionUtils.toList(pktTypes, pktQueues::get);
        TimeLong sleepTime = TimeLong.of(100, TimeUnit.MILLISECONDS);
        return () -> {
            try {
                while (true) {
                    XMLPacket pkt = pollFirstAvailable(queues);
                    if (pkt != null) {
                        future.complete(Report.of(pkt));
                        break;
                    }
                    else {
                        sleepTime.sleep();
                    }
                }
            }
            catch (InterruptedException e) {
                future.complete(Report.ofError("XMLPacketReceiver shut down"));
            }
        };
    }

    private static @CheckForNull XMLPacket pollFirstAvailable( List<BlockingQueue<XMLPacket>> queues )
    {
        for (BlockingQueue<XMLPacket> q : queues) {
            XMLPacket pkt = q.poll();
            if (pkt != null)
                return pkt;
        }

        return null;
    }

    @Override
    public void runInterruptibly() throws InterruptedException
    {
        try {
            IOReader<XMLPacket> pktReader = XMLPacket.IO.reader();
            while (true) {
                XMLPacket pkt = pktReader.read(ch);
                pktQueues.get(pkt.getType()).add(pkt);
            }
        }
        catch (IOChannelReadException e) {
            e.checkInterruptStatus();
            exceptionLogger().accept(
                String.format("!!! IO-READ error in XMLPacketReceiver: %s", e.getMessage()), null);
        }
    }

    @Override
    protected void shutDown() throws Exception
    {
        pktDeliverExec.shutdownNow();
    }
}
