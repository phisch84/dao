package com.schoste.ddd.infrastructure.dao.v4.services;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.schoste.ddd.infrastructure.dao.v4.exceptions.DAOException;
import com.schoste.ddd.infrastructure.dao.v4.models.DataObject;
import com.schoste.ddd.infrastructure.dao.v4.services.listeners.DeleteListener;
import com.schoste.ddd.infrastructure.dao.v4.services.listeners.GetListener;
import com.schoste.ddd.infrastructure.dao.v4.services.listeners.SaveListener;

/**
 * Contract tests for implementations of the {@link DAO} interface (DAO = Data Access Object).
 * An implementation is correct if it passes all of these tests.
 *
 * To test an implementation, extend this class (e.g. as a Spring test) and implement the hooks:
 * <ul>
 * <li>{@link #getDAO()}: the DAO under test. Its storage is cleared before and after every test (see {@link DAO#clear()}).</li>
 * <li>{@link #setValue(DataObject, String)} and {@link #getValue(DataObject)}: write and read some content of a data object,
 * so the tests can check that content is stored.</li>
 * <li>{@link #getNotExistingId()}: an actual id which never belongs to a data object of the storage.</li>
 * <li>{@link #deletesPhysically()}: true if the implementation deletes physically instead of logically
 * (see {@link DAO#delete(DataObject)}). The tests then expect deleted data objects to be missing instead of flagged.</li>
 * <li>{@link #awaitStorageClockTick()}: only if the storage's clock isn't the clock of the test's host.</li>
 * </ul>
 *
 * Every test creates the data objects it needs; no data objects are expected to exist before a test.
 * The Javadoc of each test names the rule of the contract it checks (B1 to B11, see the documentation of {@link DAO}).
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 *
 * @param <ID> the class of the data objects' ids
 * @param <DO> the class of the data objects
 * @param <T> the class of the DAO under test
 */
public abstract class AbstractDAOContractTest<ID extends Serializable, DO extends DataObject<ID>, T extends DAO<ID, DO>>
{
	/**
	 * Gets the DAO under test
	 *
	 * @return the DAO under test
	 */
	protected abstract T getDAO();

	/**
	 * Writes a value into the content of a data object
	 *
	 * @param dataObject the data object to write to
	 * @param value the value to write
	 */
	protected abstract void setValue(DO dataObject, String value);

	/**
	 * Reads the value written by {@link #setValue(DataObject, String)} from a data object
	 *
	 * @param dataObject the data object to read from
	 * @return the value
	 */
	protected abstract String getValue(DO dataObject);

	/**
	 * Gets an actual (not virtual) id which never belongs to a data object of the storage
	 *
	 * @return an id without data object
	 */
	protected abstract ID getNotExistingId();

	/**
	 * Says whether the DAO under test deletes physically instead of logically (see {@link DAO#delete(DataObject)}).
	 *
	 * @return false by default
	 */
	protected boolean deletesPhysically()
	{
		return false;
	}

	/**
	 * Waits until the storage's clock has advanced by at least one millisecond, so that the next modification gets a
	 * greater modification time stamp than the previous one. By default the clock of the test's host is used.
	 * Override this method if the storage uses a different clock (e.g. a database server).
	 */
	protected void awaitStorageClockTick()
	{
		long start = System.currentTimeMillis();

		while (System.currentTimeMillis() <= start) Thread.yield();
	}

	// ---------------------------------------------------------------------------------------------------------------
	// helpers
	// ---------------------------------------------------------------------------------------------------------------

	protected DO newDataObject(String value) throws Exception
	{
		DO dataObject = this.getDAO().createDataObject();

		this.setValue(dataObject, value);

		return dataObject;
	}

	protected DO saveDataObject(String value) throws Exception
	{
		DO dataObject = this.newDataObject(value);

		this.getDAO().save(dataObject);

		return dataObject;
	}

	protected static <X> List<X> collect(Stream<X> stream)
	{
		try (Stream<X> closingStream = stream)
		{
			return closingStream.collect(Collectors.toList());
		}
	}

