package com.schoste.ddd.infrastructure.dao.v4.inmemory;

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

import com.schoste.ddd.infrastructure.dao.v4.exceptions.BrokenRecordException;
import com.schoste.ddd.infrastructure.dao.v4.exceptions.DAOException;
import com.schoste.ddd.infrastructure.dao.v4.exceptions.DAORuntimeException;
import com.schoste.ddd.infrastructure.dao.v4.inmemory.InMemoryDO;
import com.schoste.ddd.infrastructure.dao.v4.services.GenericDAOTest;
import com.schoste.ddd.infrastructure.dao.v4.services.DAO;
import com.schoste.ddd.infrastructure.dao.v4.services.listeners.DeleteListener;
import com.schoste.ddd.infrastructure.dao.v4.services.listeners.GetListener;
import com.schoste.ddd.infrastructure.dao.v4.services.listeners.RecordingDeleteListener;
import com.schoste.ddd.infrastructure.dao.v4.services.listeners.RecordingGetListener;
import com.schoste.ddd.infrastructure.dao.v4.services.listeners.RecordingSaveListener;
import com.schoste.ddd.infrastructure.dao.v4.services.listeners.SaveListener;

/**
 * Test class of the {@link InMemoryDAOImpl} implementation
 * 
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 */
@ContextConfiguration(locations = { "file:src/test/resources/unittest-beans.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class InMemoryDAOImplTest extends GenericDAOTest<Long, InMemoryDO, InMemoryDAOImpl>
{
	@Autowired
	protected InMemoryDAOImpl mockedDAOImpl;

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
	 * {@link DAO#registerOnGetListener(GetListener)},
	 * {@link DAO#registerOnSaveListener(SaveListener)} and
	 * {@link DAO#registerOnDeleteListener(DeleteListener)}
	 *  
	 * @throws Exception re-throws every exception
	 */
	@Before
	public void registerListeners() throws Exception
	{
		GetListener<Long, InMemoryDO> getListener = new RecordingGetListener<InMemoryDO>();

		this.getDataAccessObject().registerOnGetListener(getListener);

		Assert.assertEquals(1, this.getDataAccessObject().getOnGetListeners().size());


		SaveListener<Long, InMemoryDO> saveListener = new RecordingSaveListener<InMemoryDO>();

		this.getDataAccessObject().registerOnSaveListener(saveListener);

		Assert.assertEquals(1, this.getDataAccessObject().getOnSaveListeners().size());


		DeleteListener<Long, InMemoryDO> deleteListener = new RecordingDeleteListener<InMemoryDO>();

		this.getDataAccessObject().registerOnDeleteListener(deleteListener);

		Assert.assertEquals(1, this.getDataAccessObject().getOnDeleteListeners().size());
	}

	/**
	 * Unregisters all listeners which might have been registered during a test using
	 * {@link DAO#unregisterOnGetListener(GetListener)},
	 * {@link DAO#unregisterOnSaveListener(SaveListener)} and
	 * {@link DAO#unregisterOnDeleteListener(DeleteListener)}
	 * 
	 * @throws Exception re-throws every exception
	 */
	@After
	public void clearListeners() throws Exception
	{
		Collection<GetListener<Long, InMemoryDO>> getListeners = this.getDataAccessObject().getOnGetListeners();
		
		for (GetListener<Long, InMemoryDO> getListener : getListeners) this.getDataAccessObject().unregisterOnGetListener(getListener);

		Assert.assertEquals(0, this.getDataAccessObject().getOnGetListeners().size());


		Collection<SaveListener<Long, InMemoryDO>> saveListeners = this.getDataAccessObject().getOnSaveListeners();
		
		for (SaveListener<Long, InMemoryDO> saveListener : saveListeners) this.getDataAccessObject().unregisterOnSaveListener(saveListener);

		Assert.assertEquals(0, this.getDataAccessObject().getOnSaveListeners().size());


		Collection<DeleteListener<Long, InMemoryDO>> deleteListeners = this.getDataAccessObject().getOnDeleteListeners();
		
		for (DeleteListener<Long, InMemoryDO> deleteListener : deleteListeners) this.getDataAccessObject().unregisterOnDeleteListener(deleteListener);

		Assert.assertEquals(0, this.getDataAccessObject().getOnDeleteListeners().size());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected boolean assertDefaultGetListenersBeforeGet(int numOfexpectedDOs)
	{
		Collection<GetListener<Long, InMemoryDO>> getListeners = this.getDataAccessObject().getOnGetListeners();
		
		for (GetListener<Long, InMemoryDO> getListener : getListeners)
		{
			RecordingGetListener<InMemoryDO> mockedGetListener = (RecordingGetListener<InMemoryDO>) getListener;

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
		Collection<GetListener<Long, InMemoryDO>> getListeners = this.getDataAccessObject().getOnGetListeners();
		
		for (GetListener<Long, InMemoryDO> getListener : getListeners)
		{
			RecordingGetListener<InMemoryDO> mockedGetListener = (RecordingGetListener<InMemoryDO>) getListener;

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
		Collection<GetListener<Long, InMemoryDO>> getListeners = this.getDataAccessObject().getOnGetListeners();
		
		for (GetListener<Long, InMemoryDO> getListener : getListeners)
		{
			RecordingGetListener<InMemoryDO> mockedGetListener = (RecordingGetListener<InMemoryDO>) getListener;

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
		Collection<SaveListener<Long, InMemoryDO>> saveListeners = this.getDataAccessObject().getOnSaveListeners();
		
		for (SaveListener<Long, InMemoryDO> saveListener : saveListeners)
		{
			RecordingSaveListener<InMemoryDO> mockedSaveListener = (RecordingSaveListener<InMemoryDO>) saveListener;

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
		Collection<SaveListener<Long, InMemoryDO>> saveListeners = this.getDataAccessObject().getOnSaveListeners();
		
		for (SaveListener<Long, InMemoryDO> saveListener : saveListeners)
		{
			RecordingSaveListener<InMemoryDO> mockedSaveListener = (RecordingSaveListener<InMemoryDO>) saveListener;

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
		Collection<DeleteListener<Long, InMemoryDO>> deleteListeners = this.getDataAccessObject().getOnDeleteListeners();
		
		for (DeleteListener<Long, InMemoryDO> deleteListener : deleteListeners)
		{
			RecordingDeleteListener<InMemoryDO> mockedSaveListener = (RecordingDeleteListener<InMemoryDO>) deleteListener;

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
		Collection<DeleteListener<Long, InMemoryDO>> deleteListeners = this.getDataAccessObject().getOnDeleteListeners();
		
		for (DeleteListener<Long, InMemoryDO> deleteListener : deleteListeners)
		{
			RecordingDeleteListener<InMemoryDO> mockedDeleteListener = (RecordingDeleteListener<InMemoryDO>) deleteListener;

			if ((mockedDeleteListener).getDOsReceivedAfterDeleting().size() != numOfexpectedDOs) return false;
		}

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected InMemoryDAOImpl getDataAccessObject() 
	{
		return this.mockedDAOImpl;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected InMemoryDO createDataObject(Object... parameters) throws Exception
	{
		InMemoryDO dataObject = this.mockedDAOImpl.createDataObject();
		
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
	protected void modifyDataObject(InMemoryDO dataObject, Object... parameters) 
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
	protected InMemoryDO getExistingDataObject(long id)
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
		InMemoryDO dataObject = this.createDataObject(0L, "clearRepositorySucceeded");
		
		this.getDataAccessObject().save(dataObject);
		this.getDataAccessObject().clear();
		
		return (this.getDataAccessObject().getRecent().collect(Collectors.toList()).size() == 0);
	}

	/**
	 * Asserts that {@link DAO#get(java.io.Serializable)} returns the data object
	 * returned by {@link GetListener#onAfterGet}, as the data object loaders do
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void testGetReturnsDataObjectOfGetListener() throws Exception
	{
		InMemoryDO replacement = this.createDataObject(0L, "testGetReturnsDataObjectOfGetListener");

		this.getDataAccessObject().registerOnGetListener(new GetListener<Long, InMemoryDO>()
		{
			@Override
			public InMemoryDO onAfterGet(InMemoryDO dataObject)
			{
				return replacement;
			}
		});

		Assert.assertSame(replacement, this.getDataAccessObject().get(1L));
	}

	/**
	 * Asserts that a failure while loading data objects is not swallowed, but ends the stream
	 * with a {@link DAORuntimeException} which wraps the original exception in a {@link DAOException}
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void testGetAllFailsOnLoadingError() throws Exception
	{
		IllegalStateException loadingError = new IllegalStateException("loading error");

		this.getDataAccessObject().registerOnGetListener(new GetListener<Long, InMemoryDO>()
		{
			@Override
			public InMemoryDO onAfterGet(InMemoryDO dataObject)
			{
				throw loadingError;
			}
		});

		try (Stream<InMemoryDO> doStream = this.getDataAccessObject().getAll())
		{
			doStream.forEach(dataObject -> Assert.fail("no data object expected"));

			Assert.fail("DAORuntimeException expected");
		}
		catch (DAORuntimeException e)
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
		InMemoryDAOImpl dao = new InMemoryDAOImpl()
		{
			@Override
			protected synchronized InMemoryDO doGet(Long id) throws Exception
			{
				if (brokenId.equals(id)) throw readError;

				return super.doGet(id);
			}
		};

		try (Stream<InMemoryDO> doStream = this.getDataAccessObject().getAll())
		{
			dao.save(doStream.collect(Collectors.toList()));
		}

		Collection<InMemoryDO> loaded = new ArrayList<>();
		Collection<BrokenRecordException> broken = new ArrayList<>();

		try (Stream<InMemoryDO> doStream = dao.getAll())
		{
			Iterator<InMemoryDO> doIterator = doStream.iterator();

			while (true)
			{
				try
				{
					if (!doIterator.hasNext()) break;

					loaded.add(doIterator.next());
				}
				catch (DAORuntimeException e)
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

		try (Stream<InMemoryDO> doStream = this.getDataAccessObject().getAll())
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
