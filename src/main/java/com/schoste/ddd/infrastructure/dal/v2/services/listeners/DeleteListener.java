package com.schoste.ddd.infrastructure.dal.v2.services.listeners;

import java.util.Collection;

import com.schoste.ddd.infrastructure.dal.v2.models.GenericDataObject;

/**
 * Interface to a listener which is notified before or after a data object is or was deleted.
 * All the methods are invoked in the same thread which will perform the actual deletion operation.
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 */
public interface DeleteListener<T extends GenericDataObject>
{
	/**
	 * Called by the DAO before deleting the dataObjectToSave.
	 * The listener may change the object. After the methods returns true,
	 * the DAO will delete the dataObjectToSave. If the method returns false, it will
	 * not attempt delete anything.
	 * 
	 * @param dataObjectToDelete the data object to delete
	 * @return true if the DAO should delete the object or false if it should discard it
	 */
	boolean onBeforeDeleting(T dataObjectToDelete);

	/**
	 * Called by the DAO before deleting any of the DOs in dataObjectsToSave.
	 * The listener may change the objects as well as the collection. After the methods returns,
	 * the DAO will delete what it finds in dataObjectsToSave. If the method returns false, it will
	 * not attempt delete anything.
	 * 
	 * @param dataObjectsToDelete a collection of data objects to delete
	 * @return true if the DAO should delete the objects or false if it should discard them
	 */
	boolean onBeforeDeleting(Collection<T> dataObjectsToDelete);

	/**
	 * Called by the DAO before deleting any of the DOs with an id in dataObjectIdsToDelete.
	 * The listener should return the ids which should be actually deleted. After the methods returns,
	 * the DAO will delete what was returned.
	 * 
	 * @param dataObjectIdsToDelete an array of ids of data objects to delete
	 * @return an array of ids of data objects to actually delete
	 */
	int[] onBeforeDeleting(int[] dataObjectIdsToDelete);

	/**
	 * Called by the DAO after the DO was successfully deleted.
	 * 
	 * @param dataObjectDeleted the DO that was deleted in the firing call
	 */
	void onAfterDeleted(T dataObjectDeleted);

	/**
	 * Called by the DAO after the DOs were successfully deleted.
	 * 
	 * @param dataObjectsDeleted a collection of DOs that were deleted in the firing call
	 */
	void onAfterDeleted(Collection<T> dataObjectsDeleted);

	/**
	 * Called by the DAO after the DOs were successfully deleted.
	 * 
	 * @param dataObjectIdsDeleted an array of DO ids that were deleted in the firing call
	 */
	void onAfterDeleted(int[] dataObjectIdsDeleted);
}
