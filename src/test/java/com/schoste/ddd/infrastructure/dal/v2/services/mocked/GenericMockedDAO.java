package com.schoste.ddd.infrastructure.dal.v2.services.mocked;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.schoste.ddd.infrastructure.dal.v2.services.GenericDAO;
import com.schoste.ddd.infrastructure.dal.v2.models.GenericDataObject;
import com.schoste.ddd.infrastructure.dal.v2.services.GenericDataAccessObject;

/**
 * Mocking implementation of the GenericDataAccessObject interface.
 * This class actually doesn't access any data source, but defines the expected
 * behavior of data access objects (DAOs) of version 2.
 * 
 * Other layers can use this class to test their implementations without an actual
 * data source.
 * 
 * @author Philipp Schosteritsch
 *
 * @param <T> the type of the data object (DO) to access
 */
public abstract class GenericMockedDAO<T extends GenericDataObject> extends GenericDAO<T> implements GenericDataAccessObject<T> 
{
	protected Map<Integer, T> dataObjects = new HashMap<Integer, T>();
	
	protected int lastDataObjectId = 0;
	
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
	 * {@inheritDoc}
	 */
	@Override
	protected synchronized T doGet(int id) throws Exception
	{
		if (!this.dataObjects.containsKey(id)) return null;
		
		return this.dataObjects.get(id);
	}

	protected Collection<T> getSomeDOs(int[] ids) throws Exception
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
	protected synchronized Collection<T> doGet(int[] ids) throws Exception
	{
		if (ids != null) return this.getSomeDOs(ids);
		else return this.getAllDOs();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected synchronized void doSave(T dataObject) throws Exception
	{
		if (dataObject.getId() < 1)
		{
			dataObject.setId(++this.lastDataObjectId);
			dataObject.setCreatedTimeStamp(System.currentTimeMillis());			
		}

		this.dataObjects.put(dataObject.getId(), dataObject);

		dataObject.updateTimeStamps();
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
	protected synchronized void doDelete(int[] dataObjectIds) throws Exception
	{
		for (int dataObjectId : dataObjectIds) this.dataObjects.remove(dataObjectId);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected synchronized Collection<T> doReloadAll() throws Exception
	{
		return this.dataObjects.values();		
	}
}
