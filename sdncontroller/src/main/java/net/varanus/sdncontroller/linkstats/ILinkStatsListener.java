package net.varanus.sdncontroller.linkstats;


import java.util.List;

import javax.annotation.ParametersAreNonnullByDefault;


/**
 * 
 */
@ParametersAreNonnullByDefault
public interface ILinkStatsListener
{
    public void generalUpdated( GeneralLinkStats stats );

    public void flowedUpdated( FlowedLinkStats stats );

    public default void flowedUpdated( List<FlowedLinkStats> statsList )
    {
        for (FlowedLinkStats stats : statsList) {
            flowedUpdated(stats);
        }
    }

    public void generalCleared( GeneralLinkStats last );

    public void flowedCleared( FlowedLinkStats last );

    public default void flowedCleared( List<FlowedLinkStats> statsList )
    {
        for (FlowedLinkStats stats : statsList) {
            flowedCleared(stats);
        }
    }
}
