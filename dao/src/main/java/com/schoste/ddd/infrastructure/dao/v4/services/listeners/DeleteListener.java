package com.schoste.ddd.infrastructure.dao.v4.services.listeners;

import java.io.Serializable;
import java.util.Collection;

import com.schoste.ddd.infrastructure.dao.v4.models.DataObject;
import com.schoste.ddd.infrastructure.dao.v4.services.DAO;

/**
 * Listener which is called before data objects are deleted by a DAO ({@link DAO}) and after they were deleted.
 * It may change the data objects or cancel the deletion. Register it by {@link DAO#registerOnDeleteListener(DeleteListener)};
 * listeners are called in the order of their registration, in the thread which calls the DAO.
 * All methods have default implementations which change nothing.
 *
 * Deleting by ids ({@link DAO#deleteById(Serializable)}, {@link DAO#deleteByIds(Collection)}) loads the data objects first and then
 * calls the collection methods of this interface with them. Data objects which are already deleted or were never saved don't
 * reach the listeners (see {@link DAO#delete(DataObject)}).
 *
 * If a listener cancels the deletion, the listeners registered after it aren't called, nothing is deleted and no
 * after-method is called; the DAO's method returns normally, so a caller which needs to know checks the data objects
 * afterwards (e.g. a new data object which wasn't saved still has its virtual id). An exception thrown by a listener is thrown by the DAO as
 * {@link com.schoste.ddd.infrastructure.dao.v4.exceptions.DAOException}; if it is thrown by an after-method,
 * the data objects have already been deleted.
 *
 * @param <ID> the class of the data objects' ids
 * @param <DO> the class of the data objects
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 */
public interface DeleteListener<ID extends Serializable, DO extends DataObject<ID>>
{
	/**
	 * Called before a data object is deleted ({@link DAO#delete(DataObject)}). The listener may change the data object.
	 *
	 * @param dataObjectToDelete the data object to delete
	 * @return true to continue, false to cancel the deletion
	 */
	default boolean onBeforeDelete(DO dataObjectToDelete) { return true; }

	/**
	 * Called before data objects are deleted ({@link DAO#delete(Collection)}, {@link DAO#deleteById(Serializable)},
	 * {@link DAO#deleteByIds(Collection)}). The listener may change the data objects and the collection; the DAO deletes what
	 * the collection contains afterwards.
	 *
	 * @param dataObjectsToDelete the data objects to delete
	 * @return true to continue, false to cancel the deletion of all of them
	 */
	default boolean onBeforeDelete(Collection<DO> dataObjectsToDelete) { return true; }

	/**
	 * Called after a data object was deleted by {@link DAO#delete(DataObject)}.
	 *
	 * @param dataObjectDeleted the deleted data object
	 */
	default void onAfterDelete(DO dataObjectDeleted) { }

	/**
	 * Called after data objects were deleted by {@link DAO#delete(Collection)}, {@link DAO#deleteById(Serializable)} or
	 * {@link DAO#deleteByIds(Collection)}.
	 *
	 * @param dataObjectsDeleted the deleted data objects
	 */
	default void onAfterDelete(Collection<DO> dataObjectsDeleted) { }
}
