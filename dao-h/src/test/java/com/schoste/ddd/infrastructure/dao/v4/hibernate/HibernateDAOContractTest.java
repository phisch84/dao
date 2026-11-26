package com.schoste.ddd.infrastructure.dao.v4.hibernate;

import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.schoste.ddd.infrastructure.dao.v4.services.AbstractDAOContractTest;

/**
 * Runs the DAO contract tests ({@link AbstractDAOContractTest}) on the Hibernate DAO with an in-memory database
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 */
@ContextConfiguration(locations = {
		"file:src/test/resources/META-INF/module.xml",
		"file:src/test/resources/META-INF/beans.xml",
		"file:src/test/resources/db-setup.xml",
		"file:src/test/resources/HibernateDAOImplTest.xml",
		})
@RunWith(SpringJUnit4ClassRunner.class)
public class HibernateDAOContractTest extends AbstractDAOContractTest<Long, HibernateDO, HibernateDAO>
{
	@Autowired
	private HibernateDAO dao;

	@Override
	protected HibernateDAO getDAO() { return this.dao; }

	@Override
	protected void setValue(HibernateDO dataObject, String value) { dataObject.setExampleStringProperty(value); }

	@Override
	protected String getValue(HibernateDO dataObject) { return dataObject.getExampleStringProperty(); }

	@Override
	protected Long getNotExistingId() { return Long.MAX_VALUE; }
}
