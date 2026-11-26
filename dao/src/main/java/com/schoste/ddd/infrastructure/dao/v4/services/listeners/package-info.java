/**
 * Listeners which are called before and after the operations of a DAO and may change or cancel them:
 * {@link com.schoste.ddd.infrastructure.dao.v4.services.listeners.GetListener},
 * {@link com.schoste.ddd.infrastructure.dao.v4.services.listeners.SaveListener} and
 * {@link com.schoste.ddd.infrastructure.dao.v4.services.listeners.DeleteListener}.
 * All their methods have default implementations, so a listener only implements the methods it needs.
 * Listeners are called in the order of their registration; a listener which cancels an operation stops the listeners
 * registered after it.
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 */
package com.schoste.ddd.infrastructure.dao.v4.services.listeners;
