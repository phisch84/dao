package com.schoste.ddd.infrastructure.dao.v4.inmemory;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.schoste.ddd.infrastructure.dao.v4.services.AbstractDAOContractTest;

/**
 * Runs the DAO contract tests ({@link AbstractDAOContractTest}) on the in-memory DAO
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 */
@ContextConfiguration(locations = { "file:src/test/resources/unittest-beans.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class InMemoryDAOContractTest extends AbstractDAOContractTest<Long, InMemoryDO, InMemoryDAOImpl>
{
	@Autowired
	private InMemoryDAOImpl dao;

	@Override
	protected InMemoryDAOImpl getDAO() { return this.dao; }

	@Override
	protected void setValue(InMemoryDO dataObject, String value) { dataObject.setExampleStringProperty(value); }

	@Override
	protected String getValue(InMemoryDO dataObject) { return dataObject.getExampleStringProperty(); }

	@Override
	protected Long getNotExistingId() { return Long.MAX_VALUE; }
}
