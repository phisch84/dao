package com.schoste.ddd.infrastructure.dal.v3.services;

import java.io.Serializable;
import java.util.Collection;
import java.util.stream.Stream;

import com.schoste.ddd.infrastructure.dal.v3.exceptions.DALException;
import com.schoste.ddd.infrastructure.dal.v3.models.GenericDataObject;
import com.schoste.ddd.infrastructure.dal.v3.services.listeners.DeleteListener;
import com.schoste.ddd.infrastructure.dal.v3.services.listeners.GetListener;
import com.schoste.ddd.infrastructure.dal.v3.services.listeners.SaveListener;

/**
 * Interface to the generic data access object (DAO) class.
 * It defines methods common to all DAOs.
 * 
 * @author Philipp Schosteritsch
 *
 * @param <ID> (Identifier) any data type / class that represents the unique identifier of the data object in the underlying storage
 * @param <DO> (DataObject) any data object that extends the GenericDataObject class
 */
public interface GenericDataAccessObject<ID extends Serializable, DO extends GenericDataObject<ID>>
{
	/**
	 * Gets the milli-second unix time stamp of the most recent modification time/date
	 * which will be used by the DAO in {@link GenericDataAccessObject#getRecent()} and {@link GenericDataAccessObject#getRecent(boolean)}.
	 * {@link GenericDataAccessObject#getRecent(long, boolean)} uses its own time stamp and only compares it with this value
	 * to decide whether this value is updated.
	 *
	 * @return the latest modification time stamp of data objects from the underlying data storage
	 */
	public long getLastModifiedTimeStamp();

	/**
	 * Override the milli-second unix time stamp of the most recent modification time/date of data objects
	 * which will be used by the DAO in {@link GenericDataAccessObject#getRecent()} and {@link GenericDataAccessObject#getRecent(boolean)}.
	 * The time stamp is usually tracked by the DAO and updated by the data object loader when the loading of the stream returned by
	 * {@link GenericDataAccessObject#getRecent()} or {@link GenericDataAccessObject#getRecent(boolean)} ends
	 * (see {@link GenericDataAccessObject#getRecent()}).
	 * 
	 * @param newTS the value to overwrite the internal most recent modification time/date of data objects
	 */
	public void setLastModifiedTimeStamp(long newTS);

