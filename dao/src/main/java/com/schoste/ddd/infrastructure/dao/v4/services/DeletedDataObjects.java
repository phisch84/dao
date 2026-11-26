package com.schoste.ddd.infrastructure.dao.v4.services;

import com.schoste.ddd.infrastructure.dao.v4.models.DataObject;

/**
 * Says whether a read operation of a {@link DAO} includes the data objects which are flagged as deleted
 * ({@link DataObject#isDeleted()} == true), e.g. {@code dao.getModifiedSince(timestamp, DeletedDataObjects.INCLUDE)}.
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 */
public enum DeletedDataObjects
{
	/**
	 * Data objects flagged as deleted are left out.
	 */
	EXCLUDE,

	/**
	 * Data objects flagged as deleted are included.
	 */
	INCLUDE
}
