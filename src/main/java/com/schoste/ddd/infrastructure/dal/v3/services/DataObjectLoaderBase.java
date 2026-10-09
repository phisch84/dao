package com.schoste.ddd.infrastructure.dal.v3.services;

import java.io.Serializable;
import java.util.Collection;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import com.schoste.ddd.infrastructure.dal.v3.models.GenericDataObject;
import com.schoste.ddd.infrastructure.dal.v3.services.listeners.GetListener;

abstract public class DataObjectLoaderBase<SR, ID extends Serializable, DO extends GenericDataObject<ID>> implements DataObjectLoader<SR, ID, DO>
{
    protected GenericDataAccessObject<ID, DO> dataAccessObject;
    protected Function<SR, DO> sourceRecordToDataObjConversionFn;

    /**
     * Calls {@link GetListener#onAfterGet(GenericDataObject)} for every {@link GetListener} registered
     * at the data access object provided to this loader after the actual data object has been retrieved
     * 
     * @param dataObject the data object that was loaded and is about to be returned
     */
    protected DO raiseAfterGet(DO dataObject)
    {
        Collection<GetListener<ID, DO>> daoGetListeners = this.dataAccessObject.getOnGetListeners();

        for (GetListener<ID, DO> listener : daoGetListeners) dataObject = listener.onAfterGet(dataObject);
    
        return dataObject;
    }

    /**
     * Creates a new instance of the loader
     * 
     * @param dataAccessObject the DAO for which the loader was created
     * @param sourceRecordToDataObjConversionFn a method to convert data records from the data source to data objects
     */
    public DataObjectLoaderBase(GenericDataAccessObject<ID, DO> dataAccessObject, Function<SR, DO> sourceRecordToDataObjConversionFn) throws IllegalArgumentException
    {
        if (dataAccessObject == null) throw new IllegalArgumentException();
        if (sourceRecordToDataObjConversionFn == null) throw new IllegalArgumentException();

        this.dataAccessObject = dataAccessObject;
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
     * When implementing the concrete DataObjectLoader, consider when to make calls to {@link GetListener#onBeforeGet(Serializable)} and {@link GetListener#onAfterGet(GenericDataObject)}
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
