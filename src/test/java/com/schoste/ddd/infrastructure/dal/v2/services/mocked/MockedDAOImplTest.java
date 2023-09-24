package com.schoste.ddd.infrastructure.dal.v2.services.mocked;

import java.util.Collection;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.schoste.ddd.infrastructure.dal.v2.models.MockedDO;
import com.schoste.ddd.infrastructure.dal.v2.services.GenericDAOTest;
import com.schoste.ddd.infrastructure.dal.v2.services.GenericDataAccessObject;
import com.schoste.ddd.infrastructure.dal.v2.services.listeners.DeleteListener;
import com.schoste.ddd.infrastructure.dal.v2.services.listeners.GetListener;
import com.schoste.ddd.infrastructure.dal.v2.services.listeners.MockedDeleteListener;
import com.schoste.ddd.infrastructure.dal.v2.services.listeners.MockedGetListener;
import com.schoste.ddd.infrastructure.dal.v2.services.listeners.MockedReloadListener;
import com.schoste.ddd.infrastructure.dal.v2.services.listeners.MockedSaveListener;
import com.schoste.ddd.infrastructure.dal.v2.services.listeners.ReloadListener;
import com.schoste.ddd.infrastructure.dal.v2.services.listeners.SaveListener;
import com.schoste.ddd.infrastructure.dal.v2.services.mocked.MockedDAOImpl;

