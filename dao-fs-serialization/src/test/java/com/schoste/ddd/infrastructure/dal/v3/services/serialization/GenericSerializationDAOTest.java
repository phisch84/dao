package com.schoste.ddd.infrastructure.dal.v3.services.serialization;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.schoste.ddd.infrastructure.dal.v3.models.GenericDataObject;
import com.schoste.ddd.infrastructure.dal.v3.services.GenericDAOTest;
import com.schoste.ddd.infrastructure.dal.v3.services.GenericDataAccessObject;

/**
 * Generic test class to test implementations of the GenericSerializationDAO class.
 * This class provides standard test methods.
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 * @param <DO> The class of the data object
 * @param <DAO> The class of the data access object
 */
public abstract class GenericSerializationDAOTest <ID extends Serializable, DO extends GenericDataObject<ID>, DAO extends GenericDataAccessObject<ID, DO>> extends GenericDAOTest<ID, DO, DAO>
{
	protected Map<Long, ID> cachedDataObjects = new HashMap<>();

	/**
	 * Creates a new data object and saves it with a given id so it can be
	 * obtained later by {@link #getExistingDataObject(long)}.
	 * 
	 * Override it if you need a different functionality in your tests.
	 * 
	 * @param id the id of the object to retrieve it
	 * @param properties properties to be passed on to {@link #createDataObject(Object...)}
	 * @throws Exception re-throws every exception
	 */
	protected void cacheDataObject(long id, Object ...properties) throws Exception
	{
		Object[] parameters = (properties == null) ? new Object[1] : new Object[properties.length +1];
		
		parameters[0] = 0L;
		
		for (int i=0; i<((properties == null) ? 0 : properties.length); i++)
		{
			parameters[1+i] = properties[i];
		}
		
		DO dataObject = this.createDataObject(parameters);
		
		this.getDataAccessObject().save(dataObject);
		this.cachedDataObjects.put(id, dataObject.getId());
	}

	/**
	 * Creates a set of existing data objects stored with an id from 1-10.
	 * Override it if you need a different functionality in your tests.
	 * 
	 * @throws Exception re-throws every exception
	 */
	protected void clearAndSetUpExistingDataObjects() throws Exception
	{
		this.cachedDataObjects.clear();
		this.getDataAccessObject().clear();
		
		this.cacheDataObject(1L, "testSaveExisting");
		this.cacheDataObject(2L, "testGetAll1");
		this.cacheDataObject(3L, "testGetAll2");
		this.cacheDataObject(4L, "testGetAll3");
		this.cacheDataObject(5L, "testGetAll4");
		this.cacheDataObject(6L, "testGetAll5");
		this.cacheDataObject(7L, "testGetAll6");
		this.cacheDataObject(8L, "testGetAll7");
		this.cacheDataObject(9L, "testGetAll8");
		this.cacheDataObject(10L, "testGetAll9");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DO getExistingDataObject(long id) 
	{
		try
		{
			if (this.cachedDataObjects.containsKey(id))
			{
				ID cachedId = this.cachedDataObjects.get(id);
				
				return this.getDataAccessObject().get(cachedId);
			}
			else
			{
				return null; // this.getDataAccessObject().get(id);				
			}
		}
		catch (Exception e)
		{
			e.printStackTrace(System.err);
			
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean clearRepositorySucceeded() throws Exception 
	{
		this.getDataAccessObject().save(this.createDataObject());
		this.getDataAccessObject().clear();

		return (this.getDataAccessObject().getAll().collect(Collectors.toList()).size() == 0);
	}
}
