package com.schoste.ddd.infrastructure.dao.v4.services;

import com.schoste.ddd.infrastructure.dao.v4.exceptions.DAOException;

/**
 * A transaction begun by a {@link DAOTransactionManager}. The DAO calls which the beginning thread makes until the transaction is
 * committed or rolled back join it. Each transaction is completed exactly once, by {@link #commit()} or {@link #rollback()};
 * {@link #close()} rolls it back if it hasn't been completed yet.
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 */
public interface DAOTransaction extends AutoCloseable
{
	/**
	 * Determines if this is the outermost transaction of its thread, which commits or rolls back the storage. An inner transaction
	 * has joined a transaction which the thread had already (see {@link DAOTransactionManager}).
	 *
	 * @return true if this transaction began the storage's transaction, false if it joined one
	 */
	public boolean isOutermost();

	/**
	 * Commits the transaction. The outermost transaction stores all changes made in it; an inner transaction doesn't store anything.
	 * If the outermost transaction has been marked for rollback (by an inner transaction or by a failed DAO call), the storage
	 * is rolled back instead and a {@link DAOException} is thrown. If storing fails, the storage is rolled back as well.
	 *
	 * @throws IllegalStateException thrown if the transaction has been completed already, if it is used by another thread than the one
	 * which began it, or if it is the outermost transaction and inner transactions haven't been completed yet (the transaction stays open)
	 * @throws DAOException thrown if the storage has been rolled back instead of committed; re-throws every exception of the storage
	 */
	public void commit() throws IllegalStateException, DAOException;

	/**
	 * Rolls the transaction back. The outermost transaction discards all changes made in it, also if inner transactions haven't been
	 * completed yet; an inner transaction marks the outermost one for rollback.
	 *
	 * @throws IllegalStateException thrown if the transaction has been completed already, or if it is used by another thread than the one
	 * which began it
	 * @throws DAOException re-throws every exception of the storage as DAOException
	 */
	public void rollback() throws IllegalStateException, DAOException;

	/**
	 * Rolls the transaction back ({@link #rollback()}) if it hasn't been completed yet; otherwise does nothing.
	 *
	 * @throws DAOException re-throws every exception of the storage as DAOException
	 */
	@Override
	public void close() throws DAOException;
}
