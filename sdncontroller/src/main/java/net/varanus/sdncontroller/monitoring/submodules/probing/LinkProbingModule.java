package net.varanus.sdncontroller.monitoring.submodules.probing;


import net.varanus.sdncontroller.monitoring.submodules.probing.internal.LinkProbingManager;
import net.varanus.sdncontroller.monitoring.util.AbstractServiceableSubmodule;


/**
 * 
 */
public final class LinkProbingModule extends AbstractServiceableSubmodule
{
    public LinkProbingModule()
    {
        super(new LinkProbingManager(), IProbingService.class);
    }
}
