package net.varanus.collector.internal;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.Instant;

import org.jnetpcap.PcapHeader;

import net.varanus.util.io.ByteBuffers;
import net.varanus.util.io.ByteBuffers.BufferType;


/**
 * 
 */
final class CapturedPacket
{
    private final byte[]  packet;
    private final Instant captureTime;

    CapturedPacket( PcapHeader header, ByteBuffer buffer )
    {
        this.packet = getCopyWithoutVLAN(buffer);
        this.captureTime = Instant.ofEpochSecond(header.seconds(), header.nanos());
    }

    byte[] getPacket()
    {
        return packet;
    }

    Instant getCaptureTime()
    {
        return captureTime;
    }

    private static final int VLAN_TYPE = 0x8100;

    private static final int ETHERTYPE_OFFSET = 12;
    private static final int VLAN_TAG_SIZE    = 32 / Byte.SIZE;

    private static final int BEFORE_VLAN_LENGTH = ETHERTYPE_OFFSET;
    private static final int AFTER_VLAN_OFFSET  = ETHERTYPE_OFFSET + VLAN_TAG_SIZE;

    private static byte[] getCopyWithoutVLAN( ByteBuffer buf )
    {
        int pos = buf.position();
        int lim = buf.limit();
        int len = lim - pos;

        if (len < AFTER_VLAN_OFFSET) {
            return ByteBuffers.getArrayCopy(buf);
        }
        else {
            int index = pos + ETHERTYPE_OFFSET;
            int ethType = Short.toUnsignedInt(ByteBuffers.getShortAtIndex(index, buf, ByteOrder.BIG_ENDIAN));
            if (ethType == VLAN_TYPE) {
                ByteBuffer beforeVLAN = ByteBuffers.getSlice(buf, BEFORE_VLAN_LENGTH);
                ByteBuffer afterVLAN = ByteBuffers.getSliceAtIndex(AFTER_VLAN_OFFSET, buf);

                int newLength = beforeVLAN.remaining() + afterVLAN.remaining();
                ByteBuffer copy = ByteBuffers.allocate(newLength, BufferType.ARRAY_BACKED);
                ByteBuffers.copy(beforeVLAN, copy);
                ByteBuffers.copy(afterVLAN, copy);

                return copy.array();
            }
            else {
                return ByteBuffers.getArrayCopy(buf);
            }
        }
    }
}
