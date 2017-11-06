from mininet.log import debug, error, output
from mininet.node import Host, OVSSwitch

from varanuspy.functions import special, getqos, setqos, delqos, resetqos, get_globals, get_locals
from varanuspy.minadapter import CustomMixin
from varanuspy.rcli import status_rcli, start_rcli, stop_rcli
from varanuspy.utils import some, as_bool, newline, is_some, is_somestr


def get_cmds():
    return {
        'pxx' : _pxx,
        'rcli' : _rcli,
        'restartsw' : _restart_switches,
        'control' : _control_node,
        'getqos' : _get_link_qos,
        'setqos' : _set_link_qos,
        'delqos' : _del_link_qos,
        'resetqos' : _reset_qos,
        'flows' : _ovs_dump_flows,
        'nuttcp' : _run_nuttcp,
        'nutudp' : _run_nutudp
    }


def _pxx( cli, line ):
    """ Executes a python statement like command 'px', except newly defined variables or functions will be temporary.

        Additionally, the following functions are available:
        - cli(cmd)                     : call a command as if it was called in the console
        - special(name)                : returns a SpecialNode object that can be passed to other functions
        - cmd(node, *args)             : call a command on the given node and return its output when complete
        - nodes()                      : returns a list of all nodes
        - switches()                   : returns a list of all switches
        - hosts()                      : returns a list of all hosts
        - links()                      : returns a list of all (unidirectional) links
        - links(src, dst)              : returns the (unidirectional) links between the given nodes
        - sid(dpid)                    : returns the switch object that has the given DPID, or None if none exists
        - hip(ip_address)              : returns the host object that has the given IP address, or None if none exists
        - isremote(node)               : returns true if the given node is remote, or false otherwise
        - nsrclinks(node)              : returns a list of links starting from the given node
        - ssrclinks(node)              : returns a list of links starting from the given node and ending in switches 
        - hsrclinks(node)              : returns a list of links starting from the given node and ending in hosts
        - ndstlinks(node)              : returns a list of links ending on the given node
        - sdstlinks(node)              : returns a list of links ending on the given node and starting in switches 
        - hdstlinks(node)              : returns a list of links ending on the given node and starting in hosts
        - getqos(src, dst)             : returns (bandwidth, netem_configuration)
        - setqos(src, dst, band, netem): sets a bandwidth and a Netem configuration on the given link
        - delqos(src, dst)             : removes the bandwidth and Netem configurations from the given link
        - resetqos()                   : removes all bandwidth and Netem configurations in the local machine

        Usage: pxx <python statement>
    """
    try:
        globs = get_globals( cli.mn )
        globs['cli'] = lambda cmd : cli.onecmd( cmd )
        locs = get_locals( cli.mn )
        exec line in globs, locs
    except Exception as e:
        output( newline( str( e ) ) )


