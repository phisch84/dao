package com.schoste.ddd.infrastructure.dal.v3.services.hibernate;

import java.util.Collection;
import java.util.Map;

import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

/**
 * Static builder for the {@link SessionFactory}.
 * {@link SessionFactoryBuilder#buildSessionFactory(Map, Collection)} is supposed to be called by Spring as configured in the module.xml.
 * In previous versions of Hibernate the session factory could be configured in Spring's XML. From Hibernate 5/6 on it must be done programmatically.
 *
 * @author Philipp Schosteritsch <s.philipp@schoste.com>
 */
public class SessionFactoryBuilder
{
    /**
     * Builds a new session factory. All given settings are applied to the service registry and all given resources
     * are added as metadata sources.
     *
     * @param settings the Hibernate settings (e.g. "hibernate.connection.url") to apply. May be null
     * @param resources the names of the mapping resources (e.g. "META-INF/example-entities.xml"). May be null
     * @return a new session factory
     */
    public static SessionFactory buildSessionFactory(Map<String, Object> settings, Collection<String> resources)
    {
        StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder();

        if (settings != null) settings.forEach(builder::applySetting);

        StandardServiceRegistry registry = builder.build();
        MetadataSources sources = new MetadataSources(registry);

        if (resources != null) resources.forEach(sources::addResource);

        Metadata metadata = sources.buildMetadata();

        return metadata.buildSessionFactory();
    }
}