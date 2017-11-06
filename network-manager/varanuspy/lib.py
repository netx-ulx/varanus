from functools import partial
from mininet.cli import CLI
from mininet.link import Intf, Link
from mininet.log import warn, output
from mininet.net import Mininet
import os

from varanuspy.minadapter import NetworkController, CustomLocalController, CustomRemoteController, LocalHost, \
    LocalHiddenHost, RemoteHost, RemoteHiddenHost, RemoteUnmanagedHost, RemoteUnmanagedHiddenHost, CustomLocalHost, \
    CustomLocalHiddenHost, CustomRemoteHost, CustomRemoteHiddenHost, LocalOVSSwitch, RemoteOVSSwitch, VirIntf, PhyIntf, \
    OVSVirIntf, OVSPhyIntf, VirLink, PhyLink
from varanuspy.utils import as_str, as_callable, is_some, resolve, as_int, check_duplicate, as_bool, some, as_the, \
    as_oneof, fallback, as_a


################################################################################
#### Command line interface
class CLIConfig( object ):
    """ A command line interface handler """

    def __init__( self ):
        """ Constructs a new CLIConfig object.
        """
        self.cmds = {}


    def register_command( self, name, cmd ):
        """ Registers a new command to be available when command line is
            instantiated.
            - name: the name of the command
            - cmd : the function to be called upon command execution
        """
        self.cmds[ 'do_' + as_str( name ) ] = as_callable( cmd )


    def run_command_line( self, net ):
        """ Starts a new command line interface.
            - net: a Mininet object
        """
        cli = type( "CustomCLI", ( CLI, object ), self.cmds )
        return cli( net )



################################################################################
#### Base for controllers and nodes

class BaseConfig( object ):

    def __init__( self, name, **params ):
        self.name = as_str( name )
        self.params = params
        self.netnode = None


    def build( self, net ):
        self.netnode = self._build_base( net, **self.params )


    def _build_base( self, net, **params ):
        """ Override this method to create and add the base object.
        """
        raise NotImplementedError()


    def __str__( self ):
        return self.name



class VaranusUtils( object ):

    @staticmethod
    def start_daemon( daemon, node, dargs=None, cli=None, **_kwargs ):
        if cli:
            VaranusUtils.run_cli_cmd( cli, daemon, node, 'start', dargs=dargs )
        else:
            VaranusUtils.run_cmd( daemon, node, 'start', dargs=dargs )


    @staticmethod
    def view_daemon( daemon, node, dargs=None, cli=None, **_kwargs ):
        if cli:
            VaranusUtils.run_cli_cmd( cli, daemon, node, 'view', dargs=dargs, noecho=True )
        else:
            output( 'Ignoring call to \'view\' (not in a CLI)' )


    @staticmethod
    def stop_daemon( daemon, node, dargs=None, cli=None, **_kwargs ):
        if cli:
            VaranusUtils.run_cli_cmd( cli, daemon, node, 'stop', dargs=dargs )
        else:
            VaranusUtils.run_cmd( daemon, node, 'stop', dargs=dargs )


    @staticmethod
    def run_cmd( daemon, node, op, dargs=None ):
        if dargs:
            output( node.cmd( daemon, op, *dargs ) )
        else:
            output( node.cmd( daemon, op ) )


    @staticmethod
    def run_cli_cmd( cli, daemon, node, op, dargs=None, noecho=False ):
        cmdline = '{} {} {}'.format( node.name, daemon, op )
        if dargs and len( dargs ) > 0:
            cmdline = '{} {}'.format( cmdline, ' '.join( map( str, dargs ) ) )
        if noecho:
            cmdline = '{} {}'.format( 'noecho', cmdline )

        cli.onecmd( cmdline )


    @staticmethod
    def get_daemonizer_script( varanus_home, sudo=None ):
        daemon = os.path.join( varanus_home, \
                               'utils', \
                               'bin', \
                               'daemonizer.sh' )
        if sudo:
            daemon = 'sudo -u {} {}'.format( sudo, daemon )
        return daemon



################################################################################
#### Controllers

