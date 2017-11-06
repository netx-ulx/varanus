package net.varanus.sdncontroller.activeforwarding;


import java.util.List;
import java.util.Optional;

import javax.annotation.ParametersAreNonnullByDefault;

import net.floodlightcontroller.core.module.IFloodlightService;
import net.varanus.sdncontroller.qosrouting.FlowedRoute;
import net.varanus.sdncontroller.types.FlowedConnection;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.openflow.types.Flow;


/**
 * 
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public interface IActiveForwardingService extends IFloodlightService
{
    public Optional<FlowedRoute> getActiveRoute( FlowedConnection connection );

    public List<FlowedRoute> getAllActiveRoutes();

    public List<FlowedRoute> getAllActiveRoutes( Flow flow );
}
