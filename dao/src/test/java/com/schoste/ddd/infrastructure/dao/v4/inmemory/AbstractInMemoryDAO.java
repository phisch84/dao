package com.schoste.ddd.infrastructure.dao.v4.inmemory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.schoste.ddd.infrastructure.dao.v4.models.DataObject;
import com.schoste.ddd.infrastructure.dao.v4.services.AbstractDAO;
import com.schoste.ddd.infrastructure.dao.v4.services.DataObjectLoader;
import com.schoste.ddd.infrastructure.dao.v4.services.DeletedDataObjects;
import com.schoste.ddd.infrastructure.dao.v4.services.SourceRecordDataObjectLoader;

/**
 * DAO (Data Access Object) which keeps its data objects in memory, e.g. for tests of code which uses DAOs.
 * It fulfills the whole contract of {@link com.schoste.ddd.infrastructure.dao.v4.services.DAO} (it passes the DAO contract tests)
 * and behaves like a real storage: it stores copies of the saved data objects and returns copies, so changes to a data object
 * only reach the storage when it is saved. Its clock is the clock of the host. Data objects are deleted logically.
 *
 * Actual ids are 1, 2, 3, ... in the order of the first saving; {@link #clear()} starts again with 1.
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 *
 * @param <T> the class of the data objects
 */
public abstract class AbstractInMemoryDAO<T extends DataObject<Long>> extends AbstractDAO<Long, T>
{
	/**
	 * The stored data objects (copies) by their ids, in the order of their first saving
	 */
	protected Map<Long, T> dataObjects = new LinkedHashMap<Long, T>();

	/**
	 * The last actual id given to a data object
	 */
	protected long lastDataObjectId = 0;

	/**
	 * Creates a copy of a data object by serializing and deserializing it
	 *
	 * @param dataObject the data object to copy
	 * @return the copy
	 * @throws Exception re-throws every exception
	 */
	@SuppressWarnings("unchecked")
	protected T copyOf(T dataObject) throws Exception
	{
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();

		try (ObjectOutputStream out = new ObjectOutputStream(bytes))
		{
			out.writeObject(dataObject);
		}

		try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray())))
		{
			return (T) in.readObject();
		}
	}

	/**
	 * Gets the ids of the stored data objects. They are the source records of the data object loaders.
	 *
	 * @return a copy of the ids of the stored data objects
	 */
	public synchronized Collection<Long> getDataObjectIds()
	{
		return new ArrayList<Long>(this.dataObjects.keySet());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected synchronized void doClear() throws Exception
	{
		this.dataObjects.clear();
		this.lastDataObjectId = 0;
	}

	/**
	 * {@inheritDoc}
	 *
	 * The source records are the requested ids.
	 */
	@Override
	protected DataObjectLoader<?, Long, T> doCreateLoader(Set<Long> ids) throws Exception
	{
		return SourceRecordDataObjectLoader.forIds(this, new ArrayList<>(ids).stream(), this::convertSourceRecord, ids);
	}

	/**
	 * {@inheritDoc}
	 *
	 * The source records are the ids of the data objects stored when the loader is created.
	 */
	@Override
	protected DataObjectLoader<?, Long, T> doCreateLoader(long modifiedSince, DeletedDataObjects deleted) throws Exception
	{
		return SourceRecordDataObjectLoader.forModifiedSince(this, this.getDataObjectIds().stream(), this::convertSourceRecord, modifiedSince, deleted);
	}

	/**
	 * {@inheritDoc}
	 *
	 * Returns a copy of the stored data object.
	 */
	@Override
	protected synchronized T doGet(Long id) throws Exception
	{
		T dataObject = this.dataObjects.get(id);

		return (dataObject == null) ? null : this.copyOf(dataObject);
	}

	/**
	 * {@inheritDoc}
	 *
	 * Stores a copy of the data object.
	 */
	@Override
	protected synchronized void doSave(T dataObject) throws Exception
	{
		if (this.isVirtualId(dataObject.getId())) dataObject.setId(++this.lastDataObjectId);

		updateTimestamps(dataObject, System.currentTimeMillis());

		this.dataObjects.put(dataObject.getId(), this.copyOf(dataObject));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected synchronized void doSave(Collection<T> dataObjects) throws Exception
	{
		for (T dataObject : dataObjects) this.doSave(dataObject);
	}
}
