package com.schoste.ddd.infrastructure.dao.v4.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.schoste.ddd.infrastructure.dao.v4.exceptions.DAOException;

/**
 * Tests of the storage independent semantics of transactions ({@link DAOTransactionManager}, {@link DAOTransaction}) as implemented by
 * {@link AbstractDAOTransactionManager}: nesting, rollback-only marks, completion and the binding of the resource to the thread.
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 */
public class AbstractDAOTransactionManagerTest
{
	/**
	 * A transaction manager which records the calls of its hooks. Its resource is a string with a sequence number.
	 */
	protected static class RecordingTransactionManager extends AbstractDAOTransactionManager<String>
	{
		protected final Object resourceKey;
		protected final List<String> calls;
		protected int resources = 0;
		protected boolean failBegin = false;
		protected boolean failCommit = false;

		protected RecordingTransactionManager(Object resourceKey, List<String> calls)
		{
			this.resourceKey = resourceKey;
			this.calls = calls;
		}

		@Override
		protected Object getResourceKey()
		{
			return this.resourceKey;
		}

		@Override
		protected synchronized String doBegin() throws Exception
		{
			if (this.failBegin) throw new IllegalStateException("begin failed");

			String resource = "resource" + (++this.resources);

			this.calls.add("begin " + resource);

			return resource;
		}

		@Override
		protected synchronized void doCommit(String resource) throws Exception
		{
			this.calls.add("commit " + resource);

			if (this.failCommit) throw new IllegalStateException("commit failed");
		}

		@Override
		protected synchronized void doRollback(String resource) throws Exception
		{
			this.calls.add("rollback " + resource);
		}

		@Override
		protected synchronized void doRelease(String resource) throws Exception
		{
			this.calls.add("release " + resource);
		}
	}

	protected final Object resourceKey = new Object();
	protected List<String> calls;
	protected RecordingTransactionManager manager;

	@Before
	public void setUp()
	{
		this.calls = Collections.synchronizedList(new ArrayList<>());
		this.manager = new RecordingTransactionManager(this.resourceKey, this.calls);
	}

	@After
	public void tearDown()
	{
		Assert.assertFalse("a test left a transaction bound to the thread", this.manager.isActive());
	}

	protected void assertCalls(String... expected)
	{
		Assert.assertEquals(Arrays.asList(expected), new ArrayList<>(this.calls));
	}

	/**
	 * Runs an operation on another thread and returns what it threw (or null).
	 */
	protected Throwable runOnOtherThread(ThrowingRunnable operation) throws Exception
	{
		AtomicReference<Throwable> thrown = new AtomicReference<>();
		Thread thread = new Thread(() ->
		{
			try
			{
				operation.run();
			}
			catch (Throwable t)
			{
				thrown.set(t);
			}
		});

		thread.start();
		thread.join(10000);

		Assert.assertFalse("the other thread didn't finish", thread.isAlive());

		return thrown.get();
	}

	@FunctionalInterface
	protected interface ThrowingRunnable
	{
		void run() throws Exception;
	}

	@Test
	public void testCommitBeginsCommitsAndReleasesOnce() throws Exception
	{
		Assert.assertFalse(this.manager.isActive());

		try (DAOTransaction transaction = this.manager.begin())
		{
			Assert.assertTrue(transaction.isOutermost());
			Assert.assertTrue(this.manager.isActive());

			transaction.commit();
		}

		Assert.assertFalse(this.manager.isActive());
		this.assertCalls("begin resource1", "commit resource1", "release resource1");
	}

	@Test
	public void testRollbackDiscardsAndReleases() throws Exception
	{
		try (DAOTransaction transaction = this.manager.begin())
		{
			transaction.rollback();
		}

		this.assertCalls("begin resource1", "rollback resource1", "release resource1");
	}

	@Test
	public void testCloseWithoutCommitRollsBack() throws Exception
	{
		try (DAOTransaction transaction = this.manager.begin())
		{
			Assert.assertTrue(transaction.isOutermost());
		}

		this.assertCalls("begin resource1", "rollback resource1", "release resource1");
	}

