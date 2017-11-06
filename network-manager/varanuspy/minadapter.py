from itertools import groupby
from mininet.link import Intf, Link
from mininet.log import debug, error, info, warn
from mininet.node import Controller, Host, Node, OVSSwitch, RemoteController
from operator import attrgetter
import os
import pty
import select
from subprocess import Popen, PIPE, STDOUT

from varanuspy.utils import as_bool, as_callable, is_some, is_mapping, as_str, is_iterable, as_a, is_somestr, \
    newline, optional_str, optional_int, resolve, some


################################################################################
#### Adapted nodes (controllers, hosts and switches)

class NodeMixin( object ):
    """ A mix-in with adapted managed node methods """

    # We have access to the node and can run commands in it
    def is_managed( self ):
        return True


    # Overriden method (calls super).
    #
    # NOTE: fix to prevent setting ARP with null IP/MAC.
    #
    def setARP( self, ip, mac ):
        if ip and mac:
            return super( NodeMixin, self ).setARP( ip, mac )


    # Overriden method (calls super).
    #
    # NOTE: for command debugging in writes (called by sendCmd and cmd).
    #
    def write( self, data ):
        debug( newline( '==== Shell command in node', self.name, '====' ) )
        debug( newline( data ) )
        debug( newline( '================' ) )
        return super( NodeMixin, self ).write( data )


    # Overriden method (calls super).
    #
    # NOTE: for command debugging in popens.
    #
    def _popen( self, cmd, **params ):
        debug( newline( '==== Popen in node', self.name, '====' ) )
        debug( newline( cmd ) )
        debug( newline( 'PARAMS:', dict( **params ) ) )
        debug( newline( '================' ) )
        return super( NodeMixin, self )._popen( cmd, **params )



class LocalNodeMixin( NodeMixin ):
    """ A mix-in with adapted local node methods """

    def is_remote( self ):
        return False



