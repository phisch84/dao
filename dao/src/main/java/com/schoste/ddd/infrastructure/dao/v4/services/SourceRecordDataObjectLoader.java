package com.schoste.ddd.infrastructure.dao.v4.services;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.schoste.ddd.infrastructure.dao.v4.exceptions.BrokenRecordException;
import com.schoste.ddd.infrastructure.dao.v4.exceptions.DAORuntimeException;
import com.schoste.ddd.infrastructure.dao.v4.models.DataObject;
import com.schoste.ddd.infrastructure.dao.v4.services.listeners.GetListener;

/**
 * Data object loader which loads data objects lazily from a stream of the storage's source records (e.g. files or database rows).
 * Every source record is converted into a data object by a conversion function, and the data object is only provided
 * if the filter accepts it. Before a data object is provided, {@link GetListener#onAfterGet(DataObject)} is called for it.
 *
 * Use {@link #forIds(DAO, Stream, Function, Set)} for {@link DAO#get(java.util.Collection)} and
 * {@link #forModifiedSince(DAO, Stream, Function, long, DeletedDataObjects)} for {@link DAO#getAll(DeletedDataObjects)} and
 * {@link DAO#getModifiedSince(long, DeletedDataObjects)}.
 *
 * Source records for which the conversion function returns null (no data object found) are skipped.
 * Source records which can't be converted because they are broken cause a {@link DAORuntimeException} with a
 * {@link BrokenRecordException} as cause, after which the loading can be continued (see {@link #tryAdvance(Consumer)}).
 * Any other failure while loading ends the stream with a {@link DAORuntimeException}.
 *
 * @param <SR> the class of the source records which are converted into data objects
 * @param <ID> the class of the data objects' ids
 * @param <DO> the class of the data objects
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 */
public class SourceRecordDataObjectLoader<SR, ID extends Serializable, DO extends DataObject<ID>> implements DataObjectLoader<SR, ID, DO>
{
    /**
     * The DAO for which the loader was created. Its {@link GetListener}s are notified by {@link #raiseAfterGet(DataObject)}.
     */
    protected DAO<ID, DO> dataAccessObject;

    /**
     * The stream of source records from the storage. It is closed when the loader is closed.
     */
    protected Stream<SR> sourceRecords;

    /**
     * The iterator over {@link #sourceRecords} the source records are polled from.
     */
    protected Iterator<SR> sourceRecordIterator;

    /**
     * The function which converts a source record into a data object. It may return null if no data object exists for a source record.
     */
    protected Function<SR, DO> conversionFunction;

    /**
     * The filter which decides which data objects are provided.
     */
    protected Predicate<DO> filter;

    /**
     * Creates a loader which provides the data objects with the given ids.
     *
     * @param <SR> the class of the source records
     * @param <ID> the class of the data objects' ids
     * @param <DO> the class of the data objects
     * @param dataAccessObject the DAO for which the loader is created
     * @param sourceRecords the source records; they may contain more records than the ones of the given ids
     * @param conversionFunction converts a source record into a data object, or returns null if none exists
     * @param ids the ids of the data objects to provide
     * @return a new loader
     * @throws IllegalArgumentException thrown if one of the parameters is null
     */
    public static <SR, ID extends Serializable, DO extends DataObject<ID>> SourceRecordDataObjectLoader<SR, ID, DO> forIds(DAO<ID, DO> dataAccessObject, Stream<SR> sourceRecords, Function<SR, DO> conversionFunction, Set<ID> ids) throws IllegalArgumentException
    {
        if (ids == null) throw new IllegalArgumentException("ids");

        Set<ID> idsToProvide = new HashSet<>(ids);

        return new SourceRecordDataObjectLoader<>(dataAccessObject, sourceRecords, conversionFunction, dataObj -> idsToProvide.contains(dataObj.getId()));
    }

    /**
     * Creates a loader which provides the data objects modified after a given time stamp.
     *
     * @param <SR> the class of the source records
     * @param <ID> the class of the data objects' ids
     * @param <DO> the class of the data objects
     * @param dataAccessObject the DAO for which the loader is created
     * @param sourceRecords the source records; they may contain more records than the ones to provide
     * @param conversionFunction converts a source record into a data object, or returns null if none exists
     * @param modifiedSince only data objects with a greater modification time stamp are provided
     * @param deleted whether data objects flagged as deleted are provided
     * @return a new loader
     * @throws IllegalArgumentException thrown if one of the parameters is null
     */
    public static <SR, ID extends Serializable, DO extends DataObject<ID>> SourceRecordDataObjectLoader<SR, ID, DO> forModifiedSince(DAO<ID, DO> dataAccessObject, Stream<SR> sourceRecords, Function<SR, DO> conversionFunction, long modifiedSince, DeletedDataObjects deleted) throws IllegalArgumentException
    {
        if (deleted == null) throw new IllegalArgumentException("deleted");

        return new SourceRecordDataObjectLoader<>(dataAccessObject, sourceRecords, conversionFunction,
            dataObj -> ((deleted == DeletedDataObjects.INCLUDE) || !dataObj.isDeleted()) && (dataObj.getModifiedTimestamp() > modifiedSince));
    }

