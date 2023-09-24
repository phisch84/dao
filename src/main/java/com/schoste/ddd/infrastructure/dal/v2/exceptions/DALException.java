package com.schoste.ddd.infrastructure.dal.v2.exceptions;

import com.schoste.ddd.infrastructure.dal.v2.services.DALExceptionHandler;

/**
 * Basic exception for all exceptions thrown by classes/service from the DAL
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 */
public class DALException extends Exception
{
	private static final long serialVersionUID = 3159659145507596391L;

	/**
	 * Instance to a handler for {@see DALException}.
	 * If not null {@ DALExceptionHandler#onExceptionCreated(DALException)} will be
	 * called whenever a {@see DALException} is created.
	 */
	public static DALExceptionHandler handler = null;

	protected static void invokeHandler(DALException exception)
	{
		try
		{
			if (handler != null) handler.onExceptionCreated(exception);
		}
		catch (Exception e)
		{
			e.printStackTrace(System.err);
		}
	}

	/**
	 * Creates a new instance of the class
	 */
	public DALException()
	{
		super();

		invokeHandler(this);
	}

	/**
	 * Creates a new instance of the class for a given cause
	 * 
	 * @param cause the actual cause of the exception
	 */
	public DALException(Throwable cause)
	{
		super(cause);

		invokeHandler(this);
	}
}
