package net.varanus.sdncontroller.monitoring.submodules.collectorhandler;


import net.varanus.sdncontroller.monitoring.submodules.collectorhandler.internal.CollectorHandlerManager;
import net.varanus.sdncontroller.monitoring.util.AbstractServiceableSubmodule;


/**
 * 
 */
public class CollectorHandlerModule extends AbstractServiceableSubmodule
{
    public CollectorHandlerModule()
    {
        super(new CollectorHandlerManager(), ICollectorHandlerService.class);
    }
}
