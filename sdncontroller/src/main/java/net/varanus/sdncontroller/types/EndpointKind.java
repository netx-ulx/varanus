package net.varanus.sdncontroller.types;


import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.openflow.types.TrafficDirection;


/**
 * 
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public enum EndpointKind
{
    SOURCE
    {
        @Override
        public EndpointKind opposite()
        {
            return DESTINATION;
        }

        @Override
        public TrafficDirection getTrafficDirection()
        {
            return TrafficDirection.EGRESS;
        }
    },

    DESTINATION
    {
        @Override
        public EndpointKind opposite()
        {
            return SOURCE;
        }

        @Override
        public TrafficDirection getTrafficDirection()
        {
            return TrafficDirection.INGRESS;
        }
    };

    public abstract EndpointKind opposite();

    public abstract TrafficDirection getTrafficDirection();

    public static EndpointKind ofDirection( TrafficDirection direction )
    {
        switch (direction) {
            case INGRESS:
                return DESTINATION;

            case EGRESS:
                return SOURCE;

            default:
                throw new AssertionError("unexpected enum value");
        }
    }
}
