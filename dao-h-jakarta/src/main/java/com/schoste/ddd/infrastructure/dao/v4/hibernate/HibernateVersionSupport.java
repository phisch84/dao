package com.schoste.ddd.infrastructure.dao.v4.hibernate;

import java.time.Instant;

import org.hibernate.Session;

/**
 * The calls of the Hibernate DAO whose API differs between the Hibernate versions; this is the version for Hibernate 6.6 and later.
 * All other classes of the package are shared with the artifact for Hibernate 5.6, which has its own version of this class.
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 */
final class HibernateVersionSupport
{
	/**
	 * Not to be instantiated; the class only has static methods.
	 */
	private HibernateVersionSupport()
	{
	}

	/**
	 * Gets the current time of the database's clock with the HQL query {@code select instant}: the SQL dialect renders it as the
	 * database's current time as point in time. (HQL's {@code current_timestamp} would be the database's local time, which is
	 * wrong if the database's time zone isn't the JVM's, e.g. with SQLite, whose time is in UTC.)
	 *
	 * @param session the open session
	 * @return the current time of the database in milliseconds since 1970-01-01 UTC
	 */
	static long getDatabaseTimestamp(Session session)
	{
		return session.createSelectionQuery("select instant", Instant.class).getSingleResult().toEpochMilli();
	}

	/**
	 * Runs an HQL update or delete statement.
	 *
	 * @param session the open session
	 * @param hql the HQL statement
	 * @return the number of affected entities
	 */
	static int executeUpdate(Session session, String hql)
	{
		return session.createMutationQuery(hql).executeUpdate();
	}
}
