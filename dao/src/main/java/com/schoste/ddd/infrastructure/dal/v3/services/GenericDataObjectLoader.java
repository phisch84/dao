package com.schoste.ddd.infrastructure.dal.v3.services;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.schoste.ddd.infrastructure.dal.v3.exceptions.BrokenRecordException;
import com.schoste.ddd.infrastructure.dal.v3.exceptions.DALRuntimeException;
import com.schoste.ddd.infrastructure.dal.v3.models.GenericDataObject;
import com.schoste.ddd.infrastructure.dal.v3.services.listeners.GetListener;

/**
 * Implementation of the {@link DataObjectLoader} (DOL) interface that loads data objects from a stream of source records.
 * Every source record is converted into a data object by a conversion function, and the data object is only reported
 * if {@link #filterIn(GenericDataObject)} accepts it. Before a data object is reported,
 * {@link GetListener#onAfterGet(GenericDataObject)} is called for it (see {@link #raiseAfterGet(GenericDataObject)}).
 *
 * By default, data objects are filtered by their ids. Deriving classes can change the filter by overriding
 * {@link #filterIn(GenericDataObject)}, and react to the end of the data by overriding {@link #onEndOfData()}
 * (see {@link GenericRecentDataObjectLoader}).
 *
 * Source records which can't be converted to a data object because none was found (the conversion function returns null) are skipped.
 * Source records which can't be converted because they are broken cause a {@link DALRuntimeException} with a {@link BrokenRecordException}
 * as cause, after which the loading can be continued (see {@link #tryAdvance(Consumer)}).
 * Any other failure while loading ends the stream with a {@link DALRuntimeException}.
 *
 * @param <SR> the data type of a potential source record (SR) that can be turned into a data object
 * @param <ID> the data type of the data objects' identifier (see {@link GenericDataObject#getId()} and {@link GenericDataObject#setId(Serializable)})
 * @param <DO> the actual data type of the data objects (DOs) that are loaded
 */
public class GenericDataObjectLoader<SR, ID extends Serializable, DO extends GenericDataObject<ID>> implements DataObjectLoader<SR, ID, DO>
{
    /**
     * The DAO for which the loader was created. Its {@link GetListener}s are notified by {@link #raiseAfterGet(GenericDataObject)}.
     */
    protected GenericDataAccessObject<ID, DO> dataAccessObject;

    /**
     * The stream of source records from the data source. It is closed when the loader is closed.
     */
    protected Stream<SR> sourceRecords;

    /**
     * The iterator over {@link #sourceRecords} the source records are polled from.
     */
    protected Iterator<SR> sourceRecordIterator;

    /**
     * The function to convert source records from the data source to data objects. It may return null
     * if no data object can be created for a source record.
     */
    protected Function<SR, DO> sourceRecordToDataObjConversionFn;

    /**
     * The ids of the data objects to report. If empty, all data objects are reported.
     */
    protected Set<ID> idsToFilterIn;

    /**
     * Creates a new instance of the loader
     *
     * @param dataAccessObject the DAO for which the loader was created
     * @param sourceRecords the stream of source records from the data source. It is closed when the loader is closed
     * @param sourceRecordToDataObjConversionFn a method to convert source records from the data source to data objects
     * @param idsToFilterIn the ids of the data objects to report. If null or empty, all data objects are reported
     * @throws IllegalArgumentException thrown if dataAccessObject, sourceRecords or sourceRecordToDataObjConversionFn are null
     */
    public GenericDataObjectLoader(GenericDataAccessObject<ID, DO> dataAccessObject, Stream<SR> sourceRecords, Function<SR, DO> sourceRecordToDataObjConversionFn, Set<ID> idsToFilterIn) throws IllegalArgumentException
    {
        if (dataAccessObject == null) throw new IllegalArgumentException("dataAccessObject");
        if (sourceRecords == null) throw new IllegalArgumentException("sourceRecords");
        if (sourceRecordToDataObjConversionFn == null) throw new IllegalArgumentException("sourceRecordToDataObjConversionFn");

        this.dataAccessObject = dataAccessObject;
        this.sourceRecords = sourceRecords;
        this.sourceRecordIterator = sourceRecords.iterator();
        this.sourceRecordToDataObjConversionFn = sourceRecordToDataObjConversionFn;
        this.idsToFilterIn = (idsToFilterIn == null) ? Collections.<ID>emptySet() : new HashSet<>(idsToFilterIn);
    }

    /**
     * Determines whether a data object shall be reported or not.
     * A data object is discarded if it is null (i.e. the source record could not be converted, e.g. because no data object was found),
     * or if ids to filter for were given and its id is not one of them.
     *
     * @param dataObj the data object to evaluate
     * @return true if the data object shall be reported, false if it shall be discarded
     */
    protected boolean filterIn(DO dataObj)
    {
        if (dataObj == null) return false;

        return this.idsToFilterIn.isEmpty() || this.idsToFilterIn.contains(dataObj.getId());
    }

