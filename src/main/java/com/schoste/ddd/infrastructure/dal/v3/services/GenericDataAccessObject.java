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
	 * which will be used by the DAO in {@link GenericDataAccessObject#getRecent(long, boolean)}.
	 * 
	 * @return the latest modification time stamp of data objects from the underlying data storage
	 */
	public long getLastModifiedTimeStamp();

	/**
	 * Override the milli-second unix time stamp of the most recent modification time/date of data objects
	 * which will be used by the DAO in {@link GenericDataAccessObject#getRecent(long, boolean)}.
	 * The time stamp is usually tracked by the DAO and updated when {@link GenericDataAccessObject#getRecent(long, boolean)} is called.
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
	 * 
	 * @param args optional arguments (can be null). The usage of these arguments depends on the actual implementation
	 * @return an instance to a new data object
	 * @throws DALException re-throws every exception as DAL exception
	 */
	DO createDataObject(Object... args) throws DALException;

	/**
	 * Determines if the provided identifier is a virtual indentifier.
	 * Virtual identifiers mark data objects as not-persisted (yet) in the underlying storage
	 * 
	 * @param id the id to test
	 * @return true if the id is a virtual identifier, false otherwise (it may be an actual identifier but it isn't guaranteed)
	 */
	public boolean isVirtualId(ID id);

	/**
	 * Gets a data object from the data source by its id.
	 * This method also returns data objects flagged deleted (if returned by the storage)
	 * or object that were created or modified after the last recorded modification.
	 * This method is usually called by repositories and services in case they need to refresh the data object of an entity.
	 * 
	 * @param id the identifier of the data object
	 * @return the data object with the given id or null if none was found
	 * @throws DALException re-throws every exception as DAL exception
	 */
	public DO get(ID id) throws DALException;

	/**
	 * Gets all data objects from the data source by its ids.
	 * This method also returns data objects flagged deleted (if returned by the storage)
	 * or object that were created or modified after the last recorded modification.
	 * This method is usually called by repositories and services in case they need to refresh the data object of an entity.
	 * 
	 * @param ids the identifiers of the data objects as array
	 * @return a stream data objects with the given ids. If the id was not found, the data objects is not included in the list.
	 * @throws IllegalArgumentException thrown if parameter ids is null
	 * @throws DALException re-throws every exception as DAL exception
	 */
	public Stream<DO> get(ID[] ids) throws IllegalArgumentException, DALException;

	/**
	 * Gets all data objects from the data source by its ids.
	 * This method also returns data objects flagged deleted (if returned by the storage)
	 * or object that were created or modified after the last recorded modification.
	 * This method is usually called by repositories and services in case they need to refresh the data object of an entity.
	 * 
	 * @param ids the identifiers of the data objects as collection
	 * @return a stream data objects with the given ids. If the id was not found, the data objects is not included in the list.
	 * @throws IllegalArgumentException thrown if parameter ids is null
	 * @throws DALException re-throws every exception as DAL exception
	 */
	public Stream<DO> get(Collection<ID> ids) throws IllegalArgumentException, DALException;

	/**
	 * Loads data objects according to the internal sourcing method (defined in the implementation of {@link GenericDefaultDataObjectLoader}).
	 * Whether time staps are updated in the DAO, or whether its listeners are notified also depends on the implementation of the interface
	 * 
	 * @return a stream of data objects
	 * @throws DALException re-throws every exception as DAL exception
	 */
	public Stream<DO> getAll() throws DALException;

	/**
	 * Loads data objects according to the internal sourcing method (defined in the implementation of {@link GenericDefaultDataObjectLoader}).
	 * Only data objects which have {@link GenericDataObject#getIsDeleted()} == false, and
	 * {@link GenericDataObject#getCreatedTimeStamp()} or {@link GenericDataObject#getModifiedTimeStamp()} smaller than {@link GenericDataAccessObject#getLastModifiedTimeStamp()}
	 * will be in the stream.
	 * 
	 * @return a stream of data objects
	 * @throws DALException re-throws every exception as DAL exception
	 */
	public Stream<DO> getRecent() throws DALException;

	/**
	 * Loads data objects according to the internal sourcing method (defined in the implementation of {@link GenericDefaultDataObjectLoader}).
	 * Only data objects which have {@link GenericDataObject#getCreatedTimeStamp()} or {@link GenericDataObject#getModifiedTimeStamp()}
	 * smaller than {@link GenericDataAccessObject#getLastModifiedTimeStamp()} will be in the stream.
	 * 
	 * @param returnDeleted if true, data objects flagged as deleted ({@link GenericDataObject#getIsDeleted()} == true) will also be returned
	 * @return a stream of data objects
	 * @throws DALException re-throws every exception as DAL exception
	 */
	public Stream<DO> getRecent(boolean returnDeleted) throws DALException;

	/**
	 * Loads data objects according to the internal sourcing method (defined in the implementation of {@link GenericDefaultDataObjectLoader}).
	 * 
	 * @param lastModTS the value to overwrite the internal most recent modification time/date of data objects for this call
	 * @param returnDeleted if true, data objects flagged as deleted ({@link GenericDataObject#getIsDeleted()} == true) will also be returned
	 * @return a stream of data objects
	 * @throws DALException re-throws every exception as DAL exception
	 */
	public Stream<DO> getRecent(long lastModTS, boolean returnDeleted) throws DALException;

	/**
	 * Persists any changes to a data object (DO) to the underlying data source and updates
	 * the DO's modification time stamp {@link GenericDataObject#getModifiedTimeStamp()}
	 * 
	 * @param dataObject the data object to save
	 * @throws IllegalArgumentException thrown if parameter dataObject is null
	 * @throws DALException re-throws every exception as DAL exception
	 */
	public void save(DO dataObject) throws IllegalArgumentException, DALException;
	
	/**
	 * Persists any changes to a data object (DO) to the underlying data source and updates
	 * the DO's modification time stamp {@link GenericDataObject#getModifiedTimeStamp()}
	 * 
	 * @param dataObject the data object to save
	 * @throws IllegalArgumentException thrown if parameter dataObject is null or an instance of an incompatible class
	 * @throws DALException re-throws every exception as DAL exception
	 */
	public void save(Object dataObject) throws IllegalArgumentException, DALException;

	/**
	 * Persists any changes to the data objects (DOs) to the underlying data source and updates
	 * the DOs' modification time stamps {@link GenericDataObject#getModifiedTimeStamp()}
	 * 
	 * @param dataObjects the data objects to save
	 * @throws IllegalArgumentException thrown if parameter dataObjects is null
	 * @throws DALException re-throws every exception as DAL exception
	 */
	public void save(Collection<DO> dataObjects) throws IllegalArgumentException, DALException;

	/**
	 * Persists any changes to the data objects (DOs) to the underlying data source and updates
	 * the DOs' modification time stamps {@link GenericDataObject#getModifiedTimeStamp()}
	 * 
	 * @param dataObjects the data objects to save
	 * @throws IllegalArgumentException thrown if parameter dataObjects is null or one element is an instance of an incompatible class
	 * @throws DALException re-throws every exception as DAL exception
	 */
	public void save(GenericDataObject<ID>[] dataObjects) throws IllegalArgumentException, DALException;

	/**
	 * Flags a data object (DO) as deleted and removes it from the underlying data source.
	 * After the call, the DO's deleted flag is true ({@link GenericDataObject#getIsDeleted()} == true).
	 * If it was physically deleted from the underlying data storage depends on the DAO's implementation.
	 * 
	 * @param dataObject the data object to delete
	 * @throws IllegalArgumentException thrown if parameter dataObject is null
	 * @throws DALException re-throws every exception as DAL exception
	 */
	public void delete(DO dataObject) throws IllegalArgumentException, DALException;
	
	/**
	 * Flags a data object (DO) as deleted and removes it from the underlying data source.
	 * After the call, the DO's deleted flag is true ({@link GenericDataObject#getIsDeleted()} == true).
	 * If it was physically deleted from the underlying data storage depends on the DAO's implementation.
	 * 
	 * @param dataObject the data object to delete
	 * @throws IllegalArgumentException thrown if parameter dataObject is null or an instance of an incompatible class
	 * @throws DALException re-throws every exception as DAL exception
	 */
	public void delete(Object dataObject) throws IllegalArgumentException, DALException;

	/**
	 * Flags the data objects (DOs) as deleted and removes them from the underlying data source.
	 * After the call, the DOs' deleted flag is true ({@link GenericDataObject#getIsDeleted()} == true).
	 * If they were physically deleted from the underlying data storage depends on the DAO's implementation.
	 * 
	 * @param dataObjects the data objects to delete as array
	 * @throws IllegalArgumentException thrown if parameter dataObjects is null
	 * @throws DALException re-throws every exception as DAL exception
	 */
	public void delete(DO[] dataObjects) throws IllegalArgumentException, DALException;

	/**
	 * Flags the data objects (DOs) as deleted and removes them from the underlying data source.
	 * After the call, the DOs' deleted flag is true ({@link GenericDataObject#getIsDeleted()} == true).
	 * If they were physically deleted from the underlying data storage depends on the DAO's implementation.
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
	 * 
	 * @param dataObjectId the id of the data object to delete
	 * @throws IllegalArgumentException thrown if parameter dataObjectIds is null
	 * @throws DALException re-throws every exception as DAL exception
	 */
	public void delete(ID dataObjectId) throws IllegalArgumentException, DALException;

	/**
	 * Deletes all data objects (DOs) with the given ids.
	 * After the call, the DOs' deleted flag MAY BE true ({@link GenericDataObject#getIsDeleted()} == true) 
	 * if an update of the DOs' deleted flag was implemented in the DAO's implementation.
	 * Otherwise it will be false.
	 * 
	 * @param dataObjectIds the ids of data objects to delete
	 * @throws IllegalArgumentException thrown if parameter dataObjectIds is null
	 * @throws DALException re-throws every exception as DAL exception
	 */
	public void delete(ID[] dataObjectIds) throws IllegalArgumentException, DALException;

	/**
	 * Resets the data access object and clears the underlying storage if supported
	 * 
	 * @throws DALException re-throws every exception as DAL exception
	 */
	public void clear() throws DALException;

	/**
	 * Registers a new listener which will be called before getting DOs and after DOs were loaded.
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