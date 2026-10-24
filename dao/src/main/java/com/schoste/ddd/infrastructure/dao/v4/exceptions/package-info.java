/**
 * The exceptions of the DAO library.
 *
 * <ul>
 * <li>{@link com.schoste.ddd.infrastructure.dao.v4.exceptions.DAOException}: thrown by the methods of a DAO for every failure except
 * invalid arguments ({@link java.lang.IllegalArgumentException}); the original exception is its cause.</li>
 * <li>{@link com.schoste.ddd.infrastructure.dao.v4.exceptions.DAORuntimeException}: the unchecked form, thrown while a returned
 * stream is consumed (streams can't throw checked exceptions); the {@code DAOException} is its cause.</li>
 * <li>{@link com.schoste.ddd.infrastructure.dao.v4.exceptions.BrokenRecordException}: the cause of a {@code DAORuntimeException}
 * if a single record of the storage can't be loaded; consuming the stream by its iterator can continue with the next record.</li>
 * </ul>
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 */
package com.schoste.ddd.infrastructure.dao.v4.exceptions;
