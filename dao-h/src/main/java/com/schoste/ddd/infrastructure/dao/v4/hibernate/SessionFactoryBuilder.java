package com.schoste.ddd.infrastructure.dao.v4.hibernate;

import java.util.Collection;
import java.util.Map;

import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

/**
 * Static builder for the {@link SessionFactory}.
 * Call {@link SessionFactoryBuilder#buildSessionFactory(Map, Collection)} from your own code, or let a dependency injection
 * framework call it (e.g. Spring's {@code MethodInvokingFactoryBean}). It is one way of creating the session factory a DAO
 * needs, without any framework; with Spring, {@code org.springframework.orm.hibernate5.LocalSessionFactoryBean} (from
 * {@code spring-orm}, which this library doesn't bring along) is an alternative.
 *
 * @author Philipp Schosteritsch &lt;s.philipp@schoste.com&gt;
 */
public class SessionFactoryBuilder
{
    /**
     * Not to be instantiated; the class only has a static method.
     */
    private SessionFactoryBuilder()
    {
    }

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