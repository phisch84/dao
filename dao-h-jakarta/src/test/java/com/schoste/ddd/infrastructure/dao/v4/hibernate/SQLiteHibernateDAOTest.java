package com.schoste.ddd.infrastructure.dao.v4.hibernate;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import org.hibernate.SessionFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests of the Hibernate DAO with SQLite, whose {@code current_timestamp} is a text in UTC without time zone: the time stamps
 * must still be the right point in time when the JVM's time zone isn't UTC. Therefore the tests run in a time zone west of UTC
 * (the build servers usually run in UTC, where the error wouldn't show).
 * <p>
 * The database is a temporary file, since every connection to an in-memory SQLite database gets a database of its own.
 * The data objects are mapped in the format of Jakarta Persistence ({@code META-INF/jakarta-entities.xml}), as in the
 * example of the package documentation.
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 */
public class SQLiteHibernateDAOTest
{
	/**
	 * The Hibernate DAO of the other tests, with the session factory of the SQLite database and without Spring.
	 */
	protected static class SQLiteHibernateDAOImpl extends HibernateDAOImpl
	{
		protected SQLiteHibernateDAOImpl(SessionFactory sessionFactory)
		{
			this.sessionFactory = sessionFactory;
		}

		@Override
		protected HibernateDO doCreateDataObject()
		{
			return new HibernateDO();
		}
	}

	protected TimeZone defaultTimeZone;
	protected File databaseFile;
	protected SessionFactory sessionFactory;
	protected HibernateDAO hibernateDAO;

	@Before
	public void setUp() throws Exception
	{
		this.defaultTimeZone = TimeZone.getDefault();

		TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));

		this.databaseFile = File.createTempFile("dao-h-jakarta-", ".sqlite");

		Map<String, Object> settings = new HashMap<>();

		settings.put("hibernate.connection.url", "jdbc:sqlite:" + this.databaseFile.getAbsolutePath());
		settings.put("hibernate.dialect", "org.hibernate.community.dialect.SQLiteDialect");
		settings.put("hibernate.hbm2ddl.auto", "create");

		this.sessionFactory = SessionFactoryBuilder.buildSessionFactory(settings, Arrays.asList("META-INF/jakarta-entities.xml"));
		this.hibernateDAO = new SQLiteHibernateDAOImpl(this.sessionFactory);
	}

	@After
	public void tearDown() throws Exception
	{
		try
		{
			if (this.sessionFactory != null) this.sessionFactory.close();
		}
		finally
		{
			TimeZone.setDefault(this.defaultTimeZone);

			if (this.databaseFile != null) this.databaseFile.delete();
		}
	}

	/**
	 * The time stamp of a saved data object is the current time of the database's clock. SQLite runs in this JVM, so its clock
	 * is the JVM's: the time stamp must be close to it. SQLite's time has whole seconds, so it may be up to one second earlier.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Test
	public void save_jvmTimeZoneWestOfUTC_dataObjectGetsCurrentTime() throws Exception
	{
		HibernateDO dataObject = this.hibernateDAO.createDataObject();

		dataObject.setExampleStringProperty("timestamp");

		long before = System.currentTimeMillis();
		this.hibernateDAO.save(dataObject);
		long after = System.currentTimeMillis();

		Assert.assertFalse(this.hibernateDAO.isVirtualId(dataObject.getId()));
		Assert.assertTrue("time stamp " + dataObject.getModifiedTimestamp() + " not in [" + before + ", " + after + "] (+/- 1 s)",
				(dataObject.getModifiedTimestamp() >= before - 1000L) && (dataObject.getModifiedTimestamp() <= after + 1000L));
		Assert.assertEquals(dataObject.getModifiedTimestamp(), this.hibernateDAO.get(dataObject.getId()).getModifiedTimestamp());
	}
}
