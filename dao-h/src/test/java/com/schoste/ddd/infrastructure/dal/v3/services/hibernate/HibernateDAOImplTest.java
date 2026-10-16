package com.schoste.ddd.infrastructure.dal.v3.services.hibernate;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.schoste.ddd.infrastructure.dal.v3.models.HibernateDO;
import com.schoste.ddd.infrastructure.dal.v3.services.HibernateDAO;

/**
 * Test class of the HibernateDAOImpl implementation
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 */
@ContextConfiguration(locations = {
		"file:src/test/resources/META-INF/module.xml",
		"file:src/test/resources/META-INF/beans.xml",
		"file:src/test/resources/db-setup.xml",
		"file:src/test/resources/HibernateDAOImplTest.xml",
		})
@RunWith(SpringJUnit4ClassRunner.class)
public class HibernateDAOImplTest extends GenericHibernateDAOTest<Long, HibernateDO, HibernateDAO>
{
	@Autowired
	protected HibernateDAO hibernateDAO;

	@Autowired
	protected DataSource hsqlTestDataSource;

	protected Map<Long, Long> idMap = new HashMap<>();

	/**
	 * Prepares the storage before each test: clears it and then saves ten data objects
	 * (original ids 1 to 10) whose actual ids are mapped in {@link #idMap}.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Before
	public void clear() throws Exception
	{
		this.getDataAccessObject().clear();
		this.createAndMapDataobject(1L, "testSaveExisting");
		this.createAndMapDataobject(2L, "@Before01");
		this.createAndMapDataobject(3L, "@Before02");
		this.createAndMapDataobject(4L, "@Before03");
		this.createAndMapDataobject(5L, "@Before04");
		this.createAndMapDataobject(6L, "@Before05");
		this.createAndMapDataobject(7L, "@Before06");
		this.createAndMapDataobject(8L, "@Before07");
		this.createAndMapDataobject(9L, "@Before08");
		this.createAndMapDataobject(10L, "@Before09");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected HibernateDAO getDataAccessObject() 
	{
		return this.hibernateDAO;
	}

	/**
	 * Creates a data object (see {@link #createDataObject(Object...)}), saves it and maps the original id
	 * (the first parameter) to the actual id the data object was given when it was saved.
	 *
	 * @param parameters the parameters passed to {@link #createDataObject(Object...)}. The first one must be the original id (a Long)
	 * @throws IllegalArgumentException thrown if no parameters are given
	 * @throws Exception re-throws every exception
	 */
	protected void createAndMapDataobject(Object... parameters) throws Exception
	{
		if ((parameters == null) || (parameters.length < 1)) throw new IllegalArgumentException("parameters");

		Long originalId = (Long) parameters[0];
		HibernateDO dataObject = this.createDataObject(parameters);

		this.getDataAccessObject().save(dataObject);
		this.idMap.put(originalId, dataObject.getId());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected HibernateDO createDataObject(Object... parameters) 
	{
		HibernateDO dataObject = this.hibernateDAO.createDataObject();
		
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
	protected void modifyDataObject(HibernateDO dataObject, Object... parameters) 
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
	protected HibernateDO getExistingDataObject(long id)
	{
		try
		{
			if (!this.idMap.containsKey(id)) return null;

			long actualId = this.idMap.get(id);

			return this.getDataAccessObject().get(actualId);
		}
		catch (Exception e)
		{
			e.printStackTrace(System.err);
			
			return null;
		}
	}

	/**
	 * Clears the repository. Does not verify the result: returns true unless clearing throws an exception.
	 *
	 * @return always true
	 * @throws Exception re-throws every exception
	 */
	@Override
	protected boolean clearRepositorySucceeded() throws Exception
	{
		this.getDataAccessObject().clear();

		return true;
	}

	/**
	 * Not implemented yet: does not assert anything.
	 *
	 * @param numOfexpectedDOs not used
	 * @return always true
	 */
	@Override
	protected boolean assertDefaultGetListenersBeforeGet(int numOfexpectedDOs)
	{
		// TODO Auto-generated method stub
		return true;
	}

	/**
	 * Not implemented yet: does not assert anything.
	 *
	 * @param numOfexpectedDOs not used
	 * @return always true
	 */
	@Override
	protected boolean assertDefaultGetListenersAfterGet(int numOfexpectedDOs) 
	{
		// TODO Auto-generated method stub
		return true;
	}

	/**
	 * Not implemented yet: does not assert anything.
	 *
	 * @param numOfexpectedDOs not used
	 * @return always true
	 */
	@Override
	protected boolean assertDefaultSaveListenersBeforeSave(int numOfexpectedDOs) 
	{
		// TODO Auto-generated method stub
		return true;
	}

	/**
	 * Not implemented yet: does not assert anything.
	 *
	 * @param numOfexpectedDOs not used
	 * @return always true
	 */
	@Override
	protected boolean assertDefaultSaveListenersAfterSave(int numOfexpectedDOs) 
	{
		// TODO Auto-generated method stub
		return true;
	}

	/**
	 * Not implemented yet: does not assert anything.
	 *
	 * @param numOfexpectedDOs not used
	 * @return always true
	 */
	@Override
	protected boolean assertDefaultDeleteListenersBeforeDelete(int numOfexpectedDOs) 
	{
		// TODO Auto-generated method stub
		return true;
	}

	/**
	 * Not implemented yet: does not assert anything.
	 *
	 * @param numOfexpectedDOs not used
	 * @return always true
	 */
	@Override
	protected boolean assertDefaultDeleteListenersAfterDelete(int numOfexpectedDOs) 
	{
		// TODO Auto-generated method stub
		return true;
	}

	/**
	 * Not implemented yet: does not assert anything.
	 *
	 * @param arg0 not used
	 * @return always true
	 */
	@Override
	protected boolean assertDefaultGetListenersDOLsBeforeGet(int arg0) 
	{
		// TODO Auto-generated method stub
		return true;
	}

	/**
	 * Reads the time stamps of a data object directly from the storage (bypassing Hibernate)
	 * and asserts that they equal the time stamps of the given data object.
	 *
	 * @param dataObject the data object to compare the stored time stamps with
	 * @throws Exception re-throws every exception
	 */
	protected void assertStoredTimeStamps(HibernateDO dataObject) throws Exception
	{
		try (Connection connection = this.hsqlTestDataSource.getConnection();
			 Statement statement = connection.createStatement();
			 ResultSet resultSet = statement.executeQuery("SELECT created, modified FROM hibernate_do WHERE id = " + dataObject.getId()))
		{
			Assert.assertTrue(resultSet.next());
			Assert.assertEquals(dataObject.getCreatedTimeStamp(), resultSet.getLong(1));
			Assert.assertEquals(dataObject.getModifiedTimeStamp(), resultSet.getLong(2));
		}
	}

	/**
	 * Asserts that the time stamps of the data object are the ones stored when it is inserted and updated via the DAO
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void testInsertAndUpdateStoreTimeStampsOfDataObject() throws Exception
	{
		HibernateDO dataObject = this.getDataAccessObject().createDataObject();
		dataObject.setExampleStringProperty("testInsertAndUpdateStoreTimeStampsOfDataObject");

		this.getDataAccessObject().save(dataObject);
		this.assertStoredTimeStamps(dataObject);

		Thread.sleep(10);

		dataObject.setExampleStringProperty("testInsertAndUpdateStoreTimeStampsOfDataObject changed");

		this.getDataAccessObject().save(dataObject);
		this.assertStoredTimeStamps(dataObject);
	}
}