class RemoteNodeMixin( NodeMixin ):
    """ A mix-in with adapted remote node methods """

    def __init__( self, *args, **kwargs ):
        """
        In kwargs:
        - user        : ssh session user name
        - server      : ssh remote server hostname/address
        - inNamespace : boolean indicating if in network namespace (must only be True when have_mininet is also True)
        - have_mininet: boolean indicating if the remote node has Mininet
        """
        user = some( kwargs['user'], name='user' )
        server = some( kwargs['server'], name='server' )
        inNamespace = as_bool( kwargs['inNamespace'], name='inNamespace' )
        have_mininet = as_bool( kwargs['have_mininet'], name='have_mininet' )
        if inNamespace and not have_mininet:
            raise ValueError( 'remote node must have Mininet in order to be in a separate network namespace' )

        # Validate server hostname/address
        resolve( server )

        self.user = user
        self.server = server
        self.have_mininet = have_mininet


        sshprefix = [ 'ssh', \
                      '-q', \
                      '-o', 'BatchMode=yes', \
                      '-o', 'ForwardAgent=yes' ]
        sshdest = '{}@{}'.format( self.user, self.server )
        self.sshcmd = sshprefix + [ sshdest ]
        self.sshcmd_tt = sshprefix + [ '-tt', sshdest ]

        if 'cmd_prefix' in kwargs:
            cmd_prefix = kwargs['cmd_prefix']
            if is_some( cmd_prefix ):
                self.sshcmd += cmd_prefix
                self.sshcmd_tt += cmd_prefix

        super( RemoteNodeMixin, self ).__init__( *args, **kwargs )


    def is_remote( self ):
        return True


    # Copied from mininet.examples.cluster.RemoteMixin.
    #
    def rpopen( self, *cmd, **opts ):
        params = { 'stdin': PIPE,
                   'stdout': PIPE,
                   'stderr': STDOUT }
        params.update( opts )
        return self._popen( *cmd, **params )


    # Copied from mininet.examples.cluster.RemoteMixin.
    #
    def rcmd( self, *cmd, **opts ):
        popen = self.rpopen( *cmd, **opts )
        # print 'RCMD: POPEN:', popen
        # These loops are tricky to get right.
        # Once the process exits, we can read
        # EOF twice if necessary.
        result = ''
        while True:
            poll = popen.poll()
            result += popen.stdout.read()
            if poll is not None:
                break
        return result


    # Overriden method (does not call super).
    #
    # NOTES:
    # - We perform the same thing as in Node, except we adjust the shell command depending on whether the remote node
    #   has Mininet or not (we remove the mnexec command from the prefix if Mininet is not present).
    # - We copy the strategy from mininet.examples.cluster.RemoteMixin to exclude the '-d' option from mnexec (when
    #   applicable) and to get the PID of the remote process.
    #
    def startShell( self, **_kwargs ):
        "Start a shell process for running commands"
        if self.shell:
            error( "%s: shell is already running\n" % self.name )
            return

        if self.have_mininet:
            # mnexec: (c)lose descriptors, run in (n)amespace
            opts = '-c'
            if self.inNamespace:
                opts += 'n'
            # bash -i: force interactive
            # -s: pass $* to shell, and make process easy to find in ps
            # prompt is set to sentinel chr( 127 )
            cmd = [ 'mnexec', opts, 'env', 'PS1=' + chr( 127 ),
                    'bash', '--norc', '-is', 'mininet:' + self.name ]
        else:
            cmd = [ 'env', 'PS1=' + chr( 127 ),
                    'bash', '--norc', '-is', 'mininet:' + self.name ]

        # Spawn a shell subprocess in a pseudo-tty, to disable buffering
        # in the subprocess and insulate it from signals (e.g. SIGINT)
        # received by the parent
        master, slave = pty.openpty()
        self.shell = self._popen( cmd, stdin=slave, stdout=slave, stderr=slave,
                                  close_fds=False )
        self.stdin = os.fdopen( master, 'rw' )
        self.stdout = self.stdin
        self.pid = self.shell.pid
        self.pollOut = select.poll()
        self.pollOut.register( self.stdout )
        # Maintain mapping between file descriptors and nodes
        # This is useful for monitoring multiple nodes
        # using select.poll()
        self.outToNode[ self.stdout.fileno() ] = self
        self.inToNode[ self.stdin.fileno() ] = self
        self.execed = False
        self.lastCmd = None
        self.lastPid = None
        self.readbuf = ''
        # Wait for prompt
        while True:
            data = self.read( 1024 )
            if data[ -1 ] == chr( 127 ):
                break
            self.pollOut.poll()
        self.waiting = False
        # +m: disable job control notification
        self.cmd( 'unset HISTFILE; stty -echo; set +m' )

        # Get the remote process PID
        self.sendCmd( 'echo $$' )
        self.pid = int( self.waitOutput() )


    # Overriden method (calls super).
    #
    # NOTES:
    # - Before calling the super method we remove the mnexec command from the prefix if Mininet is not present.
    # - Also, like in mininet.examples.cluster.RemoteMixin we disable -tt option in ssh command here.
    #
    def popen( self, *args, **kwargs ):
        if not self.have_mininet:
            kwargs['mncmd'] = []
        kwargs['tt'] = False
        return super( RemoteNodeMixin, self ).popen( *args, **kwargs )


    # Overriden method (calls super).
    #
    # NOTE: we adapt the popen command to include the ssh prefix (code is based on
    # mininet.examples.cluster.RemoteMixin).
    #
    def _popen( self, cmd, tt=True, **params ):
        sshcmd = self.sshcmd_tt if tt else self.sshcmd
        if type( cmd ) is str:
            cmd = cmd.split()
        cmd = sshcmd + cmd

        # Shell requires a string, not a list!
        if params.get( 'shell', False ):
            cmd = ' '.join( cmd )

        params.update( preexec_fn=os.setpgrp ) # ignore all signals
        return super( RemoteNodeMixin, self )._popen( cmd, **params )


    # Overriden method (calls super).
    #
    # NOTE: like in mininet.examples.cluster.RemoteMixin we change moveIntfFn to a variant that calls the necessary
    # commands remotely instead of always in the local machine (the default behaviour).
    #
    def addIntf( self, *args, **kwargs ):
        kwargs['moveIntfFn'] = RemoteNodeMixin.__moveIntf
        return super( RemoteNodeMixin, self ).addIntf( *args, **kwargs )


    @staticmethod
    def __moveIntf( intf, node, printError=True ):
        intf = str( intf )
        cmd = 'ip link set %s netns %s' % ( intf, node.pid )
        output = node.rcmd( cmd )
        if output:
            if printError:
                error( '*** Error: moveIntf: ' + intf +
                       ' not successfully moved to ' + node.name + ':\n',
                       output )
            return False
        return True



