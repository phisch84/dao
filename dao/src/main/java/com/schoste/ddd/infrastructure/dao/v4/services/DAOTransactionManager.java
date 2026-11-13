package com.schoste.ddd.infrastructure.dao.v4.services;

import com.schoste.ddd.infrastructure.dao.v4.exceptions.DAOException;

/**
 * Begins transactions which several calls of DAOs ({@link DAO}) join, so that their changes are stored all together or not at all.
 *
 * Without a transaction, every call of a DAO which changes the storage (e.g. {@link DAO#save(java.util.Collection)}) is atomic
 * on its own, but several calls aren't. A transaction groups them:
 * <pre>
 * try (DAOTransaction transaction = transactionManager.begin())
 * {
 *     customerDAO.save(customer);
 *     orderDAO.save(order);
 *
 *     transaction.commit();
 * }
 * </pre>
 * Rules which hold for every transaction manager:
 * <ul>
 * <li><b>Threads:</b> a transaction belongs to the thread which began it. The DAOs join it in calls of that thread only;
 * calls of other threads aren't part of it.</li>
 * <li><b>Storage:</b> a transaction spans the DAOs of one storage (e.g. the DAOs which share one database), which the
 * implementation names in its class documentation. DAOs of other storages don't join it.</li>
 * <li><b>Nesting:</b> {@link #begin()} on a thread which has a transaction already joins it: the returned transaction is an
 * <i>inner</i> one ({@link DAOTransaction#isOutermost()} is false). Committing an inner transaction doesn't store anything;
 * rolling it back, or closing it without commit, marks the whole transaction for rollback. Only the outermost transaction
 * commits or rolls back the storage. This lets a method use a transaction regardless of whether its caller has one.</li>
 * <li><b>Reads</b> of the DAOs in a transaction see the changes made in it.</li>
 * <li>A failed call of a DAO in a transaction marks it for rollback.</li>
 * </ul>
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 */
public interface DAOTransactionManager
{
	/**
	 * Begins a transaction on the current thread, or joins the one the thread has already (see the rules on nesting above).
	 *
	 * @return the transaction; it must be committed or rolled back, best by a try-with-resources statement, which rolls back
	 * a transaction that hasn't been committed
	 * @throws DAOException re-throws every exception of the storage as DAOException
	 */
	public DAOTransaction begin() throws DAOException;

	/**
	 * Determines if the current thread has a transaction of this manager's storage.
	 *
	 * @return true if the current thread has a transaction, which DAO calls of the thread join
	 */
	public boolean isActive();
}
