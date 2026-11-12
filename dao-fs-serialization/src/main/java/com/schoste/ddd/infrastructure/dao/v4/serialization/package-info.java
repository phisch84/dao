/**
 * A DAO (Data Access Object) which serializes every data object into its own file:
 * {@link com.schoste.ddd.infrastructure.dao.v4.serialization.AbstractSerializationDAO}.
 * Data objects are deleted logically (the deleted flag is serialized); time stamps come from the clock of the host whose
 * file system stores the files. The storage folder must contain nothing but the files of the data objects
 * (see {@link com.schoste.ddd.infrastructure.dao.v4.fs.AbstractFileStorageDAO}).
 *
 * Example of a DAO for the data object {@code CustomerDO} (see the overview of the {@code dao4} artifact), created without
 * a framework:
 * <pre>
 * import java.nio.file.Files;
 *
 * import com.schoste.ddd.infrastructure.dao.v4.exceptions.DAOException;
 * import com.schoste.ddd.infrastructure.dao.v4.serialization.AbstractSerializationDAO;
 * import com.schoste.ddd.infrastructure.dao.v4.services.DAO;
 *
 * public class CustomerDAO extends AbstractSerializationDAO&lt;Long, CustomerDO&gt;
 * {
 *     private long lastVirtualId = 0;
 *
 *     public CustomerDAO(String storageFolder) throws DAOException
 *     {
 *         super(storageFolder);   // creates the folder if necessary
 *     }
 *
 *     protected CustomerDO doCreateDataObject() { return new CustomerDO(); }
 *
 *     // virtual ids are negative, actual ids are positive
 *     protected synchronized Long doCreateVirtualId() { return --this.lastVirtualId; }
 *     protected boolean doIsVirtualId(Long id) { return id &lt;= 0; }
 *
 *     // the file of a data object is named after its actual id
 *     protected Long toId(String fileName) { return Long.valueOf(fileName); }
 *
 *     // an actual id must not belong to an existing file; starting at the current time keeps ids unique across restarts
 *     // without remembering a counter (so actual ids look like time stamps)
 *     protected synchronized Long doCreateActualId(Long virtualId)
 *     {
 *         long id = System.currentTimeMillis();
 *
 *         while (Files.exists(this.fileOf(id))) id++;
 *
 *         return id;
 *     }
 * }
 *
 * DAO&lt;Long, CustomerDO&gt; dao = new CustomerDAO("/var/data/customers");
 * </pre>
 *
 * With {@code Integer} ids the time-based actual ids above would overflow; take the first free number instead (the same
 * applies to the virtual ids, which are counted down from 0):
 * <pre>
 * private int lastVirtualId = 0;
 *
 * protected synchronized Integer doCreateVirtualId() { return --this.lastVirtualId; }
 * protected boolean doIsVirtualId(Integer id) { return id &lt;= 0; }
 *
 * protected Integer toId(String fileName) { return Integer.valueOf(fileName); }
 *
 * // searches from 1 on, so ids of deleted (removed) files may be reused
 * protected synchronized Integer doCreateActualId(Integer virtualId)
 * {
 *     int id = 1;
 *
 *     while (Files.exists(this.fileOf(id))) id++;
 *
 *     return id;
 * }
 * </pre>
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 */
package com.schoste.ddd.infrastructure.dao.v4.serialization;
