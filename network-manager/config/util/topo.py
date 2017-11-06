import argparse
import json
import os
import subprocess

from config.util.ring import Ring, RingBuilder, PortConfig
from varanuspy.runner import MininetRunner
from varanuspy.utils import as_a, is_some, as_int, as_oneof, some, as_str


class VaranusTopo( object ):
    """ Helper class for setting up varanus topology """

    _SAMPLING_OFPORT = 32768
    _SDNCONTROLLER_CFG_FILE = 'sdncontroller/config/1-varanus-sdncontroller.properties'
    _COLLECTOR_CFG_FILE = 'collector/config/1-varanus-collector.properties'

    @staticmethod
    def __build_arg_parser():
        fmtcls = argparse.ArgumentDefaultsHelpFormatter
        parser = argparse.ArgumentParser( formatter_class=fmtcls )

        parser.add_argument( '--autocfg', \
                             action='store_true', \
                             help='Generate local VARANUS sdncontroller/collector configurations and automatically deploy them when starting the applications', \
                             default=False )

        parser.add_argument( '--gencfg', \
                             metavar='FILE', \
                             help='Generate VARANUS sdncontroller/collector configuration to the specified file, and exit', \
                             default=None )

        parser.add_argument( '--cleanlocal', \
                             action='store_true', \
                             help='Delete all configured local ovs-switch and virtual interfaces, and exit', \
                             default=False )

        parser.add_argument( '--netcontroller', \
                             action='store_true', \
                             help='Use a net controller that must be started independently of Mininet', \
                             default=False )
        return parser


    def __init__( self, mr, topo_args, local_varanus_home=None ):
        parser = VaranusTopo.__build_arg_parser()
        args, _ = parser.parse_known_args( topo_args )
        self.mr = as_a( mr, instance_of=MininetRunner, name='mr' )

        self.autocfg = args.autocfg
        self.gencfg_file = args.gencfg
        self.gencfg = is_some( self.gencfg_file )
        self.cleanlocal = args.cleanlocal
        self.use_netcontroller = args.netcontroller

        self.local_varanus_home = local_varanus_home

        self.controller = None # ControllerConfig
        self.rings = {}        # Ring mapped by ringnum
        self.switches = {}     # NodeConfig mapped by name
        self.hosts = {}        # NodeConfig mapped by name
        self.collectors = {}   # NodeConfig mapped by name


    # ================ CONTROLLERS ================

    def add_local_sdncontroller( self, name='flood', sudo=None, **params ):
        if is_some( self.controller ):
            raise RuntimeError( 'only one controller is supported' )

        if self.use_netcontroller:
            self.controller = self.mr.add_network_controller( name, **params )
        else:
            varanus_home = self.local_varanus_home
            if not is_some( varanus_home ):
                raise RuntimeError( 'VARANUS home is not defined, cannot add sdncontroller' )
            self.controller = self.mr.add_local_varanus_sdncontroller( name, varanus_home, sudo=sudo, **params )


    def add_remote_sdncontroller( self, user, server, varanus_home, name='flood', sudo=None, **params ):
        if is_some( self.controller ):
            raise RuntimeError( 'only one controller is supported' )

        if self.use_netcontroller:
            self.controller = self.mr.add_network_controller( name, **params )
        else:
            self.controller = self.mr.add_remote_varanus_sdncontroller( name, user, server, varanus_home, sudo=sudo, **params )


    # ================ SWITCHES ================

    def add_local_ovs_switch( self, sw_name, dpid, **params ):
        sw_name = as_str( sw_name, name='sw_name' )
        dpid = as_int( dpid, minim=0, name='dpid' )

        sw = NodeUtils.add_local_ovs_switch( self.mr, sw_name, dpid, **params )
        self.switches[sw_name] = sw


    def add_remote_ovs_switch( self, sw_name, user, server, dpid, **params ):
        sw_name = as_str( sw_name, name='sw_name' )
        dpid = as_int( dpid, minim=0, name='dpid' )

        sw = NodeUtils.add_remote_ovs_switch( self.mr, sw_name, user, server, dpid, **params )
        self.switches[sw_name] = sw


    def add_pica8_switch( self, sw_name, user, server, dpid, **params ):
        sw_name = as_str( sw_name, name='sw_name' )
        dpid = as_int( dpid, minim=0, name='dpid' )

        sw = NodeUtils.add_pica8_switch( self.mr, sw_name, user, server, dpid, **params )
        self.switches[sw_name] = sw


    def add_vir_switch_link( self, sw_name1, sw_portnum1, sw_name2, sw_portnum2 ):
        sw_name1 = as_oneof( sw_name1, self.switches.keys(), valname='sw_name1', containername='switch names' )
        sw_name2 = as_oneof( sw_name2, self.switches.keys(), valname='sw_name2', containername='switch names' )
        sw_portnum1 = as_int( sw_portnum1, minim=0, name='sw_portnum1' )
        sw_portnum2 = as_int( sw_portnum2, minim=0, name='sw_portnum2' )

        port_cfg1 = NodeUtils.get_generic_vir_port_cfg( sw_portnum1 )
        port_cfg2 = NodeUtils.get_generic_vir_port_cfg( sw_portnum2 )

        self.__add_switch_link( sw_name1, port_cfg1, sw_name2, port_cfg2 )


    def add_phy_switch_link( self, sw_name1, sw_portname1, sw_portnum1, sw_name2, sw_portname2, sw_portnum2 ):
        sw_name1 = as_oneof( sw_name1, self.switches.keys(), valname='sw_name1', containername='switch names' )
        sw_name2 = as_oneof( sw_name2, self.switches.keys(), valname='sw_name2', containername='switch names' )
        sw_portname1 = as_str( sw_portname1, name='sw_portname1' )
        sw_portname2 = as_str( sw_portname2, name='sw_portname2' )
        sw_portnum1 = as_int( sw_portnum1, minim=0, name='sw_portnum1' )
        sw_portnum2 = as_int( sw_portnum2, minim=0, name='sw_portnum2' )

        port_cfg1 = NodeUtils.get_generic_phy_port_cfg( sw_portname1, sw_portnum1 )
        port_cfg2 = NodeUtils.get_generic_phy_port_cfg( sw_portname2, sw_portnum2 )

        self.__add_switch_link( sw_name1, port_cfg1, sw_name2, port_cfg2 )


    # ================ HOSTS ================

    def add_local_vir_host( self, h_name, **params ):
        h_name = as_str( h_name, name='h_name' )
        self.__add_local_vir_host( h_name, **params )


    def add_local_phy_host( self, h_name, **params ):
        h_name = as_str( h_name, name='h_name' )
        self.__add_local_phy_host( h_name, **params )


    def add_remote_vir_host( self, h_name, user, server, **params ):
        h_name = as_str( h_name, name='h_name' )
        self.__add_remote_vir_host( h_name, user, server, **params )


    def add_remote_phy_host( self, h_name, user, server, **params ):
        h_name = as_str( h_name, name='h_name' )
        self.__add_remote_phy_host( h_name, user, server, **params )


    def add_vir_host_link( self, h_name, h_portnum, h_port_ip, sw_name, sw_portnum ):
        h_name = as_oneof( h_name, self.hosts.keys(), valname='h_name', containername='host names' )
        sw_name = as_oneof( sw_name, self.switches.keys(), valname='sw_name', containername='switch names' )
        h_portnum = as_int( h_portnum, minim=0, name='h_portnum' )
        h_port_ip = as_str( h_port_ip, name='h_port_ip' )
        sw_portnum = as_int( sw_portnum, minim=0, name='sw_portnum' )

        h_port_cfg = NodeUtils.get_host_vir_port_cfg( h_portnum, h_port_ip )
        sw_port_cfg = NodeUtils.get_generic_vir_port_cfg( sw_portnum )

        self.__add_host_link( h_name, h_port_cfg, sw_name, sw_port_cfg )


    def add_phy_host_link( self, h_name, h_portname, h_portnum, h_port_ip, h_port_mac, sw_name, sw_portname, sw_portnum ):
        h_name = as_oneof( h_name, self.hosts.keys(), valname='h_name', containername='host names' )
        sw_name = as_oneof( sw_name, self.switches.keys(), valname='sw_name', containername='switch names' )
        h_portname = as_str( h_portname, name='h_portname' )
        h_portnum = as_int( h_portnum, minim=0, name='h_portnum' )
        h_port_ip = as_str( h_port_ip, name='h_port_ip' )
        h_port_mac = as_str( h_port_mac, name='h_port_mac' ) if is_some( h_port_mac ) else None
        sw_portname = as_str( sw_portname, name='sw_portname' )
        sw_portnum = as_int( sw_portnum, minim=0, name='sw_portnum' )

        h_port_cfg = NodeUtils.get_host_phy_port_cfg( h_portname, h_portnum, h_port_ip, h_port_mac )
        sw_port_cfg = NodeUtils.get_generic_phy_port_cfg( sw_portname, sw_portnum )

        self.__add_host_link( h_name, h_port_cfg, sw_name, sw_port_cfg )


    # ================ COLLECTORS ================

    def add_local_collector( self, c_name, cid, **params ):
        varanus_home = self.local_varanus_home
        if not is_some( varanus_home ):
            raise RuntimeError( 'VARANUS home is not defined, cannot add collector' )

        c_name = as_str( c_name, name='c_name' )
        cid = as_int( cid, name='cid' )

        self.__add_local_collector( c_name, varanus_home, cid, **params )


    def add_remote_collector( self, c_name, user, server, varanus_home, cid, **params ):
        c_name = as_str( c_name, name='c_name' )
        cid = as_int( cid, name='cid' )

        self.__add_remote_collector( c_name, user, server, varanus_home, cid, **params )


    def add_vir_collector_link( self, c_name, c_portnum, sw_name, sw_portnum ):
        c_name = as_oneof( c_name, self.collectors.keys(), valname='c_name', containername='collector names' )
        sw_name = as_oneof( sw_name, self.switches.keys(), valname='sw_name', containername='switch names' )
        c_portnum = as_int( c_portnum, minim=0, name='c_portnum' )
        sw_portnum = as_int( sw_portnum, minim=0, name='sw_portnum' )

        c_port_cfg = NodeUtils.get_generic_vir_port_cfg( c_portnum )
        sw_port_cfg = NodeUtils.get_switch_to_collector_vir_port_cfg( sw_portnum )
        self.__add_collector_link( c_name, c_port_cfg, sw_name, sw_port_cfg )


    def add_phy_collector_link( self, c_name, c_portname, c_portnum, sw_name, sw_portname, sw_portnum ):
        c_name = as_oneof( c_name, self.collectors.keys(), valname='c_name', containername='collector names' )
        sw_name = as_oneof( sw_name, self.switches.keys(), valname='sw_name', containername='switch names' )
        c_portname = as_str( c_portname, name='c_portname' )
        sw_portname = as_str( sw_portname, name='sw_portname' )
        c_portnum = as_int( c_portnum, minim=0, name='c_portnum' )
        sw_portnum = as_int( sw_portnum, minim=0, name='sw_portnum' )

        c_port_cfg = NodeUtils.get_generic_phy_port_cfg( c_portname, c_portnum )
        sw_port_cfg = NodeUtils.get_switch_to_collector_phy_port_cfg( sw_portname, sw_portnum )
        self.__add_collector_link( c_name, c_port_cfg, sw_name, sw_port_cfg )


    # ================ RINGS ================

    def get_ring_builder( self, ringnum ):
        port1_cfg = RingPorts.internal_vir_1()
        port2_cfg = RingPorts.internal_vir_2()
        link_builder = self.get_ring_link_builder()
        return RingBuilder( ringnum, port1_cfg=port1_cfg, port2_cfg=port2_cfg, link_builder=link_builder )


    def get_ring_local_switch_builder( self ):
        return RingLocalSwitchBuilder( self.mr ).build_switch


    def get_ring_remote_switch_builder( self, user, server ):
        return RingRemoteSwitchBuilder( self.mr, user, server ).build_switch


    def get_ring_link_builder( self ):
        return RingLinkBuilder( self.mr ).build_link


    def add_existing_ovs_switch_ring( self, ring ):
        ring = as_a( ring, instance_of=Ring, name='ring' )
        self.rings[ring.get_num()] = ring
        self.switches.update( ( ( s.name, s ) for s in ring.get_nodes() ) )


    def add_local_vir_ovs_switch_ring( self, ringnum, size ):
        ringnum = as_int( ringnum, minim=1, name='ringnum' )
        size = as_int( size, minim=1, name='size' )
        rb = self.get_ring_builder( ringnum )

        node_builder = self.get_ring_local_switch_builder()
        rb.add_nodes( size, node_builder )
        ring = rb.build()
        self.add_existing_ovs_switch_ring( ring )


    def add_remote_vir_ovs_switch_ring( self, ringnum, size, user, server ):
        ringnum = as_int( ringnum, minim=1, name='ringnum' )
        size = as_int( size, minim=1, name='size' )
        rb = self.get_ring_builder( ringnum )

        node_builder = self.get_ring_remote_switch_builder( user, server )
        rb.add_nodes( size, node_builder )
        ring = rb.build()
        self.add_existing_ovs_switch_ring( ring )


    def add_ring_vir_link( self, ringnum1, sw_name1, ringnum2, sw_name2 ):
        ringnum1 = as_oneof( ringnum1, self.rings.keys(), valname='ringnum1', containername='ring numbers' )
        ringnum2 = as_oneof( ringnum2, self.rings.keys(), valname='ringnum2', containername='ring numbers' )
        sw_name1 = as_oneof( sw_name1, self.switches.keys(), valname='sw_name1', containername='switch names' )
        sw_name2 = as_oneof( sw_name2, self.switches.keys(), valname='sw_name2', containername='switch names' )

        ring1 = self.rings[ringnum1]
        if not any( s.name == sw_name1 for s in ring1.get_nodes() ):
            raise ValueError( 'no switch named "{}" exists in ring {}'.format( sw_name1, ringnum1 ) )

        ring2 = self.rings[ringnum2]
        if not any( s.name == sw_name2 for s in ring2.get_nodes() ):
            raise ValueError( 'no switch named "{}" exists in ring {}'.format( sw_name2, ringnum2 ) )

        port_cfg = RingPorts.ring_to_ring_vir()
        self.__add_switch_link( sw_name1, port_cfg, sw_name2, port_cfg )


    def add_ring_local_vir_host( self, h_name, h_portnum, h_port_ip, ringnum, sw_name, **params ):
        h_name = some( h_name, name='h_name' )
        h_portnum = as_int( h_portnum, minim=0, name='h_portnum' )
        h_port_ip = as_str( h_port_ip, name='h_port_ip' )
        ringnum = as_oneof( ringnum, self.rings.keys(), valname='ringnum', containername='ring numbers' )
        sw_name = as_oneof( sw_name, self.switches.keys(), valname='sw_name', containername='switch names' )

        ring = self.rings[ringnum]
        if not any( s.name == sw_name for s in ring.get_nodes() ):
            raise ValueError( 'no switch named "{}" exists in ring {}'.format( sw_name, ringnum ) )

        self.__add_local_vir_host( h_name, **params )

        h_port_cfg = NodeUtils.get_host_vir_port_cfg( h_portnum, h_port_ip )
        sw_port_cfg = RingPorts.ring_to_host_vir()
        self.__add_host_link( h_name, h_port_cfg, sw_name, sw_port_cfg )


    def add_ring_remote_vir_host( self, h_name, user, server, h_portnum, h_port_ip, ringnum, sw_name, **params ):
        h_name = some( h_name, name='h_name' )
        h_portnum = as_int( h_portnum, minim=0, name='h_portnum' )
        h_port_ip = as_str( h_port_ip, name='h_port_ip' )
        ringnum = as_oneof( ringnum, self.rings.keys(), valname='ringnum', containername='ring numbers' )
        sw_name = as_oneof( sw_name, self.switches.keys(), valname='sw_name', containername='switch names' )

        ring = self.rings[ringnum]
        if not any( s.name == sw_name for s in ring.get_nodes() ):
            raise ValueError( 'no switch named "{}" exists in ring {}'.format( sw_name, ringnum ) )

        self.__add_remote_vir_host( h_name, user, server, **params )

        h_port_cfg = NodeUtils.get_host_vir_port_cfg( h_portnum, h_port_ip )
        sw_port_cfg = RingPorts.ring_to_host_vir()
        self.__add_host_link( h_name, h_port_cfg, sw_name, sw_port_cfg )


    def add_ring_local_vir_collector( self, c_name, cid, ringnum, **params ):
        varanus_home = self.local_varanus_home
        if not is_some( varanus_home ):
            raise RuntimeError( 'VARANUS home is not defined, cannot add collector' )

        c_name = as_str( c_name, name='c_name' )
        cid = as_int( cid, name='cid' )
        ringnum = as_oneof( ringnum, self.rings.keys(), valname='ringnum', containername='ring numbers' )
        ring = self.rings[ringnum]

        self.__add_local_collector( c_name, varanus_home, cid, **params )

        c_port_num = 1
        sw_port_cfg = RingPorts.ring_to_collector_vir()
        for sw in ring.get_nodes():
            c_port_cfg = NodeUtils.get_generic_vir_port_cfg( c_port_num )
            self.__add_collector_link( c_name, c_port_cfg, sw.name, sw_port_cfg )
            c_port_num += 1


    def add_ring_remote_vir_collector( self, c_name, user, server, varanus_home, cid, ringnum, **params ):
        c_name = as_str( c_name, name='c_name' )
        cid = as_int( cid, name='cid' )
        ringnum = as_oneof( ringnum, self.rings.keys(), valname='ringnum', containername='ring numbers' )
        ring = self.rings[ringnum]

        self.__add_remote_collector( c_name, user, server, varanus_home, cid, **params )

        c_port_num = 1
        sw_port_cfg = RingPorts.ring_to_collector_vir()
        for sw in ring.get_nodes():
            c_port_cfg = NodeUtils.get_generic_vir_port_cfg( c_port_num )
            self.__add_collector_link( c_name, c_port_cfg, sw.name, sw_port_cfg )
            c_port_num += 1


    # ================ AUXILIARY TOPOLOGY METHODS ================

    def __add_switch_link( self, sw_name1, port_cfg1, sw_name2, port_cfg2 ):
        sw1 = self.switches[sw_name1]
        sw_port1 = port_cfg1.create_port( sw1 )
        sw2 = self.switches[sw_name2]
        sw_port2 = port_cfg2.create_port( sw2 )

        self.mr.add_link( sw_port1, sw_port2 )


    def __add_local_vir_host( self, h_name, **params ):
        h = NodeUtils.add_local_vir_host( self.mr, h_name, **params )
        self.hosts[h_name] = h


    def __add_local_phy_host( self, h_name, **params ):
        h = NodeUtils.add_local_phy_host( self.mr, h_name, **params )
        self.hosts[h_name] = h


    def __add_remote_vir_host( self, h_name, user, server, **params ):
        h = NodeUtils.add_remote_vir_host( self.mr, h_name, user, server, **params )
        self.hosts[h_name] = h


    def __add_remote_phy_host( self, h_name, user, server, **params ):
        h = NodeUtils.add_remote_phy_host( self.mr, h_name, user, server, **params )
        self.hosts[h_name] = h


    def __add_host_link( self, h_name, h_port_cfg, sw_name, sw_port_cfg ):
        h = self.hosts[h_name]
        h_port = h_port_cfg.create_port( h )
        sw = self.switches[sw_name]
        sw_port = sw_port_cfg.create_port( sw )

        self.mr.add_link( h_port, sw_port )


    def __add_local_collector( self, c_name, varanus_home, cid, **params ):
        c = NodeUtils.add_local_collector( self.mr, c_name, varanus_home, cid, **params )
        self.collectors[c_name] = c


    def __add_remote_collector( self, c_name, user, server, varanus_home, cid, **params ):
        c = NodeUtils.add_remote_collector( self.mr, c_name, user, server, varanus_home, cid, **params )
        self.collectors[c_name] = c


    def __add_collector_link( self, c_name, c_port_cfg, sw_name, sw_port_cfg ):
        c = self.collectors[c_name]
        c_port = c_port_cfg.create_port( c )
        sw = self.switches[sw_name]
        sw_port = sw_port_cfg.create_port( sw )

        self.mr.add_link( c_port, sw_port )


    # ================ FINALIZE ================

    def conclude( self ):
        """ Returns True if the program should continue the execution; False otherwise.
        """
        did_gencfg = self.__handle_gencfg()
        did_cleanlocal = self.__handle_cleanlocal()
        return not ( did_gencfg or did_cleanlocal )


    # ================ CONFIG GENERATION ================

    def __handle_gencfg( self ):
        if self.gencfg:
            with open( self.gencfg_file, 'w' ) as f:
                f.write( '# ==== SDNCONTROLLER CFG ====' )
                f.write( '\n' )
                self.__write_sdncontroller_config( f )

                f.write( '\n' )
                f.write( '# ==== COLLECTOR CFG ====' )
                f.write( '\n' )
                self.__write_collector_config( f )
            return True
        else:
            if self.autocfg and is_some( self.local_varanus_home ):
                sdncontroller_file = os.path.join( self.local_varanus_home, VaranusTopo._SDNCONTROLLER_CFG_FILE )
                with open( sdncontroller_file, 'w' ) as f:
                    self.__write_sdncontroller_config( f, multiline=False )

                collector_file = os.path.join( self.local_varanus_home, VaranusTopo._COLLECTOR_CFG_FILE )
                with open( collector_file, 'w' ) as f:
                    self.__write_collector_config( f, multiline=False )
            return False


    def __write_sdncontroller_config( self, f, multiline=True ):
        f.write( 'net.varanus.sdncontroller.monitoring.MonitoringModule.collectorhandler_samplingOFPort='\
               + str( VaranusTopo._SAMPLING_OFPORT ) )
        f.write( '\n' )

        f.write( 'net.varanus.sdncontroller.flowdiscovery.FlowDiscoveryModule.staticFlowedConnections=' )
        if multiline: f.write( '\n' )
        VaranusTopo.__dump_json( f, self.__get_host_connections(), multiline=multiline )
        f.write( '\n' )

        f.write( 'net.varanus.sdncontroller.alias.AliasModule.switchAliases=' )
        if multiline: f.write( '\n' )
        VaranusTopo.__dump_json( f, self.__get_switch_aliases(), multiline=multiline )
        f.write( '\n' )


    def __write_collector_config( self, f, multiline=True ):
        f.write( 'switchIfaceMapping=' )
        if multiline: f.write( '\n' )
        VaranusTopo.__dump_json( f, self.__get_switch_iface_mapping(), multiline=multiline )
        f.write( '\n' )

        if is_some( self.controller ):
            ip = self.controller.ip
            collector_port = self.controller.collector_port
            f.write( 'controllerAddress={}:{}'.format( ip, collector_port ) )
            f.write( '\n' )

        f.write( 'switchAliases=' )
        if multiline: f.write( '\n' )
        VaranusTopo.__dump_json( f, self.__get_switch_aliases(), multiline=multiline )
        f.write( '\n' )


    def __get_switch_aliases( self ):
        return dict( ( '{:#x}'.format( s.dpid ), name ) for name, s in self.switches.iteritems() )


    def __get_host_connections( self ):
        conns = []
        for h1 in self.hosts.itervalues():
            for h1p in VaranusTopo.__get_peered_ports( h1 ):
                for h2 in filter( lambda h : h is not h1 , self.hosts.itervalues() ):
                    for h2p in VaranusTopo.__get_peered_ports( h2 ):
                        p1 = h1p.get_peer()
                        p2 = h2p.get_peer()
                        ip1 = str( h1p.ip ).partition( '/' )[0]
                        ip2 = str( h2p.ip ).partition( '/' )[0]
                        conns.append( '{:#x}[{}] >> {:#x}[{}] | v14[eth_type = 0x0800, ipv4_src = {}, ipv4_dst = {}]'\
                                      .format( p1.node.dpid, p1.num, p2.node.dpid, p2.num, ip1, ip2 ) )
        return conns


    def __get_switch_iface_mapping( self ):
        return dict( ( str( c.cid ), VaranusTopo.__get_ifaces_mapping( c ) ) for c in self.collectors.itervalues() )


    @staticmethod
    def __get_ifaces_mapping( node ):
        return dict( ( '{:#x}'.format( p.get_peer().node.dpid ), p.name ) for p in VaranusTopo.__get_peered_ports( node ) )


    @staticmethod
    def __get_peered_ports( node ):
        return filter( lambda p : p.has_peer(), node.get_ports() )


    @staticmethod
    def __dump_json( f, obj, multiline=True ):
        json.dump( obj, f, indent=4 if multiline else None, separators=( ',', ' : ' ), sort_keys=True )


    # ================ LOCAL CLEANING ================

    def __handle_cleanlocal( self ):
        if self.cleanlocal:
            for sw in self.switches.itervalues():
                if not sw.is_remote():
                    for p in sw.get_ports():
                        VaranusTopo.__delete_local_ovs_switch_port( p.name )
                        if p.is_virtual():
                            VaranusTopo.__delete_local_virtual_port( p.name )
                    VaranusTopo.__delete_local_ovs_switch_bridge( sw.name )
            for h in self.hosts.itervalues():
                if not h.is_remote():
                    for p in h.get_ports():
                        if p.is_virtual():
                            VaranusTopo.__delete_local_virtual_port( p.name )
            for c in self.collectors.itervalues():
                if not c.is_remote():
                    for p in c.get_ports():
                        if p.is_virtual():
                            VaranusTopo.__delete_local_virtual_port( p.name )
            return True
        else:
            return False


    @staticmethod
    def __delete_local_ovs_switch_port( portname ):
        print( '< Deleting local OVS port "' + portname + '" >' )
        subprocess.call( 'ovs-vsctl del-port ' + portname, shell=True )


    @staticmethod
    def __delete_local_ovs_switch_bridge( swname ):
        print( '< Deleting local OVS bridge "' + swname + '" >' )
        subprocess.call( 'ovs-vsctl del-br ' + swname, shell=True )


    @staticmethod
    def __delete_local_virtual_port( portname ):
        print( '< Deleting local virtual interface "' + portname + '" >' )
        subprocess.call( 'ip link del ' + portname, shell=True )



