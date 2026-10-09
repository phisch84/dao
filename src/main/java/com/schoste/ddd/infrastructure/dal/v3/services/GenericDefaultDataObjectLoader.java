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

    public GenericDefaultDataObjectLoader(GenericDataAccessObject<ID, DO> dataAccessObject, Function<SR, DO> sourceRecordToDataObjConversionFn) throws IllegalArgumentException
    {
        super(dataAccessObject, sourceRecordToDataObjConversionFn);
        this.idsToFilterIn = new HashSet<>();
    }

    public GenericDefaultDataObjectLoader(GenericDataAccessObject<ID, DO> dataAccessObject, Function<SR, DO> sourceRecordToDataObjConversionFn, Set<ID> idsToFilterIn) throws IllegalArgumentException
    {
        super(dataAccessObject, sourceRecordToDataObjConversionFn);

        this.idsToFilterIn = (idsToFilterIn == null) ? new HashSet<>(0) : new HashSet<>(idsToFilterIn);
    }

    /**
     * Gets the next source record from the data storage so it can be turned into a data object.
     * If there are no more records or on error this method must return null.
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

                if (dataObj == null) return false;

                if ((this.idsToFilterIn.size() < 1) || ((this.idsToFilterIn.size() > 0) && this.idsToFilterIn.contains(dataObj.getId())))
                {

                    // raise the get-listener after the DO was loaded but before it is accepted in the output stream
                    dataObj = this.raiseAfterGet(dataObj);

                    action.accept(dataObj);

                    return true;
                }
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