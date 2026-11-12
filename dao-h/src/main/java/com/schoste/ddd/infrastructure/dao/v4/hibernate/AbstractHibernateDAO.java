package com.schoste.ddd.infrastructure.dao.v4.hibernate;
import java.io.Serializable;
import java.util.Collection;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import com.schoste.ddd.infrastructure.dao.v4.services.DAO;
import com.schoste.ddd.infrastructure.dao.v4.services.DeletedDataObjects;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.query.Query;
import org.hibernate.type.DbTimestampType;

import com.schoste.ddd.infrastructure.dao.v4.models.DataObject;
import com.schoste.ddd.infrastructure.dao.v4.services.DataObjectLoader;
import com.schoste.ddd.infrastructure.dao.v4.services.AbstractDAO;
import com.schoste.ddd.infrastructure.dao.v4.services.SourceRecordDataObjectLoader;

/**
 * Implementation of the DAO interface which stores data objects in a database via Hibernate.
 * Data objects are loaded with HQL queries on the entity named {@link #getEntityName()}.
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 *
 * @param <ID> the class of the data object's identifier
 * @param <DO> the class of the data object to persist
 */
public abstract class AbstractHibernateDAO <ID extends Serializable, DO extends DataObject<ID>> extends AbstractDAO<ID, DO>
{
	/**
	 * Creates a new Hibernate DAO. Implementations call it from their own constructors.
	 */
	protected AbstractHibernateDAO()
	{
		super();
	}

	/**
	 * Gets the Hibernate session factory used to open the sessions for all storage operations of this DAO.
	 * Each operation opens its own session and closes it once it is done (data object loaders close their session
	 * when the stream they serve is closed).
	 *
	 * @return the session factory to open sessions with
	 */
	protected abstract SessionFactory getSessionFactory();

	/**
	 * An operation on an open session (see {@link AbstractHibernateDAO#inTransaction(SessionOperation)}).
	 */
	@FunctionalInterface
	protected interface SessionOperation
	{
		/**
		 * Performs the operation.
		 *
		 * @param session the open session to perform the operation with
		 * @throws Exception re-throws every exception
		 */
		void run(Session session) throws Exception;
	}

	/**
	 * Runs an operation in a transaction on a new session. The transaction is committed if the operation succeeds
	 * and rolled back if it throws an exception. The session is closed in any case.
	 *
	 * @param operation the operation to run
	 * @throws Exception re-throws every exception
	 */
	protected void inTransaction(SessionOperation operation) throws Exception
	{
		try (Session session = this.getSessionFactory().openSession())
		{
			Transaction transaction = session.beginTransaction();

			try
			{
				operation.run(session);

				transaction.commit();
			}
			catch (Exception e)
			{
				transaction.rollback();

				throw e;
			}
		}
	}

	/**
	 * Merges a data object into a session and takes over the id assigned by the storage (e.g. for a new data object).
	 *
	 * @param session the session to merge the data object into
	 * @param dataObject the data object to merge
	 */
	@SuppressWarnings("unchecked")
	private void merge(Session session, DO dataObject)
	{
		dataObject.setId(((DO) session.merge(dataObject)).getId());
	}