    /**
     * Called by {@link #tryAdvance(Consumer)} when there are no more source records, i.e. the stream was consumed to its end.
     * It is not called if loading failed. Does nothing in this implementation.
     */
    protected void onEndOfData()
    {

    }

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
        for (GetListener<ID, DO> listener : this.dataAccessObject.getOnGetListeners()) dataObject = listener.onAfterGet(dataObject);

        return dataObject;
    }

    /**
     * Calls a supplier and re-throws its exceptions as {@link DALRuntimeException}.
     *
     * @param <T> the class of the supplier's result
     * @param supplier the supplier to call
     * @return the result of the supplier
     * @throws DALRuntimeException thrown if the supplier throws any exception
     */
    private static <T> T unchecked(Supplier<T> supplier) throws DALRuntimeException
    {
        try
        {
            return supplier.get();
        }
        catch (DALRuntimeException e)
        {
            throw e;
        }
        catch (RuntimeException e)
        {
            throw new DALRuntimeException(e);
        }
    }

    /**
     * Converts a source record to a data object via the conversion function.
     *
     * @param sourceRecord the source record to convert
     * @return the data object, or null if no data object was found for the source record
     * @throws DALRuntimeException thrown if the source record can't be converted (with a {@link BrokenRecordException} as cause)
     */
    protected DO convert(SR sourceRecord) throws DALRuntimeException
    {
        try
        {
            return this.sourceRecordToDataObjConversionFn.apply(sourceRecord);
        }
        catch (DALRuntimeException e)
        {
            throw e;
        }
        catch (RuntimeException e)
        {
            throw new DALRuntimeException(new BrokenRecordException(sourceRecord, e));
        }
    }

    /**
     * Implements {@link Spliterator#tryAdvance(Consumer)}.
     * Polls source records until one of them yields a data object to report (see {@link #filterIn(GenericDataObject)}).
     * Before the data object is reported, {@link #raiseAfterGet(GenericDataObject)} is called.
     * If there are no more source records, {@link #onEndOfData()} is called.
     *
     * If a source record can't be converted to a data object, a {@link DALRuntimeException} with a {@link BrokenRecordException}
     * as cause is thrown. The loader is positioned after the broken record, so calling this method again continues with the next record.
     * Any other exception thrown while loading (e.g. by the source records or the listeners) ends the loading
     * and is re-thrown as {@link DALRuntimeException}. Exceptions thrown by the action are re-thrown as they are.
     *
     * @param action the consumer of the stream that will take the next data object
     * @return true if a data object was provided, false if there are no more data objects
     * @throws DALRuntimeException thrown if loading the next data object failed
     */
    @Override
    public boolean tryAdvance(Consumer<? super DO> action) throws DALRuntimeException
    {
        while (unchecked(this.sourceRecordIterator::hasNext))
        {
            SR sourceRecord = unchecked(this.sourceRecordIterator::next);
            DO dataObj = this.convert(sourceRecord);

            if (!unchecked(() -> this.filterIn(dataObj))) continue;

            // raise the get-listener after the DO was loaded but before it is accepted in the output stream
            action.accept(unchecked(() -> this.raiseAfterGet(dataObj)));

            return true;
        }

        unchecked(() -> { this.onEndOfData(); return null; });

        return false;
    }

    /**
     * Implements {@link Runnable#run()}.
     * Callback method for {@link Stream#onClose(Runnable)} which will be called by the encapsulating {@link Stream} class
     * when it is closed. It calls {@link #close()}.
     *
     * @throws DALRuntimeException thrown if closing the loader failed
     */
    @Override
    public void run() throws DALRuntimeException
    {
        unchecked(() -> { this.close(); return null; });
    }

    /**
     * Implements {@link Spliterator#trySplit()}. Splitting is not supported by this implementation.
     *
     * @return always null
     */
    @Override
    public Spliterator<DO> trySplit()
    {
        return null;
    }

    /**
     * Implements {@link Spliterator#estimateSize()}.
     *
     * @return always {@link Long#MAX_VALUE}, as no estimate can be given
     */
    @Override
    public long estimateSize()
    {
        return Long.MAX_VALUE;
    }

    /**
     * Implements {@link Spliterator#characteristics()}.
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
     * Implements {@link AutoCloseable#close()} by closing the stream of source records.
     * Called by {@link #run()}, which is registered as close handler ({@link Stream#onClose(Runnable)}) of the encapsulating
     * {@link Stream}, when the stream is closed.
     */
    @Override
    public void close()
    {
        this.sourceRecords.close();
    }
}
