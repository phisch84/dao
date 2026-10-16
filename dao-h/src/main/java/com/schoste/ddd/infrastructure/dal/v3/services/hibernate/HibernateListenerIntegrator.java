package com.schoste.ddd.infrastructure.dal.v3.services.hibernate;

import org.hibernate.boot.Metadata;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

/**
 * Hibernate Integrator class to register the entity listener ({@link GenericDataObjectListener})
 * for pre-insert and pre-update events.
 * The class must be defined in META-INF/services/org.hibernate.integrator.spi.Integrator.
 * 
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 */
public class HibernateListenerIntegrator implements Integrator
{
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void disintegrate(SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) 
	{
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void integrate(Metadata metadata, SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry)
	{
		final EventListenerRegistry eventRegistry = serviceRegistry.getService(EventListenerRegistry.class);
		
		eventRegistry.prependListeners(EventType.PRE_UPDATE, GenericDataObjectListener.class);		
		eventRegistry.prependListeners(EventType.PRE_INSERT, GenericDataObjectListener.class);
	}
}