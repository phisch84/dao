package com.schoste.ddd.infrastructure.dal.v3.services.listeners;

import java.io.Serializable;
import java.util.Collection;

import com.schoste.ddd.infrastructure.dal.v3.models.GenericDataObject;

/**
 * Interface to a listener which is notified before or after a data object is or was saved.
 * All the methods are invoked in the same thread which will perform the actual saving operation.
 * 
 * @param <ID> the data type of the data object's unique identifier
 * @param <DO> the data type of the data object
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 *
 */
public interface SaveListener<ID extends Serializable, DO extends GenericDataObject<ID>>
{
	/**
	 * Called by the DAO before saving the dataObjectToSave.
	 * The listener may change the object. After the methods returns true,
	 * the DAO will save the dataObjectToSave. If the method returns false, it will
	 * not attempt save anything.
	 * 
	 * @param dataObjectToSave the data object to save
	 * @return true if the DAO should save the object or false if it should discard it
	 */
	boolean onBeforeSaving(DO dataObjectToSave);

	/**
	 * Called by the DAO before saving any of the DOs in dataObjectsToSave.
	 * The listener may change the objects as well as the collection. After the methods returns,
	 * the DAO will save what it finds in dataObjectsToSave. If the method returns false, it will
	 * not attempt save anything.
	 * 
	 * @param dataObjectsToSave a collection of data objects to save
	 * @return true if the DAO should save the objects or false if it should discard them
	 */
	boolean onBeforeSaving(Collection<DO> dataObjectsToSave);

	/**
	 * Called by the DAO after the DO was successfully saved.
	 * 
	 * @param dataObjectSaved the DO that was saved in the firing call
	 */
	void onAfterSaved(DO dataObjectSaved);

	/**
	 * Called by the DAO after the DOs were successfully saved.
	 * 
	 * @param dataObjectsSaved a collection of DOs that were saved in the firing call
	 */
	void onAfterSaved(Collection<DO> dataObjectsSaved);
}