/**
 * Test class of the {@link MockedDAOImpl} implementation
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 */
@ContextConfiguration(locations = { "file:src/test/resources/unittest-beans-v2.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class MockedDAOImplTest extends GenericDAOTest<MockedDO, MockedDAOImpl>
{
	@Autowired
	protected MockedDAOImpl mockedDAOImpl;

	/**
	 * Sets up existing data objects
	 */
	@Before
	public void createDataObjects() throws Exception
	{
		this.getDataAccessObject().clear();
		this.getDataAccessObject().save(this.createDataObject(0, "testGetAll1"));
		this.getDataAccessObject().save(this.createDataObject(0, "testGetAll2"));
		this.getDataAccessObject().save(this.createDataObject(0, "testGetAll3"));
		this.getDataAccessObject().save(this.createDataObject(0, "testGetAll4"));
		this.getDataAccessObject().save(this.createDataObject(0, "testGetAll5"));
		this.getDataAccessObject().save(this.createDataObject(0, "testGetAll6"));
		this.getDataAccessObject().save(this.createDataObject(0, "testGetAll7"));
		this.getDataAccessObject().save(this.createDataObject(0, "testGetAll8"));
		this.getDataAccessObject().save(this.createDataObject(0, "testGetAll9"));
		this.getDataAccessObject().save(this.createDataObject(0, "testGetAll10"));
		this.getDataAccessObject().save(this.createDataObject(1, "testSaveExisting"));
	}

	/**
	 * Registers default listeners for tests using
	 * {@link GenericDataAccessObject#registerOnSaveListener(SaveListener)}
	 *  
	 * @throws Exception re-throws every exception
	 */
	@Before
	public void registerListeners() throws Exception
	{
		GetListener<MockedDO> getListener = new MockedGetListener<MockedDO>();

		this.getDataAccessObject().registerOnGetListener(getListener);

		Assert.assertEquals(1, this.getDataAccessObject().getOnGetListeners().size());


		SaveListener<MockedDO> saveListener = new MockedSaveListener<MockedDO>();

		this.getDataAccessObject().registerOnSaveListener(saveListener);

		Assert.assertEquals(1, this.getDataAccessObject().getOnSaveListeners().size());


		DeleteListener<MockedDO> deleteListener = new MockedDeleteListener<MockedDO>();

		this.getDataAccessObject().registerOnDeleteListener(deleteListener);

		Assert.assertEquals(1, this.getDataAccessObject().getOnDeleteListeners().size());


		ReloadListener<MockedDO> reloadListener = new MockedReloadListener<MockedDO>();

		this.getDataAccessObject().registerOnReloadListener(reloadListener);

		Assert.assertEquals(1, this.getDataAccessObject().getOnReloadListeners().size());
	}

	/**
	 * Unregisters all listeners which might have been registered during a test using
	 * {@link GenericDataAccessObject#unregisterOnSaveListener(SaveListener)}
	 * 
	 * @throws Exception re-throws every exception
	 */
	@After
	public void clearListeners() throws Exception
	{
		Collection<GetListener<MockedDO>> getListeners = this.getDataAccessObject().getOnGetListeners();
		
		for (GetListener<MockedDO> getListener : getListeners) this.getDataAccessObject().unregisterOnGetListener(getListener);

		Assert.assertEquals(0, this.getDataAccessObject().getOnGetListeners().size());


		Collection<SaveListener<MockedDO>> saveListeners = this.getDataAccessObject().getOnSaveListeners();
		
		for (SaveListener<MockedDO> saveListener : saveListeners) this.getDataAccessObject().unregisterOnSaveListener(saveListener);

		Assert.assertEquals(0, this.getDataAccessObject().getOnSaveListeners().size());


		Collection<DeleteListener<MockedDO>> deleteListeners = this.getDataAccessObject().getOnDeleteListeners();
		
		for (DeleteListener<MockedDO> deleteListener : deleteListeners) this.getDataAccessObject().unregisterOnDeleteListener(deleteListener);

		Assert.assertEquals(0, this.getDataAccessObject().getOnDeleteListeners().size());


		Collection<ReloadListener<MockedDO>> reloadListeners = this.getDataAccessObject().getOnReloadListeners();
		
		for (ReloadListener<MockedDO> deleteListener : reloadListeners) this.getDataAccessObject().unregisterOnReloadListener(deleteListener);

		Assert.assertEquals(0, this.getDataAccessObject().getOnReloadListeners().size());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean assertDefaultGetListenersBeforeGet(int numOfexpectedDOs)
	{
		Collection<GetListener<MockedDO>> getListeners = this.getDataAccessObject().getOnGetListeners();
		
		for (GetListener<MockedDO> getListener : getListeners)
		{
			MockedGetListener<MockedDO> mockedGetListener = (MockedGetListener<MockedDO>) getListener;

			if ((mockedGetListener).getIdsReceivedBeforeGetting().size() != numOfexpectedDOs) return false;
		}

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean assertDefaultGetListenersAfterGet(int numOfexpectedDOs)
	{
		Collection<GetListener<MockedDO>> getListeners = this.getDataAccessObject().getOnGetListeners();
		
		for (GetListener<MockedDO> getListener : getListeners)
		{
			MockedGetListener<MockedDO> mockedGetListener = (MockedGetListener<MockedDO>) getListener;

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
		Collection<SaveListener<MockedDO>> saveListeners = this.getDataAccessObject().getOnSaveListeners();
		
		for (SaveListener<MockedDO> saveListener : saveListeners)
		{
			MockedSaveListener<MockedDO> mockedSaveListener = (MockedSaveListener<MockedDO>) saveListener;

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
		Collection<SaveListener<MockedDO>> saveListeners = this.getDataAccessObject().getOnSaveListeners();
		
		for (SaveListener<MockedDO> saveListener : saveListeners)
		{
			MockedSaveListener<MockedDO> mockedSaveListener = (MockedSaveListener<MockedDO>) saveListener;

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
		Collection<DeleteListener<MockedDO>> deleteListeners = this.getDataAccessObject().getOnDeleteListeners();
		
		for (DeleteListener<MockedDO> deleteListener : deleteListeners)
		{
			MockedDeleteListener<MockedDO> mockedSaveListener = (MockedDeleteListener<MockedDO>) deleteListener;

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
		Collection<DeleteListener<MockedDO>> deleteListeners = this.getDataAccessObject().getOnDeleteListeners();
		
		for (DeleteListener<MockedDO> deleteListener : deleteListeners)
		{
			MockedDeleteListener<MockedDO> mockedDeleteListener = (MockedDeleteListener<MockedDO>) deleteListener;

			if ((mockedDeleteListener).getDOsReceivedAfterDeleting().size() != numOfexpectedDOs) return false;
		}

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean assertDefaultReloadListenersAfterReload(int numOfexpectedDOs)
	{
		Collection<ReloadListener<MockedDO>> reloadListeners = this.getDataAccessObject().getOnReloadListeners();
		
		for (ReloadListener<MockedDO> reloadListener : reloadListeners)
		{
			MockedReloadListener<MockedDO> mockedReloadListener = (MockedReloadListener<MockedDO>) reloadListener;

			if ((mockedReloadListener).getDOsReceivedAfterReloading().size() != numOfexpectedDOs) return false;
		}

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected MockedDAOImpl getDataAccessObject() 
	{
		return this.mockedDAOImpl;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected MockedDO createDataObject(Object... parameters) 
	{
		MockedDO dataObject = this.mockedDAOImpl.createDataObject();
		
		if ((parameters == null) || (parameters.length < 1)) return dataObject;	
		if (parameters.length > 0) dataObject.setId((Integer) parameters[0]);
		if (parameters.length <= 1) return dataObject;
		if (!(parameters[1] instanceof String)) throw new IllegalArgumentException("parameters");
		
		String callingMethod = (String) parameters[1];
		
		switch (callingMethod)
		{
			// This test method requires two identical DOs
			case "testSaveExisting":
				dataObject.setId(1);
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
	protected void modifyDataObject(MockedDO dataObject, Object... parameters) 
	{
		if (dataObject == null) return;
		if ((parameters == null) || (parameters.length < 1)) return;	
		
		String exampleStringProperty = (String) parameters[0];
		
		dataObject.setExampleStringProperty(exampleStringProperty);		
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected MockedDO getExistingDataObject(int id)
	{
		try
		{
			return this.getDataAccessObject().get(id);			
		}
		catch (Exception ex)
		{
			ex.printStackTrace(System.err);
			
			return null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean clearRepositorySucceeded() throws Exception 
	{
		MockedDO dataObject = this.createDataObject(0, "clearRepositorySucceeded");
		
		this.getDataAccessObject().save(dataObject);
		this.getDataAccessObject().clear();
		
		return (this.getDataAccessObject().reloadAll().size() == 0);
	}
}
