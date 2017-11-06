package net.varanus.sdncontroller.monitoring;


import javax.annotation.ParametersAreNonnullByDefault;

import net.floodlightcontroller.core.module.IFloodlightService;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.openflow.types.Flow;


/**
 * 
 */

@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public interface IMonitoringService extends IFloodlightService
{
    public boolean isValidMonitorableFlow( Flow flow );

    public void validateMonitorableFlow( Flow flow ) throws IllegalArgumentException;

    public boolean startMonitoring( Flow flow ) throws IllegalArgumentException;

    public boolean stopMonitoring( Flow flow ) throws IllegalArgumentException;
}