def _rcli( cli, line ):
    """ Listens for TCP connections at the specified port and accepts CLI commands from active clients.

        NOTE: Whenever we mention strings to be transmitted, we mean UTF-8 strings prefixed by a big-endian 4-byte value
              representing the string length

        Commands have the format '<type><value>', where:
        - <type> is a byte value representing the type of command to execute
        - <value> is a string representing the command value that is interpreted differently according to its type

        The following command types are available (represented as integers):
        - 0: the command value is interpreted as a python expression and its result is sent back to the client through
             the same TCP connection
        - 1: the command value is interpreted as a shell command to be executed on a specific node and its output can be
             progressively sent to a designated TCP address until the command finishes or the client interrupts it

        For python expression commands, the possible result formats are:
        - If the expression returned None       : the byte '0'
        - If the expression returned <something>: the byte '1' followed by a string representation of <something>
        - If the expression raised an exception : the byte '2' followed by a string representation of the exception

        For python expression commands, the following functions are available:
        - special(name)                : returns a SpecialNode object that can be passed to other functions
        - cmd(node, *args)             : call a command on the given node and return its output when complete
        - nodes()                      : returns a list of all nodes
        - switches()                   : returns a list of all switches
        - hosts()                      : returns a list of all hosts
        - links()                      : returns a list of all (unidirectional) links
        - links(src, dst)              : returns the (unidirectional) links between the given nodes
        - sid(dpid)                    : returns the switch object that has the given DPID, or None if none exists
        - hip(ip_address)              : returns the host object that has the given IP address, or None if none exists
        - isremote(node)               : returns true if the given node is remote, or false otherwise
        - nsrclinks(node)              : returns a list of links starting from the given node
        - ssrclinks(node)              : returns a list of links starting from the given node and ending in switches 
        - hsrclinks(node)              : returns a list of links starting from the given node and ending in hosts
        - ndstlinks(node)              : returns a list of links ending on the given node
        - sdstlinks(node)              : returns a list of links ending on the given node and starting in switches 
        - hdstlinks(node)              : returns a list of links ending on the given node and starting in hosts
        - getqos(src, dst)             : returns (bandwidth, netem_configuration)
        - setqos(src, dst, band, netem): sets a bandwidth and a Netem configuration on the given link
        - delqos(src, dst)             : removes the bandwidth and Netem configurations from the given link
        - resetqos()                   : removes all bandwidth and Netem configurations in the local machine

        Shell command details:
        - A shell command is executed asynchronously by the server until it finishes or until the client requests for it
          to stop
        - The command output can be sent to a designated TCP socket address as it is available, line by line
        - Each command is uniquely identified by a client-provided key string and has a distinct TCP connection to
          (optionally) send the output
        - Stopping a command is done either by terminating the process (sending a SIGTERM signal to it) or by executing
          a custom command provided by the client

        For shell commands, the possible command value formats are:
        - "start <key> <node> <host>:<port> <command>[ <command args>]*"
        - "start_no_output <key> <node> <command>[ <command args>]*"
        - "stop <key>"
        - "stop_custom <key> <command>[ <command_args>]*"

        For shell commands, the possible result formats are (sent back through the main TCP connection):
        - If the command was successfully started/stopped: the byte '1' followed by the command key string
        - If the command could not be started/stopped    : the byte '2' followed by the command key string followed by
                                                           an error string

        For shell commands, the format of each output line is <command key><output line>

        Usage: rcli { start <local_port> | stop | status }
    """
    usage = 'rcli { start <local_port> | stop | status }'

    args = line.split()
    if len( args ) < 1:
        error( newline( 'Invalid number of arguments' ) )
        error( newline( 'Usage:', usage ) )
    else:
        if args[0] == 'status':
            __status_rcli()
        elif args[0] == 'stop':
            __stop_rcli()
        elif args[0] == 'start':
            if len( args ) < 2:
                error( newline( 'Invalid number of arguments' ) )
                error( newline( 'Usage:', usage ) )
            else:
                __start_rcli( cli, args[1] )
        else:
            error( newline( 'Invalid argument' ) )
            error( newline( 'Usage:', usage ) )

def __status_rcli():
    if status_rcli():
        output( newline( '< RCLI server is active >' ) )
    else:
        output( newline( '< RCLI server is inactive >' ) )

def __stop_rcli():
    if stop_rcli():
        output( newline( '< RCLI server was stopped >' ) )
    else:
        error( newline( 'RCLI server is inactive, nothing to stop' ) )

def __start_rcli( cli, listenport ):
    try:
        if not start_rcli( listenport, cli.mn ):
            error( newline( 'An RCLI server is already active, cannot start a new one' ) )
    except ValueError as e:
        error( newline( e ) )


def _restart_switches( cli, line ):
    """ Stops and restarts one or more switches

        Usage: restartsw <switch>[, <switch>]*
    """
    usage = 'restartsw <switch>[, <switch>]*'

    args = line.split()
    if len( args ) < 1:
        error( newline( 'Invalid number of arguments' ) )
        error( newline( 'Usage:', usage ) )
    else:
        sw_names = args

        switches = []
        for name in sw_names:
            if name not in cli.mn:
                error( newline( 'Switch', '"' + name + '"', 'does not exist' ) )
                return
            else:
                switches.append( cli.mn[ name ] )

        for sw in switches:
            output( newline( 'Restarting switch', sw, '...' ) )

            stopline = 'switch {} stop'.format( sw )
            startline = 'switch {} start'.format( sw )
            cli.onecmd( stopline )
            cli.onecmd( startline )

            output( newline( '<done>' ) )


