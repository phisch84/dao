package com.schoste.ddd.infrastructure.dao.v4.services;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.schoste.ddd.infrastructure.dao.v4.exceptions.BrokenRecordException;
import com.schoste.ddd.infrastructure.dao.v4.exceptions.DAOException;
import com.schoste.ddd.infrastructure.dao.v4.exceptions.DAORuntimeException;
import com.schoste.ddd.infrastructure.dao.v4.models.DataObject;
import com.schoste.ddd.infrastructure.dao.v4.services.listeners.DeleteListener;
import com.schoste.ddd.infrastructure.dao.v4.services.listeners.GetListener;
import com.schoste.ddd.infrastructure.dao.v4.services.listeners.SaveListener;

/**
 * Base class for implementations of the {@link DAO} interface (DAO = Data Access Object) for a storage.
 *
 * This class implements the parts of the contract of {@link DAO} which are the same for every storage: checking the arguments,
 * calling the listeners in the order of their registration, logical deletion, deleting by ids, wrapping data object loaders in
 * streams, and wrapping exceptions in {@link DAOException}. An implementation only implements the protected methods whose names
 * start with {@code do}; they access the storage. They may throw any exception, which this class wraps.
 *
 * What an implementation must guarantee:
 * <ul>
 * <li>Its {@code do} methods are thread-safe, since a DAO instance is used by several threads.</li>
 * <li>{@link #doSave(DataObject)} and {@link #doSave(Collection)} set the time stamps from the storage's clock
 * (see {@link #updateTimestamps(DataObject, long)}) and give new data objects their actual ids.</li>
 * <li>Its data object loaders are lazy: they never load all data objects into memory at once.</li>
 * <li>{@link #doIsVirtualId(Serializable)} recognizes the ids created by {@link #doCreateVirtualId()}, without accessing the storage.</li>
 * </ul>
 * By default, data objects are deleted logically: {@link #doDelete(DataObject)} sets the deleted flag and saves the data object.
 * An implementation which deletes physically overrides {@link #doDelete(DataObject)} and {@link #doDelete(Collection)},
 * and says so in its class documentation with the sentence "Deletes physically: deletions are not reported by change tracking."
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 *
 * @param <ID> the class of the data objects' ids
 * @param <DO> the class of the data objects
 */
public abstract class AbstractDAO <ID extends Serializable, DO extends DataObject<ID>> implements DAO<ID, DO>
{
	/**
	 * The registered {@link GetListener}s, in the order of their registration.
	 * The set is thread-safe, so listeners can be (un-)registered while they are being called.
	 */
	protected Set<GetListener<ID, DO>> onGetListeners = new CopyOnWriteArraySet<>();

	/**
	 * The registered {@link SaveListener}s, in the order of their registration.
	 * The set is thread-safe, so listeners can be (un-)registered while they are being called.
	 */
	protected Set<SaveListener<ID, DO>> onSaveListeners = new CopyOnWriteArraySet<>();

	/**
	 * The registered {@link DeleteListener}s, in the order of their registration.
	 * The set is thread-safe, so listeners can be (un-)registered while they are being called.
	 */
	protected Set<DeleteListener<ID, DO>> onDeleteListeners = new CopyOnWriteArraySet<>();

	/**
	 * Creates a new DAO without listeners. Implementations call it from their own constructors.
	 */
	protected AbstractDAO()
	{
		super();
	}

	/**
	 * An operation which may throw any exception (see {@link AbstractDAO#run(Operation)}).
	 */
	@FunctionalInterface
	protected interface Operation
	{
		/**
		 * Performs the operation.
		 *
		 * @throws Exception re-throws every exception
		 */
		void run() throws Exception;
	}