	protected Set<ID> idsOf(Collection<DO> dataObjects)
	{
		return dataObjects.stream().map(DataObject::getId).collect(Collectors.toSet());
	}

	@SafeVarargs
	protected final Set<ID> setOf(ID... ids)
	{
		return new HashSet<>(Arrays.asList(ids));
	}

	/**
	 * Reads a data object which must exist in the storage
	 *
	 * @param id the id of the data object
	 * @return the data object, never null
	 * @throws Exception re-throws every exception
	 */
	protected DO getExisting(ID id) throws Exception
	{
		DO stored = this.getDAO().get(id);

		Assert.assertNotNull("the data object with id " + id + " must be readable by its id", stored);

		return stored;
	}

	/**
	 * Asserts that the data object with the given id counts as deleted: it is flagged as deleted, or missing if the DAO
	 * deletes physically (see {@link #deletesPhysically()}).
	 *
	 * @param id the id of the deleted data object
	 * @throws Exception re-throws every exception
	 */
	protected void assertDeletedOrMissing(ID id) throws Exception
	{
		DO stored = this.getDAO().get(id);

		if (this.deletesPhysically())
		{
			Assert.assertNull("a DAO which deletes physically must not provide a deleted data object", stored);
		}
		else
		{
			Assert.assertNotNull("a logically deleted data object must still be readable by its id", stored);
			Assert.assertTrue("a logically deleted data object must be flagged as deleted", stored.isDeleted());
		}
	}

	protected void removeAllListeners()
	{
		for (GetListener<ID, DO> listener : this.getDAO().getOnGetListeners()) this.getDAO().unregisterOnGetListener(listener);
		for (SaveListener<ID, DO> listener : this.getDAO().getOnSaveListeners()) this.getDAO().unregisterOnSaveListener(listener);
		for (DeleteListener<ID, DO> listener : this.getDAO().getOnDeleteListeners()) this.getDAO().unregisterOnDeleteListener(listener);
	}

	@Before
	public void clearStorage() throws Exception
	{
		this.removeAllListeners();
		this.getDAO().clear();
	}

	@After
	public void clearStorageAfterwards() throws Exception
	{
		this.removeAllListeners();
		this.getDAO().clear();
	}

	// ---------------------------------------------------------------------------------------------------------------
	// creating data objects and ids (B6)
	// ---------------------------------------------------------------------------------------------------------------

