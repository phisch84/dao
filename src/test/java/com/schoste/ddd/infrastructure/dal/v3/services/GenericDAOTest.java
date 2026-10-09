package com.schoste.ddd.infrastructure.dal.v3.services;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;

import com.schoste.ddd.infrastructure.dal.v3.models.GenericDataObject;

/**
 * Generic test class to test implementations of the GenericSerializationDAO class.
 * This class provides standard test methods.
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 * @param <ID> The class of the data object's identifier
 * @param <DO> The class of the data object
 * @param <DAO> The class of the data access object
 */
public abstract class GenericDAOTest <ID extends Serializable, DO extends GenericDataObject<ID>, DAO extends GenericDataAccessObject<ID, DO>>
{
	/**
	 * Gets the instance to the data access object used for testing
	 * 
	 * @return instance to the data access object
	 */
	protected abstract DAO getDataAccessObject();
	
	/**
	 * Creates a new data object used in testing.
	 * If {@link GenericDataAccessObject#createDataObject(Object...)} is used or not depends on the actual implementation
	 * 
	 * @param parameters controls which data access object is returned. It depends on the actual implementation
	 * @return a new data object
	 * @throws Exception re-throws every exception
	 */
	protected abstract DO createDataObject(Object ...parameters) throws Exception;

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
	 * Gets a (virtual or actual) identifier for which no data object will be found
	 * 
	 * @return an id of a data object that does not exist in the storage
	 */
	protected abstract ID getNotExistingId();

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
	 * Asserts that all registered get listeners contain a given number of data object loaders before getting
	 * 
	 * @param numOfexpectedDOLs the number of expected data object loaders (DOLs)
	 * @return true if all listeners contain the exact number of DOLs, false otherwise
	 */
	protected abstract boolean assertDefaultGetListenersDOLsBeforeGet(int numOfexpectedDOLs);

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
		
		Assert.assertNotEquals((Integer) 0, dataObject.getId());
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
		ID id = dataObject.getId();
		long modTs = dataObject.getModifiedTimeStamp();
		
		Thread.sleep(100); // required to reliably test the change of the mod ts
		
		this.modifyDataObject(dataObject, "testSaveExisting");
		this.getDataAccessObject().save(dataObject);
		
		Assert.assertEquals((Integer) id, dataObject.getId());
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
		
		ID newObjectId = newObject.getId();		
		DO dataObject = this.getDataAccessObject().get(newObjectId);
		
		Assert.assertNotNull(dataObject);
		Assert.assertNotEquals((Integer) 0, dataObject.getId());
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
		DO dataObject = this.getDataAccessObject().get(this.getNotExistingId());
		
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
	 * Asserts that the getAll() method of a DAO implementation returns all existing data objects
	 * 
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void testGetAll() throws Exception
	{
		int numOfDataObjects = 10;
		int numOfExpectedDOLoaders = 1;

		Collection<DO> dataObjects = this.getDataAccessObject().getAll().toList();

		Assert.assertTrue(dataObjects.size() >= numOfDataObjects);
		Assert.assertTrue(this.assertDefaultGetListenersBeforeGet(numOfExpectedDOLoaders));
		Assert.assertTrue(this.assertDefaultGetListenersAfterGet(numOfDataObjects));

		// New IDs can be anything but null. Since they come from the storage they must not be virtual either
		for (DO dataObject : dataObjects)
		{
			Assert.assertNotNull(dataObject.getId());
			Assert.assertFalse(this.getDataAccessObject().isVirtualId(dataObject.getId()));
		} 
	}
	
