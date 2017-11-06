package net.varanus.sdncontroller.linkstats;


import java.util.List;
import java.util.Optional;

import javax.annotation.ParametersAreNonnullByDefault;

import net.floodlightcontroller.core.module.IFloodlightService;
import net.varanus.sdncontroller.linkstats.sample.LLDPProbingSample;
import net.varanus.sdncontroller.linkstats.sample.SecureProbingSample;
import net.varanus.sdncontroller.linkstats.sample.SwitchCounterSample;
import net.varanus.sdncontroller.linkstats.sample.TrajectorySample;
import net.varanus.sdncontroller.types.DatapathLink;
import net.varanus.sdncontroller.types.FlowedLink;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.openflow.types.Flow;
import net.varanus.util.openflow.types.NodeId;
import net.varanus.util.time.Timed;
import net.varanus.util.unitvalue.si.InfoDouble;
import net.varanus.util.unitvalue.si.MetricDouble;


/**
 *
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public interface ILinkStatsService extends IFloodlightService
{
    public Optional<GeneralLinkStats> getGeneralStats( DatapathLink link );

    public List<GeneralLinkStats> getAllGeneralStats();

    public Optional<FlowedLinkStats> getFlowedStats( FlowedLink link );

    public List<FlowedLinkStats> getAllFlowedStats();

    public List<FlowedLinkStats> getAllFlowedStats( DatapathLink link );

    public List<FlowedLinkStats> getAllFlowedStats( Flow flow );

    public boolean updateVirtualCapacity( DatapathLink link, Timed<InfoDouble> capacity )
        throws IllegalArgumentException;

    public boolean updatePacketDropRate( NodeId sw, Timed<MetricDouble> pktDropRate );

    public boolean updateGeneralStats( LLDPProbingSample sample );

    public boolean updateGeneralStats( SecureProbingSample sample );

    public boolean updateFlowedStats( SwitchCounterSample sample );

    public boolean updateFlowedStats( TrajectorySample sample );

    public boolean clearFlowedStats( FlowedLink link );

    public boolean clearAllFlowedStats();

    public boolean clearAllFlowedStats( DatapathLink link );

    public boolean clearAllFlowedStats( Flow flow );

    public void addListener( ILinkStatsListener listener );

    public void removeListener( ILinkStatsListener listener );
}
