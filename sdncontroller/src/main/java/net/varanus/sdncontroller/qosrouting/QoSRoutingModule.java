package net.varanus.sdncontroller.qosrouting;


import net.varanus.sdncontroller.qosrouting.internal.QoSRoutingManager;
import net.varanus.sdncontroller.util.module.AbstractServiceableModule;


/**
 * 
 */
public final class QoSRoutingModule extends AbstractServiceableModule
{
    public QoSRoutingModule()
    {
        super(new QoSRoutingManager(), IQoSRoutingService.class);
    }
}
