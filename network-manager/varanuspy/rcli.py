from contextlib import closing
from functools import partial
from mininet.log import debug, error, output
from mininet.node import Node
import socket
import struct
import threading

from varanuspy.functions import NodeUtils, get_globals, get_locals
from varanuspy.utils import newline, multiline, is_some, as_callable, as_int, send_bytes, recv_bytes, as_str, resolve, \
as_a


def status_rcli():
    srv = _RCLIServer._ACTIVE_SERVER
    return is_some( srv ) and srv.is_active()


def stop_rcli():
    srv = _RCLIServer._ACTIVE_SERVER
    if is_some( srv ) and srv.is_active():
        srv.shutdown()
        _RCLIServer._ACTIVE_SERVER = None
        return True
    else:
        return False


def start_rcli( listenport, mn ):
    listenport = as_int( listenport, minim=1, maxim=65535, name='listenport' )

    srv = _RCLIServer._ACTIVE_SERVER
    if is_some( srv ) and srv.is_active():
        return False
    else:
        # Specialized eval() function with specific globals and locals
        eval_cmd = _build_eval_func( mn )
        srv = _RCLIServer( listenport, eval_cmd, mn )
        srv.start()
        _RCLIServer._ACTIVE_SERVER = srv
        return True


def _build_eval_func( mn ):
    globs = get_globals( mn )
    locs = get_locals( mn )
    return partial( lambda expr : eval( expr, globs, locs ) )


