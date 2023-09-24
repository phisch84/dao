package com.schoste.ddd.infrastructure.dal.v2.services;

import java.util.Collection;

import com.schoste.ddd.infrastructure.dal.v2.exceptions.DALException;
import com.schoste.ddd.infrastructure.dal.v2.models.GenericDataObject;
import com.schoste.ddd.infrastructure.dal.v2.services.listeners.DeleteListener;
import com.schoste.ddd.infrastructure.dal.v2.services.listeners.GetListener;
import com.schoste.ddd.infrastructure.dal.v2.services.listeners.ReloadListener;
import com.schoste.ddd.infrastructure.dal.v2.services.listeners.SaveListener;

/**
 * Interface to the generic data access object (DAO) class.
 * It defines methods common to all DAOs.
 * 
 * @author Philipp Schosteritsch
 *
 * @param <T> any data object that extends the GenericDataObject class
 */
public interface GenericDataAccessObject<T extends GenericDataObject>
{
	/**
	 * Creates a new data object
	 * 
	 * @return an instance to a new data object
	 * @throws DALException re-throws every exception as DAL exception
	 */
	T createDataObject() throws DALException;

	/**
	 * Gets a data object from the data source by its id.
	 * This method also returns deleted data objects.
	 * 
	 * @param id the identifier of the data object
	 * @return the data object with the given id or null if none was found
	 * @throws DALException re-throws every exception as DAL exception
	 */
	public T get(int id) throws DALException;

	/**
	 * Gets all data objects from the data source by its ids.
	 * This method also returns data objects flagged deleted (if returned by the storage).
	 * 
	 * @param ids the identifiers of the data objects
	 * @return a collection data objects with the given ids. If the id was not found, the data objects is not included in the list.
	 * @throws IllegalArgumentException thrown if parameter ids is null
	 * @throws DALException re-throws every exception as DAL exception
	 */
	Collection<T> get(int[] ids) throws IllegalArgumentException, DALException;

	/**
	 * Gets all not deleted data objects from the underlying data source
	 * which's modification time stamp is greater than the last saved
	 * modification time stamp
	 * 
	 * @return a collection of data objects
	 * @throws DALException re-throws every exception as DAL exception
	 */
	public Collection<T> getAll() throws DALException;
	
	/**
	 * Resets the modification time stamp and gets all not deleted data objects
	 * from the underlying data source
	 * 
	 * @return a collection of data objects
	 * @throws DALException re-throws every exception as DAL exception
	 */
	public Collection<T> reloadAll() throws DALException;

	/**
	 * Persists any changes to a data object to the underlying data source
	 * 
	 * @param dataObject the data object to save
	 * @throws IllegalArgumentException thrown if parameter dataObject is null
	 * @throws DALException re-throws every exception as DAL exception
	 */
	public void save(T dataObject) throws IllegalArgumentException, DALException;
	
	/**
	 * Persists any changes to a data object to the underlying data source
	 * 
	 * @param dataObject the data object to save
	 * @throws IllegalArgumentException thrown if parameter dataObject is null or an instance of an incompatible class
	 * @throws DALException re-throws every exception as DAL exception
	 */
	public void save(Object dataObject) throws IllegalArgumentException, DALException;

	/**
	 * Persists any changes to the given data objects to the underlying data source
	 * 
	 * @param dataObjects the data objects to save
	 * @throws IllegalArgumentException thrown if parameter dataObjects is null
	 * @throws DALException re-throws every exception as DAL exception
	 */
	void save(Collection<T> dataObjects) throws IllegalArgumentException, DALException;

	/**
	 * Persists any changes to the given data objects to the underlying data source
	 * 
	 * @param dataObjects the data objects to save
	 * @throws IllegalArgumentException thrown if parameter dataObjects is null or one element is an instance of an incompatible class
	 * @throws DALException re-throws every exception as DAL exception
	 */
	void save(GenericDataObject[] dataObjects) throws IllegalArgumentException, DALException;

	/**
	 * Flags a data object as deleted and persists it to the underlying data source
	 * 
	 * @param dataObject the data object to delete
	 * @throws IllegalArgumentException thrown if parameter dataObject is null
	 * @throws DALException re-throws every exception as DAL exception
	 */
	public void delete(T dataObject) throws IllegalArgumentException, DALException;
	
	/**
	 * Flags a data object as deleted and persists it to the underlying data source
	 * 
	 * @param dataObject the data object to delete
	 * @throws IllegalArgumentException thrown if parameter dataObject is null or an instance of an incompatible class
	 * @throws DALException re-throws every exception as DAL exception
	 */
	public void delete(Object dataObject) throws IllegalArgumentException, DALException;

