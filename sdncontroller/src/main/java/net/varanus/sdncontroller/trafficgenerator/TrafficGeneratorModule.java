package net.varanus.sdncontroller.trafficgenerator;


import net.varanus.sdncontroller.trafficgenerator.internal.TrafficGeneratorManager;
import net.varanus.sdncontroller.util.module.AbstractServiceableModule;


/**
 * 
 */
public final class TrafficGeneratorModule extends AbstractServiceableModule
{
    public TrafficGeneratorModule()
    {
        super(new TrafficGeneratorManager(), ITrafficGeneratorService.class);
    }
}