class _RCLIServer( object ):

    _ACTIVE_SERVER = None

    _PYTHON_CMD_TYPE = 0
    _SHELL_CMD_TYPE = 1

    _PYTHON_RESULT_NULL_CODE = 0
    _PYTHON_RESULT_NONNULL_CODE = 1
    _PYTHON_RESULT_EXCEPTION_CODE = 2

    _SHELL_RESULT_OK_CODE = 1
    _SHELL_RESULT_ERROR_CODE = 2

    def __init__( self, listenport, eval_cmd, mn ):
        self._listenport = as_int( listenport, minim=1, maxim=65535, name='listenport' )
        self._eval_cmd = as_callable( eval_cmd, name='eval_cmd' )
        self.mn = mn

        self.shell_handlers = {}
        self._server_timeout = 1
        self.sock_timeout = 1

        self._active_thread = None
        self._shutdown_check = threading.Event()

    def is_active( self ):
        return is_some( self._active_thread )

    def is_shutdown( self ):
        return self._shutdown_check.is_set()

    def shutdown( self ):
        self._shutdown_check.set()

    def start( self ):
        assert not self.is_active(), 'An active server must not be (re)started'
        if not self.is_shutdown():
            t = threading.Thread( target=self._run, name='RCLI-thread' )
            t.daemon = True
            self._active_thread = t
            t.start()

    def _run( self ):
        try:
            sock = self._accept_single_connection()
            if is_some( sock ):
                self._handle_connection( sock )
        except IOError as e:
            error( newline( 'IO error:', e ) )
        finally:
            self._active_thread = None

    # For now, accept only one client at a time...
    def _accept_single_connection( self ):
        with closing( socket.socket( socket.AF_INET, socket.SOCK_STREAM ) ) as srvsock:
            srvsock.setsockopt( socket.SOL_SOCKET, socket.SO_REUSEADDR, 1 )
            srvsock.bind( ( "localhost", self._listenport ) )
            srvsock.listen( 1 )
            srvsock.settimeout( self._server_timeout )
            output( newline( '< RCLI server was started on port', self._listenport, '>' ) )

            while not self.is_shutdown():
                try:
                    sock, addr = srvsock.accept()
                    output( newline( '< RCLI client connected at', addr, '>' ) )
                    sock.settimeout( self.sock_timeout )
                    return sock
                except socket.timeout:
                    pass # try again
            else:
                return None

    def _handle_connection( self, sock ):
        with closing( sock ):
            while not self.is_shutdown():
                ctype, cmd = self._do_read_cmd( sock )
                if is_some( ctype ) and is_some( cmd ):
                    if ctype == _RCLIServer._PYTHON_CMD_TYPE:
                        code, res = self._do_exec_python_cmd( cmd )
                    elif ctype == _RCLIServer._SHELL_CMD_TYPE:
                        code, res = self._do_exec_shell_cmd( cmd )
                    else:
                        raise IOError( 'received invalid command type' )

                    self._do_write_cmd_result( sock, ctype, code, res )

    def _do_read_cmd( self, sock ):
        ctype_raw = recv_bytes( sock, 1, exit_check=self.is_shutdown )
        if not is_some( ctype_raw ):
            return ( None, None )
        else:
            ctype = _IOUtils.unpack_byte( ctype_raw )
            cmd = _IOUtils.recv_string( sock, exit_check=self.is_shutdown )
            if not is_some( cmd ):
                return ( None, None )
            else:
                return ( ctype, cmd )

    def _do_exec_python_cmd( self, python_cmd ):
        try:
            python_cmd = _IOUtils.decode_str( python_cmd )
            res = self._eval_cmd( python_cmd )
            if is_some( res ):
                res = _IOUtils.encode_str( res )
                return ( _RCLIServer._PYTHON_RESULT_NONNULL_CODE, res )
            else:
                return ( _RCLIServer._PYTHON_RESULT_NULL_CODE, None )
        except Exception as e:
            e = _IOUtils.encode_str( e )
            return ( _RCLIServer._PYTHON_RESULT_EXCEPTION_CODE, e )

    def _do_exec_shell_cmd( self, shell_cmd ):
        try:
            shell_cmd = _IOUtils.decode_str( shell_cmd )
            op, key, node, host, port, cmd = self._do_parse_shell_cmd_args( shell_cmd )
            if op == 'start' or op == 'start_no_output':
                if key in self.shell_handlers:
                    raise ValueError( 'cannot start command for active key {}'.format( key ) )
                else:
                    handler = _ShellCommandHandler( key, node, host, port, cmd )
                    self.shell_handlers[key] = handler
                    handler.start()
                    return ( _RCLIServer._SHELL_RESULT_OK_CODE, None )
            elif op == 'stop' or op == 'stop_custom':
                if key not in self.shell_handlers:
                    raise ValueError( 'cannot stop command for inactive key {}'.format( key ) )
                else:
                    handler = self.shell_handlers[key]
                    del self.shell_handlers[key]
                    if is_some( cmd ):
                        handler.stop_custom( cmd )
                    else:
                        handler.terminate()
                    return ( _RCLIServer._SHELL_RESULT_OK_CODE, None )
            else:
                raise AssertionError( 'should never happen' )
        except Exception as e:
            e = _IOUtils.encode_str( e )
            return ( _RCLIServer._SHELL_RESULT_ERROR_CODE, e )

    def _do_parse_shell_cmd_args( self, shell_cmd ):
        args = shell_cmd.split()
        if len( args ) < 1:
            raise ValueError( 'missing operation (start/start_no_output/stop/stop_custom)' )
        elif len( args ) < 2:
            raise ValueError( 'missing command key' )
        else:
            op = args[0]
            key = args[1]
            if op == 'start':
                # "start <key> <node> <host>:<port> <command>[ <command args>]*"
                if len( args ) < 3:
                    raise ValueError( 'missing node name' )
                elif len( args ) < 4:
                    raise ValueError( 'missing TCP socket address' )
                elif len( args ) < 5:
                    raise ValueError( 'missing command' )
                else:
                    node = NodeUtils.as_node( self.mn, args[2] )
                    sock_addr = args[3].split( ':' )
                    if len( sock_addr ) != 2:
                        raise ValueError( 'invalid socket address (must be <host>:<port>)' )
                    else:
                        host, port = sock_addr[0], sock_addr[1]
                        cmd = ' '.join( args[4:] )
                        return ( op, key, node, host, port, cmd )
            elif op == 'start_no_output':
                # "start_no_output <key> <node> <command>[ <command args>]*"
                if len( args ) < 3:
                    raise ValueError( 'missing node name' )
                elif len( args ) < 4:
                    raise ValueError( 'missing command' )
                else:
                    node = NodeUtils.as_node( self.mn, args[2] )
                    cmd = ' '.join( args[3:] )
                    return ( op, key, node, None, None, cmd )
            elif op == 'stop':
                # "stop <key>"
                return ( op, key, None, None, None, None )
            elif op == 'stop_custom':
                # "stop_custom <key> <command>[ <command_args>]*"
                if len( args ) < 3:
                    raise ValueError( 'missing command' )
                else:
                    cmd = ' '.join( args[2:] )
                    return ( op, key, None, None, None, cmd )
            else:
                raise ValueError( 'invalid operation (must be start/start_no_output/stop/stop_custom)' )

    def _do_write_cmd_result( self, sock, ctype, code, res ):
        ctype_raw = _IOUtils.pack_byte( ctype )
        if send_bytes( sock, ctype_raw, exit_check=self.is_shutdown ):
            code_raw = _IOUtils.pack_byte( code )
            if send_bytes( sock, code_raw, exit_check=self.is_shutdown ):
                if is_some( res ):
                    _IOUtils.send_string( sock, res, exit_check=self.is_shutdown )


