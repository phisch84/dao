package com.schoste.ddd.infrastructure.dal.v3.services;

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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.schoste.ddd.infrastructure.dal.v3.exceptions.BrokenRecordException;
import com.schoste.ddd.infrastructure.dal.v3.exceptions.DALException;
import com.schoste.ddd.infrastructure.dal.v3.exceptions.DALRuntimeException;
import com.schoste.ddd.infrastructure.dal.v3.models.GenericDataObject;
import com.schoste.ddd.infrastructure.dal.v3.services.listeners.DeleteListener;
import com.schoste.ddd.infrastructure.dal.v3.services.listeners.GetListener;
import com.schoste.ddd.infrastructure.dal.v3.services.listeners.SaveListener;

/**
 * Basic class with functionality common to all other generic data access objects
 * 
 * @author Philipp Schosteritsch
 *
 * @param <ID> the class of the data objects' identifier
 * @param <DO> the class of the data object to persist
 */
public abstract class GenericDAO <ID extends Serializable, DO extends GenericDataObject<ID>> implements GenericDataAccessObject<ID, DO>
{
	/**
	 * The most recent modification time stamp of data objects (see {@link GenericDataAccessObject#getLastModifiedTimeStamp()}).
	 * Initially and after {@link GenericDAO#clear()} it is {@link Long#MIN_VALUE}.
	 */
	protected long latestModificationTimeStamp = Long.MIN_VALUE;

	/**
	 * The registered {@link GetListener}s. The order in which they are called is undefined.
	 * The set is thread-safe, so listeners can be (un-)registered while they are being called.
	 */
	protected Set<GetListener<ID, DO>> onGetListeners = new CopyOnWriteArraySet<>();

	/**
	 * The registered {@link SaveListener}s. The order in which they are called is undefined.
	 * The set is thread-safe, so listeners can be (un-)registered while they are being called.
	 */
	protected Set<SaveListener<ID, DO>> onSaveListeners = new CopyOnWriteArraySet<>();

	/**
	 * The registered {@link DeleteListener}s. The order in which they are called is undefined.
	 * The set is thread-safe, so listeners can be (un-)registered while they are being called.
	 */
	protected Set<DeleteListener<ID, DO>> onDeleteListeners = new CopyOnWriteArraySet<>();

	/**
	 * An operation which may throw any exception (see {@link GenericDAO#run(Operation)}).
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
	 * A {@link DALException} thrown by the operation is re-thrown as it is, every other exception is wrapped in a {@link DALException}.
	 *
	 * @param <T> the class of the operation's result
	 * @param operation the operation to call
	 * @return the result of the operation
	 * @throws DALException thrown if the operation throws any exception
	 */
	protected static <T> T call(Callable<T> operation) throws DALException
	{
		try
		{
			return operation.call();
		}
		catch (DALException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new DALException(e);
		}
	}

