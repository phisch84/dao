package com.schoste.ddd.infrastructure.dao.v4.models;

import java.io.Serializable;
import java.util.Objects;

import com.schoste.ddd.infrastructure.dao.v4.services.DAO;

/**
 * Base class of all data objects (DOs): the records which a DAO ({@link DAO}) loads from and stores into its storage.
 * Classes of data objects extend this class and add their own fields.
 *
 * A data object has
 * <ul>
 * <li>an id: virtual while the data object hasn't been saved yet, actual afterwards (see {@link DAO#isVirtualId(Serializable)});</li>
 * <li>a creation and a modification time stamp in milliseconds, set by the storage when the data object is saved
 * (0 while it hasn't been saved yet);</li>
 * <li>a deleted flag, set when the data object is deleted logically (see {@link DAO#delete(DataObject)}).</li>
 * </ul>
 *
 * Data objects are only equal to themselves ({@link Object#equals(Object)} isn't overridden), because their id and time stamps change
 * while they are used: saving gives a new data object its actual id, and every save changes its modification time stamp.
 * Use {@link #isSameRecordAs(DataObject)} or {@link #isSameVersionAs(DataObject)} to compare data objects.
 *
 * @param <ID> the class of the data object's id
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 */
public abstract class DataObject<ID extends Serializable> implements Serializable
{
	// version 4 data objects can't be read from storages written with version 3, since their package and fields differ
	private static final long serialVersionUID = -2290296019886002692L;

	private ID id;

	private long createdTimestamp=0;

	private long modifiedTimestamp=0;

	private boolean deleted=false;

	/**
	 * Gets the id of the data object.
	 *
	 * @return the id, virtual or actual (see {@link DAO#isVirtualId(Serializable)})
	 */
	public ID getId()
	{
		return id;
	}

	/**
	 * Sets the id of the data object. Usually only the DAO sets it: {@link DAO#createDataObject()} sets a virtual id,
	 * and saving replaces it by the actual id.
	 *
	 * @param id the id of the data object
	 */
	public void setId(ID id)
	{
		this.id = id;
	}

	/**
	 * Gets the time stamp of the data object's creation in the storage, in milliseconds since 1970-01-01 UTC.
	 * It is set by the storage when the data object is saved for the first time; 0 while it hasn't been saved yet.
	 *
	 * @return the creation time stamp
	 */
	public long getCreatedTimestamp()
	{
		return createdTimestamp;
	}

	/**
	 * Sets the creation time stamp (see {@link #getCreatedTimestamp()}). Usually only the DAO sets it.
	 *
	 * @param createdTimestamp the creation time stamp
	 */
	public void setCreatedTimestamp(long createdTimestamp)
	{
		this.createdTimestamp = createdTimestamp;
	}

	/**
	 * Gets the time stamp of the data object's last modification in the storage, in milliseconds since 1970-01-01 UTC.
	 * It is set by the storage whenever the data object is saved or deleted logically; 0 while it hasn't been saved yet.
	 * Change tracking compares it (see {@link DAO#getModifiedSince(long)}).
	 *
	 * @return the modification time stamp
	 */
	public long getModifiedTimestamp()
	{
		return modifiedTimestamp;
	}

	/**
	 * Sets the modification time stamp (see {@link #getModifiedTimestamp()}). Usually only the DAO sets it.
	 *
	 * @param modifiedTimestamp the modification time stamp
	 */
	public void setModifiedTimestamp(long modifiedTimestamp)
	{
		this.modifiedTimestamp = modifiedTimestamp;
	}

	/**
	 * Sets the deleted flag. Usually {@link DAO#delete(DataObject)} sets it; saving a deleted data object with the flag
	 * set to false undeletes it.
	 *
	 * @param deleted true if the data object is deleted
	 */
	public void setDeleted(boolean deleted)
	{
		this.deleted = deleted;
	}

	/**
	 * Gets the deleted flag
	 *
	 * @return true if the data object is deleted
	 */
	public boolean isDeleted()
	{
		return this.deleted;
	}

	/**
	 * Checks if another data object belongs to the same record of the storage, i.e. it has the same id.
	 *
	 * @param other the data object to compare with
	 * @return true if both data objects have the same id; false if other is null or one of the ids is null
	 */
	public boolean isSameRecordAs(DataObject<ID> other)
	{
		if ((other == null) || (this.id == null)) return false;

		return Objects.equals(this.id, other.getId());
	}

	/**
	 * Checks if another data object is the same version of the same record, i.e. it has the same id and the same
	 * modification time stamp.
	 *
	 * @param other the data object to compare with
	 * @return true if both data objects have the same id and the same modification time stamp
	 */
	public boolean isSameVersionAs(DataObject<ID> other)
	{
		return this.isSameRecordAs(other) && (this.modifiedTimestamp == other.getModifiedTimestamp());
	}
}
