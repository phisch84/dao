package com.schoste.ddd.infrastructure.dal.v2.services;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import com.schoste.ddd.infrastructure.dal.v2.exceptions.DALException;
import com.schoste.ddd.infrastructure.dal.v2.models.GenericDataObject;
import com.schoste.ddd.infrastructure.dal.v2.services.listeners.DeleteListener;
import com.schoste.ddd.infrastructure.dal.v2.services.listeners.GetListener;
import com.schoste.ddd.infrastructure.dal.v2.services.listeners.ReloadListener;
import com.schoste.ddd.infrastructure.dal.v2.services.listeners.SaveListener;

/**
 * Basic class with functionality common to all other generic data access objects
 * 
 * @author Philipp Schosteritsch
 *
 * @param <T> the class of the data object to persist
 */
public abstract class GenericDAO <T extends GenericDataObject> implements GenericDataAccessObject<T> 
{
	protected long latestModificationTimeStamp = Long.MIN_VALUE;
	protected HashSet<GetListener<T>> onGetListeners = new HashSet<>();
	protected HashSet<SaveListener<T>> onSaveListeners = new HashSet<>();
	protected HashSet<DeleteListener<T>> onDeleteListeners = new HashSet<>();
	protected HashSet<ReloadListener<T>> onReloadListeners = new HashSet<>();

	/**
	 * The method that actually gets a data object and needs to be implemented by its deriving class.
	 * Consider implementing the method synchronized for thread safety.
	 * 
	 * @param id the id of the data object to get
	 * @return the data object with the given id or null if not found
	 * @throws Exception re-throws every exception
	 */
	protected abstract T doGet(int id) throws Exception;
	
	/**
	 * The method that actually gets data objects and needs to be implemented by its deriving class.
	 * Consider implementing the method synchronized for thread safety.
	 * 
	 * @param ids the ids of the data objects to get
	 * @return a list of the data objects with the given ids.
	 * @throws Exception re-throws every exception
	 */
	protected abstract Collection<T> doGet(int[] ids) throws Exception;

	/**
	 * The method to actually save the data object and needs to be implemented by its deriving class.
	 * Consider implementing the method synchronized for thread safety.
	 * 
	 * @param dataObject the data object to save
	 * @throws Exception re-throws every exception
	 */
	protected abstract void doSave(T dataObject) throws Exception;

	/**
	 * The method to actually save the data objects and needs to be implemented by its deriving class.
	 * Consider implementing the method synchronized for thread safety.
	 * 
	 * @param dataObjects the data objects to save
	 * @throws Exception re-throws every exception
	 */
	protected abstract void doSave(Collection<T> dataObjects) throws Exception;

	/**
	 * The method to actually deletes the data object and needs to be implemented by its deriving class.
	 * Consider implementing the method synchronized for thread safety.
	 * 
	 * @param dataObject the data object to delete
	 * @throws Exception re-throws every exception
	 */
	protected abstract void doDelete(T dataObject) throws Exception;

	/**
	 * The method to actually deletes the data objects and needs to be implemented by its deriving class.
	 * Consider implementing the method synchronized for thread safety.
	 * 
	 * @param dataObjects the data objects to delete
	 * @throws Exception re-throws every exception
	 */
	protected abstract void doDelete(Collection<T> dataObjects) throws Exception;

	/**
	 * The method to actually deletes the data objects with given ids and needs to be implemented by its deriving class.
	 * Consider implementing the method synchronized for thread safety.
	 * 
	 * @param dataObjectIds the ids of the data objects to delete
	 * @throws Exception re-throws every exception
	 */
	protected abstract void doDelete(int[] dataObjectIds) throws Exception;

	/**
	 * The method that actually reloads data objects and needs to be implemented by its deriving class.
	 * Consider implementing the method synchronized for thread safety.
	 * 
	 * @return a list of loaded data objects
	 * @throws Exception re-throws every exception
	 */
	protected abstract Collection<T> doReloadAll() throws Exception;

