-- Database initialization script (TEST) for PostgreSQL
-- This script is supposed to be run on every integration test of the application
-- It is supposed to clear all existing data and re-create the structure


-- Start hibernate_do Table ----

DROP TABLE IF EXISTS public.hibernate_do CASCADE;
CREATE TABLE public.hibernate_do
(
    id serial,
    created bigint NOT NULL DEFAULT CAST(EXTRACT(EPOCH FROM now())*1000 AS bigint),
    modified bigint NOT NULL DEFAULT CAST(EXTRACT(EPOCH FROM now())*1000 AS bigint),
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    example_string_property varchar(255) NOT NULL,
    PRIMARY KEY (id)
)
WITH (
    OIDS = FALSE
);

COMMENT ON TABLE public.hibernate_do IS 'Table for storing Hibernate example DOs';
-- End displayobjects Table ----