# ================ AUXILIARY CLASSES ================

class NodeUtils( object ):

    @staticmethod
    def add_local_ovs_switch( mr, sw_name, dpid, **params ):
        params['dpid'] = dpid
        params['inNamespace'] = False
        return mr.add_local_ovs_switch( sw_name, **params )


    @staticmethod
    def add_remote_ovs_switch( mr, sw_name, user, server, dpid, **params ):
        params['dpid'] = dpid
        params['inNamespace'] = False
        params['have_mininet'] = True
        return mr.add_remote_ovs_switch( sw_name, user, server, **params )


    @staticmethod
    def add_pica8_switch( mr, sw_name, user, server, dpid, **params ):
        params['dpid'] = dpid
        return mr.add_pica8_switch( sw_name, user, server, **params )


    @staticmethod
    def add_local_vir_host( mr, h_name, **params ):
        params['inNamespace'] = True
        return mr.add_local_host( h_name, **params )


    @staticmethod
    def add_local_phy_host( mr, h_name, **params ):
        params['inNamespace'] = False
        return mr.add_local_host( h_name, **params )


    @staticmethod
    def add_remote_vir_host( mr, h_name, user, server, **params ):
        params['inNamespace'] = True
        params['have_mininet'] = True
        return mr.add_remote_host( h_name, user, server, **params )


    @staticmethod
    def add_remote_phy_host( mr, h_name, user, server, **params ):
        params['inNamespace'] = False
        params['have_mininet'] = True
        return mr.add_remote_host( h_name, user, server, **params )


    @staticmethod
    def add_local_collector( mr, c_name, varanus_home, cid, **params ):
        params['inNamespace'] = False
        params['have_mininet'] = True
        return mr.add_local_varanus_collector( c_name, varanus_home, cid, **params )


    @staticmethod
    def add_remote_collector( mr, c_name, user, server, varanus_home, cid, **params ):
        params['inNamespace'] = False
        params['have_mininet'] = True
        return mr.add_remote_varanus_collector( c_name, user, server, varanus_home, cid, **params )


    @staticmethod
    def get_generic_vir_port_cfg( portnum ):
        return PortConfig( portnum, is_virtual=True )


    @staticmethod
    def get_generic_phy_port_cfg( portname, portnum ):
        return PortConfig( portnum, name=portname, is_virtual=False )


    @staticmethod
    def get_switch_to_collector_vir_port_cfg( sw_portnum ):
        return PortConfig( sw_portnum, is_virtual=True, ofport=VaranusTopo._SAMPLING_OFPORT )


    @staticmethod
    def get_switch_to_collector_phy_port_cfg( sw_portname, sw_portnum ):
        return PortConfig( sw_portnum, name=sw_portname, is_virtual=False, ofport=VaranusTopo._SAMPLING_OFPORT )


    @staticmethod
    def get_host_vir_port_cfg( h_portnum, h_port_ip ):
        return PortConfig( h_portnum, is_virtual=True, ip=h_port_ip )


    @staticmethod
    def get_host_phy_port_cfg( h_portname, h_portnum, h_port_ip, h_port_mac ):
        return PortConfig( h_portnum, name=h_portname, is_virtual=False, ip=h_port_ip, mac=h_port_mac )



