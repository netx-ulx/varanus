package net.varanus.sdncontroller.alias;


import net.varanus.sdncontroller.alias.internal.AliasManager;
import net.varanus.sdncontroller.util.module.AbstractServiceableModule;


/**
 * 
 */
public final class AliasModule extends AbstractServiceableModule
{
    public AliasModule()
    {
        super(new AliasManager(), IAliasService.class);
    }
}
