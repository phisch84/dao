package com.schoste.ddd.infrastructure.dao.v4.inmemory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.schoste.ddd.infrastructure.dao.v4.exceptions.DAOException;
import com.schoste.ddd.infrastructure.dao.v4.inmemory.InMemoryDO;

/**
 * Example data access object implementation used in unit testing of the {@link AbstractInMemoryDAO}
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 */
public class InMemoryDAOImpl extends AbstractInMemoryDAO<InMemoryDO>
{
	@Autowired
	protected ApplicationContext applicationContext;
	protected long lastVirtualId=0;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected  InMemoryDO doCreateDataObject() throws DAOException
	{
		try
		{
			InMemoryDO dataObj = (InMemoryDO) this.applicationContext.getBean(InMemoryDO.class);

			return dataObj;
		}
		catch (Exception ex)
		{
			throw new DAOException(ex);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override 
	public boolean isVirtualId(Long id)
	{
		if (id == null) return false;
		if (id >= 0) return false;

		return true;
	}

	/**
	 * {@inheritDoc}
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
