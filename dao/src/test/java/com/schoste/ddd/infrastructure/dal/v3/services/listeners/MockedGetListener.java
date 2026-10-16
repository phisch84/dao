package com.schoste.ddd.infrastructure.dal.v3.services.listeners;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import com.schoste.ddd.infrastructure.dal.v3.models.GenericDataObject;
import com.schoste.ddd.infrastructure.dal.v3.services.DataObjectLoader;

/**
 * Mocked implementation of a get listener for unit testing
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 */
public class MockedGetListener<DO extends GenericDataObject<Long>> implements GetListener<Long, DO>
{
	protected Collection<Long> idsReceivedBeforeGetting = new ArrayList<>();
	protected Collection<DO> dosReceivedAfterGetting = new ArrayList<>();
	protected Collection<DataObjectLoader<?, Long, DO>> dolsReceivedBeforeGetting = new HashSet<>();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Long onBeforeGet(Long id)
	{
		this.idsReceivedBeforeGetting.add(id);

		return id;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Long[] onBeforeGet(Long[] ids)
	{
		if (ids == null) return null;

		for (long id : ids) this.idsReceivedBeforeGetting.add(id);

		return ids;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * Besides the data object loader, this implementation also records a null in the collection of the ids
	 * received before getting (see {@link MockedGetListener#getIdsReceivedBeforeGetting()}).
	 */
	@Override
	public void onBeforeGet(DataObjectLoader<?, Long, DO> dataObjLoader)
	{
		this.dolsReceivedBeforeGetting.add(dataObjLoader);
		this.idsReceivedBeforeGetting.add(null);

		return;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DO onAfterGet(DO dataObject)
	{
		this.dosReceivedAfterGetting.add(dataObject);

		return dataObject;
	}

	/**
	 * Gets all data object ids received before they were submitted for getting.
	 * The collection contains one null for every data object loader that was received.
	 * 
	 * @return a collection of data object ids
	 */
	public Collection<Long> getIdsReceivedBeforeGetting()
	{
		return this.idsReceivedBeforeGetting;
	}

	/**
	 * Gets all data object loaders received before any data objects have been loaded
	 * 
	 * @return a collection of data object loaders
	 */
	public Collection<DataObjectLoader<?, Long, DO>> getDOLsReceivedBeforeGetting()
	{
		return this.dolsReceivedBeforeGetting;
	}

	/**
	 * Gets all data objects received after they were gotten
	 * 
	 * @return a collection of data objects
	 */
	public Collection<DO> getDOsReceivedAfterGetting()
	{
		return this.dosReceivedAfterGetting;
	}
}
