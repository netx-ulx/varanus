package net.varanus.sdncontroller.alias;


import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import org.projectfloodlight.openflow.types.DatapathId;

import net.floodlightcontroller.core.module.IFloodlightService;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;


/**
 * 
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public interface IAliasService extends IFloodlightService
{
    /**
     * Returns the configured alias for the switch with the given datapath ID,
     * or {@code null} if no such alias is configured.
     * 
     * @param dpid
     *            A switch datapath ID
     * @return an alias for a given switch
     * @exception NullPointerException
     *                If {@code switchID} is {@code null}
     */
    public @CheckForNull String getSwitchAlias( DatapathId dpid );

    /**
     * Associates a given alias to the switch with the given datapath ID. Any
     * previously configured alias for the switch will be overwritten by this
     * one.
     * <p>
     * If {@code alias} is {@code null}, any previously configured alias for the
     * switch will be removed.
     * 
     * @param dpid
     *            A switch datapath ID
     * @param alias
     *            An alias for a switch
     * @exception NullPointerException
     *                If {@code switchID} is {@code null}
     */
    public void configureSwitchAlias( DatapathId dpid, @Nullable String alias );

    /**
     * Removes any configured alias from the switch with the given datapath ID.
     * 
     * @param dpid
     *            A switch datapath ID
     * @exception NullPointerException
     *                If {@code switchID} is {@code null}
     */
    public void removeSwitchAlias( DatapathId dpid );
}