class RemoteUnmanagedNodeMixin( object ):
    """ A remote node that we do not have SSH access to it, so nothing is ever
        executed (commands are ignored and Popen objects do nothing special and
        exit immediately).
    """

    def is_managed( self ):
        return False


    def is_remote( self ):
        return True


    # Overriden method (does not call super).
    #
    def write( self, data ):
        # do nothing
        pass


    # Overriden method (does not call super).
    #
    def startShell( self, **_kwargs ):
        # do nothing
        pass


    # Overriden method (does not call super).
    #
    def _popen( self, _cmd, **_params ):
        # return a process that exits immediately with exit code 0
        return Popen( 'true', shell=True )


    # Overriden method (does not call super).
    #
    def pexec( self, *args, **kwargs ):
        # returns out, err, exitcode
        return '', '', 0


    # Overriden method (calls super).
    #
    def addIntf( self, *args, **kwargs ):
        # don't run any command to move the interface
        kwargs['moveIntfFn'] = lambda *_args, **_kwargs : None
        return super( RemoteUnmanagedNodeMixin, self ).addIntf( *args, **kwargs )



class CustomMixin( object ):
    """ A mix-in with adapted start/stop methods with custom commands with a
        single argument (the node) and optional additional methods to be
        defined, all provided at startup """

    def __init__( self, name, startcmd, stopcmd, morecmds=None, **params ):
        super( CustomMixin, self ).__init__( name, **params )

        self.startcmd = as_callable( startcmd )
        self.stopcmd = as_callable( stopcmd )
        if is_some( morecmds ):
            if is_mapping( morecmds ):
                for method_name, cmd in morecmds.iteritems():
                    cmd = as_callable( cmd, name='cmd' )
                    method_name = as_str( method_name, name='method_name' )
                    method = lambda *args, **kwargs : cmd( self, *args, **kwargs )
                    setattr( self, method_name, method )
            elif is_iterable( morecmds ):
                for cmd in morecmds:
                    cmd = as_callable( cmd, name='cmd' )
                    method_name = as_str( cmd.__name__, name='cmd.__name__' )
                    method = lambda *args, **kwargs : cmd( self, *args, **kwargs )
                    setattr( self, method_name, method )
            else:
                raise ValueError( 'Invalid optional methods' )

        # to prevent repeated calls to start/stop
        self._is_active = False



class CustomControllerMixin( CustomMixin ):

    # Overriden method (does not call super).
    #
    def start( self, *args, **kwargs ):
        if not self._is_active:
            self.startcmd( self, *args, **kwargs )
            self._is_active = True
            # do not call Controller start method
        else:
            warn( newline( '<! Cannot start the already active controller', self.name, '!>' ) )


    # Overriden method (calls specific superclass method).
    #
    def stop( self, *args, **kwargs ):
        if self._is_active:
            self.stopcmd( self, *args, **kwargs )
            self._is_active = False
            # bypass Controller stop method
            Node.stop( self )
        else:
            warn( newline( '<! Cannot stop the inactive controller', self.name, '!>' ) )



class CustomNodeMixin( CustomMixin ):

    def start( self, *args, **kwargs ):
        if not self._is_active:
            self.startcmd( self, *args, **kwargs )
            self._is_active = True
        else:
            warn( newline( '<! Cannot start the already active node', self.name, '!>' ) )


    # Overriden method (calls super).
    #
    def stop( self, *args, **kwargs ):
        if self._is_active:
            self.stopcmd( self, *args, **kwargs )
            self._is_active = False

            deleteIntfs = kwargs.get( 'deleteIntfs' )
            if is_some( deleteIntfs ):
                super( CustomNodeMixin, self ).stop( deleteIntfs=deleteIntfs )
            else:
                super( CustomNodeMixin, self ).stop()
        else:
            warn( newline( '<! Cannot stop the inactive node', self.name, '!>' ) )


    # Overriden method (calls super).
    #
    def terminate( self ):
        # This method is called by Node.stop(), but Mininet calls this method
        # directly on hosts when exiting, so we have to run the stop command
        # here if it hasn't run before
        if self._is_active:
            self.stopcmd( self )
            self._is_active = False

        # Always call super
        super( CustomNodeMixin, self ).terminate()



