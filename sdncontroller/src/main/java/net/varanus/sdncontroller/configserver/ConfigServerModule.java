package net.varanus.sdncontroller.configserver;


import net.varanus.sdncontroller.configserver.internal.ConfigServerManager;
import net.varanus.sdncontroller.util.module.AbstractFloodlightModule;


/**
 * 
 */
public class ConfigServerModule extends AbstractFloodlightModule
{
    public ConfigServerModule()
    {
        super(new ConfigServerManager());
    }
}
