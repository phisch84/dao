package com.schoste.ddd.infrastructure.dal.v2.services;

import java.util.Spliterator;

import com.schoste.ddd.infrastructure.dal.v2.models.GenericDataObject;

/**
 * Interface to the implementation of a lazy loader that is used in {@link GenericDAO#getAll(java.util.function.Predicate)}
 * 
 * @param <SR> the data type of a potential source record that can be turned into a data object
 * @param <DO> the actual data type of the data objects that are lazily loaded
 */
public interface LazyLoader<SR, DO extends GenericDataObject> extends Spliterator<DO>, Runnable, AutoCloseable
{

}
