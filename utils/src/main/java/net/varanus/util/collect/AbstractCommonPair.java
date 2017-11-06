package net.varanus.util.collect;


import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;


/**
 * @param <T>
 */
public abstract class AbstractCommonPair<T> extends AbstractPair<T, T> implements CommonPair<T>
{
    @Override
    public Iterator<T> iterator()
    {
        return new Iterator<T>() {

            private int i = 0;

            @Override
            public boolean hasNext()
            {
                return i < 2;
            }

            @Override
            public T next()
            {
                switch (i) {
                    case 0:
                        i = 1;
                        return getFirst();

                    case 1:
                        i = 2;
                        return getSecond();

                    default:
                        throw new NoSuchElementException();
                }
            }
        };
    }

    @Override
    public Spliterator<T> spliterator()
    {
        return Spliterators.spliterator(iterator(), 2, 0);
    }
}
