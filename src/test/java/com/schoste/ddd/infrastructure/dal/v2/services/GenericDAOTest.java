package com.schoste.ddd.infrastructure.dal.v2.services;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;

import com.schoste.ddd.infrastructure.dal.v2.models.GenericDataObject;
import com.schoste.ddd.infrastructure.dal.v2.services.GenericDataAccessObject;

/**
 * Generic test class to test implementations of the GenericSerializationDAO class.
 * This class provides standard test methods.
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 * @param <DO> The class of the data object
 * @param <DAO> The class of the data access object
 */
public abstract class GenericDAOTest <DO extends GenericDataObject, DAO extends GenericDataAccessObject<DO>>
{
	/**
	 * Gets the instance to the data access object used for testing
	 * 
	 * @return instance to the data access object
	 */
	protected abstract DAO getDataAccessObject();
	
	/**
	 * Creates a new data object used in testing
	 * 
	 * @param parameters controls which data access object is returned. It depends on the actual implementation
	 * @return a new data object
	 */
	protected abstract DO createDataObject(Object ...parameters);

	/**
	 * Modifies properties of a data object used in testing
	 * 
	 * @param dataObject the data object to modify
	 * @param parameters controls which data of the object are modified. It depends on the actual implementation
	 */
	protected abstract void modifyDataObject(DO dataObject, Object ...parameters);

	/**
	 * Expects to find an existing data object with a given id.
	 * 
	 * @param id the id of the existing data object
	 * @return a data object or null if not found
	 */
	protected abstract DO getExistingDataObject(int id);

	/**
	 * This method needs to assert of the clear method of the DAO implementation was successful
	 * 
	 * @return true if successful, false otherwise
	 * @throws Exception re-throws every exception
	 */
	protected abstract boolean clearRepositorySucceeded() throws Exception;

	/**
	 * Asserts that all registered get listeners contain a given number of data objects before getting
	 * 
	 * @param numOfexpectedDOs the number of expected data objects
	 * @return true if all listeners contain the exact number of DOs, false otherwise
	 */
	protected abstract boolean assertDefaultGetListenersBeforeGet(int numOfexpectedDOs);

	/**
	 * Asserts that all registered get listeners contain a given number of data objects after getting
	 * 
	 * @param numOfexpectedDOs the number of expected data objects
	 * @return true if all listeners contain the exact number of DOs, false otherwise
	 */
	protected abstract boolean assertDefaultGetListenersAfterGet(int numOfexpectedDOs);

	/**
	 * Asserts that all registered save listeners contain a given number of data objects before saving
	 * 
	 * @param numOfexpectedDOs the number of expected data objects
	 * @return true if all listeners contain the exact number of DOs, false otherwise
	 */
	protected abstract boolean assertDefaultSaveListenersBeforeSave(int numOfexpectedDOs);

	/**
	 * Asserts that all registered save listeners contain a given number of data objects after saving
	 * 
	 * @param numOfexpectedDOs the number of expected data objects
	 * @return true if all listeners contain the exact number of DOs, false otherwise
	 */
	protected abstract boolean assertDefaultSaveListenersAfterSave(int numOfexpectedDOs);

	/**
	 * Asserts that all registered delete listeners contain a given number of data objects before deleting
	 * 
	 * @param numOfexpectedDOs the number of expected data objects
	 * @return true if all listeners contain the exact number of DOs, false otherwise
	 */
	protected abstract boolean assertDefaultDeleteListenersBeforeDelete(int numOfexpectedDOs);

	/**
	 * Asserts that all registered delete listeners contain a given number of data objects after deleting
	 * 
	 * @param numOfexpectedDOs the number of expected data objects
	 * @return true if all listeners contain the exact number of DOs, false otherwise
	 */
	protected abstract boolean assertDefaultDeleteListenersAfterDelete(int numOfexpectedDOs);

	/**
	 * Asserts that all registered reload listeners contain a given number of data objects after reloading
	 * 
	 * @param numOfexpectedDOs the number of expected data objects
	 * @return true if all listeners contain the exact number of DOs, false otherwise
	 */
	protected abstract boolean assertDefaultReloadListenersAfterReload(int numOfexpectedDOs);

