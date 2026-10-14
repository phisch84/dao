package com.schoste.ddd.infrastructure.dal.v3.services;

import java.io.Serializable;

import com.schoste.ddd.infrastructure.dal.v3.models.GenericDataObject;

/**
 * Interface to a {@link DataObjectLoader} implementation used in {@link GenericDAO#getRecent()}, {@link GenericDAO#getRecent(boolean)}
 * and {@link GenericDAO#getRecent(long, boolean)}, which can filter out data objects (DO)
 * flagged as deleted ({@link GenericDataObject#getIsDeleted()} == true, unless deleted DOs shall be returned) and data objects
 * that were not modified after the DAO's most recent modification time stamp
 * ({@link GenericDataObject#getModifiedTimeStamp()} &lt;= {@link GenericDAO#getLastModifiedTimeStamp()}), or
 * after a custom time stamp if provided ({@link GenericDataObject#getModifiedTimeStamp()} &lt;= lastModTS).
 * The DAO's most recent modification time stamp is updated when a stream of such a loader was consumed to its end.
 * Note that {@link GenericDAO} does not enforce this interface, as its loader factory methods return a {@link DataObjectLoader}.
 *
 * @param <SR> the data type of a potential source record that can be turned into a data object
 * @param <ID> the data type of the data objects' identifier (see {@link GenericDataObject#getId()} and {@link GenericDataObject#setId(Serializable)})
 * @param <DO> the actual data type of the data objects that are loaded
 */
public interface RecentDataObjectLoader<SR, ID extends Serializable, DO extends GenericDataObject<ID>> extends DataObjectLoader<SR, ID, DO>
{

}
