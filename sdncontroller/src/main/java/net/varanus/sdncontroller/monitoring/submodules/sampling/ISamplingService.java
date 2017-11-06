package net.varanus.sdncontroller.monitoring.submodules.sampling;


import javax.annotation.ParametersAreNonnullByDefault;

import net.floodlightcontroller.core.module.IFloodlightService;
import net.varanus.util.openflow.types.Flow;


/**
 * 
 */
@ParametersAreNonnullByDefault
public interface ISamplingService extends IFloodlightService
{
    public boolean isValidSamplableFlow( Flow flow );

    public void validateSamplableFlow( Flow flow ) throws IllegalArgumentException;

    public boolean startSampling( Flow flow ) throws IllegalArgumentException;

    public boolean stopSampling( Flow flow ) throws IllegalArgumentException;
}
