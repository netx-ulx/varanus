package net.varanus.sdncontroller.linkstats;


import net.varanus.sdncontroller.linkstats.internal.LinkStatisticsManager;
import net.varanus.sdncontroller.util.module.AbstractServiceableModule;


/**
 *
 */
public final class LinkStatsModule extends AbstractServiceableModule
{
    public LinkStatsModule()
    {
        super(new LinkStatisticsManager(), ILinkStatsService.class);
    }
}
