from collections import namedtuple
from functools import partial
from itertools import chain
from mininet.link import Intf
from mininet.node import Node
import re
from subprocess import PIPE, STDOUT
import subprocess

from varanuspy.utils import some, is_some, AsyncProcess, as_a, as_str, as_bool


def special( mn, node ):
    return SpecialNode( mn, node )


def run_cmd( mn, node, *args ):
    node = NodeUtils.as_node( mn, node, unpack_special=True )
    cmd = ' '.join( args )
    return NodeUtils.run_cmd( node, cmd )


def nodes( mn ):
    return switches( mn ) + hosts( mn )


def switches( mn ):
    return filter( lambda s : NodeUtils.is_valid_node( mn, s ), mn.switches )


def hosts( mn ):
    return filter( lambda h : NodeUtils.is_valid_node( mn, h ), mn.hosts )


def links( mn, *args ):
    if len( args ) == 0:
        return list( chain.from_iterable( nsrclinks( mn, node ) for node in nodes( mn ) ) )
    elif len( args ) == 2:
        src = NodeUtils.as_node( mn, args[0] )
        dst = NodeUtils.as_node( mn, args[1] )

        if isinstance( dst, SpecialNode ):
            src_name = src.node.name if isinstance( src, SpecialNode ) else src.name
            return _dstlinks_matching( mn, dst, lambda n : n.name == src_name )
        else:
            return _srclinks_matching( mn, src, lambda n : n.name == dst.name )
    else:
        raise ValueError( 'incorrect number of arguments (pass either two or none)' )


def sid( mn, dpid ):
    dpid = some( dpid, name='DPID' )
    switch = next( ( s for s in mn.switches if int( s.dpid ) == dpid ), None )
    if is_some( switch ):
        return switch
    else:
        raise ValueError( 'no switch matches the provided DPID' )


def hip( mn, ip_addr ):
    ip_addr = some( ip_addr, name='IP address' )
    host = next( ( h for h in mn.hosts if h.IP() == ip_addr ), None )
    if is_some( host ):
        return host
    else:
        raise ValueError( 'no host matches the provided IP address' )


def isremote( mn, node ):
    node = NodeUtils.as_node( mn, node, unpack_special=True )
    return node.is_remote()


def nsrclinks( mn, node ):
    node = NodeUtils.as_node( mn, node )
    return _srclinks_matching( mn, node, lambda _ : True )


def ssrclinks( mn, node ):
    node = NodeUtils.as_node( mn, node )
    return _srclinks_matching( mn, node, lambda n : n in mn.switches )


def hsrclinks( mn, node ):
    node = NodeUtils.as_node( mn, node )
    return _srclinks_matching( mn, node, lambda n : n in mn.hosts )


def ndstlinks( mn, node ):
    node = NodeUtils.as_node( mn, node )
    return _dstlinks_matching( mn, node, lambda _ : True )


def sdstlinks( mn, node ):
    node = NodeUtils.as_node( mn, node )
    return _dstlinks_matching( mn, node, lambda n : n in mn.switches )


def hdstlinks( mn, node ):
    node = NodeUtils.as_node( mn, node )
    return _dstlinks_matching( mn, node, lambda n : n in mn.hosts )


def getqos( mn, src, dst ):
    intf = __get_src_intf( mn, src, dst )
    return __getqos_intf( intf )


def setqos( mn, src, dst, band, netem ):
    intf = __get_src_intf( mn, src, dst )
    return __setqos_intf( intf, band, netem )


def delqos( mn, src, dst ):
    intf = __get_src_intf( mn, src, dst )
    return __delqos_intf( intf )


def resetqos():
    return __resetqos()


