from Queue import Empty, Queue
import collections
import ipaddress
import netifaces
import pwd
import signal
import socket
from subprocess import Popen
from threading import Thread


################################################################################
#### String utils
def newline( *args ):
    class SingleLine( object ):
        def __init__( self, words ):
            self.words = words

        def __iter__( self ):
            return SingleLineIterator( self.words )

        def __str__( self ):
            return ' '.join( self )

    if len( args ) == 1 and isinstance( args[0], list ):
        args_list = args[0]
    else:
        args_list = list( args )

    return SingleLine( args_list )


class SingleLineIterator( object ):
    def __init__( self, words ):
        self.words = words
        self.final_idx = len( words ) - 1
        self.curr_idx = 0

    def __iter__( self ):
        return self

    def __next__( self ):
        i = self.curr_idx
        if i < self.final_idx:
            self.curr_idx = i + 1
            return str( self.words[i] )
        elif i == self.final_idx:
            self.curr_idx = i + 1
            return '{}\n'.format( self.words[i] )
        else:
            raise StopIteration

    next = __next__


def multiline( *args ):
    class MultiLine( object ):
        def __init__( self, lines ):
            self.lines = lines

        def __iter__( self ):
            return self.lines

        def __str__( self ):
            return '\n'.join( self.lines )

    if len( args ) == 1 and isinstance( args[0], list ):
        args_list = args[0]
    else:
        args_list = list( args )

    return MultiLine( args_list )


################################################################################
#### Function utils

def call_until( func, stop_condition ):
    """ Calls the provided function (if defined) until the provided stop
        condition function returns True.
        - func          : a callable to be called repeatedly
        - stop_condition: a callable that returns True when no more calls to
                          func should be made
    """
    stop_condition = as_callable( stop_condition, name='stop_condition' )
    if is_some( func ):
        func = as_callable( func, name='func' )
        while stop_condition() is not True:
            func()



################################################################################
#### OS utils

def user_exists( username ):
    try:
        pwd.getpwnam( username )
        return True
    except KeyError:
        return False


def get_user_home( username ):
    try:
        return pwd.getpwnam( username ).pw_dir
    except KeyError:
        raise ValueError( 'unknown user' )


class AsyncProcess( object ):
    """ A wrapper for a Popen object that allows for asynchronous access to its
        output one line at a time.
    """

    def __init__( self, popen, cmd=None ):
        """ Creates a new AsyncProcess object that wraps the provided Popen
            object and consumes its output in a separate thread.
        """
        self.popen = as_a( popen, instance_of=Popen )
        self.queue = Queue()
        if is_some( cmd ):
            if isinstance( cmd, list ):
                cmd = ' '.join( cmd )
            self.cmd = cmd
        else:
            self.cmd = None
        t = Thread( target=self.__consume_output )
        t.daemon = True
        t.start()


    def readline( self, block=True, timeout=None ):
        block = as_bool( block, name='block' )
        try:
            return self.queue.get( block=block, timeout=timeout )
        except Empty:
            return None


    def readline_nowait( self ):
        return self.readline( block=False )


    def read_available_lines( self ):
        return list( iter( self.readline_nowait, None ) )


    def interrupt( self ):
        self.popen.send_signal( signal.SIGINT )


    def terminate( self ):
        self.popen.terminate()


    def kill( self ):
        self.popen.kill()


    def wait_to_finish( self ):
        return self.popen.wait()


    def is_finished( self ):
        return is_some( self.get_return_code() )


    def get_return_code( self ):
        return self.popen.poll()


    def __consume_output( self ):
        with self.popen.stdout as output:
            for line in iter( output.readline, b'' ):
                line = line.rstrip( '\n\r' )
                self.queue.put( line )


################################################################################
#### Network utils

def resolve( hostname ):
    hostname = some( hostname, name='hostname' )
    try:
        return str( ipaddress.ip_address( hostname ) )
    except ValueError:
        return socket.gethostbyname( hostname )


def ipv4address_of( intf, index=0 ):
    index = as_int( index, minim=0 )
    ipv4addrs = ipv4addresses_of( intf )
    naddrs = len( ipv4addrs )
    if naddrs == 0:
        raise ValueError( 'interface {} has 0 assigned IPv4 addresses'.format( intf ) )
    elif index >= naddrs:
        if naddrs == 1:
            raise ValueError( 'address index {} is too high; only one address is available'.format( index ) )
        else:
            raise ValueError( 'address index {} is too high; only {} addresses are available'.format( index, naddrs ) )
    else:
        return ipv4addrs[index]['addr']


def ipv4addresses_of( intf ):
    intf = as_str( some( intf, name='interface' ) )
    if not intf in netifaces.interfaces():
        raise ValueError( 'unknown interface "{}"'.format( intf ) )
    else:
        addrs = netifaces.ifaddresses( intf )
        if not netifaces.AF_INET in addrs:
            raise ValueError( 'interface {} does not have any assigned IPv4 addresses'.format( intf ) )
        else:
            return addrs[netifaces.AF_INET]


