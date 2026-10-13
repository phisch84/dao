package com.schoste.ddd.infrastructure.dal.v3.services;

import java.io.Serializable;

import com.schoste.ddd.infrastructure.dal.v3.models.GenericDataObject;

/**
 * Extension of the {@link DataObjectLoader} (DOL) interface to identify implementations of DOLs used in the DAO methods
 * {@link GenericDAO#get(Serializable[])}, {@link GenericDAO#get(java.util.Collection)} and
 * {@link GenericDAO#getAll()} method.
 * These loaders are supposed to ignore the data object's (DO's) modification time stamp {@link GenericDataObject#getModifiedTimeStamp()}
 * and its deleted flag {@link GenericDataObject#getIsDeleted()}.
 * 
 * @param <SR> the data type of a potential source record (SR) that can be turned into a data object
 * @param <ID> te data type of the data objects' identifier (see {@link GenericDataObject#getId()} and {@link GenericDataObject#setId(Serializable)})
 * @param <DO> the actual data type of the data objects (DOs) that are loaded
 */
public interface DefaultDataObjectLoader<SR, ID extends Serializable, DO extends GenericDataObject<ID>> extends DataObjectLoader<SR, ID, DO>
{

}
