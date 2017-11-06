package net.varanus.xmlproxy.util;


import java.nio.channels.WritableByteChannel;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.BiConsumer;

import javax.annotation.ParametersAreNonnullByDefault;

import org.slf4j.Logger;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.concurrent.ConcurrencyUtils;
import net.varanus.util.concurrent.ConcurrentService;
import net.varanus.util.io.exception.IOChannelWriteException;
import net.varanus.util.io.serializer.IOWriter;
import net.varanus.xmlproxy.xml.XMLPacket;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class XMLPacketSender extends ConcurrentService
{
    private final WritableByteChannel      ch;
    private final BlockingQueue<XMLPacket> pktQueue;

    public XMLPacketSender( WritableByteChannel ch )
    {
        this(ch, ( msg, ex ) -> {
            if (msg != null)
                System.err.print(msg);
            if (ex != null)
                ex.printStackTrace(System.err);
        });
    }

    public XMLPacketSender( WritableByteChannel ch, Logger log )
    {
        this(ch, ( msg, ex ) -> log.error(msg, ex));
    }

    private XMLPacketSender( WritableByteChannel ch, BiConsumer<String, Throwable> exceptionLogger )
    {
        super(ConcurrencyUtils.defaultDaemonThreadFactory(), exceptionLogger);

        this.ch = Objects.requireNonNull(ch);
        this.pktQueue = new LinkedBlockingQueue<>();
    }

    public void send( XMLPacket pkt ) throws InterruptedException
    {
        pktQueue.put(Objects.requireNonNull(pkt));
    }

    @Override
    public void runInterruptibly() throws InterruptedException
    {
        try {
            IOWriter<XMLPacket> pktWriter = XMLPacket.IO.writer();
            while (true) {
                XMLPacket pkt = pktQueue.take();
                pktWriter.write(pkt, ch);
            }
        }
        catch (IOChannelWriteException e) {
            e.checkInterruptStatus();
            exceptionLogger().accept(
                String.format("!!! IO-WRITE error in XMLPacketSender: %s", e.getMessage()), null);
        }
    }
}
