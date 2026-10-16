package com.schoste.ddd.infrastructure.dal.v3.services.serialization;

import java.io.File;
import java.nio.file.Files;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.Assert;

import com.schoste.ddd.infrastructure.dal.v3.models.SerializationDO;
import com.schoste.ddd.infrastructure.dal.v3.services.listeners.DeleteListener;
import com.schoste.ddd.infrastructure.dal.v3.services.listeners.GetListener;
import com.schoste.ddd.infrastructure.dal.v3.services.listeners.MockedDeleteListener;
import com.schoste.ddd.infrastructure.dal.v3.services.listeners.MockedGetListener;
import com.schoste.ddd.infrastructure.dal.v3.services.listeners.MockedSaveListener;
import com.schoste.ddd.infrastructure.dal.v3.services.listeners.SaveListener;

/**
 * Test class of the SerializationDAOImpl implementation
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 */
@ContextConfiguration(locations = { "file:src/test/resources/unittest-beans.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class SerializationDAOImplTest extends GenericSerializationDAOTest<Long, SerializationDO, SerializationDAOImpl>
{
	@Autowired
	protected SerializationDAOImpl serializationDAOImpl;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean assertDefaultGetListenersBeforeGet(int numOfexpectedDOs)
	{
		Collection<GetListener<Long, SerializationDO>> getListeners = this.getDataAccessObject().getOnGetListeners();
		
		for (GetListener<Long, SerializationDO> getListener : getListeners)
		{
			MockedGetListener<SerializationDO> mockedGetListener = (MockedGetListener<SerializationDO>) getListener;

			if ((mockedGetListener).getIdsReceivedBeforeGetting().size() != numOfexpectedDOs) return false;
		}

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean assertDefaultGetListenersDOLsBeforeGet(int numOfexpectedDOLs)
	{
		Collection<GetListener<Long, SerializationDO>> getListeners = this.getDataAccessObject().getOnGetListeners();
		
		for (GetListener<Long, SerializationDO> getListener : getListeners)
		{
			MockedGetListener<SerializationDO> mockedGetListener = (MockedGetListener<SerializationDO>) getListener;

			if ((mockedGetListener).getDOLsReceivedBeforeGetting().size() != numOfexpectedDOLs) return false;
		}

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean assertDefaultGetListenersAfterGet(int numOfexpectedDOs)
	{
		Collection<GetListener<Long, SerializationDO>> getListeners = this.getDataAccessObject().getOnGetListeners();
		
		for (GetListener<Long, SerializationDO> getListener : getListeners)
		{
			MockedGetListener<SerializationDO> mockedGetListener = (MockedGetListener<SerializationDO>) getListener;

			if ((mockedGetListener).getDOsReceivedAfterGetting().size() != numOfexpectedDOs) return false;
		}

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean assertDefaultSaveListenersBeforeSave(int numOfexpectedDOs)
	{
		Collection<SaveListener<Long, SerializationDO>> saveListeners = this.getDataAccessObject().getOnSaveListeners();
		
		for (SaveListener<Long, SerializationDO> saveListener : saveListeners)
		{
			MockedSaveListener<SerializationDO> mockedSaveListener = (MockedSaveListener<SerializationDO>) saveListener;

			if ((mockedSaveListener).getDOsReceivedBeforeSaving().size() != numOfexpectedDOs) return false;
		}

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean assertDefaultSaveListenersAfterSave(int numOfexpectedDOs)
	{
		Collection<SaveListener<Long, SerializationDO>> saveListeners = this.getDataAccessObject().getOnSaveListeners();
		
		for (SaveListener<Long, SerializationDO> saveListener : saveListeners)
		{
			MockedSaveListener<SerializationDO> mockedSaveListener = (MockedSaveListener<SerializationDO>) saveListener;

			if ((mockedSaveListener).getDOsReceivedAfterSaving().size() != numOfexpectedDOs) return false;
		}

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean assertDefaultDeleteListenersBeforeDelete(int numOfexpectedDOs)
	{
		Collection<DeleteListener<Long, SerializationDO>> deleteListeners = this.getDataAccessObject().getOnDeleteListeners();
		
		for (DeleteListener<Long, SerializationDO> deleteListener : deleteListeners)
		{
			MockedDeleteListener<SerializationDO> mockedSaveListener = (MockedDeleteListener<SerializationDO>) deleteListener;

			if ((mockedSaveListener).getDOsReceivedBeforeDeleting().size() != numOfexpectedDOs) return false;
		}

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean assertDefaultDeleteListenersAfterDelete(int numOfexpectedDOs)
	{
		Collection<DeleteListener<Long, SerializationDO>> deleteListeners = this.getDataAccessObject().getOnDeleteListeners();
		
		for (DeleteListener<Long, SerializationDO> deleteListener : deleteListeners)
		{
			MockedDeleteListener<SerializationDO> mockedDeleteListener = (MockedDeleteListener<SerializationDO>) deleteListener;

			if ((mockedDeleteListener).getDOsReceivedAfterDeleting().size() != numOfexpectedDOs) return false;
		}

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Long getNotExistingId()
	{
		return 9999L;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	protected SerializationDAOImpl getDataAccessObject() 
	{
		return this.serializationDAOImpl;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected SerializationDO createDataObject(Object... parameters) throws Exception
	{
		SerializationDO dataObject = this.serializationDAOImpl.createDataObject();
		
		if ((parameters == null) || (parameters.length < 1)) return dataObject;	
		if (parameters.length > 0) dataObject.setId((Long) parameters[0]);
		if (parameters.length <= 1) return dataObject;
		if (!(parameters[1] instanceof String)) throw new IllegalArgumentException("parameters");
		
		String callingMethod = (String) parameters[1];
		
		switch (callingMethod)
		{
			// This test method requires two identical DOs
			case "testSaveExisting":
				dataObject.setId(1L);
				dataObject.setExampleStringProperty(String.format("%s%s", parameters[0], parameters[1]));
				break;
				
			default: dataObject.setExampleStringProperty((String) parameters[1]);
		}
		
		return dataObject;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void modifyDataObject(SerializationDO dataObject, Object... parameters) 
	{
		if ((parameters == null) || (parameters.length < 1)) return;
		
		dataObject.setExampleStringProperty((String) parameters[0]);
	}

	/**
	 * Executed before every test is executed.
	 * Clears the DAO and stores new data objects into it.
	 * 
	 * @throws Exception re-throws every exception
	 */
	@Before
	public void initializeTest() throws Exception
	{
		this.clearAndSetUpExistingDataObjects();
	}
	
	/**
	 * Asserts that the DAO can be created with nested paths
	 * 
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void testConstructorWithNestedPaths() throws Exception
	{
		String daoPath = String.format("%s%s%s%s%s", System.getProperty("java.io.tmpdir"), File.separator, System.nanoTime(), File.separator, "TEST");
		SerializationDAOImpl dao = new SerializationDAOImpl(daoPath);

		Assert.notNull(dao, "");

		Files.delete(dao.getStoragePath());
		Files.delete(dao.getStoragePath().getParent());
	}
}