	/**
	 * Clears the actual storage. This method needs to be implemented by its deriving class.
	 * Consider implementing the method synchronized for thread safety.
	 * 
	 * To be overwritten by the extending class.
	 * @throws Exception re-throws every exception
	 */
	protected abstract void doClear() throws Exception;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public abstract T createDataObject() throws DALException;

	/**
	 * Updates the latest modification time stamp (ts) of the DAO to the ts of
	 * the data object in the provided collection with the greatest (most recent)
	 * ts in the collection.
	 * 
	 * @param dataObjects a set of data object to check if one's ts is greater (more recent)
	 */
	protected synchronized void updateLatestModificationDate(Collection<T> dataObjects)
	{
		for (T dataObject : dataObjects) updateLatestModificationDate(dataObject);
	}
	
	/**
	 * Updates the latest modification time stamp (ts) of the DAO to the ts of
	 * the data object, if the data object's ts is greater (more recent) than the
	 * ts of the DAO.
	 * 
	 * @param dataObject the data object to check if its ts is greater (more recent)
	 */
	protected synchronized void updateLatestModificationDate(T dataObject)
	{
		long doTS = dataObject.getModifiedTimeStamp();
		
		if (doTS > this.latestModificationTimeStamp) this.latestModificationTimeStamp = doTS;
	}
	
