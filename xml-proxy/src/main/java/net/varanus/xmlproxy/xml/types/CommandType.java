package net.varanus.xmlproxy.xml.types;


import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 */
@XmlType( name = "command_type" )
@XmlEnum
public enum CommandType
{
    IPERF, PING;

    public String value()
    {
        return name();
    }

    public static CommandType fromValue( String v )
    {
        return valueOf(v);
    }
}
