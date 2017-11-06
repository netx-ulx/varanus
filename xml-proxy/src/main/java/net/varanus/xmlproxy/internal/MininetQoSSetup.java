package net.varanus.xmlproxy.internal;


import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.xmlproxy.internal.MininetLinkInfo.BandwidthQoS;
import net.varanus.xmlproxy.internal.MininetLinkInfo.NetemQoS;


/**
 * 
 */
@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
final class MininetQoSSetup
{
    private final String       srcName;
    private final String       destName;
    private final BandwidthQoS bandQoS;
    private final NetemQoS     netemQoS;

    MininetQoSSetup( String srcName, String destName, BandwidthQoS bandQoS, NetemQoS netemQoS )
    {
        this.srcName = srcName;
        this.destName = destName;
        this.bandQoS = bandQoS;
        this.netemQoS = netemQoS;
    }

    String getSrcName()
    {
        return srcName;
    }

    String getDestName()
    {
        return destName;
    }

    BandwidthQoS getBandwidthQoS()
    {
        return bandQoS;
    }

    NetemQoS getNetemQoS()
    {
        return netemQoS;
    }

    boolean hasQoSToSetup()
    {
        return bandQoS.getBandwidth().isPresent()
               || netemQoS.getDelay().isPresent()
               || netemQoS.getLossRate().isPresent();
    }
}
