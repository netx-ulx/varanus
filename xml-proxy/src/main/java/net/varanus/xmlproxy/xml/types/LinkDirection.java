package net.varanus.xmlproxy.xml.types;


import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 */
@XmlType( name = "link_direction" )
@XmlEnum
public enum LinkDirection
{
    FORWARD, REVERSE;

    public String getValue()
    {
        return name();
    }

    public static LinkDirection fromValue( String v )
    {
        return valueOf(v);
    }
}
