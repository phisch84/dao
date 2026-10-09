package com.schoste.ddd.infrastructure.dal.v3.services.listeners;

import java.io.Serializable;

import com.schoste.ddd.infrastructure.dal.v3.models.GenericDataObject;
import com.schoste.ddd.infrastructure.dal.v3.services.DataObjectLoader;
import com.schoste.ddd.infrastructure.dal.v3.services.GenericDataAccessObject;
import com.schoste.ddd.infrastructure.dal.v3.services.GenericDefaultDataObjectLoader;
import com.schoste.ddd.infrastructure.dal.v3.services.GenericRecentDataObjectLoader;

/**
 * Interface to a listener which is notified before or after a data objects (DOs) are requested from the data access object (DAO)
 * All the methods are invoked in the same thread which will perform the actual get operation.
 * 
 * The actual loading of data objects happens in {@link GenericDefaultDataObjectLoader#tryAdvance(java.util.function.Consumer)}.
 * When implementing the for a DAO the {@link GenericDefaultDataObjectLoader} and {@link GenericRecentDataObjectLoader}
 * calls to {@link GetListener#onAfterGet(GenericDataObject)} must be made there.
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 */
public interface GetListener<ID extends Serializable, DO extends GenericDataObject<ID>>
{
	/**
	 * Always called before requesting a data object with a given id
	 * (see {@link GenericDataAccessObject#get(Serializable)}).
	 * The listener may change the id by returning a new one.
	 *
	 * @param id the id of the data object to get
	 * @return the actual id to get via the DAO
	 */
	ID onBeforeGet(ID id);

	/**
	 * Always called before requesting data objects with given ids
	 * (see {@link GenericDataAccessObject#get(Serializable[])} and {@link GenericDataAccessObject#get(java.util.Collection)}).
	 * The listener may change the ids by returning new ones.
	 *
	 * @param ids the ids of the data object to get. This parameter is null if {@link GenericDataAccessObject#getAll()} is called
	 * @return the actual ids to get via the DAO, or null if {@link GenericDataAccessObject#getAll()} should be called
	 */
	ID[] onBeforeGet(ID[] ids);

	/**
	 * Always called after creating the data object loader and before requesting data objects.
	 * (see {@link GenericDataAccessObject#getAll()} and {@link GenericDataAccessObject#getRecent(long, boolean)}).
	 * The listener may change the data object loader provided as parameter.
	 *
	 * @param dataObjLoader the data object loader that will be used to load the data
	 */
	void onBeforeGet(DataObjectLoader<?, ID, DO> dataObjLoader);

	/**
	 * Called after a data object was loaded and before it is returned to the caller.
	 * The listener may change the DO.
	 * It isn't guaranteed that this method is called for every DO that is loaded from the storage.
	 * 
	 * When calling {@link GenericDataAccessObject#get(Serializable)}, {@link GenericDataAccessObject#get(Serializable[])} and
	 * {@link GenericDataAccessObject#get(java.util.Collection)}) this method is called by the DAO.
	 * 
	 * When calling {@link GenericDataAccessObject#getAll()} and {@link GenericDataAccessObject#getRecent(long, boolean)}
	 * the call is supposed to be made by the {@link GenericDefaultDataObjectLoader} and {@link GenericRecentDataObjectLoader}
	 * implementation. If that happens depends on the actual implementation of the loaders.
	 * 
	 * @param dataObject the data object that was loaded and will be returned
	 */
	DO onAfterGet(DO dataObject);
}
