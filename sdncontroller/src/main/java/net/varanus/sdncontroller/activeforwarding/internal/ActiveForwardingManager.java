package net.varanus.sdncontroller.activeforwarding.internal;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowAdd;
import org.projectfloodlight.openflow.protocol.OFFlowDeleteStrict;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.TableId;
import org.projectfloodlight.openflow.types.U64;
import org.slf4j.Logger;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.util.AppCookie;
import net.varanus.sdncontroller.activeforwarding.IActiveForwardingService;
import net.varanus.sdncontroller.alias.IAliasService;
import net.varanus.sdncontroller.logging.Logging;
import net.varanus.sdncontroller.qosrouting.FlowedRoute;
import net.varanus.sdncontroller.qosrouting.IFlowedConnectionMap;
import net.varanus.sdncontroller.qosrouting.IQoSRoutingListener;
import net.varanus.sdncontroller.qosrouting.IQoSRoutingService;
import net.varanus.sdncontroller.types.FlowedConnection;
import net.varanus.sdncontroller.util.module.IModuleManager;
import net.varanus.sdncontroller.util.module.ModuleUtils;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.collect.CollectionUtils;
import net.varanus.util.lang.Unsigned;
import net.varanus.util.openflow.MatchUtils;
import net.varanus.util.openflow.types.Flow;
import net.varanus.util.text.StringUtils;
import net.varanus.util.text.TerminalText;
import net.varanus.util.text.TerminalText.Color;


