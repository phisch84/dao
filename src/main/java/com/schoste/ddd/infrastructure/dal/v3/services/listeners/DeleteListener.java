package com.schoste.ddd.infrastructure.dal.v3.services.listeners;

import java.io.Serializable;
import java.util.Collection;

import com.schoste.ddd.infrastructure.dal.v3.models.GenericDataObject;

/**
 * Interface to a listener which is notified before or after a data object is or was deleted.
 * All the methods are invoked in the same thread which will perform the actual deletion operation.
 * 
 * @param <ID> the data type of the data object's unique identifier
 * @param <DO> the data type of the data object
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 *
 */
public interface DeleteListener<ID extends Serializable, DO extends GenericDataObject<ID>>
{
	/**
	 * Called by the DAO before deleting the dataObjectToDelete.
	 * The listener may change the object. After the methods returns true,
	 * the DAO will delete the dataObjectToDelete. If the method returns false, it will
	 * not attempt delete anything.
	 * 
	 * @param dataObjectToDelete the data object to delete
	 * @return true if the DAO should delete the object or false if it should discard it
	 */
	boolean onBeforeDeleting(DO dataObjectToDelete);

	/**
	 * Called by the DAO before deleting any of the DOs in dataObjectsToDelete.
	 * The listener may change the objects as well as the collection. After the methods returns,
	 * the DAO will delete what it finds in dataObjectsToDelete. If the method returns false, it will
	 * not attempt delete anything.
	 * 
	 * @param dataObjectsToDelete a collection of data objects to delete
	 * @return true if the DAO should delete the objects or false if it should discard them
	 */
	boolean onBeforeDeleting(Collection<DO> dataObjectsToDelete);

	/**
	 * Called by the DAO before deleting any of the DOs with an id in dataObjectIdsToDelete.
	 * The listener should return the ids which should be actually deleted. After the methods returns,
	 * the DAO will delete what was returned. The listener must not return null.
	 * This method is called when data objects are deleted by their ids, also if only one id was given
	 * (then with an array of one id).
	 * 
	 * @param dataObjectIdsToDelete an array of ids of data objects to delete
	 * @return an array of ids of data objects to actually delete
	 */
	ID[] onBeforeDeleting(ID[] dataObjectIdsToDelete);

	/**
	 * Called by the DAO after the DO was successfully deleted.
	 * 
	 * @param dataObjectDeleted the DO that was deleted in the firing call
	 */
	void onAfterDeleted(DO dataObjectDeleted);

	/**
	 * Called by the DAO after the DOs were successfully deleted.
	 * 
	 * @param dataObjectsDeleted a collection of DOs that were deleted in the firing call
	 */
	void onAfterDeleted(Collection<DO> dataObjectsDeleted);

	/**
	 * Called by the DAO after the DOs were successfully deleted.
	 * 
	 * @param dataObjectIdsDeleted an array of DO ids that were deleted in the firing call
	 */
	void onAfterDeleted(ID[] dataObjectIdsDeleted);
}
