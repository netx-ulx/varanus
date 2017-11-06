package net.varanus.util.io;


import java.io.Closeable;
import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.Channel;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.IllegalSelectorException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.SelectorProvider;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.collect.CollectionUtils;
import net.varanus.util.io.exception.IOSelectException;


/**
 * This wraps a {@link Selector} instance and automatically closes every
 * registered channel when it closes. Additionally, if an exception is thrown
 * during channel registration this proxy also closes the provided channel.
 * <p>
 * This class also provides some convenience methods that are absent from
 * {@code Selector}.
 */
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
public final class SelectorProxy implements Closeable
{
    public static SelectorProxy open() throws IOException
    {
        return of(Selector.open());
    }

    public static SelectorProxy of( Selector sel )
    {
        return new SelectorProxy(sel);
    }

    private final Selector delegate;

    private SelectorProxy( Selector delegate )
    {
        this.delegate = Objects.requireNonNull(delegate);
    }

    /**
     * Returns the wrapped {@code Selector} instance used by this proxy.
     * 
     * @return a {@code Selector} instance
     */
    public Selector getSelector()
    {
        return delegate;
    }

    public boolean isOpen()
    {
        return delegate.isOpen();
    }

    public SelectorProvider provider()
    {
        return delegate.provider();
    }

    public Set<SelectionKey> keys()
    {
        return delegate.keys();
    }

    public Set<SelectionKey> selectedKeys()
    {
        return delegate.selectedKeys();
    }

    public int selectNow() throws IOSelectException
    {
        return NetworkChannelUtils.selectNow(delegate);
    }

    public int select() throws IOSelectException
    {
        return NetworkChannelUtils.select(delegate);
    }

    public int select( long timeout ) throws IOSelectException
    {
        return NetworkChannelUtils.select(delegate, timeout);
    }

    /**
     * Convenience method that behaves the same as calling
     * {@code select(unit.toMillis(timeout))}.
     * 
     * @param timeout
     *            If positive, block for up to timeout units, more or less,
     *            while waiting for a channel to become ready; if zero, block
     *            indefinitely; must not be negative
     * @param unit
     *            The timeout unit
     * @return The number of keys, possibly zero, whose ready-operation sets
     *         were updated
     * @throws IOSelectException
     *             If an I/O error occurs
     * @exception ClosedSelectorException
     *                If this selector is closed
     * @exception IllegalArgumentException
     *                If the value of the timeout argument is negative
     * @exception NullPointerException
     *                If the timeout unit is {@code null}
     */
    public int select( long timeout, TimeUnit unit ) throws IOSelectException
    {
        return NetworkChannelUtils.select(delegate, unit.toMillis(timeout));
    }

    /**
     * This method calls {@link #select()} and checks the interrupted status of
     * the current thread afterwards, calling the {@link Thread#interrupted()}
     * method (which also clears the interrupted status). If the interrupted
     * status is {@code true}, this method throws an
     * {@code InterruptedException}.
     * 
     * @return The number of keys, possibly zero, whose ready-operation sets
     *         were updated
     * @throws InterruptedException
     *             If the thread interrupted status was set upon returning from
     *             a call to {@code select()}
     * @throws IOSelectException
     *             If an I/O error occurs
     */
    public int selectInterruptibly() throws InterruptedException, IOSelectException
    {
        IOSelectException iose = null;
        try {
            return select();
        }
        catch (IOSelectException e) {
            iose = e;
            throw e;
        }
        finally {
            // Do not touch the interrupted status of the thread and do not
            // throw an InterruptedException in case an IOException is thrown.
            if (iose == null && Thread.interrupted()) {
                throw new InterruptedException();
            }
        }
    }

    /**
     * This method calls {@link #select(long, TimeUnit)}, passing the provided
     * arguments, and checks the interrupted status of the current thread
     * afterwards, calling the {@link Thread#interrupted()} method (which also
     * clears the interrupted status). If the interrupted status is
     * {@code true}, this method throws an {@code InterruptedException}.
     * 
     * @param timeout
     *            If positive, block for up to timeout units, more or less,
     *            while waiting for a channel to become ready; if zero, block
     *            indefinitely; must not be negative
     * @param unit
     *            The timeout unit
     * @return The number of keys, possibly zero, whose ready-operation sets
     *         were updated
     * @throws InterruptedException
     *             If the thread interrupted status was set upon returning from
     *             a call to {@code select()}
     * @throws IOSelectException
     *             If an I/O error occurs
     * @exception IllegalArgumentException
     *                If the value of the timeout argument is negative
     * @exception NullPointerException
     *                If the timeout unit is {@code null}
     */
    public int selectInterruptibly( long timeout, TimeUnit unit ) throws InterruptedException, IOSelectException
    {
        IOSelectException iose = null;
        try {
            return select(timeout, unit);
        }
        catch (IOSelectException e) {
            iose = e;
            throw e;
        }
        finally {
            // Do not touch the interrupted status of the thread and do not
            // throw an InterruptedException in case an IOException is
            // thrown.
            if (iose == null && Thread.interrupted()) {
                throw new InterruptedException();
            }
        }
    }

    public Selector wakeup()
    {
        return delegate.wakeup();
    }

