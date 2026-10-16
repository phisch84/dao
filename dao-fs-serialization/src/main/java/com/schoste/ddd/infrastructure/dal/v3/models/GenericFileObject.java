package com.schoste.ddd.infrastructure.dal.v3.models;

import java.io.Serializable;

/**
 * Implementation of the basic file object from which other file object classes should derive.
 * File objects usually model data which are stored in files (e.g. images, binary data, etc.).
 * They are used when binary data of data objects are not stored as BLOBs but in files on a file system.
 * 
 * @author Philipp Schosteritsch
 *
 * @param <ID> the class of the file object's unique identifier
 */
public abstract class GenericFileObject<ID extends Serializable> extends GenericDataObject<ID>
{
	private static final long serialVersionUID = 5905499331175572611L;
	
	protected byte[] data = new byte[0];
	
	/**
	 * Gets the file data
	 * 
	 * @return the data of the file
	 */
	public byte[] getData()
	{
		return this.data;
	}
	
	/**
	 * Sets the file data to be written to storage
	 * 
	 * @param data the file data to be written to storage
	 */
	public void setData(byte[] data)
	{
		this.data = data;
	}
}
