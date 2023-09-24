package com.schoste.ddd.infrastructure.dal.v2.services.listeners;

import java.util.Collection;

import com.schoste.ddd.infrastructure.dal.v2.models.GenericDataObject;

/**
 * Interface to a listener which is notified before or after a data object are reloaded.
 * All the methods are invoked in the same thread which will perform the actual reload operation.
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 */
public interface ReloadListener<T extends GenericDataObject>
{
	/**
	 * Fired before the DAO reloads data objects. The method can object
	 * to the reload by returning false. All objects in dataObjectsToAdd
	 * will be added to the output
	 * 
	 * @param dataObjectsToAdd objects to add to the output
	 * @return true if the DAO should reload, false otherwise
	 */
	boolean onBeforeReload(Collection<T> dataObjectsToAdd);

	/**
	 * Fired after data objects were reloaded.
	 * 
	 * @param dataObjectsReloaded a collection of reloaded data objects which will be returned to the caller
	 */
	void onAfterReload(Collection<T> dataObjectsReloaded);
}
