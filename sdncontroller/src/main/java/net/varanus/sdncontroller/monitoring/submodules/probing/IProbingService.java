package net.varanus.sdncontroller.monitoring.submodules.probing;


import net.floodlightcontroller.core.module.IFloodlightService;
import net.varanus.util.openflow.types.Flow;


/**
 * 
 */
public interface IProbingService extends IFloodlightService
{
    public Flow getProbeBaseFlow();
}
