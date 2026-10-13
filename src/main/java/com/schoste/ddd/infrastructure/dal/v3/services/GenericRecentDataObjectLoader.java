package com.schoste.ddd.infrastructure.dal.v3.services;

import java.io.Serializable;
import java.util.function.Consumer;
import java.util.function.Function;

import com.schoste.ddd.infrastructure.dal.v3.models.GenericDataObject;

/**
 * Default boiler plate implementation of the {@link RecentDataObjectLoader} interface.
 * The check if a data object shall be discarded or reported is handled by the ({@link filterInDataObjAndUpdateDAO}) method.
 * The body of this method is defined in the data access object ({@link dataAccessObject}).
 * 
 * @param <SR> the data type of a potential source record that needs to be converted into the actual data object
 * @param <ID> the data type of the data object's identifier
 * @param <DO> the data type of the actual data object returned by the loader.
 */
abstract public class GenericRecentDataObjectLoader<SR, ID extends Serializable, DO extends GenericDataObject<ID>> extends DataObjectLoaderBase<SR, ID, DO> implements RecentDataObjectLoader<SR, ID, DO>
{
    protected long lastModTS;
    protected long maxTS;
    protected boolean returnDeleted;

    /**
     * Creates a new instance
     * 
     * @param dataAccessObject the DAO for which the loader was created
     * @param sourceRecordToDataObjConversionFn a method to convert data records from the data source to data objects
     * @param lastModTS the modification date time stamp for filtering. Data objects with a smaller time stamp will be filtered out
     * @param returnDeleted if true, data objects flagged as deleted (see {@link GenericDataObject#getIsDeleted()}) will be returned too.
     * @throws IllegalArgumentException thrown if either dataAccessObject or sourceRecordToDataObjConversionFn are null
     */
    public GenericRecentDataObjectLoader(GenericDataAccessObject<ID, DO> dataAccessObject, Function<SR, DO> sourceRecordToDataObjConversionFn, long lastModTS, boolean returnDeleted) throws IllegalArgumentException 
    {
        super(dataAccessObject, sourceRecordToDataObjConversionFn);

        this.lastModTS = lastModTS;
        this.maxTS = lastModTS;
        this.returnDeleted = returnDeleted;
    }

    /**
     * Determines whether a data object shall be reported or not and keeps track of the biggest modification time stamp
     * found in the loaded data objects for a potential update (see {@link GenericDataAccessObject#setLastModifiedTimeStamp(long)}).
     * 
     * @param dataObj the data object to evaluate
     * @return true if the data object shall be reported, false if it shall be discarded
     */
    protected boolean filterInDataObjAndUpdateDAO(DO dataObj)
    {
        if (dataObj == null) return false;
        if (dataObj.getIsDeleted() && !this.returnDeleted) return false;
        if (dataObj.getModifiedTimeStamp() <= this.lastModTS) return false;

        this.maxTS = dataObj.getModifiedTimeStamp();

        return true;
    }

    /**
     * Gets the next source record from the data storage so it can be turned into a data object.
     * 
     * @return the next source record from the data storage or null if there are no more records or on error
     */
    abstract protected SR getNextSourceRecord();

    @Override
    public boolean tryAdvance(Consumer<? super DO> action)
    {
        try
        {
            SR sourceRecord = this.getNextSourceRecord();

            while (sourceRecord != null)
            {
                DO dataObj = (DO) super.sourceRecordToDataObjConversionFn.apply(sourceRecord);

                if (dataObj == null) break;

                if (this.filterInDataObjAndUpdateDAO(dataObj))
                {
                    // raise the get-listener after the DO was loaded but before it is accepted in the output stream
                    dataObj = this.raiseAfterGet(dataObj);

                    action.accept(dataObj);

                    return true;
                }

                sourceRecord = this.getNextSourceRecord();
            }

            // if the user chose to override the last modification time stamp, DO NOT update it on the DAO...
            if (this.lastModTS != this.dataAccessObject.getLastModifiedTimeStamp()) return false;

            // ... otherwise update it with the most recent time stamp returned from the storage
            this.dataAccessObject.setLastModifiedTimeStamp(this.maxTS);

            return false;
        }
        catch (Exception ex)
        {
            ex.printStackTrace(System.err);

            return false;
        }
    }

}