def get_globals( mn ):
    globs = dict( globals() )
    globs['special'] = lambda name : special( mn, name )
    globs['cmd'] = lambda node, *args : run_cmd( mn, node, *args )
    globs['nodes'] = lambda : nodes( mn )
    globs['switches'] = lambda : switches( mn )
    globs['hosts'] = lambda : hosts( mn )
    globs['links'] = partial( links, mn )
    globs['sid'] = lambda dpid : sid( mn, dpid )
    globs['hip'] = lambda ip_addr : hip( mn, ip_addr )
    globs['isremote'] = lambda node : isremote( mn, node )
    globs['nsrclinks'] = lambda node : nsrclinks( mn, node )
    globs['ssrclinks'] = lambda node : ssrclinks( mn, node )
    globs['hsrclinks'] = lambda node : hsrclinks( mn, node )
    globs['ndstlinks'] = lambda node : ndstlinks( mn, node )
    globs['sdstlinks'] = lambda node : sdstlinks( mn, node )
    globs['hdstlinks'] = lambda node : hdstlinks( mn, node )
    globs['getqos'] = lambda src, dst : getqos( mn, src, dst )
    globs['setqos'] = lambda src, dst, *args : setqos( mn, src, dst, *args )
    globs['delqos'] = lambda src, dst : delqos( mn, src, dst )
    globs['resetqos'] = resetqos
    return globs


def get_locals( mn ):
    locs = { 'net': mn }
    locs.update( mn )
    return locs


class NodeUtils( object ):

    @staticmethod
    def run_cmd( node, cmd ):
        popen = NodeUtils._popen( node, cmd )
        return popen.communicate()[0]

    @staticmethod
    def run_cmd_async( node, cmd ):
        popen = NodeUtils._popen( node, cmd, close_fds=True )
        return AsyncProcess( popen, cmd=cmd )

    @staticmethod
    def _popen( node, cmd, stdin=PIPE, stdout=PIPE, stderr=STDOUT, shell=True, close_fds=False, **params ):
        return node.popen( cmd, stdin=stdin, stdout=stdout, stderr=stderr, shell=shell, close_fds=close_fds, **params )

    @staticmethod
    def as_node( mn, node, unpack_special=False ):
        node = some( node, name='node' )
        unpack_special = as_bool( unpack_special, name='unpack_special' )
        if isinstance( node, SpecialNode ):
            return node.node if unpack_special else node
        elif isinstance( node, Node ):
            return NodeUtils.check_valid_node( mn, node )
        else:
            name = NodeUtils.check_valid_name( mn, str( node ) )
            return mn[name]

    @staticmethod
    def as_name( mn, node ):
        node = some( node, name='node' )
        if isinstance( node, Node ):
            node = NodeUtils.check_valid_node( mn, node )
            return node.name
        else:
            return NodeUtils.check_valid_name( mn, str( node ) )

    # NOTE: must always pass a Node object
    @staticmethod
    def is_valid_node( mn, node ):
        if node in mn.hosts:
            return is_some( node.IP() )
        elif node in mn.switches:
            return is_some( node.dpid )
        else:
            return False

    # NOTE: must always pass a Node object
    @staticmethod
    def check_valid_node( mn, node ):
        if node in mn.hosts:
            if is_some( node.IP() ):
                return node
            else:
                raise ValueError( 'invalid host node (must have a defined IP address)' )
        elif node in mn.switches:
            if is_some( node.dpid ):
                return node
            else:
                raise ValueError( 'invalid switch node (must have a defined DPID)' )
        else:
            raise ValueError( 'unknown node with name \'{}\''.format( node.name ) )

    # NOTE: must always pass a string
    @staticmethod
    def check_valid_name( mn, name ):
        if name in mn:
            NodeUtils.check_valid_node( mn, mn[name] )
            return name
        else:
            raise ValueError( 'unknown node with name \'{}\''.format( name ) )


class SpecialNode( object ):

    def __init__( self, mn, node ):
        node = some( node, name='node' )
        if isinstance( node, Node ):
            self.node = SpecialNode._check_valid_node( mn, node )
        else:
            name = SpecialNode._check_valid_name( mn, str( node ) )
            self.node = SpecialNode._check_valid_node( mn, mn[name] )

    # NOTE: must always pass a Node object
    @staticmethod
    def _check_valid_node( mn, node ):
        if ( node in mn.hosts ) or ( node in mn.switches ):
            return node
        else:
            raise ValueError( 'unknown node with name \'{}\''.format( node.name ) )

    # NOTE: must always pass a string
    @staticmethod
    def _check_valid_name( mn, name ):
        if name in mn:
            return name
        else:
            raise ValueError( 'unknown node with name \'{}\''.format( name ) )


def _srclinks_matching( mn, node, node_match ):
    intfs = _intfs_matching( mn, node, node_match )
    return map( lambda i : _make_link( i, _peer_intf( i ) ), intfs )

