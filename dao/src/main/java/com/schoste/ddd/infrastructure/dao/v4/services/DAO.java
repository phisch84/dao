package com.schoste.ddd.infrastructure.dao.v4.services;

import java.io.Serializable;
import java.util.Collection;
import java.util.stream.Stream;

import com.schoste.ddd.infrastructure.dao.v4.exceptions.BrokenRecordException;
import com.schoste.ddd.infrastructure.dao.v4.exceptions.DAOException;
import com.schoste.ddd.infrastructure.dao.v4.exceptions.DAORuntimeException;
import com.schoste.ddd.infrastructure.dao.v4.models.DataObject;
import com.schoste.ddd.infrastructure.dao.v4.services.listeners.DeleteListener;
import com.schoste.ddd.infrastructure.dao.v4.services.listeners.GetListener;
import com.schoste.ddd.infrastructure.dao.v4.services.listeners.SaveListener;

/**
 * DAO (Data Access Object): loads, saves, deletes and tracks changes of the data objects (DOs) of one storage,
 * e.g. a database, a folder of files or memory.
 *
 * Rules which hold for every DAO:
 * <ul>
 * <li><b>Ids:</b> a new data object ({@link #createDataObject()}) has a <i>virtual</i> id; saving it gives it its <i>actual</i> id.
 * {@link #isVirtualId(Serializable)} tells them apart without accessing the storage.</li>
 * <li><b>Deleting is logical:</b> {@link #delete(DataObject)} flags a data object as deleted ({@link DataObject#isDeleted()}) and stores
 * the flag. Physical removal is up to the storage's operators. Some implementations delete physically and say so in their class
 * documentation. Therefore <b>a data object counts as deleted if it is flagged or missing</b>.</li>
 * <li><b>Change tracking is stateless:</b> {@link #getModifiedSince(long)} provides the data objects modified at or after a time stamp.
 * The caller keeps its own time stamp (the highest modification time stamp it has processed) and passes it next time, so several
 * callers can use one DAO independently. Since several changes can get the same time stamp, the data objects with exactly the passed
 * time stamp are provided again; the caller ignores the ones it has already processed. The storage's clock sets the time stamps.
 * A change which a database transaction commits later than other changes may carry an older time stamp than them; callers which
 * must see such changes pass their time stamp minus a safety margin (e.g. the longest duration of a transaction).</li>
 * <li><b>Streams must be closed:</b> methods which provide several data objects return a lazy, sequential {@link Stream}, so that large
 * amounts of data never have to fit into memory. The DAO keeps resources (e.g. files or database sessions) open until the stream is
 * closed, e.g. by a try-with-resources statement. Failures while a stream is consumed are thrown as {@link DAORuntimeException};
 * a broken record of the storage is reported as {@link DAORuntimeException} with a {@link BrokenRecordException} as cause,
 * and consuming the stream by its {@link Stream#iterator()} can continue with the next record.</li>
 * <li><b>Threads:</b> a DAO instance is thread-safe. A returned stream is not; it belongs to the caller which requested it.
 * While it is consumed, it may or may not see concurrent changes, but it doesn't fail because of them.</li>
 * <li><b>Listeners</b> ({@link GetListener}, {@link SaveListener}, {@link DeleteListener}) are called in the order of their
 * registration and may change or cancel operations.</li>
 * <li><b>Arguments:</b> null is never accepted as data object, id, collection or element of a collection
 * ({@link IllegalArgumentException}). Every other failure is thrown as {@link DAOException}.</li>
 * </ul>
 *
 * Implementations usually extend {@link AbstractDAO}.
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 *
 * @param <ID> the class of the data objects' ids
 * @param <DO> the class of the data objects (DO = data object)
 */
public interface DAO<ID extends Serializable, DO extends DataObject<ID>>
{
	/**
	 * Creates a new data object with a virtual id ({@link #isVirtualId(Serializable)} is true for it).
	 * The data object isn't stored until it is saved ({@link #save(DataObject)}).
	 *
	 * @return a new data object
	 * @throws DAOException re-throws every exception as DAOException
	 */
	public DO createDataObject() throws DAOException;