class RingUtils( object ):

    @staticmethod
    def build_dpid( ringnum, num ):
        if num < 0x100000000:
            return ( ringnum * 0x100000000 ) + num
        else:
            raise ValueError( 'too many nodes' )



class RingLocalSwitchBuilder( object ):

    def __init__( self, mr ):
        self.mr = mr

    def build_switch( self, ringnum, sw_num, sw_name, **params ):
        dpid = RingUtils.build_dpid( ringnum, sw_num )
        return NodeUtils.add_local_ovs_switch( self.mr, sw_name, dpid, **params )



class RingRemoteSwitchBuilder( object ):

    def __init__( self, mr, user, server ):
        self.mr = mr
        self.user = user
        self.server = server

    def build_switch( self, ringnum, sw_num, sw_name, **params ):
        dpid = RingUtils.build_dpid( ringnum, sw_num )
        return NodeUtils.add_remote_ovs_switch( self.mr, sw_name, self.user, self.server, dpid, **params )



class RingLinkBuilder( object ):

    def __init__( self, mr ):
        self.mr = mr

    def build_link( self, port1, port2 ):
        return self.mr.add_link( port1, port2 )



class RingPorts( object ):

    @staticmethod
    def internal_vir_1():
        return NodeUtils.get_generic_vir_port_cfg( 2 )


    @staticmethod
    def internal_vir_2():
        return NodeUtils.get_generic_vir_port_cfg( 1 )


    @staticmethod
    def ring_to_ring_vir():
        return NodeUtils.get_generic_vir_port_cfg( 4 )


    @staticmethod
    def ring_to_collector_vir():
        return NodeUtils.get_switch_to_collector_vir_port_cfg( 3 )


    @staticmethod
    def ring_to_host_vir():
        return NodeUtils.get_generic_vir_port_cfg( 5 )