class ControllerConfig( BaseConfig ):
    """ A controller """

    def __init__( self, name, ip=None, port=None, **params ):
        super( ControllerConfig, self ).__init__( name, ip=ip, port=port, **params )
        self.ip = as_str( ip, name='ip' ) if is_some( ip ) else None
        self.port = as_int( port, minim=1, maxim=65535, name='port' ) if is_some( port ) else None



class NetworkControllerConfig( ControllerConfig ):
    """ A controller connectable via hostname/port """

    def __init__( self, name, hostname='localhost', ip=None, port=6553, **params ):
        # We do not support 'ip' parameter
        if is_some( ip ):
            warn( 'Ignoring \'ip\' parameter passed to network controller (use \'hostname\' instead)' )

        ip = resolve( hostname )
        port = as_int( port, minim=1, maxim=65535, name='port' )
        self.hostname = hostname
        super( NetworkControllerConfig, self ).__init__( name, ip=ip, port=port, **params )


    def _build_base( self, net, **params ):
        return net.addController( name=self.name, \
                                  controller=NetworkController, \
                                  **params )


    def __str__( self ):
        superstr = super( NetworkControllerConfig, self ).__str__()
        return '{0}@{1}:{2}'.format( superstr, self.hostname, self.port )



class CustomLocalControllerConfig( ControllerConfig ):
    """ A custom controller running locally """

    def _build_base( self, net, startcmd, stopcmd, morecmds=None, \
                           **params ):

        return net.addController( name=self.name, \
                                  controller=CustomLocalController, \
                                  startcmd=startcmd, stopcmd=stopcmd, \
                                  morecmds=morecmds, \
                                  **params )



class CustomRemoteControllerConfig( ControllerConfig ):
    """ A custom controller running remotely """

    def __init__( self, name, user, server, \
                        startcmd, stopcmd, morecmds=None, **params ):
        self.user = as_str( user )
        self.server = resolve( server )
        params[ 'user' ] = self.user
        params[ 'server' ] = self.server
        super( CustomRemoteControllerConfig, self ).__init__( name, \
                                                              startcmd=startcmd, \
                                                              stopcmd=stopcmd, \
                                                              morecmds=morecmds, \
                                                              **params )


    def _build_base( self, net, startcmd, stopcmd, morecmds=None, \
                           inNamespace=False, have_mininet=False, **params ):
        assert inNamespace == False
        assert have_mininet == False
        params['inNamespace'] = False
        params['have_mininet'] = False
        return net.addController( name=self.name, \
                                  controller=CustomRemoteController, \
                                  startcmd=startcmd, \
                                  stopcmd=stopcmd, \
                                  morecmds=morecmds, \
                                  **params )

    def __str__( self ):
        superstr = super( CustomRemoteControllerConfig, self ).__str__()
        return '{0}@{1}'.format( superstr, self.server )



class SDNControllerUtils( object ):

    @staticmethod
    def start_sdncontroller( varanus_home, daemon, node, **kwargs ):
        sdncont = SDNControllerUtils._get_sdncontroller_script( varanus_home )
        VaranusUtils.start_daemon( daemon, node, dargs=[ sdncont ], **kwargs )


    @staticmethod
    def view_sdncontroller( varanus_home, daemon, node, **kwargs ):
        sdncont = SDNControllerUtils._get_sdncontroller_script( varanus_home )
        VaranusUtils.view_daemon( daemon, node, dargs=[ sdncont ], **kwargs )


    @staticmethod
    def stop_sdncontroller( varanus_home, daemon, node, **kwargs ):
        sdncont = SDNControllerUtils._get_sdncontroller_script( varanus_home )
        VaranusUtils.stop_daemon( daemon, node, dargs=[ sdncont ], **kwargs )


    @staticmethod
    def _get_sdncontroller_script( varanus_home ):
        return os.path.join( varanus_home, \
                            'sdncontroller', \
                            'run-varanus-sdncontroller.sh' )