	/**
	 * Determines if an id is virtual, i.e. it belongs to a data object which hasn't been saved yet.
	 * The decision is made from the id alone, without accessing the storage, and is always the same for the same id.
	 * False means "not virtual", not "exists in the storage".
	 *
	 * @param id the id to check
	 * @return true if the id is virtual, false if it is an actual id
	 * @throws IllegalArgumentException thrown if parameter id is null
	 */
	public boolean isVirtualId(ID id) throws IllegalArgumentException;

	/**
	 * Gets the data object with the given id, also if it is flagged as deleted.
	 * Registered {@link GetListener}s may change the requested id ({@link GetListener#onBeforeGet(Serializable)}) and the
	 * loaded data object ({@link GetListener#onAfterGet(DataObject)}).
	 *
	 * @param id the id of the data object
	 * @return the data object, or null if the storage has no data object with the id
	 * @throws IllegalArgumentException thrown if parameter id is null
	 * @throws IllegalStateException thrown if a {@link GetListener} returned null as id
	 * @throws DAOException re-throws every other exception as DAOException
	 */
	public DO get(ID id) throws IllegalArgumentException, IllegalStateException, DAOException;

	/**
	 * Gets the data objects with the given ids, also the ones flagged as deleted, in no particular order.
	 * Ids without data object are ignored; no ids provide no data objects.
	 * Registered {@link GetListener}s may change the requested ids ({@link GetListener#onBeforeGet(Serializable[])}) and the
	 * loaded data objects ({@link GetListener#onAfterGet(DataObject)}).
	 *
	 * @param ids the ids of the data objects
	 * @return a stream of the data objects (see the rules for streams in the documentation of {@link DAO})
	 * @throws IllegalArgumentException thrown if parameter ids is null or contains null
	 * @throws IllegalStateException thrown if a {@link GetListener} returned null as ids
	 * @throws DAOException re-throws every other exception as DAOException
	 */
	public Stream<DO> get(Collection<ID> ids) throws IllegalArgumentException, IllegalStateException, DAOException;

	/**
	 * Gets all data objects of the storage except the ones flagged as deleted.
	 * Same as {@link #getAll(DeletedDataObjects)} with {@link DeletedDataObjects#EXCLUDE}.
	 *
	 * @return a stream of the data objects (see the rules for streams in the documentation of {@link DAO})
	 * @throws DAOException re-throws every exception as DAOException
	 */
	public Stream<DO> getAll() throws DAOException;

	/**
	 * Gets all data objects of the storage, in no particular order.
	 *
	 * @param deleted whether data objects flagged as deleted are included
	 * @return a stream of the data objects (see the rules for streams in the documentation of {@link DAO})
	 * @throws IllegalArgumentException thrown if parameter deleted is null
	 * @throws DAOException re-throws every exception as DAOException
	 */
	public Stream<DO> getAll(DeletedDataObjects deleted) throws IllegalArgumentException, DAOException;

	/**
	 * Gets the data objects which were modified at or after the given time stamp, except the ones flagged as deleted.
	 * Same as {@link #getModifiedSince(long, DeletedDataObjects)} with {@link DeletedDataObjects#EXCLUDE}.
	 *
	 * @param timestamp only data objects whose modification time stamp ({@link DataObject#getModifiedTimestamp()}) is greater than or
	 * equal to it are provided
	 * @return a stream of the data objects (see the rules for streams in the documentation of {@link DAO})
	 * @throws DAOException re-throws every exception as DAOException
	 */
	public Stream<DO> getModifiedSince(long timestamp) throws DAOException;

