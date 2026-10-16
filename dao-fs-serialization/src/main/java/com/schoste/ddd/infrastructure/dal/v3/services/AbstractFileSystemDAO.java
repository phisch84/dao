package com.schoste.ddd.infrastructure.dal.v3.services;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import com.schoste.ddd.infrastructure.dal.v3.exceptions.DALException;
import com.schoste.ddd.infrastructure.dal.v3.exceptions.DALRuntimeException;
import com.schoste.ddd.infrastructure.dal.v3.models.GenericDataObject;

/**
 * Basic class with functionality common to all data access objects which store every data object in its own file
 * in a folder of the file system (see {@link com.schoste.ddd.infrastructure.dal.v3.services.fs.GenericFileSystemDAO} and
 * {@link com.schoste.ddd.infrastructure.dal.v3.services.serialization.GenericSerializationDAO}).
 * The name of a data object's file is the string value of its id ({@link GenericDataObject#getId()}).
 * The data object loaders iterate over the files in the storage folder and load each data object via {@link #doGetUnchecked(Object)}.
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 *
 * @param <ID> the class of the data object's unique identifier
 * @param <DO> the class of the data object to persist
 */
abstract public class AbstractFileSystemDAO<ID extends Serializable, DO extends GenericDataObject<ID>> extends GenericDAO<ID, DO>
{
	/**
	 * The directory where the files of the data objects are located
	 */
	protected final Path storagePath;

	/**
	 * Turns a {@link String} value into a data object's unique identifier
	 * 
	 * @param value the unique identifier's string value
	 * @return a data object's unique identifier
	 */
	abstract protected ID toID(String value);

	/**
	 * {@inheritDoc}
	 */
	@Override
	abstract protected DO doGet(ID id) throws IllegalStateException, Exception;

	/**
	 * {@inheritDoc}
	 */
	@Override
	abstract protected void doSave(DO dataObject) throws Exception;

	/**
	 * Gets the path to the file of the data object with the given id (the file does not need to exist).
	 *
	 * @param id the id of the data object
	 * @return the path to the file named after the id in the storage folder
	 */
	protected Path fileOf(ID id)
	{
		return this.storagePath.resolve(id.toString());
	}

	/**
	 * Gets the path to the file of the data object with the given id if the file exists (see {@link #fileOf(Serializable)}).
	 *
	 * @param id the id of the data object
	 * @return the path to the file, or null if there is no file for the id
	 * @throws IllegalStateException if the path of the id is not a regular file or is not readable
	 */
	protected Path getReadableFile(ID id) throws IllegalStateException
	{
		Path file = this.fileOf(id);

		if (!Files.exists(file)) return null;
		if (!Files.isRegularFile(file) || !Files.isReadable(file)) throw new IllegalStateException();

		return file;
	}

	/**
	 * Gets the id under which a data object is to be stored: the given id, or the id returned by {@link #getActualId(Object...)}
	 * (called with the given id) if the given id is virtual ({@link #isVirtualId(Serializable)}).
	 *
	 * @param id the id of the data object
	 * @return the id to store the data object with
	 * @throws Exception re-throws every exception
	 */
	protected ID toActualId(ID id) throws Exception
	{
		return this.isVirtualId(id) ? this.getActualId(id) : id;
	}

	/**
	 * Updates the created and modified time stamps of a given data object
	 * ({@link GenericDataObject#getCreatedTimeStamp()} and {@link GenericDataObject#getModifiedTimeStamp()})
	 * with the creation and last modification time stamps from the physical file where it is stored.
	 *
	 * @param fileObject the data object to update
	 * @param pathToFile the file to get the data from
	 * @throws IOException thrown if the file's attributes cannot be read. In this case the data object is left unchanged
	 */
	protected synchronized void updateTimeStamp(DO fileObject, Path pathToFile) throws IOException
	{
		BasicFileAttributes attr = Files.readAttributes(pathToFile, BasicFileAttributes.class);

		fileObject.setCreatedTimeStamp(attr.creationTime().to(TimeUnit.MILLISECONDS));
		fileObject.setModifiedTimeStamp(attr.lastModifiedTime().to(TimeUnit.MILLISECONDS));
	}
	
	/**
	 * Initializes the instance of a file system DAO with the path
	 * to the folder where all data objects are stored. If the path
	 * does not exist the method will try to create it. If it fails an
	 * exception is thrown.
	 * To verify that the folder is writable, a test file named after the simple class name of the DAO is created
	 * and deleted again. If a file with that name already exists in the folder, the creation fails.
	 *
	 * @param storagePath the path to the folder where all data objects are stored
	 * @throws IllegalArgumentException if the provided storage path is null or it exists but is no directory.
	 * @throws IllegalStateException if the test file does not exist after it was created, or still exists after it was deleted
	 * @throws DALException re-throws every other exception as {@link DALException} (e.g. when the directory or the test file cannot be created or deleted)
	 */
	public AbstractFileSystemDAO(String storagePath) throws IllegalArgumentException, IllegalStateException, DALException
	{
		try
		{
			if (storagePath == null) throw new IllegalArgumentException("storagePath");

			this.storagePath = Paths.get(storagePath);

			if (!Files.exists(this.storagePath)) Files.createDirectories(this.storagePath);
			if (!Files.isDirectory(this.storagePath)) throw new IllegalArgumentException("storagePath");

			Path testFile = this.storagePath.resolve(this.getClass().getSimpleName());

			Files.createFile(testFile);

			if (!Files.exists(testFile)) throw new IllegalStateException();

			Files.delete(testFile);

			if (Files.exists(testFile)) throw new IllegalStateException();
		}
		catch (IllegalArgumentException | IllegalStateException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new DALException(e);
		}
	}

