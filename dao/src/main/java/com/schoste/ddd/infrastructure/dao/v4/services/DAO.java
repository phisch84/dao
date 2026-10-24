package com.schoste.ddd.infrastructure.dao.v4.services;

import java.io.Serializable;
import java.util.Collection;
import java.util.stream.Stream;

import com.schoste.ddd.infrastructure.dao.v4.exceptions.DAOException;
import com.schoste.ddd.infrastructure.dao.v4.models.DataObject;
import com.schoste.ddd.infrastructure.dao.v4.services.listeners.DeleteListener;
import com.schoste.ddd.infrastructure.dao.v4.services.listeners.GetListener;
import com.schoste.ddd.infrastructure.dao.v4.services.listeners.SaveListener;

/**
 * Interface to the generic data access object (DAO) class.
 * It defines methods common to all DAOs.
 * 
 * @author Philipp Schosteritsch
 *
 * @param <ID> (Identifier) any data type / class that represents the unique identifier of the data object in the underlying storage
 * @param <DO> (DataObject) any data object that extends the DataObject class
 */
public interface DAO<ID extends Serializable, DO extends DataObject<ID>>
{
	/**
	 * Gets the milli-second unix time stamp of the most recent modification time/date
	 * which will be used by the DAO in {@link DAO#getRecent()} and {@link DAO#getRecent(boolean)}.
	 * {@link DAO#getModifiedSince(long, DeletedDataObjects)} uses its own time stamp and only compares it with this value
	 * to decide whether this value is updated.
	 *
	 * @return the latest modification time stamp of data objects from the underlying data storage
	 */
	public long getLastModifiedTimeStamp();

	/**
	 * Override the milli-second unix time stamp of the most recent modification time/date of data objects
	 * which will be used by the DAO in {@link DAO#getRecent()} and {@link DAO#getRecent(boolean)}.
	 * The time stamp is usually tracked by the DAO and updated by the data object loader when the stream returned by
	 * {@link DAO#getRecent()} or {@link DAO#getRecent(boolean)} was consumed to its end.
	 * 
	 * @param newTS the value to overwrite the internal most recent modification time/date of data objects
	 */
	public void setLastModifiedTimeStamp(long newTS);

	/**
	 * Creates a new data object.
	 * What the created data object actually looks like depends on the actual implementation.
	 * As a standard, the newly created data object is supposed to have a virtual identifier, so that
	 * {@link DAO#isVirtualId(Serializable)} for {@link DataObject#getId()}
	 * of a newly created data object is true.
	 * The time stamps of the newly created data object are set as well (see {@link DataObject#updateTimestamps()}).
	 *
	 * @return an instance to a new data object
	 * @throws DAOException re-throws every exception as DAOException
	 */
	DO createDataObject() throws DAOException;

	/**
	 * Determines if the provided identifier is a virtual identifier.
	 * Virtual identifiers mark data objects as not-persisted (yet) in the underlying storage
	 * 
	 * @param id the id to test
	 * @return true if the id is a virtual identifier, false otherwise (it may be an actual identifier but it isn't guaranteed)
	 */
	public boolean isVirtualId(ID id);

	/**
	 * Gets a data object from the data source by its id.
	 * This method also returns data objects flagged deleted (if returned by the storage)
	 * and is regardless of their modification time stamps.
	 * Registered {@link GetListener}s may change the requested id ({@link GetListener#onBeforeGet(Serializable)})
	 * and are notified after the data object was loaded ({@link GetListener#onAfterGet(DataObject)}).
	 * This method is usually called by repositories and services in case they need to refresh the data object of an entity.
	 * 
	 * @param id the identifier of the data object
	 * @return the data object with the given id or null if none was found
	 * @throws DAOException re-throws every exception as DAOException
	 */
	public DO get(ID id) throws DAOException;

