package com.schoste.ddd.infrastructure.dao.v4.exceptions;

/**
 * Unchecked wrapper of a {@link DAOException}. It is thrown where checked exceptions are not possible,
 * e.g. while the stream returned by {@link com.schoste.ddd.infrastructure.dao.v4.services.DAO#getAll()} is consumed.
 * The actual {@link DAOException} is the cause of this exception (see {@link #getCause()}).
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 *
 */
public class DAORuntimeException extends RuntimeException
{
	private static final long serialVersionUID = -6302587458921745367L;

	/**
	 * Creates a new instance of the class for a given cause.
	 * If the cause is not a {@link DAOException}, it is wrapped in a new {@link DAOException}.
	 *
	 * @param cause the actual cause of the exception
	 */
	public DAORuntimeException(Throwable cause)
	{
		super((cause instanceof DAOException) ? cause : new DAOException(cause));
	}

	/**
	 * Gets the {@link DAOException} wrapped by this exception.
	 *
	 * @return the wrapped {@link DAOException}
	 */
	@Override
	public synchronized DAOException getCause()
	{
		return (DAOException) super.getCause();
	}
}
