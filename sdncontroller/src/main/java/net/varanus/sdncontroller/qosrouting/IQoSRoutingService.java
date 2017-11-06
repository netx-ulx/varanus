package net.varanus.sdncontroller.qosrouting;


import java.util.List;
import java.util.Optional;

import javax.annotation.ParametersAreNonnullByDefault;

import net.floodlightcontroller.core.module.IFloodlightService;
import net.varanus.sdncontroller.types.FlowedConnection;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.openflow.types.Flow;


/**
 * 
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public interface IQoSRoutingService extends IFloodlightService
{
    public Optional<IFlowedConnectionMap> getConnectionMap( FlowedConnection conn );

    public List<IFlowedConnectionMap> getAllConnectionMaps();

    public List<IFlowedConnectionMap> getAllConnectionMaps( Flow flow );

    public boolean isValidRegistrableConnection( FlowedConnection conn );

    public void validateRegistrableConnection( FlowedConnection conn ) throws IllegalArgumentException;

    public boolean registerConnection( FlowedConnection conn, boolean startFlowMonitoring )
        throws IllegalArgumentException;

    public boolean unregisterConnection( FlowedConnection conn, boolean stopFlowMonitoring )
        throws IllegalArgumentException;

    public void addListener( IQoSRoutingListener listener );

    public void removeListener( IQoSRoutingListener listener );
}
