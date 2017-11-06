from varanuspy.lib import NodeConfig, LinkConfig
from varanuspy.utils import as_int, as_str, as_callable, is_some, as_a, as_bool, some


class Ring( object ):

    def __init__( self, ringnum, nodes, links ):
        self.ringnum = ringnum
        self.nodes = some( nodes, name='nodes' )
        self.links = some( links, name='links' )


    def get_num( self ):
        return self.ringnum


    def get_nodes( self ):
        return self.nodes


    def get_links( self ):
        return self.links


    def __str__( self ):
        return '{}{{nodes{}; links{}}}'.format( self.ringnum, list( str( n ) for n in self.nodes ), \
                                                              list( str( L ) for L in self.links ) )



class RingBuilder( object ):

    def __init__( self, ringnum, prefix='n', port1_cfg=None, port2_cfg=None, link_builder=None ):
        self.ringnum = as_int( ringnum, minim=0, name='ringnum' )
        self.prefix = as_str( prefix, allow_empty=True, name='prefix' )
        self.port1_cfg = as_a( port1_cfg, instance_of=PortConfig, name='port1_cfg' ) if is_some( port1_cfg ) else None
        self.port2_cfg = as_a( port2_cfg, instance_of=PortConfig, name='port2_cfg' ) if is_some( port2_cfg ) else None
        self.link_builder = as_callable( link_builder, name='link_builder' ) if is_some( link_builder ) else None

        self.nodes = []
        self.links = []


    def add_node( self, node_builder, name=None, port1_cfg=None, port2_cfg=None, link_builder=None, **params ):
        if len( self.nodes ) == 0:
            self.nodes.append( self._new_node( node_builder, name, **params ) )
        else:
            node1 = self.nodes[-1]
            node2 = self._new_node( node_builder, name, **params )
            link = self._new_link( node1, port1_cfg, node2, port2_cfg, link_builder )
            self.nodes.append( node2 )
            self.links.append( link )


    def add_nodes( self, num_nodes, node_builder, port1_cfg=None, port2_cfg=None, link_builder=None ):
        num_nodes = as_int( num_nodes, minim=1, name='num_nodes' )
        for _ in xrange( num_nodes ):
            self.add_node( node_builder, port1_cfg, port2_cfg, link_builder )


    def build( self, lastport_cfg=None, firstport_cfg=None, link_builder=None ):
        if len( self.nodes ) >= 2:
            node1 = self.nodes[-1]
            port1_cfg = lastport_cfg
            node2 = self.nodes[0]
            port2_cfg = firstport_cfg
            self.links.append( self._new_link( node1, port1_cfg, node2, port2_cfg, link_builder ) )

        ring = Ring( self.ringnum, self.nodes, self.links )
        self.clear()
        return ring


    def clear( self ):
        self.nodes = []
        self.links = []


    def _new_node( self, node_builder, name, **params ):
        node_builder = as_callable( node_builder, name='node_builder' )
        num = self._next_node_num()
        name = as_str( name, name='name' ) if is_some( name ) else self._next_node_name( num )
        return as_a( node_builder( self.ringnum, num, name, **params ), instance_of=NodeConfig, name='created node' )


    def _next_node_num( self ):
        return len( self.nodes ) + 1


    def _next_node_name( self, num ):
        return 'r' + str( self.ringnum ) + self.prefix + str( num )


    def _new_link( self, node1, port1_cfg, node2, port2_cfg, link_builder ):
        if is_some( port1_cfg ):
            port1_cfg = as_a( port1_cfg, instance_of=PortConfig, name='port1_cfg' )
        elif is_some( self.port1_cfg ):
            port1_cfg = self.port1_cfg
        else:
            port1_cfg = PortConfig( 2 )

        if is_some( port2_cfg ):
            port2_cfg = as_a( port2_cfg, instance_of=PortConfig, name='port2_cfg' )
        elif is_some( self.port2_cfg ):
            port2_cfg = self.port2_cfg
        else:
            port2_cfg = PortConfig( 1 )

        if is_some( link_builder ):
            link_builder = as_callable( link_builder, name='link_builder' )
        elif is_some( self.link_builder ):
            link_builder = self.link_builder
        else:
            raise ValueError( 'no default link builder is available; must provide one' )

        port1 = port1_cfg.create_port( node1 )
        port2 = port2_cfg.create_port( node2 )
        return as_a( link_builder( port1, port2 ), instance_of=LinkConfig, name='created link' )



class PortConfig( object ):

    def __init__( self, portnum, name=None, is_virtual=True, **params ):
        self.portnum = as_int( portnum, minim=0, name='portnum' )
        self.name = as_str( name, name='name' ) if is_some( name ) else None
        self.is_virtual = as_bool( is_virtual, name='is_virtual' )
        self.params = params


    def create_port( self, node ):
        node = as_a( node, instance_of=NodeConfig, name='node' )
        if is_some( self.name ):
            portname = self.name
        else:
            portname = node.name + '-e' + str( self.portnum )
        return node.add_port( self.portnum, name=portname, is_virtual=self.is_virtual, **self.params )


    def get_portnum( self ):
        return self.portnum


    def get_name( self ):
        return self.name


    def is_virtual( self ):
        return self.is_virtual


    def __str__( self ):
        return '({}, {}, {})'.format( self.portnum, self.name, self.is_virtual )
