package com.schoste.ddd.infrastructure.dao.v4.services.listeners;

import java.io.Serializable;
import java.util.Collection;

import com.schoste.ddd.infrastructure.dao.v4.models.DataObject;
import com.schoste.ddd.infrastructure.dao.v4.services.DAO;

/**
 * Listener which is called before data objects are saved by a DAO ({@link DAO}) and after they were saved.
 * It may change the data objects or cancel the saving. Register it by {@link DAO#registerOnSaveListener(SaveListener)};
 * listeners are called in the order of their registration, in the thread which calls the DAO.
 * All methods have default implementations which change nothing.
 *
 * If a listener cancels the saving, the listeners registered after it aren't called, nothing is saved and no
 * after-method is called; the DAO's method returns normally, so a caller which needs to know checks the data objects
 * afterwards (e.g. a new data object which wasn't saved still has its virtual id). An exception thrown by a listener is thrown by the DAO as
 * {@link com.schoste.ddd.infrastructure.dao.v4.exceptions.DAOException}; if it is thrown by an after-method,
 * the data objects have already been saved.
 *
 * @param <ID> the class of the data objects' ids
 * @param <DO> the class of the data objects
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 */
public interface SaveListener<ID extends Serializable, DO extends DataObject<ID>>
{
	/**
	 * Called before a data object is saved ({@link DAO#save(DataObject)}). The listener may change the data object.
	 *
	 * @param dataObjectToSave the data object to save
	 * @return true to continue, false to cancel the saving
	 */
	default boolean onBeforeSave(DO dataObjectToSave) { return true; }

	/**
	 * Called before data objects are saved ({@link DAO#save(Collection)}). The listener may change the data objects
	 * and the collection; the DAO saves what the collection contains afterwards.
	 *
	 * @param dataObjectsToSave the data objects to save
	 * @return true to continue, false to cancel the saving of all of them
	 */
	default boolean onBeforeSave(Collection<DO> dataObjectsToSave) { return true; }

	/**
	 * Called after a data object was saved by {@link DAO#save(DataObject)}.
	 *
	 * @param dataObjectSaved the saved data object
	 */
	default void onAfterSave(DO dataObjectSaved) { }

	/**
	 * Called after data objects were saved by {@link DAO#save(Collection)}.
	 *
	 * @param dataObjectsSaved the saved data objects
	 */
	default void onAfterSave(Collection<DO> dataObjectsSaved) { }
}