	/**
	 * Runs an operation like {@link GenericDAO#call(Callable)} does, for operations without result.
	 *
	 * @param operation the operation to run
	 * @throws DALException thrown if the operation throws any exception
	 */
	protected static void run(Operation operation) throws DALException
	{
		call(() -> { operation.run(); return null; });
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
	 * Casts an object to the class of the data objects of this DAO.
	 *
	 * @param dataObject the object to cast
	 * @return the object as data object
	 * @throws IllegalArgumentException thrown if the object is null or not an instance of the data objects' class
	 * @throws DALException thrown if the class of the data objects cannot be obtained
	 */
	@SuppressWarnings("unchecked")
	private DO toDataObject(Object dataObject) throws IllegalArgumentException, DALException
	{
		if ((dataObject == null) || !call(() -> this.getDataObjectClass().isInstance(dataObject))) throw new IllegalArgumentException("dataObject");

		return (DO) dataObject;
	}

	/**
	 * Casts the elements of an array to the class of the data objects of this DAO (see {@link GenericDAO#toDataObject(Object)}).
	 *
	 * @param dataObjects the objects to cast
	 * @return a new collection with the objects as data objects
	 * @throws IllegalArgumentException thrown if the array is null, or if one of its elements is null or not an instance of the data objects' class
	 * @throws DALException thrown if the class of the data objects cannot be obtained
	 */
	private Collection<DO> toDataObjects(Object[] dataObjects) throws IllegalArgumentException, DALException
	{
		if (dataObjects == null) throw new IllegalArgumentException("dataObjects");

		Collection<DO> dataObjectsList = new ArrayList<>(dataObjects.length);

		for (Object dataObject : dataObjects) dataObjectsList.add(this.toDataObject(dataObject));

		return dataObjectsList;
	}

	/**
	 * Raises the event in the get-listeners for a data object loader and wraps the loader in a stream (see {@link GenericDAO#doGet(DataObjectLoader)}).
	 * Events for the loaded data objects are supposed to be raised by the data object loader.
	 *
	 * @param doLoader the data object loader
	 * @return a stream of the data objects provided by the loader
	 * @throws Exception re-throws every exception
	 */
	private Stream<DO> streamLoaded(DataObjectLoader<?, ID, DO> doLoader) throws Exception
	{
		for (GetListener<ID, DO> onGetListener : this.onGetListeners) onGetListener.onBeforeGet(doLoader);

		return this.doGet(doLoader);
	}

	/**
	 * The method that actually creates a data object and needs to be implemented by its deriving class.
	 * Consider implementing the method synchronized for thread safety.
	 * 
	 * @param args optional arguments for creating the DO
	 * @return a new instance of the DO
	 * @throws Exception re-throws every exception
	 */
	protected abstract DO doCreateDataObject(Object... args) throws Exception;

	/**
	 * The method that actually gets a data object and needs to be implemented by its deriving class.
	 * Consider implementing the method synchronized for thread safety.
	 * 
	 * @param id the id of the data object to get
	 * @return the data object with the given id or null if not found
	 * @throws Exception re-throws every exception
	 */
	protected abstract DO doGet(ID id) throws Exception;

	/**
	 * The method to actually save the data object and needs to be implemented by its deriving class.
	 * Consider implementing the method synchronized for thread safety.
	 * 
	 * @param dataObject the data object to save
	 * @throws Exception re-throws every exception
	 */
	protected abstract void doSave(DO dataObject) throws Exception;

	/**
	 * The method to actually save the data objects and needs to be implemented by its deriving class.
	 * Consider implementing the method synchronized for thread safety.
	 * 
	 * @param dataObjects the data objects to save
	 * @throws Exception re-throws every exception
	 */
	protected abstract void doSave(Collection<DO> dataObjects) throws Exception;

	/**
	 * The method to actually delete the data object and needs to be implemented by its deriving class.
	 * The implementation is expected to flag the data object as deleted ({@link GenericDataObject#setIsDeleted(boolean)}),
	 * as this class does not do it.
	 * Consider implementing the method synchronized for thread safety.
	 * 
	 * @param dataObject the data object to delete
	 * @throws Exception re-throws every exception
	 */
	protected abstract void doDelete(DO dataObject) throws Exception;

	/**
	 * The method to actually delete the data objects and needs to be implemented by its deriving class.
	 * The implementation is expected to flag the data objects as deleted ({@link GenericDataObject#setIsDeleted(boolean)}),
	 * as this class does not do it.
	 * Consider implementing the method synchronized for thread safety.
	 * 
	 * @param dataObjects the data objects to delete
	 * @throws Exception re-throws every exception
	 */
	protected abstract void doDelete(Collection<DO> dataObjects) throws Exception;

	/**
	 * The method to actually delete the data objects with given ids and needs to be implemented by its deriving class.
	 * Consider implementing the method synchronized for thread safety.
	 * 
	 * @param dataObjectIds the ids of the data objects to delete
	 * @throws Exception re-throws every exception
	 */
	protected abstract void doDelete(ID[] dataObjectIds) throws Exception;

	/**
	 * Clears the actual storage. This method needs to be implemented by its deriving class.
	 * Consider implementing the method synchronized for thread safety.
	 * 
	 * To be overwritten by the extending class.
	 * @throws Exception re-throws every exception
	 */
	protected abstract void doClear() throws Exception;

	/**
	 * Returns a datum that can be used to uniquely identify data objects that haven't been persisted yet.
	 * It is called by {@link GenericDAO#createDataObject(Object...)} to set the id of the newly created data object.
	 *
	 * @param args optional arguments (can be null). The usage of these arguments depends on the actual implementation
	 * @return a datum that can be used to uniquely identify a data object that hasn't been persisted yet
	 * @throws Exception re-throws every exception
	 */
	protected abstract ID getNextVirtualId(Object... args) throws Exception;

	/**
	 * Returns a datum that uniquely identifies a data object that exists in the underlying storage.
	 * This method is not called by this class. It is meant to be used by the deriving class (e.g. when saving a data object).
	 *
	 * @param args optional arguments (can be null). The usage of these arguments depends on the actual implementation
	 * @return a datum that uniquely identifies a data object that exists in the underlying storage
	 * @throws Exception re-throws every exception
	 */
	protected abstract ID getActualId(Object... args) throws Exception;

	/**
	 * Returns a data object loader (a {@link java.util.Spliterator}) that will poll the next available data object from the data source
	 * so it can be processed in calls to {@link GenericDataAccessObject#getAll()}, {@link GenericDataAccessObject#get(Serializable[])}
	 * and {@link GenericDataAccessObject#get(Collection)}.
	 * Usually this is a {@link GenericDataObjectLoader} that is fed with a stream of the storage's source records
	 * and a conversion function like {@link GenericDAO#doGetUnchecked(Object)}.
	 * If the set of ids is empty, the loader must not filter for ids and return all available data objects
	 * (as promised by {@link GenericDataAccessObject#get(Serializable[])}).
	 *
	 * To be overwritten by the extending class.
	 *
	 * @param ids a set of ids that identify the data objects to load from the underlying storage. If empty, all data objects shall be loaded
	 * @return a data object loader that will poll the next available data object from the data source
	 * @throws Exception re-throws every exception
	 */
	protected abstract DataObjectLoader<?,ID,DO> createDOLoader(Set<ID> ids) throws Exception;

	/**
	 * Returns a data object loader (a {@link java.util.Spliterator}) that will poll the next available data object from the data source
	 * so it can be processed in calls to {@link GenericDataAccessObject#getRecent()}, {@link GenericDataAccessObject#getRecent(boolean)}
	 * and {@link GenericDataAccessObject#getRecent(long, boolean)}.
	 * Usually this is a {@link GenericRecentDataObjectLoader}.
	 * The created data loader needs to be capable of filtering data objects out based on if they were flagged deleted
	 * ({@link GenericDataObject#getIsDeleted()} == true) and if they were not modified after the given modification time stamp
	 * ({@link GenericDataObject#getModifiedTimeStamp()} &lt;= lastModTS).
	 *
	 * This class does not update the DAO's most recent modification time stamp itself. The loader is responsible for calling
	 * {@link GenericDataAccessObject#setLastModifiedTimeStamp(long)} when the loading has ended and lastModTS equals
	 * {@link GenericDataAccessObject#getLastModifiedTimeStamp()} (as {@link GenericRecentDataObjectLoader} does).
	 *
	 * To be overwritten by the extending class.
	 *
	 * @param lastModTS the modification time stamp the data objects' modification time stamps are compared to
	 * @param returnDeleted if true, data objects flagged as deleted ({@link GenericDataObject#getIsDeleted()} == true) shall also be returned
	 * @return a data object loader that will poll the next available data object from the data source
	 * @throws Exception re-throws every exception
	 */
	protected abstract DataObjectLoader<?,ID,DO> createRecentDOLoader(long lastModTS, boolean returnDeleted) throws Exception;

	/**
	 * Updates the created and modified time stamp of a data object (DO) by calling its implementation of
	 * {@link GenericDataObject#updateTimeStamps()}.
	 * 
	 * 1. always updates the modification time stamp of the DO
	 * 2. if the DO has no created time stamp yet ({@link GenericDataObject#getCreatedTimeStamp()} is smaller than 1), it is set as well
	 * 
	 * Does nothing if the data object is null.
	 * 
	 * @param dataObj the data object to update
	 */
	protected synchronized void updateDataObjTimeStamps(DO dataObj)
	{
		if (dataObj == null) return;

		dataObj.updateTimeStamps();
	}

	/**
	 * Updates the created and modified time stamp of data objects (DOs) by calling the implementation of
	 * {@link GenericDataObject#updateTimeStamps()} (see {@link GenericDAO#updateDataObjTimeStamps(GenericDataObject)}).
	 *
	 * Does nothing if the collection is null. Elements that are null are skipped.
	 *
	 * @param dataObjects the data objects to update
	 */
	protected synchronized void updateDataObjTimeStamps(Collection<DO> dataObjects)
	{
		if (dataObjects == null) return;

		for (DO dataObj : dataObjects) this.updateDataObjTimeStamps(dataObj);
	}

	/**
	 * Wraps a data object loader in a sequential stream. The data objects are loaded lazily by the loader
	 * while the stream is consumed.
	 * The loader is registered as close handler of the stream, so {@link DataObjectLoader#close()} is only called
	 * if the caller closes the stream.
	 * 
	 * @param doLoader the actual data object loader
	 * @return a stream of the data objects provided by the loader
	 * @throws Exception re-throws every exception
	 */
	protected synchronized Stream<DO> doGet(DataObjectLoader<?, ID, DO> doLoader) throws Exception
	{
		Stream<DO> dataObjStream = StreamSupport.stream(doLoader, false);
				   dataObjStream.onClose(doLoader);

		return dataObjStream;
	}

	/**
	 * Calls {@link GenericDAO#doGet(Serializable)} and returns whatever it returns, but re-throws its exceptions unchecked,
	 * so the method can be used as conversion function of data object loaders (see {@link GenericDataObjectLoader}).
	 *
	 * @param sourceRecordIdentifier any identifier that indicates the next record to load as data object from the data source
	 * @return the data object of the source record or null if no source record was found
	 * @throws DALRuntimeException thrown if loading the data object failed (with a {@link BrokenRecordException} as cause)
	 */
	@SuppressWarnings("unchecked")
	protected DO doGetUnchecked(Object sourceRecordIdentifier) throws DALRuntimeException
	{
		try
		{
			return this.doGet((ID) sourceRecordIdentifier);
		}
		catch (Exception ex)
		{
			throw new DALRuntimeException(new BrokenRecordException(sourceRecordIdentifier, ex));
		}
	}

	/**
	 * Resolves the actual class of one of the type parameters of {@link GenericDAO} for this instance
	 * by walking up the class hierarchy from the instance's class to {@link GenericDAO}.
	 * 
	 * @param typeParamIndex the index of the type parameter of {@link GenericDAO} (0 = ID, 1 = DO)
	 * @return the actual class of the type parameter
	 * @throws IllegalStateException thrown if the actual class cannot be obtained
	 */
	private Class<?> resolveTypeParameterClass(int typeParamIndex) throws IllegalStateException
	{
		Map<TypeVariable<?>, Type> typeArguments = new HashMap<>();

		for (Class<?> clazz = this.getClass(); (clazz != null) && (clazz != GenericDAO.class); clazz = clazz.getSuperclass())
		{
			Type genericSuperclass = clazz.getGenericSuperclass();

			if (!(genericSuperclass instanceof ParameterizedType)) continue;

			ParameterizedType parameterizedSuperclass = (ParameterizedType) genericSuperclass;
			TypeVariable<?>[] typeParameters = ((Class<?>) parameterizedSuperclass.getRawType()).getTypeParameters();
			Type[] actualTypeArguments = parameterizedSuperclass.getActualTypeArguments();

			for (int i=0; i<typeParameters.length; i++) typeArguments.put(typeParameters[i], actualTypeArguments[i]);
		}

		Type type = GenericDAO.class.getTypeParameters()[typeParamIndex];

		while (typeArguments.containsKey(type)) type = typeArguments.get(type);

		if (type instanceof ParameterizedType) type = ((ParameterizedType) type).getRawType();
		if (!(type instanceof Class)) throw new IllegalStateException();

		return (Class<?>) type;
	}

	/**
	 * Obtains the actual data class of data objects that derive from GenericDataObject.
	 * The class is resolved from the type argument given for DO, regardless of how many classes
	 * are between the class of this instance and {@link GenericDAO}.
	 * 
	 * @return the actual data class
	 * @throws IllegalStateException thrown if the actual data class cannot be obtained
	 * @throws Exception re-throws every exception
	 */
	@SuppressWarnings("rawtypes")
	protected Class getDataObjectClass() throws IllegalStateException, Exception
	{
		return this.resolveTypeParameterClass(1);
	}

	/**
	 * Obtains the actual class of the data objects' identifiers.
	 * The class is resolved from the type argument given for ID, regardless of how many classes
	 * are between the class of this instance and {@link GenericDAO}.
	 * 
	 * @return the actual class of the identifiers
	 * @throws IllegalStateException thrown if the actual class cannot be obtained
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
	public long getLastModifiedTimeStamp()
	{
		return this.latestModificationTimeStamp;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void setLastModifiedTimeStamp(long newTS)
	{
		this.latestModificationTimeStamp = newTS;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DO createDataObject(Object... args) throws DALException
	{
		return call(() ->
		{
			DO dataObj = this.doCreateDataObject(args);
			   dataObj.setId(this.getNextVirtualId(args));
			   dataObj.updateTimeStamps();

			return dataObj;
		});
	}

	/**
	 * {@inheritDoc}
	 */
	@Override 
	public abstract boolean isVirtualId(ID id);

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
		return new HashSet<>(this.onGetListeners);
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
		return new HashSet<>(this.onSaveListeners);
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
		return new HashSet<>(this.onDeleteListeners);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DO get(ID id) throws DALException
	{
		return call(() ->
		{
			ID actualId = id;

			// raise the event in the get-listeners for the requested id
			for (GetListener<ID, DO> onGetListener : this.onGetListeners) actualId = onGetListener.onBeforeGet(actualId);

			DO dataObject = this.doGet(actualId);

			// raise the event in the get-listeners for the loaded data object (after it was loaded)
			for (GetListener<ID, DO> onGetListener : this.onGetListeners) dataObject = onGetListener.onAfterGet(dataObject);

			return dataObject;
		});
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Stream<DO> get(ID[] ids) throws IllegalArgumentException, DALException
	{
		if (ids == null) throw new IllegalArgumentException("ids");

		return call(() ->
		{
			ID[] actualIds = ids;

			// raise the event in the get-listeners for the ids that are about to be retrieved
			for (GetListener<ID, DO> onGetListener : this.onGetListeners)
			{
				actualIds = onGetListener.onBeforeGet(actualIds);

				if (actualIds == null) return this.getAll();
			}

			Set<ID> filterInIds = new HashSet<>(Arrays.asList(actualIds));

			return this.streamLoaded(this.createDOLoader(filterInIds));
		});
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Stream<DO> get(Collection<ID> ids) throws IllegalArgumentException, DALException
	{
		if (ids == null) throw new IllegalArgumentException("ids");

		// get-listener events will be fired in the called method
		return this.get(call(() -> ids.toArray((ID[]) Array.newInstance(this.getDataObjectIdClass(), ids.size()))));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Stream<DO> getAll() throws DALException
	{
		return call(() -> this.streamLoaded(this.createDOLoader(Collections.<ID>emptySet())));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void save(DO dataObject) throws IllegalArgumentException, DALException
	{
		if (dataObject == null) throw new IllegalArgumentException("dataObject");

		run(() ->
		{
			for (SaveListener<ID, DO> onSaveListener : this.onSaveListeners)
			{
				if (!onSaveListener.onBeforeSaving(dataObject)) return;
			}

			this.updateDataObjTimeStamps(dataObject);
			this.doSave(dataObject);

			for (SaveListener<ID, DO> onSaveListener : this.onSaveListeners) onSaveListener.onAfterSaved(dataObject);
		});
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void save(Object dataObject) throws IllegalArgumentException, DALException
	{
		this.save(this.toDataObject(dataObject));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void save(Collection<DO> dataObjects) throws IllegalArgumentException, DALException
	{
		if (dataObjects == null) throw new IllegalArgumentException("dataObjects");

		run(() ->
		{
			for (SaveListener<ID, DO> onSaveListener : this.onSaveListeners)
			{
				if (!onSaveListener.onBeforeSaving(dataObjects)) return;
			}

			this.updateDataObjTimeStamps(dataObjects);
			this.doSave(dataObjects);

			for (SaveListener<ID, DO> onSaveListener : this.onSaveListeners) onSaveListener.onAfterSaved(dataObjects);
		});
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void save(GenericDataObject<ID>[] dataObjects) throws DALException
	{
		this.save(this.toDataObjects(dataObjects));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void delete(DO dataObject) throws IllegalArgumentException, DALException
	{
		if (dataObject == null) throw new IllegalArgumentException("dataObject");

		run(() ->
		{
			for (DeleteListener<ID, DO> onDeleteListener : this.onDeleteListeners)
			{
				if (!onDeleteListener.onBeforeDeleting(dataObject)) return;
			}

			this.doDelete(dataObject);

			for (DeleteListener<ID, DO> onDeleteListener : this.onDeleteListeners) onDeleteListener.onAfterDeleted(dataObject);
		});
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void delete(Object dataObject) throws IllegalArgumentException, DALException
	{
		this.delete(this.toDataObject(dataObject));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void delete(Collection<DO> dataObjects) throws IllegalArgumentException, DALException
	{
		if (dataObjects == null) throw new IllegalArgumentException("dataObjects");

		run(() ->
		{
			for (DeleteListener<ID, DO> onDeleteListener : this.onDeleteListeners)
			{
				if (!onDeleteListener.onBeforeDeleting(dataObjects)) return;
			}

			this.doDelete(dataObjects);

			for (DeleteListener<ID, DO> onDeleteListener : this.onDeleteListeners) onDeleteListener.onAfterDeleted(dataObjects);
		});
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void delete(DO[] dataObjects) throws DALException
	{
		this.delete(this.toDataObjects(dataObjects));
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void delete(ID dataObjectId) throws IllegalArgumentException, DALException
	{
		if (dataObjectId == null) throw new IllegalArgumentException("dataObjectId");

		ID[] dataObjectIds = (ID[]) Array.newInstance(dataObjectId.getClass(), 1);
			 dataObjectIds[0] = dataObjectId;

		this.delete(dataObjectIds);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void delete(ID[] dataObjectIds) throws IllegalArgumentException, DALException
	{
		if (dataObjectIds == null) throw new IllegalArgumentException("dataObjectIds");

		run(() ->
		{
			ID[] dataObjectIdsToDelete = dataObjectIds;

			for (DeleteListener<ID, DO> onDeleteListener : this.onDeleteListeners)
			{
				dataObjectIdsToDelete = onDeleteListener.onBeforeDeleting(dataObjectIdsToDelete);
			}

			this.doDelete(dataObjectIdsToDelete);

			for (DeleteListener<ID, DO> onDeleteListener : this.onDeleteListeners) onDeleteListener.onAfterDeleted(dataObjectIdsToDelete);
		});
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clear() throws DALException
	{
		run(() ->
		{
			this.doClear();
			this.latestModificationTimeStamp = Long.MIN_VALUE;
		});
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Stream<DO> getRecent() throws DALException
	{
		return this.getRecent(latestModificationTimeStamp, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Stream<DO> getRecent(boolean returnDeleted) throws DALException
	{
		return this.getRecent(this.latestModificationTimeStamp, returnDeleted);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Stream<DO> getRecent(long lastModTS, boolean returnDeleted) throws DALException
	{
		return call(() -> this.streamLoaded(this.createRecentDOLoader(lastModTS, returnDeleted)));
	}
}