class QuietController( Controller ):
    """ Quiet adapted controller methods """

    def checkListening( self ):
        """ Do nothing by default
        """
        pass


    def start( self, *_args, **_kwargs ):
        """ Do nothing by default.
        """
        pass


    def stop( self, *_args, **_kwargs ):
        """ Do nothing by default.
        """
        pass


    @classmethod
    def isAvailable( cls ):
        """ Do nothing by default.
        """
        return True



class NetworkController( NodeMixin, \
                         RemoteController ):
    """ A Mininet RemoteController with another name to avoid confusion """
    pass



class CustomLocalController( CustomControllerMixin, \
                             LocalNodeMixin, \
                             QuietController ):
    """ A custom adapted controller running locally """
    pass



class CustomRemoteController( CustomControllerMixin, \
                              RemoteNodeMixin, \
                              QuietController ):
    """ A custom adapted controller running remotely """
    pass



class VisibleHostMixin( object ):
    """ A mix-in to define visible hosts """

    def is_visible( self ):
        return True



class HiddenHostMixin( object ):
    """ A mix-in to define hidden hosts """

    def is_visible( self ):
        return False



class LocalHost( LocalNodeMixin, VisibleHostMixin, Host ):
    """ An adapted host running locally """
    pass



class LocalHiddenHost( LocalNodeMixin, HiddenHostMixin, Host ):
    """ An adapted hidden host running locally """
    pass



class RemoteHost( RemoteNodeMixin, VisibleHostMixin, Host ):
    """ An adapted host running remotely """
    pass



class RemoteHiddenHost( RemoteNodeMixin, HiddenHostMixin, Host ):
    """ An adapted hidden host running remotely """
    pass



class RemoteUnmanagedHost( RemoteUnmanagedNodeMixin, VisibleHostMixin, Host ):
    """ An adapted unmanaged host running remotely """
    pass



class RemoteUnmanagedHiddenHost( RemoteUnmanagedNodeMixin, HiddenHostMixin, Host ):
    """ An adapted unmanaged hidden host running remotely """
    pass



class CustomLocalHost( CustomNodeMixin, LocalHost ):
    """ A custom adapted host running locally """
    pass



class CustomLocalHiddenHost( CustomNodeMixin, LocalHiddenHost ):
    """ A custom adapted hidden host running locally """
    pass



class CustomRemoteHost( CustomNodeMixin, RemoteHost ):
    """ A custom adapted host running remotely """
    pass



class CustomRemoteHiddenHost( CustomNodeMixin, RemoteHiddenHost ):
    """ A custom adapted hidden host running remotely """
    pass



class OVSSwitchMixin( object ):
    """ A mix-in with adapted OVS switch methods """

    def __init__( self, *args, **kwargs ):
        super( OVSSwitchMixin, self ).__init__( *args, **kwargs )
        self.datapath_type = kwargs.get( 'datapath_type' )


    def dpid_as_hex( self, add_prefix=True ):
        add_prefix = as_bool( add_prefix, name='add_prefix' )
        if add_prefix:
            return '0x{}'.format( self.dpid )
        else:
            return str( self.dpid )


    # Overriden method (does not call super).
    #
    # NOTE: OVS interfaces are configured in a specific way
    #
    def intfOpts( self, intf ):
        return as_a( intf, instance_of=OVSIntfMixin ).ovs_intfopts()


    # Overriden method (does not call super).
    #
    # NOTE: OVS bridges are configured in a specific way
    #
    def bridgeOpts( self ):
        opts = ''

        if is_somestr( self.datapath_type ):
            opts += ' datapath_type={0}'.format( self.datapath_type )

        if is_somestr( self.failMode ):
            opts += ' fail_mode={0}'.format( self.failMode )

        if is_somestr( self.protocols ):
            opts += ' protocols={0}'.format( self.protocols )

        if self.inband is True:
            opts += ' other-config:disable-in-band=false'
        elif self.inband is False:
            opts += ' other-config:disable-in-band=true'

        opts += ' other_config:datapath-id={0}'.format( self.dpid )

        return opts



class LocalOVSSwitch( LocalNodeMixin, \
                      OVSSwitchMixin, \
                      OVSSwitch ):
    """ An adapted OVS switch running locally """
    pass