	/**
	 * Creates a new data object.
	 * What the created data object actually looks like depends on the actual implementation.
	 * As a standard, the newly created data object is supposed to have a virtual identifier, so that
	 * {@link GenericDataAccessObject#isVirtualId(Serializable)} for {@link GenericDataObject#getId()}
	 * of a newly created data object is true.
	 * The time stamps of the newly created data object are set as well (see {@link GenericDataObject#updateTimeStamps()}).
	 * 
	 * @param args optional arguments (can be null). The usage of these arguments depends on the actual implementation
	 * @return an instance to a new data object
	 * @throws DALException re-throws every exception as DAL exception
	 */
	DO createDataObject(Object... args) throws DALException;

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
	 * and are notified after the data object was loaded ({@link GetListener#onAfterGet(GenericDataObject)}).
	 * This method is usually called by repositories and services in case they need to refresh the data object of an entity.
	 * 
	 * @param id the identifier of the data object
	 * @return the data object with the given id or null if none was found
	 * @throws DALException re-throws every exception as DAL exception
	 */
	public DO get(ID id) throws DALException;

	/**
	 * Gets all data objects from the data source by its ids. If an empty id-array was provided, all available data objects are returned.
	 * This method also returns data objects flagged deleted (if returned by the storage)
	 * and is regardless of their modification time stamps.
	 * Registered {@link GetListener}s may change the requested ids ({@link GetListener#onBeforeGet(Serializable[])}).
	 * If one of them returns null, the result of {@link GenericDataAccessObject#getAll()} is returned instead.
	 * This method is usually called by repositories and services in case they need to refresh the data object of an entity.
	 *
	 * @param ids the identifiers of the data objects as array
	 * @return a stream of data objects with the given ids. If an id was not found, its data object is not included in the stream.
	 * @throws IllegalArgumentException thrown if parameter ids is null
	 * @throws DALException re-throws every exception as DAL exception
	 */
	public Stream<DO> get(ID[] ids) throws IllegalArgumentException, DALException;

	/**
	 * Gets all data objects from the data source by its ids. If an empty collection was provided, all available data objects are returned.
	 * This method also returns data objects flagged deleted (if returned by the storage)
	 * and is regardless of their modification time stamps.
	 * Otherwise it behaves like {@link GenericDataAccessObject#get(Serializable[])}.
	 * This method is usually called by repositories and services in case they need to refresh the data object of an entity.
	 *
	 * @param ids the identifiers of the data objects as collection
	 * @return a stream of data objects with the given ids. If an id was not found, its data object is not included in the stream.
	 * @throws IllegalArgumentException thrown if parameter ids is null
	 * @throws DALException re-throws every exception as DAL exception
	 */
	public Stream<DO> get(Collection<ID> ids) throws IllegalArgumentException, DALException;

	/**
	 * Loads all data objects according to the internal sourcing method (defined in the implementation of the data object loader,
	 * e.g. {@link GenericDefaultDataObjectLoader}).
	 * This includes data objects flagged as deleted (if returned by the storage) and is regardless of their time stamps.
	 * Whether time stamps are updated in the DAO, or whether its listeners are notified also depends on the implementation of the interface
	 * 
	 * @return a stream of data objects
	 * @throws DALException re-throws every exception as DAL exception
	 */
	public Stream<DO> getAll() throws DALException;

	/**
	 * Loads data objects according to the internal sourcing method (defined in the implementation of the data object loader,
	 * e.g. {@link GenericRecentDataObjectLoader}).
	 * Only data objects which have {@link GenericDataObject#getIsDeleted()} == false, and
	 * {@link GenericDataObject#getModifiedTimeStamp()} greater than {@link GenericDataAccessObject#getLastModifiedTimeStamp()}
	 * will be in the stream.
	 * When the loading ends, {@link GenericDataAccessObject#getLastModifiedTimeStamp()} is updated
	 * to the biggest modification time stamp of the data objects in the stream.
	 * With {@link GenericRecentDataObjectLoader} the loading ends when the stream was consumed to its end, but also as soon as
	 * a source record can't be converted to a data object (e.g. because it was not found or failed to load).
	 * In the latter case the stream ends early and the time stamp is updated nonetheless.
	 *
	 * @return a stream of data objects
	 * @throws DALException re-throws every exception as DAL exception
	 */
	public Stream<DO> getRecent() throws DALException;

	/**
	 * Loads data objects according to the internal sourcing method (defined in the implementation of the data object loader,
	 * e.g. {@link GenericRecentDataObjectLoader}).
	 * Only data objects which have {@link GenericDataObject#getModifiedTimeStamp()}
	 * greater than {@link GenericDataAccessObject#getLastModifiedTimeStamp()} will be in the stream.
	 * When the loading ends, {@link GenericDataAccessObject#getLastModifiedTimeStamp()} is updated
	 * to the biggest modification time stamp of the data objects in the stream.
	 * With {@link GenericRecentDataObjectLoader} the loading ends when the stream was consumed to its end, but also as soon as
	 * a source record can't be converted to a data object (e.g. because it was not found or failed to load).
	 * In the latter case the stream ends early and the time stamp is updated nonetheless.
	 *
	 * @param returnDeleted if true, data objects flagged as deleted ({@link GenericDataObject#getIsDeleted()} == true) will also be returned
	 * @return a stream of data objects
	 * @throws DALException re-throws every exception as DAL exception
	 */
	public Stream<DO> getRecent(boolean returnDeleted) throws DALException;

	/**
	 * Loads data objects according to the internal sourcing method (defined in the implementation of the data object loader,
	 * e.g. {@link GenericRecentDataObjectLoader}).
	 * Only data objects which have {@link GenericDataObject#getModifiedTimeStamp()} greater than lastModTS will be in the stream.
	 * {@link GenericDataAccessObject#getLastModifiedTimeStamp()} is only updated (when the loading ends)
	 * if lastModTS equals {@link GenericDataAccessObject#getLastModifiedTimeStamp()}.
	 * With {@link GenericRecentDataObjectLoader} the loading ends when the stream was consumed to its end, but also as soon as
	 * a source record can't be converted to a data object (e.g. because it was not found or failed to load).
	 * In the latter case the stream ends early and the time stamp is updated nonetheless.
	 *
	 * @param lastModTS the value to overwrite the internal most recent modification time/date of data objects for this call
	 * @param returnDeleted if true, data objects flagged as deleted ({@link GenericDataObject#getIsDeleted()} == true) will also be returned
	 * @return a stream of data objects
	 * @throws DALException re-throws every exception as DAL exception
	 */
	public Stream<DO> getRecent(long lastModTS, boolean returnDeleted) throws DALException;

	/**
	 * Persists any changes to a data object (DO) to the underlying data source and updates
	 * the DO's time stamps (see {@link GenericDataObject#updateTimeStamps()}).
	 * If a {@link SaveListener} is registered, it may cancel the saving of the data object when its
	 * {@link SaveListener#onBeforeSaving(GenericDataObject)} returns false.
	 *
	 * @param dataObject the data object to save
	 * @throws IllegalArgumentException thrown if parameter dataObject is null
	 * @throws DALException re-throws every exception as DAL exception
	 */
	public void save(DO dataObject) throws IllegalArgumentException, DALException;
	
	/**
	 * Persists any changes to a data object (DO) to the underlying data source and updates
	 * the DO's time stamps (see {@link GenericDataObject#updateTimeStamps()}).
	 * If a {@link SaveListener} is registered, it may cancel the saving of the data object when its
	 * {@link SaveListener#onBeforeSaving(GenericDataObject)} returns false.
	 * 
	 * @param dataObject the data object to save
	 * @throws IllegalArgumentException thrown if parameter dataObject is null or an instance of an incompatible class
	 * @throws DALException re-throws every exception as DAL exception
	 */
	public void save(Object dataObject) throws IllegalArgumentException, DALException;

	/**
	 * Persists any changes to the data objects (DOs) to the underlying data source and updates
	 * the DOs' time stamps (see {@link GenericDataObject#updateTimeStamps()}).
	 * If a {@link SaveListener} is registered, it may cancel the saving of all the data objects when its
	 * {@link SaveListener#onBeforeSaving(Collection)} returns false.
	 *
	 * @param dataObjects the data objects to save
	 * @throws IllegalArgumentException thrown if parameter dataObjects is null
	 * @throws DALException re-throws every exception as DAL exception
	 */
	public void save(Collection<DO> dataObjects) throws IllegalArgumentException, DALException;

	/**
	 * Persists any changes to the data objects (DOs) to the underlying data source and updates
	 * the DOs' time stamps (see {@link GenericDataObject#updateTimeStamps()}).
	 * If a {@link SaveListener} is registered, it may cancel the saving of all the data objects when its
	 * {@link SaveListener#onBeforeSaving(Collection)} returns false.
	 * 
	 * @param dataObjects the data objects to save
	 * @throws IllegalArgumentException thrown if parameter dataObjects is null or one element is an instance of an incompatible class
	 * @throws DALException re-throws every exception as DAL exception
	 */
	public void save(GenericDataObject<ID>[] dataObjects) throws IllegalArgumentException, DALException;

	/**
	 * Deletes a data object (DO).
	 * After the call, the DO's deleted flag is supposed to be true ({@link GenericDataObject#getIsDeleted()} == true).
	 * Setting the flag is up to the DAO's implementation, as well as if the DO is physically deleted from the underlying data storage.
	 * Nothing is deleted or flagged if a registered {@link DeleteListener} rejects the deletion.
	 * 
	 * @param dataObject the data object to delete
	 * @throws IllegalArgumentException thrown if parameter dataObject is null
	 * @throws DALException re-throws every exception as DAL exception
	 */
	public void delete(DO dataObject) throws IllegalArgumentException, DALException;
	
	/**
	 * Deletes a data object (DO).
	 * After the call, the DO's deleted flag is supposed to be true ({@link GenericDataObject#getIsDeleted()} == true).
	 * Setting the flag is up to the DAO's implementation, as well as if the DO is physically deleted from the underlying data storage.
	 * Nothing is deleted or flagged if a registered {@link DeleteListener} rejects the deletion.
	 * 
	 * @param dataObject the data object to delete
	 * @throws IllegalArgumentException thrown if parameter dataObject is null or an instance of an incompatible class
	 * @throws DALException re-throws every exception as DAL exception
	 */
	public void delete(Object dataObject) throws IllegalArgumentException, DALException;

	/**
	 * Deletes the data objects (DOs).
	 * After the call, the DOs' deleted flag is supposed to be true ({@link GenericDataObject#getIsDeleted()} == true).
	 * Setting the flag is up to the DAO's implementation, as well as if the DOs are physically deleted from the underlying data storage.
	 * Nothing is deleted or flagged if a registered {@link DeleteListener} rejects the deletion.
	 * 
	 * @param dataObjects the data objects to delete as array
	 * @throws IllegalArgumentException thrown if parameter dataObjects is null or one element is null or an instance of an incompatible class
	 * @throws DALException re-throws every exception as DAL exception
	 */
	public void delete(DO[] dataObjects) throws IllegalArgumentException, DALException;

	/**
	 * Deletes the data objects (DOs).
	 * After the call, the DOs' deleted flag is supposed to be true ({@link GenericDataObject#getIsDeleted()} == true).
	 * Setting the flag is up to the DAO's implementation, as well as if the DOs are physically deleted from the underlying data storage.
	 * Nothing is deleted or flagged if a registered {@link DeleteListener} rejects the deletion.
	 * 
	 * @param dataObjects the data objects to delete as collection
	 * @throws IllegalArgumentException thrown if parameter dataObjects is null
	 * @throws DALException re-throws every exception as DAL exception
	 */
	public void delete(Collection<DO> dataObjects) throws IllegalArgumentException, DALException;

	/**
	 * Deletes the data object (DO) with the given id.
	 * After the call, the DO's deleted flag MAY BE true ({@link GenericDataObject#getIsDeleted()} == true) 
	 * if an update of the DO's deleted flag was implemented in the DAO's implementation.
	 * Otherwise it will be false.
	 * Registered {@link DeleteListener}s may change the ids to delete ({@link DeleteListener#onBeforeDeleting(Serializable[])}),
	 * which is called with an array containing only the given id.
	 *
	 * @param dataObjectId the id of the data object to delete
	 * @throws IllegalArgumentException thrown if parameter dataObjectId is null
	 * @throws DALException re-throws every exception as DAL exception
	 */
	public void delete(ID dataObjectId) throws IllegalArgumentException, DALException;

	/**
	 * Deletes all data objects (DOs) with the given ids.
	 * After the call, the DOs' deleted flag MAY BE true ({@link GenericDataObject#getIsDeleted()} == true) 
	 * if an update of the DOs' deleted flag was implemented in the DAO's implementation.
	 * Otherwise it will be false.
	 * Registered {@link DeleteListener}s may change the ids to delete ({@link DeleteListener#onBeforeDeleting(Serializable[])}).
	 *
	 * @param dataObjectIds the ids of data objects to delete
	 * @throws IllegalArgumentException thrown if parameter dataObjectIds is null
	 * @throws DALException re-throws every exception as DAL exception
	 */
	public void delete(ID[] dataObjectIds) throws IllegalArgumentException, DALException;

	/**
	 * Clears the underlying storage if supported and resets the DAO's most recent modification time stamp
	 * ({@link GenericDataAccessObject#getLastModifiedTimeStamp()}).
	 * Registered listeners are kept.
	 *
	 * @throws DALException re-throws every exception as DAL exception
	 */
	public void clear() throws DALException;

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