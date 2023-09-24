package com.schoste.ddd.infrastructure.dal.v2.services.listeners;

import java.util.ArrayList;
import java.util.Collection;

import com.schoste.ddd.infrastructure.dal.v2.models.GenericDataObject;

/**
 * Mocked implementation of a reload listener for unit testing
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 */
public class MockedReloadListener<T extends GenericDataObject> implements ReloadListener<T>
{
	protected Collection<T> dataObjectsReloaded = new ArrayList<>();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onBeforeReload(Collection<T> dataObjectsToAdd)
	{
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onAfterReload(Collection<T> dataObjectsReloaded)
	{
		this.dataObjectsReloaded.addAll(dataObjectsReloaded);
	}

	/**
	 * Gets all data objects received after they were deleted
	 * 
	 * @return a collection of data objects
	 */
	public Collection<T> getDOsReceivedAfterReloading()
	{		
		return this.dataObjectsReloaded;
	}
}