	/**
	 * Gets all data objects from the data source by its ids. If an empty collection was provided, all available data objects are returned.
	 * This method also returns data objects flagged deleted (if returned by the storage)
	 * and is regardless of their modification time stamps.
	 * Registered {@link GetListener}s may change the requested ids ({@link GetListener#onBeforeGet(Serializable[])}).
	 * If one of them returns null, the result of {@link DAO#getAll()} is returned instead.
	 * This method is usually called by repositories and services in case they need to refresh the data object of an entity.
	 *
	 * @param ids the identifiers of the data objects as collection
	 * @return a stream of data objects with the given ids. If an id was not found, its data object is not included in the stream.
	 * @throws IllegalArgumentException thrown if parameter ids is null
	 * @throws DAOException re-throws every exception as DAOException
	 */
	public Stream<DO> get(Collection<ID> ids) throws IllegalArgumentException, DAOException;

	/**
	 * Loads all data objects according to the internal sourcing method (defined in the implementation of the data object loader,
	 * e.g. {@link SourceRecordDataObjectLoader}).
	 * This includes data objects flagged as deleted (if returned by the storage) and is regardless of their time stamps.
	 * Whether time stamps are updated in the DAO, or whether its listeners are notified also depends on the implementation of the interface
	 * 
	 * @return a stream of data objects
	 * @throws DAOException re-throws every exception as DAOException
	 */
	public Stream<DO> getAll() throws DAOException;

	/**
	 * Loads data objects according to the internal sourcing method (defined in the implementation of the data object loader,
	 * e.g. {@link GenericRecentDataObjectLoader}).
	 * Only data objects which have {@link DataObject#isDeleted()} == false, and
	 * {@link DataObject#getModifiedTimestamp()} greater than {@link DAO#getLastModifiedTimeStamp()}
	 * will be in the stream.
	 * After the stream was consumed to its end, {@link DAO#getLastModifiedTimeStamp()} is updated
	 * to the biggest modification time stamp of the data objects in the stream.
	 * With {@link GenericRecentDataObjectLoader}, source records which can't be converted to a data object
	 * (e.g. because it was not found) are skipped. If loading fails, consuming the stream throws a {@link com.schoste.ddd.infrastructure.dao.v4.exceptions.DAORuntimeException}
	 * (with a {@link com.schoste.ddd.infrastructure.dao.v4.exceptions.BrokenRecordException} as cause if only a single record is broken).
	 *
	 * @return a stream of data objects
	 * @throws DAOException re-throws every exception as DAOException
	 */
	public Stream<DO> getRecent() throws DAOException;

	/**
	 * Loads data objects according to the internal sourcing method (defined in the implementation of the data object loader,
	 * e.g. {@link GenericRecentDataObjectLoader}).
	 * Only data objects which have {@link DataObject#getModifiedTimestamp()}
	 * greater than {@link DAO#getLastModifiedTimeStamp()} will be in the stream.
	 * After the stream was consumed to its end, {@link DAO#getLastModifiedTimeStamp()} is updated
	 * to the biggest modification time stamp of the data objects in the stream.
	 * With {@link GenericRecentDataObjectLoader}, source records which can't be converted to a data object
	 * (e.g. because it was not found) are skipped. If loading fails, consuming the stream throws a {@link com.schoste.ddd.infrastructure.dao.v4.exceptions.DAORuntimeException}
	 * (with a {@link com.schoste.ddd.infrastructure.dao.v4.exceptions.BrokenRecordException} as cause if only a single record is broken).
	 *
	 * @param returnDeleted if true, data objects flagged as deleted ({@link DataObject#isDeleted()} == true) will also be returned
	 * @return a stream of data objects
	 * @throws DAOException re-throws every exception as DAOException
	 */
	public Stream<DO> getRecent(boolean returnDeleted) throws DAOException;

	/**
	 * Loads the data objects which were modified after a given time stamp
	 * ({@link DataObject#getModifiedTimestamp()} greater than the given time stamp).
	 * {@link DAO#getLastModifiedTimeStamp()} is only updated (after the stream was consumed to its end)
	 * if the given time stamp equals {@link DAO#getLastModifiedTimeStamp()}.
	 * With {@link GenericRecentDataObjectLoader}, source records which can't be converted to a data object
	 * (e.g. because it was not found) are skipped. If loading fails, consuming the stream throws a {@link com.schoste.ddd.infrastructure.dao.v4.exceptions.DAORuntimeException}
	 * (with a {@link com.schoste.ddd.infrastructure.dao.v4.exceptions.BrokenRecordException} as cause if only a single record is broken).
	 *
	 * @param timestamp only data objects modified after this time stamp are loaded
	 * @param deleted whether data objects flagged as deleted ({@link DataObject#isDeleted()} == true) are included
	 * @return a stream of data objects
	 * @throws IllegalArgumentException thrown if parameter deleted is null
	 * @throws DAOException re-throws every exception as DAOException
	 */
	public Stream<DO> getModifiedSince(long timestamp, DeletedDataObjects deleted) throws IllegalArgumentException, DAOException;

