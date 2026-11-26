package com.schoste.ddd.infrastructure.dao.v4.hibernate;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.schoste.ddd.infrastructure.dao.v4.exceptions.DAOException;
import com.schoste.ddd.infrastructure.dao.v4.services.DAOTransaction;

/**
 * Tests of transactions of Hibernate DAOs ({@link HibernateDAOTransactionManager}): DAO calls of the thread join the transaction,
 * so that their changes are stored all together or not at all, also across DAOs of the same session factory.
 * The rows are counted directly in the database (bypassing Hibernate), and only while no transaction is open.
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 */
@ContextConfiguration(locations = {
		"classpath:META-INF/module.xml",
		"classpath:META-INF/beans.xml",
		"classpath:db-setup.xml",
		"classpath:HibernateDAOTransactionTest.xml",
		})
@RunWith(SpringJUnit4ClassRunner.class)
public class HibernateDAOTransactionTest
{
	@Autowired
	@Qualifier("HibernateDAO")
	protected HibernateDAO hibernateDAO;

	@Autowired
	@Qualifier("OtherHibernateDAO")
	protected HibernateDAO otherHibernateDAO;

	@Autowired
	protected HibernateDAOTransactionManager transactionManager;

	@Autowired
	protected DataSource hsqlTestDataSource;

	@Before
	public void setUp() throws Exception
	{
		this.hibernateDAO.clear();
	}

	@After
	public void tearDown() throws Exception
	{
		Assert.assertFalse("a test left a transaction bound to the thread", this.transactionManager.isActive());

		this.hibernateDAO.clear();
	}

	protected HibernateDO newDataObject(HibernateDAO dao, String value) throws Exception
	{
		HibernateDO dataObject = dao.createDataObject();

		dataObject.setExampleStringProperty(value);

		return dataObject;
	}

	protected HibernateDO saveDataObject(HibernateDAO dao, String value) throws Exception
	{
		HibernateDO dataObject = this.newDataObject(dao, value);

		dao.save(dataObject);

		return dataObject;
	}

	/**
	 * Gets the stored values of all rows which aren't flagged as deleted, sorted, directly from the database.
	 */
	protected String storedValues() throws Exception
	{
		StringBuilder values = new StringBuilder();

		try (Connection connection = this.hsqlTestDataSource.getConnection();
			 Statement statement = connection.createStatement();
			 ResultSet resultSet = statement.executeQuery("SELECT example_string_property FROM hibernate_do WHERE deleted = FALSE ORDER BY example_string_property"))
		{
			while (resultSet.next()) values.append((values.length() > 0) ? "," : "").append(resultSet.getString(1));
		}

		return values.toString();
	}

	@Test
	public void commit_changesOfSeveralDAOs_storesAll() throws Exception
	{
		try (DAOTransaction transaction = this.transactionManager.begin())
		{
			this.saveDataObject(this.hibernateDAO, "a");
			this.saveDataObject(this.otherHibernateDAO, "b");
			this.hibernateDAO.save(Arrays.asList(this.newDataObject(this.hibernateDAO, "c"), this.newDataObject(this.hibernateDAO, "d")));

			transaction.commit();
		}

		Assert.assertEquals("a,b,c,d", this.storedValues());
	}

	@Test
	public void rollback_changesOfSeveralDAOs_discardsAll() throws Exception
	{
		HibernateDO stored = this.saveDataObject(this.hibernateDAO, "stored");

		try (DAOTransaction transaction = this.transactionManager.begin())
		{
			this.saveDataObject(this.hibernateDAO, "a");
			this.saveDataObject(this.otherHibernateDAO, "b");

			stored.setExampleStringProperty("changed");
			this.otherHibernateDAO.save(stored);

			transaction.rollback();
		}

		Assert.assertEquals("stored", this.storedValues());
	}

	@Test
	public void close_withoutCommit_discardsChanges() throws Exception
	{
		try (DAOTransaction transaction = this.transactionManager.begin())
		{
			this.saveDataObject(this.hibernateDAO, "a");
		}

		Assert.assertEquals("", this.storedValues());
	}

	@Test
	public void rollback_deletion_discardsIt() throws Exception
	{
		HibernateDO stored = this.saveDataObject(this.hibernateDAO, "stored");

		try (DAOTransaction transaction = this.transactionManager.begin())
		{
			this.otherHibernateDAO.deleteById(stored.getId());

			Assert.assertTrue(this.hibernateDAO.get(stored.getId()).isDeleted());
		}

		Assert.assertEquals("stored", this.storedValues());
	}

