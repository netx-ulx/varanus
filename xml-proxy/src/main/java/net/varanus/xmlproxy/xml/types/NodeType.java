package net.varanus.xmlproxy.xml.types;


import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 */
@XmlType( name = "node_type" )
@XmlEnum
public enum NodeType
{
    HOST, SWITCH;

    public String value()
    {
        return name();
    }

    public static NodeType fromValue( String v )
    {
        return valueOf(v);
    }
}
