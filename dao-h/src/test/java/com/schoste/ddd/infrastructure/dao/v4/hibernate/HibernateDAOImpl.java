package com.schoste.ddd.infrastructure.dao.v4.hibernate;

import org.hibernate.SessionFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.schoste.ddd.infrastructure.dao.v4.exceptions.DAOException;
import com.schoste.ddd.infrastructure.dao.v4.hibernate.HibernateDO;
import com.schoste.ddd.infrastructure.dao.v4.hibernate.HibernateDAO;

/**
 * Example Hibernate data access object used in unit testing of the AbstractHibernateDAO implementation
 *
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 */
public class HibernateDAOImpl extends AbstractHibernateDAO<Long, HibernateDO> implements HibernateDAO
{
	@Autowired
	protected ApplicationContext applicationContext;
	protected long lastVirtualId=0;

	@Autowired
	protected SessionFactory sessionFactory;

	/**
	 * {@inheritDoc}
	 *
	 * Returns the session factory injected by Spring.
	 */
	protected SessionFactory getSessionFactory()
	{
		return this.sessionFactory;
	}

	/**
	 * {@inheritDoc}
	 *
	 * Negative identifiers are virtual.
	 *
	 * @throws NullPointerException thrown if the id is null
	 */
	@Override
	public boolean isVirtualId(Long id)
	{
		return (id < 0L);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected HibernateDO doCreateDataObject() throws Exception
	{
		try
		{
			HibernateDO dataObj = (HibernateDO) this.applicationContext.getBean(HibernateDO.class);

			return dataObj;
		}
		catch (Exception ex)
		{
			throw new DAOException(ex);
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * Virtual identifiers are negative and strictly decreasing, derived from the current time in milliseconds.
	 */
	@Override
	protected Long doCreateVirtualId() throws Exception
	{
		long virtualId = System.currentTimeMillis()*(-1);

		while (this.lastVirtualId <= virtualId) virtualId--;

		this.lastVirtualId = virtualId;

		return virtualId;
	}
}