	@Test
	public void save_newDataObjectInTransaction_getsActualIdBeforeCommit() throws Exception
	{
		try (DAOTransaction transaction = this.transactionManager.begin())
		{
			HibernateDO dataObject = this.saveDataObject(this.hibernateDAO, "a");

			Assert.assertFalse(this.hibernateDAO.isVirtualId(dataObject.getId()));

			// a child can refer to the actual id of its parent before the transaction is committed
			HibernateDO child = this.saveDataObject(this.otherHibernateDAO, "child of " + dataObject.getId());

			Assert.assertNotEquals(dataObject.getId(), child.getId());

			transaction.commit();
		}
	}

	@Test
	public void get_inTransaction_seesItsChanges() throws Exception
	{
		try (DAOTransaction transaction = this.transactionManager.begin())
		{
			HibernateDO a = this.saveDataObject(this.hibernateDAO, "a");
			HibernateDO b = this.saveDataObject(this.otherHibernateDAO, "b");

			Assert.assertEquals("a", this.otherHibernateDAO.get(a.getId()).getExampleStringProperty());

			try (Stream<HibernateDO> all = this.hibernateDAO.getAll())
			{
				Assert.assertEquals("a,b", all.map(HibernateDO::getExampleStringProperty).sorted().collect(Collectors.joining(",")));
			}

			try (Stream<HibernateDO> byIds = this.hibernateDAO.get(Arrays.asList(a.getId(), b.getId())))
			{
				Assert.assertEquals(2, byIds.count());
			}

			try (Stream<HibernateDO> modified = this.hibernateDAO.getModifiedSince(a.getModifiedTimestamp()))
			{
				Assert.assertEquals(2, modified.count());
			}

			transaction.commit();
		}
	}

	@Test
	public void closeStream_inTransaction_keepsTransaction() throws Exception
	{
		try (DAOTransaction transaction = this.transactionManager.begin())
		{
			this.saveDataObject(this.hibernateDAO, "a");

			try (Stream<HibernateDO> all = this.hibernateDAO.getAll())
			{
				Assert.assertEquals(1, all.count());
			}

			this.saveDataObject(this.hibernateDAO, "b");

			transaction.commit();
		}

		Assert.assertEquals("a,b", this.storedValues());
	}

	@Test
	public void get_inTransaction_providesDetachedDataObjects() throws Exception
	{
		HibernateDO stored = this.saveDataObject(this.hibernateDAO, "stored");

		try (DAOTransaction transaction = this.transactionManager.begin())
		{
			HibernateDO read = this.hibernateDAO.get(stored.getId());

			// a change which isn't saved isn't stored by the commit
			read.setExampleStringProperty("changed, but not saved");

			try (Stream<HibernateDO> all = this.hibernateDAO.getAll())
			{
				all.forEach(dataObject -> dataObject.setExampleStringProperty("changed in stream, but not saved"));
			}

			HibernateDO saved = this.saveDataObject(this.hibernateDAO, "saved");

			saved.setExampleStringProperty("changed after save, but not saved");

			transaction.commit();
		}

		Assert.assertEquals("saved,stored", this.storedValues());
	}

	@Test
	public void failedCall_inTransaction_marksItForRollback() throws Exception
	{
		try (DAOTransaction transaction = this.transactionManager.begin())
		{
			this.saveDataObject(this.hibernateDAO, "a");

			try
			{
				// the column of the property is NOT NULL
				this.saveDataObject(this.otherHibernateDAO, null);
				Assert.fail("saved a data object which violates a constraint");
			}
			catch (DAOException e) { }

			try
			{
				transaction.commit();
				Assert.fail("committed a transaction with a failed DAO call");
			}
			catch (DAOException e) { }
		}

		Assert.assertEquals("", this.storedValues());
	}

	@Test
	public void commit_nestedTransactions_onlyOutermostCommits() throws Exception
	{
		try (DAOTransaction outer = this.transactionManager.begin())
		{
			try (DAOTransaction inner = this.transactionManager.begin())
			{
				this.saveDataObject(this.hibernateDAO, "a");

				inner.commit();
			}

			this.saveDataObject(this.hibernateDAO, "b");

			outer.rollback();
		}

		Assert.assertEquals("", this.storedValues());
	}

	@Test
	public void save_withoutTransaction_storesImmediately() throws Exception
	{
		this.saveDataObject(this.hibernateDAO, "a");

		Assert.assertEquals("a", this.storedValues());

		try (DAOTransaction transaction = this.transactionManager.begin())
		{
			transaction.commit();
		}

		this.saveDataObject(this.otherHibernateDAO, "b");

		Assert.assertEquals("a,b", this.storedValues());
	}
}
