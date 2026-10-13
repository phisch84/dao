package com.schoste.ddd.infrastructure.dal.v3.services.listeners;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.schoste.ddd.infrastructure.dal.v3.models.GenericDataObject;

/**
 * Mocked implementation of a delete listener for unit testing
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 */
public class MockedDeleteListener<T extends GenericDataObject<Long>> implements DeleteListener<Long, T>
{
	protected Collection<T> dosReceivedBeforeDeleting = new ArrayList<T>();
	protected Set<T> dosReceivedAfterDeleting = new HashSet<T>();
	protected Collection<Long> idsReceivedBeforeDeleting = new ArrayList<>();
	protected Set<Long> idsReceivedAfterDeleting = new HashSet<>();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onBeforeDeleting(T dataObjectToDelete)
	{
		this.dosReceivedBeforeDeleting.add(dataObjectToDelete);

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onBeforeDeleting(Collection<T> dataObjectsToDelete)
	{
		for (T dataObjectToDelete : dataObjectsToDelete)
		{
			this.dosReceivedBeforeDeleting.add(dataObjectToDelete);
		}

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Long[] onBeforeDeleting(Long[] dataObjectIdsToDelete)
	{
		for (long dataObjectIdToDelete : dataObjectIdsToDelete) this.idsReceivedBeforeDeleting.add(dataObjectIdToDelete);

		return dataObjectIdsToDelete;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onAfterDeleted(T dataObjectDeleted)
	{
		this.dosReceivedAfterDeleting.add(dataObjectDeleted);	
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onAfterDeleted(Collection<T> dataObjectsDeleted)
	{
		for (T dataObjectDeleted : dataObjectsDeleted) this.dosReceivedAfterDeleting.add(dataObjectDeleted);	
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onAfterDeleted(Long[] dataObjectIdsDeleted)
	{
		for (long dataObjectIdDeleted : dataObjectIdsDeleted) this.idsReceivedAfterDeleting.add(dataObjectIdDeleted);
	}

	/**
	 * Gets all data objects received before they were submitted for deleting
	 * 
	 * @return a collection of data objects
	 */
	public Collection<Long> getDOsReceivedBeforeDeleting()
	{
		Collection<Long> ids = new ArrayList<>();

		ids.addAll(this.idsReceivedBeforeDeleting);

		for (T dataObjectToDelete : this.dosReceivedBeforeDeleting) ids.add(dataObjectToDelete.getId());
		
		return ids;
	}

	/**
	 * Gets all data objects received after they were deleted
	 * 
	 * @return a collection of data objects
	 */
	public Collection<Long> getDOsReceivedAfterDeleting()
	{
		Collection<Long> ids = new ArrayList<>();

		ids.addAll(this.idsReceivedAfterDeleting);

		for (T dataObjectToDelete : this.dosReceivedAfterDeleting) ids.add(dataObjectToDelete.getId());
		
		return ids;
	}
}
