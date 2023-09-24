package com.schoste.ddd.infrastructure.dal.v2.models;

import java.io.Serializable;

/**
 * Functionality of the basic data object from which other data object classes should derive.
 * Data objects are persisted either by serialization or persisted in a database.
 * 
 * @author Philipp Schosteritsch
 */
public abstract class GenericDataObject implements Serializable
{
	private static final long serialVersionUID = -2290296019886002692L; // use Id of V1 for compatibility (no fields changed)

	private int id=0;

	private long createdTimeStamp=0;
	
	private long modifiedTimeStamp=0;
	
	private boolean isDeleted=false;
	
	/**
	 * Gets the numeric id of the object in the database.
	 * If the id is zero or smaller, the object was not persisted yet
	 * 
	 * @return the numeric id of the object in the database
	 */
	public int getId() 
	{
		return id;
	}

	/**
	 * Sets the numeric id of the object in the database.
	 * If the id is zero or smaller, the object was not persisted yet.
	 * When the object is persisted, such an id will be overwritten.
	 * 
	 * @param id the numeric id of the object in the database
	 */
	public void setId(int id) 
	{
		this.id = id;
	}

	/**
	 * Gets the time stamp of the creation date
	 * 
	 * @return the time stamp of the creation date
	 */
	public long getCreatedTimeStamp() 
	{
		return createdTimeStamp;
	}

	/**
	 * Sets the time stamp of the creation date
	 * 
	 * @param createdTimeStamp the time stamp of the creation date
	 */
	public void setCreatedTimeStamp(long createdTimeStamp) 
	{
		this.createdTimeStamp = createdTimeStamp;
	}

	/**
	 * Gets the time stamp of the last modification date
	 * 
	 * @return the time stamp of the last modification date
	 */
	public long getModifiedTimeStamp() 
	{
		return modifiedTimeStamp;
	}

	/**
	 * Sets the time stamp of the last modification date.
	 * If the object was newly created, the modification date should be equal to the creation date
	 * 
	 * @param modifiedTimeStamp the time stamp of the last modification date
	 */
	public void setModifiedTimeStamp(long modifiedTimeStamp) 
	{
		this.modifiedTimeStamp = modifiedTimeStamp;
	}
	
	/**
	 * Sets the deleted-flag in the database
	 * 
	 * @param isDeleted true if the object is marked as deleted. False otherwise
	 */
	public void setIsDeleted(boolean isDeleted)
	{
		this.isDeleted = isDeleted;
	}
	
	/**
	 * Gets the deleted-flag of the object in the database
	 * 
	 * @return true if the object is marked as deleted. False otherwise
	 */
	public boolean getIsDeleted()
	{
		return this.isDeleted;
	}
	
	/**
	 * Updates the time stamps of the instance before it is persisted.
	 * To be called by the entity manager before the instance is persisted.
	 */
	public void updateTimeStamps()
	{
		long unixTs = System.currentTimeMillis();
		
		if (this.getCreatedTimeStamp() < 1) this.setCreatedTimeStamp(unixTs);
		
		this.setModifiedTimeStamp(unixTs);
	}
	
	/**
	 * Gets the id of the object as hash code
	 */
	@Override
	public int hashCode() 
	{
		return this.getId();
	}

	/**
	 * Determines if the object is equal to another object by looking
	 * on the id, and the time stamps
	 * 
	 * @param obj the object to compare to
	 * @return true if both objects are equal, false otherwise
	 */
	@Override
	public boolean equals(Object obj) 
	{
		if (this == obj) return true;
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		
		GenericDataObject other = (GenericDataObject) obj;
		
		if (createdTimeStamp != other.createdTimeStamp) return false;
		if (modifiedTimeStamp != other.modifiedTimeStamp) return false;
		if (id != other.id) return false;

		return true;
	}
}
