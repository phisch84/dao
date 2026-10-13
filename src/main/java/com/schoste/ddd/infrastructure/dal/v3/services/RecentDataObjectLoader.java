package com.schoste.ddd.infrastructure.dal.v3.services;

import java.io.Serializable;

import com.schoste.ddd.infrastructure.dal.v3.models.GenericDataObject;

/**
 * Interface to a {@link DataObjectLoader} implementation which can filter out data objects (DO)
 * for source records (SR) flagged as deleted ({@link GenericDataObject#getIsDeleted()} == true) and records
 * that were not modified since the last call to {@link GenericDAO#getRecent(long, boolean)}
 * ({@link GenericDataObject#getModifiedTimeStamp()} <= {@link GenericDAO#getLastModifiedTimeStamp()})
 * 
 * @param <SR> the data type of a potential source record that can be turned into a data object
 * @param <ID> te data type of the data objects' identifier (see {@link GenericDataObject#getId()} and {@link GenericDataObject#setId(Serializable)})
 * @param <DO> the actual data type of the data objects that are loaded
 */
public interface RecentDataObjectLoader<SR, ID extends Serializable, DO extends GenericDataObject<ID>> extends DataObjectLoader<SR, ID, DO>
{

}