class LocalSDNControllerConfig( CustomLocalControllerConfig ):
    """ A VARANUS SDN controller running locally """

    def __init__( self, name, varanus_home, sudo=None, collector_port=32800, **params ):
        self.collector_port = as_int( collector_port, minim=1, maxim=65535, name='collector_port' )

        daemon = VaranusUtils.get_daemonizer_script( varanus_home, sudo=sudo )
        startcmd = partial( SDNControllerUtils.start_sdncontroller, varanus_home, daemon )
        stopcmd = partial( SDNControllerUtils.stop_sdncontroller, varanus_home, daemon )
        viewcmd = partial( SDNControllerUtils.view_sdncontroller, varanus_home, daemon )

        super( LocalSDNControllerConfig, self ).__init__( name, \
                                                          startcmd=startcmd, \
                                                          stopcmd=stopcmd, \
                                                          morecmds={'view' : viewcmd}, \
                                                          **params )



class RemoteSDNControllerConfig( CustomRemoteControllerConfig ):
    """ A VARANUS SDN controller running remotely """

    def __init__( self, name, user, server, varanus_home, sudo=None, collector_port=32800, **params ):
        self.collector_port = as_int( collector_port, minim=1, maxim=65535, name='collector_port' )

        daemon = VaranusUtils.get_daemonizer_script( varanus_home, sudo=sudo )
        startcmd = partial( SDNControllerUtils.start_sdncontroller, varanus_home, daemon )
        stopcmd = partial( SDNControllerUtils.stop_sdncontroller, varanus_home, daemon )
        viewcmd = partial( SDNControllerUtils.view_sdncontroller, varanus_home, daemon )

        super( RemoteSDNControllerConfig, self ).__init__( name, user, server, \
                                                           startcmd=startcmd, \
                                                           stopcmd=stopcmd, \
                                                           morecmds={'view' : viewcmd}, \
                                                           **params )



class NullControllerConfig( ControllerConfig ):
    """ A non-existent controller """

    def _build_base( self, _net, **_params ):
        return _MockObject()



################################################################################
#### Hosts and Switches

class NodeConfig( BaseConfig ):
    """ A host or a switch """

    def __init__( self, name, **params ):
        super( NodeConfig, self ).__init__( name, **params )
        self.ports = {}


    def _build_base( self, net, inNamespace=None, **params ):
        """ Override this method to create and add the node object.
        """
        raise NotImplementedError()


    def is_remote( self ):
        """ Override this method to specify the node as local or remote.
        """
        raise NotImplementedError()


    def get_ports( self ):
        return self.ports.values()


    def get_port( self, portnum ):
        return self.ports.get( portnum )


    def add_port( self, portnum, name=None, is_virtual=None, **params ):
        check_duplicate( self.ports, portnum )

        p = self._new_port( portnum, name=name, is_virtual=is_virtual, **params )
        self.ports[ portnum ] = p

        return p


    def _new_port( self, portnum, name=None, is_virtual=None, **params ):
        """ Override this method to return a specified PortConfig object.
        """
        raise NotImplementedError()



class LocalNodeConfig( NodeConfig ):
    """ A node running locally """

    def is_remote( self ):
        return False


    def _new_port( self, portnum, name=None, is_virtual=None, **params ):
        is_virtual = as_bool( is_virtual, name='is_virtual' )
        cls = VirPortConfig if is_virtual else PhyPortConfig
        return cls( portnum, self, name=name, **params )



class RemoteNodeConfig( NodeConfig ):
    """ A node running remotely """

    def __init__( self, name, user, server, **params ):
        self.user = as_str( user )
        self.server = as_str( server ) # resolve( server )
        params[ 'user' ] = self.user
        params[ 'server' ] = self.server
        super( RemoteNodeConfig, self ).__init__( name, **params )


    def is_remote( self ):
        return True


    def _new_port( self, portnum, name=None, is_virtual=None, **params ):
        is_virtual = as_bool( is_virtual, name='is_virtual' )
        cls = VirPortConfig if is_virtual else PhyPortConfig
        return cls( portnum, self, name=name, **params )


    def __str__( self ):
        superstr = super( RemoteNodeConfig, self ).__str__()
        return '{0}@{1}'.format( superstr, self.server )



class SimpleHostMixin( object ):
    """ A mix-in for defining a simple host (local or remote) """

    def _build_host( self, net, inNamespace=None, **params ):
        # Do not configure by default any IP or MAC
        params[ 'ip' ] = None
        params[ 'mac' ] = None
        params[ 'inNamespace' ] = as_bool( inNamespace, name='inNamespace' )
        return net.addHost( self.name, **params )