/**
 * 
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
public final class ActiveForwardingManager implements IModuleManager, IActiveForwardingService, IQoSRoutingListener
{

    // for route forwarding rule cookies
    private static final int ROUTES_APP_ID = 302;
    static {
        AppCookie.registerApp(ROUTES_APP_ID, "varanus-routes");
    }

    private static final Logger LOG = Logging.activeforwarding.LOG;

    private final Map<FlowedConnection, FlowedRoute> activeRoutes;
    private final Object                             writeLock;

    private @Nullable RouteSortingStrategy  sortStrat;
    private @Nullable Set<FlowedConnection> printableConns;
    private @Nullable IOFSwitchService      switchService;

    public ActiveForwardingManager()
    {
        this.activeRoutes = new LinkedHashMap<>();
        this.writeLock = new Object();
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies()
    {
        return ModuleUtils.services(
            IAliasService.class,
            IQoSRoutingService.class,
            IOFSwitchService.class);
    }

    @Override
    public void init( FloodlightModuleContext context, Class<? extends IFloodlightModule> moduleClass )
        throws FloodlightModuleException
    {
        IAliasService aliasService = context.getServiceImpl(IAliasService.class);

        Map<String, String> params = context.getConfigParams(moduleClass);
        this.sortStrat = Props.getSortingStrategy(params);
        this.printableConns = Props.getPrintableFlowedConnection(params, aliasService);

        ModuleUtils.getServiceImpl(context, IQoSRoutingService.class).addListener(this);
        this.switchService = ModuleUtils.getServiceImpl(context, IOFSwitchService.class);
    }

    @Override
    public void startUp( FloodlightModuleContext context, Class<? extends IFloodlightModule> moduleClass )
        throws FloodlightModuleException
    {
        LOG.info("Using route sorting strategy of {}", sortStrat);

        if (printableConns.isEmpty()) {
            LOG.info("No printable flowed-connections configured, no routes will be printed");
        }
        else {
            LOG.info("The following flowed-connections will have their routes printed:{}{}",
                System.lineSeparator(),
                StringUtils.joinAllInLines(printableConns));
        }
    }

    @Override
    public Optional<FlowedRoute> getActiveRoute( FlowedConnection connection )
    {
        synchronized (writeLock) {
            return Optional.ofNullable(activeRoutes.get(Objects.requireNonNull(connection)));
        }
    }

    @Override
    public List<FlowedRoute> getAllActiveRoutes()
    {
        synchronized (writeLock) {
            return new ArrayList<>(activeRoutes.values());
        }
    }

    @Override
    public List<FlowedRoute> getAllActiveRoutes( Flow flow )
    {
        synchronized (writeLock) {
            return CollectionUtils.toOptFlatList(activeRoutes.values(),
                ( route ) -> Optional.of(route)
                    .filter(r -> flow.equals(r.getFlow())));
        }
    }

    @Override
    public void connectionRegistered( FlowedConnection connection )
    {
        // do nothing
    }

    @Override
    public void connectionUnregistered( FlowedConnection connection )
    {
        synchronized (writeLock) {
            FlowedRoute route = activeRoutes.remove(connection);
            if (route != null) {
                LOG.debug("Flowed-connection unregistered (removing route): {}", connection);
                removeFlowRules(route);
            }
        }
    }

    @Override
    public void connectionMapUpdated( IFlowedConnectionMap map )
    {
        synchronized (writeLock) {
            FlowedConnection conn = map.getConnection();
            Optional<FlowedRoute> optOldRoute = Optional.ofNullable(activeRoutes.get(conn));
            Optional<FlowedRoute> optNewRoute = map.getBestRoute(sortStrat::weightStats);

            if (optOldRoute.isPresent() && optNewRoute.isPresent()) {
                FlowedRoute oldRoute = optOldRoute.get();
                FlowedRoute newRoute = optNewRoute.get();

                if (!oldRoute.hasSamePath(newRoute)) {
                    onRouteReplacing(newRoute);
                    activeRoutes.put(conn, newRoute);
                    removeFlowRules(oldRoute);
                    installFlowRules(newRoute);
                }
                else if (!oldRoute.hasSameCoreStats(newRoute)) {
                    onRouteStatsUpdate(newRoute);
                }
            }
            else if (optOldRoute.isPresent()) { // && !optNewRoute.isPresent()
                FlowedRoute oldRoute = optOldRoute.get();
                onRouteRemoval(conn);
                activeRoutes.remove(conn);
                removeFlowRules(oldRoute);
            }
            else if (optNewRoute.isPresent()) { // && !optOldRoute.isPresent()
                FlowedRoute newRoute = optNewRoute.get();
                onRouteInstall(newRoute);
                activeRoutes.put(conn, newRoute);
                installFlowRules(newRoute);
            }
            // else do nothing
        }
    }

    private void installFlowRules( FlowedRoute route )
    {
        Flow flow = route.getFlow();
        route.getPath().getHops().forEach(hop -> {
            IOFSwitch sw = switchService.getActiveSwitch(hop.getNodeId().getDpid());
            if (sw != null) {
                OFFactory fact = sw.getOFFactory();
                OFFlowAdd installRule = fact.buildFlowAdd()
                    .setTableId(TableId.ZERO)
                    .setPriority(1)
                    .setCookie(AppCookie.makeCookie(ROUTES_APP_ID, Long.hashCode(route.getPathId())))
                    .setCookieMask(U64.NO_MASK)
                    .setMatch(
                        MatchUtils.builderFrom(flow.getMatch(fact))
                            .setExact(MatchField.IN_PORT, hop.getInPortId().getOFPort())
                            .build())
                    .setInstructions(
                        Collections.singletonList(
                            fact.instructions().applyActions(
                                Collections.singletonList(
                                    fact.actions()
                                        .output(hop.getOutPortId().getOFPort(), Unsigned.MAX_SHORT)))))
                    .build();

                sw.write(installRule);
            }
        });
    }

    private void removeFlowRules( FlowedRoute route )
    {
        Flow flow = route.getFlow();
        route.getPath().getHops().forEach(hop -> {
            IOFSwitch sw = switchService.getActiveSwitch(hop.getNodeId().getDpid());
            if (sw != null) {
                OFFactory fact = sw.getOFFactory();
                OFFlowDeleteStrict removeRule = fact.buildFlowDeleteStrict()
                    .setTableId(TableId.ZERO)
                    .setPriority(1)
                    .setCookie(AppCookie.makeCookie(ROUTES_APP_ID, Long.hashCode(route.getPathId())))
                    .setCookieMask(U64.NO_MASK)
                    .setMatch(
                        MatchUtils.builderFrom(flow.getMatch(fact))
                            .setExact(MatchField.IN_PORT, hop.getInPortId().getOFPort())
                            .build())
                    .setOutPort(hop.getOutPortId().getOFPort())
                    .build();

                // FIXME:
                // sampling.Utils clears the outPort

                sw.write(removeRule);
            }
        });
    }

    private static final TerminalText INSTALL_FMT = TerminalText.newBuilder().textColor(Color.BRIGHT_GREEN).build();
    private static final TerminalText REMOVAL_FMT = TerminalText.newBuilder().textColor(Color.RED).build();
    private static final TerminalText REPLACE_FMT = TerminalText.newBuilder().textColor(Color.BRIGHT_CYAN).build();

    private void onRouteInstall( FlowedRoute route )
    {
        if (mayLogRoute(route))
            LOG.info("Installing new flowed-route:{}{}",
                System.lineSeparator(), INSTALL_FMT.format(route.toPrettyString()));
    }

    private void onRouteRemoval( FlowedConnection conn )
    {
        if (mayLogRoute(conn))
            LOG.info("Removing flowed-route for connection: {}",
                REMOVAL_FMT.format(conn));
    }

    private void onRouteReplacing( FlowedRoute route )
    {
        if (mayLogRoute(route))
            LOG.info("Replacing flowed-route:{}{}",
                System.lineSeparator(), REPLACE_FMT.format(route.toPrettyString()));
    }

    private void onRouteStatsUpdate( FlowedRoute route )
    {
        if (mayLogRoute(route))
            LOG.debug("Stats updated for flowed-route:{}{}",
                System.lineSeparator(), route.toPrettyString());
    }

    private boolean mayLogRoute( FlowedRoute route )
    {
        return mayLogRoute(route.getConnection());
    }

    private boolean mayLogRoute( FlowedConnection conn )
    {
        return printableConns.contains(conn);
    }
}
