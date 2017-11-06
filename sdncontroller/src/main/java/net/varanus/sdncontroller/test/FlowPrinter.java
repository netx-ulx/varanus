package net.varanus.sdncontroller.test;


import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.projectfloodlight.openflow.protocol.OFFlowStatsEntry;
import org.projectfloodlight.openflow.protocol.OFFlowStatsReply;
import org.projectfloodlight.openflow.protocol.OFFlowStatsRequest;
import org.projectfloodlight.openflow.types.DatapathId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.threadpool.IThreadPoolService;
import net.varanus.sdncontroller.util.module.ModuleUtils;


/**
 * 
 */
public class FlowPrinter implements IFloodlightModule, Runnable
{
    private static final long TASK_PERIOD_MILLIS = 2500L;

    private static final Logger LOG = LoggerFactory.getLogger(FlowPrinter.class);

    private IOFSwitchService switchService;

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices()
    {
        return null;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls()
    {
        return null;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies()
    {
        return ModuleUtils.services(IOFSwitchService.class, IThreadPoolService.class);
    }

    @Override
    public void init( FloodlightModuleContext context ) throws FloodlightModuleException
    {
        this.switchService = ModuleUtils.getServiceImpl(context, IOFSwitchService.class);
    }

    @Override
    public void startUp( FloodlightModuleContext context ) throws FloodlightModuleException
    {
        ModuleUtils.getServiceImpl(context, IThreadPoolService.class).getScheduledExecutor().scheduleWithFixedDelay(
            this,
            TASK_PERIOD_MILLIS,
            TASK_PERIOD_MILLIS,
            TimeUnit.MILLISECONDS);
    }

    @Override
    public void run()
    {
        try {
            Map<DatapathId, Future<List<OFFlowStatsReply>>> allReplies = new HashMap<>();

            for (IOFSwitch sw : switchService.getAllSwitchMap().values()) {
                OFFlowStatsRequest req = sw.getOFFactory().buildFlowStatsRequest()
                    .setMatch(sw.getOFFactory().matchWildcardAll())
                    .build();

                allReplies.put(sw.getId(), sw.writeStatsRequest(req));
            }

            for (Entry<DatapathId, Future<List<OFFlowStatsReply>>> switchReplies : allReplies.entrySet()) {
                DatapathId swID = switchReplies.getKey();
                Future<List<OFFlowStatsReply>> futureReplies = switchReplies.getValue();

                LOG.info("=================================================");
                LOG.info("Flows in switch {}:", swID);
                for (OFFlowStatsReply reply : futureReplies.get()) {
                    for (OFFlowStatsEntry entry : reply.getEntries()) {
                        LOG.info("{}", entry);
                    }
                }
            }
        }
        catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}
