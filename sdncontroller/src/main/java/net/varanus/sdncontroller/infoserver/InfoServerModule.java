package net.varanus.sdncontroller.infoserver;


import net.varanus.sdncontroller.infoserver.internal.InfoServerManager;
import net.varanus.sdncontroller.util.module.AbstractFloodlightModule;


/**
 * 
 */
public class InfoServerModule extends AbstractFloodlightModule
{
    public InfoServerModule()
    {
        super(new InfoServerManager());
    }
}