    /**
     * Provides the same functionality as {@link Selector#close()}, except that
     * the channels associated with uncancelled keys are also closed beforehand.
     * <p>
     * <b>Note that if a channel is registered concurrently when this method is
     * called, there is no guarantee that the new channel will also be closed.
     * Callers should take care to disallow concurrent channel registration and
     * selector closing, in order to have predictable behaviour.</b>
     * 
     * @throws IOException
     */
    @Override
    public void close() throws IOException
    {
        // First try to retrieve the channels associated with uncancelled keys
        // (this set may be changed concurrently, so we do not guarantee that
        // newly added channels will also be closed)

        final Set<Channel> channels;
        try {
            channels = CollectionUtils.toSet(delegate.keys(), SelectionKey::channel);
        }
        catch (ClosedSelectorException e) {
            // if the delegate is already closed, then this method should simply
            // return, as specified in Selector.close()
            return;
        }

        // Then close the retrieved channels, storing any thrown IOExceptions in
        // a chain of suppressed exceptions

        IOException channelExceptions = null;
        try {
            for (Channel ch : channels) {
                try {
                    ch.close();
                }
                catch (IOException e) {
                    if (channelExceptions == null) {
                        channelExceptions = e;
                    }
                    else {
                        channelExceptions.addSuppressed(e);
                    }
                }
            }
        }
        finally {
            // Finally close the delegate selector while making sure that no
            // exception is lost.
            // The possibilities are:
            //
            // A) The Selector.close() method returns normally. Additionally:
            // -- A.a) If there any previously captured channel IOExceptions
            // -- ---- then they are propagated to the caller of this method
            //
            // B) The Selector.close() method throws an IOException, which is
            // -- propagated to the caller of this method. Additionally:
            // -- B.a) If there any previously captured channel IOExceptions
            // -- ---- then they are suppressed into the exception thrown in
            // -- ---- Selector.close()

            IOException selectorException = null;
            try {
                delegate.close();
            }
            catch (IOException e) {
                selectorException = e;
                throw e;
            }
            finally {
                if (selectorException != null && channelExceptions != null) {
                    selectorException.addSuppressed(channelExceptions);
                }
                else if (channelExceptions != null) {
                    throw channelExceptions;
                }
            }
        }
    }

    /**
     * Convenience method that behaves in the same way as calling
     * {@link #registerChannel(SelectableChannel, int, Object)
     * registerChannel(ch, ops, null)}.
     * 
     * @param ch
     *            A selectable channel
     * @param ops
     *            The interest set for the resulting key
     * @return A key representing the registration of this channel with this
     *         selector
     * @throws ClosedChannelException
     *             If the channel is closed
     * @throws IOException
     *             If some other I/O error occurs
     * @exception ClosedSelectorException
     *                If this selector is closed
     * @exception IllegalSelectorException
     *                If the channel was not created by the same provider
     *                as this selector
     * @exception CancelledKeyException
     *                If the channel is currently registered with this selector
     *                but the corresponding key has already been cancelled
     * @exception IllegalArgumentException
     *                If a bit in the <tt>ops</tt> set does not correspond to an
     *                operation that is supported by the channel, that is, if
     *                {@code set & ~ch.validOps() != 0}
     */
    public SelectionKey registerChannel( SelectableChannel ch, int ops ) throws IOException, ClosedChannelException
    {
        return registerChannel(ch, ops, null);
    }

    /**
     * Registers the provided channel into this selector by calling
     * {@link SelectableChannel#register(Selector, int, Object)
     * register(Selector, int, Object)} with the provided arguments and returns
     * the resulting selection key.
     * <p>
     * If an exception is thrown at any moment of registration, this method
     * closes the channel before returning (adding a possible exception thrown
     * on {@link Channel#close()} to the set of suppressed exceptions of the
     * thrown exception).
     * <p>
     * <em>Note: this method synchronizes on the object returned by
     * {@link SelectableChannel#blockingLock()} and sets the channel as
     * non-blocking before registering it.</em>
     * 
     * @param ch
     *            A selectable channel
     * @param ops
     *            The interest set for the resulting key
     * @param att
     *            The attachment for the resulting key; may be <tt>null</tt>
     * @return A key representing the registration of this channel with this
     *         selector
     * @throws ClosedChannelException
     *             If the channel is closed
     * @throws IOException
     *             If some other I/O error occurs
     * @exception ClosedSelectorException
     *                If this selector is closed
     * @exception IllegalSelectorException
     *                If the channel was not created by the same provider
     *                as this selector
     * @exception CancelledKeyException
     *                If the channel is currently registered with this selector
     *                but the corresponding key has already been cancelled
     * @exception IllegalArgumentException
     *                If a bit in the <tt>ops</tt> set does not correspond to an
     *                operation that is supported by the channel, that is, if
     *                {@code set & ~ch.validOps() != 0}
     */
    public SelectionKey registerChannel( SelectableChannel ch, int ops, @Nullable Object att )
        throws IOException, ClosedChannelException
    {
        try {
            synchronized (ch.blockingLock()) {
                ch.configureBlocking(false);
                return ch.register(delegate, ops, att);
            }
        }
        catch (ClosedChannelException e) {
            throw e; // no need to close the channel here
        }
        catch (IOException e) {
            try {
                ch.close();
            }
            catch (IOException e2) {
                e.addSuppressed(e2);
            }
            throw e;
        }
    }
}
