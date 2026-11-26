package com.schoste.ddd.infrastructure.dao.v4.services;

import java.io.Serializable;
import java.util.Spliterator;

import com.schoste.ddd.infrastructure.dao.v4.models.DataObject;

/**
 * A data object loader loads the data objects of a stream lazily from the storage: {@link AbstractDAO} wraps it in the
 * {@link java.util.stream.Stream} returned by {@link DAO#get(java.util.Collection)}, {@link DAO#getAll(DeletedDataObjects)} and
 * {@link DAO#getModifiedSince(long, DeletedDataObjects)}, and registers it as the stream's close handler ({@link Runnable}),
 * so closing the stream closes the loader and releases the storage's resources.
 *
 * Implementations of {@link AbstractDAO} usually create a {@link SourceRecordDataObjectLoader}.
 *
 * @param <SR> the class of the storage's source records which are converted into data objects
 * @param <ID> the class of the data objects' ids
 * @param <DO> the class of the data objects
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 */
public interface DataObjectLoader<SR, ID extends Serializable, DO extends DataObject<ID>> extends Spliterator<DO>, Runnable, AutoCloseable
{

}