	/**
	 * Gets the data objects which were modified at or after the given time stamp, in no particular order. This is the change
	 * tracking of the DAO: a caller passes the highest modification time stamp it has processed so far, and gets the data objects
	 * modified since then, including the ones with exactly that time stamp (several changes can get the same time stamp, so a change
	 * stored right after the processed ones isn't missed). The caller ignores data objects it has already processed.
	 * The DAO doesn't remember anything between calls.
	 * A change committed late by a database transaction may carry an older time stamp than changes the caller has already processed;
	 * to see such changes, pass the time stamp minus a safety margin.
	 * To learn about deletions, include the data objects flagged as deleted; a deletion is only reported while the flagged
	 * data object still exists in the storage.
	 *
	 * @param timestamp only data objects whose modification time stamp ({@link DataObject#getModifiedTimestamp()}) is greater than or
	 * equal to it are provided
	 * @param deleted whether data objects flagged as deleted are included
	 * @return a stream of the data objects (see the rules for streams in the documentation of {@link DAO})
	 * @throws IllegalArgumentException thrown if parameter deleted is null
	 * @throws DAOException re-throws every exception as DAOException
	 */
	public Stream<DO> getModifiedSince(long timestamp, DeletedDataObjects deleted) throws IllegalArgumentException, DAOException;

	/**
	 * Stores a data object. A new data object gets its actual id. Afterwards the data object carries the time stamps
	 * which the storage has stored ({@link DataObject#getCreatedTimestamp()}, {@link DataObject#getModifiedTimestamp()}).
	 * The deleted flag is stored as it is, so saving a deleted data object with the flag set to false undeletes it.
	 * A {@link SaveListener} may cancel the saving ({@link SaveListener#onBeforeSave(DataObject)}); then this method returns normally,
	 * nothing is stored, and the data object is left as it was (e.g. a new data object keeps its virtual id).
	 *
	 * @param dataObject the data object to save
	 * @throws IllegalArgumentException thrown if parameter dataObject is null
	 * @throws DAOException re-throws every exception as DAOException, also from a listener after the data object was stored
	 */
	public void save(DO dataObject) throws IllegalArgumentException, DAOException;

	/**
	 * Stores data objects like {@link #save(DataObject)} does.
	 * A {@link SaveListener} may cancel the saving of all of them ({@link SaveListener#onBeforeSave(Collection)}); then this method
	 * returns normally and nothing is stored.
	 *
	 * @param dataObjects the data objects to save
	 * @throws IllegalArgumentException thrown if parameter dataObjects is null or contains null; then nothing is saved
	 * @throws DAOException re-throws every exception as DAOException, also from a listener after the data objects were stored
	 */
	public void save(Collection<DO> dataObjects) throws IllegalArgumentException, DAOException;

	/**
	 * Deletes a data object logically: it is flagged as deleted ({@link DataObject#isDeleted()}) and the flag is stored, which is
	 * a modification reported by change tracking; afterwards the data object carries the stored modification time stamp, like after
	 * {@link #save(DataObject)}. Implementations which delete physically say so in their class documentation.
	 * <ul>
	 * <li>A data object which is already flagged as deleted is left as it is.</li>
	 * <li>A data object which was never saved (virtual id) is only flagged; nothing is stored and no listener is called.</li>
	 * </ul>
	 * A {@link DeleteListener} may cancel the deletion ({@link DeleteListener#onBeforeDelete(DataObject)}); then this method returns
	 * normally, and neither the data object nor the storage is changed.
	 *
	 * @param dataObject the data object to delete
	 * @throws IllegalArgumentException thrown if parameter dataObject is null
	 * @throws DAOException re-throws every exception as DAOException, also from a listener after the data object was deleted
	 */
	public void delete(DO dataObject) throws IllegalArgumentException, DAOException;

	/**
	 * Deletes data objects like {@link #delete(DataObject)} does.
	 * A {@link DeleteListener} may cancel the deletion of all of them ({@link DeleteListener#onBeforeDelete(Collection)}); then this
	 * method returns normally and nothing is deleted.
	 *
	 * @param dataObjects the data objects to delete
	 * @throws IllegalArgumentException thrown if parameter dataObjects is null or contains null; then nothing is deleted
	 * @throws DAOException re-throws every exception as DAOException, also from a listener after the data objects were deleted
	 */
	public void delete(Collection<DO> dataObjects) throws IllegalArgumentException, DAOException;

