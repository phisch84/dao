package com.schoste.ddd.infrastructure.dao.v4.exceptions;

import com.schoste.ddd.infrastructure.dao.v4.services.DAO;

/**
 * Basic exception for all exceptions thrown by classes and services of the DAO library
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 *
 */
public class DAOException extends Exception
{
	private static final long serialVersionUID = 3159659145507596391L;

	/**
	 * Creates a new instance of the class
	 */
	public DAOException()
	{
		super();
	}

	/**
	 * Creates a new instance of the class for a given cause
	 *
	 * @param cause the actual cause of the exception
	 */
	public DAOException(Throwable cause)
	{
		super(cause);
	}
}
