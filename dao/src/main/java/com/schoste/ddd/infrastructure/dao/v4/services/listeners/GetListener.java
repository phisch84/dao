package com.schoste.ddd.infrastructure.dao.v4.services.listeners;

import java.io.Serializable;

import com.schoste.ddd.infrastructure.dao.v4.models.DataObject;
import com.schoste.ddd.infrastructure.dao.v4.services.DAO;
import com.schoste.ddd.infrastructure.dao.v4.services.DataObjectLoader;
import com.schoste.ddd.infrastructure.dao.v4.services.DeletedDataObjects;

/**
 * Listener which is called before data objects are got from a DAO ({@link DAO}) and after they were loaded.
 * It may change the requested ids and the loaded data objects. Register it by {@link DAO#registerOnGetListener(GetListener)};
 * listeners are called in the order of their registration. All methods have default implementations which change nothing.
 *
 * The before-methods are called in the thread which calls the DAO. For streams, {@link #onAfterGet(DataObject)} is called
 * while the stream is consumed, i.e. possibly later and in the thread which consumes the stream.
 * An exception thrown by a listener ends the operation; the DAO throws it as {@link com.schoste.ddd.infrastructure.dao.v4.exceptions.DAOException}
 * (or as {@link com.schoste.ddd.infrastructure.dao.v4.exceptions.DAORuntimeException} while a stream is consumed).
 *
 * @param <ID> the class of the data objects' ids
 * @param <DO> the class of the data objects
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 */
public interface GetListener<ID extends Serializable, DO extends DataObject<ID>>
{
	/**
	 * Called before the data object with the given id is got ({@link DAO#get(Serializable)}).
	 * The listener may request another id by returning it.
	 *
	 * @param id the requested id
	 * @return the id to get; must not be null (the DAO throws an {@link IllegalStateException} then)
	 */
	default ID onBeforeGet(ID id) { return id; }

	/**
	 * Called before the data objects with the given ids are got ({@link DAO#get(java.util.Collection)}).
	 * It isn't called by {@link DAO#getAll()} and {@link DAO#getModifiedSince(long, DeletedDataObjects)}.
	 * The listener may request other ids by returning them; an empty array makes the DAO provide no data objects.
	 *
	 * @param ids the requested ids
	 * @return the ids to get; must not be null (the DAO throws an {@link IllegalStateException} then)
	 */
	default ID[] onBeforeGet(ID[] ids) { return ids; }

	/**
	 * Called before a stream of data objects is returned ({@link DAO#get(java.util.Collection)}, {@link DAO#getAll(DeletedDataObjects)},
	 * {@link DAO#getModifiedSince(long, DeletedDataObjects)}), with the loader which will load its data objects.
	 * The listener may change the state of the loader, but it can't replace it.
	 *
	 * @param dataObjLoader the loader which will load the data objects
	 */
	default void onBeforeGet(DataObjectLoader<?, ID, DO> dataObjLoader) { }

	/**
	 * Called after a data object was loaded and before it is provided to the caller; never with null (it isn't called if
	 * no data object was found). The listener may change the data object or replace it by returning another one;
	 * the returned data object is passed to the next listener and finally provided. If null is returned, the data object
	 * isn't provided.
	 *
	 * @param dataObject the loaded data object
	 * @return the data object to provide (usually the given one)
	 */
	default DO onAfterGet(DO dataObject) { return dataObject; }
}
