/**
 * An in-memory DAO (Data Access Object) for tests of code which uses DAOs:
 * {@link com.schoste.ddd.infrastructure.dao.v4.inmemory.AbstractInMemoryDAO}. It fulfills the whole DAO contract,
 * so tests behave as with a real storage, without one. It is part of the test jar ({@code dao4}, classifier {@code tests}).
 *
 * Example for the data object {@code CustomerDO} (see the overview of the {@code dao4} artifact):
 * <pre>
 * import com.schoste.ddd.infrastructure.dao.v4.inmemory.AbstractInMemoryDAO;
 *
 * public class InMemoryCustomerDAO extends AbstractInMemoryDAO&lt;CustomerDO&gt;
 * {
 *     private long lastVirtualId = 0;
 *
 *     protected CustomerDO doCreateDataObject() { return new CustomerDO(); }
 *
 *     // virtual ids are negative; the in-memory DAO gives actual ids 1, 2, 3, ...
 *     protected synchronized Long doCreateVirtualId() { return --this.lastVirtualId; }
 *     protected boolean doIsVirtualId(Long id) { return id &lt;= 0; }
 * }
 * </pre>
 *
 * The classes {@code InMemoryDAOImpl} and {@code InMemoryDO} are the in-memory DAO used by the tests of this library.
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 */
package com.schoste.ddd.infrastructure.dao.v4.inmemory;
