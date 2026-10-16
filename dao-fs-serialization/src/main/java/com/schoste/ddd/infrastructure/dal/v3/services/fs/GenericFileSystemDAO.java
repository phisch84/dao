package com.schoste.ddd.infrastructure.dal.v3.services.fs;

import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;

import com.schoste.ddd.infrastructure.dal.v3.services.AbstractFileSystemDAO;
import com.schoste.ddd.infrastructure.dal.v3.exceptions.DALException;
import com.schoste.ddd.infrastructure.dal.v3.models.GenericFileObject;

/**
 * Implementation of the GenericDataAccessObject interface to persist file objects
 * ({@link GenericFileObject}) to the file system. The data of every file object ({@link GenericFileObject#getData()})
 * is stored in its own file.
 *
 * When files are saved, their content is completely overwritten and never appended.
 *
 * Note that the maximum size of a single file is Integer.MAX_VALUE bytes.
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 *
 * @param <ID> the class of the data object's unique identifier
 * @param <DO> the class of the data object to persist
 */
abstract public class GenericFileSystemDAO<ID extends Serializable, DO extends GenericFileObject<ID>> extends AbstractFileSystemDAO<ID, DO>
{
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
	public GenericFileSystemDAO(String storagePath) throws IllegalArgumentException, IllegalStateException, DALException
	{
		super(storagePath);
	}

	/**
	 * Reads the file object with the given id from the file named after the id.
	 * The file object is created via {@link #createDataObject(Object...)} and gets the given id, the content of the file
	 * as data ({@link GenericFileObject#setData(byte[])}) and the creation and last modification time stamps of the file
	 * (see {@link #updateTimeStamp(com.schoste.ddd.infrastructure.dal.v3.models.GenericDataObject, Path)}).
	 *
	 * @param id the id of the file object to get
	 * @return the file object with the given id or null if there is no file for the id
	 * @throws IllegalStateException if the path of the id is not a regular file, is not readable or is bigger than Integer.MAX_VALUE bytes
	 * @throws Exception re-throws every exception
	 */
	@Override
	protected synchronized DO doGet(ID id) throws IllegalStateException, Exception
	{
		Path pathToFile = this.getReadableFile(id);

		if (pathToFile == null) return null;
		if (Files.size(pathToFile) > Integer.MAX_VALUE) throw new IllegalStateException();

		DO fileObject = this.createDataObject();
		   fileObject.setId(id);
		   fileObject.setData(Files.readAllBytes(pathToFile));

		this.updateTimeStamp(fileObject, pathToFile);
		
		return fileObject;
	}

	/**
	 * Writes the data of the file object ({@link GenericFileObject#getData()}) to the file named after its id.
	 * An existing file is completely overwritten.
	 * If the file object has a virtual id ({@link #isVirtualId(Serializable)}), it is replaced by the id returned by
	 * {@link #getActualId(Object...)} (called with the virtual id) before the file is written.
	 * After the file was written, the file object's time stamps are replaced by the creation and last modification time stamps
	 * of the file (see {@link #updateTimeStamp(com.schoste.ddd.infrastructure.dal.v3.models.GenericDataObject, Path)}).
	 *
	 * @param fileObject the file object to save
	 * @throws IllegalArgumentException if the file object is null
	 * @throws Exception re-throws every exception
	 */
	@Override
	protected synchronized void doSave(DO fileObject) throws Exception
	{
		if (fileObject == null) throw new IllegalArgumentException("fileObject");

		ID fileId = this.toActualId(fileObject.getId());
		Path pathToFile = this.fileOf(fileId);

		Files.write(pathToFile, fileObject.getData());

		fileObject.setId(fileId);
		
		this.updateTimeStamp(fileObject, pathToFile);
	}
}