def _control_node( cli, line ):
    """ Executes a method on one or more Mininet nodes (controllers, hosts, switches, etc.)

        Usage: control <op> <node>[, <node>]*
    """
    usage = 'control <op> <node>[, <node>]*'

    args = line.split()
    if len( args ) < 2:
        error( newline( 'Invalid number of arguments' ) )
        error( newline( 'Usage:', usage ) )
    else:
        op_name = args[0]
        node_names = args[1:]

        nodes = []
        for name in node_names:
            if name not in cli.mn:
                error( newline( 'Node', '"' + name + '"', 'does not exist' ) )
                return
            else:
                node = cli.mn[ name ]
                if not isinstance( node, CustomMixin ):
                    error( newline( 'Node', node, 'cannot be controlled in this manner' ) )
                else:
                    nodes.append( node )

        ops = []
        for node in nodes:
            op = getattr( node, op_name ) if hasattr( node, op_name ) else None
            if not is_some( op ) or not callable( op ):
                error( newline( 'Unrecognized method', '"' + op_name + '"', \
                        'on node', node ) )
                return
            else:
                ops.append( op )

        # Run all the methods once we know they're safe
        for op in ops:
            op( cli=cli )


def _get_link_qos( cli, line ):
    """ Returns the bandwidth and Netem configurations currently assigned to the provided unidirectional link.
    
        Usage: getqos <src_node> <dst_node>
    """
    usage = 'getqos <src_node> <dst_node>'

    args = line.split()
    if len( args ) != 2:
        error( newline( 'Invalid number of arguments' ) )
        error( newline( 'Usage:', usage ) )
    else:
        try:
            src = special( cli.mn, args[0] )
            dst = special( cli.mn, args[1] )
            band, netem = getqos( cli.mn, src, dst )

            output( newline( 'Bandwidth: {}'.format( band ) ) )
            output( newline( 'Netem    : {}'.format( netem ) ) )
        except ( ValueError, RuntimeError ) as e:
            error( newline( e ) )


def _set_link_qos( cli, line ):
    """ Sets up bandwidth and Netem configurations on the provided unidirectional link.
    
        Usage: setqos <src_node> <dst_node> <bandwidth> [<netem_params...>]
    """
    usage = 'setqos <src_node> <dst_node> <bandwidth> [<netem_params...>]'

    args = line.split()
    if len( args ) < 3:
        error( newline( 'Invalid number of arguments' ) )
        error( newline( 'Usage:', usage ) )
    else:
        try:
            src = special( cli.mn, args[0] )
            dst = special( cli.mn, args[1] )
            band = args[2]
            if len( args ) > 3:
                netem = ' '.join( args[3:] )
            else:
                netem = None
            if setqos( cli.mn, src, dst, band, netem ):
                output( newline( '<done>' ) )
            else:
                output( newline( '<operation failed>' ) )
        except ( ValueError, RuntimeError ) as e:
            error( newline( e ) )


def _del_link_qos( cli, line ):
    """ Removes bandwidth and Netem configurations from the provided unidirectional link.
    
        Usage: delqos <src_node> <dst_node>
    """
    usage = 'delqos <src_node> <dst_node>'

    args = line.split()
    if len( args ) != 2:
        error( newline( 'Invalid number of arguments' ) )
        error( newline( 'Usage:', usage ) )
    else:
        try:
            src = special( cli.mn, args[0] )
            dst = special( cli.mn, args[1] )
            if delqos( cli.mn, src, dst ):
                output( newline( '<done>' ) )
            else:
                output( newline( '<operation failed>' ) )
        except ( ValueError, RuntimeError ) as e:
            error( newline( e ) )


def _reset_qos( _cli, line ):
    """ Removes all bandwidth and Netem configurations in the local machine.
    
        Usage: resetqos
    """
    usage = 'resetqos'
    args = line.split()
    if len( args ) != 0:
        error( newline( 'Invalid number of arguments' ) )
        error( newline( 'Usage:', usage ) )
    else:
        if resetqos():
            output( newline( '<done>' ) )
        else:
            output( newline( '<operation failed>' ) )