	/**
	 * Gets the current time of the database's clock, as used for the time stamps of saved data objects.
	 * The time is queried by the means of the session's SQL dialect; if the dialect doesn't support it,
	 * Hibernate falls back to the clock of the host. Precision and time zone are therefore the database's
	 * (e.g. SQLite's {@code current_timestamp} has whole seconds only).
	 * <p>
	 * This is a hook: a DAO which needs another clock overrides it, e.g. to return {@link System#currentTimeMillis()}
	 * when the application is the only writer of the database and needs time stamps with milliseconds. The returned
	 * time is set as modification time stamp (and as creation time stamp of new data objects) of all data objects
	 * saved in the session's transaction.
	 *
	 * @param session the open session
	 * @return the current time of the database in milliseconds since 1970-01-01 UTC
	 */
	protected long getStorageTimestamp(Session session)
	{
		return DbTimestampType.INSTANCE.seed((SharedSessionContractImplementor) session).getTime();
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected synchronized DO doGet(ID id) throws Exception
	{
		try (Session session = this.getSessionFactory().openSession())
		{
			return (DO) session.get(this.getDataObjectClass(), id);
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * The time stamps are taken from the database's clock (see {@link #getStorageTimestamp(Session)}).
	 */
	@Override
	protected synchronized void doSave(DO dataObject) throws Exception
	{
		this.inTransaction(session ->
		{
			updateTimestamps(dataObject, this.getStorageTimestamp(session));

			this.merge(session, dataObject);
		});
	}

	/**
	 * {@inheritDoc}
	 *
	 * The time stamps are taken from the database's clock (see {@link #getStorageTimestamp(Session)}); all data objects of
	 * the call get the same time stamp, since they are saved in one transaction.
	 */
	@Override
	protected synchronized void doSave(Collection<DO> dataObjects) throws Exception
	{
		this.inTransaction(session ->
		{
			long storageTimestamp = this.getStorageTimestamp(session);

			for (DO dataObject : dataObjects)
			{
				updateTimestamps(dataObject, storageTimestamp);

				this.merge(session, dataObject);
			}
		});
	}

	/**
	 * Deletes all data objects from the storage with a single HQL statement.
	 *
	 * @throws Exception re-throws every exception
	 */
	@Override
	protected void doClear() throws Exception
	{
		this.inTransaction(session -> session.createQuery(String.format("DELETE FROM %s e", this.getEntityName())).executeUpdate());
	}

	/**
	 * Gets the name of the data objects' entity as used in HQL queries.
	 * By default it is the simple class name of the data objects ({@link AbstractDAO#getDataObjectClass()}).
	 * Override this method if the entity is mapped with a different name.
	 *
	 * @return the entity name of the data objects
	 * @throws Exception re-throws every exception
	 */
	protected String getEntityName() throws Exception
	{
		return this.getDataObjectClass().getSimpleName();
	}

	/**
	 * Creates a query on a new session that streams the data objects which match the given condition.
	 * Every data object is evicted from the session before it is passed on, to release it from the session's memory.
	 * Closing the returned stream also closes the session.
	 *
	 * @param whereClause the HQL condition (entity alias e, positional parameters ?1, ?2, ...) or null to stream all data objects
	 * @param params the values of the positional parameters, in order
	 * @return a stream of the data objects that match the condition
	 * @throws Exception re-throws every exception
	 */
	@SuppressWarnings("unchecked")
	protected Stream<DO> streamDataObjects(String whereClause, Object... params) throws Exception
	{
		Session session = this.getSessionFactory().openSession();

		try
		{
			String hql = String.format("SELECT e FROM %s e", this.getEntityName());

			if (whereClause != null) hql += " WHERE " + whereClause;

			Query<DO> query = session.createQuery(hql, this.getDataObjectClass());

			for (int i=0; i<params.length; i++) query.setParameter(i+1, params[i]);

			return query.stream().peek(session::evict).onClose(session::close);
		}
		catch (Exception e)
		{
			session.close();

			throw e;
		}
	}

	/**
	 * {@inheritDoc}
	 *
	 * The data objects are filtered by their ids in the HQL query. The loaded entities are the source records.
	 */
	@Override
	protected DataObjectLoader<?, ID, DO> doCreateLoader(Set<ID> ids) throws Exception
	{
		Stream<DO> dataObjects = this.streamDataObjects("e.id IN ?1", ids);

		return SourceRecordDataObjectLoader.forIds(this, dataObjects, Function.identity(), ids);
	}

	/**
	 * {@inheritDoc}
	 *
	 * The data objects are filtered by their deleted flag and modification time stamp in the HQL query. The loaded entities are the source records.
	 */
	@Override
	protected DataObjectLoader<?, ID, DO> doCreateLoader(long modifiedSince, DeletedDataObjects deleted) throws Exception
	{
		Stream<DO> dataObjects = this.streamDataObjects("((e.deleted = false) OR (e.deleted = ?1)) AND (e.modifiedTimestamp >= ?2)", deleted == DeletedDataObjects.INCLUDE, modifiedSince);

		return SourceRecordDataObjectLoader.forModifiedSince(this, dataObjects, Function.identity(), modifiedSince, deleted);
	}
}