package net.varanus.xmlproxy.internal;


import net.varanus.util.functional.Report;
import net.varanus.util.functional.functions.ExceptionalConsumer;


/**
 * @param <T>
 */
interface CommandResultHandler<T> extends ExceptionalConsumer<Report<T>, InterruptedException>
{
    //
}