	/**
	 * Asserts that the getAll() method of a DAO implementation reloads
	 * data objects only if they were changed or added meanwhile
	 * 
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void testGetAllOldAndDeleted() throws Exception
	{
		int numOfDataObjects = 10;
		int numOfExpectedDOLoaders = 2;
		int numOfExpectedDOsInListener = 10*2 +3; // 2 times getAll() + 3 times get() in getExistingDataObject()

		// this call is supposed to have no effect on the DAO's internal time stamp keeping
		@SuppressWarnings("unused")
		Collection<DO> dataObjects = this.getDataAccessObject().getAll().toList();
		
		DO dataObject1 = this.getExistingDataObject(2);
		DO dataObject2 = this.getExistingDataObject(numOfDataObjects-1);
		DO dataObject3 = this.getExistingDataObject(1);
		   dataObject3.setIsDeleted(true);
		
		Thread.sleep(100);
		
		this.modifyDataObject(dataObject1, "testGetAllOldAndDeleted");
		this.modifyDataObject(dataObject2, "testGetAllOldAndDeleted");

		// save the modified objects so their modification date time stamps are updated
		this.getDataAccessObject().save(dataObject1);
		this.getDataAccessObject().save(dataObject2);

		// save the DO that was flagged deleted instead of calling DAO's delete() as this function might actually remove it
		this.getDataAccessObject().save(dataObject3);
		
		Collection<DO> dataObjects_ = this.getDataAccessObject().getAll().toList();
		
		Assert.assertEquals(numOfDataObjects, dataObjects_.size());
		Assert.assertTrue(this.assertDefaultGetListenersDOLsBeforeGet(numOfExpectedDOLoaders));
		Assert.assertTrue(this.assertDefaultGetListenersAfterGet(numOfExpectedDOsInListener));
	}
	
	/**
	 * Asserts that the getRecent() method of a DAO implementation reads all
	 * existing data objects from the storage and regards the deleted flag as well as time stamps
	 * In this test the expected data objects are modified and persisted.
	 * 
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void testGetRecentModified() throws Exception
	{
		int numOfDataObjects = 10;
		int numOfExpectedDOs = 2; // 3 modified, but 1 deleted = 2 expected
		int numOfExpectedAfterGetCalls = 15; // 2*getRecent() + 3*getExistingDataObject() [will call DAO's get()]
		int numOfExpectedDOLoaders = 2;

		// reset the DAO's last modification time stamp and assert it was reset correctly
		this.getDataAccessObject().setLastModifiedTimeStamp(Long.MIN_VALUE);
		long daoModTS = this.getDataAccessObject().getLastModifiedTimeStamp();

		Assert.assertEquals(Long.MIN_VALUE, daoModTS);

		// call getRecent() to update the DAO's internal last modification time stamp 
		Collection<DO> dataObjects = this.getDataAccessObject().getRecent(true).toList();

		// assert the time stamp was updated as expected
		Assert.assertTrue(this.getDataAccessObject().getLastModifiedTimeStamp() > Long.MIN_VALUE);
		Assert.assertTrue(this.getDataAccessObject().getLastModifiedTimeStamp() <= System.currentTimeMillis());

		// if these assertations fail, the test initialization went wrong
		for (DO dataObj : dataObjects)
		{
			Assert.assertFalse(dataObj.getIsDeleted());
			Assert.assertTrue(dataObj.getModifiedTimeStamp() >= daoModTS);
			Assert.assertTrue(dataObj.getModifiedTimeStamp() <= System.currentTimeMillis());
		}

		Assert.assertEquals(numOfDataObjects, dataObjects.size()); // 10 DOs are expected to be already present

		// store the modification time stamps of the DOs to ensure later they were not changed by accident
		HashMap<ID, Long> dataObjModTSes = new HashMap<>(dataObjects.size());

		for (DO dataObject : dataObjects) dataObjModTSes.put(dataObject.getId(), dataObject.getModifiedTimeStamp());

		DO dataObject1 = this.getExistingDataObject(2);
		DO dataObject2 = this.getExistingDataObject(numOfDataObjects-1);
		DO dataObject3 = this.getExistingDataObject(1);
		   dataObject3.setIsDeleted(true);

		this.modifyDataObject(dataObject1, "testGetRecentModified-1");
		this.modifyDataObject(dataObject2, "testGetRecentModified-2");
		this.modifyDataObject(dataObject3, "testGetRecentModified-3");

		Thread.sleep(100);

		long tsBeforeSave = System.currentTimeMillis();

		// save the modified objects so their modification date time stamps are updated
		this.getDataAccessObject().save(dataObject1);
		this.getDataAccessObject().save(dataObject2);

		// save the DO that was flagged deleted instead of calling DAO's delete() as this function might actually remove it
		this.getDataAccessObject().save(dataObject3);

		Thread.sleep(100);

		// make sure only the modified data object match criteria to get filtered in
		for (DO dataObj : dataObjects)
		{
			if (dataObj.equals(dataObject1))
			{
				Assert.assertFalse(dataObj.getIsDeleted());
				Assert.assertTrue(dataObj.getModifiedTimeStamp() >= tsBeforeSave);

				continue;
			}

			if (dataObj.equals(dataObject2))
			{
				Assert.assertFalse(dataObj.getIsDeleted());
				Assert.assertTrue(dataObj.getModifiedTimeStamp() >= tsBeforeSave);

				continue;
			}

			if (dataObj.equals(dataObject3))
			{
				Assert.assertTrue(dataObj.getIsDeleted());
				Assert.assertTrue(dataObj.getModifiedTimeStamp() >= tsBeforeSave);

				continue;
			}

			Assert.assertFalse(dataObj.getIsDeleted());
			Assert.assertTrue(dataObj.getModifiedTimeStamp() < tsBeforeSave);
			Assert.assertEquals(dataObjModTSes.get(dataObj.getId()).longValue(), dataObj.getModifiedTimeStamp());
		}

		daoModTS = this.getDataAccessObject().getLastModifiedTimeStamp();
		dataObjects = this.getDataAccessObject().getRecent().toList();

		// assert the time stamp was updated as expected
		Assert.assertTrue(this.getDataAccessObject().getLastModifiedTimeStamp() >= daoModTS);
		Assert.assertTrue(this.getDataAccessObject().getLastModifiedTimeStamp() <= System.currentTimeMillis());

		for (DO dataObj : dataObjects)
		{
			Assert.assertFalse(dataObj.getIsDeleted());

			if (dataObj.equals(dataObject1) || dataObj.equals(dataObject2) || dataObj.equals(dataObject3))
			{
				Assert.assertTrue(dataObj.getModifiedTimeStamp() >= daoModTS);
			}
			else
			{
				Assert.assertTrue(dataObj.getModifiedTimeStamp() < daoModTS);
				Assert.assertEquals(dataObjModTSes.get(dataObj.getId()).longValue(), dataObj.getModifiedTimeStamp());
			}
		}

		Assert.assertEquals(numOfExpectedDOs, dataObjects.size());
		Assert.assertTrue(this.assertDefaultGetListenersDOLsBeforeGet(numOfExpectedDOLoaders));
		Assert.assertTrue(this.assertDefaultGetListenersAfterGet(numOfExpectedAfterGetCalls));
	}

	/**
	 * Asserts that the getRecent() method of a DAO implementation reads all
	 * existing data objects from the storage regardless of the deleted flag,
	 * but with regards to the time stamps.
	 * In this test the expected data objects are modified and persisted.
	 * 
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void testGetRecentModifiedAndDeleted() throws Exception
	{
		int numOfDataObjects = 10;
		int numOfExpectedDOs = 3; // 3 modified, and 1 deleted (which is expected anyways)
		int numOfExpectedAfterGetCalls = 16; // 2*getRecent() + 3*getExistingDataObject() [will gall DAO's get()] +deleted
		int numOfExpectedDOLoaders = 2;

		// call getRecent() to update the DAO's internal last modification time stamp 
		this.getDataAccessObject().getRecent().toList();

		DO dataObject1 = this.getExistingDataObject(2);
		DO dataObject2 = this.getExistingDataObject(numOfDataObjects-1);
		DO dataObject3 = this.getExistingDataObject(1);
		   dataObject3.setIsDeleted(true);
				
		this.modifyDataObject(dataObject1, "testGetRecentModified-1");
		this.modifyDataObject(dataObject2, "testGetRecentModified-2");
		this.modifyDataObject(dataObject3, "testGetRecentModified-3");

		Thread.sleep(100);

		// save the modified objects so their modification date time stamps are updated
		this.getDataAccessObject().save(dataObject1);
		this.getDataAccessObject().save(dataObject2);

		// save the DO that was flagged deleted instead of calling DAO's delete() as this function might actually remove it
		this.getDataAccessObject().save(dataObject3);

		Thread.sleep(100);

		long daoModTS = this.getDataAccessObject().getLastModifiedTimeStamp();
		Collection<DO> dataObjects = this.getDataAccessObject().getRecent(true).toList();
		
		for (DO dataObj : dataObjects)
		{
			Assert.assertTrue(dataObj.getModifiedTimeStamp() >= daoModTS);
		}

		Assert.assertTrue(this.getDataAccessObject().getLastModifiedTimeStamp() >= daoModTS);
		Assert.assertEquals(numOfExpectedDOs, dataObjects.size());
		Assert.assertTrue(this.assertDefaultGetListenersDOLsBeforeGet(numOfExpectedDOLoaders));
		Assert.assertTrue(this.assertDefaultGetListenersAfterGet(numOfExpectedAfterGetCalls));
	}

	/**
	 * Asserts that the getRecent() method of a DAO implementation reads all
	 * existing data objects from the storage and regards the deleted flag as well as time stamps.
	 * In this test the expected data objects are created and persisted.
	 * 
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void testGetRecentCreated() throws Exception
	{
		int numOfDataObjects = 5;
		int numOfExpectedDOLoaders = 2;

		// call getRecent() to update the DAO's internal last modification time stamp 
		this.getDataAccessObject().getRecent().toList();

		Thread.sleep(100);

		for (int i=0; i<numOfDataObjects; i++)
		{
			DO dataObject = this.createDataObject(-1, "testGetRecentCreated", i);
			
			this.getDataAccessObject().save(dataObject);
		}

		// a data object that is flagged as deleted by still exists in the storage. It should not be returned by getRecent()
		DO deletedDataObject = this.createDataObject(-1, "testGetRecentCreated", (numOfDataObjects+1));
		   deletedDataObject.setIsDeleted(true);

		this.getDataAccessObject().save(deletedDataObject);

		Thread.sleep(100);

		long daoModTS = this.getDataAccessObject().getLastModifiedTimeStamp();
		Collection<DO> dataObjects = this.getDataAccessObject().getRecent().toList();

		for (DO dataObj : dataObjects)
		{
			Assert.assertFalse(dataObj.getIsDeleted());
			Assert.assertTrue(dataObj.getModifiedTimeStamp() >= daoModTS);
		}

		Assert.assertTrue(this.getDataAccessObject().getLastModifiedTimeStamp() >= daoModTS);
		Assert.assertTrue(this.assertDefaultGetListenersDOLsBeforeGet(numOfExpectedDOLoaders));
		Assert.assertEquals(numOfDataObjects, dataObjects.size());
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
		
		ID dataObjectId = dataObject.getId();
		
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
	 * Asserts that data objects with virtual ids can be saved
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSaveNegativeId() throws Exception
	{
		DO dataObject = this.createDataObject(-1, "testSaveNegativeId");
		
		this.getDataAccessObject().save(dataObject);
		
		Assert.assertNotNull(dataObject.getId());
		Assert.assertFalse(this.getDataAccessObject().isVirtualId(dataObject.getId()));
	}

	/**
	 * Asserts that data objects with virtual ids can be deleted.
	 * Since the ids are still virtual these objects are not supposed to exist
	 * in the storage anyways. But no error should be thrown and nothing should
	 * be saved.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testDeleteNegativeId() throws Exception
	{
		DO dataObjectToSave = this.createDataObject(-1, "testDeleteNegativeId");
		DO dataObjectNotToSave = this.createDataObject(-1, "testDeleteNegativeId");
		
		this.getDataAccessObject().delete(dataObjectNotToSave);		
		this.getDataAccessObject().save(dataObjectToSave);

		Assert.assertNotNull(dataObjectToSave.getId());
		Assert.assertFalse(this.getDataAccessObject().isVirtualId(dataObjectToSave.getId()));
		Assert.assertFalse(dataObjectToSave.getIsDeleted());

		Assert.assertNotNull(dataObjectNotToSave.getId());
		Assert.assertTrue(this.getDataAccessObject().isVirtualId(dataObjectNotToSave.getId()));
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
		Assert.assertNotNull(dataObject.getId());
		Assert.assertTrue(this.getDataAccessObject().isVirtualId(dataObject.getId()));
	}

	/**
	 * Asserts that the {@link GenericDataAccessObject#get(java.util.Collection)} method of a DAO implementation actually
	 * gets all data objects which have one of the provided ids
	 * 
	 * @throws Exception re-throws every exception
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testGetMany() throws Exception
	{
		int NUM_DATA_OBJECTS = 10;
		int nomOfExpectedDOLs = 1;
		ID dataObjId = this.getDataAccessObject().createDataObject().getId();
		ID[] dataObjectIds = (ID[]) Array.newInstance(dataObjId.getClass(), NUM_DATA_OBJECTS);

		for (int i=0; i<NUM_DATA_OBJECTS; i++)
		{
			DO dataObject = this.createDataObject(-1, "testGetMany", i);
			
			this.getDataAccessObject().save(dataObject);
			
			dataObjectIds[i] = dataObject.getId();
		}

		Collection<DO> dataObjects = this.getDataAccessObject().get(dataObjectIds).toList();
		Collection<ID> expectedDataObjectIds = new ArrayList<ID>(NUM_DATA_OBJECTS);

		for (int i=0; i<NUM_DATA_OBJECTS; i++) expectedDataObjectIds.add(dataObjectIds[i]);

		Assert.assertEquals(NUM_DATA_OBJECTS, dataObjects.size());

		Collection<ID> actualDataObjectIds = new ArrayList<ID>(dataObjects.size());
		
		for (DO dataObject : dataObjects)
		{
			Assert.assertNotNull(dataObject.getId());
			Assert.assertFalse(this.getDataAccessObject().isVirtualId(dataObject.getId()));
			actualDataObjectIds.add(dataObject.getId());
		}

		Assert.assertTrue(expectedDataObjectIds.containsAll(actualDataObjectIds));
		Assert.assertTrue(actualDataObjectIds.containsAll(expectedDataObjectIds));
		Assert.assertTrue(this.assertDefaultGetListenersDOLsBeforeGet(nomOfExpectedDOLs));
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
		Assert.assertNotNull(dataObject.getId());
		Assert.assertFalse(this.getDataAccessObject().isVirtualId(dataObject.getId()));
		}
	}

	/**
	 * Asserts that the {@link GenericDataAccessObject#delete(Collection)} method of a DAO implementation actually
	 * deletes data objects
	 * 
	 * @throws Exception re-throws every exception
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testDeleteMany() throws Exception
	{
		int NUM_DATA_OBJECTS = 10;
		ID dataObjId = this.getDataAccessObject().createDataObject().getId();
		ID[] dataObjectIds = (ID[]) Array.newInstance(dataObjId.getClass(), NUM_DATA_OBJECTS);
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

		for (ID dataObjectId : dataObjectIds)
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
	@SuppressWarnings("unchecked")
	@Test
	public void testDeleteManyIds() throws Exception
	{
		int NUM_DATA_OBJECTS = 10;
		ID dataObjId = this.getDataAccessObject().createDataObject().getId();
		ID[] dataObjectIds = (ID[]) Array.newInstance(dataObjId.getClass(), NUM_DATA_OBJECTS);

		for (int i=0; i<NUM_DATA_OBJECTS; i++)
		{
			DO dataObject = this.createDataObject(0, "testDeleteManyIds", i);
			
			this.getDataAccessObject().save(dataObject);
			
			dataObjectIds[i] = dataObject.getId();
		}

		this.getDataAccessObject().delete(dataObjectIds);

		Assert.assertTrue(this.assertDefaultDeleteListenersBeforeDelete(NUM_DATA_OBJECTS));
		Assert.assertTrue(this.assertDefaultDeleteListenersAfterDelete(NUM_DATA_OBJECTS));

		for (ID dataObjectId : dataObjectIds)
		{
			Assert.assertNull(this.getDataAccessObject().get(dataObjectId));
		}
	}

	/**
	 * Asserts that the {@link GenericDataAccessObject#getAll(java.util.function.Predicate)} method of a DAO implementation
	 * actually returns a stream with all expected objects since no predicate (null) was provided
	 * 
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void testGetAllStreamed() throws Exception
	{
		try (Stream<DO> doStream = this.getDataAccessObject().getAll())
		{
			// New IDs can be anything greater than 0
			doStream.forEach(dataObject 
				-> { 
		Assert.assertNotNull(dataObject.getId());
		Assert.assertFalse(this.getDataAccessObject().isVirtualId(dataObject.getId()));
				});
		}
	}

	/**
	 * Asserts that data object newly created via {@link GenericDataAccessObject#createDataObject(Object...)}
	 * will have an identifier (see {@link GenericDataObject#getId()}) for that:
	 * 1. no other object was created with the same id, and
	 * 2. {@link GenericDataAccessObject#isVirtualId(java.io.Serializable) returns true
	 * 
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void testCreateDataObjectAndVirtualId() throws Exception
	{
		int NUM_DATA_OBJECTS = 10;
		Collection<ID> dataObjectIds = new ArrayList<>(NUM_DATA_OBJECTS);

		for (int i=0; i<NUM_DATA_OBJECTS; i++)
		{
			DO dataObject = this.getDataAccessObject().createDataObject(0, "testDeleteManyIds", i);

			Assert.assertNotNull(dataObject);
			Assert.assertTrue(this.getDataAccessObject().isVirtualId(dataObject.getId()));
			Assert.assertFalse(dataObjectIds.contains(dataObject.getId()));

			dataObjectIds.add(dataObject.getId());
		}
	}

	/**
	 * Asserts that the save() method of a DAO implementation updates a data object's identifier
	 * so that the data access object recognizes it no longer as virtual
	 * 
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void testSaveAndVirtualId() throws Exception
	{
		DO dataObj = this.getDataAccessObject().createDataObject();

		this.getDataAccessObject().save(dataObj);

		Assert.assertNotNull(dataObj.getId());
		Assert.assertFalse(this.getDataAccessObject().isVirtualId(dataObj.getId()));
	}

}