	/**
	 * Calls an operation and returns its result.
	 * A {@link DAOException} thrown by the operation is re-thrown as it is, every other exception is wrapped in a {@link DAOException}.
	 *
	 * @param <T> the class of the operation's result
	 * @param operation the operation to call
	 * @return the result of the operation
	 * @throws DAOException thrown if the operation throws any exception
	 */
	protected static <T> T call(Callable<T> operation) throws DAOException
	{
		try
		{
			return operation.call();
		}
		catch (DAOException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new DAOException(e);
		}
	}

	/**
	 * Runs an operation like {@link AbstractDAO#call(Callable)} does, for operations without result.
	 *
	 * @param operation the operation to run
	 * @throws DAOException thrown if the operation throws any exception
	 */
	protected static void run(Operation operation) throws DAOException
	{
		call(() -> { operation.run(); return null; });
	}

	/**
	 * Sets the time stamps of a data object which the storage has just stored: the modification time stamp always,
	 * the creation time stamp only if it hasn't been set yet (it is smaller than 1).
	 * Meant for implementations of {@link #doSave(DataObject)} and {@link #doSave(Collection)}.
	 *
	 * @param dataObject the stored data object
	 * @param storageTimestamp the time of the storage's clock when it stored the data object, in milliseconds since 1970-01-01 UTC
	 */
	protected static void updateTimestamps(DataObject<?> dataObject, long storageTimestamp)
	{
		if (dataObject.getCreatedTimestamp() < 1) dataObject.setCreatedTimestamp(storageTimestamp);

		dataObject.setModifiedTimestamp(storageTimestamp);
	}

	/**
	 * Makes sure a listener to (un-)register is not null.
	 *
	 * @param <L> the class of the listener
	 * @param listener the listener to check
	 * @return the listener
	 * @throws IllegalArgumentException thrown if the listener is null
	 */
	private static <L> L requireListener(L listener) throws IllegalArgumentException
	{
		if (listener == null) throw new IllegalArgumentException("listener");

		return listener;
	}

	/**
	 * Makes sure a collection argument is neither null nor contains null.
	 *
	 * @param collection the collection to check
	 * @param name the name of the argument
	 * @throws IllegalArgumentException thrown if the collection is null or contains null
	 */
	private static void requireElements(Collection<?> collection, String name) throws IllegalArgumentException
	{
		if (collection == null) throw new IllegalArgumentException(name);

		for (Object element : collection)
		{
			if (element == null) throw new IllegalArgumentException(name);
		}
	}

	/**
	 * Copies a collection of ids into an array of the ids' class (see {@link AbstractDAO#getDataObjectIdClass()}),
	 * as needed by {@link GetListener#onBeforeGet(Serializable[])}.
	 *
	 * @param ids the ids to copy
	 * @return a new array with the ids
	 * @throws DAOException thrown if the class of the ids cannot be obtained
	 */
	@SuppressWarnings("unchecked")
	private ID[] toIdArray(Collection<ID> ids) throws DAOException
	{
		return call(() -> ids.toArray((ID[]) Array.newInstance(this.getDataObjectIdClass(), ids.size())));
	}

	/**
	 * Calls {@link GetListener#onBeforeGet(DataObjectLoader)} of the get listeners and wraps the loader in a stream.
	 * The data objects are loaded lazily by the loader while the stream is consumed; closing the stream closes the loader.
	 *
	 * @param doLoader the data object loader
	 * @return a sequential stream of the data objects provided by the loader
	 * @throws Exception re-throws every exception
	 */
	private Stream<DO> streamLoaded(DataObjectLoader<?, ID, DO> doLoader) throws Exception
	{
		for (GetListener<ID, DO> onGetListener : this.onGetListeners) onGetListener.onBeforeGet(doLoader);

		return StreamSupport.stream(doLoader, false).onClose(doLoader);
	}

	/**
	 * Creates the instance of a new data object. {@link #createDataObject()} gives it a virtual id afterwards.
	 *
	 * @return a new instance of the data object class
	 * @throws Exception re-throws every exception
	 */
	protected abstract DO doCreateDataObject() throws Exception;

