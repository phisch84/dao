package com.schoste.ddd.infrastructure.dao.v4.fs;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import com.schoste.ddd.infrastructure.dao.v4.services.AbstractDAO;
import com.schoste.ddd.infrastructure.dao.v4.services.DataObjectLoader;
import com.schoste.ddd.infrastructure.dao.v4.services.SourceRecordDataObjectLoader;
import com.schoste.ddd.infrastructure.dao.v4.services.DAO;
import com.schoste.ddd.infrastructure.dao.v4.services.DeletedDataObjects;

import com.schoste.ddd.infrastructure.dao.v4.exceptions.DAOException;
import com.schoste.ddd.infrastructure.dao.v4.exceptions.DAORuntimeException;
import com.schoste.ddd.infrastructure.dao.v4.models.DataObject;

/**
 * Basic class with functionality common to all data access objects which store every data object in its own file
 * in a folder of the file system (see {@link com.schoste.ddd.infrastructure.dao.v4.fs.AbstractRawFileDAO} and
 * {@link com.schoste.ddd.infrastructure.dao.v4.serialization.AbstractSerializationDAO}).
 * The name of a data object's file is the string value of its id ({@link DataObject#getId()}).
 * The data object loaders iterate over the files in the storage folder and load each data object via {@link #convertSourceRecord(Object)}.
 *
 * The storage folder, including its sub folders, must contain nothing but the files of the data objects: every file is read
 * as data object. A file whose name isn't an id ({@link #toId(String)}) or whose content can't be read is reported as broken
 * record while a stream is consumed (a {@link DAORuntimeException} with a
 * {@link com.schoste.ddd.infrastructure.dao.v4.exceptions.BrokenRecordException} as cause), e.g. a stray {@code notes.txt}.
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 *
 * @param <ID> the class of the data object's unique identifier
 * @param <DO> the class of the data object to persist
 */
abstract public class AbstractFileStorageDAO<ID extends Serializable, DO extends DataObject<ID>> extends AbstractDAO<ID, DO>
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
	abstract protected ID toId(String value);

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
	 * Creates the actual id under which a data object with a virtual id is stored for the first time.
	 * The file of the data object is named after this id, so it must not belong to an existing file.
	 *
	 * @param virtualId the virtual id of the data object
	 * @return a new actual id
	 * @throws Exception re-throws every exception
	 */
	protected abstract ID doCreateActualId(ID virtualId) throws Exception;

	/**
	 * Gets the id under which a data object is to be stored: the given id, or the id returned by {@link #doCreateActualId(Serializable)}
	 * if the given id is virtual ({@link #isVirtualId(Serializable)}).
	 *
	 * @param id the id of the data object
	 * @return the id to store the data object with
	 * @throws Exception re-throws every exception
	 */
	protected ID toActualId(ID id) throws Exception
	{
		return this.isVirtualId(id) ? this.doCreateActualId(id) : id;
	}

	/**
	 * Updates the created and modified time stamps of a given data object
	 * ({@link DataObject#getCreatedTimestamp()} and {@link DataObject#getModifiedTimestamp()})
	 * with the creation and last modification time stamps from the physical file where it is stored.
	 *
	 * @param fileObject the data object to update
	 * @param pathToFile the file to get the data from
	 * @throws IOException thrown if the file's attributes cannot be read. In this case the data object is left unchanged
	 */
	protected synchronized void updateTimeStamp(DO fileObject, Path pathToFile) throws IOException
	{
		BasicFileAttributes attr = Files.readAttributes(pathToFile, BasicFileAttributes.class);

		fileObject.setCreatedTimestamp(attr.creationTime().to(TimeUnit.MILLISECONDS));
		fileObject.setModifiedTimestamp(attr.lastModifiedTime().to(TimeUnit.MILLISECONDS));
	}
	
	/**
	 * Initializes the instance of a file system DAO with the path
	 * to the folder where all data objects are stored. If the path
	 * does not exist the method will try to create it. If it fails an
	 * exception is thrown.
	 * To verify that the folder is writable, a test file named after the simple class name of the DAO is created
	 * and deleted again. If a file with that name already exists in the folder, the constructor throws a {@link DAOException}.
	 *
	 * @param storagePath the path to the folder where all data objects are stored
	 * @throws IllegalArgumentException if the provided storage path is null or it exists but is no directory.
	 * @throws IllegalStateException if the test file does not exist after it was created, or still exists after it was deleted
	 * @throws DAOException re-throws every other exception as {@link DAOException} (e.g. when the directory or the test file cannot be created or deleted)
	 */
	public AbstractFileStorageDAO(String storagePath) throws IllegalArgumentException, IllegalStateException, DAOException
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
			throw new DAOException(e);
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

		return dataFiles.filter(file -> ids.contains(this.toId(file.getFileName().toString())));
	}

	/**
	 * Like {@link AbstractDAO#convertSourceRecord(Object)}, but also accepts the path to the file of a data object as source record identifier
	 * (as provided by the data object loaders of this class). In this case the id is obtained from the file name (see {@link #toId(String)}).
	 *
	 * @param sourceRecordIdentifier the id of the data object to load, or the path to its file
	 * @return the data object of the source record or null if no source record was found
	 * @throws DAORuntimeException thrown if loading the data object failed (with a {@link com.schoste.ddd.infrastructure.dao.v4.exceptions.BrokenRecordException} as cause)
	 * @throws RuntimeException thrown if a file name isn't an id ({@link #toId(String)} fails); the data object loaders report it as broken record, like a data object which can't be loaded
	 */
	@Override
	protected DO convertSourceRecord(Object sourceRecordIdentifier) throws DAORuntimeException
	{
		if (sourceRecordIdentifier instanceof Path) sourceRecordIdentifier = this.toId(((Path) sourceRecordIdentifier).getFileName().toString());

		return super.convertSourceRecord(sourceRecordIdentifier);
	}

	/**
	 * {@inheritDoc}
	 *
	 * The source records are the paths to the files of the data objects (see {@link #streamDataFiles(Set)}).
	 */
	@Override
	protected DataObjectLoader<?, ID, DO> doCreateLoader(Set<ID> ids) throws Exception
	{
		return SourceRecordDataObjectLoader.forIds(this, this.streamDataFiles(ids), this::convertSourceRecord, ids);
	}

	/**
	 * {@inheritDoc}
	 *
	 * The source records are the paths to the files of all data objects (see {@link #streamDataFiles(Set)}).
	 */
	@Override
	protected DataObjectLoader<?, ID, DO> doCreateLoader(long modifiedSince, DeletedDataObjects deleted) throws Exception
	{
		return SourceRecordDataObjectLoader.forModifiedSince(this, this.streamDataFiles(null), this::convertSourceRecord, modifiedSince, deleted);
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