	/**
	 * B6: a new data object has a virtual id.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void createDataObject_returnsDataObjectWithVirtualId() throws Exception
	{
		DO dataObject = this.getDAO().createDataObject();

		Assert.assertNotNull(dataObject);
		Assert.assertNotNull(dataObject.getId());
		Assert.assertTrue(this.getDAO().isVirtualId(dataObject.getId()));
	}

	/**
	 * B6: new data objects have different virtual ids.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void createDataObject_calledRepeatedly_returnsUniqueVirtualIds() throws Exception
	{
		Set<ID> ids = new HashSet<>();

		for (int i=0; i<5; i++) Assert.assertTrue(ids.add(this.getDAO().createDataObject().getId()));
	}

	/**
	 * B6: null isn't an id.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void isVirtualId_null_throwsIllegalArgument()
	{
		this.getDAO().isVirtualId(null);
	}

	/**
	 * B6: saving a new data object gives it an actual id, under which it can be read.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void save_newDataObject_getsActualIdAndCanBeRead() throws Exception
	{
		DO dataObject = this.saveDataObject("value");

		Assert.assertFalse(this.getDAO().isVirtualId(dataObject.getId()));

		DO stored = this.getDAO().get(dataObject.getId());

		Assert.assertNotNull(stored);
		Assert.assertEquals("value", this.getValue(stored));
	}

	// ---------------------------------------------------------------------------------------------------------------
	// reading by id (B1, B2, B10)
	// ---------------------------------------------------------------------------------------------------------------

	/**
	 * An id without data object gives null.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void get_notExistingId_returnsNull() throws Exception
	{
		Assert.assertNull(this.getDAO().get(this.getNotExistingId()));
	}

	/**
	 * B10: null isn't an id.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test(expected = IllegalArgumentException.class)
	public void get_null_throwsIllegalArgument() throws Exception
	{
		this.getDAO().get((ID) null);
	}

	/**
	 * Only the data objects of existing ids are provided.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void getCollection_existingAndNotExistingIds_providesExistingOnes() throws Exception
	{
		DO dataObject1 = this.saveDataObject("1");
		DO dataObject2 = this.saveDataObject("2");

		this.saveDataObject("3");

		List<DO> dataObjects = collect(this.getDAO().get(Arrays.asList(dataObject1.getId(), dataObject2.getId(), this.getNotExistingId())));

		Assert.assertEquals(this.setOf(dataObject1.getId(), dataObject2.getId()), this.idsOf(dataObjects));
	}

	/**
	 * B2: no ids provide no data objects (not all of them).
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void getCollection_emptyCollection_providesNothing() throws Exception
	{
		this.saveDataObject("1");

		Assert.assertTrue(collect(this.getDAO().get(Collections.<ID>emptyList())).isEmpty());
	}

	/**
	 * B10: null isn't accepted as collection.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test(expected = IllegalArgumentException.class)
	public void getCollection_null_throwsIllegalArgument() throws Exception
	{
		this.getDAO().get((Collection<ID>) null);
	}

	/**
	 * B10: null isn't accepted as id in a collection.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test(expected = IllegalArgumentException.class)
	public void getCollection_nullElement_throwsIllegalArgument() throws Exception
	{
		this.getDAO().get(Arrays.asList(this.saveDataObject("1").getId(), null));
	}

	/**
	 * B1: reading a deleted data object by its id provides it flagged as deleted, or not at all if the DAO deletes physically.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void get_deletedDataObject_returnsItFlaggedOrNull() throws Exception
	{
		DO dataObject = this.saveDataObject("1");

		this.getDAO().delete(dataObject);

		this.assertDeletedOrMissing(dataObject.getId());
	}

	/**
	 * B1: reading deleted data objects by their ids provides them flagged as deleted, or not at all if the DAO deletes physically.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void getCollection_deletedDataObject_providesItFlaggedOrNot() throws Exception
	{
		DO deleted = this.saveDataObject("1");
		DO kept = this.saveDataObject("2");

		this.getDAO().delete(deleted);

		List<DO> dataObjects = collect(this.getDAO().get(Arrays.asList(deleted.getId(), kept.getId())));

		if (this.deletesPhysically())
		{
			Assert.assertEquals(this.setOf(kept.getId()), this.idsOf(dataObjects));
		}
		else
		{
			Assert.assertEquals(this.setOf(deleted.getId(), kept.getId()), this.idsOf(dataObjects));

			for (DO dataObject : dataObjects) Assert.assertEquals(dataObject.getId().equals(deleted.getId()), dataObject.isDeleted());
		}
	}

	// ---------------------------------------------------------------------------------------------------------------
	// reading all data objects (B1)
	// ---------------------------------------------------------------------------------------------------------------

	/**
	 * All saved data objects are provided.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void getAll_savedDataObjects_providesAll() throws Exception
	{
		DO dataObject1 = this.saveDataObject("1");
		DO dataObject2 = this.saveDataObject("2");
		DO dataObject3 = this.saveDataObject("3");

		Assert.assertEquals(this.setOf(dataObject1.getId(), dataObject2.getId(), dataObject3.getId()), this.idsOf(collect(this.getDAO().getAll())));
	}

	/**
	 * B1: deleted data objects are left out.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void getAll_deletedDataObject_isLeftOut() throws Exception
	{
		DO deleted = this.saveDataObject("1");
		DO kept = this.saveDataObject("2");

		this.getDAO().delete(deleted);

		Assert.assertEquals(this.setOf(kept.getId()), this.idsOf(collect(this.getDAO().getAll())));
	}

	/**
	 * B1: deleted data objects are included if asked for, unless the DAO deletes physically.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void getAllIncludingDeleted_deletedDataObject_isIncludedFlagged() throws Exception
	{
		DO deleted = this.saveDataObject("1");
		DO kept = this.saveDataObject("2");

		this.getDAO().delete(deleted);

		List<DO> dataObjects = collect(this.getDAO().getAll(DeletedDataObjects.INCLUDE));

		if (this.deletesPhysically()) Assert.assertEquals(this.setOf(kept.getId()), this.idsOf(dataObjects));
		else Assert.assertEquals(this.setOf(deleted.getId(), kept.getId()), this.idsOf(dataObjects));
	}

	/**
	 * Null isn't accepted for the deleted data objects.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test(expected = IllegalArgumentException.class)
	public void getAll_nullForDeleted_throwsIllegalArgument() throws Exception
	{
		this.getDAO().getAll(null);
	}

	// ---------------------------------------------------------------------------------------------------------------
	// saving (B1, B10)
	// ---------------------------------------------------------------------------------------------------------------

	/**
	 * Saving a stored data object again stores its changed content.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void save_changedDataObject_storesChange() throws Exception
	{
		DO dataObject = this.saveDataObject("before");

		this.setValue(dataObject, "after");
		this.getDAO().save(dataObject);

		Assert.assertEquals("after", this.getValue(this.getDAO().get(dataObject.getId())));
	}

	/**
	 * B10: null isn't accepted.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test(expected = IllegalArgumentException.class)
	public void save_null_throwsIllegalArgument() throws Exception
	{
		this.getDAO().save((DO) null);
	}

	/**
	 * B10: null isn't accepted as collection.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test(expected = IllegalArgumentException.class)
	public void saveCollection_null_throwsIllegalArgument() throws Exception
	{
		this.getDAO().save((Collection<DO>) null);
	}

	/**
	 * B10: null isn't accepted as element; nothing is saved.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void saveCollection_nullElement_throwsIllegalArgumentAndSavesNothing() throws Exception
	{
		try
		{
			this.getDAO().save(Arrays.asList(this.newDataObject("1"), null));

			Assert.fail();
		}
		catch (IllegalArgumentException e)
		{
			Assert.assertTrue(collect(this.getDAO().getAll()).isEmpty());
		}
	}

	/**
	 * B1: saving a deleted data object with the deleted flag set to false undeletes it.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void save_deletedDataObjectWithFlagCleared_undeletesIt() throws Exception
	{
		if (this.deletesPhysically()) return;

		DO dataObject = this.saveDataObject("1");

		this.getDAO().delete(dataObject);

		dataObject.setDeleted(false);

		this.getDAO().save(dataObject);

		Assert.assertFalse(this.getExisting(dataObject.getId()).isDeleted());
		Assert.assertEquals(this.setOf(dataObject.getId()), this.idsOf(collect(this.getDAO().getAll())));
	}

	// ---------------------------------------------------------------------------------------------------------------
	// deleting (B1, B7, B10)
	// ---------------------------------------------------------------------------------------------------------------

	/**
	 * B1: deleting flags the data object; it can still be read by its id, unless the DAO deletes physically.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void delete_savedDataObject_flagsIt() throws Exception
	{
		DO dataObject = this.saveDataObject("1");

		this.getDAO().delete(dataObject);

		Assert.assertTrue(dataObject.isDeleted());

		this.assertDeletedOrMissing(dataObject.getId());
	}

	/**
	 * B1, B4: a logical deletion is a modification, so change tracking reports it (if deleted data objects are included).
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void delete_savedDataObject_isReportedByChangeTracking() throws Exception
	{
		if (this.deletesPhysically()) return;

		DO dataObject = this.saveDataObject("1");
		long savedAt = dataObject.getModifiedTimestamp();

		this.awaitStorageClockTick();
		this.getDAO().delete(dataObject);

		List<DO> changed = collect(this.getDAO().getModifiedSince(savedAt, DeletedDataObjects.INCLUDE));

		Assert.assertEquals(this.setOf(dataObject.getId()), this.idsOf(changed));
		Assert.assertTrue(changed.get(0).isDeleted());
	}

	/**
	 * B1: deleting a deleted data object again does nothing; its modification time stamp stays the same.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void delete_deletedDataObject_doesNothing() throws Exception
	{
		if (this.deletesPhysically()) return;

		DO dataObject = this.saveDataObject("1");

		this.getDAO().delete(dataObject);

		long deletedAt = this.getExisting(dataObject.getId()).getModifiedTimestamp();

		this.awaitStorageClockTick();
		this.getDAO().delete(dataObject);

		Assert.assertEquals(deletedAt, this.getExisting(dataObject.getId()).getModifiedTimestamp());
	}

	/**
	 * B1: deleting a data object which was never saved only sets its flag; nothing is stored.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void delete_neverSavedDataObject_onlySetsFlag() throws Exception
	{
		DO dataObject = this.newDataObject("1");

		this.getDAO().delete(dataObject);

		Assert.assertTrue(dataObject.isDeleted());
		Assert.assertTrue(collect(this.getDAO().getAll(DeletedDataObjects.INCLUDE)).isEmpty());
	}

	/**
	 * B1, B7: deleting by id deletes like deleting the data object.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void deleteById_savedDataObject_deletesIt() throws Exception
	{
		DO dataObject = this.saveDataObject("1");

		this.getDAO().deleteById(dataObject.getId());

		this.assertDeletedOrMissing(dataObject.getId());
	}

	/**
	 * B1: ids without data object are ignored when deleting by ids.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void deleteByIds_notExistingId_isIgnored() throws Exception
	{
		DO dataObject = this.saveDataObject("1");

		this.getDAO().deleteByIds(Arrays.asList(dataObject.getId(), this.getNotExistingId()));

		Assert.assertTrue(collect(this.getDAO().getAll()).isEmpty());
	}

	/**
	 * B10: null isn't accepted.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test(expected = IllegalArgumentException.class)
	public void delete_null_throwsIllegalArgument() throws Exception
	{
		this.getDAO().delete((DO) null);
	}

	/**
	 * B10: null isn't accepted as id.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test(expected = IllegalArgumentException.class)
	public void deleteById_null_throwsIllegalArgument() throws Exception
	{
		this.getDAO().deleteById(null);
	}

	/**
	 * B10: null isn't accepted as id in a collection; nothing is deleted.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void deleteByIds_nullElement_throwsIllegalArgumentAndDeletesNothing() throws Exception
	{
		DO dataObject = this.saveDataObject("1");

		try
		{
			this.getDAO().deleteByIds(Arrays.asList(dataObject.getId(), null));

			Assert.fail();
		}
		catch (IllegalArgumentException e)
		{
			Assert.assertFalse(this.getExisting(dataObject.getId()).isDeleted());
		}
	}

	// ---------------------------------------------------------------------------------------------------------------
	// change tracking and time stamps (B4, B5)
	// ---------------------------------------------------------------------------------------------------------------

	/**
	 * B4: only data objects modified after the given time stamp are provided.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void getModifiedSince_timestamp_providesOnlyLaterModifications() throws Exception
	{
		DO before = this.saveDataObject("before");
		long timestamp = before.getModifiedTimestamp();

		this.awaitStorageClockTick();

		DO after = this.saveDataObject("after");

		Assert.assertEquals(this.setOf(after.getId()), this.idsOf(collect(this.getDAO().getModifiedSince(timestamp))));
	}

	/**
	 * B1, B4: deleted data objects are left out unless asked for (and unless the DAO deletes physically).
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void getModifiedSince_deletedDataObject_isLeftOutUnlessIncluded() throws Exception
	{
		DO deleted = this.saveDataObject("deleted");
		DO kept = this.saveDataObject("kept");

		this.getDAO().delete(deleted);

		Assert.assertEquals(this.setOf(kept.getId()), this.idsOf(collect(this.getDAO().getModifiedSince(Long.MIN_VALUE))));

		Set<ID> included = this.idsOf(collect(this.getDAO().getModifiedSince(Long.MIN_VALUE, DeletedDataObjects.INCLUDE)));

		if (this.deletesPhysically()) Assert.assertEquals(this.setOf(kept.getId()), included);
		else Assert.assertEquals(this.setOf(deleted.getId(), kept.getId()), included);
	}

	/**
	 * B4: change tracking is stateless; consuming a result doesn't change the next result for the same time stamp.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void getModifiedSince_calledTwice_providesSameDataObjects() throws Exception
	{
		DO dataObject1 = this.saveDataObject("1");
		DO dataObject2 = this.saveDataObject("2");

		Set<ID> first = this.idsOf(collect(this.getDAO().getModifiedSince(Long.MIN_VALUE)));
		Set<ID> second = this.idsOf(collect(this.getDAO().getModifiedSince(Long.MIN_VALUE)));

		Assert.assertEquals(this.setOf(dataObject1.getId(), dataObject2.getId()), first);
		Assert.assertEquals(first, second);
	}

	/**
	 * Null isn't accepted for the deleted data objects.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test(expected = IllegalArgumentException.class)
	public void getModifiedSince_nullForDeleted_throwsIllegalArgument() throws Exception
	{
		this.getDAO().getModifiedSince(Long.MIN_VALUE, null);
	}

	/**
	 * B5: after saving, the data object carries the time stamp which the storage has stored.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void save_dataObject_carriesStoredTimestamp() throws Exception
	{
		DO dataObject = this.saveDataObject("1");

		Assert.assertEquals(this.getExisting(dataObject.getId()).getModifiedTimestamp(), dataObject.getModifiedTimestamp());
	}

	/**
	 * B5: a later modification gets a greater modification time stamp.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void save_laterModification_getsGreaterTimestamp() throws Exception
	{
		DO dataObject = this.saveDataObject("1");
		long firstSave = dataObject.getModifiedTimestamp();

		this.awaitStorageClockTick();
		this.setValue(dataObject, "2");
		this.getDAO().save(dataObject);

		Assert.assertTrue(dataObject.getModifiedTimestamp() > firstSave);
	}

	// ---------------------------------------------------------------------------------------------------------------
	// streams (B9)
	// ---------------------------------------------------------------------------------------------------------------

	/**
	 * B9: returned streams are sequential.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void getAll_returnedStream_isSequential() throws Exception
	{
		try (Stream<DO> dataObjects = this.getDAO().getAll())
		{
			Assert.assertFalse(dataObjects.isParallel());
		}
	}

	/**
	 * B9: closing a stream which was only partially consumed releases it; the DAO keeps working.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void getAll_partiallyConsumedStreamClosed_daoKeepsWorking() throws Exception
	{
		this.saveDataObject("1");
		this.saveDataObject("2");

		try (Stream<DO> dataObjects = this.getDAO().getAll())
		{
			Assert.assertTrue(dataObjects.findFirst().isPresent());
		}

		DO dataObject = this.saveDataObject("3");

		Assert.assertNotNull(this.getDAO().get(dataObject.getId()));
		Assert.assertEquals(3, collect(this.getDAO().getAll()).size());
	}

	// ---------------------------------------------------------------------------------------------------------------
	// listeners (B3, B7)
	// ---------------------------------------------------------------------------------------------------------------

	/**
	 * Save listener which records its calls in a shared list
	 */
	protected class NamedSaveListener implements SaveListener<ID, DO>
	{
		private final String name;
		private final List<String> calls;
		private final boolean allowSaving;