	@Test
	public void testCloseAfterCommitDoesNothing() throws Exception
	{
		DAOTransaction transaction = this.manager.begin();

		transaction.commit();
		transaction.close();
		transaction.close();

		this.assertCalls("begin resource1", "commit resource1", "release resource1");
	}

	@Test
	public void testCompletingTwiceFails() throws Exception
	{
		DAOTransaction transaction = this.manager.begin();

		transaction.commit();

		try
		{
			transaction.commit();
			Assert.fail("committed twice");
		}
		catch (IllegalStateException e) { }

		try
		{
			transaction.rollback();
			Assert.fail("rolled back after commit");
		}
		catch (IllegalStateException e) { }

		this.assertCalls("begin resource1", "commit resource1", "release resource1");
	}

	@Test
	public void testInnerTransactionJoinsOuterOne() throws Exception
	{
		try (DAOTransaction outer = this.manager.begin())
		{
			try (DAOTransaction inner = this.manager.begin())
			{
				Assert.assertFalse(inner.isOutermost());

				inner.commit();
			}

			this.assertCalls("begin resource1");
			Assert.assertTrue(this.manager.isActive());

			outer.commit();
		}

		this.assertCalls("begin resource1", "commit resource1", "release resource1");
	}

	@Test
	public void testInnerRollbackMarksOuterForRollback() throws Exception
	{
		try (DAOTransaction outer = this.manager.begin())
		{
			try (DAOTransaction inner = this.manager.begin())
			{
				inner.rollback();
			}

			this.assertCalls("begin resource1");

			try
			{
				outer.commit();
				Assert.fail("committed a transaction marked for rollback");
			}
			catch (DAOException e) { }
		}

		this.assertCalls("begin resource1", "rollback resource1", "release resource1");
	}

	@Test
	public void testInnerCloseWithoutCommitMarksOuterForRollback() throws Exception
	{
		try (DAOTransaction outer = this.manager.begin())
		{
			try (DAOTransaction inner = this.manager.begin())
			{
				Assert.assertFalse(inner.isOutermost());
			}

			try
			{
				outer.commit();
				Assert.fail("committed a transaction marked for rollback");
			}
			catch (DAOException e) { }
		}

		this.assertCalls("begin resource1", "rollback resource1", "release resource1");
	}

	@Test
	public void testOuterCommitWithOpenInnerTransactionFailsAndStaysOpen() throws Exception
	{
		try (DAOTransaction outer = this.manager.begin())
		{
			DAOTransaction inner = this.manager.begin();

			try
			{
				outer.commit();
				Assert.fail("committed with an open inner transaction");
			}
			catch (IllegalStateException e) { }

			Assert.assertTrue(this.manager.isActive());

			inner.commit();
			outer.commit();
		}

		this.assertCalls("begin resource1", "commit resource1", "release resource1");
	}

	@Test
	public void testOuterRollbackWithOpenInnerTransactionRollsBack() throws Exception
	{
		DAOTransaction inner;

		try (DAOTransaction outer = this.manager.begin())
		{
			inner = this.manager.begin();

			outer.rollback();
		}

		Assert.assertFalse(this.manager.isActive());

		// the inner transaction has nothing left to roll back
		inner.close();

		this.assertCalls("begin resource1", "rollback resource1", "release resource1");
	}

	@Test
	public void testFailingCommitRollsBackAndReleases() throws Exception
	{
		this.manager.failCommit = true;

		try (DAOTransaction transaction = this.manager.begin())
		{
			try
			{
				transaction.commit();
				Assert.fail("the commit didn't fail");
			}
			catch (DAOException e)
			{
				Assert.assertTrue(e.getCause() instanceof IllegalStateException);
			}
		}

		Assert.assertFalse(this.manager.isActive());
		this.assertCalls("begin resource1", "commit resource1", "rollback resource1", "release resource1");
	}

	@Test
	public void testFailingBeginLeavesNoTransaction() throws Exception
	{
		this.manager.failBegin = true;

		try
		{
			this.manager.begin();
			Assert.fail("the begin didn't fail");
		}
		catch (DAOException e)
		{
			Assert.assertTrue(e.getCause() instanceof IllegalStateException);
		}

		Assert.assertFalse(this.manager.isActive());

		this.manager.failBegin = false;

		try (DAOTransaction transaction = this.manager.begin())
		{
			Assert.assertTrue(transaction.isOutermost());

			transaction.commit();
		}

		this.assertCalls("begin resource1", "commit resource1", "release resource1");
	}