class CustomHostMixin( object ):
    """ A mix-in for defining a custom host (local or remote) """

    def _build_host( self, net, startcmd, stopcmd, morecmds=None, inNamespace=None, \
                     **params ):

        # Do not configure by default any IP or MAC
        params[ 'ip' ] = None
        params[ 'mac' ] = None
        params[ 'inNamespace' ] = as_bool( inNamespace, name='inNamespace' )
        return net.addHost( self.name, startcmd=startcmd, stopcmd=stopcmd, \
                            morecmds=morecmds, **params )



class CollectorUtils( object ):

    @staticmethod
    def start_collector( varanus_home, daemon, cid, node, **kwargs ):
        collect = CollectorUtils._get_collector_script( varanus_home )
        VaranusUtils.start_daemon( daemon, node, dargs=[ collect, '-id', cid ], **kwargs )


    @staticmethod
    def view_collector( varanus_home, daemon, cid, node, **kwargs ):
        collect = CollectorUtils._get_collector_script( varanus_home )
        VaranusUtils.view_daemon( daemon, node, dargs=[ collect, '-id', cid ], **kwargs )


    @staticmethod
    def stop_collector( varanus_home, daemon, cid, node, **kwargs ):
        collect = CollectorUtils._get_collector_script( varanus_home )
        VaranusUtils.stop_daemon( daemon, node, dargs=[ collect, '-id', cid ], **kwargs )


    @staticmethod
    def _get_collector_script( varanus_home ):
        return os.path.join( varanus_home, \
                            'collector', \
                            'run-varanus-collector.sh' )



class OVSSwitchMixin( object ):
    """ A mix-in for defining an OVS switch (local or remote or pica8) """

    def __init__( self, name, dpid=None, **params ):
        super( OVSSwitchMixin, self ).__init__( name, dpid=dpid, **params )
        self.dpid = dpid


    def _build_switch( self, net, dpid=None, inNamespace=None, **params ):
        if is_some( dpid ):
            params['dpid'] = '{:x}'.format( dpid )
        params['inNamespace'] = as_bool( inNamespace, name='inNamespace' )
        return net.addSwitch( self.name, **params )


    def add_flow_rule( self, *rule ):
        ofver = some( self.netnode.protocols )
        return self.netnode.cmd( 'ovs-ofctl', '-O', ofver, 'add-flow', self.name, *rule )


    def forward_ports( self, src, dest ):

        def as_valid_portnum( port, self ):
            if isinstance( port, PortConfig ):
                as_the( port.node, self )
                return port.num
            else:
                return as_oneof( as_int( port ), self.ports )

        src = as_valid_portnum( src, self )
        dest = as_valid_portnum( dest, self )

        rule = OVSSwitchMixin._build_forward_rule( src, dest )
        return self.add_flow_rule( rule )


    @staticmethod
    def _build_forward_rule( src, dest, table=0, priority=1 ):
        src = as_str( src )
        dest = as_str( dest )
        table = as_int( table, minim=0 )
        priority = as_int( priority, minim=0 )

        rule = 'table={0}'.format( table )
        rule += ',priority={0}'.format( priority )
        rule += ',in_port={0}'.format( src )
        rule += ',actions=output:{0}'.format( dest )
        return rule



class LocalHostConfig( SimpleHostMixin, LocalNodeConfig ):
    """ A simple host running locally """

    def _build_base( self, net, hidden=False, **params ):
        hidden = as_bool( hidden, name='hidden' )
        params[ 'cls' ] = LocalHiddenHost if hidden else LocalHost
        return self._build_host( net, **params )



class RemoteHostConfig( SimpleHostMixin, RemoteNodeConfig ):
    """ A simple host running remotely """

    def _build_base( self, net, have_mininet=None, managed=True, hidden=False, **params ):
        params['have_mininet'] = as_bool( have_mininet, name='have_mininet' )
        managed = as_bool( managed, name='managed' )
        hidden = as_bool( hidden, name='hidden' )
        if managed:
            params[ 'cls' ] = RemoteHiddenHost if hidden else RemoteHost
        else:
            params[ 'cls' ] = RemoteUnmanagedHiddenHost if hidden else RemoteUnmanagedHost
        return self._build_host( net, **params )



