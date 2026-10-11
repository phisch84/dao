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

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected DataObjectLoader<?, Integer, MockedDO> createDOLoader(GenericDataAccessObject<Integer, MockedDO> dataAccessObject, Function<?, MockedDO> sourceRecordToDataObjConversionFn, Object... args) throws Exception
	{
		Function<?, MockedDO> cf = (sourceRecordToDataObjConversionFn != null) ? sourceRecordToDataObjConversionFn : this.defaultSRtoDOConversionFn;
		DataObjectLoader<?, Integer, MockedDO> dol = this.applicationContext.getBean(DefaultDataObjectLoader.class, this, cf);

		return dol;
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected DataObjectLoader<?, Integer, MockedDO> createDOLoader(GenericDataAccessObject<Integer, MockedDO> dataAccessObject, Function<?, MockedDO> sourceRecordToDataObjConversionFn, Set<Integer> ids, Object... args) throws Exception
	{
		Function<?, MockedDO> cf = (sourceRecordToDataObjConversionFn != null) ? sourceRecordToDataObjConversionFn : this.defaultSRtoDOConversionFn;
		DataObjectLoader<?, Integer, MockedDO> dol = this.applicationContext.getBean(DefaultDataObjectLoader.class, this,  cf, ids);

		return dol;
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected RecentDataObjectLoader<?, Integer, MockedDO> createRecentDOLoader(GenericDataAccessObject<Integer, MockedDO> dataAccessObject, Function<?, MockedDO> sourceRecordToDataObjConversionFn, long lastModTS, boolean skipDeleted, Object... args) throws Exception
	{
		Function<?, MockedDO> cf = (sourceRecordToDataObjConversionFn != null) ? sourceRecordToDataObjConversionFn : this.defaultSRtoDOConversionFn;
		RecentDataObjectLoader<?, Integer, MockedDO> dol = this.applicationContext.getBean(RecentDataObjectLoader.class, this, cf, lastModTS, skipDeleted);

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
	public boolean isVirtualId(Integer id)
	{
		if (id == null) return false;
		if (id >= 0) return false;

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected Integer getNextVirtualId(Object... args) throws Exception 
	{
		Integer virtualId = (int) (System.nanoTime() / 1000);
				virtualId = (virtualId < 0) ? virtualId : virtualId*(-1);

		return virtualId;
	}

	/**
	 * {@inheritDoc}
	 */

	@Override
	protected Integer getActualId(Object... args) throws Exception 
	{
		Integer actualId = (int) (System.nanoTime() / 1000);

		return actualId;
	}
}
