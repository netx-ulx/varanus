package net.varanus.mirroringprotocol;


import net.varanus.util.io.Serializers;
import net.varanus.util.io.serializer.IOReader;
import net.varanus.util.io.serializer.IOWriter;


/**
 * 
 */
public enum CollectionType
{
    SAMPLING,
    SKETCHING,
    PROBING;

    public static final class IO
    {
        public static IOWriter<CollectionType> writer()
        {
            return Serializers.enumWriter();
        }

        public static IOReader<CollectionType> reader()
        {
            return Serializers.enumReader(CollectionType.class);
        }

        private IO()
        {
            // not used
        }
    }
}