class CustomLocalHostConfig( CustomHostMixin, LocalNodeConfig ):
    """ A custom host running locally """

    def _build_base( self, net, startcmd, stopcmd, morecmds=None, hidden=False, \
                           **params ):

        hidden = as_bool( hidden, name='hidden' )
        params[ 'cls' ] = CustomLocalHiddenHost if hidden else CustomLocalHost
        return self._build_host( net, startcmd, stopcmd, morecmds=morecmds, \
                                 **params )



class CustomRemoteHostConfig( CustomHostMixin, RemoteNodeConfig ):
    """ A custom host running remotely """

    def _build_base( self, net, startcmd, stopcmd, morecmds=None, \
                     have_mininet=None, hidden=False, **params ):

        params['have_mininet'] = as_bool( have_mininet, name='have_mininet' )
        hidden = as_bool( hidden, name='hidden' )
        params[ 'cls' ] = CustomRemoteHiddenHost if hidden else CustomRemoteHost
        return self._build_host( net, startcmd, stopcmd, morecmds=morecmds, \
                                 **params )



class LocalCollectorConfig( CustomLocalHostConfig ):
    """ A VARANUS collector running locally """

    def __init__( self, name, varanus_home, cid, **params ):
        self.cid = cid
        daemon = VaranusUtils.get_daemonizer_script( varanus_home )
        startcmd = partial( CollectorUtils.start_collector, varanus_home, daemon, cid )
        stopcmd = partial( CollectorUtils.stop_collector, varanus_home, daemon, cid )
        viewcmd = partial( CollectorUtils.view_collector, varanus_home, daemon, cid )

        super( LocalCollectorConfig, self ).__init__( name, \
                                                    startcmd=startcmd, \
                                                    stopcmd=stopcmd, \
                                                    morecmds={'view' : viewcmd}, \
                                                    **params )



class RemoteCollectorConfig( CustomRemoteHostConfig ):
    """ A VARANUS collector running remotely """

    def __init__( self, name, user, server, varanus_home, cid, **params ):
        self.cid = cid
        daemon = VaranusUtils.get_daemonizer_script( varanus_home )
        startcmd = partial( CollectorUtils.start_collector, varanus_home, daemon, cid )
        stopcmd = partial( CollectorUtils.stop_collector, varanus_home, daemon, cid )
        viewcmd = partial( CollectorUtils.view_collector, varanus_home, daemon, cid )

        super( RemoteCollectorConfig, self ).__init__( name, user, server, \
                                                       startcmd=startcmd, \
                                                       stopcmd=stopcmd, \
                                                       morecmds={'view' : viewcmd}, \
                                                       **params )



class LocalOVSSwitchConfig( OVSSwitchMixin, LocalNodeConfig ):
    """ An OVS switch running locally """

    def _build_base( self, net, **params ):
        params[ 'cls' ] = LocalOVSSwitch
        return self._build_switch( net, **params )


    def _new_port( self, portnum, name=None, is_virtual=None, **params ):
        is_virtual = as_bool( is_virtual, name='is_virtual' )
        cls = VirOVSPortConfig if is_virtual else PhyOVSPortConfig
        return cls( portnum, self, name=name, **params )



class RemoteOVSSwitchConfig( OVSSwitchMixin, RemoteNodeConfig ):
    """ An OVS switch running remotely """

    def _build_base( self, net, have_mininet=None, **params ):
        params['have_mininet'] = as_bool( have_mininet, name='have_mininet' )
        params['cls'] = RemoteOVSSwitch
        return self._build_switch( net, **params )


    def _new_port( self, portnum, name=None, is_virtual=None, **params ):
        is_virtual = as_bool( is_virtual, name='is_virtual' )
        cls = VirOVSPortConfig if is_virtual else PhyOVSPortConfig
        return cls( portnum, self, name=name, **params )