	/**
	 * Loads the data objects which were modified after a given time stamp, without the ones flagged as deleted.
	 * Same as {@link DAO#getModifiedSince(long, DeletedDataObjects)} with {@link DeletedDataObjects#EXCLUDE}.
	 *
	 * @param timestamp only data objects modified after this time stamp are loaded
	 * @return a stream of data objects
	 * @throws DAOException re-throws every exception as DAOException
	 */
	public Stream<DO> getModifiedSince(long timestamp) throws DAOException;

	/**
	 * Persists any changes to a data object (DO) to the underlying data source and updates
	 * the DO's time stamps (see {@link DataObject#updateTimestamps()}).
	 * If a {@link SaveListener} is registered, it may cancel the saving of the data object when its
	 * {@link SaveListener#onBeforeSave(DataObject)} returns false.
	 *
	 * @param dataObject the data object to save
	 * @throws IllegalArgumentException thrown if parameter dataObject is null
	 * @throws DAOException re-throws every exception as DAOException
	 */
	public void save(DO dataObject) throws IllegalArgumentException, DAOException;

	/**
	 * Persists any changes to the data objects (DOs) to the underlying data source and updates
	 * the DOs' time stamps (see {@link DataObject#updateTimestamps()}).
	 * If a {@link SaveListener} is registered, it may cancel the saving of all the data objects when its
	 * {@link SaveListener#onBeforeSave(Collection)} returns false.
	 *
	 * @param dataObjects the data objects to save
	 * @throws IllegalArgumentException thrown if parameter dataObjects is null
	 * @throws DAOException re-throws every exception as DAOException
	 */
	public void save(Collection<DO> dataObjects) throws IllegalArgumentException, DAOException;

	/**
	 * Deletes a data object (DO).
	 * After the call, the DO's deleted flag is supposed to be true ({@link DataObject#isDeleted()} == true).
	 * Setting the flag is up to the DAO's implementation, as well as if the DO is physically deleted from the underlying data storage.
	 * Nothing is deleted or flagged if a registered {@link DeleteListener} rejects the deletion.
	 * 
	 * @param dataObject the data object to delete
	 * @throws IllegalArgumentException thrown if parameter dataObject is null
	 * @throws DAOException re-throws every exception as DAOException
	 */
	public void delete(DO dataObject) throws IllegalArgumentException, DAOException;

	/**
	 * Deletes the data objects (DOs).
	 * After the call, the DOs' deleted flag is supposed to be true ({@link DataObject#isDeleted()} == true).
	 * Setting the flag is up to the DAO's implementation, as well as if the DOs are physically deleted from the underlying data storage.
	 * Nothing is deleted or flagged if a registered {@link DeleteListener} rejects the deletion.
	 * 
	 * @param dataObjects the data objects to delete as collection
	 * @throws IllegalArgumentException thrown if parameter dataObjects is null
	 * @throws DAOException re-throws every exception as DAOException
	 */
	public void delete(Collection<DO> dataObjects) throws IllegalArgumentException, DAOException;

	/**
	 * Deletes the data object (DO) with the given id.
	 * After the call, the DO's deleted flag MAY BE true ({@link DataObject#isDeleted()} == true) 
	 * if an update of the DO's deleted flag was implemented in the DAO's implementation.
	 * Otherwise it will be false.
	 * Registered {@link DeleteListener}s may change the ids to delete ({@link DeleteListener#onBeforeDelete(Serializable[])}),
	 * which is called with an array containing only the given id.
	 *
	 * @param dataObjectId the id of the data object to delete
	 * @throws IllegalArgumentException thrown if parameter dataObjectId is null
	 * @throws DAOException re-throws every exception as DAOException
	 */
	public void deleteById(ID dataObjectId) throws IllegalArgumentException, DAOException;