	/**
	 * Gets the path where data objects are stored
	 * 
	 * @return the path where data objects are stored
	 */
	public Path getStoragePath()
	{
		return this.storagePath;
	}

	/**
	 * Opens a stream of the files of the data objects, i.e. all regular files in the storage folder, including the ones in its sub folders.
	 * The caller is responsible for closing the stream.
	 *
	 * @param ids the ids of the data objects whose files shall be in the stream. If null or empty, the files of all data objects are in the stream
	 * @return a stream of the paths to the files of the data objects
	 * @throws IOException re-throws every exception thrown when opening the storage folder
	 */
	protected Stream<Path> streamDataFiles(Set<ID> ids) throws IOException
	{
		Stream<Path> dataFiles = Files.walk(this.storagePath).filter(Files::isRegularFile);

		if ((ids == null) || ids.isEmpty()) return dataFiles;

		return dataFiles.filter(file -> ids.contains(this.toID(file.getFileName().toString())));
	}

	/**
	 * Like {@link GenericDAO#doGetUnchecked(Object)}, but also accepts the path to the file of a data object as source record identifier
	 * (as provided by the data object loaders of this class). In this case the id is obtained from the file name (see {@link #toID(String)}).
	 *
	 * @param sourceRecordIdentifier the id of the data object to load, or the path to its file
	 * @return the data object of the source record or null if no source record was found
	 * @throws DALRuntimeException thrown if loading the data object failed (with a {@link com.schoste.ddd.infrastructure.dal.v3.exceptions.BrokenRecordException} as cause)
	 * @throws RuntimeException re-throws every exception thrown by {@link #toID(String)}
	 */
	@Override
	protected DO doGetUnchecked(Object sourceRecordIdentifier) throws DALRuntimeException
	{
		if (sourceRecordIdentifier instanceof Path) sourceRecordIdentifier = this.toID(((Path) sourceRecordIdentifier).getFileName().toString());

		return super.doGetUnchecked(sourceRecordIdentifier);
	}

	/**
	 * {@inheritDoc}
	 *
	 * The source records are the paths to the files of the data objects (see {@link #streamDataFiles(Set)}).
	 */
	@Override
	protected DataObjectLoader<?, ID, DO> createDOLoader(Set<ID> ids) throws Exception
	{
		return new GenericDataObjectLoader<>(this, this.streamDataFiles(ids), this::doGetUnchecked, ids);
	}

	/**
	 * {@inheritDoc}
	 *
	 * The source records are the paths to the files of the data objects (see {@link #streamDataFiles(Set)}).
	 */
	@Override
	protected DataObjectLoader<?, ID, DO> createRecentDOLoader(long lastModTS, boolean returnDeleted) throws Exception
	{
		return new GenericRecentDataObjectLoader<>(this, this.streamDataFiles(null), this::doGetUnchecked, lastModTS, returnDeleted);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void doSave(Collection<DO> fileObjects) throws Exception
	{
		for (DO fileObject : fileObjects) this.doSave(fileObject);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected synchronized void doDelete(DO fileObject) throws Exception 
	{
		if (fileObject == null) throw new IllegalArgumentException("fileObject");
		
		Path pathToFile = this.fileOf(fileObject.getId());
		
		if (Files.exists(pathToFile, LinkOption.NOFOLLOW_LINKS)) Files.delete(pathToFile);
		
		fileObject.setIsDeleted(!Files.exists(pathToFile, LinkOption.NOFOLLOW_LINKS));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void doDelete(Collection<DO> fileObjects) throws Exception
	{
		for (DO fileObject : fileObjects) this.doDelete(fileObject);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void doDelete(ID[] fileObjectIds) throws Exception
	{
		for (ID fileObjectId : fileObjectIds)
		{
			Path pathToFile = this.fileOf(fileObjectId);
			
			if (Files.exists(pathToFile, LinkOption.NOFOLLOW_LINKS)) Files.delete(pathToFile);			
		}
	}

	/**
	 * Clears the actual storage by deleting all regular files in the storage folder, including the ones in its sub folders.
	 * The folders themselves are not deleted.
	 *
	 * If a file cannot be deleted, the remaining files are still deleted. Afterwards the exception of the first file
	 * that could not be deleted is thrown (with the exceptions of the other files added as suppressed exceptions).
	 *
	 * @throws Exception re-throws every exception
	 */
	@Override
	protected void doClear() throws Exception
	{
		IOException failure = null;

		try (Stream<Path> files = Files.walk(this.storagePath))
		{
			for (Path file : (Iterable<Path>) files.filter(Files::isRegularFile)::iterator)
			{
				try
				{
					Files.delete(file);
				}
				catch (IOException e)
				{
					if (failure == null) failure = e;
					else failure.addSuppressed(e);
				}
			}
		}

		if (failure != null) throw failure;
	}
}
