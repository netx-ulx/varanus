package net.varanus.configprotocol;


import static net.varanus.configprotocol.Generic.GenericType.LINK_BANDWIDTH;
import static net.varanus.configprotocol.Generic.GenericType.LINK_ENABLING;

import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import com.google.common.base.Preconditions;

import net.varanus.configprotocol.Generic.GenericTyped;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.io.exception.IOChannelReadException;
import net.varanus.util.io.exception.IOChannelWriteException;
import net.varanus.util.io.serializer.IOReader;
import net.varanus.util.io.serializer.IOWriter;


/**
 * @param <TYPE>
 * @param <LE>
 * @param <LB>
 */
@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
abstract class Generic<TYPE extends GenericTyped, LE extends Generic.LinkEnabling, LB extends Generic.LinkBandwidth>
{
    private final TYPE         type;
    private final Optional<LE> linkEnabling;
    private final Optional<LB> linkBandwidth;

    protected Generic( TYPE type, LE linkEnabling )
    {
        Preconditions.checkArgument(type.type().equals(LINK_ENABLING));
        this.type = type;
        this.linkEnabling = Optional.of(linkEnabling);
        this.linkBandwidth = Optional.empty();
    }

    protected Generic( TYPE type, LB linkBandwidth )
    {
        Preconditions.checkArgument(type.type().equals(LINK_BANDWIDTH));
        this.type = type;
        this.linkEnabling = Optional.empty();
        this.linkBandwidth = Optional.of(linkBandwidth);
    }

    public final TYPE getType()
    {
        return type;
    }

    public final LE forLinkEnabling() throws NoSuchElementException
    {
        return linkEnabling.orElseThrow(() -> new NoSuchElementException("called incorrect method"));
    }

    public final LB forLinkBandwidth() throws NoSuchElementException
    {
        return linkBandwidth.orElseThrow(() -> new NoSuchElementException("called incorrect method"));
    }

    @Override
    public final String toString()
    {
        switch (type.type()) {
            case LINK_ENABLING:
                return "link-enabling" + forLinkEnabling().toString();

            case LINK_BANDWIDTH:
                return "link-bandwidth" + forLinkBandwidth().toString();

            default:
                throw new AssertionError("unexpected enum value");
        }
    }

    protected static enum GenericType
    {
        LINK_ENABLING, LINK_BANDWIDTH;
    }

    protected static interface GenericTyped
    {
        GenericType type();
    }

    protected static abstract class LinkEnabling
    {
        // helper class
    }

    protected static abstract class LinkBandwidth
    {
        // helper class
    }

    protected static final class IO
    {
        //@formatter:off
        public static <TYPE extends GenericTyped,
                       LE extends Generic.LinkEnabling,
                       LB extends Generic.LinkBandwidth,
                       G extends Generic<TYPE, LE, LB>>
                       IOWriter<G> writer( IOWriter<TYPE> typeWriter,
                                           IOWriter<LE> linkEnablingWriter,
                                           IOWriter<LB> linkBandwidthWriter )
        {//@formatter:on
            return new IOWriter<G>() {
                @Override
                public void write( G generic, WritableByteChannel ch ) throws IOChannelWriteException
                {
                    TYPE type = generic.getType();
                    typeWriter.write(type, ch);
                    switch (type.type()) {
                        case LINK_ENABLING:
                            linkEnablingWriter.write(generic.forLinkEnabling(), ch);
                        break;

                        case LINK_BANDWIDTH:
                            linkBandwidthWriter.write(generic.forLinkBandwidth(), ch);
                        break;

                        default:
                            throw new AssertionError("unexpected enum value");
                    }
                }
            };
        }

        //@formatter:off
        public static <TYPE extends GenericTyped,
                       LE extends Generic.LinkEnabling,
                       LB extends Generic.LinkBandwidth,
                       G extends Generic<TYPE, LE, LB>>
                       IOReader<G> reader( IOReader<TYPE> typeReader,
                                           IOReader<LE> linkEnablingReader,
                                           Function<LE, G> linkEnablingFactory,
                                           IOReader<LB> linkBandwidthReader,
                                           Function<LB, G> linkBandwidthFactory )
        {//@formatter:on
            return new IOReader<G>() {
                @Override
                public G read( ReadableByteChannel ch ) throws IOChannelReadException
                {
                    TYPE type = typeReader.read(ch);
                    switch (type.type()) {
                        case LINK_ENABLING:
                            return linkEnablingFactory.apply(linkEnablingReader.read(ch));

                        case LINK_BANDWIDTH:
                            return linkBandwidthFactory.apply(linkBandwidthReader.read(ch));

                        default:
                            throw new AssertionError("unexpected enum value");
                    }
                }
            };
        }
    }
}
