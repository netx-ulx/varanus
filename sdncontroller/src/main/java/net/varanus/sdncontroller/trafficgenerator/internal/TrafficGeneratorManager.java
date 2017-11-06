package net.varanus.sdncontroller.trafficgenerator.internal;


import static net.varanus.util.openflow.OFMessageUtils.eternalFlow;
import static net.varanus.util.openflow.OFMessageUtils.withNoOverlap;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.DelayQueue;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFFlowDeleteStrict;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.TableId;
import org.slf4j.Logger;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.restserver.IRestApiService;
import net.varanus.sdncontroller.logging.Logging;
import net.varanus.sdncontroller.topologygraph.ITopologyGraphListener;
import net.varanus.sdncontroller.topologygraph.ITopologyGraphService;
import net.varanus.sdncontroller.topologygraph.event.ITopologyLinkEvent;
import net.varanus.sdncontroller.topologygraph.event.ITopologyNodeEvent;
import net.varanus.sdncontroller.topologygraph.event.ITopologyPortEvent;
import net.varanus.sdncontroller.trafficgenerator.ITrafficGeneratorService;
import net.varanus.sdncontroller.trafficgenerator.TrafficProperties;
import net.varanus.sdncontroller.trafficgenerator.web.TGWebRoutable;
import net.varanus.sdncontroller.types.DatapathLink;
import net.varanus.sdncontroller.types.FlowedLink;
import net.varanus.sdncontroller.util.module.IModuleManager;
import net.varanus.sdncontroller.util.module.ModuleUtils;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.collect.AbstractDelayed;
import net.varanus.util.concurrent.ConcurrentService;
import net.varanus.util.lang.Unsigned;
import net.varanus.util.openflow.MatchUtils;
import net.varanus.util.openflow.types.Flow;


/**
 * 
 */
@FieldsAreNonnullByDefault
public final class TrafficGeneratorManager implements IModuleManager, ITrafficGeneratorService, ITopologyGraphListener
{
    private static final Logger LOG = Logging.trafficgenerator.LOG;

    private final Generator generator;