	@Test
	public void testSetRollbackOnlyMarksTransactionOfTheThread() throws Exception
	{
		Assert.assertFalse(AbstractDAOTransactionManager.setRollbackOnly(this.resourceKey));

		try (DAOTransaction transaction = this.manager.begin())
		{
			Assert.assertTrue(AbstractDAOTransactionManager.setRollbackOnly(this.resourceKey));

			try
			{
				transaction.commit();
				Assert.fail("committed a transaction marked for rollback");
			}
			catch (DAOException e) { }
		}

		this.assertCalls("begin resource1", "rollback resource1", "release resource1");
	}

	@Test
	public void testResourceIsBoundToTheThreadUntilCompletion() throws Exception
	{
		Assert.assertNull(AbstractDAOTransactionManager.getBoundResource(this.resourceKey, String.class));

		try (DAOTransaction transaction = this.manager.begin())
		{
			Assert.assertEquals("resource1", AbstractDAOTransactionManager.getBoundResource(this.resourceKey, String.class));
			Assert.assertNull(AbstractDAOTransactionManager.getBoundResource(new Object(), String.class));

			AtomicReference<String> seenByOtherThread = new AtomicReference<>("not run");

			Assert.assertNull(this.runOnOtherThread(() -> seenByOtherThread.set(AbstractDAOTransactionManager.getBoundResource(this.resourceKey, String.class))));
			Assert.assertNull(seenByOtherThread.get());

			transaction.commit();
		}

		Assert.assertNull(AbstractDAOTransactionManager.getBoundResource(this.resourceKey, String.class));
	}

	@Test
	public void testOtherThreadBeginsItsOwnTransaction() throws Exception
	{
		try (DAOTransaction transaction = this.manager.begin())
		{
			Assert.assertNull(this.runOnOtherThread(() ->
			{
				Assert.assertFalse(this.manager.isActive());

				try (DAOTransaction other = this.manager.begin())
				{
					Assert.assertTrue(other.isOutermost());

					other.commit();
				}
			}));

			transaction.commit();
		}

		this.assertCalls("begin resource1", "begin resource2", "commit resource2", "release resource2", "commit resource1", "release resource1");
	}

	@Test
	public void testTransactionCantBeCompletedByOtherThread() throws Exception
	{
		try (DAOTransaction transaction = this.manager.begin())
		{
			Assert.assertTrue(this.runOnOtherThread(transaction::commit) instanceof IllegalStateException);
			Assert.assertTrue(this.runOnOtherThread(transaction::rollback) instanceof IllegalStateException);
			Assert.assertTrue(this.manager.isActive());

			transaction.commit();
		}

		this.assertCalls("begin resource1", "commit resource1", "release resource1");
	}

	@Test
	public void testManagersOfTheSameStorageShareTheTransaction() throws Exception
	{
		RecordingTransactionManager otherManager = new RecordingTransactionManager(this.resourceKey, this.calls);

		try (DAOTransaction outer = this.manager.begin())
		{
			Assert.assertTrue(otherManager.isActive());

			try (DAOTransaction inner = otherManager.begin())
			{
				Assert.assertFalse(inner.isOutermost());

				inner.commit();
			}

			outer.commit();
		}

		this.assertCalls("begin resource1", "commit resource1", "release resource1");
	}

	@Test
	public void testManagersOfOtherStoragesHaveTheirOwnTransactions() throws Exception
	{
		RecordingTransactionManager otherManager = new RecordingTransactionManager(new Object(), this.calls);

		try (DAOTransaction transaction = this.manager.begin())
		{
			Assert.assertFalse(otherManager.isActive());

			try (DAOTransaction other = otherManager.begin())
			{
				Assert.assertTrue(other.isOutermost());

				other.rollback();
			}

			transaction.commit();
		}

		this.assertCalls("begin resource1", "begin resource1", "rollback resource1", "release resource1", "commit resource1", "release resource1");
	}
}
