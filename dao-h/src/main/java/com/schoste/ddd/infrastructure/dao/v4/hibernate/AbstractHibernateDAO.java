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
import org.hibernate.query.Query;

import com.schoste.ddd.infrastructure.dao.v4.models.DataObject;
import com.schoste.ddd.infrastructure.dao.v4.services.DataObjectLoader;
import com.schoste.ddd.infrastructure.dao.v4.services.AbstractDAO;
import com.schoste.ddd.infrastructure.dao.v4.services.SourceRecordDataObjectLoader;
import com.schoste.ddd.infrastructure.dao.v4.services.GenericRecentDataObjectLoader;

/**
 * Version 3 implementation of the DAO interface to persist data objects
 * via Hibernate.
 * Data objects are loaded with HQL queries on the entity named {@link #getEntityName()}.
 *
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 *
 * @param <ID> the class of the data object's identifier
 * @param <DO> the class of the data object to persist
 */
public abstract class AbstractHibernateDAO <ID extends Serializable, DO extends DataObject<ID>> extends AbstractDAO<ID, DO>
{
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
	 * {@inheritDoc}
	 */
	@Override
	protected synchronized void doDelete(DO dataObject) throws Exception
	{
		dataObject.setDeleted(true);

		if (this.isVirtualId(dataObject.getId())) return;

		this.inTransaction(session -> session.remove(dataObject));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void doDelete(Collection<DO> dataObjects) throws Exception
	{
		this.inTransaction(session ->
		{
			for (DO dataObject : dataObjects)
			{
				dataObject.setDeleted(true);

				if (this.isVirtualId(dataObject.getId())) continue;

				session.remove(dataObject);
			}
		});
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void doDelete(ID[] dataObjectIds) throws Exception
	{
		this.inTransaction(session ->
		{
			for (ID dataObjectId : dataObjectIds)
			{
				Object dataObject = session.get(this.getDataObjectClass(), dataObjectId);

				// ids without data object in the storage are ignored (as by the other DAO implementations)
				if (dataObject != null) session.remove(dataObject);
			}
		});
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
	 * The time stamps of the data object are expected to be up to date already (as {@link AbstractDAO#save(DataObject)} does).
	 */
	@Override
	protected synchronized void doSave(DO dataObject) throws Exception
	{
		this.inTransaction(session -> this.merge(session, dataObject));
	}

	/**
	 * {@inheritDoc}
	 *
	 * The time stamps of the data objects are expected to be up to date already (as {@link AbstractDAO#save(Collection)} does).
	 */
	@Override
	protected synchronized void doSave(Collection<DO> dataObjects) throws Exception
	{
		this.inTransaction(session ->
		{
			for (DO dataObject : dataObjects) this.merge(session, dataObject);
		});
	}

	/**
	 * Deletes all data objects from the storage with a single HQL statement.
	 * The DAO's most recent modification time stamp is reset by {@link AbstractDAO#clear()}.
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
		Stream<DO> dataObjects = ids.isEmpty() ? this.streamDataObjects(null) : this.streamDataObjects("e.id IN ?1", ids);

		return new SourceRecordDataObjectLoader<>(this, dataObjects, Function.identity(), null);
	}

	/**
	 * {@inheritDoc}
	 *
	 * The data objects are filtered by their deleted flag and modification time stamp in the HQL query. The loaded entities are the source records.
	 */
	@Override
	protected DataObjectLoader<?, ID, DO> doCreateLoader(long modifiedSince, DeletedDataObjects deleted) throws Exception
	{
		Stream<DO> dataObjects = this.streamDataObjects("((e.deleted = false) OR (e.deleted = ?1)) AND (e.modifiedTimestamp > ?2)", deleted == DeletedDataObjects.INCLUDE, modifiedSince);

		return new GenericRecentDataObjectLoader<>(this, dataObjects, Function.identity(), modifiedSince, deleted == DeletedDataObjects.INCLUDE);
	}
}