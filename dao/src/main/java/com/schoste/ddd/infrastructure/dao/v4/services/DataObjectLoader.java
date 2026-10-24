package com.schoste.ddd.infrastructure.dao.v4.services;

import java.io.Serializable;
import java.util.Spliterator;

import com.schoste.ddd.infrastructure.dao.v4.models.DataObject;

/**
 * Interface to the implementation of a data object loader (DOL) that is used in {@link AbstractDAO#getAll()},
 * {@link AbstractDAO#get(java.util.Collection)} (see {@link SourceRecordDataObjectLoader})
 * and {@link AbstractDAO#getRecent()}, {@link AbstractDAO#getRecent(boolean)}, {@link AbstractDAO#getModifiedSince(long, DeletedDataObjects)}
 * (see {@link GenericRecentDataObjectLoader}).
 * The DAO wraps the loader in a {@link java.util.stream.Stream} and registers it as the stream's close handler ({@link Runnable}).
 *
 * @param <SR> the data type of a potential source record (SR) that can be turned into a data object
 * @param <ID> the data type of the data objects' identifier (see {@link DataObject#getId()} and {@link DataObject#setId(Serializable)})
 * @param <DO> the actual data type of the data objects (DOs) that are loaded
 */
public interface DataObjectLoader<SR, ID extends Serializable, DO extends DataObject<ID>> extends Spliterator<DO>, Runnable, AutoCloseable
{

}
