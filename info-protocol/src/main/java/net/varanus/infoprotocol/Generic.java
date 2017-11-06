package net.varanus.infoprotocol;


import static net.varanus.infoprotocol.Generic.GenericType.PACKET;
import static net.varanus.infoprotocol.Generic.GenericType.ROUTE;
import static net.varanus.infoprotocol.Generic.GenericType.STATISTICS;
import static net.varanus.infoprotocol.Generic.GenericType.TOPOLOGY;

import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;

import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

import com.google.common.base.Preconditions;

import net.varanus.infoprotocol.Generic.GenericTyped;
import net.varanus.util.annotation.FieldsAreNonnullByDefault;
import net.varanus.util.annotation.ReturnValuesAreNonnullByDefault;
import net.varanus.util.io.exception.IOChannelReadException;
import net.varanus.util.io.exception.IOChannelWriteException;
import net.varanus.util.io.serializer.IOReader;
import net.varanus.util.io.serializer.IOWriter;


/**
 * @param <TYPE>
 * @param <T>
 * @param <S>
 * @param <R>
 * @param <P>
 */
@Immutable
@FieldsAreNonnullByDefault
@ParametersAreNonnullByDefault
@ReturnValuesAreNonnullByDefault
//@formatter:off
abstract class Generic<TYPE extends GenericTyped,
                       T extends Generic.Topo,
                       S extends Generic.Stat,
                       R extends Generic.Route,
                       P extends Generic.Packet>
{//@formatter:on
    private final TYPE        type;
    private final Optional<T> topo;
    private final Optional<S> stat;
    private final Optional<R> route;
    private final Optional<P> packet;

    protected Generic( TYPE type, T topo )
    {
        Preconditions.checkArgument(type.type().equals(TOPOLOGY));
        this.type = type;
        this.topo = Optional.of(topo);
        this.stat = Optional.empty();
        this.route = Optional.empty();
        this.packet = Optional.empty();
    }

    protected Generic( TYPE type, S stat )
    {
        Preconditions.checkArgument(type.type().equals(STATISTICS));
        this.type = type;
        this.topo = Optional.empty();
        this.stat = Optional.of(stat);
        this.route = Optional.empty();
        this.packet = Optional.empty();
    }

    protected Generic( TYPE type, R route )
    {
        Preconditions.checkArgument(type.type().equals(ROUTE));
        this.type = type;
        this.topo = Optional.empty();
        this.stat = Optional.empty();
        this.route = Optional.of(route);
        this.packet = Optional.empty();
    }

    protected Generic( TYPE type, P packet )
    {
        Preconditions.checkArgument(type.type().equals(PACKET));
        this.type = type;
        this.topo = Optional.empty();
        this.stat = Optional.empty();
        this.route = Optional.empty();
        this.packet = Optional.of(packet);
    }

    public final TYPE getType()
    {
        return type;
    }

    public final T forTopology() throws NoSuchElementException
    {
        return topo.orElseThrow(() -> new NoSuchElementException("called incorrect method"));
    }

    public final S forStatistics() throws NoSuchElementException
    {
        return stat.orElseThrow(() -> new NoSuchElementException("called incorrect method"));
    }

    public final R forRoute() throws NoSuchElementException
    {
        return route.orElseThrow(() -> new NoSuchElementException("called incorrect method"));
    }

    public final P forPacket() throws NoSuchElementException
    {
        return packet.orElseThrow(() -> new NoSuchElementException("called incorrect method"));
    }

    @Override
    public final String toString()
    {
        switch (type.type()) {
            case TOPOLOGY:
                return "topology" + forTopology().toString();

            case STATISTICS:
                return "statistics" + forStatistics().toString();

            case ROUTE:
                return "route" + forRoute().toString();

            case PACKET:
                return "packet" + forPacket().toString();

            default:
                throw new AssertionError("unexpected enum value");
        }
    }

    protected static enum GenericType
    {
        TOPOLOGY, STATISTICS, ROUTE, PACKET;
    }

    protected static interface GenericTyped
    {
        GenericType type();
    }

    protected static abstract class Topo
    {
        // helper class
    }

    protected static abstract class Stat
    {
        // helper class
    }

    protected static abstract class Route
    {
        // helper class
    }

    protected static abstract class Packet
    {
        // helper class
    }

    protected static final class IO
    {
        //@formatter:off
        public static <TYPE extends GenericTyped,
                       T extends Generic.Topo,
                       S extends Generic.Stat,
                       R extends Generic.Route,
                       P extends Generic.Packet,
                       G extends Generic<TYPE, T, S, R, P>>
                       IOWriter<G> writer( IOWriter<TYPE> typeWriter,
                                           IOWriter<T> topoWriter,
                                           IOWriter<S> statWriter,
                                           IOWriter<R> routeWriter,
                                           IOWriter<P> packetWriter )
        {//@formatter:on
            return new IOWriter<G>() {
                @Override
                public void write( G generic, WritableByteChannel ch ) throws IOChannelWriteException
                {
                    TYPE type = generic.getType();
                    typeWriter.write(type, ch);
                    switch (type.type()) {
                        case TOPOLOGY:
                            topoWriter.write(generic.forTopology(), ch);
                        break;

                        case STATISTICS:
                            statWriter.write(generic.forStatistics(), ch);
                        break;

                        case ROUTE:
                            routeWriter.write(generic.forRoute(), ch);
                        break;

                        case PACKET:
                            packetWriter.write(generic.forPacket(), ch);
                        break;

                        default:
                            throw new AssertionError("unexpected enum value");
                    }
                }
            };
        }

        //@formatter:off
        public static <TYPE extends GenericTyped,
                       T extends Generic.Topo,
                       S extends Generic.Stat,
                       R extends Generic.Route,
                       P extends Generic.Packet,
                       G extends Generic<TYPE, T, S, R, P>>
                       IOReader<G> reader( IOReader<TYPE> typeReader,
                                           IOReader<T> topoReader,
                                           Function<T, G> topoFactory,
                                           IOReader<S> statReader,
                                           Function<S, G> statFactory,
                                           IOReader<R> routeReader,
                                           Function<R, G> routeFactory,
                                           IOReader<P> packetReader,
                                           Function<P, G> packetFactory )
        {//@formatter:on
            return new IOReader<G>() {
                @Override
                public G read( ReadableByteChannel ch ) throws IOChannelReadException
                {
                    TYPE type = typeReader.read(ch);
                    switch (type.type()) {
                        case TOPOLOGY:
                            return topoFactory.apply(topoReader.read(ch));

                        case STATISTICS:
                            return statFactory.apply(statReader.read(ch));

                        case ROUTE:
                            return routeFactory.apply(routeReader.read(ch));

                        case PACKET:
                            return packetFactory.apply(packetReader.read(ch));

                        default:
                            throw new AssertionError("unexpected enum value");
                    }
                }
            };
        }
    }
}
