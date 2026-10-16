package com.schoste.ddd.infrastructure.dal.v3.services.hibernate;

import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;

import com.schoste.ddd.infrastructure.dal.v3.models.GenericDataObject;

/**
 * Listener class for data objects which is used by Hibernate.
 * Used to intercept inserts and updates (including those from outside of the DAO - if any)
 * and to update the time stamps of the data object.
 *
 * Note: Hibernate builds the SQL statements from the state array of the event, not from the entity's fields.
 * The time stamps set by this listener are therefore set on the entity instance, but are not guaranteed to be
 * written to the storage. {@link GenericHibernateDAO} updates the time stamps itself before saving.
 *
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 */
public class GenericDataObjectListener implements PreInsertEventListener, PreUpdateEventListener
{
	private static final long serialVersionUID = -8040039180526528598L;

	/**
	 * Updates the modification and creation time fields of a data object
	 * before the data object is updated (see {@link GenericDataObject#updateTimeStamps()}).
	 *
	 * @param event event arguments (containing the entity to be updated)
	 * @return true (veto against processing) if the event contains no entity, false (no veto) otherwise
	 * @throws ClassCastException thrown if the entity is not a {@link GenericDataObject}
	 */
	@Override
	public boolean onPreUpdate(PreUpdateEvent event)
	{
		return updateTimeStamps(event.getEntity());
	}

	/**
	 * Updates the modification and creation time fields of a data object
	 * before the data object is inserted (see {@link GenericDataObject#updateTimeStamps()}).
	 *
	 * @param event event arguments (containing the entity to be inserted)
	 * @return true (veto against processing) if the event contains no entity, false (no veto) otherwise
	 * @throws ClassCastException thrown if the entity is not a {@link GenericDataObject}
	 */
	@Override
	public boolean onPreInsert(PreInsertEvent event)
	{
		return updateTimeStamps(event.getEntity());
	}

	/**
	 * Updates the modification and creation time fields of an entity (see {@link GenericDataObject#updateTimeStamps()}).
	 *
	 * @param entity the entity to be inserted or updated
	 * @return true (veto against processing) if there is no entity, false (no veto) otherwise
	 * @throws ClassCastException thrown if the entity is not a {@link GenericDataObject}
	 */
	private static boolean updateTimeStamps(Object entity)
	{
		if (entity == null) return true;

		((GenericDataObject<?>) entity).updateTimeStamps();

		return false;
	}
}