class RemoteOVSSwitch( RemoteNodeMixin, \
                       OVSSwitchMixin, \
                       OVSSwitch ):
    """ An adapted OVS switch running remotely """

    # Overriden method (calls super).
    #
    # NOTE: like in mininet.examples.cluster.RemoteMixin we must start the switches per server.
    #
    @classmethod
    def batchStartup( cls, switches, **_kwargs ):
        key = attrgetter( 'server' )
        for server, switchGroup in groupby( sorted( switches, key=key ), key ):
            info( '(%s)' % server )
            group = tuple( switchGroup )
            switch = group[ 0 ]
            OVSSwitch.batchStartup( group, run=switch.cmd )
        return switches


    # Overriden method (calls super).
    #
    # NOTE: like in mininet.examples.cluster.RemoteMixin we must stop the switches per server.
    #
    @classmethod
    def batchShutdown( cls, switches, **_kwargs ):
        key = attrgetter( 'server' )
        for server, switchGroup in groupby( sorted( switches, key=key ), key ):
            info( '(%s)' % server )
            group = tuple( switchGroup )
            switch = group[ 0 ]
            OVSSwitch.batchShutdown( group, run=switch.rcmd )
        return switches



################################################################################
#### Adapted physical and virtual interfaces

class IntfMixin( object ):
    """ A mix-in with adapted interface methods """

    def is_virtual( self ):
        """ Override this method.
        """
        raise NotImplementedError()



class VirIntf( IntfMixin, Intf ):
    """ A virtual interface """

    def is_virtual( self ):
        return True



class PhyIntf( IntfMixin, Intf ):
    """ A physical interface """

    def is_virtual( self ):
        return False


    # Overriden method (does not call super).
    #
    def delete( self ):
        # Do not try to remove the physical interface but flush its addresses instead
        self.cmd( 'ip addr flush dev {0}'.format( self.name ) )



class OVSIntfMixin( object ):
    """ A mix-in with adapted OVS switch interface methods """

    def __init__( self, name, \
                  intf_type=None, ofport=None, tag=None, intf_opts={}, **params ):

        super( OVSIntfMixin, self ).__init__( name, **params )

        self.intf_type = optional_str( intf_type )
        self.ofport = optional_int( ofport )
        self.tag = optional_int( tag )
        self.intf_opts = dict( intf_opts )


    def ovs_intfopts( self ):
        popts = ''
        iopts = ''

        if is_some( self.tag ):
            popts += ' vlan_mode=access tag={}'.format( self.tag )
        else:
            popts += ' vlan_mode=trunk tag=[] trunks=[]'


        if is_some( self.intf_type ):
            iopts += ' type={0}'.format( self.intf_type )

        if is_some( self.ofport ):
            iopts += ' ofport_request={0}'.format( self.ofport )

        is_valid = lambda ( k, v ) : is_somestr( k ) and is_somestr( v )
        opt_to_str = lambda ( k, v ) : '{0}={1}'.format( k, v )
        valid_opts = list( filter( is_valid, self.intf_opts.iteritems() ) )
        if len( valid_opts ) == 1:
            single_opt = valid_opts[0]
            iopts += ' options:' + opt_to_str( single_opt )
        elif len( valid_opts ) > 0:
            iopts += ' options:{'
            iopts += ','.join( map( opt_to_str, valid_opts ) )
            iopts += '}'

        if len( iopts ) > 0:
            iopts = ' -- set Interface {} {}'.format( self.name, iopts )

        return popts + iopts



class OVSPhyIntf( OVSIntfMixin, PhyIntf ):
    """ A physical OVS switch interface """
    pass



class OVSVirIntf( OVSIntfMixin, VirIntf ):
    """ A virtual OVS switch interface """
    pass



################################################################################
#### Adapted links

class LinkMixin( object ):
    """ A mix-in with adapted link methods """

    # Overriden method (does not call super).
    #
    # Make sure both interfaces are properly cleaned up
    #
    def stop( self ):
        self.intf1.delete()
        self.intf2.delete()



class VirLink( LinkMixin, Link ):
    """ A link between two virtual interfaces """
    pass



class PhyLink( LinkMixin, Link ):
    """ A link between two physical interfaces """

    # Overriden method (does not call super).
    #
    # NOTE: hack to disable veth pair in physical links.
    #
    @classmethod
    def makeIntfPair( _cls, *_args, **_kwargs ):
        pass