	/**
	 * Flags the given data objects as deleted and persists them to the underlying data source
	 * 
	 * @param dataObjects the data objects to delete
	 * @throws IllegalArgumentException thrown if parameter dataObjects is null
	 * @throws DALException re-throws every exception as DAL exception
	 */
	void delete(Collection<T> dataObjects) throws IllegalArgumentException, DALException;

	/**
	 * Flags the given data objects as deleted and persists them to the underlying data source
	 * 
	 * @param dataObjects the data objects to delete
	 * @throws IllegalArgumentException thrown if parameter dataObjects is null or one element is an instance of an incompatible class
	 * @throws DALException re-throws every exception as DAL exception
	 */
	void delete(GenericDataObject[] dataObjects) throws IllegalArgumentException, DALException;

	/**
	 * Deletes the data objects with given ids
	 * 
	 * @param dataObjectIds the ids of data objects to delete
	 * @throws IllegalArgumentException thrown if parameter dataObjectIds is null
	 * @throws DALException re-throws every exception as DAL exception
	 */
	void delete(int[] dataObjectIds) throws IllegalArgumentException, DALException;

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
	void registerOnGetListener(GetListener<T> listener) throws IllegalArgumentException;

	/**
	 * Unregisters a listener so it will not be called before getting DOs and after DOs were loaded.
	 * 
	 * @param listener the instance of the listener to unregister
	 * @throws IllegalArgumentException thrown if parameter listener is null
	 */
	void unregisterOnGetListener(GetListener<T> listener) throws IllegalArgumentException;

	/**
	 * Gets a copy of the collection of all registered listeners which will be called before getting DOs and after DOs were loaded.
	 * 
	 * @return a collection of {@link GetListener}
	 */
	Collection<GetListener<T>> getOnGetListeners();

	/**
	 * Registers a new listener which will be called before saving DOs and after DOs were saved.
	 * 
	 * @param listener the instance of the listener to register
	 * @throws IllegalArgumentException thrown if parameter listener is null
	 */
	void registerOnSaveListener(SaveListener<T> listener) throws IllegalArgumentException;

	/**
	 * Unregisters a listener so it will not be called before saving DOs and after DOs were saved.
	 * 
	 * @param listener the instance of the listener to unregister
	 * @throws IllegalArgumentException thrown if parameter listener is null
	 */
	void unregisterOnSaveListener(SaveListener<T> listener) throws IllegalArgumentException;

	/**
	 * Gets a copy of the collection of all registered listeners which will be called before saving DOs and after DOs were saved.
	 * 
	 * @return a collection of {@link SaveListener}
	 */
	Collection<SaveListener<T>> getOnSaveListeners();

	/**
	 * Registers a new listener which will be called before deleting DOs and after DOs were deleted.
	 * 
	 * @param listener the instance of the listener to register
	 * @throws IllegalArgumentException thrown if parameter listener is null
	 */
	void registerOnDeleteListener(DeleteListener<T> listener) throws IllegalArgumentException;

	/**
	 * Unregisters a listener so it will not be called before deleting DOs and after DOs were deleted.
	 * 
	 * @param listener the instance of the listener to unregister
	 * @throws IllegalArgumentException thrown if parameter listener is null
	 */
	void unregisterOnDeleteListener(DeleteListener<T> listener) throws IllegalArgumentException;

	/**
	 * Gets a copy of the collection of all registered listeners which will be called before deleting DOs and after DOs were deleted.
	 * 
	 * @return a collection of {@link DeleteListener}
	 */
	Collection<DeleteListener<T>> getOnDeleteListeners();

	/**
	 * Registers a new listener which will be called before reloading DOs and after DOs were reloaded.
	 * 
	 * @param listener the instance of the listener to register
	 * @throws IllegalArgumentException thrown if parameter listener is null
	 */
	void registerOnReloadListener(ReloadListener<T> listener) throws IllegalArgumentException;

	/**
	 * Unregisters a listener so it will not be called before reloading DOs and after DOs were reloaded.
	 * 
	 * @param listener the instance of the listener to unregister
	 * @throws IllegalArgumentException thrown if parameter listener is null
	 */
	void unregisterOnReloadListener(ReloadListener<T> listener) throws IllegalArgumentException;

	/**
	 * Gets a copy of the collection of all registered listeners which will be called before reloading DOs and after DOs were reloaded.
	 * 
	 * @return a collection of {@link DeleteListener}
	 */
	Collection<ReloadListener<T>> getOnReloadListeners();
}