package com.schoste.ddd.infrastructure.dal.v2.services.listeners;

import java.util.Collection;

import com.schoste.ddd.infrastructure.dal.v2.models.GenericDataObject;
import com.schoste.ddd.infrastructure.dal.v2.services.GenericDataAccessObject;

/**
 * Interface to a listener which is notified before or after a data objects are requested.
 * All the methods are invoked in the same thread which will perform the actual get operation.
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 */
public interface GetListener<T extends GenericDataObject>
{
	/**
	 * Called before requesting a data object with a given id.
	 * The listener may change the id by returning a new one.
	 *
	 * @param id the id of the data object to get
	 * @return the actual id to get via the DAO
	 */
	int onBeforeGet(int id);

	/**
	 * Called before requesting data objects with given ids.
	 * The listener may change the ids by returning new ones.
	 *
	 * @param ids the ids of the data object to get. This parameter is null if {@link GenericDataAccessObject#getAll()} is called
	 * @return the actual ids to get via the DAO, or null if {@link GenericDataAccessObject#getAll()} should be called
	 */
	int[] onBeforeGet(int[] ids);

	/**
	 * Called after a data object was loaded. The listener may change the DO
	 * 
	 * @param dataObject the data object that was loaded and will be returned
	 */
	void onAfterGet(T dataObject);

	/**
	 * Called after data objects were loaded. The listener may change the DOs
	 * 
	 * @param dataObjects the data objects that were loaded and will be returned
	 */
	void onAfterGet(Collection<T> dataObjects);
}