	/**
	 * Deletes the data object with the given id like {@link #delete(DataObject)} does. An id without data object is ignored.
	 * The {@link DeleteListener}s are called with the data object, as if it had been passed to {@link #delete(Collection)}.
	 *
	 * @param dataObjectId the id of the data object to delete
	 * @throws IllegalArgumentException thrown if parameter dataObjectId is null
	 * @throws DAOException re-throws every exception as DAOException
	 */
	public void deleteById(ID dataObjectId) throws IllegalArgumentException, DAOException;

	/**
	 * Deletes the data objects with the given ids like {@link #delete(Collection)} does. Ids without data object are ignored.
	 *
	 * @param dataObjectIds the ids of the data objects to delete
	 * @throws IllegalArgumentException thrown if parameter dataObjectIds is null or contains null; then nothing is deleted
	 * @throws DAOException re-throws every exception as DAOException
	 */
	public void deleteByIds(Collection<ID> dataObjectIds) throws IllegalArgumentException, DAOException;

	/**
	 * Removes all data objects physically from the storage, including the ones flagged as deleted.
	 * Meant for tests and administration; the removal isn't reported by change tracking. Registered listeners are kept.
	 *
	 * @throws UnsupportedOperationException thrown if the storage can't be cleared by the DAO
	 * @throws DAOException re-throws every other exception as DAOException
	 */
	public void clear() throws UnsupportedOperationException, DAOException;

	/**
	 * Registers a listener which is called before data objects are got and after they were loaded.
	 * Listeners are called in the order of their registration.
	 *
	 * @param listener the listener to register
	 * @throws IllegalArgumentException thrown if parameter listener is null
	 */
	public void registerOnGetListener(GetListener<ID, DO> listener) throws IllegalArgumentException;

	/**
	 * Unregisters a listener registered by {@link #registerOnGetListener(GetListener)}.
	 *
	 * @param listener the listener to unregister
	 * @throws IllegalArgumentException thrown if parameter listener is null
	 */
	public void unregisterOnGetListener(GetListener<ID, DO> listener) throws IllegalArgumentException;

	/**
	 * Gets a copy of the registered get listeners, in the order of their registration.
	 *
	 * @return the registered {@link GetListener}s
	 */
	public Collection<GetListener<ID, DO>> getOnGetListeners();

	/**
	 * Registers a listener which is called before data objects are saved and after they were saved.
	 * Listeners are called in the order of their registration.
	 *
	 * @param listener the listener to register
	 * @throws IllegalArgumentException thrown if parameter listener is null
	 */
	public void registerOnSaveListener(SaveListener<ID, DO> listener) throws IllegalArgumentException;

	/**
	 * Unregisters a listener registered by {@link #registerOnSaveListener(SaveListener)}.
	 *
	 * @param listener the listener to unregister
	 * @throws IllegalArgumentException thrown if parameter listener is null
	 */
	public void unregisterOnSaveListener(SaveListener<ID, DO> listener) throws IllegalArgumentException;

	/**
	 * Gets a copy of the registered save listeners, in the order of their registration.
	 *
	 * @return the registered {@link SaveListener}s
	 */
	public Collection<SaveListener<ID, DO>> getOnSaveListeners();

	/**
	 * Registers a listener which is called before data objects are deleted and after they were deleted.
	 * Listeners are called in the order of their registration.
	 *
	 * @param listener the listener to register
	 * @throws IllegalArgumentException thrown if parameter listener is null
	 */
	public void registerOnDeleteListener(DeleteListener<ID, DO> listener) throws IllegalArgumentException;

	/**
	 * Unregisters a listener registered by {@link #registerOnDeleteListener(DeleteListener)}.
	 *
	 * @param listener the listener to unregister
	 * @throws IllegalArgumentException thrown if parameter listener is null
	 */
	public void unregisterOnDeleteListener(DeleteListener<ID, DO> listener) throws IllegalArgumentException;

	/**
	 * Gets a copy of the registered delete listeners, in the order of their registration.
	 *
	 * @return the registered {@link DeleteListener}s
	 */
	public Collection<DeleteListener<ID, DO>> getOnDeleteListeners();
}
