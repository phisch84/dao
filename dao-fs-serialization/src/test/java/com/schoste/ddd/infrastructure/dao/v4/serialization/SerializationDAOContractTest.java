package com.schoste.ddd.infrastructure.dao.v4.serialization;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.schoste.ddd.infrastructure.dao.v4.services.AbstractDAOContractTest;

/**
 * Runs the DAO contract tests ({@link AbstractDAOContractTest}) on the serialization DAO
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 */
@ContextConfiguration(locations = { "file:src/test/resources/unittest-beans.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class SerializationDAOContractTest extends AbstractDAOContractTest<Long, SerializationDO, SerializationDAOImpl>
{
	@Autowired
	private SerializationDAOImpl dao;

	@Override
	protected SerializationDAOImpl getDAO() { return this.dao; }

	@Override
	protected void setValue(SerializationDO dataObject, String value) { dataObject.setExampleStringProperty(value); }

	@Override
	protected String getValue(SerializationDO dataObject) { return dataObject.getExampleStringProperty(); }

	@Override
	protected Long getNotExistingId() { return Long.MAX_VALUE; }
}
