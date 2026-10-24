package com.schoste.ddd.infrastructure.dao.v4.models;

import java.io.Serializable;
import com.schoste.ddd.infrastructure.dao.v4.services.DAO;

/**
 * Functionality of the basic data object (DO) from which other data object classes must derive.
 * Data objects are persisted either by serialization or persisted in a database.
 * 
 * @param <ID> the data type of the DO's unique identifier
 * @author Philipp Schosteritsch
 */
public abstract class DataObject<ID extends Serializable> implements Serializable
{
	private static final long serialVersionUID = -2290296019886002692L; // use Id of V1 for compatibility (no fields changed)

	private ID id;

	private long createdTimestamp=0;
	
	private long modifiedTimestamp=0;
	
	private boolean deleted=false;
	
	/**
	 * Gets the unique identifier of the object in the database.
	 * 
	 * @return the unique identifier of the object in the database
	 */
	public ID getId() 
	{
		return id;
	}

	/**
	 * Sets the unique identifier of the object in the database.
	 * When the object is persisted, the id may be overwritten
	 * (e.g. a virtual id may be turned into an actual id)
	 * 
	 * @param id the unique identifier of the object in the database
	 */
	public void setId(ID id) 
	{
		this.id = id;
	}

	/**
	 * Gets the time stamp of the creation date. It is set by the first call to {@link DataObject#updateTimestamps()},
	 * which is either when the data object is created via the data access object or when it is persisted for the first time.
	 * If neither happened yet, the time stamp is expected to be 0.
	 * 
	 * @return the time stamp of the creation date
	 */
	public long getCreatedTimestamp() 
	{
		return createdTimestamp;
	}

	/**
	 * Sets the time stamp of the creation date (see {@link DataObject#getCreatedTimestamp()}).
	 * 
	 * @param createdTimestamp the time stamp of the creation date
	 */
	public void setCreatedTimestamp(long createdTimestamp) 
	{
		this.createdTimestamp = createdTimestamp;
	}

	/**
	 * Gets the time stamp of the last modification date. It is set by every call to {@link DataObject#updateTimestamps()},
	 * which is when the data object is created via the data access object and whenever it is persisted.
	 * If neither happened yet, the time stamp is expected to be 0.
	 * 
	 * @return the time stamp of the last modification date
	 */
	public long getModifiedTimestamp() 
	{
		return modifiedTimestamp;
	}

	/**
	 * Sets the time stamp of the last modification date (see {@link DataObject#getModifiedTimestamp()}).
	 * 
	 * @param modifiedTimestamp the time stamp of the last modification date
	 */
	public void setModifiedTimestamp(long modifiedTimestamp) 
	{
		this.modifiedTimestamp = modifiedTimestamp;
	}
	
	/**
	 * Sets the deleted-flag
	 * 
	 * @param deleted true if the object is marked as deleted. False otherwise
	 */
	public void setDeleted(boolean deleted)
	{
		this.deleted = deleted;
	}
	
	/**
	 * Gets the deleted-flag of the object
	 * 
	 * @return true if the object is marked as deleted. False otherwise
	 */
	public boolean isDeleted()
	{
		return this.deleted;
	}
	
	/**
	 * Updates the time stamps of the instance to the current time.
	 * The modification time stamp is always updated. The creation time stamp is only set if it is smaller than 1
	 * (i.e. it was not set yet).
	 * To be called by the DAO when the instance is created and before it is persisted.
	 */
	public void updateTimestamps()
	{
		long unixTs = System.currentTimeMillis();
		
		if (this.getCreatedTimestamp() < 1) this.setCreatedTimestamp(unixTs);
		
		this.setModifiedTimestamp(unixTs);
	}
	
	/**
	 * Gets the id of the object as hash code.
	 * The return value is the hash code of the data object's id (see {@link DataObject#getId()}).
	 * If the id is null the method returns 0.
	 * 
	 * @return the hash code of this instance
	 */
	@Override
	public int hashCode() 
	{
		ID id = this.getId();

		if (id == null) return 0;

		return id.hashCode();
	}

	/**
	 * Determines if the object is equal to another object by looking
	 * on the runtime class, the id, and the time stamps.
	 * The deleted-flag ({@link DataObject#isDeleted()}) is not considered.
	 * Two objects of the same class with equal time stamps and both without id are considered equal.
	 *
	 * @param obj the object to compare to
	 * @return true if both objects are equal, false otherwise
	 */
	@SuppressWarnings("unchecked")
	@Override
	public boolean equals(Object obj) 
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		
		DataObject<ID> other = (DataObject<ID>) obj;
		
		if (createdTimestamp != other.createdTimestamp) return false;
		if (modifiedTimestamp != other.modifiedTimestamp) return false;
		if ((id == null) && (other.id != null)) return false;
		if ((id == null) && (other.id == null)) return true;
		if (!id.equals(other.id)) return false;

		return true;
	}
}
