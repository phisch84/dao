package com.schoste.ddd.infrastructure.dao.v4.inmemory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
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
import com.schoste.ddd.infrastructure.dao.v4.exceptions.DAORuntimeException;
import com.schoste.ddd.infrastructure.dao.v4.services.listeners.GetListener;

/**
 * Tests of the stream and loader mechanics of {@link AbstractInMemoryDAO} (and thereby of
 * {@link com.schoste.ddd.infrastructure.dao.v4.services.AbstractDAO} and
 * {@link com.schoste.ddd.infrastructure.dao.v4.services.SourceRecordDataObjectLoader}) which go beyond the DAO contract tests
 * ({@link InMemoryDAOContractTest}).
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 */
@ContextConfiguration(locations = { "file:src/test/resources/unittest-beans.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class InMemoryDAOImplTest
{
	@Autowired
	private InMemoryDAOImpl dao;

	protected void removeAllListeners()
	{
		for (GetListener<Long, InMemoryDO> listener : this.dao.getOnGetListeners()) this.dao.unregisterOnGetListener(listener);
	}

	@Before
	public void setUp() throws Exception
	{
		this.removeAllListeners();
		this.dao.clear();
	}

	@After
	public void tearDown() throws Exception
	{
		this.removeAllListeners();
		this.dao.clear();
	}

	protected InMemoryDO saveDataObject(String value) throws Exception
	{
		InMemoryDO dataObject = this.dao.createDataObject();

		dataObject.setExampleStringProperty(value);

		this.dao.save(dataObject);

		return dataObject;
	}

	/**
	 * {@link com.schoste.ddd.infrastructure.dao.v4.services.DAO#get(java.io.Serializable)} returns the data object returned by
	 * {@link GetListener#onAfterGet(com.schoste.ddd.infrastructure.dao.v4.models.DataObject)}.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void get_listenerReplacesDataObject_returnsReplacement() throws Exception
	{
		InMemoryDO stored = this.saveDataObject("stored");
		InMemoryDO replacement = this.dao.createDataObject();

		this.dao.registerOnGetListener(new GetListener<Long, InMemoryDO>()
		{
			@Override
			public InMemoryDO onAfterGet(InMemoryDO dataObject) { return replacement; }
		});

		Assert.assertSame(replacement, this.dao.get(stored.getId()));
	}

	/**
	 * A failure while loading isn't swallowed: it ends the stream with a {@link DAORuntimeException}
	 * which wraps the original exception.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void getAll_loadingFails_endsStreamWithDAORuntimeException() throws Exception
	{
		IllegalStateException loadingError = new IllegalStateException("loading error");

		this.saveDataObject("1");
		this.dao.registerOnGetListener(new GetListener<Long, InMemoryDO>()
		{
			@Override
			public InMemoryDO onAfterGet(InMemoryDO dataObject) { throw loadingError; }
		});

		try (Stream<InMemoryDO> dataObjects = this.dao.getAll())
		{
			dataObjects.forEach(dataObject -> Assert.fail("no data object expected"));

			Assert.fail("DAORuntimeException expected");
		}
		catch (DAORuntimeException e)
		{
			Assert.assertFalse(e.getCause() instanceof BrokenRecordException);
			Assert.assertSame(loadingError, e.getCause().getCause());
		}
	}

	/**
	 * A record which can't be loaded is reported as {@link BrokenRecordException}; iterating the stream can continue
	 * with the remaining data objects.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void getAll_brokenRecord_isReportedAndLoadingContinues() throws Exception
	{
		Long brokenId = 2L;
		IOException readError = new IOException("broken record");
		InMemoryDAOImpl brokenDAO = new InMemoryDAOImpl()
		{
			@Override
			protected synchronized InMemoryDO doGet(Long id) throws Exception
			{
				if (brokenId.equals(id)) throw readError;

				return super.doGet(id);
			}
		};

		for (long i=1; i<=3; i++)
		{
			InMemoryDO dataObject = new InMemoryDO();

			dataObject.setId(-i);
			brokenDAO.save(dataObject);
		}

		Collection<InMemoryDO> loaded = new ArrayList<>();
		Collection<BrokenRecordException> broken = new ArrayList<>();

		try (Stream<InMemoryDO> dataObjects = brokenDAO.getAll())
		{
			Iterator<InMemoryDO> iterator = dataObjects.iterator();

			while (true)
			{
				try
				{
					if (!iterator.hasNext()) break;

					loaded.add(iterator.next());
				}
				catch (DAORuntimeException e)
				{
					broken.add((BrokenRecordException) e.getCause());
				}
			}
		}

		Assert.assertEquals(2, loaded.size());
		Assert.assertEquals(1, broken.size());
		Assert.assertEquals(brokenId, broken.iterator().next().getSourceRecord());
		Assert.assertSame(readError, broken.iterator().next().getCause());
	}

	/**
	 * Exceptions thrown by the consumer of a stream are re-thrown as they are.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void getAll_consumerThrows_exceptionPassesThrough() throws Exception
	{
		IllegalStateException consumerError = new IllegalStateException("consumer error");

		this.saveDataObject("1");

		try (Stream<InMemoryDO> dataObjects = this.dao.getAll())
		{
			dataObjects.forEach(dataObject -> { throw consumerError; });

			Assert.fail("IllegalStateException expected");
		}
		catch (IllegalStateException e)
		{
			Assert.assertSame(consumerError, e);
		}
	}
}