class _ShellCommandHandler( object ):

    def __init__( self, key, node, host, port, cmd ):
        self.key = _IOUtils.encode_str( as_str( key, name='key' ) )
        self.node = as_a( node, instance_of=Node, name="node" )
        self.addr = _ShellCommandHandler.get_address( host, port )
        self.cmd = as_str( cmd, name='cmd' )
        self.sock_timeout = 1
        self.line_timeout = 1
        self.cmd_proc = None

    @staticmethod
    def get_address( host, port ):
        if is_some( host ) and is_some( port ):
            host = resolve( host )
            port = as_int( port, minim=1, maxim=65535, name='port' )
            return ( host, port )
        else:
            return None

    def start( self ):
        t = threading.Thread( target=self._run, name='ShellCommandHandler-{}-thread'.format( self.key ) )
        t.daemon = True

        output( newline( '<', 'Starting shell command', self.cmd, '>' ) )
        self.cmd_proc = NodeUtils.run_cmd_async( self.node, self.cmd )
        t.start()

    def terminate( self ):
        cmd_proc = self.cmd_proc
        if is_some( cmd_proc ):
            output( newline( '<', 'Terminating shell command', "'" + self.cmd + "'", '>' ) )
            cmd_proc.terminate()

    def stop_custom( self, cmd ):
        output( newline( '<', 'Stopping shell command', "'" + self.cmd + "'", 'by executing', "'" + cmd + "'", '>' ) )
        NodeUtils.run_cmd( self.node, cmd )

    def is_finished( self ):
        cmd_proc = self.cmd_proc
        if is_some( cmd_proc ):
            return cmd_proc.is_finished()
        else:
            return False

    def _run( self ):
        if is_some( self.addr ):
            with closing( socket.socket( socket.AF_INET, socket.SOCK_STREAM ) ) as sock:
                sock.connect( self.addr )
                sock.settimeout( self.sock_timeout )
                output( newline( '< Shell command client with key', self.key, 'connected to', self.addr, '>' ) )

                while not self.cmd_proc.is_finished():
                    line = self.cmd_proc.readline( block=True, timeout=self.line_timeout )
                    if is_some( line ):
                        debug( newline( '== Sending output line ===============================' ) )
                        debug( newline( line ) )
                        debug( newline( '======================================================' ) )
                        line = _IOUtils.encode_str( line )
                        self._do_write_output_line( sock, line )
        else:
            self.cmd_proc.wait_to_finish()
            debug( newline( '== Command output ======================================' ) )
            debug( multiline( self.cmd_proc.read_available_lines() ) )
            debug( newline( '========================================================' ) )


    def _do_write_output_line( self, sock, line ):
        if _IOUtils.send_string( sock, self.key, exit_check=self.is_finished ):
            _IOUtils.send_string( sock, line, exit_check=self.is_finished )


class _IOUtils( object ):

    _CMD_STRING_CHARSET = 'utf-8'

    @staticmethod
    def encode_str( s ):
        return str( s ).encode( _IOUtils._CMD_STRING_CHARSET )

    @staticmethod
    def decode_str( s ):
        return str( s ).decode( _IOUtils._CMD_STRING_CHARSET )

    @staticmethod
    def send_string( sock, s, exit_check=None, exit_on_timeout=False ):
        slen_raw = _IOUtils.pack_int( len( s ) )
        if send_bytes( sock, slen_raw, exit_check=exit_check, exit_on_timeout=exit_on_timeout ):
            return send_bytes( sock, s, exit_check=exit_check, exit_on_timeout=exit_on_timeout )
        else:
            return False

    @staticmethod
    def recv_string( sock, exit_check=None, exit_on_timeout=False ):
        slen_raw = recv_bytes( sock, 4, exit_check=exit_check, exit_on_timeout=exit_on_timeout )
        if not is_some( slen_raw ):
            return None
        else:
            slen = _IOUtils.unpack_int( slen_raw )
            if slen < 0:
                raise IOError( 'Received invalid string length' )
            else:
                return recv_bytes( sock, slen, exit_check=exit_check, exit_on_timeout=exit_on_timeout )

    @staticmethod
    def pack_int( num ):
        # pack big-endian 4-byte integer
        return struct.pack( '>i', num )

    @staticmethod
    def unpack_int( raw_bytes ):
        # unpack big-endian 4-byte integer
        return struct.unpack( '>i', raw_bytes )[0]

    @staticmethod
    def pack_byte( num ):
        # pack 1 byte
        return struct.pack( '=B', num )

    @staticmethod
    def unpack_byte( raw_bytes ):
        # unpack 1 byte
        return struct.unpack( '=B', raw_bytes )[0]