def _dstlinks_matching( mn, node, node_match ):
    intfs = _intfs_matching( mn, node, node_match )
    return map( lambda i : _make_link( _peer_intf( i ), i ), intfs )

def _intfs_matching( mn, node, node_match ):
    validate_node = True
    if isinstance( node, SpecialNode ):
        node = node.node
        validate_node = False

    intf_match = lambda i :  _is_valid_intf( mn, i, validate_node=validate_node ) and node_match( _peer_intf( i ).node )
    return filter( intf_match, node.intfList() )

# NOTE: must always pass an Intf object
def _is_valid_intf( mn, intf, validate_node=True ):
    if is_some( intf.link ) and is_some( intf.node ):
        valid_node = ( not validate_node ) or NodeUtils.is_valid_node( mn, intf.node )
        return valid_node and _has_valid_peer( mn, intf, validate_node=validate_node )
    else:
        return False

# NOTE: must always pass a valid Intf object
def _has_valid_peer( mn, intf, validate_node=True ):
    peer = _peer_intf( intf )
    if is_some( peer.link ) and is_some( peer.node ):
        return ( not validate_node ) or  NodeUtils.is_valid_node( mn, peer.node )
    else:
        return False

# NOTE: must always pass a valid Intf object
def _peer_intf( intf ):
    link = intf.link
    if intf is link.intf1:
        return link.intf2
    elif intf is link.intf2:
        return link.intf1
    else:
        raise ValueError( 'inconsistent link state' )

def _make_link( isrc, idst ):
    qos = __getqos_intf( isrc )
    return UniLink( isrc.node, isrc, idst.node, idst, qos )

class UniLink( namedtuple( 'UniLink', ['nsrc', 'isrc', 'ndst', 'idst', 'qos'] ) ):
    def __repr__( self ):
        return '('\
                + '{0}={n.name}'.format( 'nsrc', n=self._asdict()['nsrc'] ) + ', '\
                + '{0}={i.name}'.format( 'isrc', i=self._asdict()['isrc'] ) + ', '\
                + '{0}={n.name}'.format( 'ndst', n=self._asdict()['ndst'] ) + ', '\
                + '{0}={i.name}'.format( 'idst', i=self._asdict()['idst'] ) + ', '\
                + '{0}={q}'.format( 'qos', q=self._asdict()['qos'] )\
                + ')'

class QoSCache( object ):

    def __init__( self ):
        self.cache_map = {}

    def get( self, intf ):
        intf = as_a( intf, instance_of=Intf, name='intf' )
        return self.cache_map.get( intf.name )

    def put( self, intf, qos ):
        intf = as_a( intf, instance_of=Intf, name='intf' )
        qos = some( qos, name='qos' )
        self.cache_map[intf.name] = qos

    def remove( self, intf ):
        intf = as_a( intf, instance_of=Intf, name='intf' )
        return self.cache_map.pop( intf.name, None )

    def reset( self ):
        self.cache_map.clear()

QOS_CACHE = QoSCache()

def __getqos_intf( intf ):
    qos = QOS_CACHE.get( intf )
    if is_some( qos ):
        return qos
    elif __check_iface_is_available( intf ):
        qos = __get_bandwidth_and_netem( intf )
        QOS_CACHE.put( intf, qos )
        return qos
    else:
        return ( None, None )

def __setqos_intf( intf, band, netem ):
    success = False
    if __check_iface_is_available( intf ):
        success = __set_bandwidth_and_netem( intf, band, netem )

    if success:
        qos = __get_bandwidth_and_netem( intf )
        QOS_CACHE.put( intf, qos )
    else:
        QOS_CACHE.remove( intf )

    return success

def __delqos_intf( intf ):
    success = False
    if __check_iface_is_available( intf ):
        success = __del_bandwidth_and_netem( intf )

    QOS_CACHE.remove( intf )

    return success

def __resetqos():
    ret = subprocess.call( 'ovs-vsctl --all destroy QoS -- --all destroy Queue', shell=True )
    sucess = ret == 0

    QOS_CACHE.reset()

    return sucess

