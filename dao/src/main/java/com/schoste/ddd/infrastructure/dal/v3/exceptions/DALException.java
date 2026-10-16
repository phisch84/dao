package com.schoste.ddd.infrastructure.dal.v3.exceptions;

import com.schoste.ddd.infrastructure.dal.v3.services.DALExceptionHandler;

/**
 * Basic exception for all exceptions thrown by classes/service from the DAL
 * 
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 *
 */
public class DALException extends Exception
{
	private static final long serialVersionUID = 3159659145507596391L;

	/**
	 * Instance to a handler for {@link DALException}.
	 * If not null {@link DALExceptionHandler#onExceptionCreated(DALException)} will be
	 * called whenever a {@link DALException} is created.
	 */
	public static DALExceptionHandler handler = null;

	/**
	 * Called whenever a new {@link DALException} is created.
	 * If a handler is present {@link DALExceptionHandler#onExceptionCreated(DALException)} will be called
	 * 
	 * @param exception the exception that was created
	 */
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
		this(cause, true);
	}

	/**
	 * Creates a new instance of the class for a given cause.
	 * Deriving classes which need to initialize their own fields before the handler is notified
	 * pass false and call {@link #invokeHandler(DALException)} themselves.
	 *
	 * @param cause the actual cause of the exception
	 * @param notifyHandler if true, the handler is notified (see {@link #invokeHandler(DALException)})
	 */
	protected DALException(Throwable cause, boolean notifyHandler)
	{
		super(cause);

		if (notifyHandler) invokeHandler(this);
	}
}
