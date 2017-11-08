#!/usr/bin/env python

import argparse
import importlib
from mininet.log import LEVELS
import os
import re
import sys

from varanuspy.runner import MininetRunner
from varanuspy.utils import as_bool


def run( prog_args=sys.argv[1:] ):
    parser = build_arg_parser()
    args, extra = parser.parse_known_args( prog_args )

    local_varanus_home = os.path.dirname( os.path.dirname( os.path.realpath( sys.argv[0] ) ) )

    cfg_mod = import_config_module( args.config )
    mn = MininetRunner( of_version=args.of, \
                        autoarp=args.arp, \
                        enable_varanus=not args.novaranus, \
                        verbosity=args.v )

    continue_prog = as_bool( cfg_mod.pre_start_config( mn, extra, local_varanus_home ), name='continue_prog' )
    if continue_prog:
        mn.build()
        mn.start()
        cfg_mod.post_start_config( mn, extra, local_varanus_home )
        mn.interact()


def build_arg_parser():
    fmtcls = argparse.ArgumentDefaultsHelpFormatter
    parser = argparse.ArgumentParser( formatter_class=fmtcls )

    parser.add_argument( '--config', \
                         help='The name of a config file, inside \'config\' directory, containing pre/post-start configuration methods.', \
                         metavar='FILE', \
                         default='null' )

    parser.add_argument( '--of', \
                         help='The OpenFlow version to use', \
                         metavar='VERSION', \
                         default='14' )

    parser.add_argument( '--arp', \
                         action='store_true', \
                         help='Hosts will be automatically configured with ARP entries', \
                         default=False )

    parser.add_argument( '--novaranus', \
                         action='store_true', \
                         help='Disable VARANUS usage', \
                         default=False )

    parser.add_argument( '-v', \
                         help='The level of verbosity', \
                         metavar='VERBOSITY', \
                         choices=LEVELS, \
                         default='info' )
    return parser


def import_config_module( cfg_file ):
    """ Returns valid imported config module.
    """
    cfg_file = re.sub( r'\.py$', '', cfg_file )
    cfg_file = re.sub( r'-', '_', cfg_file )
    mod_name = 'config.' + cfg_file
    cfg_mod = importlib.import_module( mod_name )

    if not hasattr( cfg_mod, 'pre_start_config' ):
        raise ImportError( 'Config file must define \'pre_start_config\' method' )
    if not hasattr( cfg_mod, 'post_start_config' ):
        raise ImportError( 'Config file must define \'post_start_config\' method' )

    return cfg_mod


if __name__ == '__main__':
    run()
