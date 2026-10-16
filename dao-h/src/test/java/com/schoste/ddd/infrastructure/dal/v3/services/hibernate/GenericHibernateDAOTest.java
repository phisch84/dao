package com.schoste.ddd.infrastructure.dal.v3.services.hibernate;

import java.io.Serializable;

import com.schoste.ddd.infrastructure.dal.v3.services.GenericDAOTest;
import com.schoste.ddd.infrastructure.dal.v3.models.GenericDataObject;
import com.schoste.ddd.infrastructure.dal.v3.services.GenericDataAccessObject;

/**
 * Generic test class to test implementations of the {@link GenericHibernateDAO} class.
 * It adds no test methods of its own. All standard test methods are inherited from {@link GenericDAOTest}.
 *
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 * @param <ID> The class of the data object's identifier
 * @param <DO> The class of the data object
 * @param <DAO> The class of the data access object
 */
public abstract class GenericHibernateDAOTest <ID extends Serializable, DO extends GenericDataObject<ID>, DAO extends GenericDataAccessObject<ID, DO>> extends GenericDAOTest<ID, DO, DAO>
{	

}
