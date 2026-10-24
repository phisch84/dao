package com.schoste.ddd.infrastructure.dao.v4.services.listeners;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.schoste.ddd.infrastructure.dao.v4.models.DataObject;

/**
 * Mocked implementation of a save listener for unit testing
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 */
public class RecordingSaveListener<T extends DataObject<Long>> implements SaveListener<Long, T>
{
	protected Collection<T> dosReceivedBeforeSaving = new ArrayList<>();
	protected Set<T> dosReceivedAfterSaving = new HashSet<>();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onBeforeSave(T dataObjectToSave)
	{
		this.dosReceivedBeforeSaving.add(dataObjectToSave);

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean onBeforeSave(Collection<T> dataObjectsToSave)
	{
		for (T dataObjectToSave : dataObjectsToSave)
		{
			this.dosReceivedBeforeSaving.add(dataObjectToSave);
		}

		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onAfterSave(T dataObjectSaved)
	{
		this.dosReceivedAfterSaving.add(dataObjectSaved);	
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void onAfterSave(Collection<T> dataObjectsSaved)
	{
		for (T dataObjectSaved : dataObjectsSaved) this.dosReceivedAfterSaving.add(dataObjectSaved);	
	}

	/**
	 * Gets all data objects received before they were submitted for saving
	 * 
	 * @return a collection of data objects
	 */
	public Collection<T> getDOsReceivedBeforeSaving()
	{
		return this.dosReceivedBeforeSaving;
	}

	/**
	 * Gets all data objects received after they were saved
	 * 
	 * @return a collection of data objects
	 */
	public Collection<T> getDOsReceivedAfterSaving()
	{
		return this.dosReceivedAfterSaving;
	}
}
