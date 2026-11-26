package com.schoste.ddd.infrastructure.dao.v4.hibernate;

import org.hibernate.Session;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.DbTimestampType;

/**
 * The calls of the Hibernate DAO whose API differs between the Hibernate versions; this is the version for Hibernate 5.6.
 * All other classes of the package are shared with the artifact for Hibernate 6.6 and later, which has its own version of this class.
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
	 * Gets the current time of the database's clock with Hibernate's {@code DbTimestampType}: it queries the time with the select
	 * statement of the session's SQL dialect; if the dialect has none, it falls back to the clock of the host.
	 *
	 * @param session the open session
	 * @return the current time of the database in milliseconds since 1970-01-01 UTC
	 */
	static long getDatabaseTimestamp(Session session)
	{
		return DbTimestampType.INSTANCE.seed((SharedSessionContractImplementor) session).getTime();
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
		return session.createQuery(hql).executeUpdate();
	}
}