    /**
     * Creates a new instance of the loader
     *
     * @param dataAccessObject the DAO for which the loader is created
     * @param sourceRecords the source records. They are closed when the loader is closed
     * @param conversionFunction converts a source record into a data object, or returns null if none exists
     * @param filter decides which data objects are provided (it is never called with null)
     * @throws IllegalArgumentException thrown if one of the parameters is null
     */
    public SourceRecordDataObjectLoader(DAO<ID, DO> dataAccessObject, Stream<SR> sourceRecords, Function<SR, DO> conversionFunction, Predicate<DO> filter) throws IllegalArgumentException
    {
        if (dataAccessObject == null) throw new IllegalArgumentException("dataAccessObject");
        if (sourceRecords == null) throw new IllegalArgumentException("sourceRecords");
        if (conversionFunction == null) throw new IllegalArgumentException("conversionFunction");
        if (filter == null) throw new IllegalArgumentException("filter");

        this.dataAccessObject = dataAccessObject;
        this.sourceRecords = sourceRecords;
        this.sourceRecordIterator = sourceRecords.iterator();
        this.conversionFunction = conversionFunction;
        this.filter = filter;
    }

    /**
     * Calls {@link GetListener#onAfterGet(DataObject)} of the DAO's get listeners, in the order of their registration.
     * A listener may change the data object or replace it; if one returns null, the data object isn't provided
     * and the remaining listeners aren't called.
     *
     * @param dataObject the loaded data object
     * @return the data object to provide, or null if a listener returned null
     */
    protected DO raiseAfterGet(DO dataObject)
    {
        for (GetListener<ID, DO> listener : this.dataAccessObject.getOnGetListeners())
        {
            dataObject = listener.onAfterGet(dataObject);

            if (dataObject == null) return null;
        }

        return dataObject;
    }

    /**
     * Calls a supplier and re-throws its exceptions as {@link DAORuntimeException}.
     *
     * @param <T> the class of the supplier's result
     * @param supplier the supplier to call
     * @return the result of the supplier
     * @throws DAORuntimeException thrown if the supplier throws any exception
     */
    private static <T> T unchecked(Supplier<T> supplier) throws DAORuntimeException
    {
        try
        {
            return supplier.get();
        }
        catch (DAORuntimeException e)
        {
            throw e;
        }
        catch (RuntimeException e)
        {
            throw new DAORuntimeException(e);
        }
    }

    /**
     * Converts a source record into a data object via the conversion function.
     *
     * @param sourceRecord the source record to convert
     * @return the data object, or null if no data object was found for the source record
     * @throws DAORuntimeException thrown if the source record can't be converted (with a {@link BrokenRecordException} as cause)
     */
    protected DO convert(SR sourceRecord) throws DAORuntimeException
    {
        try
        {
            return this.conversionFunction.apply(sourceRecord);
        }
        catch (DAORuntimeException e)
        {
            throw e;
        }
        catch (RuntimeException e)
        {
            throw new DAORuntimeException(new BrokenRecordException(sourceRecord, e));
        }
    }

    /**
     * Implements {@link Spliterator#tryAdvance(Consumer)}.
     * Polls source records until one of them yields a data object which the filter accepts, and provides it after
     * {@link #raiseAfterGet(DataObject)} was called for it.
     *
     * If a source record can't be converted into a data object, a {@link DAORuntimeException} with a {@link BrokenRecordException}
     * as cause is thrown. The loader is positioned after the broken record, so calling this method again continues with the next record.
     * Any other exception thrown while loading (e.g. by the source records or the listeners) ends the loading
     * and is re-thrown as {@link DAORuntimeException}. Exceptions thrown by the action are re-thrown as they are.
     *
     * @param action the consumer of the stream which takes the next data object
     * @return true if a data object was provided, false if there are no more data objects
     * @throws DAORuntimeException thrown if loading the next data object failed
     */
    @Override
    public boolean tryAdvance(Consumer<? super DO> action) throws DAORuntimeException
    {
        while (unchecked(this.sourceRecordIterator::hasNext))
        {
            SR sourceRecord = unchecked(this.sourceRecordIterator::next);
            DO dataObj = this.convert(sourceRecord);

            if (dataObj == null) continue;
            if (!unchecked(() -> this.filter.test(dataObj))) continue;

            DO providedDataObj = unchecked(() -> this.raiseAfterGet(dataObj));

            if (providedDataObj == null) continue;

            action.accept(providedDataObj);

            return true;
        }

        return false;
    }

    /**
     * Implements {@link Runnable#run()}: the close handler ({@link Stream#onClose(Runnable)}) of the stream which wraps the loader.
     * It calls {@link #close()}.
     *
     * @throws DAORuntimeException thrown if closing the loader failed
     */
    @Override
    public void run() throws DAORuntimeException
    {
        unchecked(() -> { this.close(); return null; });
    }

    /**
     * Implements {@link Spliterator#trySplit()}. Splitting is not supported; streams of data objects are sequential.
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
     *
     * @return {@link Spliterator#NONNULL}, as no null elements are provided
     */
    @Override
    public int characteristics()
    {
        return Spliterator.NONNULL;
    }

    /**
     * Implements {@link AutoCloseable#close()} by closing the stream of source records.
     */
    @Override
    public void close()
    {
        this.sourceRecords.close();
    }
}
