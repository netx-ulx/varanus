package net.varanus.sdncontroller.monitoring.submodules.sampling.internal;


import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.projectfloodlight.openflow.protocol.OFFactories;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;

import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.varanus.sdncontroller.linkstats.sample.TrajectorySample;
import net.varanus.sdncontroller.logging.Logging;
import net.varanus.sdncontroller.monitoring.internal.IMonitoringModuleContext;
import net.varanus.sdncontroller.monitoring.submodules.collectorhandler.ICollectorHandlerService;
import net.varanus.sdncontroller.monitoring.util.ISubmoduleManager;
import net.varanus.sdncontroller.types.FlowedLink;
import net.varanus.sdncontroller.util.module.ModuleUtils;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.lang.Unsigned;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
final class CollectionManager implements ISubmoduleManager
{
    private static final Logger LOG = Logging.monitoring.sampling.LOG;

    private @Nullable ICollectorHandlerService collHandService;

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies()
    {
        return ModuleUtils.services(ICollectorHandlerService.class);
    }

    @Override
    public void init( IMonitoringModuleContext context ) throws FloodlightModuleException
    {
        this.collHandService = context.getServiceImpl(ICollectorHandlerService.class);
    }

    @Override
    public void startUp( IMonitoringModuleContext context ) throws FloodlightModuleException
    { /* do nothing */}

    CollectionRequester newCollectionRequester( FlowedLink flowedLink )
    {
        if (collHandService.hasNecessaryCollectors(flowedLink)) {
            return new ActiveCollectionRequester(flowedLink, collHandService);
        }
        else {
            LOG.debug("Cannot request sampling collection due to lack of collectors for flowed-link {}", flowedLink);
            return new InactiveCollectionRequester(flowedLink);
        }
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    static abstract class CollectionRequester
    {
        private final FlowedLink flowedLink;

        private CollectionRequester( FlowedLink flowedLink )
        {
            this.flowedLink = flowedLink;
        }

        final FlowedLink getFlowedLink()
        {
            return flowedLink;
        }

        abstract Optional<OFAction> getSrcSamplingAction( OFVersion version );

        abstract Optional<OFAction> getDestSamplingAction( OFVersion version );

        abstract CompletableFuture<TrajectorySample> requestCollection( Duration collDuration );
    }

    @ParametersAreNonnullByDefault
    private static final class InactiveCollectionRequester extends CollectionRequester
    {
        InactiveCollectionRequester( FlowedLink flowedLink )
        {
            super(flowedLink);
        }

        @Override
        Optional<OFAction> getSrcSamplingAction( OFVersion version )
        {
            return Optional.empty();
        }

        @Override
        Optional<OFAction> getDestSamplingAction( OFVersion version )
        {
            return Optional.empty();
        }

        @Override
        CompletableFuture<TrajectorySample> requestCollection( Duration collDuration )
        {
            return CompletableFuture.completedFuture(TrajectorySample.noResults(getFlowedLink()));
        }
    }

    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    private static final class ActiveCollectionRequester extends CollectionRequester
    {
        private final ICollectorHandlerService collHandService;

        ActiveCollectionRequester( FlowedLink flowedLink, ICollectorHandlerService collHandlerService )
        {
            super(flowedLink);
            this.collHandService = collHandlerService;
        }

        @Override
        Optional<OFAction> getSrcSamplingAction( OFVersion version )
        {
            return Optional.of(newSamplingAction(version));
        }

        @Override
        Optional<OFAction> getDestSamplingAction( OFVersion version )
        {
            return Optional.of(newSamplingAction(version));
        }

        @Override
        CompletableFuture<TrajectorySample> requestCollection( Duration collDuration )
        {
            return collHandService.sendSamplingRequest(getFlowedLink(), collDuration);
        }

        private OFAction newSamplingAction( OFVersion version )
        {
            return OFFactories.getFactory(version).actions()
                .output(getSamplingOFPort(), Unsigned.MAX_SHORT);
        }

        private OFPort getSamplingOFPort()
        {
            return collHandService.getSamplingPort().getOFPort();
        }
    }
}
