package com.schoste.ddd.infrastructure.dao.v4.hibernate;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.schoste.ddd.infrastructure.dao.v4.exceptions.DAOException;

/**
 * Tests of the Hibernate DAO which go beyond the DAO contract tests ({@link HibernateDAOContractTest})
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 */
@ContextConfiguration(locations = {
		"classpath:META-INF/module.xml",
		"classpath:META-INF/beans.xml",
		"classpath:db-setup.xml",
		"classpath:HibernateDAOImplTest.xml",
		})
@RunWith(SpringJUnit4ClassRunner.class)
public class HibernateDAOImplTest
{
	@Autowired
	protected HibernateDAO hibernateDAO;

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
		this.hibernateDAO.clear();
	}

	/**
	 * Reads the time stamps of a data object directly from the database (bypassing Hibernate)
	 * and asserts that they equal the time stamps of the given data object.
	 *
	 * @param dataObject the data object to compare the stored time stamps with
	 * @throws Exception re-throws every exception
	 */
	protected void assertStoredTimestamps(HibernateDO dataObject) throws Exception
	{
		try (Connection connection = this.hsqlTestDataSource.getConnection();
			 Statement statement = connection.createStatement();
			 ResultSet resultSet = statement.executeQuery("SELECT created, modified FROM hibernate_do WHERE id = " + dataObject.getId()))
		{
			Assert.assertTrue(resultSet.next());
			Assert.assertEquals(dataObject.getCreatedTimestamp(), resultSet.getLong(1));
			Assert.assertEquals(dataObject.getModifiedTimestamp(), resultSet.getLong(2));
		}
	}

	/**
	 * After inserting and updating, the data object carries the time stamps which are stored in the database columns.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void save_insertAndUpdate_dataObjectCarriesStoredTimestamps() throws Exception
	{
		HibernateDO dataObject = this.hibernateDAO.createDataObject();

		dataObject.setExampleStringProperty("inserted");
		this.hibernateDAO.save(dataObject);
		this.assertStoredTimestamps(dataObject);

		dataObject.setExampleStringProperty("updated");
		this.hibernateDAO.save(dataObject);
		this.assertStoredTimestamps(dataObject);
	}

	/**
	 * The time stamp of a saved data object is the current time of the database's clock. The embedded test database runs in
	 * this JVM, so its clock is the JVM's: the time stamp must be close to it (also proves that the time zone is applied right).
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void save_newDataObject_getsCurrentTimeOfTheDatabase() throws Exception
	{
		HibernateDO dataObject = this.hibernateDAO.createDataObject();

		dataObject.setExampleStringProperty("timestamp");

		long before = System.currentTimeMillis();
		this.hibernateDAO.save(dataObject);
		long after = System.currentTimeMillis();

		Assert.assertTrue("time stamp " + dataObject.getModifiedTimestamp() + " not in [" + before + ", " + after + "] (+/- 1 s)",
				(dataObject.getModifiedTimestamp() >= before - 1000L) && (dataObject.getModifiedTimestamp() <= after + 1000L));
	}

	/**
	 * A new data object which the database refuses (the column of the property is NOT NULL) isn't stored
	 * and keeps its virtual id, so it can be saved again.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void save_newDataObjectRefusedByTheDatabase_keepsItsVirtualId() throws Exception
	{
		HibernateDO dataObject = this.hibernateDAO.createDataObject();
		Long virtualId = dataObject.getId();

		try
		{
			this.hibernateDAO.save(dataObject);
			Assert.fail("saved a data object which violates a constraint");
		}
		catch (DAOException e) { }

		Assert.assertEquals(virtualId, dataObject.getId());

		dataObject.setExampleStringProperty("saved again");
		this.hibernateDAO.save(dataObject);

		Assert.assertFalse(this.hibernateDAO.isVirtualId(dataObject.getId()));
		this.assertStoredTimestamps(dataObject);
	}
}