	/**
	 * Creates a new, unique virtual id for a new data object (see {@link #createDataObject()}).
	 * {@link #doIsVirtualId(Serializable)} must recognize it as virtual.
	 *
	 * @return a new virtual id
	 * @throws Exception re-throws every exception
	 */
	protected abstract ID doCreateVirtualId() throws Exception;

	/**
	 * Determines if an id is virtual (see {@link DAO#isVirtualId(Serializable)}), without accessing the storage.
	 *
	 * @param id the id to check, never null
	 * @return true if the id is virtual
	 */
	protected abstract boolean doIsVirtualId(ID id);

	/**
	 * Loads the data object with the given id from the storage, also if it is flagged as deleted.
	 *
	 * @param id the id of the data object, never null
	 * @return the data object, or null if the storage has none with the id
	 * @throws Exception re-throws every exception
	 */
	protected abstract DO doGet(ID id) throws Exception;

	/**
	 * Stores a data object, including its deleted flag. A data object with a virtual id gets its actual id.
	 * The time stamps are set from the storage's clock (see {@link #updateTimestamps(DataObject, long)}).
	 *
	 * @param dataObject the data object to store, never null
	 * @throws Exception re-throws every exception
	 */
	protected abstract void doSave(DO dataObject) throws Exception;

	/**
	 * Stores data objects like {@link #doSave(DataObject)} does.
	 *
	 * @param dataObjects the data objects to store, never null and without null elements
	 * @throws Exception re-throws every exception
	 */
	protected abstract void doSave(Collection<DO> dataObjects) throws Exception;

	/**
	 * Deletes a stored data object which isn't flagged as deleted yet.
	 * By default it is deleted logically: the deleted flag is set and the data object is stored ({@link #doSave(DataObject)}).
	 * Implementations which delete physically override this method and set the flag themselves.
	 *
	 * @param dataObject the data object to delete, never null
	 * @throws Exception re-throws every exception
	 */
	protected void doDelete(DO dataObject) throws Exception
	{
		dataObject.setDeleted(true);

		this.doSave(dataObject);
	}

	/**
	 * Deletes stored data objects which aren't flagged as deleted yet, like {@link #doDelete(DataObject)} does.
	 *
	 * @param dataObjects the data objects to delete, never null and without null elements
	 * @throws Exception re-throws every exception
	 */
	protected void doDelete(Collection<DO> dataObjects) throws Exception
	{
		for (DO dataObject : dataObjects) dataObject.setDeleted(true);

		this.doSave(dataObjects);
	}

	/**
	 * Removes all data objects physically from the storage (see {@link DAO#clear()}).
	 *
	 * @throws UnsupportedOperationException thrown if the storage can't be cleared
	 * @throws Exception re-throws every exception
	 */
	protected abstract void doClear() throws Exception;

	/**
	 * Creates the loader for {@link DAO#get(Collection)}: it provides the data objects with the given ids, also the ones
	 * flagged as deleted. Usually {@link SourceRecordDataObjectLoader#forIds(DAO, Stream, java.util.function.Function, Set)}.
	 *
	 * @param ids the ids of the data objects to provide, never empty
	 * @return a new, lazy data object loader
	 * @throws Exception re-throws every exception
	 */
	protected abstract DataObjectLoader<?,ID,DO> doCreateLoader(Set<ID> ids) throws Exception;

	/**
	 * Creates the loader for {@link DAO#getAll(DeletedDataObjects)} and {@link DAO#getModifiedSince(long, DeletedDataObjects)}:
	 * it provides the data objects modified at or after the given time stamp. Usually
	 * {@link SourceRecordDataObjectLoader#forModifiedSince(DAO, Stream, java.util.function.Function, long, DeletedDataObjects)}.
	 *
	 * @param modifiedSince only data objects with a greater or equal modification time stamp are provided ({@link Long#MIN_VALUE} for all)
	 * @param deleted whether data objects flagged as deleted are provided
	 * @return a new, lazy data object loader
	 * @throws Exception re-throws every exception
	 */
	protected abstract DataObjectLoader<?,ID,DO> doCreateLoader(long modifiedSince, DeletedDataObjects deleted) throws Exception;

