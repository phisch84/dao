package com.schoste.ddd.infrastructure.dao.v4.services;

import java.util.HashMap;
import java.util.Map;

import com.schoste.ddd.infrastructure.dao.v4.exceptions.DAOException;

/**
 * Base class for implementations of {@link DAOTransactionManager} for a storage.
 *
 * This class implements the parts of the contract of {@link DAOTransactionManager} which are the same for every storage: binding
 * a transaction to the thread which began it, nesting (inner transactions join the outermost one), marks for rollback, and the
 * completion of each transaction exactly once. An implementation only implements the protected methods whose names start with
 * {@code do}; they begin, commit and roll back the transaction of a <i>resource</i> of the storage (e.g. a database session),
 * and release the resource.
 * <p>
 * A transaction is bound to its thread by the <i>resource key</i> ({@link #getResourceKey()}), which identifies the storage
 * (e.g. the session factory of a database). Managers with the same resource key share the transactions of a thread.
 * The DAOs of the storage join a transaction by getting its resource with {@link #getBoundResource(Object, Class)}: if there
 * is one, they use it instead of an own one, and they don't complete its transaction. If one of their calls fails, they mark
 * the transaction for rollback with {@link #setRollbackOnly(Object)}.
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 *
 * @param <R> the class of the storage's resource which carries a transaction (e.g. a database session)
 */
public abstract class AbstractDAOTransactionManager<R> implements DAOTransactionManager
{
	/**
	 * The transaction of a thread for one resource key: its resource, the number of transactions which haven't been completed yet
	 * (the outermost one and the inner ones which joined it), and whether it is marked for rollback.
	 */
	protected static class Binding
	{
		/**
		 * The resource which carries the transaction.
		 */
		protected final Object resource;

		/**
		 * The thread which began the transaction.
		 */
		protected final Thread thread;

		/**
		 * The number of transactions (the outermost and the inner ones) which haven't been completed yet.
		 */
		protected int openTransactions = 0;

		/**
		 * Whether the transaction is marked for rollback.
		 */
		protected boolean rollbackOnly = false;

		/**
		 * Whether the outermost transaction has been completed and the resource has been released.
		 */
		protected boolean released = false;

		/**
		 * Creates the binding of a resource to the current thread.
		 *
		 * @param resource the resource which carries the transaction
		 */
		protected Binding(Object resource)
		{
			this.resource = resource;
			this.thread = Thread.currentThread();
		}
	}

	/**
	 * The transactions of each thread, by their resource keys.
	 */
	private static final ThreadLocal<Map<Object, Binding>> BINDINGS = ThreadLocal.withInitial(HashMap::new);

	/**
	 * Gets the transaction of the current thread for a resource key.
	 *
	 * @param resourceKey the resource key
	 * @return the binding of the transaction, or null if the thread has none
	 */
	private static Binding getBinding(Object resourceKey)
	{
		return BINDINGS.get().get(resourceKey);
	}

	/**
	 * Gets the resource of the transaction which the current thread has for a storage. DAOs call it to join the transaction.
	 *
	 * @param <T> the class of the resource
	 * @param resourceKey the resource key of the storage (see {@link #getResourceKey()})
	 * @param resourceClass the class of the resource
	 * @return the resource, or null if the current thread has no transaction for the resource key, or its resource isn't an instance
	 * of the given class
	 */
	public static <T> T getBoundResource(Object resourceKey, Class<T> resourceClass)
	{
		Binding binding = getBinding(resourceKey);

		return ((binding != null) && resourceClass.isInstance(binding.resource)) ? resourceClass.cast(binding.resource) : null;
	}

	/**
	 * Marks the transaction which the current thread has for a storage for rollback. DAOs call it when one of their calls fails
	 * in a transaction; committing the outermost transaction then rolls back the storage and fails.
	 *
	 * @param resourceKey the resource key of the storage (see {@link #getResourceKey()})
	 * @return true if the thread has a transaction for the resource key, which is marked now; false if it has none
	 */
	public static boolean setRollbackOnly(Object resourceKey)
	{
		Binding binding = getBinding(resourceKey);

		if (binding == null) return false;

		binding.rollbackOnly = true;

		return true;
	}

	/**
	 * A transaction begun by {@link AbstractDAOTransactionManager#begin()}: the outermost transaction of its thread, or an inner one
	 * which has joined it.
	 */
	protected class BoundTransaction implements DAOTransaction
	{
		/**
		 * The resource key of the storage.
		 */
		protected final Object resourceKey;

		/**
		 * The binding which this transaction began or joined.
		 */
		protected final Binding binding;

		/**
		 * Whether this transaction began the binding.
		 */
		protected final boolean outermost;

		/**
		 * Whether this transaction has been committed or rolled back.
		 */
		protected boolean completed = false;

