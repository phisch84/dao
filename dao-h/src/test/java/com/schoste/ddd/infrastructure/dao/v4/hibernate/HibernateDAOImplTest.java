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

/**
 * Tests of the Hibernate DAO which go beyond the DAO contract tests ({@link HibernateDAOContractTest})
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 */
@ContextConfiguration(locations = {
		"file:src/test/resources/META-INF/module.xml",
		"file:src/test/resources/META-INF/beans.xml",
		"file:src/test/resources/db-setup.xml",
		"file:src/test/resources/HibernateDAOImplTest.xml",
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
}
