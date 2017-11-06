package net.varanus.sdncontroller.topologygraph;


import net.varanus.sdncontroller.topologygraph.internal.TopologyGraphManager;
import net.varanus.sdncontroller.util.module.AbstractServiceableModule;


/**
 * 
 */
public final class TopologyGraphModule extends AbstractServiceableModule
{
    public TopologyGraphModule()
    {
        super(new TopologyGraphManager(), ITopologyGraphService.class);
    }
}
