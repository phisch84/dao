package com.schoste.ddd.infrastructure.dal.v2.services;

import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import com.schoste.ddd.infrastructure.dal.v2.models.GenericDataObject;

/**
 * A generic stub for an implementation of the {@link LazyLoader} interface.
 * 
 * @param <SR> the data type of a potential source record that needs to be converted into the actual data object
 * @param <DO> the data type of the actual data object returned by the loader.
 */
abstract public class GenericLazyLoader<SR, DO extends GenericDataObject> implements LazyLoader<SR, DO>
{
    protected Function<SR, DO> sourceRecordToDataObjConversionFn;

    public GenericLazyLoader(Function<SR, DO> sourceRecordToDataObjConversionFn)
    {
        if (sourceRecordToDataObjConversionFn == null) throw new IllegalArgumentException();

        this.sourceRecordToDataObjConversionFn = sourceRecordToDataObjConversionFn;
    }

    /**
     * Implements {@link Runnable#run()}.
     * Callback method for {@link java.util.stream.Stream#onClose(Runnable)} which will be called by the encapsulating {@link Stream} class
     * when it is closed.
     */
    @Override
    public void run() 
    {
        try
        {
            this.close();
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
        }
    }

    /**
     * Implements {@link Spliterator#tryAdvance(Consumer)}.
     * Called by the encapsulating {@link Stream} class when the next record is requested.
     * Needs to be implemented by the deriving class.
     * 
     * @param action the consumer of the stream that will take the next data object when {@link Consumer#accept(Object)} is called by this method.
     * @return true if a data object was provided, false if there are no more data objects
     */
    @Override
    abstract public boolean tryAdvance(Consumer<? super DO> action);

    /**
     * Implements {@link Spliterator#trySplit()}.
     * May be implemented by the deriving class.
     * Unused in this implementation, but can be overwritten and implemented if needed.
     * 
     * @return an instance of a {@link Spliterator} with a subset of elements
     */
    @Override
    public Spliterator<DO> trySplit()
    {
        return null;
    }

    /**
     * Implements {@link Spliterator#estimateSize()}.
     * Needs to be implemented by the deriving class.
     * 
     * @return an estimated amount of elements available. {@link Long#MAX_VALUE} in case no estimate can be given.
     */
    @Override
    public long estimateSize()
    {
        return Long.MAX_VALUE;
    }

    /**
     * Implements {@link Spliterator#characteristics()}.
     * May be implemented by the deriving class.
     * In this implementation it simply reports an immutable collection
     * 
     * @return a value indicating the characteristics of the collection
     */
    @Override
    public int characteristics()
    {
        return Spliterator.IMMUTABLE;
    }

    /**
     * Implements {@link AutoCloseable#close()}.
     * Called by {@link GenericLazyLoader#run()} which will be called by the encapsulating {@link Stream} class
     * when it is closed.
     */
    abstract public void close();
}
