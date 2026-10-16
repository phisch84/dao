package com.schoste.ddd.infrastructure.dal.v3.services.serialization;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;

import com.schoste.ddd.infrastructure.dal.v3.services.AbstractFileSystemDAO;
import com.schoste.ddd.infrastructure.dal.v3.exceptions.DALException;
import com.schoste.ddd.infrastructure.dal.v3.models.GenericDataObject;

/**
 * Implementation of the GenericDataAccessObject interface to persist data objects
 * via serialization. Every data object is serialized into its own file.
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 *
 * @param <ID> the class of the data object's unique identifier
 * @param <DO> the class of the data object to persist
 */
public abstract class GenericSerializationDAO<ID extends Serializable, DO extends GenericDataObject<ID>> extends AbstractFileSystemDAO<ID, DO>
{
	/**
	 * Initializes the instance of a serialization DAO with the path
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
	public GenericSerializationDAO(String storagePath) throws IllegalArgumentException, IllegalStateException, DALException
	{
		super(storagePath);
	}

	/**
	 * Deserializes the data object with the given id from the file named after the id.
	 * The data object is returned as it was serialized, including its time stamps.
	 * The class of the deserialized object is not checked.
	 *
	 * @param id the id of the data object to get
	 * @return the data object with the given id or null if there is no file for the id
	 * @throws IllegalStateException if the path of the id is not a regular file or is not readable
	 * @throws Exception re-throws every exception (e.g. if the file cannot be deserialized)
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected synchronized DO doGet(ID id) throws Exception
	{
		Path pathToFile = this.getReadableFile(id);

		if (pathToFile == null) return null;

		try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(pathToFile)))
		{
			return (DO) ois.readObject();
		}
	}

	/**
	 * Serializes the data object into the file named after its id. An existing file is completely overwritten.
	 * If the data object has a virtual id ({@link #isVirtualId(Serializable)}), it is replaced by the id returned by
	 * {@link #getActualId(Object...)} (called with the virtual id) before the file is written.
	 * The data object's time stamps are expected to be up to date already (as {@link #save(GenericDataObject)} does).
	 * The last modification time stamp of the file is set to the data object's modification time stamp after it was serialized.
	 *
	 * @param dataObject the data object to save
	 * @throws Exception re-throws every exception
	 */
	@Override
	protected synchronized void doSave(DO dataObject) throws Exception
	{
		dataObject.setId(this.toActualId(dataObject.getId()));

		Path pathToFile = this.fileOf(dataObject.getId());

		try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(pathToFile)))
		{
			oos.writeObject(dataObject);
		}

		pathToFile.toFile().setLastModified(dataObject.getModifiedTimeStamp());
	}
}
