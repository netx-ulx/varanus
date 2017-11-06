package net.varanus.sdncontroller.monitoring.submodules.sampling.internal;


import java.util.ArrayList;
import java.util.List;

import org.projectfloodlight.openflow.protocol.OFBarrierReply;

import com.google.common.collect.Iterables;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.UncheckedExecutionException;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.SwitchDisconnectedException;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
final class BarrierHandler
{
    private final List<ListenableFuture<OFBarrierReply>> awaitingReplies;

    BarrierHandler()
    {
        this.awaitingReplies = new ArrayList<>();
    }

    void sendRequest( IOFSwitch sw )
    {
        awaitingReplies.add(sw.writeRequest(sw.getOFFactory().barrierRequest()));
    }

    void waitForReplies()
    {
        // this removes each element as it is traversed
        for (ListenableFuture<OFBarrierReply> future : Iterables.consumingIterable(awaitingReplies)) {
            waitSkippingSwitchDisconnection(future);
        }

        assert (awaitingReplies.isEmpty());
    }

    private static void waitSkippingSwitchDisconnection( ListenableFuture<OFBarrierReply> future )
    {
        try {
            Futures.getUnchecked(future);
        }
        catch (UncheckedExecutionException e) {
            if (!(e.getCause() instanceof SwitchDisconnectedException)) {
                throw e;
            }
        }
    }
}
