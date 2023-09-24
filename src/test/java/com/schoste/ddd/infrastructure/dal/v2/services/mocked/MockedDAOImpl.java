package com.schoste.ddd.infrastructure.dal.v2.services.mocked;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.schoste.ddd.infrastructure.dal.v2.models.MockedDO;
import com.schoste.ddd.infrastructure.dal.v2.services.mocked.GenericMockedDAO;

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
}
