package net.varanus.util.security;


import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import net.varanus.util.lang.SizeOf;


/**
 * 
 */
public final class SecureRandoms
{
    private static final List<SRSpec> STRONG_SR_SPECS =
        Collections.unmodifiableList(Arrays.asList(
            new SRSpec("NativePRNG", "SUN", 160),
            new SRSpec("Windows-PRNG", "SunMSCAPI", 160)));

    /**
     * Asserts the availability of a {@code SecureRandom} which provides a
     * strong seed generation mechanism, with some specifically configured
     * algorithm and provider. An exception is thrown if none of the algorithms
     * or none of the providers are available.
     * <p>
     * If this method returns normally, it is safe to call the method
     * {@link #newStrongSeedSecureRandom(boolean)}.
     * 
     * @throws UnavailableSecureRandomException
     *             If none of the configured strong-seed {@code SecureRandom}
     *             algorithms or providers are available
     */
    public static void assertAvailableStrongSeedSecureRandom() throws UnavailableSecureRandomException
    {
        initStrongSeedSecureRandom();
    }

    /**
     * Returns a new {@code SecureRandom} object which provides a strong seed
     * generation mechanism, with the first available configured algorithm and
     * security provider.
     * <p>
     * This method should only be called after a successful call (i.e.,
     * returning normally) of the method
     * {@link #assertAvailableStrongSeedSecureRandom()}.
     * 
     * @param selfSeed
     *            If {@code true} the returned {@code SecureRandom} will be
     *            self-seeded
     * @return a new {@code SecureRandom} object with the configured algorithm
     *         and security provider
     * @exception IllegalStateException
     *                If none of the configured algorithms or providers are
     *                available
     */
    public static SecureRandom newStrongSeedSecureRandom( boolean selfSeed )
    {
        try {
            final SecureRandomAndSpec srandAndSpec = initStrongSeedSecureRandom();
            final SecureRandom srand = srandAndSpec.srand;

            if (selfSeed) {
                final int seedSize = SizeOf.bitsInBytes(srandAndSpec.spec.seedBitLength);
                srand.nextBytes(new byte[seedSize]);
            }

            return srand;
        }
        catch (UnavailableSecureRandomException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns a thread-safe self-seeded {@code SecureRandom} object which
     * provides a strong seed generation mechanism, with the first available
     * configured algorithm and security provider.
     * <p>
     * This method should only be called after a successful call (i.e.,
     * returning normally) of the method
     * {@link #assertAvailableStrongSeedSecureRandom()}.
     * 
     * @return a thread-safe self-seeded {@code SecureRandom} object with the
     *         configured algorithm and security provider
     * @exception IllegalStateException
     *                If none of the configured algorithms or providers are
     *                available
     */
    public static SecureRandom threadSafeSecureRandom()
    {
        return ThreadSafe.rand();
    }

    private static SecureRandomAndSpec initStrongSeedSecureRandom() throws UnavailableSecureRandomException
    {
        for (SRSpec spec : STRONG_SR_SPECS) {
            try {
                return new SecureRandomAndSpec(spec);
            }
            catch (NoSuchAlgorithmException | NoSuchProviderException e) {
                continue;
            }
        }

        throw new UnavailableSecureRandomException();
    }

    private static final class SecureRandomAndSpec
    {
        final SecureRandom srand;
        final SRSpec       spec;

        SecureRandomAndSpec( SRSpec spec ) throws NoSuchAlgorithmException, NoSuchProviderException
        {
            this.srand = SecureRandom.getInstance(spec.algorithm, spec.provider);
            this.spec = spec;
        }
    }

    private static final class SRSpec
    {
        final String algorithm;
        final String provider;
        final int    seedBitLength;

        SRSpec( String algorithm, String provider, int seedBitLength )
        {
            this.algorithm = Objects.requireNonNull(algorithm);
            this.provider = Objects.requireNonNull(provider);
            if (seedBitLength < 1) throw new IllegalArgumentException("seedBitLength < 1");
            this.seedBitLength = seedBitLength;
        }
    }

    private static final class ThreadSafe
    {
        private static final ThreadLocal<SecureRandom> TL_RANDOM = new ThreadLocal<SecureRandom>() {
            @Override
            protected SecureRandom initialValue()
            {
                return SecureRandoms.newStrongSeedSecureRandom(true);
            }
        };

        static SecureRandom rand()
        {
            return TL_RANDOM.get();
        }
    }

    private SecureRandoms()
    {
        // not used
    }
}
