package com.schoste.ddd.infrastructure.dal.v3.services.listeners;

import java.io.Serializable;

import com.schoste.ddd.infrastructure.dal.v3.models.GenericDataObject;
import com.schoste.ddd.infrastructure.dal.v3.services.DataObjectLoader;
import com.schoste.ddd.infrastructure.dal.v3.services.GenericDataAccessObject;
import com.schoste.ddd.infrastructure.dal.v3.services.GenericDefaultDataObjectLoader;
import com.schoste.ddd.infrastructure.dal.v3.services.GenericRecentDataObjectLoader;

/**
 * Interface to a listener which is notified before or after data objects (DOs) are requested from the data access object (DAO).
 * All the methods are invoked in the same thread which will perform the actual get operation.
 *
 * When streams of data objects are requested, the actual loading of data objects happens in the data object loader
 * (see {@link GenericDefaultDataObjectLoader#tryAdvance(java.util.function.Consumer)} and
 * {@link GenericRecentDataObjectLoader#tryAdvance(java.util.function.Consumer)}).
 * When implementing data object loaders for a DAO, calls to {@link GetListener#onAfterGet(GenericDataObject)} are supposed to be made there.
 * Whether that happens depends on the actual implementation of the loaders.
 * 
 * @param <ID> the data type of the data object's unique identifier
 * @param <DO> the data type of the data object
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
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
	default ID onBeforeGet(ID id) { return id; }

	/**
	 * Always called before requesting data objects with given ids
	 * (see {@link GenericDataAccessObject#get(Serializable[])} and {@link GenericDataAccessObject#get(java.util.Collection)}).
	 * The listener may change the ids by returning new ones.
	 *
	 * This method is not called for {@link GenericDataAccessObject#getAll()}.
	 * If null is returned, the DAO stops calling this method on the remaining listeners and returns the result of {@link GenericDataAccessObject#getAll()}.
	 * 
	 * @param ids the ids of the data object to get
	 * @return the actual ids to get via the DAO, or null if {@link GenericDataAccessObject#getAll()} should be called
	 */
	default ID[] onBeforeGet(ID[] ids) { return ids; }

	/**
	 * Always called after creating the data object loader and before requesting data objects.
	 * (see {@link GenericDataAccessObject#get(Serializable[])}, {@link GenericDataAccessObject#get(java.util.Collection)},
	 * {@link GenericDataAccessObject#getAll()} and {@link GenericDataAccessObject#getRecent(long, boolean)}).
	 * The listener may change the state of the data object loader provided as parameter, but it can't replace it.
	 *
	 * @param dataObjLoader the data object loader that will be used to load the data
	 */
	default void onBeforeGet(DataObjectLoader<?, ID, DO> dataObjLoader) { }

	/**
	 * Called after a data object was loaded and before it is returned to the caller.
	 * The listener may change the DO.
	 * It isn't guaranteed that this method is called for every DO that is loaded from the storage.
	 * 
	 * When calling {@link GenericDataAccessObject#get(Serializable)} this method is called by the DAO.
	 * In this case the parameter is null if no data object was found, and the returned value is ignored:
	 * the listener can change the DO, but it can't replace it by another instance.
	 * 
	 * When calling {@link GenericDataAccessObject#get(Serializable[])}, {@link GenericDataAccessObject#get(java.util.Collection)},
	 * {@link GenericDataAccessObject#getAll()} and {@link GenericDataAccessObject#getRecent(long, boolean)}
	 * the call is supposed to be made by the {@link GenericDefaultDataObjectLoader} and {@link GenericRecentDataObjectLoader}
	 * implementation. If that happens depends on the actual implementation of the loaders.
	 * In this case the returned DO is the one that is passed to another listener (if present) and finally returned to the caller.
	 * 
	 * @param dataObject the data object that was loaded and will be returned
	 * @return the data object to return to the caller (usually the one provided as parameter)
	 */
	default DO onAfterGet(DO dataObject) { return dataObject; }
}
