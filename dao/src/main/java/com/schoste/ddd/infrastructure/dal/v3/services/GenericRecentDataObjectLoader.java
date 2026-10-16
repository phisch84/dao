package com.schoste.ddd.infrastructure.dal.v3.services;

import java.io.Serializable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import com.schoste.ddd.infrastructure.dal.v3.models.GenericDataObject;

/**
 * Data object loader for {@link GenericDataAccessObject#getRecent(long, boolean)}.
 * It only reports data objects which were modified after a given time stamp (see {@link #filterIn(GenericDataObject)}).
 * When the loading ends, it updates the most recent modification time stamp of the data access object
 * (see {@link #onEndOfData()} and {@link GenericDataAccessObject#setLastModifiedTimeStamp(long)}).
 *
 * @param <SR> the data type of a potential source record that needs to be converted into the actual data object
 * @param <ID> the data type of the data object's identifier
 * @param <DO> the data type of the actual data object returned by the loader.
 */
public class GenericRecentDataObjectLoader<SR, ID extends Serializable, DO extends GenericDataObject<ID>> extends GenericDataObjectLoader<SR, ID, DO>
{
    /**
     * The modification time stamp given to the loader. Data objects with a smaller or equal modification time stamp are discarded.
     */
    protected long lastModTS;

    /**
     * The biggest modification time stamp of the reported data objects (initially {@link #lastModTS}).
     */
    protected long maxTS;

    /**
     * If true, data objects flagged as deleted (see {@link GenericDataObject#getIsDeleted()}) are reported too.
     */
    protected boolean returnDeleted;

    /**
     * Creates a new instance
     *
     * @param dataAccessObject the DAO for which the loader was created
     * @param sourceRecords the stream of source records from the data source. It is closed when the loader is closed
     * @param sourceRecordToDataObjConversionFn a method to convert source records from the data source to data objects
     * @param lastModTS the modification date time stamp for filtering. Data objects with a smaller or equal time stamp will be filtered out
     * @param returnDeleted if true, data objects flagged as deleted (see {@link GenericDataObject#getIsDeleted()}) will be returned too.
     * @throws IllegalArgumentException thrown if dataAccessObject, sourceRecords or sourceRecordToDataObjConversionFn are null
     */
    public GenericRecentDataObjectLoader(GenericDataAccessObject<ID, DO> dataAccessObject, Stream<SR> sourceRecords, Function<SR, DO> sourceRecordToDataObjConversionFn, long lastModTS, boolean returnDeleted) throws IllegalArgumentException
    {
        super(dataAccessObject, sourceRecords, sourceRecordToDataObjConversionFn, null);

        this.lastModTS = lastModTS;
        this.maxTS = lastModTS;
        this.returnDeleted = returnDeleted;
    }

    /**
     * Determines whether a data object shall be reported or not and keeps track of the biggest modification time stamp
     * found in the reported data objects for a potential update (see {@link #onEndOfData()}).
     *
     * A data object is discarded if it is null, if it is flagged as deleted and the loader shall not return deleted
     * data objects, or if its modification time stamp is not greater than the time stamp given to the loader.
     *
     * @param dataObj the data object to evaluate
     * @return true if the data object shall be reported, false if it shall be discarded
     */
    @Override
    protected boolean filterIn(DO dataObj)
    {
        if (dataObj == null) return false;
        if (dataObj.getIsDeleted() && !this.returnDeleted) return false;
        if (dataObj.getModifiedTimeStamp() <= this.lastModTS) return false;

        this.maxTS = Math.max(this.maxTS, dataObj.getModifiedTimeStamp());

        return true;
    }

    /**
     * Called by {@link #tryAdvance(Consumer)} when the stream was consumed to its end.
     * Sets the last modification time stamp of the data access object to the biggest modification time stamp of the reported data objects.
     * This only happens if the time stamp given to the loader still equals {@link GenericDataAccessObject#getLastModifiedTimeStamp()},
     * i.e. it was not overridden by the caller.
     */
    @Override
    protected void onEndOfData()
    {
        // if the user chose to override the last modification time stamp, DO NOT update it on the DAO...
        if (this.lastModTS != this.dataAccessObject.getLastModifiedTimeStamp()) return;

        // ... otherwise update it with the most recent time stamp returned from the storage
        this.dataAccessObject.setLastModifiedTimeStamp(this.maxTS);
    }
}
