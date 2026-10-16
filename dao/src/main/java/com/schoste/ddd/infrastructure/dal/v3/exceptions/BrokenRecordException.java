package com.schoste.ddd.infrastructure.dal.v3.exceptions;

/**
 * Exception for a record in the storage which can't be loaded as data object (e.g. a corrupt file).
 * When data objects are streamed, it is thrown as cause of a {@link DALRuntimeException}. Unlike other failures,
 * it does not end the loading: a caller iterating over the stream (see {@link java.util.stream.Stream#iterator()})
 * can catch the exception and continue with the next record.
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 *
 */
public class BrokenRecordException extends DALException
{
	private static final long serialVersionUID = 4791286357915376023L;

	/**
	 * The source record (e.g. the id or the file of the data object) which can't be loaded.
	 */
	private final transient Object sourceRecord;

	/**
	 * Creates a new instance of the class
	 *
	 * @param sourceRecord the source record (e.g. the id or the file of the data object) which can't be loaded
	 * @param cause the actual cause of the exception
	 */
	public BrokenRecordException(Object sourceRecord, Throwable cause)
	{
		super(cause, false);

		this.sourceRecord = sourceRecord;

		invokeHandler(this);
	}

	/**
	 * Gets the source record (e.g. the id or the file of the data object) which can't be loaded.
	 *
	 * @return the source record, or null if the exception was deserialized
	 */
	public Object getSourceRecord()
	{
		return this.sourceRecord;
	}
}
