package net.varanus.sdncontroller.trafficgenerator;


import javax.annotation.ParametersAreNonnullByDefault;

import net.floodlightcontroller.core.module.IFloodlightService;
import net.varanus.sdncontroller.types.FlowedLink;


/**
 * 
 */
@ParametersAreNonnullByDefault
public interface ITrafficGeneratorService extends IFloodlightService
{
    public boolean startTraffic( TrafficProperties props );

    public boolean stopTraffic( FlowedLink flowedLink );

    public boolean stopAllTraffic();
}
