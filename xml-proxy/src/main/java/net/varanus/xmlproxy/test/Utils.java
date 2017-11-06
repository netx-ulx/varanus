package net.varanus.xmlproxy.test;


import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;


/**
 * 
 */
final class Utils
{
    static SocketAddress parseSocketAddress( String s ) throws IllegalArgumentException, UnknownHostException
    {
        if (!s.matches("[^:]+:\\d+"))
            throw new IllegalArgumentException("Argument does not match a valid HOST:PORT string");

        String[] split = s.split(":");
        InetAddress host = InetAddress.getByName(split[0]);
        int port = Integer.parseInt(split[1]);
        return new InetSocketAddress(host, port);
    }

    private Utils()
    {
        // not used
    }
}
