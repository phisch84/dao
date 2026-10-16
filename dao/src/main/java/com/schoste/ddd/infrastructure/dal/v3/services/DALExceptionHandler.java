package com.schoste.ddd.infrastructure.dal.v3.services;

import com.schoste.ddd.infrastructure.dal.v3.exceptions.DALException;

/**
 * Version 3 interface to a handler of {@link DALException}
 * If there is an implementation and the instance is set at {@link DALException#handler}, this handler is
 * called whenever a {@link DALException} is created.
 * 
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 *
 */
public interface DALExceptionHandler
{
	/**
	 * Called by constructors of {@link DALException}.
	 * Exceptions thrown by this method are printed to {@link System#err} and not re-thrown.
	 *
	 * @param exception the exception that was created
	 */
	void onExceptionCreated(DALException exception);
}
