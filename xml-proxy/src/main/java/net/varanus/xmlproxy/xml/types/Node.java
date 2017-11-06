package net.varanus.xmlproxy.xml.types;


import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * 
 */
@XmlType( name = "node" )
@XmlAccessorType( XmlAccessType.FIELD )
public final class Node
{
    @XmlAttribute( name = "id", required = true )
    private String id;

    @XmlElement( name = "type", required = true )
    private NodeType type;

    @XmlElement( name = "name", required = true )
    private String name;

    @XmlElement( name = "virtual", required = true )
    private Boolean virtual;

    @XmlElement( name = "visible", required = true )
    private Boolean visible;

    @XmlElement( name = "title", required = true )
    private String title;

    public String getId()
    {
        return Objects.requireNonNull(id);
    }

    public void setId( String id )
    {
        this.id = Objects.requireNonNull(id);
    }

    public NodeType getType()
    {
        return Objects.requireNonNull(type);
    }

    public void setType( NodeType type )
    {
        this.type = Objects.requireNonNull(type);
    }

    public String getName()
    {
        return Objects.requireNonNull(name);
    }

    public void setName( String name )
    {
        this.name = Objects.requireNonNull(name);
    }

    public boolean getVirtual()
    {
        return Objects.requireNonNull(virtual);
    }

    public void setVirtual( boolean virtual )
    {
        this.virtual = virtual;
    }

    public boolean getVisible()
    {
        return Objects.requireNonNull(visible);
    }

    public void setVisible( boolean visible )
    {
        this.visible = visible;
    }

    public String getTitle()
    {
        return Objects.requireNonNull(title);
    }

    public void setTitle( String title )
    {
        this.title = Objects.requireNonNull(title);
    }
}