		protected NamedSaveListener(String name, List<String> calls, boolean allowSaving)
		{
			this.name = name;
			this.calls = calls;
			this.allowSaving = allowSaving;
		}

		@Override
		public boolean onBeforeSave(DO dataObjectToSave)
		{
			this.calls.add(this.name + ".onBeforeSave");

			return this.allowSaving;
		}

		@Override
		public void onAfterSave(DO dataObjectSaved)
		{
			this.calls.add(this.name + ".onAfterSave");
		}
	}

	/**
	 * B7: listeners are called in the order of their registration.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void listeners_registeredInOrder_areCalledInThatOrder() throws Exception
	{
		List<String> calls = new ArrayList<>();

		this.getDAO().registerOnSaveListener(new NamedSaveListener("first", calls, true));
		this.getDAO().registerOnSaveListener(new NamedSaveListener("second", calls, true));
		this.getDAO().registerOnSaveListener(new NamedSaveListener("third", calls, true));

		this.saveDataObject("1");

		Assert.assertEquals(Arrays.asList("first.onBeforeSave", "second.onBeforeSave", "third.onBeforeSave",
										  "first.onAfterSave", "second.onAfterSave", "third.onAfterSave"), calls);
	}

	/**
	 * B7: a listener which cancels stops the listeners registered after it; nothing is saved and no onAfter... is called.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void saveListener_cancels_laterListenersAreNotCalledAndNothingIsSaved() throws Exception
	{
		List<String> calls = new ArrayList<>();

		this.getDAO().registerOnSaveListener(new NamedSaveListener("first", calls, true));
		this.getDAO().registerOnSaveListener(new NamedSaveListener("cancelling", calls, false));
		this.getDAO().registerOnSaveListener(new NamedSaveListener("third", calls, true));

		this.saveDataObject("1");

		Assert.assertEquals(Arrays.asList("first.onBeforeSave", "cancelling.onBeforeSave"), calls);
		Assert.assertTrue(collect(this.getDAO().getAll()).isEmpty());
	}

	/**
	 * B7: an exception from an onAfter... listener is thrown, although the data object has been saved.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void saveListener_onAfterSaveThrows_exceptionIsThrownAndDataObjectIsSaved() throws Exception
	{
		this.getDAO().registerOnSaveListener(new SaveListener<ID, DO>()
		{
			@Override
			public void onAfterSave(DO dataObjectSaved) { throw new IllegalStateException("test"); }
		});

		DO dataObject = this.newDataObject("1");

		try
		{
			this.getDAO().save(dataObject);

			Assert.fail();
		}
		catch (DAOException e)
		{
			Assert.assertNotNull(this.getDAO().get(dataObject.getId()));
		}
	}

	/**
	 * B7: deleting by id calls the same delete listener methods as deleting the data object.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void deleteListener_deleteById_isCalledWithTheDataObject() throws Exception
	{
		DO dataObject = this.saveDataObject("1");
		List<ID> deletedIds = new ArrayList<>();

		this.getDAO().registerOnDeleteListener(new DeleteListener<ID, DO>()
		{
			@Override
			public boolean onBeforeDelete(DO dataObjectToDelete) { deletedIds.add(dataObjectToDelete.getId()); return true; }

			@Override
			public boolean onBeforeDelete(Collection<DO> dataObjectsToDelete) { for (DO d : dataObjectsToDelete) deletedIds.add(d.getId()); return true; }
		});

		this.getDAO().deleteById(dataObject.getId());

		Assert.assertEquals(Collections.singletonList(dataObject.getId()), deletedIds);
	}

	/**
	 * B7: onAfterGet is only called for data objects which were found, never with null.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void getListener_notExistingId_onAfterGetIsNotCalled() throws Exception
	{
		List<DO> afterGet = new ArrayList<>();

		this.getDAO().registerOnGetListener(new GetListener<ID, DO>()
		{
			@Override
			public DO onAfterGet(DO dataObject) { afterGet.add(dataObject); return dataObject; }
		});

		this.getDAO().get(this.getNotExistingId());

		Assert.assertTrue(afterGet.isEmpty());
	}

	/**
	 * B3: a get listener must not return null as ids.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test(expected = IllegalStateException.class)
	public void getListener_returnsNullIds_throwsIllegalState() throws Exception
	{
		DO dataObject = this.saveDataObject("1");

		this.getDAO().registerOnGetListener(new GetListener<ID, DO>()
		{
			@Override
			public ID[] onBeforeGet(ID[] ids) { return null; }
		});

		collect(this.getDAO().get(Collections.singletonList(dataObject.getId())));
	}

	/**
	 * B2, B3: a get listener which returns no ids makes the DAO provide nothing.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void getListener_returnsNoIds_providesNothing() throws Exception
	{
		DO dataObject = this.saveDataObject("1");

		this.getDAO().registerOnGetListener(new GetListener<ID, DO>()
		{
			@Override
			public ID[] onBeforeGet(ID[] ids) { return Arrays.copyOf(ids, 0); }
		});

		Assert.assertTrue(collect(this.getDAO().get(Collections.singletonList(dataObject.getId()))).isEmpty());
	}

	/**
	 * B7: getAll() doesn't call the id-based onBeforeGet.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void getListener_getAll_idBasedOnBeforeGetIsNotCalled() throws Exception
	{
		List<String> calls = new ArrayList<>();

		this.saveDataObject("1");
		this.getDAO().registerOnGetListener(new GetListener<ID, DO>()
		{
			@Override
			public ID[] onBeforeGet(ID[] ids) { calls.add("onBeforeGet(ids)"); return ids; }
		});

		collect(this.getDAO().getAll());

		Assert.assertTrue(calls.isEmpty());
	}

	// ---------------------------------------------------------------------------------------------------------------
	// equality of data objects (B11)
	// ---------------------------------------------------------------------------------------------------------------

	/**
	 * B11: data objects are only equal to themselves; records and versions are compared explicitly.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void dataObject_sameIdAndTimestamp_isNotEqualButSameRecordAndVersion() throws Exception
	{
		DO dataObject = this.saveDataObject("1");
		DO copy = this.newDataObject("1");

		copy.setId(dataObject.getId());
		copy.setModifiedTimestamp(dataObject.getModifiedTimestamp());

		Assert.assertNotEquals(dataObject, copy);
		Assert.assertTrue(dataObject.isSameRecordAs(copy));
		Assert.assertTrue(dataObject.isSameVersionAs(copy));

		copy.setModifiedTimestamp(dataObject.getModifiedTimestamp() + 1);

		Assert.assertTrue(dataObject.isSameRecordAs(copy));
		Assert.assertFalse(dataObject.isSameVersionAs(copy));
		Assert.assertFalse(dataObject.isSameRecordAs(this.newDataObject("2")));
	}

	/**
	 * B11: a data object stays findable in a hash set when saving changes its id.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void dataObject_inHashSet_isFoundAfterSaving() throws Exception
	{
		DO dataObject = this.newDataObject("1");
		Set<DO> dataObjects = new HashSet<>();

		dataObjects.add(dataObject);
		this.getDAO().save(dataObject);

		Assert.assertTrue(dataObjects.contains(dataObject));
	}

	// ---------------------------------------------------------------------------------------------------------------
	// clear (B12) and threads (B8)
	// ---------------------------------------------------------------------------------------------------------------

	/**
	 * B12: clear() removes all data objects physically, including the deleted ones.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void clear_storageWithDeletedDataObject_removesEverything() throws Exception
	{
		DO deleted = this.saveDataObject("1");

		this.saveDataObject("2");
		this.getDAO().delete(deleted);
		this.getDAO().clear();

		Assert.assertTrue(collect(this.getDAO().getAll(DeletedDataObjects.INCLUDE)).isEmpty());
		Assert.assertNull(this.getDAO().get(deleted.getId()));
	}

	/**
	 * B8: a DAO instance can be used by several threads at the same time.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void save_fromSeveralThreads_storesAllDataObjects() throws Exception
	{
		int threads = 4;
		int savesPerThread = 10;
		ExecutorService executor = Executors.newFixedThreadPool(threads);
		List<Future<?>> futures = new ArrayList<>();

		try
		{
			for (int t=0; t<threads; t++)
			{
				int thread = t;

				futures.add(executor.submit(() ->
				{
					for (int i=0; i<savesPerThread; i++) this.saveDataObject(thread + "-" + i);

					return null;
				}));
			}

			for (Future<?> future : futures) future.get(1, TimeUnit.MINUTES);
		}
		finally
		{
			executor.shutdownNow();
		}

		List<DO> dataObjects = collect(this.getDAO().getAll());

		Assert.assertEquals(threads * savesPerThread, dataObjects.size());
		Assert.assertEquals(threads * savesPerThread, this.idsOf(dataObjects).size());
	}
}
