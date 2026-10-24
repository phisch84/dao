package com.schoste.ddd.infrastructure.dao.v4.hibernate;

import java.io.Serializable;

import com.schoste.ddd.infrastructure.dao.v4.services.GenericDAOTest;
import com.schoste.ddd.infrastructure.dao.v4.models.DataObject;
import com.schoste.ddd.infrastructure.dao.v4.services.DAO;

/**
 * Generic test class to test implementations of the {@link AbstractHibernateDAO} class.
 * It adds no test methods of its own. All standard test methods are inherited from {@link GenericDAOTest}.
 *
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 * @param <ID> The class of the data object's identifier
 * @param <DO> The class of the data object
 * @param <DAO> The class of the data access object
 */
public abstract class AbstractHibernateDAOTest <ID extends Serializable, DO extends DataObject<ID>, T extends DAO<ID, DO>> extends GenericDAOTest<ID, DO, T>
{	

}
