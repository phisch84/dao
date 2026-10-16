package com.schoste.ddd.infrastructure.dal.v3.services.mocked;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.schoste.ddd.infrastructure.dal.v3.exceptions.BrokenRecordException;
import com.schoste.ddd.infrastructure.dal.v3.exceptions.DALException;
import com.schoste.ddd.infrastructure.dal.v3.exceptions.DALRuntimeException;
import com.schoste.ddd.infrastructure.dal.v3.models.MockedDO;
import com.schoste.ddd.infrastructure.dal.v3.services.GenericDAOTest;
import com.schoste.ddd.infrastructure.dal.v3.services.GenericDataAccessObject;
import com.schoste.ddd.infrastructure.dal.v3.services.listeners.DeleteListener;
import com.schoste.ddd.infrastructure.dal.v3.services.listeners.GetListener;
import com.schoste.ddd.infrastructure.dal.v3.services.listeners.MockedDeleteListener;
import com.schoste.ddd.infrastructure.dal.v3.services.listeners.MockedGetListener;
import com.schoste.ddd.infrastructure.dal.v3.services.listeners.MockedSaveListener;
import com.schoste.ddd.infrastructure.dal.v3.services.listeners.SaveListener;

/**
 * Test class of the {@link MockedDAOImpl} implementation
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 */
@ContextConfiguration(locations = { "file:src/test/resources/unittest-beans-v3.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class MockedDAOImplTest extends GenericDAOTest<Long, MockedDO, MockedDAOImpl>
{
	@Autowired
	protected MockedDAOImpl mockedDAOImpl;

	/**
	 * Sets up existing data objects
	 * 
	 * @throws Exception re-throws every exception
	 */
	@Before
	public void createDataObjects() throws Exception
	{
		this.getDataAccessObject().clear();
		this.getDataAccessObject().save(this.createDataObject(0L, "@Before01"));
		this.getDataAccessObject().save(this.createDataObject(0L, "@Before02"));
		this.getDataAccessObject().save(this.createDataObject(0L, "@Before03"));
		this.getDataAccessObject().save(this.createDataObject(0L, "@Before04"));
		this.getDataAccessObject().save(this.createDataObject(0L, "@Before05"));
		this.getDataAccessObject().save(this.createDataObject(0L, "@Before06"));
		this.getDataAccessObject().save(this.createDataObject(0L, "@Before07"));
		this.getDataAccessObject().save(this.createDataObject(0L, "@Before08"));
		this.getDataAccessObject().save(this.createDataObject(0L, "@Before09"));
		this.getDataAccessObject().save(this.createDataObject(0L, "@Before10"));
		this.getDataAccessObject().save(this.createDataObject(1L, "testSaveExisting"));
	}

	/**
	 * Registers default listeners for tests using
	 * {@link GenericDataAccessObject#registerOnGetListener(GetListener)},
	 * {@link GenericDataAccessObject#registerOnSaveListener(SaveListener)} and
	 * {@link GenericDataAccessObject#registerOnDeleteListener(DeleteListener)}
	 *  
	 * @throws Exception re-throws every exception
	 */
	@Before
	public void registerListeners() throws Exception
	{
		GetListener<Long, MockedDO> getListener = new MockedGetListener<MockedDO>();

		this.getDataAccessObject().registerOnGetListener(getListener);

		Assert.assertEquals(1, this.getDataAccessObject().getOnGetListeners().size());


		SaveListener<Long, MockedDO> saveListener = new MockedSaveListener<MockedDO>();

		this.getDataAccessObject().registerOnSaveListener(saveListener);

		Assert.assertEquals(1, this.getDataAccessObject().getOnSaveListeners().size());


		DeleteListener<Long, MockedDO> deleteListener = new MockedDeleteListener<MockedDO>();

		this.getDataAccessObject().registerOnDeleteListener(deleteListener);

		Assert.assertEquals(1, this.getDataAccessObject().getOnDeleteListeners().size());
	}

	/**
	 * Unregisters all listeners which might have been registered during a test using
	 * {@link GenericDataAccessObject#unregisterOnGetListener(GetListener)},
	 * {@link GenericDataAccessObject#unregisterOnSaveListener(SaveListener)} and
	 * {@link GenericDataAccessObject#unregisterOnDeleteListener(DeleteListener)}
	 * 
	 * @throws Exception re-throws every exception
	 */
	@After
	public void clearListeners() throws Exception
	{
		Collection<GetListener<Long, MockedDO>> getListeners = this.getDataAccessObject().getOnGetListeners();
		
		for (GetListener<Long, MockedDO> getListener : getListeners) this.getDataAccessObject().unregisterOnGetListener(getListener);

		Assert.assertEquals(0, this.getDataAccessObject().getOnGetListeners().size());


		Collection<SaveListener<Long, MockedDO>> saveListeners = this.getDataAccessObject().getOnSaveListeners();
		
		for (SaveListener<Long, MockedDO> saveListener : saveListeners) this.getDataAccessObject().unregisterOnSaveListener(saveListener);

		Assert.assertEquals(0, this.getDataAccessObject().getOnSaveListeners().size());


		Collection<DeleteListener<Long, MockedDO>> deleteListeners = this.getDataAccessObject().getOnDeleteListeners();
		
		for (DeleteListener<Long, MockedDO> deleteListener : deleteListeners) this.getDataAccessObject().unregisterOnDeleteListener(deleteListener);

		Assert.assertEquals(0, this.getDataAccessObject().getOnDeleteListeners().size());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean assertDefaultGetListenersBeforeGet(int numOfexpectedDOs)
	{
		Collection<GetListener<Long, MockedDO>> getListeners = this.getDataAccessObject().getOnGetListeners();
		
		for (GetListener<Long, MockedDO> getListener : getListeners)
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
	protected boolean assertDefaultGetListenersDOLsBeforeGet(int numOfexpectedDOLs)
	{
		Collection<GetListener<Long, MockedDO>> getListeners = this.getDataAccessObject().getOnGetListeners();
		
		for (GetListener<Long, MockedDO> getListener : getListeners)
		{
			MockedGetListener<MockedDO> mockedGetListener = (MockedGetListener<MockedDO>) getListener;

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
		Collection<GetListener<Long, MockedDO>> getListeners = this.getDataAccessObject().getOnGetListeners();
		
		for (GetListener<Long, MockedDO> getListener : getListeners)
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
		Collection<SaveListener<Long, MockedDO>> saveListeners = this.getDataAccessObject().getOnSaveListeners();
		
		for (SaveListener<Long, MockedDO> saveListener : saveListeners)
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
		Collection<SaveListener<Long, MockedDO>> saveListeners = this.getDataAccessObject().getOnSaveListeners();
		
		for (SaveListener<Long, MockedDO> saveListener : saveListeners)
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
		Collection<DeleteListener<Long, MockedDO>> deleteListeners = this.getDataAccessObject().getOnDeleteListeners();
		
		for (DeleteListener<Long, MockedDO> deleteListener : deleteListeners)
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
		Collection<DeleteListener<Long, MockedDO>> deleteListeners = this.getDataAccessObject().getOnDeleteListeners();
		
		for (DeleteListener<Long, MockedDO> deleteListener : deleteListeners)
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
	protected MockedDAOImpl getDataAccessObject() 
	{
		return this.mockedDAOImpl;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected MockedDO createDataObject(Object... parameters) throws Exception
	{
		MockedDO dataObject = this.mockedDAOImpl.createDataObject();
		
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
	protected Long getNotExistingId()
	{
		return 9999L;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected MockedDO getExistingDataObject(long id)
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
		MockedDO dataObject = this.createDataObject(0L, "clearRepositorySucceeded");
		
		this.getDataAccessObject().save(dataObject);
		this.getDataAccessObject().clear();
		
		return (this.getDataAccessObject().getRecent().collect(Collectors.toList()).size() == 0);
	}

	/**
	 * Asserts that a failure while loading data objects is not swallowed, but ends the stream
	 * with a {@link DALRuntimeException} which wraps the original exception in a {@link DALException}
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void testGetAllFailsOnLoadingError() throws Exception
	{
		IllegalStateException loadingError = new IllegalStateException("loading error");

		this.getDataAccessObject().registerOnGetListener(new GetListener<Long, MockedDO>()
		{
			@Override
			public MockedDO onAfterGet(MockedDO dataObject)
			{
				throw loadingError;
			}
		});

		try (Stream<MockedDO> doStream = this.getDataAccessObject().getAll())
		{
			doStream.forEach(dataObject -> Assert.fail("no data object expected"));

			Assert.fail("DALRuntimeException expected");
		}
		catch (DALRuntimeException e)
		{
			Assert.assertFalse(e.getCause() instanceof BrokenRecordException);
			Assert.assertSame(loadingError, e.getCause().getCause());
		}
	}

	/**
	 * Asserts that a record which can't be loaded causes a {@link BrokenRecordException},
	 * after which the remaining data objects can still be loaded
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void testGetAllReportsBrokenRecordAndContinues() throws Exception
	{
		Long brokenId = 3L;
		IOException readError = new IOException("broken record");
		MockedDAOImpl dao = new MockedDAOImpl()
		{
			@Override
			protected synchronized MockedDO doGet(Long id) throws Exception
			{
				if (brokenId.equals(id)) throw readError;

				return super.doGet(id);
			}
		};

		try (Stream<MockedDO> doStream = this.getDataAccessObject().getAll())
		{
			dao.save(doStream.collect(Collectors.toList()));
		}

		Collection<MockedDO> loaded = new ArrayList<>();
		Collection<BrokenRecordException> broken = new ArrayList<>();

		try (Stream<MockedDO> doStream = dao.getAll())
		{
			Iterator<MockedDO> doIterator = doStream.iterator();

			while (true)
			{
				try
				{
					if (!doIterator.hasNext()) break;

					loaded.add(doIterator.next());
				}
				catch (DALRuntimeException e)
				{
					broken.add((BrokenRecordException) e.getCause());
				}
			}
		}

		Assert.assertEquals(dao.getDataObjectIds().size() - 1, loaded.size());
		Assert.assertEquals(1, broken.size());
		Assert.assertEquals(brokenId, broken.iterator().next().getSourceRecord());
		Assert.assertSame(readError, broken.iterator().next().getCause());
	}

	/**
	 * Asserts that exceptions thrown by the consumer of a stream of data objects are re-thrown as they are
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void testGetAllPassesConsumerErrorsThrough() throws Exception
	{
		IllegalStateException consumerError = new IllegalStateException("consumer error");

		try (Stream<MockedDO> doStream = this.getDataAccessObject().getAll())
		{
			doStream.forEach(dataObject -> { throw consumerError; });

			Assert.fail("IllegalStateException expected");
		}
		catch (IllegalStateException e)
		{
			Assert.assertSame(consumerError, e);
		}
	}
}
