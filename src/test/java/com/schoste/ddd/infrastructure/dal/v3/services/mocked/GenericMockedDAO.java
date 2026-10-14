package com.schoste.ddd.infrastructure.dal.v3.services.mocked;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.schoste.ddd.infrastructure.dal.v3.services.GenericDAO;
import com.schoste.ddd.infrastructure.dal.v3.models.GenericDataObject;

/**
 * Mocking implementation of the GenericDataAccessObject interface.
 * This class actually doesn't access any data source, but defines the expected
 * behavior of data access objects (DAOs) of version 3.
 * 
 * Other layers can use this class to test their implementations without an actual
 * data source.
 * 
 * @author Philipp Schosteritsch
 *
 * @param <T> the type of the data object (DO) to access
 */
public abstract class GenericMockedDAO<T extends GenericDataObject<Long>> extends GenericDAO<Long, T>
{
	protected Map<Long, T> dataObjects = new HashMap<Long, T>();
	protected Iterator<Long> itDataObjectIds = null;
	protected long lastDataObjectId = 0;
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	protected synchronized void doClear() throws Exception
	{
		this.dataObjects.clear();
		this.lastDataObjectId = 0;
	}

	/**
	 * Gets the ids of all data objects which are currently in the mocked storage.
	 * Used by the mocked data object loader to determine the source records to load.
	 * 
	 * @return a copy of the ids of all data objects in the mocked storage
	 */
	public synchronized Collection<Long> getDataObjectIds()
	{
		return new ArrayList<Long>(this.dataObjects.keySet());
	}

	/**
	 * Gets the data object with the given id from the mocked storage.
	 * 
	 * If the id is a virtual id, the method doesn't look for a data object with this id. Instead every call returns
	 * the next data object of the mocked storage, and null once after all of them were returned.
	 * {@link MockedRecentDataObjectLoaderImpl} relies on this behavior to iterate over all data objects.
	 * 
	 * @param id the id of the data object to get, or a virtual id to get the next data object of the mocked storage
	 * @return the data object, or null if not found (or if there is no next data object)
	 * @throws Exception re-throws every exception
	 */
	@Override
	protected synchronized T doGet(Long id) throws Exception
	{
		if (this.isVirtualId(id))
		{
			if (this.itDataObjectIds == null) this.itDataObjectIds = this.dataObjects.keySet().iterator();
			if (!this.itDataObjectIds.hasNext())
			{
				this.itDataObjectIds = null;

				return null;
			}

			Long doID = this.itDataObjectIds.next();

			if (this.dataObjects.containsKey(doID)) return this.dataObjects.get(doID);
			else return null;
		}

		if (this.dataObjects.containsKey(id)) return this.dataObjects.get(id);

		return null;
	}

	protected Collection<T> getSomeDOs(Integer[] ids) throws Exception
	{
		Collection<T> dataObjects = this.dataObjects.values();
		Collection<T> found = new ArrayList<T>(ids.length);
		Collection<Integer> dataObjectIds = new ArrayList<Integer>(ids.length);

		for (int id : ids) dataObjectIds.add(id);

		for (T dataObject : dataObjects)
		{
			if (dataObjectIds.contains(dataObject.getId())) found.add(dataObject);
		}

		return found;
	}

	/**
	 * Gets all data objects from the mocked storage which are not flagged as deleted and which were modified after
	 * the DAO's last modification time stamp. The DAO's last modification time stamp is updated to the biggest
	 * modification time stamp of the returned data objects.
	 * This method is currently not used.
	 * 
	 * @return a collection of data objects
	 * @throws Exception re-throws every exception
	 */
	protected Collection<T> getAllDOs() throws Exception
	{
		Collection<T> all = this.dataObjects.values();
		Collection<T> undeletedAndNewOrModified = new ArrayList<T>(all.size());

		for (T dataObject : all)
		{
			if (dataObject.getIsDeleted()) continue;
			if (dataObject.getModifiedTimeStamp() <= this.latestModificationTimeStamp) continue;
			
			undeletedAndNewOrModified.add(dataObject);
		}

		T latestModifiedObject = undeletedAndNewOrModified.stream().max(Comparator.comparing(GenericDataObject::getModifiedTimeStamp)).orElseGet(() -> null);

		if (latestModifiedObject != null) this.latestModificationTimeStamp = latestModifiedObject.getModifiedTimeStamp();

		return undeletedAndNewOrModified;		
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected synchronized void doSave(T dataObject) throws Exception
	{
		if (dataObject.getId() < 1) dataObject.setId(++this.lastDataObjectId);

		this.dataObjects.put(dataObject.getId(), dataObject);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected synchronized void doSave(Collection<T> dataObjects) throws Exception
	{
		for (T dataObject : dataObjects) this.doSave(dataObject);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected synchronized void doDelete(T dataObject) throws Exception
	{
		dataObject.setIsDeleted(true);

		this.dataObjects.remove(dataObject.getId());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected synchronized void doDelete(Collection<T> dataObjects) throws Exception
	{
		for (T dataObject : dataObjects) this.doDelete(dataObject);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected synchronized void doDelete(Long[] dataObjectIds) throws Exception
	{
		for (long dataObjectId : dataObjectIds) 
		{
			T dataObj = this.dataObjects.remove(dataObjectId);

			if (dataObj != null) dataObj.setIsDeleted(true);
		}
	}
}
