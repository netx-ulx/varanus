package net.varanus.util.io;


import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.google.common.collect.Iterables;


/**
 * 
 */
public final class CloseableList implements Closeable
{
    private final List<Closeable> list;
    private final Object          writeLock;

    public CloseableList()
    {
        this.list = new ArrayList<>();
        this.writeLock = new Object();
    }

    public void add( Closeable cl )
    {
        synchronized (writeLock) {
            list.add(Objects.requireNonNull(cl));
        }
    }

    @Override
    public void close() throws IOException
    {
        synchronized (writeLock) {
            Throwable ex = null;
            try {
                // removes closeables as it iterates
                for (Closeable ac : Iterables.consumingIterable(list)) {
                    try {
                        ac.close();
                    }
                    catch (Throwable t) {
                        if (ex == null)
                            ex = t;
                        else
                            ex.addSuppressed(t);
                    }
                }
            }
            finally {
                if (ex != null) {
                    if (ex instanceof IOException)
                        throw (IOException)ex;
                    else if (ex instanceof Error)
                        throw (Error)ex;
                    else
                        throw new RuntimeException(ex);
                }
            }
        }
    }
}
