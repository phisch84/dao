package com.schoste.ddd.infrastructure.dal.v2.services.listeners;

import java.util.ArrayList;
import java.util.Collection;
import com.schoste.ddd.infrastructure.dal.v2.models.GenericDataObject;

/**
 * Mocked implementation of a get listener for unit testing
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 */
public class MockedGetListener<T extends GenericDataObject> implements GetListener<T>
{
	protected Collection<Integer> idsReceivedBeforeGetting = new ArrayList<>();
	protected Collection<T> dosReceivedAfterGetting = new ArrayList<T>();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int onBeforeGet(int id)
	{
		this.idsReceivedBeforeGetting.add(id);

		return id;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int[] onBeforeGet(int[] ids)
	{
		if (ids == null) return null;

		for (int id : ids) this.idsReceivedBeforeGetting.add(id);

		return ids;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onAfterGet(T dataObject)
	{
		this.dosReceivedAfterGetting.add(dataObject);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onAfterGet(Collection<T> dataObjects)
	{
		for (T dataObject : dataObjects) this.dosReceivedAfterGetting.add(dataObject);
	}

	/**
	 * Gets all data object ids received before they were submitted for getting
	 * 
	 * @return a collection of data object ids
	 */
	public Collection<Integer> getIdsReceivedBeforeGetting()
	{
		return this.idsReceivedBeforeGetting;
	}

	/**
	 * Gets all data objects received after they were gotten
	 * 
	 * @return a collection of data objects
	 */
	public Collection<T> getDOsReceivedAfterGetting()
	{
		return this.dosReceivedAfterGetting;
	}
}
