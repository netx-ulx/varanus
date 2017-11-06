package net.varanus.util.io;


import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketAddress;
import java.net.SocketOption;
import java.nio.channels.NetworkChannel;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.google.common.collect.ImmutableList;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.io.exception.IOChannelAcceptException;
import net.varanus.util.io.exception.IOChannelConnectException;
import net.varanus.util.io.exception.IOSelectException;


/**
 * 
 */
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class NetworkChannelUtils
{
    /**
     * Convenience method that calls
     * {@link #stubbornConnect(SocketChannelParameters)
     * stubbornConnect(SocketChannelParameters.of(remoteAddr))}
     * 
     * @param remoteAddress
     *            The remote address to connect to
     * @return a connected {@code SocketChannel} instance
     * @exception InterruptedException
     *                If the calling thread is interrupted while waiting between
     *                connection retries
     * @exception IOChannelConnectException
     *                If some {@code IOException} (other than
     *                {@code ConnectException}) is thrown during channel
     *                creation, configuration or connection (will be
     *                encapsulated inside this exception)
     */
    public static SocketChannel stubbornConnect( SocketAddress remoteAddress )
        throws InterruptedException, IOChannelConnectException
    {
        return stubbornConnect(SocketChannelParameters.of(remoteAddress));
    }

    /**
     * Creates a new {@code SocketChannel} configured according to the provided
     * parameters and attempts to connect it to the remote address specified in
     * the configuration.
     * <p>
     * If the connection fails (due to a thrown {@link ConnectException}), this
     * method will retry the connection after a short period of time, and will
     * do this repeatedly until the connection succeeds, or until the calling
     * thread is interrupted between connection retries, or until some other
     * {@code IOException} is thrown, whichever happens first.
     * 
     * @param params
     *            Configuration parameters to set up and connect the returned
     *            {@code SocketChannel}
     * @return a connected {@code SocketChannel} instance
     * @exception InterruptedException
     *                If the calling thread is interrupted while waiting between
     *                connection retries
     * @exception IOChannelConnectException
     *                If some {@code IOException} (other than
     *                {@code ConnectException}) is thrown during channel
     *                creation, configuration or connection (will be
     *                encapsulated inside this exception)
     */
    public static SocketChannel stubbornConnect( SocketChannelParameters params )
        throws InterruptedException, IOChannelConnectException
    {
        final SocketAddress remoteAddr = params.getRemoteAddress();
        final Optional<SocketAddress> localAddr = params.getLocalAddress();
        final List<SocketOptionValue<?>> optionVals = params.getOptionValues();
        for (;;) {
            try {
                SocketChannel ch = SocketChannel.open();
                if (localAddr.isPresent())
                    ch.bind(localAddr.get());
                for (SocketOptionValue<?> opVal : optionVals)
                    opVal.configure(ch);
                ch.connect(remoteAddr);
                return ch;
            }
            catch (ConnectException e) {
                // ignore, try again after a while
                TimeUnit.SECONDS.sleep(1);
                continue;
            }
            catch (IOException e) {
                throw new IOChannelConnectException(e);
            }
        }
    }

    public static SocketChannel accept( ServerSocketChannel srvCh ) throws IOChannelAcceptException
    {
        try {
            return srvCh.accept();
        }
        catch (IOException e) {
            throw new IOChannelAcceptException(e);
        }
    }

    public static @CheckForNull SocketAddress getLocalAddress( NetworkChannel netCh )
    {
        try {
            return netCh.getLocalAddress();
        }
        catch (IOException e) {
            return null;
        }
    }

    public static @CheckForNull SocketAddress getRemoteAddress( SocketChannel ch )
    {
        try {
            return ch.getRemoteAddress();
        }
        catch (IOException e) {
            return null;
        }
    }

    public static int selectNow( Selector sel ) throws IOSelectException
    {
        try {
            return sel.selectNow();
        }
        catch (IOException e) {
            throw new IOSelectException(e);
        }
    }

    public static int select( Selector sel ) throws IOSelectException
    {
        try {
            return sel.select();
        }
        catch (IOException e) {
            throw new IOSelectException(e);
        }
    }

    public static int select( Selector sel, long timeoutMillis ) throws IOSelectException
    {
        try {
            return sel.select(timeoutMillis);
        }
        catch (IOException e) {
            throw new IOSelectException(e);
        }
    }

    /**
     * Configuration parameters to set up and connect a {@link SocketChannel}.
     */
    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class SocketChannelParameters
    {
        /**
         * Returns parameters containing the provided remote address and no
         * specified local address or socket option values.
         * 
         * @param remoteAddress
         *            An address to connect a {@code SocketChannel} to
         * @return a new {@code SocketChannelParameters} instance
         */
        public static SocketChannelParameters of( SocketAddress remoteAddress )
        {
            return new SocketChannelParameters(
                Objects.requireNonNull(remoteAddress),
                Optional.empty(),
                ImmutableList.of());
        }

        /**
         * Returns parameters containing the provided remote address and local
         * address, and no socket option values.
         * 
         * @param remoteAddress
         *            An address to connect a {@code SocketChannel} to
         * @param localAddress
         *            An address to bind a {@code SocketChannel} to
         * @return a new {@code SocketChannelParameters} instance
         */
        public static SocketChannelParameters of( SocketAddress remoteAddress, SocketAddress localAddress )
        {
            return new SocketChannelParameters(
                Objects.requireNonNull(remoteAddress),
                Optional.of(localAddress),
                ImmutableList.of());
        }

        /**
         * Returns parameters containing the provided remote address and socket
         * option values, and no local address.
         * 
         * @param remoteAddress
         *            An address to connect a {@code SocketChannel} to
         * @param optionValues
         *            Socket option values to configure a {@code SocketChannel}
         * @return a new {@code SocketChannelParameters} instance
         */
        public static SocketChannelParameters of( SocketAddress remoteAddress, SocketOptionValue<?>... optionValues )
        {
            return of(remoteAddress, Arrays.asList(optionValues));
        }

        /**
         * Returns parameters containing the provided remote address and socket
         * option values, and no local address.
         * 
         * @param remoteAddress
         *            An address to connect a {@code SocketChannel} to
         * @param optionValues
         *            Socket option values to configure a {@code SocketChannel}
         * @return a new {@code SocketChannelParameters} instance
         */
        public static SocketChannelParameters of( SocketAddress remoteAddress,
                                                  Iterable<SocketOptionValue<?>> optionValues )
        {
            return new SocketChannelParameters(
                Objects.requireNonNull(remoteAddress),
                Optional.empty(),
                ImmutableList.copyOf(optionValues)); // checks for null objects
        }

        /**
         * Returns parameters containing the provided remote address, local
         * address and socket option values.
         * 
         * @param remoteAddress
         *            An address to connect a {@code SocketChannel} to
         * @param localAddress
         *            An address to bind a {@code SocketChannel} to
         * @param optionValues
         *            Socket option values to configure a {@code SocketChannel}
         * @return a new {@code SocketChannelParameters} instance
         */
        public static SocketChannelParameters of( SocketAddress remoteAddress,
                                                  SocketAddress localAddress,
                                                  SocketOptionValue<?>... optionValues )
        {
            return of(remoteAddress, localAddress, Arrays.asList(optionValues));
        }

        /**
         * Returns parameters containing the provided remote address, local
         * address and socket option values.
         * 
         * @param remoteAddress
         *            An address to connect a {@code SocketChannel} to
         * @param localAddress
         *            An address to bind a {@code SocketChannel} to
         * @param optionValues
         *            Socket option values to configure a {@code SocketChannel}
         * @return a new {@code SocketChannelParameters} instance
         */
        public static SocketChannelParameters of( SocketAddress remoteAddress,
                                                  SocketAddress localAddress,
                                                  Iterable<SocketOptionValue<?>> optionValues )
        {
            return new SocketChannelParameters(
                Objects.requireNonNull(remoteAddress),
                Optional.of(localAddress),
                ImmutableList.copyOf(optionValues)); // checks for null objects
        }

        private final SocketAddress                       remoteAddress;
        private final Optional<SocketAddress>             localAddress;
        private final ImmutableList<SocketOptionValue<?>> optionValues;

        private SocketChannelParameters( SocketAddress remoteAddress,
                                         Optional<SocketAddress> localAddress,
                                         ImmutableList<SocketOptionValue<?>> optionValues )
        {
            this.remoteAddress = remoteAddress;
            this.localAddress = localAddress;
            this.optionValues = optionValues;
        }

        /**
         * Returns the remote address to connect a {@code SocketChannel} to.
         * 
         * @return a {@code SocketAddress} instance
         */
        public SocketAddress getRemoteAddress()
        {
            return remoteAddress;
        }

        /**
         * Returns an optional local address to bind a {@code SocketChannel} to.
         * 
         * @return an optional {@code SocketAddress} instance
         */
        public Optional<SocketAddress> getLocalAddress()
        {
            return localAddress;
        }

        /**
         * Returns a list of socket option values to configure a
         * {@code SocketChannel}.
         * 
         * @return a list of {@code SocketOptionValue} instances
         */
        public List<SocketOptionValue<?>> getOptionValues()
        {
            return optionValues;
        }
    }

    /**
     * A type-safe encapsulation of a {@link SocketOption} and an associated
     * value.
     * 
     * @param <T>
     *            The type of the socket option value
     */
    @FieldsAreNonnullByDefault
    @ParametersAreNonnullByDefault
    @ReturnValuesAreNonnullByDefault
    public static final class SocketOptionValue<T>
    {
        /**
         * Returns a new {@code SocketOptionValue} containing the specified
         * socket option and associated value.
         * 
         * @param option
         *            A socket option
         * @param value
         *            The value for the socket option (may be null)
         * @return a new {@code SocketOptionValue} instance
         */
        public static <T> SocketOptionValue<T> of( SocketOption<T> option, @Nullable T value )
        {
            return new SocketOptionValue<>(Objects.requireNonNull(option), value);
        }

        private final SocketOption<T> option;
        private final @Nullable T     value;

        private SocketOptionValue( SocketOption<T> option, @Nullable T value )
        {
            this.option = option;
            this.value = value;
        }

        /**
         * Returns the socket option.
         * 
         * @return a {@code SocketOption} instance
         */
        public SocketOption<T> getOption()
        {
            return option;
        }

        /**
         * Returns the value for the socket option (may be null).
         * 
         * @return an arbitrary object
         */
        public @Nullable T getValue()
        {
            return value;
        }

        /**
         * Configures the provided channel by calling
         * {@link NetworkChannel#setOption(SocketOption, Object)
         * setOption(SocketOption&lt;T&gt;, T)} with this instance's option and
         * value.
         * 
         * @param channel
         *            A network channel to be configured
         * @throws IOException
         *             If an I/O error occurs during channel configuration
         */
        public void configure( NetworkChannel channel ) throws IOException
        {
            channel.setOption(option, value);
        }

        @Override
        public boolean equals( Object other )
        {
            return (other instanceof SocketOptionValue<?>)
                   && this.equals((SocketOptionValue<?>)other);
        }

        public boolean equals( SocketOptionValue<?> other )
        {
            return (other != null)
                   && this.option.name().equals(other.option.name())
                   && Objects.equals(this.value, other.value);
        }

        @Override
        public int hashCode()
        {
            return option.name().hashCode();
        }

        @Override
        public String toString()
        {
            return option.name() + "=" + value;
        }
    }

    private NetworkChannelUtils()
    {
        // not used
    }
}
