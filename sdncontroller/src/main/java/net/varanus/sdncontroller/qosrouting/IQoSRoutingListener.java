package net.varanus.sdncontroller.qosrouting;


import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.sdncontroller.types.FlowedConnection;


/**
 * 
 */
@ParametersAreNonnullByDefault
public interface IQoSRoutingListener
{
    public void connectionRegistered( FlowedConnection connection );

    public void connectionUnregistered( FlowedConnection connection );

    public void connectionMapUpdated( IFlowedConnectionMap map );
}
