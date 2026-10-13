package com.schoste.ddd.infrastructure.dal.v3.services.mocked;

import java.util.Set;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.schoste.ddd.infrastructure.dal.v3.exceptions.DALException;
import com.schoste.ddd.infrastructure.dal.v3.models.MockedDO;
import com.schoste.ddd.infrastructure.dal.v3.services.DataObjectLoader;
import com.schoste.ddd.infrastructure.dal.v3.services.DefaultDataObjectLoader;
import com.schoste.ddd.infrastructure.dal.v3.services.GenericDataAccessObject;
import com.schoste.ddd.infrastructure.dal.v3.services.RecentDataObjectLoader;

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
	@SuppressWarnings("unchecked")
	@Override
	protected DataObjectLoader<?, Long, MockedDO> createDOLoader(GenericDataAccessObject<Long, MockedDO> dataAccessObject, Function<?, MockedDO> sourceRecordToDataObjConversionFn, Object... args) throws Exception
	{
		Function<?, MockedDO> cf = (sourceRecordToDataObjConversionFn != null) ? sourceRecordToDataObjConversionFn : this.defaultSRtoDOConversionFn;
		DataObjectLoader<?, Long, MockedDO> dol = this.applicationContext.getBean(DefaultDataObjectLoader.class, this, cf);

		return dol;
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected DataObjectLoader<?, Long, MockedDO> createDOLoader(GenericDataAccessObject<Long, MockedDO> dataAccessObject, Function<?, MockedDO> sourceRecordToDataObjConversionFn, Set<Long> ids, Object... args) throws Exception
	{
		Function<?, MockedDO> cf = (sourceRecordToDataObjConversionFn != null) ? sourceRecordToDataObjConversionFn : this.defaultSRtoDOConversionFn;
		DataObjectLoader<?, Long, MockedDO> dol = this.applicationContext.getBean(DefaultDataObjectLoader.class, this,  cf, ids);

		return dol;
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected RecentDataObjectLoader<?, Long, MockedDO> createRecentDOLoader(GenericDataAccessObject<Long, MockedDO> dataAccessObject, Function<?, MockedDO> sourceRecordToDataObjConversionFn, long lastModTS, boolean skipDeleted, Object... args) throws Exception
	{
		Function<?, MockedDO> cf = (sourceRecordToDataObjConversionFn != null) ? sourceRecordToDataObjConversionFn : this.defaultSRtoDOConversionFn;
		RecentDataObjectLoader<?, Long, MockedDO> dol = this.applicationContext.getBean(RecentDataObjectLoader.class, this, cf, lastModTS, skipDeleted);

		return dol;
	}


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
