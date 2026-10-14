package com.schoste.ddd.infrastructure.dal.v3.services;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import com.schoste.ddd.infrastructure.dal.v3.models.GenericDataObject;

/**
 * A generic stub for an implementation of the {@link DataObjectLoader} interface.
 * 
 * @param <SR> the data type of a potential source record that needs to be converted into the actual data object
 * @param <ID> the data type of the data object's identifier
 * @param <DO> the data type of the actual data object returned by the loader.
 */
abstract public class GenericDefaultDataObjectLoader<SR, ID extends Serializable, DO extends GenericDataObject<ID>> extends DataObjectLoaderBase<SR, ID, DO> implements DefaultDataObjectLoader<SR, ID, DO>
{
    protected Set<ID> idsToFilterIn;

    /**
     * Creates a new instance that will not filter for specific data object ids ({@link GenericDataObject#getId()})
     * 
     * @param dataAccessObject the DAO for which the loader was created
     * @param sourceRecordToDataObjConversionFn a method to convert data records from the data source to data objects
     * @throws IllegalArgumentException thrown if either dataAccessObject or sourceRecordToDataObjConversionFn are null
     */
    public GenericDefaultDataObjectLoader(GenericDataAccessObject<ID, DO> dataAccessObject, Function<SR, DO> sourceRecordToDataObjConversionFn) throws IllegalArgumentException
    {
        super(dataAccessObject, sourceRecordToDataObjConversionFn);
        this.idsToFilterIn = new HashSet<>();
    }

    /**
     * Creates a new instance that will specific data object ids ({@link GenericDataObject#getId()})
     * if at least one id to filter for was provided
     * 
     * @param dataAccessObject the DAO for which the loader was created
     * @param sourceRecordToDataObjConversionFn a method to convert data records from the data source to data objects
     * @param idsToFilterIn a set of data object ids ({@link GenericDataObject#getId()}) to filter the source record for. May be null (then no filtering is done).
     * @throws IllegalArgumentException thrown if either dataAccessObject or sourceRecordToDataObjConversionFn are null
     */
    public GenericDefaultDataObjectLoader(GenericDataAccessObject<ID, DO> dataAccessObject, Function<SR, DO> sourceRecordToDataObjConversionFn, Set<ID> idsToFilterIn) throws IllegalArgumentException
    {
        super(dataAccessObject, sourceRecordToDataObjConversionFn);

        this.idsToFilterIn = (idsToFilterIn == null) ? new HashSet<>(0) : new HashSet<>(idsToFilterIn);
    }

    /**
     * Gets the next source record from the data storage so it can be turned into a data object.
     * If there are no more records or on error this method must return null.
     * This is the only way to signal the end of the data: {@link #tryAdvance(Consumer)} keeps calling this method
     * until it returns null or a source record yields a data object to report.
     * 
     * @return the next source record from the data storage or null if there are no more records or on error
     */
    abstract protected SR getNextSourceRecord();

    /**
     * Implements {@link java.util.Spliterator#tryAdvance(Consumer)}.
     * Polls source records via {@link #getNextSourceRecord()} until one of them yields a data object to report.
     * Source records which can't be converted to a data object (the conversion function returns null, e.g. because
     * no data object was found) and data objects whose id is not in the set of ids to filter for are skipped.
     * Before a data object is reported, {@link #raiseAfterGet(GenericDataObject)} is called.
     * 
     * @param action the consumer of the stream that will take the next data object
     * @return true if a data object was provided, false if there are no more data objects or on error
     */
    @Override
    public boolean tryAdvance(Consumer<? super DO> action)
    {
        try
        {
            SR sourceRecord = this.getNextSourceRecord();

            while (sourceRecord != null)
            {
                DO dataObj = (DO) super.sourceRecordToDataObjConversionFn.apply(sourceRecord);

                // source records without data object (not found or failed to load) are skipped
                if ((dataObj != null) && ((this.idsToFilterIn.size() < 1) || this.idsToFilterIn.contains(dataObj.getId())))
                {

                    // raise the get-listener after the DO was loaded but before it is accepted in the output stream
                    dataObj = this.raiseAfterGet(dataObj);

                    action.accept(dataObj);

                    return true;
                }

                sourceRecord = this.getNextSourceRecord();
            }

            return false;
        }
        catch (Exception ex)
        {
            ex.printStackTrace(System.err);

            return false;
        }
    }

}