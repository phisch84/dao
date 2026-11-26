/**
 * DAOs (Data Access Objects) which store every data object in its own file of a folder; the file is named after the
 * data object's id.
 *
 * <ul>
 * <li>{@link com.schoste.ddd.infrastructure.dao.v4.fs.AbstractFileStorageDAO}: the common base class of the file storage DAOs
 * (also of {@link com.schoste.ddd.infrastructure.dao.v4.serialization.AbstractSerializationDAO}).</li>
 * <li>{@link com.schoste.ddd.infrastructure.dao.v4.fs.AbstractRawFileDAO} with
 * {@link com.schoste.ddd.infrastructure.dao.v4.fs.FileDataObject}: stores the data of a file object as the content of its file,
 * e.g. for images or documents. Its time stamps are the file's time stamps. It deletes physically: deletions are not reported
 * by change tracking.</li>
 * </ul>
 *
 * An implementation of {@code AbstractRawFileDAO} provides the same methods as the example in the documentation of the
 * package {@code com.schoste.ddd.infrastructure.dao.v4.serialization}: {@code doCreateDataObject}, {@code doCreateVirtualId},
 * {@code doIsVirtualId}, {@code toId} and {@code doCreateActualId}.
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 */
package com.schoste.ddd.infrastructure.dao.v4.fs;
