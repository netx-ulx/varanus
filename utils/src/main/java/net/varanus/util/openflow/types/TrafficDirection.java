package net.varanus.util.openflow.types;


import javax.annotation.ParametersAreNonnullByDefault;

import org.projectfloodlight.openflow.types.PrimitiveSinkable;

import com.google.common.hash.PrimitiveSink;

import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.io.Serializers;
import net.varanus.util.io.serializer.IOReader;
import net.varanus.util.io.serializer.IOWriter;


/**
 * 
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public enum TrafficDirection implements PrimitiveSinkable
{
    INGRESS
    {
        @Override
        public TrafficDirection opposite()
        {
            return EGRESS;
        }

        @Override
        public String toString()
        {
            return "Ingress";
        }
    },

    EGRESS
    {
        @Override
        public TrafficDirection opposite()
        {
            return INGRESS;
        }

        @Override
        public String toString()
        {
            return "Egress";
        }
    };

    public abstract TrafficDirection opposite();

    @Override
    public void putTo( PrimitiveSink sink )
    {
        sink.putInt(ordinal());
    }

    public static final class IO
    {
        public static IOWriter<TrafficDirection> writer()
        {
            return Serializers.enumWriter();
        }

        public static IOReader<TrafficDirection> reader()
        {
            return Serializers.enumReader(TrafficDirection.class);
        }

        private IO()
        {
            // not used
        }
    }
}