def _ovs_dump_flows( cli, line ):
    """ Runs ovs-ofctl in one or more OvS switches to dump the flows of each switch.

        Usage: flows <switch>[, <switch>]*
    """
    usage = 'flows <switch>[, <switch>]*'

    args = line.split()
    if len( args ) < 1:
        error( newline( 'Invalid number of arguments' ) )
        error( newline( 'Usage:', usage ) )
    else:
        sw_names = args

        switches = []
        for name in sw_names:
            if name not in cli.mn:
                error( newline( 'Switch', '"' + name + '"', 'does not exist' ) )
                return
            else:
                sw = cli.mn[ name ]
                if not isinstance( sw, OVSSwitch ):
                    error( newline( '"' + name + '"', 'is not an OvS switch' ) )
                    return
                else:
                    switches.append( sw )

        for sw in switches:
            intro = '{} flows'.format( sw )

            output( newline() )
            output( newline( '=' * 80 ) )
            output( newline( intro.center( 80 ) ) )
            output( newline( '=' * 80 ) )

            ofver = some( sw.protocols, name='sw.protocols' )
            cmdline = '{0} ovs-ofctl -O {1} dump-flows \'{0}\''.format( sw, ofver )
            cli.onecmd( cmdline )


def _run_nuttcp( cli, line ):
    """ Runs nuttcp between a pair of client/server hosts with TCP traffic for 60 minutes.

        Usage: nuttcp client_host server_host [rate_limit]
    """
    usage = 'nuttcp client_host server_host [rate_limit]'

    try:
        client, server, rate = __parse_nuttcp_args( cli, line, usage )
        __run_nuttcp( cli, client, server, rate=rate )
    except ValueError:
        pass


def _run_nutudp( cli, line ):
    """ Runs nuttcp between a pair of client/server hosts with UDP traffic for 60 minutes.

        Usage: nutudp client_host server_host [rate_limit]
    """
    usage = 'nutudp client_host server_host [rate_limit]'

    try:
        client, server, rate = __parse_nuttcp_args( cli, line, usage )
        __run_nuttcp( cli, client, server, rate=rate, udp=True )
    except ValueError:
        pass


def __parse_nuttcp_args( cli, line, usage ):
    args = line.split()
    if len( args ) < 2 or len( args ) > 3:
        error( newline( 'Invalid number of arguments' ) )
        error( newline( 'Usage:', usage ) )
        raise ValueError()
    else:
        cname = args[0]
        sname = args[1]
        rate = args[2] if len( args ) == 3 else None

        if cname not in cli.mn:
            error( newline( 'Host', '"' + cname + '"', 'does not exist' ) )
            raise ValueError()
        elif sname not in cli.mn:
            error( newline( 'Host', '"' + sname + '"', 'does not exist' ) )
            raise ValueError()
        else:
            client = cli.mn[ cname ]
            server = cli.mn[ sname ]

            if not isinstance( client, Host ):
                error( newline( '"' + cname + '"', 'is not a Host' ) )
                raise ValueError()
            elif not isinstance( server, Host ):
                error( newline( '"' + sname + '"', 'is not a Host' ) )
                raise ValueError()
            else:
                return ( client, server, rate )


def __run_nuttcp( cli, client, server, rate=None, udp=False ):
    traffic = 'UDP traffic' if as_bool( udp ) is True else 'TCP traffic'
    if is_somestr( rate ):
        traffic = '{} ({})'.format( traffic, rate )
    intro = 'Running nuttcp between ' + str( client ) + ' and ' + str( server )\
            + ' with ' + traffic + ' for 60 minutes'

    output( newline() )
    output( newline( '=' * 80 ) )
    output( newline( intro.center( 80 ) ) )
    output( newline( '=' * 80 ) )

    sstartline = '{} nuttcp -S'.format( server )
    sstopline = '{} pkill -f \'nuttcp -S\''.format( server )
    copts = ''
    if as_bool( udp ) is True:
        copts += ' -u'
    if is_somestr( rate ):
        copts += ' -Ri{}'.format( rate )
    cline = '{} nuttcp -ib -T60m{} {}'.format( client, copts, server.IP() )

    output( newline( '- Starting nuttcp server at', server ) )
    debug( newline( 'Running command: ', sstartline ) )
    cli.onecmd( sstartline )

    output( newline( '- Running nuttcp client at', client ) )
    debug( newline( 'Running command: ', cline ) )
    cli.onecmd( cline )

    output( newline( '- Stopping nuttcp server at', server ) )
    debug( newline( 'Running command: ', sstopline ) )
    cli.onecmd( sstopline )