	/**
	 * Obtains the actual data class of data objects that derive from GenericDataObject
	 * 
	 * @return the actual data class
	 * @throws IllegalStateException thrown if the actual data class cannot be obtained
	 * @throws Exception re-throws every exception
	 */
	@SuppressWarnings("rawtypes")
	protected Class getDataObjectClass() throws IllegalStateException, Exception
	{
		Type[] types = ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments();

		if (types.length < 1) throw new IllegalStateException();

		return (Class) types[0];
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clear() throws DALException
	{
		try
		{
			this.doClear();
			this.latestModificationTimeStamp = Long.MIN_VALUE;
		}
		catch (Exception e)
		{
			throw new DALException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void registerOnGetListener(GetListener<T> listener) throws IllegalArgumentException
	{
		if (listener == null) throw new IllegalArgumentException("listener");

		this.onGetListeners.add(listener);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void unregisterOnGetListener(GetListener<T> listener) throws IllegalArgumentException
	{
		if (listener == null) throw new IllegalArgumentException("listener");

		this.onGetListeners.remove(listener);		
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized Collection<GetListener<T>> getOnGetListeners()
	{
		return new HashSet<>(this.onGetListeners);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void registerOnSaveListener(SaveListener<T> listener) throws IllegalArgumentException
	{
		if (listener == null) throw new IllegalArgumentException("listener");

		this.onSaveListeners.add(listener);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void unregisterOnSaveListener(SaveListener<T> listener) throws IllegalArgumentException
	{
		if (listener == null) throw new IllegalArgumentException("listener");

		this.onSaveListeners.remove(listener);		
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized Collection<SaveListener<T>> getOnSaveListeners()
	{
		return new HashSet<>(this.onSaveListeners);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void registerOnDeleteListener(DeleteListener<T> listener) throws IllegalArgumentException
	{
		if (listener == null) throw new IllegalArgumentException("listener");

		this.onDeleteListeners.add(listener);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void unregisterOnDeleteListener(DeleteListener<T> listener) throws IllegalArgumentException
	{
		if (listener == null) throw new IllegalArgumentException("listener");

		this.onDeleteListeners.remove(listener);		
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized Collection<DeleteListener<T>> getOnDeleteListeners()
	{
		return new HashSet<>(this.onDeleteListeners);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void registerOnReloadListener(ReloadListener<T> listener) throws IllegalArgumentException
	{
		if (listener == null) throw new IllegalArgumentException("listener");

		this.onReloadListeners.add(listener);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void unregisterOnReloadListener(ReloadListener<T> listener) throws IllegalArgumentException
	{
		if (listener == null) throw new IllegalArgumentException("listener");

		this.onReloadListeners.remove(listener);		
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized Collection<ReloadListener<T>> getOnReloadListeners()
	{
		return new HashSet<>(this.onReloadListeners);
	}

	/**
	 * {@inheritDoc}
	 */
	public T get(int id) throws DALException 
	{
		try
		{
			int actualId = id;
			
			for (GetListener<T> onGetListener : this.onGetListeners)
			{
				actualId = onGetListener.onBeforeGet(actualId);
			}
	
			T dataObject = this.doGet(actualId);
	
			for (GetListener<T> onGetListener : this.onGetListeners) onGetListener.onAfterGet(dataObject);

			return dataObject;
		}
		catch (Exception e)
		{
			throw new DALException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Collection<T> get(int[] ids) throws IllegalArgumentException, DALException
	{
		if (ids == null) throw new IllegalArgumentException("ids");

		try
		{
			int[] actualIds = ids;
			
			for (GetListener<T> onGetListener : this.onGetListeners)
			{
				actualIds = onGetListener.onBeforeGet(actualIds);
			}
	
			Collection<T> dataObjects = this.doGet(actualIds);
	
			for (GetListener<T> onGetListener : this.onGetListeners) onGetListener.onAfterGet(dataObjects);

			return dataObjects;
		}
		catch (Exception e)
		{
			throw new DALException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Collection<T> getAll() throws DALException 
	{
		try
		{
			int[] actualIds = null;
			
			for (GetListener<T> onGetListener : this.onGetListeners)
			{
				actualIds = onGetListener.onBeforeGet(actualIds);
			}
	
			Collection<T> dataObjects = this.doGet(actualIds);
	
			for (GetListener<T> onGetListener : this.onGetListeners) onGetListener.onAfterGet(dataObjects);

			return dataObjects;
		}
		catch (Exception e)
		{
			throw new DALException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void save(T dataObject) throws IllegalArgumentException, DALException 
	{
		if (dataObject == null) throw new IllegalArgumentException("dataObject");

		try
		{
			for (SaveListener<T> onSaveListener : this.onSaveListeners)
			{
				if (!onSaveListener.onBeforeSaving(dataObject)) return;
			}
	
			this.doSave(dataObject);
	
			for (SaveListener<T> onSaveListener : this.onSaveListeners) onSaveListener.onAfterSaved(dataObject);
		}
		catch (Exception e)
		{
			throw new DALException(e);
		}
	}
	
	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	public void save(Object dataObject) throws IllegalArgumentException, DALException 
	{
		try
		{
			if (dataObject == null) throw new IllegalArgumentException("dataObject");
			if (!this.getDataObjectClass().isInstance(dataObject)) throw new IllegalArgumentException("dataObject");

			this.save((T) dataObject);
		}
		catch (IllegalArgumentException e)
		{
			throw e;
		}
		catch (DALException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new DALException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void save(Collection<T> dataObjects) throws IllegalArgumentException, DALException
	{
		if (dataObjects == null) throw new IllegalArgumentException("dataObjects");

		try
		{
			for (SaveListener<T> onSaveListener : this.onSaveListeners)
			{
				if (!onSaveListener.onBeforeSaving(dataObjects)) return;
			}
	
			this.doSave(dataObjects);
	
			for (SaveListener<T> onSaveListener : this.onSaveListeners) onSaveListener.onAfterSaved(dataObjects);
		}
		catch (Exception e)
		{
			throw new DALException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void save(GenericDataObject[] dataObjects) throws DALException
	{
		try
		{
			if (dataObjects == null) throw new IllegalArgumentException("dataObjects");
	
			Collection<T> dataObjectsList = new ArrayList<>(dataObjects.length);
	
			for (GenericDataObject dataObject : dataObjects)
			{
				if (!this.getDataObjectClass().isInstance(dataObject)) throw new IllegalArgumentException("dataObject");

				dataObjectsList.add((T) dataObject);
			}

			for (SaveListener<T> onSaveListener : this.onSaveListeners)
			{
				if (!onSaveListener.onBeforeSaving(dataObjectsList)) return;
			}

			this.doSave(dataObjectsList);

			for (SaveListener<T> onSaveListener : this.onSaveListeners) onSaveListener.onAfterSaved(dataObjectsList);
		}
		catch (IllegalArgumentException e)
		{
			throw e;
		}
		catch (DALException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new DALException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void delete(T dataObject) throws IllegalArgumentException, DALException 
	{
		if (dataObject == null) throw new IllegalArgumentException("dataObject");

		try
		{
			for (DeleteListener<T> onDeleteListener : this.onDeleteListeners)
			{
				if (!onDeleteListener.onBeforeDeleting(dataObject)) return;
			}
	
			this.doDelete(dataObject);
	
			for (DeleteListener<T> onDeleteListener : this.onDeleteListeners) onDeleteListener.onAfterDeleted(dataObject);
		}
		catch (Exception e)
		{
			throw new DALException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	public void delete(Object dataObject) throws IllegalArgumentException, DALException 
	{
		try
		{
			if (dataObject == null) throw new IllegalArgumentException("dataObject");
			if (!this.getDataObjectClass().isInstance(dataObject)) throw new IllegalArgumentException("dataObject");
			
			this.delete((T) dataObject);
		}
		catch (IllegalArgumentException e)
		{
			throw e;
		}
		catch (DALException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new DALException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void delete(Collection<T> dataObjects) throws IllegalArgumentException, DALException
	{
		if (dataObjects == null) throw new IllegalArgumentException("dataObjects");

		try
		{
			for (DeleteListener<T> onDeleteListener : this.onDeleteListeners)
			{
				if (!onDeleteListener.onBeforeDeleting(dataObjects)) return;
			}
	
			this.doDelete(dataObjects);
	
			for (DeleteListener<T> onDeleteListener : this.onDeleteListeners) onDeleteListener.onAfterDeleted(dataObjects);
		}
		catch (Exception e)
		{
			throw new DALException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void delete(int[] dataObjectIds) throws IllegalArgumentException, DALException
	{
		if (dataObjectIds == null) throw new IllegalArgumentException("dataObjectIds");

		try
		{
			int[] dataObjectIdsToDelete = dataObjectIds;
	
			for (DeleteListener<T> onDeleteListener : this.onDeleteListeners)
			{
				dataObjectIdsToDelete = onDeleteListener.onBeforeDeleting(dataObjectIdsToDelete);
			}
	
			this.doDelete(dataObjectIdsToDelete);
	
			for (DeleteListener<T> onDeleteListener : this.onDeleteListeners) onDeleteListener.onAfterDeleted(dataObjectIdsToDelete);
		}
		catch (Exception e)
		{
			throw new DALException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void delete(GenericDataObject[] dataObjects) throws DALException
	{
		try
		{
			if (dataObjects == null) throw new IllegalArgumentException("dataObjects");
	
			Collection<T> dataObjectsList = new ArrayList<>(dataObjects.length);
	
			for (GenericDataObject dataObject : dataObjects)
			{
				if (!this.getDataObjectClass().isInstance(dataObject)) throw new IllegalArgumentException("dataObject");

				dataObjectsList.add((T) dataObject);
			}

			for (DeleteListener<T> onDeleteListener : this.onDeleteListeners)
			{
				if (!onDeleteListener.onBeforeDeleting(dataObjectsList)) return;
			}
	
			this.doDelete(dataObjectsList);
	
			for (DeleteListener<T> onDeleteListener : this.onDeleteListeners) onDeleteListener.onAfterDeleted(dataObjectsList);
		}
		catch (IllegalArgumentException e)
		{
			throw e;
		}
		catch (DALException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new DALException(e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Collection<T> reloadAll() throws DALException
	{
		try
		{
			Collection<T> dataObjects = new ArrayList<>();

			for (ReloadListener<T> onReloadListener : this.onReloadListeners)
			{
				if (!onReloadListener.onBeforeReload(dataObjects)) return dataObjects;
			}

			dataObjects.addAll(this.doReloadAll());

			for (ReloadListener<T> onReloadListener : this.onReloadListeners) onReloadListener.onAfterReload(dataObjects);

			return dataObjects;
		}
		catch (Exception e)
		{
			throw new DALException(e);
		}
	}
}