    public TrafficGeneratorManager()
    {
        this.generator = new Generator();
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies()
    {
        return ModuleUtils.services(
            IOFSwitchService.class,
            ITopologyGraphService.class,
            IRestApiService.class);
    }

    @Override
    public void init( FloodlightModuleContext context, Class<? extends IFloodlightModule> moduleClass )
    {
        generator.init(context);
        ModuleUtils.getServiceImpl(context, ITopologyGraphService.class).addListener(this);
        ModuleUtils.getServiceImpl(context, IRestApiService.class).addRestletRoutable(new TGWebRoutable());
    }

    @Override
    public void startUp( FloodlightModuleContext context, Class<? extends IFloodlightModule> moduleClass )
    {
        generator.start();
    }

    @Override
    public boolean startTraffic( TrafficProperties props )
    {
        return generator.startTraffic(props);
    }

    @Override
    public boolean stopTraffic( FlowedLink flowedLink )
    {
        return generator.stopTraffic(flowedLink);
    }

    @Override
    public boolean stopAllTraffic()
    {
        return generator.stopAllTraffic();
    }

    @Override
    public void onLinkEvent( ITopologyLinkEvent event )
    {
        switch (event.getType()) {
            case LINK_ADDED:
                generator.onLinkUp(event.getLink());
            break;

            case LINK_REMOVED:
                generator.onLinkDown(event.getLink());
            break;

            default:
                throw new AssertionError("unexpected enum value");
        }
    }

    @Override
    public void onNodeEvent( ITopologyNodeEvent event )
    {/* not used */}

    @Override
    public void onPortEvent( ITopologyPortEvent event )
    {/* not used */}

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    private static final class Generator extends ConcurrentService
    {
        private final Map<FlowedLink, TrafficProperties> trafficProps;
        private final Set<DatapathLink>                  activeLinks;
        private final Object                             writeLock;

        private final DelayQueue<TrafficTask> taskQueue;

        private @Nullable IOFSwitchService switchService;

        Generator()
        {
            super(( msg, ex ) -> LOG.error(msg, ex));

            this.trafficProps = new HashMap<>();
            this.activeLinks = new HashSet<>();
            this.writeLock = new Object();

            this.taskQueue = new DelayQueue<>();

        }

        void init( FloodlightModuleContext context )
        {
            this.switchService = ModuleUtils.getServiceImpl(context, IOFSwitchService.class);
        }

        boolean startTraffic( TrafficProperties props )
        {
            synchronized (writeLock) {
                if (null == trafficProps.putIfAbsent(props.getFlowedLink(), props)) {
                    // schedule task for this flowed-link with given properties
                    LOG.debug("Started traffic for flowed-link {}", props.getFlowedLink());
                    taskQueue.add(new TrafficTask(props));
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        boolean stopTraffic( FlowedLink flowedLink )
        {
            synchronized (writeLock) {
                // will stop re-schedule of the task for this flowed-link
                if (null != trafficProps.remove(flowedLink)) {
                    LOG.debug("Stopped traffic for flowed-link {}", flowedLink);
                    return true;
                }
                else {
                    return false;
                }
            }
        }

        boolean stopAllTraffic()
        {
            synchronized (writeLock) {
                if (trafficProps.isEmpty()) {
                    return false;
                }
                else {
                    // will stop re-schedule of all tasks
                    LOG.debug("Stopped all traffic");
                    trafficProps.clear();
                    return true;
                }
            }
        }

        void onLinkUp( DatapathLink link )
        {
            synchronized (writeLock) {
                if (activeLinks.add(link)) {
                    LOG.debug("LINK UP: {}", link);
                }
            }
        }

        void onLinkDown( DatapathLink link )
        {
            synchronized (writeLock) {
                if (activeLinks.remove(link)) {
                    LOG.debug("LINK DOWN: {}", link);
                }
            }
        }

        @Override
        public void runInterruptibly() throws InterruptedException
        {
            while (isRunning()) {
                LOG.trace("Waiting before sending traffic...");
                handleTask(taskQueue.take());
            }
        }

        private void handleTask( TrafficTask task )
        {
            synchronized (writeLock) {
                TrafficProperties props = task.getProperties();
                FlowedLink flowedLink = props.getFlowedLink();
                if (trafficProps.containsKey(flowedLink)) {
                    if (activeLinks.contains(flowedLink.unflowed())) {
                        IOFSwitch srcSw = switchService.getActiveSwitch(flowedLink.getSrcNode().getDpid());
                        IOFSwitch destSw = switchService.getActiveSwitch(flowedLink.getDestNode().getDpid());
                        if (srcSw != null && destSw != null) {
                            task.transmitPacket(srcSw, destSw);
                        }
                    }
                    // else do nothing on link-down

                    task.reschedule();
                    taskQueue.add(task);
                }
                else {
                    // shutdown the task and do not re-schedule it
                    LOG.debug("Traffic generator stopped generating traffic for flowed-link {}", flowedLink);

                    IOFSwitch srcSw = switchService.getActiveSwitch(flowedLink.getSrcNode().getDpid());
                    IOFSwitch destSw = switchService.getActiveSwitch(flowedLink.getDestNode().getDpid());
                    task.shutdown(srcSw, destSw);
                }
            }
        }
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    private static final class TrafficTask extends AbstractDelayed
    {
        private final TrafficProperties props;
        private boolean                 flowsInstalled;

        TrafficTask( TrafficProperties props )
        {
            super(props.getTransmissionInterval());
            this.props = props;
            this.flowsInstalled = false;
        }

        TrafficProperties getProperties()
        {
            return props;
        }

        void reschedule()
        {
            resetTimer();
        }

        void transmitPacket( IOFSwitch srcSw, IOFSwitch destSw )
        {
            if (!flowsInstalled) {
                installFlows(srcSw, destSw);
                flowsInstalled = true;
            }

            LOG.trace("Sending {} packets for flowed-link {}", props.getBatchSize(), props.getFlowedLink());
            OFPacketOut pOut = props.buildPacketOut(srcSw.getOFFactory());
            srcSw.write(Collections.nCopies(props.getBatchSize(), pOut));
        }

        void shutdown( @Nullable IOFSwitch srcSw, @Nullable IOFSwitch destSw )
        {
            if (flowsInstalled) {
                removeFlows(srcSw, destSw);
            }
        }

        private void installFlows( IOFSwitch srcSw, IOFSwitch destSw )
        {
            FlowedLink flowedLink = props.getFlowedLink();
            Flow flow = flowedLink.getFlow();

            { // source switch
                OFFactory fact = srcSw.getOFFactory();
                OFFlowAdd.Builder addFlow = fact.buildFlowAdd()
                    .setTableId(TableId.ZERO)
                    .setPriority(1)
                    .setMatch(
                        MatchUtils.builderFrom(flow.getMatch(fact))
                            .wildcard(MatchField.IN_PORT)
                            .build()) // output the packet
                    .setInstructions(
                        Collections.singletonList(
                            fact.instructions().applyActions(
                                Collections.singletonList(
                                    fact.actions()
                                        .output(flowedLink.getSrcPort().getOFPort(), Unsigned.MAX_SHORT)))));

                LOG.trace("Installing traffic SRC flow rule for flowed-link {}", flowedLink);
                srcSw.write(
                    eternalFlow(
                        withNoOverlap(
                            addFlow))
                                .build());
            }

            { // destination switch
                OFFactory fact = destSw.getOFFactory();
                OFFlowAdd.Builder addFlow = fact.buildFlowAdd()
                    .setTableId(TableId.ZERO)
                    .setPriority(1)
                    .setMatch(
                        MatchUtils.builderFrom(flow.getMatch(fact))
                            .setExact(MatchField.IN_PORT, flowedLink.getDestPort().getOFPort())
                            .build()) // drop the packet
                    .setInstructions(Collections.emptyList());

                LOG.trace("Installing traffic DEST flow rule for flowed-link {}", flowedLink);
                destSw.write(
                    eternalFlow(
                        withNoOverlap(
                            addFlow))
                                .build());
            }
        }

        private void removeFlows( @Nullable IOFSwitch srcSw, @Nullable IOFSwitch destSw )
        {
            FlowedLink flowedLink = props.getFlowedLink();
            Flow flow = flowedLink.getFlow();

            if (srcSw != null) { // source switch
                OFFactory fact = srcSw.getOFFactory();
                OFFlowDeleteStrict.Builder deleteFlow = fact.buildFlowDeleteStrict()
                    .setTableId(TableId.ZERO)
                    .setPriority(1)
                    .setMatch(
                        MatchUtils.builderFrom(flow.getMatch(fact))
                            .wildcard(MatchField.IN_PORT)
                            .build());
                // .setOutPort(flowedLink.getSrcPort()); // does not work with
                // monitoring

                LOG.trace("Removing traffic SRC flow rule for flowed-link {}", flowedLink);
                srcSw.write(
                    deleteFlow.build());
            }

            if (destSw != null) { // destination switch
                OFFactory fact = destSw.getOFFactory();
                OFFlowDeleteStrict.Builder deleteFlow = fact.buildFlowDeleteStrict()
                    .setTableId(TableId.ZERO)
                    .setPriority(1)
                    .setMatch(
                        MatchUtils.builderFrom(flow.getMatch(fact))
                            .setExact(MatchField.IN_PORT, flowedLink.getDestPort().getOFPort())
                            .build());

                LOG.trace("Removing traffic DEST flow rule for flowed-link {}", flowedLink);
                destSw.write(
                    deleteFlow.build());
            }
        }
    }
}
