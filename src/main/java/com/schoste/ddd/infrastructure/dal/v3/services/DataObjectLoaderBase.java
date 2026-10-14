package com.schoste.ddd.infrastructure.dal.v3.services;

import java.io.Serializable;
import java.util.Collection;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import com.schoste.ddd.infrastructure.dal.v3.models.GenericDataObject;
import com.schoste.ddd.infrastructure.dal.v3.services.listeners.GetListener;

/**
 * Basic implementation of the {@link DataObjectLoader} (DOL) interface that defines certain business rules that DOLs need to adhere to.
 * 
 * @param <SR> the data type of a potential source record (SR) that can be turned into a data object
 * @param <ID> the data type of the data objects' identifier (see {@link GenericDataObject#getId()} and {@link GenericDataObject#setId(Serializable)})
 * @param <DO> the actual data type of the data objects (DOs) that are loaded
 */
abstract public class DataObjectLoaderBase<SR, ID extends Serializable, DO extends GenericDataObject<ID>> implements DataObjectLoader<SR, ID, DO>
{
    /**
     * The DAO for which the loader was created. Its {@link GetListener}s are notified by {@link #raiseAfterGet(GenericDataObject)}.
     */
    protected GenericDataAccessObject<ID, DO> dataAccessObject;

    /**
     * The function to convert source records from the data source to data objects. It may return null
     * if no data object can be created for a source record.
     */
    protected Function<SR, DO> sourceRecordToDataObjConversionFn;

    /**
     * Calls {@link GetListener#onAfterGet(GenericDataObject)} for every {@link GetListener} registered
     * at the data access object provided to this loader after the actual data object has been retrieved.
     * {@link GetListener#onAfterGet(GenericDataObject)} might change the data object or even return a completely different instance.
     * 
     * @param dataObject the data object that was loaded and is about to be returned
     * @return the data object received back by {@link GetListener#onAfterGet(GenericDataObject)}
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
     * @exception IllegalArgumentException thrown if either dataAccessObject or sourceRecordToDataObjConversionFn are null
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
     * when it is closed. It calls {@link #close()}. Exceptions thrown by {@link #close()} are printed to {@link System#err} and not re-thrown.
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
     * When implementing the concrete DataObjectLoader, call {@link #raiseAfterGet(GenericDataObject)} for every data object
     * before it is passed to the consumer, so that {@link GetListener#onAfterGet(GenericDataObject)} is called for it.
     * {@link GetListener#onBeforeGet(Serializable)} is not meant to be called by loaders.
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
     * @return usually an instance of a {@link Spliterator} with a subset of elements. In this implementation it always returns null.
     */
    @Override
    public Spliterator<DO> trySplit()
    {
        return null;
    }

    /**
     * Implements {@link Spliterator#estimateSize()}.
     * May be implemented by the deriving class.
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
     * In this implementation it only reports {@link Spliterator#IMMUTABLE}.
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
     * Called by {@link #run()}, which is registered as close handler ({@link Stream#onClose(Runnable)}) of the encapsulating
     * {@link Stream}, when the stream is closed. Needs to be implemented by the deriving class to release the resources of the loader.
     */
    abstract public void close();
}
