package com.schoste.ddd.infrastructure.dal.v3.exceptions;

/**
 * Unchecked wrapper of a {@link DALException}. It is thrown where checked exceptions are not possible,
 * e.g. while the stream returned by {@link com.schoste.ddd.infrastructure.dal.v3.services.GenericDataAccessObject#getAll()} is consumed.
 * The actual {@link DALException} is the cause of this exception (see {@link #getCause()}).
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 *
 */
public class DALRuntimeException extends RuntimeException
{
	private static final long serialVersionUID = -6302587458921745367L;

	/**
	 * Creates a new instance of the class for a given cause.
	 * If the cause is not a {@link DALException}, it is wrapped in a new {@link DALException}
	 * (which notifies {@link DALException#handler}).
	 *
	 * @param cause the actual cause of the exception
	 */
	public DALRuntimeException(Throwable cause)
	{
		super((cause instanceof DALException) ? cause : new DALException(cause));
	}

	/**
	 * Gets the {@link DALException} wrapped by this exception.
	 *
	 * @return the wrapped {@link DALException}
	 */
	@Override
	public synchronized DALException getCause()
	{
		return (DALException) super.getCause();
	}
}
