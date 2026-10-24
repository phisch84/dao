package com.schoste.ddd.infrastructure.dao.v4.inmemory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import com.schoste.ddd.infrastructure.dao.v4.services.DAO;
import com.schoste.ddd.infrastructure.dao.v4.services.DeletedDataObjects;

import com.schoste.ddd.infrastructure.dao.v4.services.DataObjectLoader;
import com.schoste.ddd.infrastructure.dao.v4.services.AbstractDAO;
import com.schoste.ddd.infrastructure.dao.v4.services.SourceRecordDataObjectLoader;
import com.schoste.ddd.infrastructure.dao.v4.services.GenericRecentDataObjectLoader;
import com.schoste.ddd.infrastructure.dao.v4.models.DataObject;

/**
 * Mocking implementation of the DAO interface.
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
public abstract class AbstractInMemoryDAO<T extends DataObject<Long>> extends AbstractDAO<Long, T>
{
	protected Map<Long, T> dataObjects = new HashMap<Long, T>();
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
	 * Used by the data object loaders to determine the source records to load.
	 *
	 * @return a copy of the ids of all data objects in the mocked storage
	 */
	public synchronized Collection<Long> getDataObjectIds()
	{
		return new ArrayList<Long>(this.dataObjects.keySet());
	}

	/**
	 * {@inheritDoc}
	 *
	 * The source records are the ids to filter for if there are any. Otherwise these are the ids of all data objects
	 * in the mocked storage at the time the loader is created.
	 */
	@Override
	protected DataObjectLoader<?, Long, T> doCreateLoader(Set<Long> ids) throws Exception
	{
		Collection<Long> sourceIds = ids.isEmpty() ? this.getDataObjectIds() : ids;

		return new SourceRecordDataObjectLoader<>(this, sourceIds.stream(), this::convertSourceRecord, ids);
	}

	/**
	 * {@inheritDoc}
	 *
	 * The source records are the ids of all data objects in the mocked storage at the time the loader is created.
	 */
	@Override
	protected DataObjectLoader<?, Long, T> doCreateLoader(long modifiedSince, DeletedDataObjects deleted) throws Exception
	{
		return new GenericRecentDataObjectLoader<>(this, this.getDataObjectIds().stream(), this::convertSourceRecord, modifiedSince, deleted == DeletedDataObjects.INCLUDE);
	}

	/**
	 * Gets the data object with the given id from the mocked storage.
	 *
	 * @param id the id of the data object to get
	 * @return the data object, or null if not found
	 * @throws Exception re-throws every exception
	 */
	@Override
	protected synchronized T doGet(Long id) throws Exception
	{
		if (this.dataObjects.containsKey(id)) return this.dataObjects.get(id);

		return null;
	}

	/**
	 * Gets all data objects with the given ids if they are present in this DAO
	 * 
	 * @param ids the ids of the data objects to get
	 * @return a collection of data objects that existed in the DAO if their id is among the requested ids
	 * @throws Exception re-throws every exception
	 */
	@SuppressWarnings("unlikely-arg-type")
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
			if (dataObject.isDeleted()) continue;
			if (dataObject.getModifiedTimestamp() <= this.latestModificationTimeStamp) continue;
			
			undeletedAndNewOrModified.add(dataObject);
		}

		T latestModifiedObject = undeletedAndNewOrModified.stream().max(Comparator.comparing(DataObject::getModifiedTimestamp)).orElseGet(() -> null);

		if (latestModifiedObject != null) this.latestModificationTimeStamp = latestModifiedObject.getModifiedTimestamp();

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
		dataObject.setDeleted(true);

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

			if (dataObj != null) dataObj.setDeleted(true);
		}
	}
}