	/**
	 * Asserts that the save() method of a DAO implementation actually
	 * saves a data object
	 * 
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void testSave() throws Exception
	{
		DO dataObject = this.createDataObject(0, "testSave");
		
		this.getDataAccessObject().save(dataObject);
		
		Assert.assertNotEquals(0, dataObject.getId());
		Assert.assertNotEquals(0, dataObject.getModifiedTimeStamp());
	}
	
	/**
	 * Asserts that the save() method of a DAO implementation actually
	 * saves an existing data object.
	 * 
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void testSaveExisting() throws Exception
	{
		DO dataObject = this.getExistingDataObject(1);
		int id = dataObject.getId();
		long modTs = dataObject.getModifiedTimeStamp();
		
		Thread.sleep(100); // required to reliably test the change of the mod ts
		
		this.modifyDataObject(dataObject, "testSaveExisting");
		this.getDataAccessObject().save(dataObject);
		
		Assert.assertEquals(id, dataObject.getId());
		Assert.assertTrue(modTs < dataObject.getModifiedTimeStamp());
	}

	/**
	 * Asserts that the get() method of a DAO implementation returns a data object
	 * if it exists
	 * 
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void testSaveAndGetExisting() throws Exception
	{
		DO newObject = this.createDataObject(0, "testSaveAndGetExisting");
		
		this.getDataAccessObject().save(newObject);
		
		int newObjectId = newObject.getId();		
		DO dataObject = this.getDataAccessObject().get(newObjectId);
		
		Assert.assertNotNull(dataObject);
		Assert.assertNotEquals(0, dataObject.getId());
		Assert.assertNotEquals(0, dataObject.getModifiedTimeStamp());
	}
	
	/**
	 * Asserts that the get() method of a DAO implementation returns null
	 * if a not-existing data object is requested
	 * 
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void testGetNotExisting() throws Exception
	{
		DO dataObject = this.getDataAccessObject().get(9999);
		
		Assert.assertNull(dataObject);
	}
	
	/**
	 * Asserts that the get() method of a DAO implementation returns a data object
	 * if it exists
	 * 
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void testGetExisting() throws Exception
	{	
		DO dataObject = this.getExistingDataObject(1);

		Assert.assertNotNull(dataObject);
		Assert.assertNotEquals(0, dataObject.getModifiedTimeStamp());
		Assert.assertTrue(this.assertDefaultGetListenersBeforeGet(1));
		Assert.assertTrue(this.assertDefaultGetListenersAfterGet(1));
		
		// Cannot be asserted. What the id is depends on the implementation
		// Assert.assertEquals(1, dataObject.getId());
	}
	
	/**
	 * Asserts that the getAll() method of a DAO implementation returns 
	 * existing data objects
	 * 
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void testGetAll() throws Exception
	{
		int numOfDataObjects = 10;

		Collection<DO> dataObjects = this.getDataAccessObject().getAll();

		Assert.assertTrue(dataObjects.size() >= numOfDataObjects);
		Assert.assertTrue(this.assertDefaultGetListenersBeforeGet(0));
		Assert.assertTrue(this.assertDefaultGetListenersAfterGet(numOfDataObjects));

		// New IDs can be anything greater than 0
		for (DO dataObject : dataObjects) Assert.assertTrue(dataObject.getId() > 0);
	}
	
	/**
	 * Asserts that the getAll() method of a DAO implementation reloads
	 * data objects only if they were changed or added meanwhile
	 * 
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void testGetAllNewer() throws Exception
	{
		int numOfDataObjects = 10;
				
		this.getDataAccessObject().getAll();
		
		Collection<DO> dataObjects = this.getDataAccessObject().getAll();

		Assert.assertEquals(0, dataObjects.size());
		
		DO dataObject1 = this.getExistingDataObject(2);
		DO dataObject2 = this.getExistingDataObject(numOfDataObjects-1);
		
		Thread.sleep(100);
		
		this.modifyDataObject(dataObject1, "testGetAllNewer");
		this.modifyDataObject(dataObject2, "testGetAllNewer");
		
		this.getDataAccessObject().save(dataObject1);
		this.getDataAccessObject().save(dataObject2);
		
		Collection<DO> dataObjects_ = this.getDataAccessObject().getAll();
		
		Assert.assertEquals(2, dataObjects_.size());
//		Assert.assertTrue(this.assertDefaultGetListenersBeforeGet(2));
//		Assert.assertTrue(this.assertDefaultGetListenersAfterGet(14));
	}
	
	/**
	 * Asserts that the reloadAll() method of a DAO implementation reads all
	 * existing data objects from the file system
	 * 
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void testReloadAll() throws Exception
	{
		int numOfDataObjects = 10;
		
		this.getDataAccessObject().getAll();

		Collection<DO> dataObjects = this.getDataAccessObject().reloadAll();

		Assert.assertTrue(dataObjects.size() >= numOfDataObjects);
//		Assert.assertTrue(this.assertDefaultReloadListenersAfterReload(numOfDataObjects));
	}
	
	/**
	 * Asserts that the delete() method of a DAO implementation actually
	 * deletes the file of a data object in the file system
	 * 
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void testDelete() throws Exception
	{
		DO dataObject = this.createDataObject(0, "testDelete");
		
		this.getDataAccessObject().save(dataObject);
		
		int dataObjectId = dataObject.getId();
		
		this.getDataAccessObject().delete(dataObject);
		
		Assert.assertTrue(dataObject.getIsDeleted());
		Assert.assertNull(this.getDataAccessObject().get(dataObjectId));
	}
	
	/**
	 * Asserts that the {@link GenericDAO#clear()} method works properly.
	 * This test must be implemented by its extending class since {@link GenericDAO#clear()}
	 * may be implemented specific to the implementation.
	 * To do so implement {@link GenericDAOTest#clearRepositorySucceeded()}
	 * 
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void testClear() throws Exception
	{
		Assert.assertTrue(this.clearRepositorySucceeded());
	}
	
	/**
	 * Asserts that data objects with virtual ids (ids <= 0) can be saved
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSaveNegativeId() throws Exception
	{
		DO dataObject = this.createDataObject(0, "testSaveNegativeId");
		
		dataObject.setId(-1);
		
		this.getDataAccessObject().save(dataObject);
		
		Assert.assertTrue(dataObject.getId() > 0);
	}

	/**
	 * Asserts that data objects with virtual ids (ids <= 0) can be deleted.
	 * Since the ids are still virtual these objects are not supposed to exist
	 * in the storage anyways. But no error should be thrown and nothing should
	 * be saved.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testDeleteNegativeId() throws Exception
	{
		DO dataObjectToSave = this.createDataObject(0, "testDeleteNegativeId");
		DO dataObjectNotToSave = this.createDataObject(1, "testDeleteNegativeId");
		
		dataObjectNotToSave.setId(-1);
		
		this.getDataAccessObject().delete(dataObjectNotToSave);		
		this.getDataAccessObject().save(dataObjectToSave);
		
		Assert.assertTrue(dataObjectToSave.getId() > 0);
		Assert.assertFalse(dataObjectToSave.getIsDeleted());

		Assert.assertTrue(dataObjectNotToSave.getId() <= 0);
		Assert.assertTrue(dataObjectNotToSave.getIsDeleted());
	}

	/**
	 * Asserts that the {@link GenericDataAccessObject#createDataObject()} method of a DAO implementation actually
	 * creates a data objects
	 * 
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void testCreateDataObject() throws Exception
	{
		DO dataObject = this.getDataAccessObject().createDataObject();
		
		Assert.assertNotNull(dataObject);
		Assert.assertTrue(dataObject.getId() < 1);
	}

	/**
	 * Asserts that the {@link GenericDataAccessObject#get(java.util.Collection)} method of a DAO implementation actually
	 * gets data objects
	 * 
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void testGetMany() throws Exception
	{
		int NUM_DATA_OBJECTS = 10;
		int[] dataObjectIds = new int[NUM_DATA_OBJECTS];

		for (int i=0; i<NUM_DATA_OBJECTS; i++)
		{
			DO dataObject = this.createDataObject(0, "testGetMany", i);
			
			this.getDataAccessObject().save(dataObject);
			
			dataObjectIds[i] = dataObject.getId();
		}

		Collection<DO> dataObjects = this.getDataAccessObject().get(dataObjectIds);
		Collection<Integer> expectedDataObjectIds = new ArrayList<Integer>(NUM_DATA_OBJECTS);

		for (int i=0; i<NUM_DATA_OBJECTS; i++) expectedDataObjectIds.add(dataObjectIds[i]);

		Assert.assertEquals(NUM_DATA_OBJECTS, dataObjects.size());

		Collection<Integer> actualDataObjectIds = new ArrayList<Integer>(dataObjects.size());
		
		for (DO dataObject : dataObjects)
		{
			Assert.assertTrue(dataObject.getId() > 0);
			actualDataObjectIds.add(dataObject.getId());
		}

		Assert.assertTrue(expectedDataObjectIds.containsAll(actualDataObjectIds));
		Assert.assertTrue(actualDataObjectIds.containsAll(expectedDataObjectIds));
		Assert.assertTrue(this.assertDefaultGetListenersBeforeGet(NUM_DATA_OBJECTS));
		Assert.assertTrue(this.assertDefaultGetListenersAfterGet(NUM_DATA_OBJECTS));
	}

	/**
	 * Asserts that the {@link GenericDataAccessObject#save(java.util.Collection)} method of a DAO implementation actually
	 * saves data objects
	 * 
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void testSaveMany() throws Exception
	{
		int NUM_DATA_OBJECTS = 10;
		Collection<DO> dataObjects = new ArrayList<DO>(NUM_DATA_OBJECTS);
		
		for (int i=0; i<NUM_DATA_OBJECTS; i++)
		{
			dataObjects.add(this.createDataObject(0, "testSaveMany", i));
		}

		this.getDataAccessObject().save(dataObjects);

		Assert.assertTrue(this.assertDefaultSaveListenersBeforeSave(NUM_DATA_OBJECTS));
		Assert.assertTrue(this.assertDefaultSaveListenersAfterSave(NUM_DATA_OBJECTS));

		for (DO dataObject : dataObjects)
		{
			Assert.assertTrue(dataObject.getId() > 0);
		}
	}

	/**
	 * Asserts that the {@link GenericDataAccessObject#delete(Collection)} method of a DAO implementation actually
	 * deletes data objects
	 * 
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void testDeleteMany() throws Exception
	{
		int NUM_DATA_OBJECTS = 10;
		int[] dataObjectIds = new int[NUM_DATA_OBJECTS];
		Collection<DO> dataObjects = new ArrayList<>(dataObjectIds.length);
		
		for (int i=0; i<NUM_DATA_OBJECTS; i++)
		{
			DO dataObject = this.createDataObject(0, "testDeleteMany", i);
			
			this.getDataAccessObject().save(dataObject);
			
			dataObjects.add(dataObject);
			dataObjectIds[i] = dataObject.getId();
		}

		this.getDataAccessObject().delete(dataObjects);

		Assert.assertTrue(this.assertDefaultDeleteListenersBeforeDelete(NUM_DATA_OBJECTS));
		Assert.assertTrue(this.assertDefaultDeleteListenersAfterDelete(NUM_DATA_OBJECTS));

		for (Integer dataObjectId : dataObjectIds)
		{
			Assert.assertNull(this.getDataAccessObject().get(dataObjectId));
		}
	}

	/**
	 * Asserts that the {@link GenericDataAccessObject#delete(int[])} method of a DAO implementation actually
	 * deletes data objects
	 * 
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void testDeleteManyIds() throws Exception
	{
		int NUM_DATA_OBJECTS = 10;
		int[] dataObjectIds = new int[NUM_DATA_OBJECTS];

		for (int i=0; i<NUM_DATA_OBJECTS; i++)
		{
			DO dataObject = this.createDataObject(0, "testDeleteMany", i);
			
			this.getDataAccessObject().save(dataObject);
			
			dataObjectIds[i] = dataObject.getId();
		}

		this.getDataAccessObject().delete(dataObjectIds);

		Assert.assertTrue(this.assertDefaultDeleteListenersBeforeDelete(NUM_DATA_OBJECTS));
		Assert.assertTrue(this.assertDefaultDeleteListenersAfterDelete(NUM_DATA_OBJECTS));

		for (Integer dataObjectId : dataObjectIds)
		{
			Assert.assertNull(this.getDataAccessObject().get(dataObjectId));
		}
	}
}