class Pica8SwitchConfig( OVSSwitchMixin, RemoteNodeConfig ):
    """ An OVS switch running in a remote pica8 switch """

    path = '/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:/usr/local/games:/usr/games:/ovs/bin:/ovs/sbin:/pica/bin'

    def _build_base( self, net, inNamespace=False, have_mininet=False, **params ):
        assert inNamespace == False
        assert have_mininet == False
        params['inNamespace'] = False
        params['have_mininet'] = False
        params['cls'] = RemoteOVSSwitch
        params['datapath_type'] = 'pica8'
        params['cmd_prefix'] = [ 'env', 'PATH={}'.format( Pica8SwitchConfig.path ) ]
        params['lo'] = ''
        return self._build_switch( net, **params )


    def _new_port( self, portnum, is_virtual=False, tagged=False, **params ):
        assert is_virtual != True
        return Pica8PortConfig( portnum, self, tagged=tagged, **params )



class NullNodeConfig( NodeConfig ):
    """ A non-existent node """

    def _build_base( self, _net, **_params ):
        return _MockObject()


    def is_remote( self ):
        return False


    def _new_port( self, portnum, name=None, **_params ):
        return NullPortConfig( portnum, self, name=name )



################################################################################
#### Node ports

class PortConfig( object ):
    """ A node port """

    def __init__( self, num, node, name=None, cls=VirIntf, mac=None, ip=None, \
                  **params ):
        """ Constructs a new PortConfig object.
            - num : the port number
            - node: the node that owns this port
            - name: the port name (optional)
            - cls : the interface class (default is VirIntf)
            - mac : the MAC address for the interface
            - ip  : the IP address for the interface
        """
        self.num = as_int( num )
        self.node = some( node )
        self.name = as_str( fallback( name, '{0}-e{1}'.format( node.name, num ) ) )
        self.cls = as_a( some( cls ), subclass_of=Intf )
        self.mac = mac
        self.ip = ip
        params['ip'] = ip
        self.params = params
        self._peer = None # linked PortConfig


    def is_virtual( self ):
        """ Override this method.
        """
        raise NotImplementedError()


    def build( self ):
        """ Override this method.
        """
        raise NotImplementedError()


    def has_peer( self ):
        return is_some( self._peer )


    def get_peer( self ):
        return self._peer


    def set_peer( self, peer ):
        if is_some( self._peer ):
            raise RuntimeError( 'a peer already exists' )
        else:
            self._peer = as_a( peer, instance_of=PortConfig, name='peer' )


    def __str__( self ):
        return '[{0}]@({1})'.format( self.name, self.node )


    @staticmethod
    def _get_mac( node, name ):
        cmd = 'ip addr show {0}'.format( name )
        cmd += r" | awk '/link\/ether/ { print $2 }'"
        return node.netnode.cmd( cmd )



class VirPortConfig( PortConfig ):
    """ A virtual node port """

    def __init__( self, num, node, **params ):
        params[ 'cls' ] = VirIntf
        params.setdefault( 'mac', Mininet.randMac() )
        super( VirPortConfig, self ).__init__( num, node, **params )

    def is_virtual( self ):
        return True

    def build( self ):
        # No need to do anything at this stage
        pass



class PhyPortConfig( PortConfig ):
    """ A physical node port """

    def __init__( self, num, node, name, **params ):
        params[ 'name' ] = as_str( name )
        params[ 'cls' ] = PhyIntf
        super( PhyPortConfig, self ).__init__( num, node, **params )

    def is_virtual( self ):
        return False

    def build( self ):
        if not is_some( self.mac ):
            # We only have the netnode after it is built
            self.mac = PortConfig._get_mac( self.node, self.name )



class OVSPortMixinConfig( object ):
    """ An OVS switch port """

    def __init__( self, num, node, **params ):
        params.setdefault( 'ofport', num )
        super( OVSPortMixinConfig, self ).__init__( num, node, **params )



class VirOVSPortConfig( OVSPortMixinConfig, PortConfig ):
    """ A virtual OVS switch port """

    def __init__( self, num, node, **params ):
        params[ 'cls' ] = OVSVirIntf
        params.setdefault( 'mac', Mininet.randMac() )
        super( VirOVSPortConfig, self ).__init__( num, node, **params )

    def is_virtual( self ):
        return True

    def build( self ):
        # No need to do anything at this stage
        pass



