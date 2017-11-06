from config.util.topo import VaranusTopo
from varanuspy.functions import resetqos, setqos, ssrclinks, switches
from varanuspy.rcli import start_rcli
from varanuspy.utils import resolve


def pre_start_config( mr, extra_args, local_varanus_home ):
    """ Configure a MininetRunner object before Mininet starts.
        - mr        : a MininetRunner object
        - extra_args: extra arguments passed by the command line
    """
    topo = VaranusTopo( mr, extra_args, local_varanus_home=local_varanus_home )


    # Controllers
    topo.add_local_sdncontroller( ip=resolve( 'localhost' ), port=6653 )


    # Switches
    rb = topo.get_ring_builder( 1 )
    node_builder = topo.get_ring_local_switch_builder()
    rb.add_node( node_builder, 'Core1' )
    rb.add_node( node_builder, 'A1' )
    rb.add_node( node_builder, 'A2' )
    rb.add_node( node_builder, 'A3' )
    rb.add_node( node_builder, 'Core2' )
    rb.add_node( node_builder, 'Core3' )
    rb.add_node( node_builder, 'B1' )
    rb.add_node( node_builder, 'B2' )
    rb.add_node( node_builder, 'B3' )
    rb.add_node( node_builder, 'Core4' )
    rb.add_node( node_builder, 'Core5' )
    topo.add_existing_ovs_switch_ring( rb.build() )

    topo.add_ring_vir_link( 1, 'Core1', 1, 'Core2' )
    topo.add_ring_vir_link( 1, 'Core3', 1, 'Core4' )


    # Hosts
    topo.add_ring_local_vir_host( 'SCADA_Client', 1, '192.168.10.101/24', 1, 'A2' )
    topo.add_ring_local_vir_host( 'RTU', 1, '192.168.10.102/24', 1, 'B2' )
    topo.add_ring_local_vir_host( 'Cntrl_Center', 1, '192.168.10.103/24', 1, 'Core5' )
    topo.add_ring_local_vir_host( 'Attacker', 1, '192.168.10.104/24', 1, 'A1', hidden=True )


    # Collectors
    topo.add_ring_local_vir_collector( 'c1', 1, 1 )


    return topo.conclude()



def post_start_config( mr, _extra_args, _local_varanus_home ):
    """ Configure a MininetRunner object after Mininet starts.
        - mr        : a MininetRunner object
        - extra_args: extra arguments passed by the command line
    """
    net = mr.net

    # Setup base QoS in all switch links
    band = '100000000' # bit/s
    netem = 'delay 5ms'
    resetqos()
    for sw in switches( net ):
        for L in ssrclinks( mr.net, sw ):
            setqos( mr.net, L.nsrc, L.ndst, band, netem )

    # Start the collectors
    mr.get_host( 'c1' ).netnode.start()

    # Auto-start the remote CLI
    start_rcli( 32770, net )
