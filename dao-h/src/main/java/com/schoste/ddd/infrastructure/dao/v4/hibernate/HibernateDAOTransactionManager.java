package com.schoste.ddd.infrastructure.dao.v4.hibernate;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import com.schoste.ddd.infrastructure.dao.v4.services.AbstractDAOTransactionManager;

/**
 * Transaction manager for the Hibernate DAOs ({@link AbstractHibernateDAO}) of one session factory: a transaction spans
 * all DAOs whose {@link AbstractHibernateDAO#getSessionFactory()} returns that session factory.
 *
 * A transaction is a Hibernate session with a database transaction, opened when the outermost transaction begins and closed when
 * it is completed. While it is open, the DAOs of the session factory use this session for all calls of the thread which began it
 * (see {@link AbstractHibernateDAO}). Managers of the same session factory share the transactions of a thread.
 * <pre>
 * DAOTransactionManager transactionManager = new HibernateDAOTransactionManager(sessionFactory);
 *
 * try (DAOTransaction transaction = transactionManager.begin())
 * {
 *     customerDAO.save(customer);
 *     orderDAO.save(order);
 *
 *     transaction.commit();
 * }
 * </pre>
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 */
public class HibernateDAOTransactionManager extends AbstractDAOTransactionManager<Session>
{
	/**
	 * The session factory whose DAOs join the transactions of this manager.
	 */
	protected final SessionFactory sessionFactory;

	/**
	 * Creates a transaction manager for the DAOs of a session factory.
	 *
	 * @param sessionFactory the session factory of the DAOs
	 * @throws IllegalArgumentException thrown if parameter sessionFactory is null
	 */
	public HibernateDAOTransactionManager(SessionFactory sessionFactory) throws IllegalArgumentException
	{
		super();

		if (sessionFactory == null) throw new IllegalArgumentException("sessionFactory");

		this.sessionFactory = sessionFactory;
	}

	/**
	 * {@inheritDoc}
	 *
	 * The resource key is the session factory.
	 */
	@Override
	protected Object getResourceKey()
	{
		return this.sessionFactory;
	}

	/**
	 * {@inheritDoc}
	 *
	 * Opens a session and begins its transaction.
	 */
	@Override
	protected Session doBegin() throws Exception
	{
		Session session = this.sessionFactory.openSession();

		try
		{
			session.beginTransaction();

			return session;
		}
		catch (Exception e)
		{
			session.close();

			throw e;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void doCommit(Session session) throws Exception
	{
		session.getTransaction().commit();
	}

	/**
	 * {@inheritDoc}
	 *
	 * Does nothing if the session's transaction isn't active any more (e.g. a failed commit has rolled it back already).
	 */
	@Override
	protected void doRollback(Session session) throws Exception
	{
		Transaction transaction = session.getTransaction();

		if (transaction.isActive()) transaction.rollback();
	}

	/**
	 * {@inheritDoc}
	 *
	 * Closes the session.
	 */
	@Override
	protected void doRelease(Session session) throws Exception
	{
		session.close();
	}
}
