package com.schoste.ddd.infrastructure.dal.v2.services;

import com.schoste.ddd.infrastructure.dal.v2.exceptions.DALException;

/**
 * Version 2 interface to a handler of {@see DALException}
 * If there is an implementation and the instance is set at {@see DALException#handler}, this handler is
 * called whenever a {@see DALException} is created.
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 */
public interface DALExceptionHandler
{
	/**
	 * Called by constructors of {@see DALException}
	 * 
	 * @param exception the exception that was created
	 */
	void onExceptionCreated(DALException exception);
}
