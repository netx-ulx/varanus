from mininet.log import info, setLogLevel
from mininet.net import Mininet
from mininet.node import NullController

from varanuspy.cli import get_cmds as get_cli_cmds
from varanuspy.lib import CLIConfig, NetworkControllerConfig, LocalSDNControllerConfig, RemoteSDNControllerConfig, \
    CustomLocalControllerConfig, CustomRemoteControllerConfig, NullControllerConfig, LocalHostConfig, RemoteHostConfig, \
    LocalCollectorConfig, RemoteCollectorConfig, CustomLocalHostConfig, CustomRemoteHostConfig, LocalOVSSwitchConfig, \
    RemoteOVSSwitchConfig, Pica8SwitchConfig, NullNodeConfig, NullPortConfig, PhyLinkConfig, VirLinkConfig, \
    NullLinkConfig
from varanuspy.utils import some, as_bool, newline, as_str, check_duplicate


class MininetRunner( object ):
    """ Utility class that abstracts controller/host/switch/link
        additions and Mininet startup/interaction/shutdown. It also allows
        adding custom commands to be available when interacting with the Mininet
        console"""

    def __init__( self, of_version='14', autoarp=False, enable_varanus=True, verbosity='info' ):
        """ Construct a new MininetRunner with specific arguments.
            - of_version    : The OpenFlow version to use (default is 14)
            - autoarp       : If true, each host will be automatically configured
                              with ARP entries for every other host
            - enable_varanus: If true, VARANUS SDN controllers and collectors
                              will be enabled
            - verbosity     : The level of verbosity
        """
        self.of_version = 'OpenFlow' + some( of_version )
        setLogLevel( some( verbosity ) )

        self.enable_varanus = as_bool( enable_varanus, name='enable_varanus' )

        self.cli = CLIConfig()
        self.controllers = {}
        self.hosts = {}
        self.switches = {}
        self.links = []

        self.built = False
        self.started = False
        self.net = Mininet( build=False, \
                            autoStaticArp=as_bool( autoarp, name='autoarp' ), \
                            controller=NullController )

        self._preconfig()


    def _preconfig( self ):
        """ Automatic configuration at the end of the object initialization.
        """
        for name, cmd in get_cli_cmds().iteritems():
            self.add_console_command( name, cmd )


    def build( self ):
        """ Builds the Mininet objects for controllers, nodes and links.
        """
        if self.built:
            raise RuntimeError( 'can only call build() method once' )
        else:
            info( newline( '=== Building controllers...' ) )
            for c in self.controllers.itervalues():
                c.build( self.net )

            info( newline( '=== Building hosts...' ) )
            for h in self.hosts.itervalues():
                h.build( self.net )

            info( newline( '=== Building switches...' ) )
            for s in self.switches.itervalues():
                s.build( self.net )

            info( newline( '=== Building links...' ) )
            for l in self.links:
                l.build( self.net )

            self.built = True


    def start( self ):
        """ Starts the Mininet object with the current topology (if not already
            started).
        """
        if not self.built:
            raise RuntimeError( 'can only start after calling build() method once' )
        elif not self.started:
            info( newline() )
            info( newline( newline( '=== Starting Mininet...' ) ) )
            self.net.start()
            self.started = True


    def interact( self ):
        """ Starts the Mininet object (if not started already), launches a
            console to interact with user and shutdowns Mininet when done.
        """
        self.start()

        info( newline() )
        info( newline( newline( '=== Interacting now with Mininet ===' ) ) )
        result = self.cli.run_command_line( self.net )

        info( newline() )
        info( newline( newline( '=== Stopping Mininet...' ) ) )
        self.net.stop()

        return result


    def get_controller( self, name ):
        return self.controllers.get( as_str( name ) )


    def get_host( self, name ):
        return self.hosts.get( as_str( name ) )


    def get_switch( self, name ):
        return self.switches.get( as_str( name ) )


    def add_console_command( self, name, cmd ):
        """ Adds a named command to be available to call when interacting with
            Mininet.
            - name: the name of the command
            - cmd : the function to be called when the command is executed
        """
        name = as_str( name )

        info ( newline( '=== Registered console command', '"' + name + '"' ) )
        self.cli.register_command( name, cmd )


    def add_network_controller( self, name, **params ):
        """ Adds a network controller to the current topology and returns a
            NetworkControllerConfig object representing it.
            - name: a textual representation of the controller
        """
        if self.built:
            raise RuntimeError( 'build() method was already called; cannot add any more controllers' )

        name = as_str( name )
        storage = self.controllers
        check_duplicate( storage, name )

        c = self._new_node( NetworkControllerConfig, name, **params )
        storage[ name ] = c

        info( newline( '=== Added network controller', c ) )
        return c


    def add_local_varanus_sdncontroller( self, name, varanus_home, sudo=None, **params ):
        """ Adds a local VARANUS SDN controller to the current topology and returns a
            LocalSDNControllerConfig object representing it, or a NullControllerConfig
            if VARANUS usage is disabled.
            - name        : a textual representation of the controller
            - varanus_home: home directory of varanus project
        """
        if self.enable_varanus:
            if self.built:
                raise RuntimeError( 'build() method was already called; cannot add any more controllers' )

            name = as_str( name )
            storage = self.controllers
            check_duplicate( storage, name )

            c = self._new_node( LocalSDNControllerConfig, name, \
                                varanus_home=varanus_home, sudo=sudo, \
                                **params )
            storage[ name ] = c

            info( newline( '=== Added local VARANUS SDN controller', c ) )
            return c
        else:
            return self.add_null_controller( name )


    def add_remote_varanus_sdncontroller( self, name, user, server, varanus_home, sudo=None, **params ):
        """ Adds a remote VARANUS SDN controller to the current topology and returns a
            RemoteSDNControllerConfig object representing it, or a NullControllerConfig
            if VARANUS usage is disabled.
            - name        : a textual representation of the controller
            - user        : the name of the user on the remote server
            - server      : the IP address of the remote server
            - varanus_home: home directory of varanus project
        """
        if self.enable_varanus:
            if self.built:
                raise RuntimeError( 'build() method was already called; cannot add any more controllers' )

            name = as_str( name )
            storage = self.controllers
            check_duplicate( storage, name )

            c = self._new_remote_node( RemoteSDNControllerConfig, name, user, server, \
                                       varanus_home=varanus_home, sudo=sudo, \
                                       **params )
            storage[ name ] = c

            info( newline( '=== Added remote VARANUS SDN controller', c ) )
            return c
        else:
            return self.add_null_controller( name )


    def add_custom_local_controller( self, name, startcmd, stopcmd, morecmds=None, \
                                     **params ):
        """ Adds a custom local controller to the current topology and returns
            a CustomLocalControllerConfig object representing it.
            - name    : a textual representation of the controller
            - startcmd: a function used to start the controller when called
            - stopcmd : a function used to stop the controller when called
            - morecmds: additional optional commands to define in the Mininet
                        node
        """
        if self.built:
            raise RuntimeError( 'build() method was already called; cannot add any more controllers' )

        name = as_str( name )
        storage = self.controllers
        check_duplicate( storage, name )

        c = self._new_node( CustomLocalControllerConfig, name, \
                            startcmd=startcmd, stopcmd=stopcmd, morecmds=morecmds, \
                            **params )
        storage[ name ] = c

        info( newline( '=== Added custom local controller', c ) )
        return c


    def add_custom_remote_controller( self, name, user, server, \
                                      startcmd, stopcmd, morecmds=None, \
                                      **params ):
        """ Adds a custom remote controller to the current topology and returns
            a CustomRemoteControllerConfig object representing it.
            - name    : a textual representation of the controller
            - user    : the name of the user on the remote server
            - server  : the IP address of the remote server
            - startcmd: a function used to start the controller when called
            - stopcmd : a function used to stop the controller when called
            - morecmds: additional optional commands to define in the Mininet
                        node
        """
        if self.built:
            raise RuntimeError( 'build() method was already called; cannot add any more controllers' )

        name = as_str( name )
        storage = self.controllers
        check_duplicate( storage, name )

        c = self._new_remote_node( CustomRemoteControllerConfig, name, user, server, \
                                   startcmd=startcmd, stopcmd=stopcmd, morecmds=morecmds, \
                                   **params )
        storage[ name ] = c

        info( newline( '=== Added custom remote controller', c ) )
        return c


    def add_null_controller( self, name, **params ):
        """ Adds a dummy non-existent controller to the current topology and
            returns a NullControllerConfig object representing it.
            - name: a textual representation of the controller
        """
        if self.built:
            raise RuntimeError( 'build() method was already called; cannot add any more controllers' )

        name = as_str( name )
        storage = self.controllers
        check_duplicate( storage, name )

        c = self._new_node( NullControllerConfig, name, **params )
        storage[ name ] = c

        info( newline( '=== Added null controller', c ) )
        return c


    def add_local_host( self, name, **params ):
        """ Adds a new local host to the current topology and returns a
            LocalHostConfig object representing it.
            - name: a textual representation of the host 
        """
        if self.built:
            raise RuntimeError( 'build() method was already called; cannot add any more hosts' )

        name = as_str( name )
        storage = self.hosts
        check_duplicate( storage, name )

        h = self._new_node( LocalHostConfig, name, **params )
        storage[ name ] = h

        info( newline( '=== Added local host', h ) )
        return h


    def add_remote_host( self, name, user, server, **params ):
        """ Adds a new remote host to the current topology and returns a
            RemoteHostConfig object representing it.
            - name  : a textual representation of the host
            - user  : the name of the user on the remote server
            - server: the IP address of the remote server
        """
        if self.built:
            raise RuntimeError( 'build() method was already called; cannot add any more hosts' )

        name = as_str( name )
        storage = self.hosts
        check_duplicate( storage, name )

        h = self._new_remote_node( RemoteHostConfig, name, user, server, **params )
        storage[ name ] = h

        info( newline( '=== Added remote host', h ) )
        return h


    def add_local_varanus_collector( self, name, varanus_home, cid, **params ):
        """ Adds a local VARANUS collector to the current topology and returns a
            LocalCollectorConfig object representing it, or a NullNodeConfig
            if VARANUS usage is disabled.
            - name        : a textual representation of the collector
            - varanus_home: home directory of varanus project
            - cid         : the collector identifier
        """
        if self.enable_varanus:
            if self.built:
                raise RuntimeError( 'build() method was already called; cannot add any more collectors' )

            name = as_str( name )
            storage = self.hosts
            check_duplicate( storage, name )

            c = self._new_node( LocalCollectorConfig, name, \
                                varanus_home=varanus_home, cid=cid, \
                                **params )
            storage[ name ] = c

            info( newline( '=== Added local VARANUS collector', c ) )
            return c
        else:
            return self.add_null_host( name )


    def add_remote_varanus_collector( self, name, user, server, varanus_home, cid, **params ):
        """ Adds a remote VARANUS collector to the current topology and returns a
            RemoteCollectorConfig object representing it, or a NullNodeConfig
            if VARANUS usage is disabled.
            - name        : a textual representation of the collector
            - user        : the name of the user on the remote server
            - server      : the IP address of the remote server
            - varanus_home: home directory of varanus project
            - cid         : the collector identifier
        """
        if self.enable_varanus:
            if self.built:
                raise RuntimeError( 'build() method was already called; cannot add any more collectors' )

            name = as_str( name )
            storage = self.hosts
            check_duplicate( storage, name )

            c = self._new_remote_node( RemoteCollectorConfig, name, user, server, \
                                       varanus_home=varanus_home, cid=cid, \
                                       **params )
            storage[ name ] = c

            info( newline( '=== Added remote VARANUS collector', c ) )
            return c
        else:
            return self.add_null_host( name )


    def add_custom_local_host( self, name, startcmd, stopcmd, morecmds=None, \
                               **params ):
        """ Adds a new custom local host to the current topology and returns a
            CustomLocalHostConfig object representing it.
            - name    : a textual representation of the host
            - startcmd: a function used to start the host when called
            - stopcmd : a function used to stop the host when called
            - morecmds: additional optional commands to define in the Mininet
                        node
        """
        if self.built:
            raise RuntimeError( 'build() method was already called; cannot add any more hosts' )

        name = as_str( name )
        storage = self.hosts
        check_duplicate( storage, name )

        h = self._new_node( CustomLocalHostConfig, name, \
                            startcmd=startcmd, stopcmd=stopcmd, morecmds=morecmds, \
                            **params )
        storage[ name ] = h

        info( newline( '=== Added custom local host', h ) )
        return h


    def add_custom_remote_host( self, name, user, server, \
                                startcmd, stopcmd, morecmds=None, \
                                **params ):
        """ Adds a custom remote host to the current topology and returns a
            CustomRemoteHostConfig object representing it.
            - name    : a textual representation of the host
            - user    : the name of the user on the remote server
            - server  : the IP address of the remote server
            - startcmd: a function used to start the host when called
            - stopcmd : a function used to stop the host when called
            - morecmds: additional optional commands to define in the Mininet
                        node
        """
        if self.built:
            raise RuntimeError( 'build() method was already called; cannot add any more hosts' )

        name = as_str( name )
        storage = self.hosts
        check_duplicate( storage, name )

        h = self._new_remote_node( CustomRemoteHostConfig, name, user, server, \
                                   startcmd=startcmd, stopcmd=stopcmd, morecmds=morecmds, \
                                   **params )
        storage[ name ] = h

        info( newline( '=== Added custom remote host', h ) )
        return h


    def add_null_host( self, name, **params ):
        """ Adds a dummy non-existent host to the current topology and returns a
            NullNodeConfig object representing it.
            - name: a textual representation of the host
        """
        if self.built:
            raise RuntimeError( 'build() method was already called; cannot add any more hosts' )

        name = as_str( name )
        storage = self.hosts
        check_duplicate( storage, name )

        h = self._new_node( NullNodeConfig, name, **params )
        storage[ name ] = h

        info( newline( '=== Added null host', h ) )
        return h


    def add_local_ovs_switch( self, name, **params ):
        """ Adds a new local OVS switch to the current topology and returns a
            LocalOVSSwitchConfig object representing it.
            - name: a textual representation of the switch
        """
        if self.built:
            raise RuntimeError( 'build() method was already called; cannot add any more switches' )

        name = as_str( name )
        storage = self.switches
        check_duplicate( storage, name )

        s = self._new_node( LocalOVSSwitchConfig, name, \
                            protocols=self.of_version, **params )
        storage[ name ] = s

        info( newline( '=== Added local OVS switch', s ) )
        return s


    def add_remote_ovs_switch( self, name, user, server, **params ):
        """ Adds a new remote OVS switch to the current topology and returns a
            RemoteOVSSwitchConfig object representing it.
            - name  : a textual representation of the switch
            - user  : the name of the user on the remote server
            - server: the IP address of the remote server
        """
        if self.built:
            raise RuntimeError( 'build() method was already called; cannot add any more switches' )

        name = as_str( name )
        storage = self.switches
        check_duplicate( storage, name )

        s = self._new_remote_node( RemoteOVSSwitchConfig, name, user, server, \
                                   protocols=self.of_version, \
                                   **params )
        storage[ name ] = s

        info( newline( '=== Added remote OVS switch', s ) )
        return s


    def add_pica8_switch( self, name, user, server, **params ):
        """ Adds a new pica8 switch (remote) to the current topology and returns
            a Pica8SwitchConfig object representing it.
            - name  : a textual representation of the switch
            - user  : the name of the user on the remote server
            - server: the IP address of the remote server
        """
        if self.built:
            raise RuntimeError( 'build() method was already called; cannot add any more switches' )

        name = as_str( name )
        storage = self.switches
        check_duplicate( storage, name )

        s = self._new_remote_node( Pica8SwitchConfig, name, user, server, \
                                   protocols=self.of_version, \
                                   **params )
        storage[ name ] = s

        info( newline( '=== Added Pica8 switch', s ) )
        return s


    def add_null_switch( self, name, **params ):
        """ Adds a dummy non-existent switch to the current topology and returns
            a NullNodeConfig object representing it.
            - name: a textual representation of the switch
        """
        if self.built:
            raise RuntimeError( 'build() method was already called; cannot add any more switches' )

        name = as_str( name )
        storage = self.switches
        check_duplicate( storage, name )

        s = self._new_node( NullNodeConfig, name, **params )
        storage[ name ] = s

        info( newline( '=== Added null switch', s ) )
        return s


    def _new_node( self, node_cls, name, **params ):
        return node_cls( name, **params )


    def _new_remote_node( self, node_cls, name, user, server, **params ):
        return node_cls( name, user, server, **params )


    def add_link( self, port1, port2, **params ):
        """ Adds a new link to the current topology and returns a LinkConfig object
            representing it. If any of the ports is a null port then this method
            returns a NullLinkConfig object.
            - port1: a PortConfig object representing the port of the 1st node
            - port2: a PortConfig object representing the port of the 2nd node
        """
        if self.built:
            raise RuntimeError( 'build() method was already called; cannot add any more links' )

        if isinstance( port1, NullPortConfig ) or isinstance( port2, NullPortConfig ):
            return NullLinkConfig( port1, port2, **params )

        if port1.is_virtual() and port2.is_virtual():
            link_cls = VirLinkConfig
        elif not ( port1.is_virtual() or port2.is_virtual() ):
            link_cls = PhyLinkConfig
        else:
            raise RuntimeError( "link between virtual port and physical port is not supported" )

        l = link_cls( port1, port2, **params )
        self.links.append( l )

        info( newline( '=== Added link', l ) )
        return l


    def forward_OVS_ports( self, sw, src, dest ):
        if not isinstance( sw, LocalOVSSwitchConfig ) and \
           not isinstance( sw, RemoteOVSSwitchConfig ):
            sw = self.switches[ sw ]

        sw.forward_ports( src, dest )
        info( newline( '=== Forwarded OVS ports in switch', sw, \
              '-', src, '-->', dest ) )