def __get_src_intf( mn, src, dst ):
    Ls = links( mn, src, dst )
    if len( Ls ) < 1:
        raise ValueError( 'No links exist between ' + NodeUtils.as_name( mn, src ) + ' and ' + NodeUtils.as_name( mn, dst ) )
    elif len( Ls ) > 1:
        raise ValueError( 'Multiple links exist between ' + NodeUtils.as_name( mn, src ) + ' and ' + NodeUtils.as_name( mn, dst ) )
    else:
        return Ls[0].isrc

def __check_iface_is_available( intf ):
    if intf.node.is_managed():
        cmd = 'ip link show | grep -E "^[[:digit:]]+: {}"'.format( intf )
        res = NodeUtils.run_cmd( intf.node, cmd )
        return len( res ) > 0
    else:
        return False

def __get_bandwidth_and_netem( intf ):
    return ( __get_tc_bandwidth( intf ), __get_tc_netem( intf ) )

def __get_tc_bandwidth( intf ):
    cmd = 'tc class show dev {i} | grep -E "htb 1:1"'.format( i=intf )
    res = NodeUtils.run_cmd( intf.node, cmd )

    if len( res ) == 0:
        return None
    else:
        re_res = re.search( r'ceil ([^ ]+)', res )
        if is_some( re_res ):
            return re_res.group( 1 )
        else:
            raise RuntimeError( 'unexpected "tc class show" output for bandwidth' )

def __get_tc_netem( intf ):
    cmd = 'tc qdisc show dev {i} | grep -E "netem"'.format( i=intf )
    res = NodeUtils.run_cmd( intf.node, cmd )

    if len( res ) == 0:
        return None
    else:
        re_res = re.search( r'limit [^ ]+(.*)', res )
        if is_some( re_res ):
            return re_res.group( 1 ).strip()
        else:
            raise RuntimeError( 'unexpected "tc qdisc show" output for netem' )

def __del_bandwidth_and_netem( intf ):
    # netem is also removed automatically
    return __del_ovs_qos( intf )

def __del_ovs_qos( intf ):
    cmd_fmt = 'ovs-vsctl --if-exists destroy QoS {i}' + \
              ' -- --if-exists clear Port {i} qos'
    cmd = cmd_fmt.format( i=intf )

    res = NodeUtils.run_cmd( intf.node, cmd )
    if len( res ) == 0:
        return True
    else:
        raise RuntimeError( 'ovs-vsctl error while deleting QoS: ' + res )

def __set_bandwidth_and_netem( intf, band, netem ):
    band = as_str( band, allow_empty=True, name='band' ) if is_some( band ) else None
    netem = as_str( netem, allow_empty=True, name='netem' ) if is_some( netem ) else None
    return __del_ovs_qos( intf ) and __set_ovs_qos( intf, band ) and __set_tc_netem( intf, netem )

def __set_ovs_qos( intf, band ):
    if is_some( band ) and len( band ) > 0:
        cmd_fmt = 'ovs-vsctl -- set Port {i} qos=@newqos' + \
                  ' -- --id=@newqos create QoS type=linux-htb other-config:max-rate={b} queues=0=@q0' + \
                  ' -- --id=@q0 create Queue other-config:min-rate={b} other-config:max-rate={b}'
        cmd = cmd_fmt.format( i=intf, b=band )
    else:
        cmd_fmt = 'ovs-vsctl -- set Port {i} qos=@newqos' + \
                  ' -- --id=@newqos create QoS type=linux-htb queues=0=@q0' + \
                  ' -- --id=@q0 create Queue other-config={{}}'
        cmd = cmd_fmt.format( i=intf )

    _res = NodeUtils.run_cmd( intf.node, cmd )
    return True
    ###########################################################################
    # FIXME It is possible to get an output without any errors, so this doesn't
    # work...
    ###########################################################################
    # if len(res) == 0:
    #    return True
    # else:
    #    raise RuntimeError('ovs-vsctl error while setting QoS: ' + res)

def __set_tc_netem( intf, netem ):
    if is_some( netem ) and len( netem ) > 0:
        cmd = 'tc qdisc add dev {i} parent 1:1 handle 10: netem {n}'.format( i=intf, n=netem )
        res = NodeUtils.run_cmd( intf.node, cmd )
        if len( res ) == 0:
            return True
        else:
            raise RuntimeError( 'tc error while setting netem: ' + res )
    else:
        return True
