package net.varanus.mirroringprotocol;


import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import net.varanus.mirroringprotocol.SamplingReply.IO.CompressionStrategy;


/**
 * 
 */
public final class MirroringConfig
{
    // FIXME change to SHA-3
    public static final HashFunction        HASH_FUNCTION     = Hashing.sha256();
    public static final CompressionStrategy COMPRESSION_STRAT = CompressionStrategy.MINIMIZE_MESSAGE_SIZE;

    private MirroringConfig()
    {
        // not used
    }
}
