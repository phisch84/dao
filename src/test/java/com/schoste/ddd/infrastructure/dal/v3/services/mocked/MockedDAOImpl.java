package com.schoste.ddd.infrastructure.dal.v3.services.mocked;

import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.schoste.ddd.infrastructure.dal.v3.models.MockedDO;
import com.schoste.ddd.infrastructure.dal.v3.services.DataObjectLoader;

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
	 * Creates a new data object
	 * 
	 * @return an instance to a new data object
	 */
	public MockedDO createDataObject()
	{
		return (MockedDO) this.applicationContext.getBean(MockedDO.class);
	}

	protected MockedDO safeGet(int id)
	{
		try
		{
			return this.dataObjects.get(id);
		}
		catch (Exception e)
		{
			e.printStackTrace(System.err);

			return null;
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected DataObjectLoader<Integer, MockedDO> createDOLoader() throws Exception 
	{
		Function<Integer, MockedDO> cf = id -> this.safeGet(id);
		DataObjectLoader<Integer, MockedDO> ll = this.applicationContext.getBean(DataObjectLoader.class, cf, this.dataObjects.keySet());

		return ll;
	}
}
