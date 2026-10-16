package com.schoste.ddd.infrastructure.dal.v3.services;

import com.schoste.ddd.infrastructure.dal.v3.models.HibernateDO;

/**
 * Example interface to the implementation of the DAO
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 */
public interface HibernateDAO extends GenericDataAccessObject<Long, HibernateDO> 
{
	/**
	 * Creates a new instance of a data object.
	 * Unlike {@link GenericDataAccessObject#createDataObject(Object...)} the data object is neither given a virtual
	 * identifier nor are its time stamps set. Its identifier is null, so it must be set before
	 * {@link GenericDataAccessObject#isVirtualId(java.io.Serializable)} is called with it.
	 *
	 * @return a new data object with a null identifier
	 */
	HibernateDO createDataObject();
}