	/**
	 * Deletes all data objects (DOs) with the given ids.
	 * After the call, the DOs' deleted flag MAY BE true ({@link DataObject#isDeleted()} == true)
	 * if an update of the DOs' deleted flag was implemented in the DAO's implementation.
	 * Otherwise it will be false.
	 * Registered {@link DeleteListener}s may change the ids to delete ({@link DeleteListener#onBeforeDelete(Serializable[])}).
	 *
	 * @param dataObjectIds the ids of data objects to delete
	 * @throws IllegalArgumentException thrown if parameter dataObjectIds is null
	 * @throws DAOException re-throws every exception as DAOException
	 */
	public void deleteByIds(Collection<ID> dataObjectIds) throws IllegalArgumentException, DAOException;

	/**
	 * Clears the underlying storage if supported and resets the DAO's most recent modification time stamp
	 * ({@link DAO#getLastModifiedTimeStamp()}).
	 * Registered listeners are kept.
	 *
	 * @throws DAOException re-throws every exception as DAOException
	 */
	public void clear() throws DAOException;

	/**
	 * Registers a new listener which will be called before getting DOs and after DOs were loaded.
	 * The order in which multiple registered listeners are called is undefined.
	 * 
	 * @param listener the instance of the listener to register
	 * @throws IllegalArgumentException thrown if parameter listener is null
	 */
	public void registerOnGetListener(GetListener<ID, DO> listener) throws IllegalArgumentException;

	/**
	 * Unregisters a listener so it will not be called before getting DOs and after DOs were loaded.
	 * 
	 * @param listener the instance of the listener to unregister
	 * @throws IllegalArgumentException thrown if parameter listener is null
	 */
	public void unregisterOnGetListener(GetListener<ID, DO> listener) throws IllegalArgumentException;

	/**
	 * Gets a copy of the collection of all registered listeners which will be called before getting DOs and after DOs were loaded.
	 * 
	 * @return a collection of {@link GetListener}
	 */
	public Collection<GetListener<ID, DO>> getOnGetListeners();

	/**
	 * Registers a new listener which will be called before saving DOs and after DOs were saved.
	 * The order in which multiple registered listeners are called is undefined.
	 * 
	 * @param listener the instance of the listener to register
	 * @throws IllegalArgumentException thrown if parameter listener is null
	 */
	public void registerOnSaveListener(SaveListener<ID, DO> listener) throws IllegalArgumentException;

	/**
	 * Unregisters a listener so it will not be called before saving DOs and after DOs were saved.
	 * 
	 * @param listener the instance of the listener to unregister
	 * @throws IllegalArgumentException thrown if parameter listener is null
	 */
	public void unregisterOnSaveListener(SaveListener<ID, DO> listener) throws IllegalArgumentException;

	/**
	 * Gets a copy of the collection of all registered listeners which will be called before saving DOs and after DOs were saved.
	 * 
	 * @return a collection of {@link SaveListener}
	 */
	public Collection<SaveListener<ID, DO>> getOnSaveListeners();

	/**
	 * Registers a new listener which will be called before deleting DOs and after DOs were deleted.
	 * The order in which multiple registered listeners are called is undefined.
	 * 
	 * @param listener the instance of the listener to register
	 * @throws IllegalArgumentException thrown if parameter listener is null
	 */
	public void registerOnDeleteListener(DeleteListener<ID, DO> listener) throws IllegalArgumentException;

	/**
	 * Unregisters a listener so it will not be called before deleting DOs and after DOs were deleted.
	 * 
	 * @param listener the instance of the listener to unregister
	 * @throws IllegalArgumentException thrown if parameter listener is null
	 */
	public void unregisterOnDeleteListener(DeleteListener<ID, DO> listener) throws IllegalArgumentException;

	/**
	 * Gets a copy of the collection of all registered listeners which will be called before deleting DOs and after DOs were deleted.
	 * 
	 * @return a collection of {@link DeleteListener}
	 */
	public Collection<DeleteListener<ID, DO>> getOnDeleteListeners();
}