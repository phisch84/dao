/**
 * The DAO interface and the base class for its implementations.
 *
 * <ul>
 * <li>{@link com.schoste.ddd.infrastructure.dao.v4.services.DAO}: the interface callers use (DAO = Data Access Object).
 * Its class documentation lists the rules which hold for every DAO.</li>
 * <li>{@link com.schoste.ddd.infrastructure.dao.v4.services.DeletedDataObjects}: says whether reads include data objects flagged as deleted.</li>
 * <li>{@link com.schoste.ddd.infrastructure.dao.v4.services.AbstractDAO}: the base class for implementations; an implementation only
 * implements the methods whose names start with {@code do}.</li>
 * <li>{@link com.schoste.ddd.infrastructure.dao.v4.services.DataObjectLoader} and
 * {@link com.schoste.ddd.infrastructure.dao.v4.services.SourceRecordDataObjectLoader}: load the data objects of returned streams
 * lazily; implementations create them in {@code doCreateLoader}.</li>
 * </ul>
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 */
package com.schoste.ddd.infrastructure.dao.v4.services;
