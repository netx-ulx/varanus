package net.varanus.collector.internal;


import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.CheckForNull;
import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.openflow.types.BitMatch;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
final class CollectionHandle
{
    private final BitMatch                      match;
    private final BlockingQueue<CapturedPacket> matchedQueue;

    private final AtomicLong unmatchedBytes;
    private final AtomicLong unmatchedPkts;

    CollectionHandle( BitMatch match )
    {
        this.match = match;
        this.matchedQueue = new LinkedBlockingQueue<>();

        this.unmatchedBytes = new AtomicLong(0);
        this.unmatchedPkts = new AtomicLong(0);
    }

    BitMatch getMatch()
    {
        return match;
    }

    @CheckForNull
    CapturedPacket peekMatched()
    {
        return matchedQueue.peek();
    }

    @CheckForNull
    CapturedPacket pollMatched()
    {
        return matchedQueue.poll();
    }

    @CheckForNull
    CapturedPacket pollMatched( long timeout, TimeUnit unit ) throws InterruptedException
    {
        return matchedQueue.poll(timeout, unit);
    }

    CapturedPacket takeMatched() throws InterruptedException
    {
        return matchedQueue.take();
    }

    long getUnmatchedBytes()
    {
        return unmatchedBytes.get();
    }

    long getUnmatchedPackets()
    {
        return unmatchedPkts.get();
    }

    void collectMatched( CapturedPacket pkt )
    {
        matchedQueue.add(pkt);
    }

    void logUnmatched( CapturedPacket pkt )
    {
        unmatchedBytes.addAndGet(pkt.getPacket().length);
        unmatchedPkts.incrementAndGet();
    }
}