	/**
	 * Calls {@link AbstractDAO#doGet(Serializable)} and returns its result, but re-throws its exceptions unchecked,
	 * so the method can be used as conversion function of data object loaders (see {@link SourceRecordDataObjectLoader}).
	 *
	 * @param sourceRecordIdentifier the id of the data object to load
	 * @return the data object, or null if the storage has none with the id
	 * @throws DAORuntimeException thrown if loading the data object failed (with a {@link BrokenRecordException} as cause)
	 */
	@SuppressWarnings("unchecked")
	protected DO convertSourceRecord(Object sourceRecordIdentifier) throws DAORuntimeException
	{
		try
		{
			return this.doGet((ID) sourceRecordIdentifier);
		}
		catch (Exception ex)
		{
			throw new DAORuntimeException(new BrokenRecordException(sourceRecordIdentifier, ex));
		}
	}

	/**
	 * Resolves the actual class of one of the type parameters of {@link AbstractDAO} for this instance
	 * by walking up the class hierarchy from the instance's class to {@link AbstractDAO}.
	 *
	 * @param typeParamIndex the index of the type parameter of {@link AbstractDAO} (0 = ID, 1 = DO)
	 * @return the actual class of the type parameter
	 * @throws IllegalStateException thrown if the actual class cannot be obtained
	 */
	private Class<?> resolveTypeParameterClass(int typeParamIndex) throws IllegalStateException
	{
		Map<TypeVariable<?>, Type> typeArguments = new HashMap<>();

		for (Class<?> clazz = this.getClass(); (clazz != null) && (clazz != AbstractDAO.class); clazz = clazz.getSuperclass())
		{
			Type genericSuperclass = clazz.getGenericSuperclass();

			if (!(genericSuperclass instanceof ParameterizedType)) continue;

			ParameterizedType parameterizedSuperclass = (ParameterizedType) genericSuperclass;
			TypeVariable<?>[] typeParameters = ((Class<?>) parameterizedSuperclass.getRawType()).getTypeParameters();
			Type[] actualTypeArguments = parameterizedSuperclass.getActualTypeArguments();

			for (int i=0; i<typeParameters.length; i++) typeArguments.put(typeParameters[i], actualTypeArguments[i]);
		}

		Type type = AbstractDAO.class.getTypeParameters()[typeParamIndex];

		while (typeArguments.containsKey(type)) type = typeArguments.get(type);

		if (type instanceof ParameterizedType) type = ((ParameterizedType) type).getRawType();
		if (!(type instanceof Class)) throw new IllegalStateException();

		return (Class<?>) type;
	}

	/**
	 * Obtains the class of the data objects, resolved from the type argument given for DO.
	 *
	 * @return the class of the data objects
	 * @throws IllegalStateException thrown if the class cannot be obtained
	 * @throws Exception re-throws every exception
	 */
	@SuppressWarnings("rawtypes")
	protected Class getDataObjectClass() throws IllegalStateException, Exception
	{
		return this.resolveTypeParameterClass(1);
	}

