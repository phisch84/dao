package com.schoste.ddd.infrastructure.dal.v3.services.mocked;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.schoste.ddd.infrastructure.dal.v3.exceptions.DALException;
import com.schoste.ddd.infrastructure.dal.v3.models.MockedDO;

/**
 * Example data access object implementation used in unit testing of the {@link GenericMockedDAO}
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 */
public class MockedDAOImpl extends GenericMockedDAO<MockedDO>
{
	@Autowired
	protected ApplicationContext applicationContext;
	protected long lastVirtualId=0;

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected  MockedDO doCreateDataObject(Object... args) throws DALException
	{
		try
		{
			MockedDO dataObj = (MockedDO) this.applicationContext.getBean(MockedDO.class);

			return dataObj;
		}
		catch (Exception ex)
		{
			throw new DALException(ex);
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
	protected Long getNextVirtualId(Object... args) throws Exception 
	{
		long virtualId = System.currentTimeMillis()*(-1);

		while (this.lastVirtualId <= virtualId) virtualId--;

		this.lastVirtualId = virtualId;

		return virtualId;
	}

	/**
	 * {@inheritDoc}
	 */

	@Override
	protected Long getActualId(Object... args) throws Exception 
	{
		Long actualId = System.currentTimeMillis();

		return actualId;
	}
}