def send_bytes( sock, buf, exit_check=None, exit_on_timeout=False ):
    """ Sends some bytes to the provided socket. Returns 'True' on success.
        - sock           : a socket object
        - buf            : a buffer containing bytes to send
        - exit_check     : an optional callable indicating if the send operation should be aborted;
                           if the callable is defined and returns 'True', and this function detects it,
                           then the send operation is aborted and 'False' is returned (defaults to
                           'None')
        - exit_on_timeout: if 'True' then raised socket.timeout exceptions are propagated to the
                           caller, otherwise they are ignored (defaults to 'False')
    """
    sock = some( sock, name='sock' )
    buf = some( buf, name='buf' )
    exit_check = as_callable( exit_check, name='exit_check' ) if is_some( exit_check ) else lambda : False
    exit_on_timeout = as_bool( exit_on_timeout, name='exit_on_timeout' )

    while not exit_check():
        try:
            sock.sendall( buf )
            return True
        except socket.timeout: # no bytes were sent
            if exit_on_timeout:
                raise # abort
            else:
                pass # try again
    else:
        return False


def recv_bytes( sock, nbytes, exit_check=None, exit_on_timeout=False ):
    """ Returns a received number of bytes from the provided socket.
        - sock           : a socket object
        - nbytes         : the number of bytes to receive
        - exit_check     : an optional callable indicating if the receive operation should be aborted;
                           if the callable is defined and returns 'True', and this function detects it,
                           then the receive operation is aborted and 'None' is returned (defaults to
                           'None')
        - exit_on_timeout: if 'True' then raised socket.timeout exceptions are propagated to the
                           caller, otherwise they are ignored (defaults to 'False')

        An IOError is raised if the socket receives an EOF.
    """
    sock = some( sock, name='sock' )
    nbytes = as_int( nbytes, minim=0, name='nbytes' )
    exit_check = as_callable( exit_check, name='exit_check' ) if is_some( exit_check ) else lambda : False
    exit_on_timeout = as_bool( exit_on_timeout, name='exit_on_timeout' )

    buf = bytearray( nbytes )
    if nbytes == 0:
        return buf

    bufview = memoryview( buf )
    while not exit_check():
        try:
            nread = sock.recv_into( bufview )
            if nread == 0: # EOF
                raise IOError( 'remote side terminated the connection' )
            else:
                bufview = bufview[nread:]
                if len( bufview ) == 0:
                    return buf
        except socket.timeout: # no bytes were received
            if exit_on_timeout:
                raise # abort
            else:
                pass # try again
    else:
        return None



################################################################################
#### Value testing utils

def is_some( value ):
    return False if value is None else True


def is_somestr( value, allow_empty=False ):
    if value is None:
        return False
    else:
        svalue = str( value )
        if svalue is None:
            return False
        elif allow_empty == False and len( svalue ) == 0:
            return False
        else:
            return True


def is_iterable( value ):
    if not isinstance( value, collections.Iterable ):
        try:
            iter( value )
        except TypeError:
            return False

    return True


def is_mapping( value ):
    return isinstance( value, collections.Mapping )



################################################################################
#### Value checking utils

def some( value, name='value' ):
    if value is None:
        raise ValueError( 'expected {} to be defined (not None)'.format( name ) )
    else:
        return value


def as_oneof( value, container, valname='value', containername='container' ):
    if value not in container:
        raise ValueError( 'expected {} to be in {}'.format( valname, containername ) )
    else:
        return value


def as_bool( value, name='value' ):
    if value is not True and value is not False:
        raise ValueError( 'expected {} to be a boolean'.format( name ) )
    else:
        return value


def as_int( value, minim=None, maxim=None, name='value' ):
    try:
        ivalue = int( value )
    except ValueError:
        raise ValueError( 'expected {} to be an integer'.format( name ) )

    if minim is not None and ivalue < minim:
        raise ValueError( 'expected {} to be at least {}'.format( name, minim ) )
    elif maxim is not None and ivalue > maxim:
        raise ValueError( 'expected {} to be at most {}'.format( name, maxim ) )
    else:
        return ivalue


def as_str( value, allow_empty=False, name='value' ):
    if not is_somestr( value, allow_empty ):
        raise ValueError( 'expected {} to be a valid string'.format( name ) )
    else:
        return str( value )


def as_the( value, other, valname='value', othername='other' ):
    if value is not other:
        raise ValueError( 'expected {} to be the same as {}'.format( valname, othername ) )
    else:
        return value


def as_a( value, instance_of=None, subclass_of=None, name='value' ):
    if instance_of is not None and not isinstance( value, instance_of ):
        raise ValueError( 'expected {} to be an instance of {}'.format( name, instance_of ) )
    elif subclass_of is not None and not issubclass( value, subclass_of ):
        raise ValueError( 'expected {} to be a subclass of {}'.format( name, subclass_of ) )
    else:
        return value


def as_callable( value, name='value' ):
    if not callable( value ):
        raise ValueError( 'expected {} to be a callable'.format( name ) )
    else:
        return value



################################################################################
#### Misc. utils

def fallback( value, default ):
    if value is None:
        return default
    else:
        return value


def optional( value, mapper ):
    if is_some( value ):
        value = mapper( value )
    return value


def optional_bool( value ):
    return optional( value, as_bool )


def optional_int( value, minim=None, maxim=None ):
    return optional( value, lambda x : as_int( x, minim=minim, maxim=maxim ) )


def optional_str( value, allow_empty=False ):
    return optional( value, lambda x : as_str( x, allow_empty=allow_empty ) )


def check_duplicate( container, value, containername='container', valname='value' ):
    if value in container:
        raise ValueError( 'found duplicate {} in {}'.format( valname, containername ) )

