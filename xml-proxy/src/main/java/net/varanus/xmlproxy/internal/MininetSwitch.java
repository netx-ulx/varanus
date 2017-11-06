package net.varanus.xmlproxy.internal;


import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import org.projectfloodlight.openflow.types.DatapathId;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.openflow.NodePortUtils;
import net.varanus.util.text.StringUtils;


/**
 * 
 */
@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
final class MininetSwitch
{
    static MininetSwitch parse( String s ) throws IllegalArgumentException
    {
        String[] split = s.split("\\|");
        if (split.length != 3) {
            throw new IllegalArgumentException("invalid split length in mininet switch");
        }
        else {
            String sName = validateSwitchName(split[0]);
            DatapathId dpid = NodePortUtils.parseDatapathId(split[1]);
            boolean isRemote = StringUtils.convertToBoolean(split[2]);
            return new MininetSwitch(sName, dpid, isRemote);
        }
    }

    private static String validateSwitchName( String name ) throws IllegalArgumentException
    {
        if (!name.isEmpty())
            return name;
        else
            throw new IllegalArgumentException("invalid mininet switch name (is empty)");
    }

    private final String     name;
    private final DatapathId dpid;
    private final boolean    isRemote;

    MininetSwitch( String name, DatapathId dpid, boolean isRemote )
    {
        this.name = name;
        this.dpid = dpid;
        this.isRemote = isRemote;
    }

    String getName()
    {
        return name;
    }

    DatapathId getDpid()
    {
        return dpid;
    }

    boolean isRemote()
    {
        return isRemote;
    }

    @Override
    public String toString()
    {
        return String.format("%s (%s) (%s)", name, (isRemote ? "remote" : "local"), dpid);
    }

}