class PhyOVSPortConfig( OVSPortMixinConfig, PortConfig ):
    """ A physical OVS switch port """

    def __init__( self, num, node, name, **params ):
        params[ 'name' ] = as_str( name )
        params[ 'cls' ] = OVSPhyIntf
        super( PhyOVSPortConfig, self ).__init__( num, node, **params )

    def is_virtual( self ):
        return False

    def build( self ):
        if not is_some( self.mac ):
            # We only have the netnode after it is built
            self.mac = PortConfig._get_mac( self.node, self.name )



class Pica8PortConfig( OVSPortMixinConfig, PortConfig ):
    """ A port of a Pica8 switch """

    def __init__( self, num, node, **params ):
        name, intf_opts = Pica8PortConfig._get_props( num )
        params[ 'name' ] = name
        params[ 'cls' ] = Pica8Intf
        params[ 'intf_type' ] = 'pica8'
        params[ 'intf_opts' ] = intf_opts

        super( Pica8PortConfig, self ).__init__( num, node, **params )


    def is_virtual( self ):
        return False

    def build( self ):
        # No need to do anything at this stage
        pass


    @staticmethod
    def _get_props( num ):
        """ Returns name, intf_opts given a port number.
        """
        num = as_int( num, minim=1, maxim=52 )
        if num < 49:
            name = 'ge-1/1/' + str( num )
            intf_opts = { 'link_speed' : 'auto' }
        else:
            name = 'te-1/1/' + str( num )
            intf_opts = { 'link_speed' : '10G' }

        return name, intf_opts



class Pica8Intf( OVSPhyIntf ):
    """ A Pica8 switch interface """

    def ifconfig( self, *_args, **_kwargs ):
        # Disable ifconfig commands on Pica8
        pass


    def delete( self ):
        # Do not try to remove the physical interface
        pass



class NullPortConfig( PortConfig ):
    """ A non-existent port """

    def is_virtual( self ):
        return False



################################################################################
#### Links

class LinkConfig( object ):
    """ A link between two nodes """

    def __init__( self, port1, port2, **params ):
        port1 = as_a( port1, instance_of=PortConfig )
        if port1.has_peer():
            raise ValueError( 'port1 is already linked' )
        port2 = as_a( port2, instance_of=PortConfig )
        if port2.has_peer():
            raise ValueError( 'port2 is already linked' )

        port1.set_peer( port2 )
        port2.set_peer( port1 )

        self.node1 = port1.node
        self.port1 = port1
        self.node2 = port2.node
        self.port2 = port2
        self.params = params

        self.netlink = None


    def build( self, net ):
        self.port1.build()
        self.port2.build()
        self.netlink = self._build_link( net, **self.params )


    def _build_link( self, net, **params ):
        """ Override this method to create and add the link object.
        """
        raise NotImplementedError()


    def __str__( self ):
        return '{0} <--> {1}'.format( self.port1, self.port2 )



class CommonLinkMixin( object ):
    """ Common link structure """

    def _build_common_link( self, net, cls=Link, **params ):
        p1, p2 = self.port1, self.port2

        params[ 'port1' ] = p1.num
        params[ 'port2' ] = p2.num
        params[ 'cls' ] = some( cls )
        params[ 'intfName1' ] = p1.name
        params[ 'intfName2' ] = p2.name
        params[ 'addr1' ] = p1.mac
        params[ 'addr2' ] = p2.mac
        params[ 'cls1' ] = p1.cls
        params[ 'cls2' ] = p2.cls
        params[ 'params1' ] = p1.params
        params[ 'params2' ] = p2.params

        n1, n2 = self.node1, self.node2
        return net.addLink( n1.name, n2.name, **params )



class VirLinkConfig( CommonLinkMixin, LinkConfig ):
    """ A link between two virtual ports """

    def _build_link( self, net, **params ):
        params[ 'cls' ] = VirLink
        return self._build_common_link( net, **params )



class PhyLinkConfig( CommonLinkMixin, LinkConfig ):
    """ A link between two physical ports """

    def _build_link( self, net, **params ):
        params[ 'cls' ] = PhyLink
        return self._build_common_link( net, **params )



class NullLinkConfig( LinkConfig ):
    """ A non-existent link """

    def _build_link( self, _net, **_params ):
        return _MockObject()



################################################################################
#### Common utils

class _MockObject( object ):

    def __getattr__( self, _name ):
        return _MockObject()

    def __call__( self, *_args, **_kwargs ):
        return _MockObject()
