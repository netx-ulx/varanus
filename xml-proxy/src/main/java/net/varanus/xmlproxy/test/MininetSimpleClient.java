package net.varanus.xmlproxy.test;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import javax.annotation.Nullable;

import net.varanus.util.io.ExtraChannels;
import net.varanus.util.io.Serializers;
import net.varanus.util.io.exception.IOChannelReadException;
import net.varanus.util.io.exception.IOChannelWriteException;
import net.varanus.util.io.serializer.IOReader;
import net.varanus.util.io.serializer.IOWriter;
import net.varanus.util.lang.Unsigned;


/**
 * 
 */
public final class MininetSimpleClient
{
    private static final Charset       INPUT_LINE_CS         = StandardCharsets.UTF_8;
    private static final Charset       MININET_COMM_CS       = StandardCharsets.UTF_8;
    private static final PrintStream   OUTPUT                = System.out;
    private static final SocketAddress DEFAULT_PROVIDER_ADDR = new InetSocketAddress("localhost", 32770);

    public static void main( String[] args )
    {
        try {
            SocketAddress mininetAddr = getMininetAddress(args);
            try (SocketChannel ch = SocketChannel.open(mininetAddr)) {
                BufferedReader lineReader = new BufferedReader(new InputStreamReader(System.in, INPUT_LINE_CS));
                IOWriter<String> cmdWriter = Serializers.stringWriter(MININET_COMM_CS);
                IOReader<String> resultReader = Serializers.stringReader(MININET_COMM_CS);
                while (true) {
                    println();
                    println("Enter Mininet remote command. Use Ctrl-D to quit.");
                    print("> ");
                    String cmd = lineReader.readLine();
                    if (cmd == null) {
                        println();
                        break;
                    }
                    else {
                        cmdWriter.write(cmd, ch);

                        int resCode = Unsigned.byteValue(ExtraChannels.readByte(ch));
                        switch (resCode) {
                            case 0:
                                println("<null result>");
                            break;

                            case 1:
                                String result = resultReader.read(ch);
                                printf("Result: %s%n", result);
                            break;

                            case 2:
                                String exception = resultReader.read(ch);
                                printf("! Exception: %s%n", exception);
                            break;

                            default:
                                printf("!!! ERROR: received invalid result code %d%n", resCode);
                            break;
                        }
                    }
                }
            }
            catch (IOChannelWriteException e) {
                abort(String.format("IO-WRITE error: %s", e.getMessage()), e);
                return;
            }
            catch (IOChannelReadException e) {
                abort(String.format("IO-READ error: %s", e.getMessage()), e);
                return;
            }
            catch (IOException e) {
                abort(String.format("IO error: %s", e.getMessage()), e);
                return;
            }
        }
        catch (UnknownHostException e) {
            abort(String.format("Unknown host: %s", e.getMessage()));
            return;
        }
        catch (IllegalArgumentException e) {
            abort(String.format("Illegal argument: %s", e.getMessage()));
            return;
        }

        println("<done>");
    }

    private static SocketAddress getMininetAddress( String[] args )
        throws UnknownHostException, IllegalArgumentException
    {
        if (args.length > 0) {
            if (args.length > 1)
                println("WARN: Ignoring extra arguments beyond the first");
            return Utils.parseSocketAddress(args[0]);
        }
        else {
            printf(
                "INFO: No arguments provided, using default mininet address %s%n",
                DEFAULT_PROVIDER_ADDR);
            return DEFAULT_PROVIDER_ADDR;
        }
    }

    private static void abort( @Nullable String msg )
    {
        abort(msg, null);
    }

    private static void abort( @Nullable String msg, @Nullable Throwable t )
    {
        println(Objects.toString(msg, "ERROR: aborted"));
        if (t != null)
            printStackTrace(t);
        System.exit(1);
    }

    private static void print( String s )
    {
        OUTPUT.print(s);
    }

    private static void println()
    {
        OUTPUT.println();
    }

    private static void println( String s )
    {
        OUTPUT.println(s);
    }

    private static void printf( String format, Object... args )
    {
        OUTPUT.printf(format, args);
    }

    private static void printStackTrace( Throwable t )
    {
        t.printStackTrace(OUTPUT);
    }

    private MininetSimpleClient()
    {
        // not used
    }
}