	/**
	 * Obtains the class of the data objects' ids, resolved from the type argument given for ID.
	 *
	 * @return the class of the ids
	 * @throws IllegalStateException thrown if the class cannot be obtained
	 * @throws Exception re-throws every exception
	 */
	@SuppressWarnings("rawtypes")
	protected Class getDataObjectIdClass() throws IllegalStateException, Exception
	{
		return this.resolveTypeParameterClass(0);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DO createDataObject() throws DAOException
	{
		return call(() ->
		{
			DO dataObj = this.doCreateDataObject();
			   dataObj.setId(this.doCreateVirtualId());

			return dataObj;
		});
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public final boolean isVirtualId(ID id) throws IllegalArgumentException
	{
		if (id == null) throw new IllegalArgumentException("id");

		return this.doIsVirtualId(id);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void registerOnGetListener(GetListener<ID, DO> listener) throws IllegalArgumentException
	{
		this.onGetListeners.add(requireListener(listener));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void unregisterOnGetListener(GetListener<ID, DO> listener) throws IllegalArgumentException
	{
		this.onGetListeners.remove(requireListener(listener));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Collection<GetListener<ID, DO>> getOnGetListeners()
	{
		return new ArrayList<>(this.onGetListeners);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void registerOnSaveListener(SaveListener<ID, DO> listener) throws IllegalArgumentException
	{
		this.onSaveListeners.add(requireListener(listener));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void unregisterOnSaveListener(SaveListener<ID, DO> listener) throws IllegalArgumentException
	{
		this.onSaveListeners.remove(requireListener(listener));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Collection<SaveListener<ID, DO>> getOnSaveListeners()
	{
		return new ArrayList<>(this.onSaveListeners);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void registerOnDeleteListener(DeleteListener<ID, DO> listener) throws IllegalArgumentException
	{
		this.onDeleteListeners.add(requireListener(listener));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void unregisterOnDeleteListener(DeleteListener<ID, DO> listener) throws IllegalArgumentException
	{
		this.onDeleteListeners.remove(requireListener(listener));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Collection<DeleteListener<ID, DO>> getOnDeleteListeners()
	{
		return new ArrayList<>(this.onDeleteListeners);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DO get(ID id) throws IllegalArgumentException, IllegalStateException, DAOException
	{
		if (id == null) throw new IllegalArgumentException("id");

		ID actualId = id;

		for (GetListener<ID, DO> onGetListener : this.onGetListeners)
		{
			ID requestedId = actualId;

			actualId = call(() -> onGetListener.onBeforeGet(requestedId));

			if (actualId == null) throw new IllegalStateException(String.format("GetListener %s returned null as id", onGetListener));
		}

		ID idToGet = actualId;
		DO dataObject = call(() -> this.doGet(idToGet));

		for (GetListener<ID, DO> onGetListener : this.onGetListeners)
		{
			if (dataObject == null) return null;

			DO loadedDataObject = dataObject;

			dataObject = call(() -> onGetListener.onAfterGet(loadedDataObject));
		}

		return dataObject;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Stream<DO> get(Collection<ID> ids) throws IllegalArgumentException, IllegalStateException, DAOException
	{
		requireElements(ids, "ids");

		ID[] actualIds = this.toIdArray(ids);

		for (GetListener<ID, DO> onGetListener : this.onGetListeners)
		{
			ID[] requestedIds = actualIds;

			actualIds = call(() -> onGetListener.onBeforeGet(requestedIds));

			if (actualIds == null) throw new IllegalStateException(String.format("GetListener %s returned null as ids", onGetListener));
		}

		Set<ID> idsToGet = new HashSet<>(Arrays.asList(actualIds));

		idsToGet.remove(null);

		if (idsToGet.isEmpty()) return Stream.empty();

		return call(() -> this.streamLoaded(this.doCreateLoader(idsToGet)));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Stream<DO> getAll() throws DAOException
	{
		return this.getAll(DeletedDataObjects.EXCLUDE);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Stream<DO> getAll(DeletedDataObjects deleted) throws IllegalArgumentException, DAOException
	{
		return this.getModifiedSince(Long.MIN_VALUE, deleted);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Stream<DO> getModifiedSince(long timestamp) throws DAOException
	{
		return this.getModifiedSince(timestamp, DeletedDataObjects.EXCLUDE);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Stream<DO> getModifiedSince(long timestamp, DeletedDataObjects deleted) throws IllegalArgumentException, DAOException
	{
		if (deleted == null) throw new IllegalArgumentException("deleted");

		return call(() -> this.streamLoaded(this.doCreateLoader(timestamp, deleted)));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void save(DO dataObject) throws IllegalArgumentException, DAOException
	{
		if (dataObject == null) throw new IllegalArgumentException("dataObject");

		run(() ->
		{
			for (SaveListener<ID, DO> onSaveListener : this.onSaveListeners)
			{
				if (!onSaveListener.onBeforeSave(dataObject)) return;
			}

			this.doSave(dataObject);

			for (SaveListener<ID, DO> onSaveListener : this.onSaveListeners) onSaveListener.onAfterSave(dataObject);
		});
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void save(Collection<DO> dataObjects) throws IllegalArgumentException, DAOException
	{
		requireElements(dataObjects, "dataObjects");

		run(() ->
		{
			for (SaveListener<ID, DO> onSaveListener : this.onSaveListeners)
			{
				if (!onSaveListener.onBeforeSave(dataObjects)) return;
			}

			this.doSave(dataObjects);

			for (SaveListener<ID, DO> onSaveListener : this.onSaveListeners) onSaveListener.onAfterSave(dataObjects);
		});
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void delete(DO dataObject) throws IllegalArgumentException, DAOException
	{
		if (dataObject == null) throw new IllegalArgumentException("dataObject");

		// already deleted: nothing happens, so change tracking doesn't report it twice
		if (dataObject.isDeleted()) return;

		// never saved: there is nothing to delete in the storage
		if (this.isVirtualId(dataObject.getId()))
		{
			dataObject.setDeleted(true);

			return;
		}

		run(() ->
		{
			for (DeleteListener<ID, DO> onDeleteListener : this.onDeleteListeners)
			{
				if (!onDeleteListener.onBeforeDelete(dataObject)) return;
			}

			this.doDelete(dataObject);

			for (DeleteListener<ID, DO> onDeleteListener : this.onDeleteListeners) onDeleteListener.onAfterDelete(dataObject);
		});
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void delete(Collection<DO> dataObjects) throws IllegalArgumentException, DAOException
	{
		requireElements(dataObjects, "dataObjects");

		List<DO> dataObjectsToDelete = new ArrayList<>(dataObjects.size());

		for (DO dataObject : dataObjects)
		{
			if (dataObject.isDeleted()) continue;

			if (this.isVirtualId(dataObject.getId()))
			{
				dataObject.setDeleted(true);

				continue;
			}

			dataObjectsToDelete.add(dataObject);
		}

		if (dataObjectsToDelete.isEmpty()) return;

		run(() ->
		{
			for (DeleteListener<ID, DO> onDeleteListener : this.onDeleteListeners)
			{
				if (!onDeleteListener.onBeforeDelete(dataObjectsToDelete)) return;
			}

			this.doDelete(dataObjectsToDelete);

			for (DeleteListener<ID, DO> onDeleteListener : this.onDeleteListeners) onDeleteListener.onAfterDelete(dataObjectsToDelete);
		});
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void deleteById(ID dataObjectId) throws IllegalArgumentException, DAOException
	{
		if (dataObjectId == null) throw new IllegalArgumentException("dataObjectId");

		this.deleteByIds(Collections.singletonList(dataObjectId));
	}

	/**
	 * {@inheritDoc}
	 *
	 * The data objects are loaded by {@link #doGet(Serializable)}, without calling the get listeners.
	 */
	@Override
	public void deleteByIds(Collection<ID> dataObjectIds) throws IllegalArgumentException, DAOException
	{
		requireElements(dataObjectIds, "dataObjectIds");

		List<DO> storedDataObjects = call(() ->
		{
			List<DO> found = new ArrayList<>(dataObjectIds.size());

			for (ID dataObjectId : new HashSet<>(dataObjectIds))
			{
				DO dataObject = this.doGet(dataObjectId);

				if (dataObject != null) found.add(dataObject);
			}

			return found;
		});

		this.delete(storedDataObjects);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clear() throws UnsupportedOperationException, DAOException
	{
		try
		{
			this.doClear();
		}
		catch (UnsupportedOperationException | DAOException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new DAOException(e);
		}
	}
}
