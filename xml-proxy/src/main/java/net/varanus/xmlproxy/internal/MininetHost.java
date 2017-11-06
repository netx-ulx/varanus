package net.varanus.xmlproxy.internal;


import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import org.projectfloodlight.openflow.types.IPv4Address;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.text.StringUtils;


/**
 * 
 */
@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
final class MininetHost
{
    static MininetHost parse( String s ) throws IllegalArgumentException
    {
        String[] split = s.split("\\|");
        if (split.length != 4) {
            throw new IllegalArgumentException("invalid split length in mininet host");
        }
        else {
            String hName = validateHostName(split[0]);
            String hAddr = IPv4Address.of(split[1]).toString();
            boolean isRemote = StringUtils.convertToBoolean(split[2]);
            boolean isVisible = StringUtils.convertToBoolean(split[3]);
            return new MininetHost(hName, hAddr, isRemote, isVisible);
        }
    }

    private static String validateHostName( String name ) throws IllegalArgumentException
    {
        if (!name.isEmpty())
            return name;
        else
            throw new IllegalArgumentException("invalid mininet host name (is empty)");
    }

    private final String  name;
    private final String  address;
    private final boolean isRemote;
    private final boolean isVisible;

    MininetHost( String name, String address, boolean isRemote, boolean isVisible )
    {
        this.name = name;
        this.address = address;
        this.isRemote = isRemote;
        this.isVisible = isVisible;
    }

    String getName()
    {
        return name;
    }

    String getAddress()
    {
        return address;
    }

    boolean isRemote()
    {
        return isRemote;
    }

    boolean isVisible()
    {
        return isVisible;
    }

    @Override
    public String toString()
    {
        return String.format("%s (%s) (%s)%s",
            name, (isRemote ? "remote" : "local"), address, (isVisible ? "" : " (hidden)"));
    }
}
