package net.varanus.sdncontroller.alias.internal;


import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.projectfloodlight.openflow.types.DatapathId;
import org.slf4j.Logger;

import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.varanus.sdncontroller.alias.IAliasService;
import net.varanus.sdncontroller.logging.Logging;
import net.varanus.sdncontroller.util.module.IModuleManager;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
public final class AliasManager implements IModuleManager, IAliasService
{
    private static final Logger LOG = Logging.alias.LOG;

    private @Nullable ConcurrentMap<DatapathId, String> datapathAliases;

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies()
    {
        return Collections.emptySet();
    }

    @Override
    public void init( FloodlightModuleContext context, Class<? extends IFloodlightModule> moduleClass )
        throws FloodlightModuleException
    {
        Map<String, String> params = context.getConfigParams(moduleClass);
        this.datapathAliases = Props.getDatapathAliases(params);
    }

    @Override
    public void startUp( FloodlightModuleContext context, Class<? extends IFloodlightModule> moduleClass )
        throws FloodlightModuleException
    {
        datapathAliases.forEach(( dpid, alias ) -> {
            LOG.info("Aliasing datapath ID {} to {}", dpid, alias);
        });
    }

    @Override
    public String getSwitchAlias( DatapathId dpid )
    {
        return datapathAliases.get(Objects.requireNonNull(dpid));
    }

    @Override
    public void configureSwitchAlias( DatapathId dpid, String alias )
    {
        if (alias == null) {
            removeSwitchAlias(dpid);
        }
        else {
            datapathAliases.put(Objects.requireNonNull(dpid), alias);
        }
    }

    @Override
    public void removeSwitchAlias( DatapathId dpid )
    {
        datapathAliases.remove(Objects.requireNonNull(dpid));
    }
}
