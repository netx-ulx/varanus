def pre_start_config( _mr, _extra_args, _local_varanus_home ):
    """ Configure a MininetRunner object before Mininet starts.
        - mr        : a MininetRunner object
        - extra_args: extra arguments passed by the command line
    """
    return True


def post_start_config( _mr, _extra_args, _local_varanus_home ):
    """ Configure a MininetRunner object after Mininet starts.
        - mr        : a MininetRunner object
        - extra_args: extra arguments passed by the command line
    """
    pass