		/**
		 * Creates a transaction.
		 *
		 * @param resourceKey the resource key of the storage
		 * @param binding the binding which this transaction began or joined
		 * @param outermost whether this transaction began the binding
		 */
		protected BoundTransaction(Object resourceKey, Binding binding, boolean outermost)
		{
			this.resourceKey = resourceKey;
			this.binding = binding;
			this.outermost = outermost;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public boolean isOutermost()
		{
			return this.outermost;
		}

		/**
		 * Checks that this transaction can be completed by the current thread.
		 *
		 * @throws IllegalStateException thrown if it has been completed already, or if it belongs to another thread
		 */
		protected void checkCompletable() throws IllegalStateException
		{
			if (this.completed) throw new IllegalStateException("The transaction has been completed already");
			if (this.binding.thread != Thread.currentThread()) throw new IllegalStateException("The transaction belongs to another thread");
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void commit() throws IllegalStateException, DAOException
		{
			this.checkCompletable();

			if (!this.outermost)
			{
				if (this.binding.released) throw new IllegalStateException("The outermost transaction has been completed already");

				this.completeInner();

				return;
			}

			if (this.binding.openTransactions > 1) throw new IllegalStateException("Inner transactions haven't been completed yet");

			this.completed = true;

			boolean rollbackOnly = this.binding.rollbackOnly;

			AbstractDAOTransactionManager.this.complete(this.resourceKey, this.binding, !rollbackOnly);

			// an inner transaction or a failed DAO call has marked the transaction for rollback, so it has been rolled back instead
			if (rollbackOnly) throw new DAOException(new IllegalStateException("The transaction has been rolled back, since it was marked for rollback"));
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void rollback() throws IllegalStateException, DAOException
		{
			this.checkCompletable();

			if (!this.outermost)
			{
				// once the outermost transaction has been completed, an inner one has nothing left to roll back
				this.binding.rollbackOnly = true;

				this.completeInner();

				return;
			}

			this.completed = true;

			AbstractDAOTransactionManager.this.complete(this.resourceKey, this.binding, false);
		}

		/**
		 * Completes an inner transaction: it no longer counts as open in its binding.
		 */
		protected void completeInner()
		{
			this.completed = true;
			this.binding.openTransactions--;
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void close() throws DAOException
		{
			if (!this.completed) this.rollback();
		}
	}

	/**
	 * Creates a new transaction manager. Implementations call it from their own constructors.
	 */
	protected AbstractDAOTransactionManager()
	{
		super();
	}

	/**
	 * Gets the key which identifies the storage, e.g. the session factory of a database. Transactions are bound to a thread by it:
	 * managers with the same resource key share the transactions of a thread, and DAOs find the resource of the transaction by it
	 * ({@link #getBoundResource(Object, Class)}). It must be the same object for the whole life of the manager.
	 *
	 * @return the resource key, never null
	 */
	protected abstract Object getResourceKey();

	/**
	 * Gets a resource of the storage and begins a transaction on it.
	 *
	 * @return the resource which carries the transaction, never null
	 * @throws Exception re-throws every exception
	 */
	protected abstract R doBegin() throws Exception;

	/**
	 * Commits the transaction of a resource. If it throws, the transaction is rolled back ({@link #doRollback(Object)}).
	 *
	 * @param resource the resource which carries the transaction
	 * @throws Exception re-throws every exception
	 */
	protected abstract void doCommit(R resource) throws Exception;

	/**
	 * Rolls back the transaction of a resource.
	 *
	 * @param resource the resource which carries the transaction
	 * @throws Exception re-throws every exception
	 */
	protected abstract void doRollback(R resource) throws Exception;

	/**
	 * Releases a resource after its transaction has been committed or rolled back, e.g. closes a database session.
	 * It is called in any case, also if committing or rolling back failed.
	 *
	 * @param resource the resource which carried the transaction
	 * @throws Exception re-throws every exception
	 */
	protected abstract void doRelease(R resource) throws Exception;

	/**
	 * {@inheritDoc}
	 */
	@Override
	public DAOTransaction begin() throws DAOException
	{
		Object resourceKey = this.getResourceKey();
		Binding binding = getBinding(resourceKey);
		boolean outermost = (binding == null);

		if (outermost)
		{
			try
			{
				binding = new Binding(this.doBegin());
			}
			catch (Exception e)
			{
				throw new DAOException(e);
			}

			BINDINGS.get().put(resourceKey, binding);
		}

		binding.openTransactions++;

		return new BoundTransaction(resourceKey, binding, outermost);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isActive()
	{
		return getBinding(this.getResourceKey()) != null;
	}

	/**
	 * Completes the outermost transaction of the current thread: removes its binding from the thread, commits or rolls back
	 * its resource, and releases the resource in any case. If committing fails, the resource is rolled back.
	 *
	 * @param resourceKey the resource key of the storage
	 * @param binding the binding of the transaction
	 * @param commit true to commit, false to roll back
	 * @throws DAOException re-throws every exception of the storage
	 */
	@SuppressWarnings("unchecked")
	protected void complete(Object resourceKey, Binding binding, boolean commit) throws DAOException
	{
		R resource = (R) binding.resource;
		Exception failure = null;

		BINDINGS.get().remove(resourceKey);
		binding.released = true;

		try
		{
			if (commit)
			{
				try
				{
					this.doCommit(resource);
				}
				catch (Exception e)
				{
					failure = e;

					this.doRollback(resource);
				}
			}
			else
			{
				this.doRollback(resource);
			}
		}
		catch (Exception e)
		{
			if (failure == null) failure = e;
			else failure.addSuppressed(e);
		}
		finally
		{
			try
			{
				this.doRelease(resource);
			}
			catch (Exception e)
			{
				if (failure == null) failure = e;
				else failure.addSuppressed(e);
			}
		}

		if (failure != null) throw new DAOException(failure);
	}
}
