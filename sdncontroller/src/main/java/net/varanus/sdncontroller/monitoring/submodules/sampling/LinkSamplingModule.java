package net.varanus.sdncontroller.monitoring.submodules.sampling;


import net.varanus.sdncontroller.monitoring.submodules.sampling.internal.LinkSamplingManager;
import net.varanus.sdncontroller.monitoring.util.AbstractServiceableSubmodule;


/**
 * 
 */
public final class LinkSamplingModule extends AbstractServiceableSubmodule
{
    public LinkSamplingModule()
    {
        super(new LinkSamplingManager(), ISamplingService.class);
    }
}
