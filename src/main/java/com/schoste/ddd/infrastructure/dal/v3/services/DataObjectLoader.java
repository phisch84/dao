package com.schoste.ddd.infrastructure.dal.v3.services;

import java.io.Serializable;
import java.util.Spliterator;

import com.schoste.ddd.infrastructure.dal.v3.models.GenericDataObject;

/**
 * Interface to the implementation of a data object loader that is used in {@link GenericDAO#getAll()}
 * 
 * @param <SR> the data type of a potential source record that can be turned into a data object
 * @param <ID> te data type of the data objects' identifier (see {@link GenericDataObject#getId()} and {@link GenericDataObject#setId(Serializable)})
 * @param <DO> the actual data type of the data objects that are loaded
 */
public interface DataObjectLoader<SR, ID extends Serializable, DO extends GenericDataObject<ID>> extends Spliterator<DO>, Runnable, AutoCloseable
{

}
